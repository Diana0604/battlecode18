import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class MarsPlanning{

    private static MarsPlanning instance;

    private static final int AUX = 6;
    private static final int base = 0x3F;
    private static final int DEPTH = 30;
    private static final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};

    static int[][] cc; // va entre [1,ccs]
    private static int ccs;
    private static boolean[][] passable;

    static int W;
    static int H;

    private static int[] arrivalTime = new int[1001];
    private static int[] departTime = new int[1001]; //todo aixo mai es fa servir
    private static int[] optimArrivalTime = new int[1001];

    static int[][] marsInitialKarbonite;
    static double[][] marsInitialPriorityKarbo;
    static double[] marsInitialKarbo_cc;

    static MarsPlanning getInstance(){
        if (instance == null) instance = new MarsPlanning();
        return instance;
    }

    private MarsPlanning(){
        initGame();
    }

    public static void initGame(){
        try {
            PlanetMap planetMap = GC.gc.startingMap(Planet.Mars);
            W = (int) planetMap.getWidth();
            H = (int) planetMap.getHeight();
            // fill cc
            cc = new int[W][H];
            ccs = 0;
            passable = new boolean[W][H];
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    passable[x][y] = planetMap.isPassableTerrainAt(new MapLocation(Planet.Mars, x, y)) != 0;
                    if (!passable[x][y]) cc[x][y] = -1;
                }
            }

            planetMap.delete();
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    if (cc[x][y] == 0) bfs(x, y);
                }
            }
            Rocket.allyRocketLandingsCcs = new int[ccs + 1];
            Rocket.enemyRocketLandingsCcs = new int[ccs + 1];
            marsInitialKarbo_cc = new double[ccs + 1];

            // compute times from earth to mars
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
            for (int i = 1; i <= 1000; ++i) {
                int arrival = arrivalTime[i];
                for (int j = i; j >= 1; --j) {
                    if (optimArrivalTime[j] == 0 || arrival < optimArrivalTime[j]) optimArrivalTime[j] = arrival;
                    else break;
                }
            }

            // construct asteroid pattern
            ArrayList<Integer> aRounds = new ArrayList<>();
            //ArrayList<AsteroidStrike> aStrikes = new ArrayList<>();
            ArrayList<Integer> aCarbo = new ArrayList<>();
            ArrayList<AuxMapLocation> locCarbo = new ArrayList<>();
            AsteroidPattern ap = GC.gc.asteroidPattern();
            for (int i = 1; i <= 1000; ++i) { // round numbers are in [0,1000) or (0,1000]??
                if (ap.hasAsteroid(i)) {
                    AsteroidStrike as = ap.asteroid(i);
                    //aStrikes.add(as);
                    aRounds.add(i);
                    aCarbo.add((int) as.getKarbonite());
                    locCarbo.add(new AuxMapLocation(as.getLocation()));
                }
            }
            Karbonite.asteroidRounds = aRounds.toArray(new Integer[0]);
            //GC.asteroidStrikes = aStrikes.toArray(new AsteroidStrike[0]);
            Karbonite.asteroidKarbo = aCarbo.toArray(new Integer[0]);
            Karbonite.asteroidLocations = locCarbo.toArray(new AuxMapLocation[0]);

            marsInitialKarbonite = Wrapper.getMarsInitialKarbonite();
            marsInitialPriorityKarbo = computeInitialPriorityKarboMars();
        }
        catch(Exception e) {
            e.printStackTrace();
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

    static boolean shouldWaitToLaunchRocket(int round) {
        try {
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
        double[][] initialPriorityKarbo = new double[W][H];
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                double value = marsInitialKarbonite[x][y];
                boolean addValueToAdj = passable[x][y];
                addGradually(initialPriorityKarbo, new AuxMapLocation(x,y), DEPTH, value, addValueToAdj);
            }
        }
        return initialPriorityKarbo;
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

    private static void addKarboCC(double[] karbo_cc, AuxMapLocation loc, double value) {
        try {
            // tot aquest rollo es perque una mina de karbo pot ser que es pugui minar des de mes d'una cc
            boolean[] seen_cc = new boolean[ccs + 1];
            for (int d = 0; d < 8; ++d) {
                AuxMapLocation newLoc = loc.add(d);
                if (!isOnMars(newLoc) || !passable[newLoc.x][newLoc.y]) continue;
                int comp = cc[newLoc.x][newLoc.y];
                if (!seen_cc[comp]) karbo_cc[comp] += value;
                seen_cc[comp] = true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // basicament aquesta funcio suma value a la matriu priority comencant per initLoc va decaient gradualment fins que
    // a distancia^2=depth suma zero
    // els bools del final son per decidir si es suma o no a la posicio initLoc i a les seves adjacents
    private static void addGradually(double[][] priority, AuxMapLocation initLoc, int depth, double value, boolean addValueToAdj) {
        try {
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
                    if (addValueToAdj || dist > 2) priority[newLoc.x][newLoc.y] += value * (depth - dist) / depth;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void putToOneNear(double[][] nearRocket, AuxMapLocation initLoc, int depth, double centerValue) {
        try {
            nearRocket[initLoc.x][initLoc.y] = centerValue;
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
                    if (nearRocket[newLoc.x][newLoc.y] == 0) nearRocket[newLoc.x][newLoc.y] = 1;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void addAsteroids(double[][] priorityKarbo, double[] karbo_cc, int round) {
        for (int i = 0; i < Karbonite.asteroidRounds.length; ++i) {
            int round_i = Karbonite.asteroidRounds[i];
            if (round_i - round > 20) return; // no tenim en compte la karbonite que arriba en mes de 20 torns
            AuxMapLocation loc = Karbonite.asteroidLocations[i];
            int rocketsInAdjCCs = getRocketsInAdjCCs(loc);
            double value = (double) Karbonite.asteroidKarbo[i] / ((double) rocketsInAdjCCs + 1);
            boolean addValueToAdj = passable[loc.x][loc.y]; // si la karbonite esta a una paret no volem que el
            // rocket estigui tocant-la. Si la karbonite no esta en paret el rocket no molesta tant
            // no volem mai sumar la prioritat a la casella central (on esta la karbonite)
            addGradually(priorityKarbo, loc, DEPTH, value, addValueToAdj);
            addKarboCC(karbo_cc, loc, value);
        }
    }

    private static double[][] computeRocketNear() {
        double[][] rocketNear = new double[W][H];
        for (AuxMapLocation loc : Rocket.allyRocketLandingsLocs) {
            putToOneNear(rocketNear, loc, 2*DEPTH, 3);
        }
        for (AuxMapLocation loc: Rocket.enemyRocketLandingsLocs) {
            putToOneNear(rocketNear, loc, 2*DEPTH, 2);
        }
        return rocketNear;
    }

    private static void addUpdatedInitialKarbo(double[][] priorityKarbo, double[] karbo_cc) {
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                int rocketsInAdjCCs = getRocketsInAdjCCs(new AuxMapLocation(x,y));
                priorityKarbo[x][y] = marsInitialPriorityKarbo[x][y] / (double)(rocketsInAdjCCs + 1);
                double value = marsInitialKarbonite[x][y];
                addKarboCC(karbo_cc, new AuxMapLocation(x,y), value);
            }
        }
    }

    static AuxMapLocation bestPlaceForRound(int round) {
        try {
            double[][] priorityKarbo = new double[W][H]; // prioritat: te en compte karbonite propera
            double[] karbo_cc = new double[ccs + 1]; // total de karbonite de cada cc dividida entre els coets d'aquella cc +1
            addUpdatedInitialKarbo(priorityKarbo, karbo_cc);
            addAsteroids(priorityKarbo, karbo_cc, round);

            double[][] rocketNear; // 0 si buit, 1 si hi ha un coet a prop, 2 si hi ha un coet enemic, 3 si hi ha un coet aliat
            rocketNear = computeRocketNear();

            // find best location
            AuxMapLocation bestLoc = null;
            double best_priority = -Const.INF;
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    if (!passable[x][y]) continue;
                    if (bestLoc == null) bestLoc = new AuxMapLocation(x, y);
                    double priority_xy = priorityKarbo[x][y]*(1-rocketNear[x][y]) + 1000*karbo_cc[cc[x][y]];
                    if (rocketNear[x][y] > 1) priority_xy = -rocketNear[x][y];
                    // prioritzem karbo de la cc, i despres escollim el lloc dins de la cc
                    if (priority_xy > best_priority) {
                        best_priority = priority_xy;
                        bestLoc = new AuxMapLocation(x, y);
                    }
                }
            }
            return bestLoc;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}