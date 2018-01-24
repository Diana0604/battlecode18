import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Worker {

    private static Worker instance = null;

    static Worker getInstance(){
        if (instance == null) instance = new Worker();
        return instance;
    }

    public Worker(){
        WorkerUtil.computeApproxMapValue();
    }

    private AuxUnit unit;
    private boolean danger;
    private boolean wait;
    private final int MAX_FACTORIES = 5;



    /*----------- ACTIONS ------------*/

    boolean doAction(AuxUnit _unit){
        try {
            unit = _unit;
            tryReplicate(_unit);
            if (!unit.canAttack()) return true;
            if (tryBuildAndRepair()) return true;
            if (tryPlaceBlueprint()) {
                unit.canMove = false;
                return true;
            }
            if (tryMine()) return true;
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean tryReplicate(AuxUnit unit){
        try {
            if (!unit.canUseAbility()) return false;
            if (shouldReplicate()) {
                int dir = 0;
                if (unit.target != null) dir = unit.getMapLocation().dirBFSTo(unit.target);
                for (int i = 0; i <= 4; ++i) {
                    int newDir = (dir + i)%8;
                    if (tryReplicateDir(unit, newDir)) return true;
                    if (i == 0 || i == 4) continue;
                    newDir = (dir + 8 - i)%8;
                    if (tryReplicateDir(unit, newDir)) return true;
                }
            }
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean tryReplicateDir(AuxUnit unit, int dir){
        if (Wrapper.canReplicate(unit, dir)) {
            AuxUnit newWorker = Wrapper.replicate(unit, dir);
            Units.unitTypeCount.put(UnitType.Worker, Units.unitTypeCount.get(UnitType.Worker) + 1);
            WorkerUtil.extra_workers++;

            int index = Units.myUnits.size();
            int newID = newWorker.getID();
            Units.myUnits.add(newWorker);
            Units.allUnits.put(newID, index);
            Units.workers.add(index);
            Units.unitMap[newWorker.getX()][newWorker.getY()] = index + 1;
            newWorker.target = getTarget(newWorker);
            doAction(newWorker);
            UnitManager.move(newWorker);
            doAction(newWorker);
            return true;
        }
        return false;
    }

    private boolean shouldReplicate(){
        if (Mapa.onEarth()) return shouldReplicateEarth();
        else return shouldReplicateMars();
    }

    private boolean shouldReplicateEarth(){
        try {
            Danger.computeDanger(unit);
            boolean danger = (Danger.DPSlong[8] > 0);
            if (danger) return false;
            int nb_actions = WorkerUtil.getWorkerActions(unit.getMapLocation(), 30);
            if (Units.unitTypeCount.get(UnitType.Worker) < WorkerUtil.min_nb_workers) return (nb_actions >= 12);
            return (nb_actions >= 30);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean shouldReplicateMars(){
        if (Utils.round > 750) return true;
        if (Utils.karbonite > 120) return true;
        //if (Utils.karbonite < 40) return false; //no se si aixo va be o no
        HashMap<Integer, Integer> tasks = Karbonite.asteroidTasksLocs;
        HashMap<Integer, Integer> karboAt = Karbonite.karboniteAt;
        for(Integer encoding: karboAt.keySet()){
            if (!tasks.containsKey(encoding)) continue; //no hauria de passar mai pero just in case
            int assignedID = tasks.get(encoding);
            if (!Units.allUnits.containsKey(assignedID)) return true; //ha trobat una mina sense assignar, crea worker per enviar-li
        }
        return false;
    }

    //Intenta reparar o construir una structure adjacent
    private boolean tryBuildAndRepair(){
        try {
            if (Mapa.onMars()) return false;
            int minDif = 1000;
            int minDifIndex = -1;
            int minHP = 1000;
            int minHPIndex = -1;
            AuxUnit[] adjUnits = Wrapper.senseUnits(unit.getMapLocation(), 2, true);
            for (int i = 0; i < adjUnits.length; ++i) {
                AuxUnit u = adjUnits[i];
                if (!(u.getType() == UnitType.Factory || u.getType() == UnitType.Rocket)) continue;
                if (u.isMaxHealth()) continue;
                int health = u.getHealth();
                int maxHealth = Units.getMaxHealth(u.getType());
                boolean built = u.isBuilt();
                if (built && health < minHP) {
                    minHP = health;
                    minHPIndex = i;
                }
                if (!built && maxHealth - health < minDif) {
                    minDif = maxHealth - health;
                    minDifIndex = i;
                }
            }
            if (minDifIndex >= 0) {
                AuxUnit structure = adjUnits[minDifIndex];
                Wrapper.build(unit, structure);
                if (structure.getHealth() < Units.getMaxHealth(structure.getType())) unit.canMove = false;
                return true;
            }
            if (minHPIndex >= 0) {
                AuxUnit structure = adjUnits[minHPIndex];
                Wrapper.repair(unit, adjUnits[minHPIndex]);
                if (structure.getHealth() < Units.getMaxHealth(structure.getType())) unit.canMove = false;
                return true;
            }
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //Posen un blueprint en una posicio adjacent (aixo s'ha de canviar quan ho fem global)
    private boolean tryPlaceBlueprint(){
        try {
            if (Mapa.onMars()) return false;
            if (danger) return false;
            UnitType type = chooseStructure();
            if (type == null) return false;

            int i = WorkerUtil.getBestFactoryLocation(unit);
            if (i < 8) {
                Danger.computeDanger(unit);
                if(Danger.DPSlong[i] > 0) return false;
                Wrapper.placeBlueprint(unit, type, i);
                unit.canMove = false;
                return true;
            }
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private UnitType chooseStructure(){
        boolean canBuildRocket = Research.researchInfo.getLevel(UnitType.Rocket) > 0;
        boolean isFirstRocket = Units.firstRocket && Units.unitTypeCount.get(UnitType.Rocket) == 0;

        if (canBuildRocket && isFirstRocket) return UnitType.Rocket;
        int numFactories = Units.unitTypeCount.get(UnitType.Factory);
        if (numFactories == 0) return UnitType.Factory;
        int roundsOver100Karbo = Utils.round - Units.lastRoundUnder100Karbo;
        if (roundsOver100Karbo < 5) return null;
        if (numFactories < MAX_FACTORIES) return UnitType.Factory;
        if (canBuildRocket) return UnitType.Rocket;
        return null;
    }

    //minen una mina adjacent
    private boolean tryMine(){
        try {
            //System.out.println("Trying to mine! " + unit.getID());
            int dir = WorkerUtil.getMostKarboLocation(unit.getMapLocation());
            AuxMapLocation newLoc = unit.getMapLocation().add(dir);
            if (Karbonite.karboMap[newLoc.x][newLoc.y] > 0 && Wrapper.canHarvest(unit, dir)) {
                Wrapper.harvest(unit, dir);
                //System.out.println(newLoc.x + " " + newLoc.y);
                return true;
            }
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    /*-------------- Targets ---------------*/

    AuxMapLocation getTarget(AuxUnit _unit){
        unit = _unit;
        if (Mapa.onEarth()) return earthTarget();
        return marsTarget();
    }

    private AuxMapLocation earthTarget(){
        try {
            if (!unit.canMove()) return null;

            ArrayList<Target> targets = new ArrayList<>();

            Target rocket = getRocketTarget();
            if (rocket != null) targets.add(rocket);

            Target karboTarget = getKarboniteTarget();
            if (karboTarget != null) targets.add(karboTarget);

            Target buildTarget = getBuildTarget();
            if (buildTarget != null) targets.add(buildTarget);

            Target repairTarget = getRepairTarget();
            if (repairTarget != null) targets.add(repairTarget);

            targets.sort((a, b) -> targetEval(a) < targetEval(b) ? -1 : targetEval(a) == targetEval(b) ? 0 : 1);

            AuxMapLocation dest;
            if (targets.size() > 0) {
                Target bestTarget = targets.get(0);
                dest = bestTarget.mloc;
            }else dest = unit.getMapLocation(); //move to self per evitar perill
            //System.out.println("Worker " + unit.getID() + " loc " + unit.getMapLocation().x + "," + unit.getMapLocation().y + " va a " + dest.x + "," + dest.y + "   " + wait);
            //MovementManager.getInstance().move(unit, dest);
            return dest;

        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private AuxMapLocation marsTarget(){
        try{
            if (!unit.canMove()) return null;
            //asteroidTasks.get(positiu) retorna el worker assignat a la location codificada
            //asteroidTasks.get(negatiu) retorna la location assignada al worker amb id -negatiu
            HashMap<Integer, Integer> tasksLocs = Karbonite.asteroidTasksLocs;
            HashMap<Integer, Integer> tasksIDs = Karbonite.asteroidTasksIDs;
            AuxMapLocation destination;
            int id = unit.getID();
            if (tasksIDs.containsKey(id)){
                //si ja tinc mina assignada, hi vaig
                int encoding = tasksIDs.get(id);
                AuxMapLocation location = new AuxMapLocation(encoding);
                if (location.getKarbonite() == 0){
                    //si la mina ja esta buida, trec la task
                    //System.out.println(GC.round + " worker " + id + " removes mine " + location.x + "," + location.y);
                    tasksLocs.remove(encoding);
                    tasksIDs.remove(id);
                }
                destination = location;
            }else {
                double minDist = 100000000;
                AuxMapLocation minLoc = null;
                for (Map.Entry<Integer, Integer> entry : tasksLocs.entrySet()) {
                    AuxMapLocation loc = new AuxMapLocation(entry.getKey());
                    int assignedID = entry.getValue();
                    if (Units.allUnits.containsKey(assignedID)) continue; //si ja esta assignada suda
                    //if (assignedID != -1) continue;
                    double dist = unit.getMapLocation().distanceBFSTo(loc);
                    if (dist >= Const.INF) continue; //si no esta passable
                    if (dist < minDist) {
                        minDist = dist;
                        minLoc = loc;
                    }
                }
                if (minLoc == null){
                    //si no queda cap mina passable per assignar, va a la mes propera
                    minDist = 100000000;
                    minLoc = null;
                    for (Integer encoding : tasksLocs.keySet()) {
                        AuxMapLocation loc = new AuxMapLocation(encoding);
                        double dist = unit.getMapLocation().distanceSquaredTo(loc);
                        if (dist < minDist){
                            minDist = dist;
                            minLoc = loc;
                        }
                    }
                    if (minLoc == null) destination = unit.getMapLocation();
                    else destination = minLoc;
                }else{
                    //ha trobat una mina buida, se l'assigna
                    int encoding = minLoc.encode();
                    //System.out.println(GC.round + " worker " + id + " s'assigna la mina " + minLoc.x + "," + minLoc.y + "   " + tasksLocs.get(encoding));
                    tasksIDs.put(id, encoding);
                    tasksLocs.put(encoding, id);
                    destination = minLoc;
                }
            }
            //System.out.println("Worker moves to position " + destination.x + "," + destination.y + "  //  " + l.x + "," + l.y);
            return destination;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //value = worker turns, capat a 50
    private Target getKarboniteTarget(){
        try {
            final int KARBONITE_MULTIPLIER = 1;
            Target ans = null;
            for (HashMap.Entry<Integer, Integer> entry : Karbonite.karboniteAt.entrySet()) {
                AuxMapLocation mineLoc = new AuxMapLocation(entry.getKey());
                int workerTurns = Math.min(50, entry.getValue()/ Units.harvestingPower);
                int value = workerTurns * KARBONITE_MULTIPLIER;
                if (WorkerUtil.buildingAt(mineLoc)) continue;
                double dist = unit.getMapLocation().distanceBFSTo(mineLoc);
                if (ans == null) {
                    ans = new Target(value, dist, mineLoc, 3);
                    continue;
                }
                Target aux = new Target(value, dist, mineLoc, 3);

                if (aux.dist <= 1 && ans.dist > 1) ans = aux; //sempre prioritza anar a les mines que estiguin al quadrat 3x3 (no se si aixo ajuda, es per provar)
                if (targetEval(aux) < targetEval(ans)) ans = aux;
            }
            return ans;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //value = 20 * worker turns
    private Target getBuildTarget() {
        try {
            final int BLUEPRINT_MULTIPLIER = 20;
            if (Units.blueprintsToBuild.containsKey(unit.getID())) {
                int bID = Units.blueprintsToBuild.get(unit.getID());

                AuxUnit blueprint = Units.getUnitByID(bID);

                AuxMapLocation bLoc = blueprint.getMapLocation();
                double dist = Math.max(unit.getMapLocation().distanceBFSTo(bLoc) - 1, 0);
                //vida que li faltara quan arribi
                int remainingHP = (Units.getMaxHealth(blueprint.getType()) - blueprint.getHealth())
                        - (int) (dist * WorkerUtil.getAdjacentWorkers(blueprint.getMapLocation()) * Units.buildingPower);
                if (remainingHP <= 0) return null;
                int workerTurns = remainingHP/ Units.buildingPower + 1;
                int value = workerTurns * BLUEPRINT_MULTIPLIER;
                return new Target(value, dist, bLoc, 2);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //value = 10 * worker turns
    private Target getRepairTarget(){
        try{
            final int STRUCTURE_MULTIPLIER = 10;
            if (Units.structuresToRepair.containsKey(unit.getID())) {
                int sID = Units.structuresToRepair.get(unit.getID());

                AuxUnit structure = Units.getUnitByID(sID);

                AuxMapLocation sLoc = structure.getMapLocation();
                double dist = Math.max(unit.getMapLocation().distanceBFSTo(sLoc) - 1, 0);
                int missingHP = (Units.getMaxHealth(structure.getType()) - structure.getHealth())
                        - (int)(dist*WorkerUtil.getAdjacentWorkers(structure.getMapLocation())* Units.repairingPower);
                if (missingHP <= 0) return null;
                int workerTurns = missingHP/ Units.repairingPower + 1;
                int value = workerTurns * STRUCTURE_MULTIPLIER;
                return new Target(value, dist, sLoc, 2);
            }
            return null;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //value = 200
    private Target getRocketTarget() {
        try {
            final int ROCKET_MULTIPLIER = 200;
            AuxMapLocation mLoc = Rocket.callsToRocket.get(unit.getID());
            if (mLoc == null) return null;
            return new Target(ROCKET_MULTIPLIER, unit.getMapLocation().distanceBFSTo(mLoc), mLoc, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private double targetEval(Target a){
        try {
            return -(a.value / (a.dist + 1));
        }catch(Exception e) {
            e.printStackTrace();
            return Double.parseDouble(null);
        }
    }

    private class Target {
        double value;
        double dist;
        AuxMapLocation mloc;
        int type;

        Target(double v, double d, AuxMapLocation loc, int _type){
            value = v; //valor global del target
            dist = d;
            mloc = loc;
            type = _type;
        }

    }


}