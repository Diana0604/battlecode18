import bc.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Ivan on 1/20/2018.
 */
public class WorkerUtil {

    static int[][] workerActions;
    static int[][] workerActionsExpanded;
    static int approxMapValue = 0;
    static int min_nb_workers = 2;
    static int min_nb_workers1 = 2;
    static int extra_workers;

    static boolean[] connectivityArray;
    static int[][] workerAreas;
    static final int workerRadius = 16;

    static int[][] workersDeployed;
    static ArrayList<AuxMapLocation> importantLocations;

    static AuxMapLocation bestFactoryLocation;

    static double worker_value1 = 63, worker_value = 70;

    final static double decrease_rate = 0.95;
    final static int MIN_DIST = 4;

    static int workerCont;
    static int workersCreated;

    static boolean safe;
    static boolean hasReplicated;
    static int totalKarboCollected;

    static double minSafeTurns;

    static boolean closeFactory = true;


    public static void initTurn(){
        try {
            workerCont = 0;
            fillWorkerActions();
            workersDeployed = new int[Mapa.W][Mapa.H];
            hasReplicated = false;
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    static void putAction(AuxMapLocation m, int val, int factor){
        try {
            workerActions[m.x][m.y] = val;
            for (int i = 0; i < 9; ++i){
                AuxMapLocation newLoc = m.add(i);
                if (newLoc.isOnMap() && newLoc.isPassable()){
                    workerActionsExpanded[newLoc.x][newLoc.y] += factor*val;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //emplena la matriu worker actions
    //worker actions[i][j] = quantes accions de worker hi ha a la posicio (i,j)
    //also puts in the map where workers are close
    static void fillWorkerActions(){
        try {
            workerAreas = new int[Mapa.W][Mapa.H];
            workerActionsExpanded = new int[Mapa.W][Mapa.H];
            importantLocations = new ArrayList<>();
            extra_workers = 0;
            for (int i = 0; i < Mapa.W; ++i) {
                for (int j = 0; j < Mapa.H; ++j) {
                    AuxMapLocation currentLoc = new AuxMapLocation(i,j);
                    AuxUnit unit = currentLoc.getUnit(true);
                    if (unit == null) {
                        putAction(currentLoc, (Karbonite.karboMap[i][j] + (Units.harvestingPower - 1)) / Units.harvestingPower, 1);
                        continue;
                    }
                    if (unit.getType() == UnitType.Factory || unit.getType() == UnitType.Rocket) {
                        int dif = Units.getMaxHealth(unit.getType()) - unit.getHealth();
                        if (dif > 0) {
                            int factor = 1;
                            if (Danger.enemySeen) factor = 2;
                            if (!unit.isBuilt()) putAction(currentLoc, (dif + Units.buildingPower - 1) / Units.buildingPower, factor);

                            else putAction(currentLoc, (dif + Units.repairingPower - 1) / Units.repairingPower, 1);
                        }
                    } else {
                        if (unit.getType() == UnitType.Worker){
                            for (int k = 0; k < Vision.Mx[workerRadius].length; ++k){
                                AuxMapLocation newLoc = currentLoc.add(new AuxMapLocation(Vision.Mx[workerRadius][k], Vision.My[workerRadius][k]));
                                if (newLoc.isOnMap()) ++workerAreas[newLoc.x][newLoc.y];
                            }
                        }
                        putAction(currentLoc, (Karbonite.karboMap[i][j] + Units.harvestingPower - 1) / Units.harvestingPower, 1);
                    }
                }
            }

            for (int i = 0; i < Mapa.W; ++i) {
                for (int j = 0; j < Mapa.H; ++j) {
                    if (workerActions[i][j] > 0) importantLocations.add(new AuxMapLocation(i,j));
                }
            }
            //System.out.println(importantLocations.size());
        }catch(Exception e) {
            e.printStackTrace();;
        }
    }

    static int difLoc(AuxMapLocation loc){
        int dist1 = 100000, dist2 = 100000;
        for (int i = 0; i < Utils.startingLocations.size(); ++i){
            dist1 = Math.min(dist1, loc.distanceBFSTo(Utils.startingLocations.get(i)));
        }
        for (int i = 0; i < Utils.enemyStartingLocations.size(); ++i){
            dist2 = Math.min(dist2, loc.distanceBFSTo(Utils.enemyStartingLocations.get(i)));
        }

        return dist2 - dist1;
    }

    static boolean isSafe(AuxMapLocation loc){
        try {
            int dist1 = 100000, dist2 = 100000;
            for (int i = 0; i < Utils.startingLocations.size(); ++i){
                dist1 = Math.min(dist1, loc.distanceBFSTo(Utils.startingLocations.get(i)));
            }
            for (int i = 0; i < Utils.enemyStartingLocations.size(); ++i){
                dist2 = Math.min(dist2, loc.distanceBFSTo(Utils.enemyStartingLocations.get(i)));
            }

            return (dist1 <= dist2);
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    static boolean isCloseToFactory(AuxMapLocation loc){
        try {
            AuxUnit[] closeUnits = Wrapper.senseUnits(loc, 16, true);
            for (int i = 0; i < closeUnits.length; ++i){
                if (closeUnits[i].getType() == UnitType.Factory) return true;
            }
            return false;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    static int value(AuxUnit worker){
        try {
            int val = 0;
            int mindist = 10000;
            for (int i = 0; i < Utils.enemyStartingLocations.size(); ++i){
                mindist = Math.min(mindist, worker.getMapLocation().distanceBFSTo(Utils.enemyStartingLocations.get(i)));
            }

            if (getConnectivity(worker.getMapLocation())) val += (1 << 21);
            if (isSafe(worker.getMapLocation())){
                val += (1 << 20) - mindist;
            }
            else val += mindist;
            if (closeFactory && isCloseToFactory(worker.getMapLocation())) val += (1 << 19);

            return -val;
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    static void doFirstActions(){
        try {
            ArrayList<AuxUnit> workers = new ArrayList<>();
            for (int i : Units.workers){
                AuxUnit worker = Units.myUnits.get(i);
                workers.add(worker);
            }
            //if (Utils.round < 60 && Utils.round > 30) {
                //System.out.println("Placing blueprint!!");
           // }

            workers.sort((a, b) -> value(a) < value(b) ? -1 : value(a) == value(b) ? 0 : 1);
            for (int i = 0; i < workers.size(); ++i){
                if(workers.get(i).isInSpace() || workers.get(i).isInGarrison()) continue;
                Worker.getInstance().doAction(workers.get(i), true);
                //if (Utils.round < 60 && Utils.round > 30) {
                    //System.out.println("Working on " + workers.get(i).getMapLocation().x + " " + workers.get(i).getMapLocation().y);
                    //System.out.println(sd);
                //}
            }
        }catch(Exception e){
            e.printStackTrace();
        }
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
            e.printStackTrace();
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
            e.printStackTrace();
            return 0;
        }
    }

    static void addWorkers(AuxMapLocation loc, int val){
        try {
            int largeRad = 2*workerRadius;
            int n = Vision.Mx[largeRad].length;
            for (int i = 0; i < n; ++i){
                AuxMapLocation newLoc = loc.add(new AuxMapLocation(Vision.Mx[largeRad][i], Vision.My[largeRad][i]));
                if (!newLoc.isOnMap()) continue;
                int dist = loc.distanceBFSTo(newLoc);
                if (newLoc.isOnMap() && dist * dist <= workerRadius) workersDeployed[newLoc.x][newLoc.y] += val;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    static boolean getConnectivity(AuxMapLocation loc){
        try {
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
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    static int getBestRocketLocation(AuxUnit unit){
        try {
            int bestDir = 8;
            int bestScore = -100;
            Danger.computeDanger(unit);
            for (int i = 0; i < 8; ++i) {
                AuxMapLocation newLoc = unit.getMapLocation().add(i);
                if (!newLoc.isOnMap()) continue;
                if (!newLoc.isPassable()) continue;
                if (Danger.DPS[i] != 0) continue;
                AuxUnit unit2 = newLoc.getUnit();
                if (unit2 != null && !unit2.myTeam) continue; //hi ha un enemic
                else if (unit2 != null) {
                    //hi ha un aliat
                    if (unit2.isStructure()) continue;
                    if (Units.rocketRequest != null && !Units.rocketRequest.urgent) continue; //si no es urgent suda
                    if (Units.rocketRequest != null && Units.rocketRequest.roundRequested - Utils.round < 3) continue; //si fa menys de 3 rondes que s'ha demanat
                }
                int score = getRocketBlueprintScore(newLoc);
                if (score > bestScore){
                    bestDir = i;
                    bestScore = score;
                }
            }
            if (Units.rocketRequest != null && !Units.rocketRequest.urgent && bestScore < 0) return 8;
            return bestDir;
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    static int getRocketBlueprintScore(AuxMapLocation loc){
        try {
            int score = 0;
            if (loc.getUnit() != null) score -= 6;
            for (int i = 0; i < 8; i++){
                AuxMapLocation newLoc = loc.add(i);
                AuxUnit unit2 = newLoc.getUnit();
                if (unit2 == null) continue;
                if (!unit2.myTeam) continue;
                if (unit2.type == UnitType.Factory) score -= 2;
                if (unit2.type == UnitType.Rocket) score -= 3;
            }
            return score;
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    static int getBestFactoryLocation(AuxUnit unit){
        try {
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
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    static class FactoryData{
        AuxMapLocation loc;
        boolean connectivity;
        double distToWalls;
        double workersNear;

        public FactoryData(AuxMapLocation _loc){
            try {
                loc = _loc;
                if (!loc.isOnMap()) return;
                connectivity = getConnectivity(loc);
                distToWalls = Pathfinder.distToWalls[loc.x][loc.y];
                if (distToWalls > 3) distToWalls = 3;
                workersNear = workerAreas[loc.x][loc.y];
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        public boolean isBetter(FactoryData B){
            try {
                if (B == null) return true;
                if (connectivity && !B.connectivity) return true;
                if (B.connectivity && !connectivity) return false;
                if (distToWalls > B.distToWalls) return true;
                if (B.distToWalls < distToWalls) return false;
                return workersNear > B.workersNear;
            }catch(Exception e){
                e.printStackTrace();
            }
            return false;
        }

    }

}
