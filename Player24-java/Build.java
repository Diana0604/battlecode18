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

    static UnitType nextStructure = null;
    static AuxMapLocation nextStructureLocation = null;

    static void initTurn(){
        updateBlueprintsToBuild();
        updateStructuresToRepair();
        updateRocketRequest();
        if (Utils.karbonite < 200) lastRoundUnder200Karbo = Utils.round;
        if (Units.enemies.size() != 0) lastRoundEnemySeen = Utils.round;
        if (Utils.round > 50) WorkerUtil.safe = false;
        if (Utils.round > 400) oneWorkerToMars = false;
        if (nextStructure == null) pickNextStructure();
    }

    /* todo
    Coses per decidir globalment de la build:
    - Next unit to build (rocket/factory/troop)
    - Quin worker es replica
    - Quin worker construeix factory/rocket
    - Quina factory construeix tropa
    - Quins workers van a construir/reparar?

     */

    private static void pickNextStructure(){
        nextStructure = chooseStructure();
        nextStructureLocation = chooseStructureLocation();
    }

    private static UnitType chooseStructure(){
        try {
            int numFactories = Units.unitTypeCount.get(UnitType.Factory);
            if (numFactories < 3) return UnitType.Factory;

            if (Build.canBuildRockets && Build.rocketRequest != null && Build.rocketRequest.urgent) return UnitType.Rocket;

            int roundsOver200Karbo = Utils.round - Build.lastRoundUnder200Karbo;
            if (roundsOver200Karbo == 5 && numFactories < Worker.MAX_FACTORIES) return UnitType.Factory;

            if (Build.canBuildRockets && Build.rocketRequest != null) return UnitType.Rocket;

            if (Utils.karbonite >= 800) return UnitType.Factory;

            return null;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static AuxMapLocation chooseStructureLocation(){
        if (nextStructure == null) return null;
        if (nextStructure == UnitType.Factory) return chooseFactoryLocation();
        if (nextStructure == UnitType.Rocket) return chooseRocketLocation();
        return null;
    }

    //triem la millor location adjacent a qualsevol worker
    private static AuxMapLocation chooseFactoryLocation(){
        AuxMapLocation bestLoc = null;
        for (int index: Units.workers) {
            AuxUnit worker = Units.myUnits.get(index);
            AuxMapLocation workerLoc = worker.getMapLocation();
            WorkerUtil.FactoryData bestFactory = null;
            for (int i = 0; i < 8; ++i) {
                if (Wrapper.canPlaceBlueprint(worker, UnitType.Factory, i)) {
                    WorkerUtil.FactoryData fd = new WorkerUtil.FactoryData(workerLoc.add(i));
                    if (fd.isBetter(bestFactory)) {
                        bestFactory = fd;
                        bestLoc = workerLoc.add(i);
                    }
                }
            }
        }
        return bestLoc;
    }

    private static AuxMapLocation chooseRocketLocation(){
        try {
            AuxMapLocation bestLoc = null;
            int bestScore = -100;
            for (int index: Units.workers) {
                AuxUnit worker = Units.myUnits.get(index);
                //hem de tenir en compte els workers que no poden construir?
                Danger.computeDanger(worker);
                for (int i = 0; i < 8; ++i) {
                    AuxMapLocation rocketLoc = worker.getMapLocation().add(i);
                    if (!rocketLoc.isOnMap()) continue;
                    if (!rocketLoc.isPassable()) continue;
                    if (!Wrapper.canPlaceBlueprint(worker, UnitType.Rocket, i)) continue;
                    if (Danger.DPS[i] != 0) continue;
                    AuxUnit unit2 = rocketLoc.getUnit();
                    if (unit2 != null && !unit2.myTeam) continue; //hi ha un enemic
                    else if (unit2 != null) {
                        //hi ha un aliat
                        if (unit2.isStructure()) continue;
                        if (Build.rocketRequest != null && !Build.rocketRequest.urgent) continue; //si no es urgent suda
                        if (Build.rocketRequest != null && Build.rocketRequest.roundRequested - Utils.round < 3)
                            continue; //si fa menys de 3 rondes que s'ha demanat
                    }
                    int score = getRocketBlueprintScore(rocketLoc);
                    if (score > bestScore) {
                        bestLoc = rocketLoc;
                        bestScore = score;
                    }
                }
            }
            if (Build.rocketRequest != null && !Build.rocketRequest.urgent && bestScore < 0) return null;
            return bestLoc;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static int getRocketBlueprintScore(AuxMapLocation loc){
        try {
            final int DISINTEGRATE_PENALTY = -6;
            final int ADJACENT_FACTORY_PENALTY = -2;
            final int ADJACENT_ROCKET_PENALTY = -3;
            int score = 0;
            if (loc.getUnit() != null) score += DISINTEGRATE_PENALTY;
            for (int i = 0; i < 8; i++){
                AuxMapLocation newLoc = loc.add(i);
                AuxUnit unit2 = newLoc.getUnit();
                if (unit2 == null) continue;
                if (!unit2.myTeam) continue;
                if (unit2.type == UnitType.Factory) score += ADJACENT_FACTORY_PENALTY;
                if (unit2.type == UnitType.Rocket) score += ADJACENT_ROCKET_PENALTY;
            }
            return score;
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }


    static void updateBlueprintsToBuild(){
        try {
            final int MAX_WORKERS_TO_CALL = 6;

            for (int index : Units.blueprints) {
                //Per cada blueprint, crida els 6 workers mes propers a construir-lo
                AuxUnit bp = Units.myUnits.get(index);
                if (bp.isMaxHealth()) continue;
                ArrayList<Pair> sorted = new ArrayList<>();
                for (int index2 : Units.workers) {
                    AuxUnit worker = Units.myUnits.get(index2);
                    AuxMapLocation workerLoc = worker.getMapLocation();
                    if (workerLoc == null) continue;
                    Pair p = new Pair(bp.getMapLocation().distanceSquaredTo(worker.getMapLocation()), worker);
                    sorted.add(p);
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
