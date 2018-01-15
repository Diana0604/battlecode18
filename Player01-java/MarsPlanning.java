import bc.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MarsPlanning{

    static MarsPlanning instance;
    static GameController gc;

    private final int AUX = 6;
    private final int base = 0x3F;
    private final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    private final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};

    private PlanetMap map;
    int W;
    int H;
    int[][] cc;
    int ccs;
    int orbitPeriod;

    Integer[] asteroidRounds;
    AsteroidStrike[] asteroidStrikes;

    int[] arrivalTime = new int[1001];
    int[] departTime = new int[1001];
    int[] optimArrivalTime = new int[1001];

    public static void initialize(GameController _gc){
        gc = _gc;
    }

    static MarsPlanning getInstance(){
        if (instance == null) instance = new MarsPlanning();
        return instance;
    }

    private MarsPlanning(){
        gc = UnitManager.gc;
        init();
    }

    private void init(){
        map = gc.startingMap(Planet.Mars);
        W = (int)map.getWidth();
        H = (int)map.getHeight();

        // fill cc
        cc = new int[W][H];
        ccs = 0;
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                if (cc[x][y] != 0) continue;
                if (map.isPassableTerrainAt(new MapLocation(Planet.Mars, x, y)) == 0) cc[x][y] = -1;
                else bfs(x, y);
            }
        }

        // compute times from earth to mars
        OrbitPattern op = gc.orbitPattern();
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
        ArrayList<AsteroidStrike> aStrikes = new ArrayList<>();
        AsteroidPattern ap = gc.asteroidPattern();
        for (int i = 1; i <= 1000; ++i){ // round numbers are in [0,1000) or (0,1000]??
            if (ap.hasAsteroid(i)) {
                aStrikes.add(ap.asteroid(i));
                aRounds.add(i);
            }
        }
        asteroidRounds = aRounds.toArray(new Integer[0]);
        asteroidStrikes = aStrikes.toArray(new AsteroidStrike[0]);

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
                if(map.isPassableTerrainAt(new MapLocation(Planet.Mars, newPosX, newPosY)) != 0) {
                    queue.add((newPosX << AUX) | newPosY);
                    cc[newPosX][newPosY] = cc[a][b];
                }
                else cc[newPosX][newPosY] = -1;
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
    
}