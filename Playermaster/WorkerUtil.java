import bc.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Ivan on 1/20/2018.
 */
public class WorkerUtil {

    static int[][] workerActions; //(i,j) = nº de worker actions a (i,j)
    static int[][] workerActionsExpanded; //nº de worker actions al 3x3 de (i,j)
    static int approxMapValue = 0;
    static int min_nb_workers = 2;
    static int min_nb_workers1 = 2;
    static int extra_workers;

    static boolean[] connectivityArray;
    static int[][] workerAreas;
    static double[][] workerAreas2;
    static final int workerRadius = 16;

    static int[][] workersDeployed;
    static ArrayList<AuxMapLocation> importantLocations; //totes les locations amb workActions adjacents

    static AuxMapLocation bestFactoryLocation;

    static double worker_value1 = 63, worker_value = 70;

    final static double decrease_rate = 0.95;
    final static double decrease_rate_worker_area = 0.75;
    final static int MIN_DIST = 4;

    static int workerCont;
    static int workersCreated;

    static boolean safe;
    static boolean hasReplicated;
    static int totalKarboCollected;

    static double minSafeTurns;

    static boolean closeFactory = true;


    static HashMap<AuxMapLocation, Integer> valueForFactory;


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
                    if((factor == 1 && i != 8 && !newLoc.isOccupiedByStructure()) || i == 8) workerActionsExpanded[newLoc.x][newLoc.y] += factor*val;
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
            workerAreas2 = new double[Mapa.W][Mapa.H];
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
                    if (unit.isStructure()) {
                        int dif = Units.getMaxHealth(unit.getType()) - unit.getHealth();
                        if (dif > 0) {
                            int factor = 1;
                            if (Danger.enemySeen) factor = 2;
                            if (!unit.isBuilt()) putAction(currentLoc, (dif + Units.buildingPower - 1) / Units.buildingPower, factor);
                            else putAction(currentLoc, (dif + Units.repairingPower - 1) / Units.repairingPower, 2);
                        }
                    } else {
                        if (unit.getType() == UnitType.Worker){
                            for (int k = 0; k < Vision.Mx[2*workerRadius].length; ++k){
                                AuxMapLocation newLoc = currentLoc.add(new AuxMapLocation(Vision.Mx[2*workerRadius][k], Vision.My[2*workerRadius][k]));
                                if (!newLoc.isOnMap()) continue;
                                int d = newLoc.distanceBFSTo(currentLoc);
                                if (d <= workerRadius){
                                    ++workerAreas[newLoc.x][newLoc.y];
                                    workerAreas2[newLoc.x][newLoc.y] += Math.pow(decrease_rate_worker_area, d);
                                }
                            }
                        }
                        putAction(currentLoc, (Karbonite.karboMap[i][j] + Units.harvestingPower - 1) / Units.harvestingPower, 1);
                    }
                }
            }

            for (int i = 0; i < Mapa.W; ++i) {
                for (int j = 0; j < Mapa.H; ++j) {
                    if (workerActionsExpanded[i][j] > 0) importantLocations.add(new AuxMapLocation(i,j));
                }
            }
            //System.out.println(importantLocations.size());
        }catch(Exception e) {
            e.printStackTrace();;
        }
    }

    //si una casella esta mes a prop nostra que de l'enemic
    static boolean isSafe(AuxMapLocation loc){
        try {
            int dist1 = 100000, dist2 = 100000;
            for (int i = 0; i < Utils.startingLocations.size(); ++i){
                dist1 = Math.min(dist1, loc.distanceBFSTo(Utils.startingLocations.get(i)));
            }
            for (int i = 0; i < Utils.enemyStartingLocations.size(); ++i){
                dist2 = Math.min(dist2, loc.distanceBFSTo(Utils.enemyStartingLocations.get(i)));
            }

            return (1.2*dist1 <= dist2);
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    //a distancia <= 32 d'una factory
    private static boolean isCloseToFactory(AuxMapLocation loc){
        try {
            AuxUnit[] closeUnits = Wrapper.senseUnits(loc, 32, true);
            for (int i = 0; i < closeUnits.length; ++i){
                //if (loc.distanceSquaredTo(closeUnits[i].getMapLocation()) <= 8) continue; //no volem factories contigues
                if (closeUnits[i].getType() == UnitType.Factory) return true;
            }
            return false;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    static int getFactoryValue(AuxMapLocation location){
        try {
            int val = 0;
            int mindist = 10000;
            for (int i = 0; i < Utils.enemyStartingLocations.size(); ++i){
                mindist = Math.min(mindist, location.distanceBFSTo(Utils.enemyStartingLocations.get(i)));
            }

            if (getConnectivity(location)) val += (1 << 28);

            if (closeFactory && isCloseToFactory(location)) val += (1 << 27);
            val += (int)(10.0*workerAreas2[location.x][location.y])*(1 << 15);

            if (isSafe(location)){
                val += (1 << 14) - mindist;
            }
            else val += mindist;

            return -val;
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }
    //retorna la direccio on hi ha mes karbo (nomes adjacent)
    static int getMostKarboLocation(AuxMapLocation loc){
        //BUG: no detecten la karbonite de llocs on abans hi havia hagut una factory, perque l'han borrat de l'array
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
                    AuxUnit unit = newLoc.getUnit();
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




}
