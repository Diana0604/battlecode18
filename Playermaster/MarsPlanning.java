import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class MarsPlanning{

    private static MarsPlanning instance;

    private static final int AUX = 6;
    private static final int base = 0x3F;
    private static final int DEPTH = 40;
    private static final int MAX_BURST_ROUND = 900;
    private static final int BURST_WAIT_ROUNDS = 100;
    private static final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};

    static int[][] cc; // va entre [1,ccs]
    private static int ccs;
    private static boolean[][] passable;

    static int W;
    static int H;
    static int takeoffBurstRound;

    private static int[] arrivalTime = new int[1001];
    private static int[] departTime = new int[1001]; //todo aixo mai es fa servir
    private static int[] optimArrivalTime = new int[1001];

    static int[][] marsInitialKarbonite;
    static double[][] marsInitialPriorityKarbo;
    static double[] marsInitialKarbo_cc;
    static int[][] clear;
    static int[] areaCC;
    static int sumaArees;
    static double karbo350 = -1;
    static double biggestArea;
    static double biggestAreaPercent;


    public static void initGame(){
        try {
            PlanetMap planetMap = GC.gc.startingMap(Planet.Mars);
            initBasics(planetMap);
            computePassable(planetMap);
            planetMap.delete();
            computeCCs();
            initArraysDependingOnCCs();
            computeClear();
            computeArees();
            computeOrbitStuff();
            constructAsteroidPattern();

            marsInitialKarbonite = Wrapper.getMarsInitialKarbonite();
            marsInitialPriorityKarbo = computeInitialPriorityKarboMars();

            int round350 = optimArrivalTime[276+27];
            bestPlaceForRound(round350); // aixo calcula biggestArea i karbo350
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void initBasics(PlanetMap planetMap) {
        W = (int) planetMap.getWidth();
        H = (int) planetMap.getHeight();
        cc = new int[W][H];
        ccs = 0;
    }

    private static void computePassable(PlanetMap planetMap) {
        passable = new boolean[W][H];
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                passable[x][y] = planetMap.isPassableTerrainAt(new MapLocation(Planet.Mars, x, y)) != 0;
                if (!passable[x][y]) cc[x][y] = -1;
            }
        }
    }

    private static void computeCCs() {
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                if (cc[x][y] == 0) bfs(x, y);
            }
        }
    }

    private static void bfs(int a, int b){
        try {
            Queue<Integer> queue = new LinkedList<>();
            queue.offer((a << AUX) | b);
            cc[a][b] = ++ccs;

            while (!queue.isEmpty()) {
                int data = queue.poll();
                int myPosX = (data >> AUX) & base;
                int myPosY = data & base;
                for (int i = 0; i < X.length; ++i) {
                    int newPosX = myPosX + X[i];
                    int newPosY = myPosY + Y[i];
                    if (newPosX >= W || newPosX < 0 || newPosY >= H || newPosY < 0) continue;
                    if (cc[newPosX][newPosY] != 0) continue;
                    queue.add((newPosX << AUX) | newPosY);
                    cc[newPosX][newPosY] = cc[a][b];
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void initArraysDependingOnCCs() {
        Rocket.allyRocketLandingsCcs = new int[ccs + 1];
        Rocket.enemyRocketLandingsCcs = new int[ccs + 1];
        marsInitialKarbo_cc = new double[ccs + 1];
        areaCC = new int[ccs+1];
    }

    private static void computeClear() {
        clear = new int[W][H];
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                AuxMapLocation loc = new AuxMapLocation(x,y);
                for (int d = 0; d < 8; ++d) {
                    AuxMapLocation newLoc = loc.add(d);
                    if (isOnMars(newLoc) && passable[newLoc.x][newLoc.y]) clear[x][y]++;
                }
            }
        }
    }

    private static void computeArees() {
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                if (cc[x][y] > 0) areaCC[cc[x][y]]++;
            }
        }
        for (int i : areaCC) sumaArees += i;
    }

    private static void computeOrbitStuff() {
        OrbitPattern op = GC.gc.orbitPattern();
        int orbitPeriod = (int) op.getPeriod();
        for (int i = 1; i <= 1000; ++i) {
            int time = (int) op.duration(i);
            int arrival = i + time;
            arrivalTime[i] = arrival;
            for (int j = arrival; j <= Math.min(1000, arrival + orbitPeriod); ++j) {
                departTime[j] = i;
            }
        }
        op.delete();
        for (int i = 1; i <= 1000; ++i) {
            int arrival = arrivalTime[i];
            for (int j = i; j >= 1; --j) {
                if (optimArrivalTime[j] == 0 || arrival < optimArrivalTime[j]) optimArrivalTime[j] = arrival;
                else break;
            }
        }
        int arrivalBurstRound = Math.min(MAX_BURST_ROUND, arrivalTime[749]);
        takeoffBurstRound = departTime[arrivalBurstRound];
    }

    private static void constructAsteroidPattern() {
        ArrayList<Integer> aRounds = new ArrayList<>();
        ArrayList<Integer> aCarbo = new ArrayList<>();
        ArrayList<AuxMapLocation> locCarbo = new ArrayList<>();
        AsteroidPattern ap = GC.gc.asteroidPattern();
        for (int i = 1; i <= 1000; ++i) {
            if (ap.hasAsteroid(i)) {
                AsteroidStrike as = ap.asteroid(i);
                aRounds.add(i);
                aCarbo.add((int) as.getKarbonite());
                locCarbo.add(new AuxMapLocation(as.getLocation()));
            }
        }
        ap.delete();
        Karbonite.asteroidRounds = aRounds.toArray(new Integer[0]);
        Karbonite.asteroidKarbo = aCarbo.toArray(new Integer[0]);
        Karbonite.asteroidLocations = locCarbo.toArray(new AuxMapLocation[0]);
    }

    static boolean shouldWaitToLaunchRocket(int round) {
        try {
            if (takeoffBurstRound - round < BURST_WAIT_ROUNDS) return true;
            int arrival = arrivalTime[round];
            return optimArrivalTime[round] < arrival;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean shouldWaitToLaunchRocket(int round, int maxRounds) {
        try {
            int arrival = arrivalTime[round];
            for (int i = round + 1; i <= round + maxRounds; ++i) {
                if (arrivalTime[i] < arrival) return true;
            }
            return false;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isOnMars(AuxMapLocation loc){
        try {
            if (loc.x < 0 || loc.x >= W) return false;
            if (loc.y < 0 || loc.y >= H) return false;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static double[][] computeInitialPriorityKarboMars() {
        try {
            double[][] initialPriorityKarbo = new double[W][H];
            double[][] rocketNear = new double[W][H];
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    double value = marsInitialKarbonite[x][y];
                    boolean addValueToAdj = passable[x][y];
                    addGradually(initialPriorityKarbo, new AuxMapLocation(x,y), DEPTH, value, addValueToAdj, rocketNear);
                }
            }
            return initialPriorityKarbo;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getRocketsInAdjCCs(AuxMapLocation loc) {
        try {
            int ret = 0;
            boolean[] seen_cc = new boolean[ccs + 1];
            for (int d = 0; d < 8; ++d) {
                AuxMapLocation newLoc = loc.add(d);
                if (!isOnMars(newLoc)) continue;
                int comp = cc[newLoc.x][newLoc.y];
                if (comp < 0) continue;
                if (!seen_cc[comp]) ret += Rocket.allyRocketLandingsCcs[comp] + Rocket.enemyRocketLandingsCcs[comp];
                seen_cc[comp] = true;
            }
            return ret;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static void addKarboCC(double[] karbo_cc, AuxMapLocation loc, double value, double[][] rocketNear) {
        try {
            // tot aquest rollo es perque una mina de karbo pot ser que es pugui minar des de mes d'una cc
            boolean[] seen_cc = new boolean[ccs + 1];
            for (int d = 0; d < 8; ++d) {
                AuxMapLocation newLoc = loc.add(d);
                if (!isOnMars(newLoc) || !passable[newLoc.x][newLoc.y]) continue;
                int comp = cc[newLoc.x][newLoc.y];
                if (!seen_cc[comp]) {
                    if (rocketNear[newLoc.x][newLoc.y] == 0 && areaCC[comp] >= 2) karbo_cc[comp] += value;
                }
                seen_cc[comp] = true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void addGradually(double[][] priority, AuxMapLocation initLoc, int depth, double value, boolean addValueToAdj, double[][] rocketNear) {
        try {
            HashSet<Integer> seen = new HashSet<>();
            Queue<AuxMapLocation> queue = new LinkedList<>();
            seen.add(initLoc.x << 6 | initLoc.y);
            queue.offer(initLoc);
            while (!queue.isEmpty()) {
                AuxMapLocation loc = queue.poll();
                for (int d = 0; d < 8; ++d) {
                    AuxMapLocation newLoc = loc.add(d);
                    if (!isOnMars(newLoc) || !passable[newLoc.x][newLoc.y] || (rocketNear != null && rocketNear[newLoc.x][newLoc.y] == 1)) continue;
                    int dist = initLoc.distanceSquaredTo(newLoc);
                    if (dist > depth) continue;
                    if (seen.contains(newLoc.x << 6 | newLoc.y)) continue;
                    seen.add(newLoc.x << 6 | newLoc.y);
                    queue.add(newLoc);
                    if (addValueToAdj || dist > 2) priority[newLoc.x][newLoc.y] += value * (0.8 + 0.2*(depth - dist)/depth);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void putToOneNear(double[][] rocketNear, double[][] rocketAdj, AuxMapLocation initLoc, int depth, boolean ally) {
        try {
            rocketNear[initLoc.x][initLoc.y] = 1;
            rocketAdj[initLoc.x][initLoc.y] = ally?20:10;
            HashSet<Integer> seen = new HashSet<>();
            Queue<AuxMapLocation> queue = new LinkedList<>();
            seen.add(initLoc.x << 6 | initLoc.y);
            queue.offer(initLoc);
            while (!queue.isEmpty()) {
                AuxMapLocation loc = queue.poll();
                for (int d = 0; d < 8; ++d) {
                    AuxMapLocation newLoc = loc.add(d);
                    if (!isOnMars(newLoc) || !passable[newLoc.x][newLoc.y]) continue;
                    int dist = initLoc.distanceSquaredTo(newLoc);
                    if (dist > depth) continue;
                    if (seen.contains(newLoc.x << 6 | newLoc.y)) continue;
                    seen.add(newLoc.x << 6 | newLoc.y);
                    queue.add(newLoc);
                    rocketNear[newLoc.x][newLoc.y] = 1;
                    if (dist <= 2 && ally) rocketAdj[newLoc.x][newLoc.y]++;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void addAsteroids(double[][] priorityKarbo, double[] karbo_cc, int round, double[][] rocketNear) {
        try {
            for (int i = 0; i < Karbonite.asteroidRounds.length; ++i) {
                int round_i = Karbonite.asteroidRounds[i];
                if (round_i - round > 20) return; // no tenim en compte la karbonite que arriba en mes de 20 torns
                AuxMapLocation loc = Karbonite.asteroidLocations[i];
                addGradually(priorityKarbo, loc, DEPTH, Karbonite.asteroidKarbo[i], passable[loc.x][loc.y], rocketNear);
                double value = (double) Karbonite.asteroidKarbo[i] / ((double) getRocketsInAdjCCs(loc) + 1);
                addKarboCC(karbo_cc, loc, value, rocketNear);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static double[][] computeRocketNearAdj(double[][] rocketNear, double[][] rocketAdj) {
        try {
            for (AuxMapLocation loc : Rocket.allyRocketLandingsLocs) {
                putToOneNear(rocketNear, rocketAdj, loc, 2*DEPTH, true);
            }
            for (AuxMapLocation loc: Rocket.enemyRocketLandingsLocs) {
                putToOneNear(rocketNear, rocketAdj, loc, 2*DEPTH, false);
            }
            return rocketNear;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void addUpdatedInitialKarbo(double[] karbo_cc, double[][] rocketNear) {
        try {
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    int rocketsInAdjCCs = getRocketsInAdjCCs(new AuxMapLocation(x,y));
                    double value = marsInitialKarbonite[x][y] / (double)(rocketsInAdjCCs + 1);
                    addKarboCC(karbo_cc, new AuxMapLocation(x,y), value, rocketNear);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static AuxMapLocation bestPlaceForRound(int round) {
        try {
            // primer comptem la karbonite que hi ha a cada cc sense sumar la que esta (adjacent) a casella dominada i dividint pels rockets+1
            // segons aixo escollim cc
            // despres fem priorityKarbo tambe sense sumar la que esta (adjacent) a casella dominada sense dividir pels rockets
            // escollim la posicio concreta aixi
            // aixo serveix per la primera fase
            double[][] rocketNear = new double[W][H];
            double[][] rocketAdj = new double[W][H];
            computeRocketNearAdj(rocketNear, rocketAdj);
            double[] karbo_cc = new double[ccs + 1];
            double[][] priorityKarbo = marsInitialPriorityKarbo.clone();
            addUpdatedInitialKarbo(karbo_cc, rocketNear);
            addAsteroids(priorityKarbo, karbo_cc, round, rocketNear);

            // s'ha d'anar gradualment des de priorityKarbo cap a prioritySecurity
            //double[][] prioritySecurity = new double[W][H];

            if (round < 720) {
                // choose cc
                double best_cc_factor = -1;
                for (int i = 1; i <= ccs; ++i) {
                    double factor_cc = karbo_cc[i] + (double) areaCC[i] / (double) sumaArees;
                    if (factor_cc > best_cc_factor && areaCC[i] > 10) best_cc_factor = factor_cc;
                }

                // find best location
                AuxMapLocation bestLoc = null;
                double best_priority = -Const.INF;
                for (int x = 0; x < W; ++x) {
                    for (int y = 0; y < H; ++y) {
                        if (!passable[x][y]) continue;
                        if (bestLoc == null) bestLoc = new AuxMapLocation(x, y);
                        if (karbo_cc[cc[x][y]] + (double) areaCC[cc[x][y]] / (double) sumaArees < best_cc_factor) continue;
                        double factor_clear = ((double) clear[x][y]) / 8;
                        double priority_xy = factor_clear * (1 + priorityKarbo[x][y]) - 1000 * rocketAdj[x][y];
                        if (rocketNear[x][y] > 1) priority_xy = -rocketNear[x][y];
                        if (priority_xy > best_priority) {
                            best_priority = priority_xy;
                            bestLoc = new AuxMapLocation(x, y);
                        }
                    }
                }
                if (karbo350 == -1) {
                    karbo350 = karbo_cc[cc[bestLoc.x][bestLoc.y]];
                    biggestArea = (double) areaCC[cc[bestLoc.x][bestLoc.y]];
                    biggestAreaPercent = biggestArea / (double) sumaArees;
                }
                return bestLoc;
            }
            else {
                int best_cc = 0;
                double best_priority = -Const.INF;
                for (int i = 1; i <= ccs; ++i) {
                    if (areaCC[i] > areaCC[best_cc]) best_cc = i;
                }
                AuxMapLocation bestLoc = null;
                double[][] rocketSec = computeRocketSec();
                for (int x = 0; x < W; ++x) {
                    for (int y = 0; y < H; ++y) {
                        if (!passable[x][y]) continue;
                        if (bestLoc == null) bestLoc = new AuxMapLocation(x,y);
                        double factor_clear = ((double) clear[x][y]) / 8;
                        double priority = factor_clear * (10+rocketSec[x][y]) - 1000 * rocketAdj[x][y];
                        if (priority > best_priority) {
                             best_priority = priority;
                             bestLoc = new AuxMapLocation(x,y);
                        }
                    }
                }

            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static double[][] computeRocketSec() {
        double[][] rocketSec = new double[W][H];
        for (AuxMapLocation loc : Rocket.allyRocketLandingsLocs) {
            addGradually(rocketSec,loc,DEPTH*2,10, false, null);
        }
        for (AuxMapLocation loc: Rocket.enemyRocketLandingsLocs) {
            addGradually(rocketSec,loc,DEPTH*2,-10, false, null);
        }
        return rocketSec;
    }

}