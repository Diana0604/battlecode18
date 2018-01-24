import bc.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Ivan on 1/20/2018.
 */
public class WorkerUtil {

    private final double sqrt2 = Math.sqrt(2);
    private final double[] dists = {1, sqrt2, 1, sqrt2, 1, sqrt2, 1, sqrt2};

    static int[][] workerActions;
    static int approxMapValue = 0;
    static int min_nb_workers = 3;
    static int extra_workers;

    static boolean[] connectivityArray;
    static int[][] workerAreas;
    static final int workerRadius = 16;

    static AuxMapLocation bestFactoryLocation;

    static double worker_value = 45;

    final static double decrease_rate = 0.90;


    public static void initGame(){
        workerActions = new int[Mapa.W][Mapa.H];
    }

    public static void initTurn(){
        fillWorkerActions();
    }


    //emplena la matriu worker actions
    //worker actions[i][j] = quantes accions de worker hi ha a la posicio (i,j)
    //also puts in the map where workers are close
    static void fillWorkerActions(){
        try {
            workerAreas = new int[Mapa.W][Mapa.H];
            extra_workers = 0;
            for (int i = 0; i < Mapa.W; ++i) {
                for (int j = 0; j < Mapa.H; ++j) {
                    AuxUnit unit = new AuxMapLocation(i,j).getUnit(true);
                    if (unit == null) {
                        workerActions[i][j] = (Karbonite.karboMap[i][j] + (Units.harvestingPower - 1)) / Units.harvestingPower;
                        continue;
                    }
                    if (unit.getType() == UnitType.Factory || unit.getType() == UnitType.Rocket) {
                        int dif = Units.getMaxHealth(unit.getType()) - unit.getHealth();
                        if (dif > 0) {
                            if (unit.isBlueprint())
                                workerActions[i][j] = (dif + Units.buildingPower - 1) / Units.buildingPower;
                            else workerActions[i][j] = (dif + Units.repairingPower - 1) / Units.repairingPower;
                        }
                    } else {
                        if (unit.getType() == UnitType.Worker){
                            for (int k = 0; k < Vision.Mx[workerRadius].length; ++k){
                                AuxMapLocation newLoc = new AuxMapLocation(i,j).add(new AuxMapLocation(Vision.Mx[workerRadius][k], Vision.My[workerRadius][k]));
                                if (newLoc.isOnMap()) ++workerAreas[newLoc.x][newLoc.y];
                            }
                        }
                        workerActions[i][j] = (Karbonite.karboMap[i][j] + (Units.harvestingPower - 1)) / Units.harvestingPower;
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();;
        }
    }

    static void preComputeConnectivity(){
        connectivityArray = new boolean[(1 << 9)-1];
        for (int i = 0; i < (1 << 9) - 1; ++i){
            connectivityArray[i] = computeConnectivity(i);
        }

        //for (int i = 0; i < 32; ++i) System.out.println(connectivityArray[i]);

    }

    static boolean computeConnectivity(int s){
        Queue<Integer> q = new LinkedList<>();
        for (int i = 0; i < 8; ++i) {
            if (((s >> i)&1) > 0){
                q.add(i);
                s = s & (~(1 << i));
                break;
            }
        }
        while (!q.isEmpty()){
            int t = q.poll();
            int x = 2;
            if (t%2 == 1) x = 1;
            for (int i = -x; i <= x; ++i){
                int newT = (t+8-i)%8;
                if (((s >> newT)&1) > 0){
                    q.add(newT);
                    s = s &(~(1 << newT));
                }
            }
        }
        return s == 0;
    }

    /*
    static AuxMapLocation getBestFactoryLocation(){
        bestFactoryLocation = null;

        HashSet<Integer> locations = new HashSet<Integer>();
        for (int i = 0; i < GC.myUnits.length; ++i){
            if (GC.myUnits[i].getType() != UnitType.Worker) continue;
            AuxMapLocation loc = GC.myUnits[i].getMapLocation();
            if (loc != null){
                locations.add(Utils.encode(loc.x, loc.y));
            }
        }

    }*/

    //quants workers adjacents hi ha a la posicio mLoc
    static int getAdjacentWorkers(AuxMapLocation mLoc){
        try {
            int ans = 0;
            AuxUnit[] units = Wrapper.senseUnits(mLoc, 2, true);
            for (AuxUnit unit : units) if (unit.getType() == UnitType.Worker) ++ans;
            return ans;
        }catch(Exception e) {
            e.printStackTrace();;
            return 0;
        }
    }

    //retorna la direccio on hi ha mes karbo (nomes adjacent)
    static int getMostKarboLocation(AuxMapLocation loc){
        try {
            int bestDir = 0;
            int bestKarbo = -1;
            for (int i = 0; i < 9; ++i) {
                AuxMapLocation newLoc = loc.add(i);
                if (newLoc.isOnMap() && Karbonite.karboMap[newLoc.x][newLoc.y] > bestKarbo && !buildingAt(newLoc)) {
                    bestKarbo = Karbonite.karboMap[newLoc.x][newLoc.y];
                    bestDir = i;
                }
            }
            return bestDir;
        }catch(Exception e) {
            e.printStackTrace();;
            return 0;
        }
    }

    //si hi ha un building a la posicio (per no tenir en compte la karbonite)
    static boolean buildingAt(AuxMapLocation loc) {
        try {
            AuxUnit u = loc.getUnit(true);
            if (u == null) u = loc.getUnit(false);
            if (u == null) return false;
            return (u.getType() == UnitType.Factory || u.getType() == UnitType.Rocket);
        } catch (Exception e) {
            e.printStackTrace();;
            return false;
        }
    }

    //compta les workerActions/#workers en un radi r (30)
    static int getWorkerActions(AuxMapLocation loc, int r){
        try {
            int ans = 0;
            int workers = 0;
            for (int i = 0; i < Vision.Mx[r].length; ++i) {
                AuxMapLocation mLoc = new AuxMapLocation(loc.x + Vision.Mx[r][i], loc.y + Vision.My[r][i]);
                if (!mLoc.isOnMap()) continue;
                double d = mLoc.distanceBFSTo(loc);
                if (d * d > r) continue;
                ans += workerActions[mLoc.x][mLoc.y];
                AuxUnit unit = mLoc.getUnit(true);
                if (unit == null) continue;
                if (unit.getType() == UnitType.Worker) ++workers;
            }
            return ans / (workers + 1 + extra_workers);
        }catch(Exception e) {
            e.printStackTrace();;
            return 0;
        }
    }


    static void computeApproxMapValue() {
        try {
            /*int AUX = 6;
            int AUX2 = 12;
            double distFactor = 100;
            int base = 0x3F;*/
            approxMapValue = 0;
            VecUnit v = Mapa.planetMap.getInitial_units();

            //Queue<Integer> queue = new LinkedList<>();

            ArrayList<AuxMapLocation> initialPositions = new ArrayList<>();

            for (int i = 0; i < v.size(); ++i) {
                Location loc = v.get(i).location();
                if (!loc.isInGarrison()) {
                    MapLocation mLoc = loc.mapLocation();
                    int x = mLoc.getX();
                    int y = mLoc.getY();
                    AuxMapLocation mloc = new AuxMapLocation(x, y);
                    initialPositions.add(mloc);
                    if (i % 2 == 0) ++min_nb_workers;
                }
            }

            for (int i = 0; i < Mapa.W; ++i) {
                for (int j = 0; j < Mapa.H; ++j) {
                    AuxMapLocation mloc = new AuxMapLocation(i, j);
                    double mindist = 1000000;
                    for (int t = 0; t < initialPositions.size(); ++t) {
                        mindist = Math.min(mindist, initialPositions.get(t).distanceBFSTo(mloc));
                    }
                    approxMapValue += Karbonite.karboMap[i][j] * Math.pow(decrease_rate, mindist);
                }
            }

            approxMapValue /= 2;

            min_nb_workers = (int)Math.max(min_nb_workers, approxMapValue / worker_value);

            //System.out.println(approxMapValue + " " + min_nb_workers);


            preComputeConnectivity();
            /*
            while(queue.size() > 0){
                int data = queue.poll();
                int myPosX = (data >> AUX)&base;
                int myPosY = data&base;
                double dist = ((double)(data >> AUX2))/distFactor;
                AuxMapLocation myLoc = new AuxMapLocation(myPosX, myPosY);
                approxMapValue += GC.karboMap[myLoc.x][myLoc.y]*Math.pow(decrease_rate, dist);
                for(int i = 0; i < 8; ++i){
                    AuxMapLocation newLoc = myLoc.add(i);
                    double newDist = dist + dists[i];
                    int parsedDist = (int)Math.round(distFactor*newDist);
                    if(!newLoc.isOnMap()) continue;
                    if(newDist < visited[newLoc.x][newLoc.y]) {
                        if(GC.passable[newLoc.x][newLoc.y]) queue.add((((parsedDist << AUX) | newLoc.x) << AUX) | newLoc.y);
                        visited[newLoc.x][newLoc.y] = newDist;
                    }
                }
            }
            */

            //return approxMapValue;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean getConnectivity(AuxMapLocation loc){
        int a = 0;
        for (int i = 0; i < 8; ++i){
            AuxMapLocation newLoc = loc.add(i);
            if (newLoc.isAccessible()){
                a = a|(1 << i);
            }
            else if (newLoc.isOnMap()){
                AuxUnit unit = newLoc.getUnit(true); //TODO aixo no es pot fer simplement newLoc.getUnit()?
                if (unit == null) unit = newLoc.getUnit(false);
                if (unit != null){
                    if (unit.getType() != UnitType.Factory && unit.getType() != UnitType.Rocket) a = a|(1 << i);
                }
            }
        }
        return connectivityArray[a];
    }

    static int getBestFactoryLocation(AuxUnit unit){
        int ans = 8;
        FactoryData bestFactory = null;
        for (int i = 0; i < 8; ++i) {
            if (Wrapper.canPlaceBlueprint(unit, UnitType.Factory, i)){
                FactoryData fd = new FactoryData(unit.getMapLocation().add(i));
                if (fd.isBetter(bestFactory)){
                    bestFactory = fd;
                    ans = i;
                }
            }
        }
        return ans;
    }

    static class FactoryData{
        AuxMapLocation loc;
        boolean connectivity;
        double distToWalls;
        double workersNear;

        public FactoryData(AuxMapLocation _loc){
            loc = _loc;
            if (!loc.isOnMap()) return;
            connectivity = getConnectivity(loc);
            distToWalls = Pathfinder.distToWalls[loc.x][loc.y];
            if (distToWalls > 3) distToWalls = 3;
            workersNear = workerAreas[loc.x][loc.y];
        }

        public boolean isBetter(FactoryData B){
            if (B == null) return true;
            if (connectivity && !B.connectivity) return true;
            if (B.connectivity && !connectivity) return false;
            if (distToWalls > B.distToWalls) return true;
            if (B.distToWalls < distToWalls) return false;
            return workersNear > B.workersNear;
        }

    }

}
