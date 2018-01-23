import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class MarsPlanning{

    static MarsPlanning instance;

    private final int AUX = 6;
    private final int base = 0x3F;
    private final int DEPTH = 15;
    private final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    private final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};

    PlanetMap map;
    int W;
    int H;
    int[][] cc; // va entre [1,ccs]
    int ccs;
    int orbitPeriod;
    boolean[][] passable;

    int[] arrivalTime = new int[1001];
    int[] departTime = new int[1001];
    int[] optimArrivalTime = new int[1001];

    static MarsPlanning getInstance(){
        if (instance == null) instance = new MarsPlanning();
        return instance;
    }

    private MarsPlanning(){
        init();
    }

    private void init(){
        map = Data.gc.startingMap(Planet.Mars);
        W = (int)map.getWidth();
        H = (int)map.getHeight();

        // fill cc
        cc = new int[W][H];
        ccs = 0;
        passable = new boolean[W][H];
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                passable[x][y] = map.isPassableTerrainAt(new MapLocation(Planet.Mars, x, y)) != 0;
                if (!passable[x][y]) cc[x][y] = -1;
            }
        }
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                if (cc[x][y] != 0) continue;
                else bfs(x, y);
            }
        }
        Rocket.rocketLandingsCcs = new int[ccs+1];

        // compute times from earth to mars
        OrbitPattern op = Data.gc.orbitPattern();
        //System.out.println("orbites: "+ op.getCenter() + " " + op.getAmplitude() + " " + op.getPeriod());
        orbitPeriod = (int)op.getPeriod();
        for (int i = 1; i <= 1000; ++i) {
            int time = (int)op.duration(i);
            int arrival = i + time;
            arrivalTime[i] = arrival;
            for (int j = arrival; j <= Math.min(1000, arrival+orbitPeriod); ++j) {
                departTime[j] = i;
            }
        }
        for (int i = 1; i <= 1000; ++i) {
            int arrival = arrivalTime[i];
            for (int j = i; j >= 1; --j) {
                if(optimArrivalTime[j] == 0 || arrival < optimArrivalTime[j]) optimArrivalTime[j] = arrival;
                else break;
            }
        }
        for (int i = 1; i <= 1000; ++i) {
            //if (gc.planet() == Planet.Mars && gc.team() == Team.Blue) System.out.println("trip on round " + i + " arrives at " + arrivalTime[i]);
            //if (gc.planet() == Planet.Mars && gc.team() == Team.Blue) System.out.println("ready at round "+i+" means arriving at "+ optimArrivalTime[i]);
        }
        for (int i = 1; i <= 1000; ++i) {
            //if (gc.planet() == Planet.Mars && gc.team() == Team.Blue) System.out.println("to arrive at round "+i+" better depart at "+departTime[i]);
        }

        // construct asteroid pattern
        ArrayList<Integer> aRounds = new ArrayList<>();
        //ArrayList<AsteroidStrike> aStrikes = new ArrayList<>();
        ArrayList<Integer> aCarbo = new ArrayList<>();
        ArrayList<AuxMapLocation> locCarbo = new ArrayList<>();
        AsteroidPattern ap = Data.gc.asteroidPattern();
        for (int i = 1; i <= 1000; ++i){ // round numbers are in [0,1000) or (0,1000]??
            if (ap.hasAsteroid(i)) {
                AsteroidStrike as = ap.asteroid(i);
                //aStrikes.add(as);
                aRounds.add(i);
                aCarbo.add((int)as.getKarbonite());
                locCarbo.add(new AuxMapLocation(as.getLocation()));
            }
        }
        Data.asteroidRounds = aRounds.toArray(new Integer[0]);
        //Data.asteroidStrikes = aStrikes.toArray(new AsteroidStrike[0]);
        Data.asteroidCarbo = aCarbo.toArray(new Integer[0]);
        Data.asteroidLocations = locCarbo.toArray(new AuxMapLocation[0]);
    }

    private void bfs(int a, int b){
        Queue<Integer> queue = new LinkedList<>();
        queue.offer((a << AUX) | b);
        cc[a][b] = ++ccs;

        while(!queue.isEmpty()){
            int data = queue.poll();
            int myPosX = (data >> AUX)&base;
            int myPosY = data&base;
            for(int i = 0; i < X.length; ++i){
                int newPosX = myPosX + X[i];
                int newPosY = myPosY + Y[i];
                if(newPosX >= W || newPosX < 0 || newPosY >= H || newPosY < 0) continue;
                if(cc[newPosX][newPosY] != 0) continue;
                queue.add((newPosX << AUX) | newPosY);
                cc[newPosX][newPosY] = cc[a][b];
            }
        }
    }

    public boolean shouldWaitToLaunchRocket(int round) {
        int arrival = arrivalTime[round];
        return optimArrivalTime[round] < arrival;
    }

    public boolean shouldWaitToLaunchRocket(int round, int maxRounds) {
        int arrival = arrivalTime[round];
        for (int i = round+1; i <= round+maxRounds; ++i) {
            if(arrivalTime[i] < arrival) return true;
        }
        return false;
    }

    boolean isOnMars (AuxMapLocation loc){
        if (loc.x < 0 || loc.x >= W) return false;
        if (loc.y < 0 || loc.y >= H) return false;
        return true;
    }

    private int getRocketsInAdjCCs(AuxMapLocation loc) {
        int ret = 0;
        boolean[] seen_cc = new boolean[ccs+1];
        for (int d = 0; d < 8; ++d) {
            AuxMapLocation newLoc = loc.add(d);
            if (!isOnMars(newLoc)) continue;
            int comp = cc[newLoc.x][newLoc.y];
            if (comp < 0) continue;
            if (!seen_cc[comp]) ret += Rocket.rocketLandingsCcs[comp];
            seen_cc[comp] = true;
        }
        return ret;
    }

    private void addKarboCC(double[] karbo_cc, AuxMapLocation loc, double value) {
        boolean[] seen_cc = new boolean[ccs+1];
        for (int d = 0; d < 8; ++d) {
            AuxMapLocation newLoc = loc.add(d);
            if (!isOnMars(newLoc)) continue;
            int comp = cc[newLoc.x][newLoc.y];
            if (comp < 0) continue;
            if (!seen_cc[comp]) karbo_cc[comp] += value;
            seen_cc[comp] = true;
        }
    }

    private void addPriority(double[][] priority, AuxMapLocation initLoc, int depth, double value, boolean addValueToAdj, boolean addValueToCenter) {
        if (addValueToCenter) priority[initLoc.x][initLoc.y] += value;
        HashSet<Integer> seen = new HashSet<>();
        Queue<AuxMapLocation> queue = new LinkedList<>();
        seen.add(initLoc.x << 6 | initLoc.y);
        queue.offer(initLoc);
        while (!queue.isEmpty()) {
            //System.out.print(queue.size());
            AuxMapLocation loc = queue.poll();
            //System.out.println(" " + queue.size());
            for (int d = 0; d < 8; ++d) {
                AuxMapLocation newLoc = loc.add(d);
                if (!isOnMars(newLoc)) continue;
                if (!passable[newLoc.x][newLoc.y]) continue;
                int dist = initLoc.distanceSquaredTo(newLoc);
                if (dist > depth) continue;
                if (seen.contains(newLoc.x << 6 | newLoc.y)) continue;
                seen.add(newLoc.x << 6 | newLoc.y);
                queue.add(newLoc);
                if (addValueToAdj || dist > 2) priority[newLoc.x][newLoc.y] += value*(depth-dist)/depth;
            }
        }
    }

    public AuxMapLocation bestPlaceForRound(int round) {
        /**
         * Per cada casella amb karbonite hauria de fer un bfs al voltant, amb una profunditat maxima
         * A cada posicio de les que passi el bfs li dono una prioritat. Si la casella inicial era habitable, començo
         * a donar prioritat a partir de les adjacents. Si la casella inicial no era habitable començo a donar prioritat
         * a les que estan a distancia 2
         * Per cada coet que ha caigut, redueixo la prioritat de les caselles al seu voltant. Tambe es pot fer amb bfs
         * Al comptar la karbonite he de dividir-la entre el nombre de coets que han caigut a la seva cc (si toca mes
         * d'una cc, compto els que han caigut a les dues)
         *
         */

        double[][] priority = new double[W][H];
        double[] karbo_cc = new double[ccs+1];
        for (int i = 0; i < Data.asteroidRounds.length; ++i) {
            int round_i = Data.asteroidRounds[i];
            if (round_i-round > 50) break;
            AuxMapLocation loc = Data.asteroidLocations[i];
            int rocketsInAdjCCs = getRocketsInAdjCCs(loc);
            double value = (double)Data.asteroidCarbo[i] / ((double)rocketsInAdjCCs+1);
            boolean addValueToAdj = passable[loc.x][loc.y];
            addPriority(priority, loc, DEPTH, value, addValueToAdj, false);
            addKarboCC(karbo_cc, loc, 100*value);
        }
        for (AuxMapLocation loc:Rocket.rocketLandingsLocs) {
            double value = priority[loc.x][loc.y];
            addPriority(priority, loc, DEPTH*2, -value, true, true);
            priority[loc.x][loc.y] = -10000;
        }

        AuxMapLocation bestLoc = null;
        double best_priority = 0;
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                if (!passable[x][y]) continue;
                if (bestLoc == null) bestLoc = new AuxMapLocation(x, y);
                double priority_xy = priority[x][y] + karbo_cc[cc[x][y]];
                if (priority_xy > best_priority){
                    best_priority = priority_xy;
                    bestLoc = new AuxMapLocation(x, y);
                }
            }
        }
        return bestLoc;
    }
    
}