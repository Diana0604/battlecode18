import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class MarsPlanning{

    private static MarsPlanning instance;

    private static final int AUX = 6;
    private static final int base = 0x3F;
    private static final int DEPTH = 15;
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
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    if (cc[x][y] == 0) bfs(x, y);
                }
            }
            Rocket.rocketLandingsCcs = new int[ccs + 1];

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

    private static int getRocketsInAdjCCs(AuxMapLocation loc) {
        try {
            int ret = 0;
            boolean[] seen_cc = new boolean[ccs + 1];
            for (int d = 0; d < 8; ++d) {
                AuxMapLocation newLoc = loc.add(d);
                if (!isOnMars(newLoc)) continue;
                int comp = cc[newLoc.x][newLoc.y];
                if (comp < 0) continue;
                if (!seen_cc[comp]) ret += Rocket.rocketLandingsCcs[comp];
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
                if (!isOnMars(newLoc)) continue;
                int comp = cc[newLoc.x][newLoc.y];
                if (comp < 0) continue;
                if (!seen_cc[comp]) karbo_cc[comp] += value;
                seen_cc[comp] = true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void addPriority(double[][] priority, AuxMapLocation initLoc, int depth, double value, boolean addValueToAdj, boolean addValueToCenter) {
        try {
            if (addValueToCenter) priority[initLoc.x][initLoc.y] += value;
            int n = Vision.Mx[depth].length;
            for (int i = 0; i < n; ++i) {
                AuxMapLocation newLoc = initLoc.add(new AuxMapLocation(Vision.Mx[depth][i],Vision.My[depth][i]));
                double dist = initLoc.distanceBFSTo(newLoc);
                if (isOnMars(newLoc) && passable[newLoc.x][newLoc.y] && dist <= depth) {
                    if (addValueToAdj || dist > 1.8) priority[newLoc.x][newLoc.y] += value * (depth - dist) / depth;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    static AuxMapLocation bestPlaceForRound(int round) {
        try {
            double[][] priority = new double[W][H]; // prioritat: te en compte karbonite propera i altres coets propers
            double[] karbo_cc = new double[ccs + 1]; // total de karbonite de cada cc dividida entre els coets d'aquella cc +1
            for (int i = 0; i < Karbonite.asteroidRounds.length; ++i) {
                int round_i = Karbonite.asteroidRounds[i];
                if (round_i - round > 50) break; // no tenim en compte la karbonite que arriba en mes de 50 torns
                AuxMapLocation loc = Karbonite.asteroidLocations[i];
                int rocketsInAdjCCs = getRocketsInAdjCCs(loc);
                double value = (double) Karbonite.asteroidKarbo[i] / ((double) rocketsInAdjCCs + 1);
                boolean addValueToAdj = passable[loc.x][loc.y]; // si la karbonite esta a una paret no volem que el
                // rocket estigui tocant-la. Si la karbonite no esta en paret el rocket no molesta tant
                // no volem mai sumar la prioritat a la casella central (on esta la karbonite)
                addPriority(priority, loc, DEPTH, value, addValueToAdj, false);
                addKarboCC(karbo_cc, loc, value);
            }
            for (AuxMapLocation loc : Rocket.rocketLandingsLocs) {
                double value = priority[loc.x][loc.y]; // el rocket resta exactament la prioritat que hi ha a la casella on cau
                // pero ho fa en el doble de radi, per si no estava al mig del cluster de karbonite abarcar-lo tot igualment
                // el rocket tambe resta la prioritat a la casella central i a les adjacents
                addPriority(priority, loc, DEPTH * 2, -value, true, true);
                priority[loc.x][loc.y] = -10000; // extra per si de cas
            }

            AuxMapLocation bestLoc = null;
            double best_priority = 0;
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    if (!passable[x][y]) continue;
                    if (bestLoc == null) bestLoc = new AuxMapLocation(x, y);
                    double priority_xy = priority[x][y] + 100*karbo_cc[cc[x][y]];
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