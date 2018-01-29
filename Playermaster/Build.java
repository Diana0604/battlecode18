import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Build {

    static boolean oneWorkerToMars = true;
    static int rocketsLaunched = 0;
    static int rocketsBuilt = 0;
    static int knightsBuilt = 0;
    static int troopsSinceRocketResearch = 0;
    static RocketRequest rocketRequest;
    static HashMap<Integer, Integer> blueprintsToBuild;
    static HashMap<Integer, Integer> structuresToRepair;
    static int lastRoundEnemySeen;
    static int lastRoundUnder200Karbo;
    static boolean canBuildRockets;
    static boolean isolated;
    static int initDistToEnemy;
    static boolean firstFactory = false;

    static UnitType nextStructureType = null;

    static void initTurn(){
        updateBlueprintsToBuild();
        updateStructuresToRepair();
        updateRocketRequest();
        if (Utils.karbonite < 200) lastRoundUnder200Karbo = Utils.round;
        if (Units.enemies.size() != 0) lastRoundEnemySeen = Utils.round;
        if (Utils.round > 50) WorkerUtil.safe = false;
        if (Utils.round > 400) oneWorkerToMars = false;
        if (Mapa.onMars()) return;
        pickNextStructure();
    }

    /* todo
    Coses per decidir globalment de la build:
    - Next unit to build (rocket/factory/troop)
    - Quin worker es replica
    - Quin worker construeix factory/rocket
    - Quina factory construeix tropa
    - Quins workers van a construir/reparar?

     */

    /*------------ NEXT STRUCTURE -------------*/

    static void pickNextStructure(){
        nextStructureType = chooseStructureType();
    }

    private static UnitType chooseStructureType(){
        try {
            int numFactories = Units.unitTypeCount.get(UnitType.Factory);
            if (numFactories < 3) return UnitType.Factory;

            if (Build.canBuildRockets && Build.rocketRequest != null && Build.rocketRequest.urgent) return UnitType.Rocket;

            if (Utils.karbonite > Const.factoryCost && numFactories < Worker.MAX_FACTORIES) return UnitType.Factory;

            if (Build.canBuildRockets && Build.rocketRequest != null) return UnitType.Rocket;

            if (Utils.karbonite >= 400) return UnitType.Factory;

            return null;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /*---------------random shit-----------------*/

    static void updateBlueprintsToBuild(){
        try {
            final int MAX_WORKERS_TO_CALL = 7;

            for (int index : Units.blueprints) {
                //Per cada blueprint, crida els 6 workers mes propers a construir-lo
                AuxUnit bp = Units.myUnits.get(index);
                if (bp.isMaxHealth()) continue;
                ArrayList<Pair> sorted = new ArrayList<>();
                for (int index2 : Units.workers) {
                    AuxUnit worker = Units.myUnits.get(index2);
                    AuxMapLocation workerLoc = worker.getMapLocation();
                    if (workerLoc == null) continue;
                    int dist = bp.getMapLocation().distanceBFSTo(worker.getMapLocation());
                    if (dist < 6) sorted.add(new Pair(dist, worker));
                }
                sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
                int workersToCall =  Math.min(MAX_WORKERS_TO_CALL, sorted.size() - 1);
                if (workersToCall == 0) workersToCall = 1;
                if (workersToCall < 0) workersToCall = 0;
                List<Pair> cut = sorted.subList(0, workersToCall);
                for (Pair p : cut) {
                    int key = p.unit.getID();
                    int value = bp.getID();
                    if (!blueprintsToBuild.containsKey(key)) blueprintsToBuild.put(key, value);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void updateStructuresToRepair(){
        try {
            final int MAX_WORKERS_TO_CALL = 3;

            for (int index : Units.structures) {
                //Per cada blueprint, crida els 8 workers mes propers a construir-lo
                AuxUnit s = Units.myUnits.get(index);
                if (s.isBlueprint()) continue;
                if (s.isMaxHealth()) continue;

                ArrayList<Pair> sorted = new ArrayList<>();
                for (int index2 : Units.workers) {
                    AuxUnit worker = Units.myUnits.get(index2);
                    //no fiquem workers si ja son cridats per un blueprint
                    if (blueprintsToBuild.containsKey(worker.getID())) continue;
                    AuxMapLocation workerLoc = worker.getMapLocation();
                    if (workerLoc == null) continue;
                    Pair p = new Pair(s.getMapLocation().distanceSquaredTo(worker.getMapLocation()), worker);
                    sorted.add(p);
                }
                sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
                int workersToCall =  Math.min(MAX_WORKERS_TO_CALL, sorted.size() - 1);
                if (workersToCall == 0) workersToCall = 1;
                if (workersToCall <= 0) workersToCall = 0;
                List<Pair> cut = sorted.subList(0, workersToCall);
                for (Pair p : cut) {
                    int key = p.unit.getID();
                    int value = s.getID();
                    if (!structuresToRepair.containsKey(key)) structuresToRepair.put(key, value);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void updateRocketRequest(){
        try {
            if (!canBuildRockets) return;
            if (Mapa.onMars()) return;

            if (rocketRequest != null && rocketRequest.urgent) return;
            if (rocketRequest != null){
                //check if normal request becomes urgent
                if (Utils.round - rocketRequest.roundRequested >= 10){
                    rocketRequest.urgent = true;
                }
            }
            //check urgent requests
            final int MAX_ROCKETS_BUILT = 5; //per si de cas lol

            if (Units.unitTypeCount.get(UnitType.Rocket) >= MAX_ROCKETS_BUILT) return;

            if (rocketsLaunched == 0 && Units.unitTypeCount.get(UnitType.Rocket) == 0){
                //firstrocket
                rocketRequest = new RocketRequest(Utils.round, true);
                return;
            }
            if (Utils.round >= 500){
                //endgame
                rocketRequest = new RocketRequest(Utils.round, true);
                return;
            }

            if (Units.robots.size() - Units.workers.size() < 6) return;

            //check normal requests
            if (rocketsBuilt * 8 < troopsSinceRocketResearch){
                //1 rocket cada 8 tropes fetes
                rocketRequest = new RocketRequest(Utils.round, false);
                return;
            }
            if (isolated){
                rocketRequest = new RocketRequest(Utils.round, false);
                return;
            }
            if (Utils.round - lastRoundEnemySeen > 10){
                //enemy exterminated
                rocketRequest = new RocketRequest(Utils.round, false);
                return;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    static class Pair {
        double dist;
        AuxUnit unit;

        Pair(double dist, AuxUnit unit){
            this.dist = dist;
            this.unit = unit;
        }
    }

    static class RocketRequest{
        boolean urgent;
        int roundRequested;

        RocketRequest(int round, boolean urgent){
            this.urgent = urgent;
            roundRequested = round;
        }
    }

}
