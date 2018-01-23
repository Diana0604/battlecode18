import bc.Planet;
import bc.UnitType;

import java.util.*;

public class Worker {

    static Worker instance = null;

    static Worker getInstance(){
        if (instance == null) instance = new Worker();
        return instance;
    }

    public Worker(){
        WorkerUtil.computeApproxMapValue();
        WorkerUtil.fillKarboniteAround();
        WorkerUtil.getKarboConstants();
        WorkerUtil.ExploreMap = new HashMap<Integer, Integer>();
    }

    private AuxUnit unit;
    private boolean danger;
    private boolean wait;

    /*----------- ACTIONS ------------*/

    public boolean doAction(AuxUnit _unit){
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
            if (Data.queue.needsUnit(UnitType.Worker) || shouldReplicate()) {
                int dir = 0;
                if (unit.target != null) dir = unit.getMaplocation().dirBFSTo(unit.target);
                for (int i = 0; i <= 4; ++i) {
                    int newDir = (dir + i)%8;
                    if (Wrapper.canReplicate(unit, newDir)) {
                        Wrapper.replicate(unit, newDir);
                        Data.unitTypeCount.put(UnitType.Worker, Data.unitTypeCount.get(UnitType.Worker) + 1);
                        WorkerUtil.extra_workers++;
                        //Data.queue.requestUnit(UnitType.Worker, false);
                        return true;
                    }
                    if (i == 0 || i == 4) continue;
                    newDir = (dir + 8 - i)%8;
                    if (Wrapper.canReplicate(unit, newDir)) {
                        Wrapper.replicate(unit, newDir);
                        Data.unitTypeCount.put(UnitType.Worker, Data.unitTypeCount.get(UnitType.Worker) + 1);
                        WorkerUtil.extra_workers++;
                        //Data.queue.requestUnit(UnitType.Worker, false);
                        return true;
                    }
                }
            }
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean shouldReplicate(){
        if (Data.onEarth()) return shouldReplicateEarth();
        else return shouldReplicateMars();
    }

    private boolean shouldReplicateEarth(){
        try {
            Danger.computeDanger(unit);
            boolean danger = (Danger.DPSlong[8] > 0);
            if (danger) return false;
            int nb_actions = WorkerUtil.getWorkerActions(unit.getMaplocation(), 30);
            if (Data.unitTypeCount.get(UnitType.Worker) < WorkerUtil.min_nb_workers) return (nb_actions >= 12);
            return (nb_actions >= 20);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean shouldReplicateMars(){
        if (Data.round > 750) return true;
        if (Data.getKarbonite() > 120) return true;
        //if (Data.karbonite < 40) return false; //no se si aixo va be o no
        HashMap<Integer, Integer> tasks = Data.asteroidTasksLocs;
        HashMap<Integer, Integer> karboAt = Data.karboniteAt;
        for(Integer encoding: karboAt.keySet()){
            if (!tasks.containsKey(encoding)) continue; //no hauria de passar mai pero just in case
            AuxMapLocation loc = Data.toLocation(encoding);
            if (unit.getMaplocation().distanceBFSTo(loc) > 10000) continue;
            int assignedID = tasks.get(encoding);
            if (!Data.allUnits.containsKey(assignedID)) return true; //ha trobat una mina sense assignar, crea worker per enviar-li
        }
        return false;
    }

    //Intenta reparar o construir una structure adjacent
    private boolean tryBuildAndRepair(){
        try {
            if (Data.onMars()) return false;
            int minDif = 1000;
            int minDifIndex = -1;
            int minHP = 1000;
            int minHPIndex = -1;
            AuxUnit[] adjUnits = Wrapper.senseUnits(unit.getMaplocation(), 2, true);
            for (int i = 0; i < adjUnits.length; ++i) {
                AuxUnit u = adjUnits[i];
                if (!(u.getType() == UnitType.Factory || u.getType() == UnitType.Rocket)) continue;
                if (u.isMaxHealth()) continue;
                int health = u.getHealth();
                int maxHealth = Wrapper.getMaxHealth(u.getType());
                boolean bp = u.isBlueprint();
                if (!bp && health < minHP) {
                    minHP = health;
                    minHPIndex = i;
                }
                if (bp && maxHealth - health < minDif) {
                    minDif = maxHealth - health;
                    minDifIndex = i;
                }
            }
            if (minDifIndex >= 0) {
                AuxUnit structure = adjUnits[minDifIndex];
                Wrapper.build(unit, structure);
                if (structure.getHealth() < Wrapper.getMaxHealth(structure.getType())) unit.canMove = false;
                return true;
            }
            if (minHPIndex >= 0) {
                AuxUnit structure = adjUnits[minHPIndex];
                Wrapper.repair(unit, adjUnits[minHPIndex]);
                if (structure.getHealth() < Wrapper.getMaxHealth(structure.getType())) unit.canMove = false;
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
            if (Data.onMars()) return false;
            if (danger) return false;
            UnitType type = chooseStructure();
            if (type == null) return false;

            int i = WorkerUtil.getBestFactoryLocation(unit);
            if (i < 8) {
                Danger.computeDanger(unit);
                if(Danger.DPSlong[i] > 0) return false;
                Wrapper.placeBlueprint(unit, type, i);
                if (type == UnitType.Rocket) Data.rocketsBuilt++;
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
        boolean canBuildRocket = Data.researchInfo.getLevel(UnitType.Rocket) > 0;
        boolean isFirstRocket = Data.firstRocket && Data.unitTypeCount.get(UnitType.Rocket) == 0;

        if (canBuildRocket && isFirstRocket) return UnitType.Rocket;
        int numFactories = Data.unitTypeCount.get(UnitType.Factory);
        if (numFactories == 0) return UnitType.Factory;
        int roundsOver100Karbo = Data.round - Data.lastRoundUnder100Karbo;
        if (numFactories < WorkerUtil.MAX_FACTORIES && roundsOver100Karbo >= 5) return UnitType.Factory;
        if (canBuildRocket) return UnitType.Rocket;
        return null;
    }

    //minen una mina adjacent
    private boolean tryMine(){
        try {
            //System.out.println("Trying to mine! " + unit.getID());
            int dir = WorkerUtil.getMostKarboLocation(unit.getMaplocation());
            AuxMapLocation newLoc = unit.getMaplocation().add(dir);
            if (Data.karboMap[newLoc.x][newLoc.y] > 0 && Wrapper.canHarvest(unit, dir)) {
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

    Boolean canKarbo(AuxMapLocation myLoc, AuxMapLocation newLoc){
        try {
            if (!newLoc.isOnMap()) return false;
            if (!Data.accessible[newLoc.x][newLoc.y]) return false;
            if (newLoc.distanceBFSTo(myLoc) > WorkerUtil.KARBO_DISTANCE) return false;
            return true;
        } catch(Exception e){
            System.out.println(e);
        }
        return false;
    }

    AuxMapLocation getTarget(AuxUnit _unit){
        unit = _unit;
        if (Data.onEarth()) return earthTarget();
        return marsTarget();
    }

    private AuxMapLocation earthTarget(){
        try {
            if (!unit.canMove()) return null;
            if(Data.round < WorkerUtil.EXPLORE_ROUND) {
                System.out.println(WorkerUtil.EXPLORE_ROUND);
                System.out.print("ronda: ");
                System.out.println(Data.round);
                AuxMapLocation target = getFirstTarget();
                if(target!= null) {
                    System.out.print("target: ");
                    System.out.print(target.x);
                    System.out.print(" ");
                    System.out.println(target.y);
                    return target;
                }
            }
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
            }else dest = unit.getMaplocation(); //move to self per evitar perill
            //System.out.println("Worker " + unit.getID() + " loc " + unit.getMaplocation().x + "," + unit.getMaplocation().y + " va a " + dest.x + "," + dest.y + "   " + wait);
            //MovementManager.getInstance().moveTo(unit, dest);
            //System.out.println(Data.round + " worker " + unit.getMaplocation().x + "," + unit.getMaplocation().y + " has target " + dest.x + "," + dest.y);
            return dest;

        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    AuxMapLocation marsTarget(){
        try{
            if (!unit.canMove()) return null;
            //asteroidTasks.get(positiu) retorna el worker assignat a la location codificada
            //asteroidTasks.get(negatiu) retorna la location assignada al worker amb id -negatiu
            HashMap<Integer, Integer> tasksLocs = Data.asteroidTasksLocs;
            HashMap<Integer, Integer> tasksIDs = Data.asteroidTasksIDs;
            AuxMapLocation destination;
            int id = unit.getID();
            if (tasksIDs.containsKey(id)){
                //si ja tinc mina assignada, hi vaig
                int encoding = tasksIDs.get(id);
                AuxMapLocation location = Data.toLocation(encoding);
                if (!Data.karboniteAt.containsKey(encoding)){
                    //si la mina ja esta buida, trec la task
                    //System.out.println(Data.round + " worker " + id + " removes mine " + location.x + "," + location.y);
                    tasksLocs.remove(encoding);
                    tasksIDs.remove(id);
                }
                destination = location;
            }else {
                double minDist = 100000000;
                AuxMapLocation minLoc = null;
                for (Map.Entry<Integer, Integer> entry : tasksLocs.entrySet()) {
                    AuxMapLocation loc = Data.toLocation(entry.getKey());
                    int assignedID = entry.getValue();
                    if (Data.allUnits.containsKey(assignedID)) continue; //si ja esta assignada suda
                    //if (assignedID != -1) continue;
                    double dist = unit.getMaplocation().distanceBFSTo(loc);
                    if (dist >= Pathfinder.INF) continue; //si no esta accessible
                    if (dist < minDist) {
                        minDist = dist;
                        minLoc = loc;
                    }
                }
                if (minLoc == null){
                    //si no queda cap mina accessible per assignar, va a la mes propera
                    minDist = 100000000;
                    minLoc = null;
                    for (Integer encoding : tasksLocs.keySet()) {
                        AuxMapLocation loc = Data.toLocation(encoding);
                        double dist = unit.getMaplocation().distanceSquaredTo(loc);
                        if (dist < minDist){
                            minDist = dist;
                            minLoc = loc;
                        }
                    }
                    if (minLoc == null) destination = unit.getMaplocation();
                    else destination = minLoc;
                }else{
                    //ha trobat una mina buida, se l'assigna
                    int encoding = Data.encodeOcc(minLoc.x, minLoc.y);
                    //System.out.println(Data.round + " worker " + id + " s'assigna la mina " + minLoc.x + "," + minLoc.y + "   " + tasksLocs.get(encoding));
                    tasksIDs.put(id, encoding);
                    tasksLocs.put(encoding, id);
                    destination = minLoc;
                }
            }
            if (destination == null){
                System.out.println(Data.round + " worker " + unit.getID() + " destination null!");
                destination = unit.getMaplocation();
            }
            //System.out.println("Worker moves to position " + destination.x + "," + destination.y + "  //  " + l.x + "," + l.y);
            return destination;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //target for first rounds
    AuxMapLocation getFirstTarget(){
        try {
            AuxMapLocation myLoc = unit.getMaplocation();
            int encodeLoc = Data.encodeOcc(myLoc.x, myLoc.y);
            if (WorkerUtil.ExploreMap.containsKey(unit.getID())) {
                int encodeTarget = WorkerUtil.ExploreMap.get(unit.getID());
                if (encodeLoc == encodeTarget) return null;
                return Data.toLocation(encodeTarget);
            }

            boolean[][] visited = new boolean[Data.W][Data.H];
            visited[myLoc.x][myLoc.y] = true;
            Queue<AuxMapLocation> q = new LinkedList<AuxMapLocation>();
            q.add(myLoc);
            AuxMapLocation target = myLoc;
            int maxKarbo = 0;
            while (!q.isEmpty()) {
                AuxMapLocation oldLoc = q.poll();
                //we want to go as far as possible without going to an empty space
                if (WorkerUtil.karboniteAround[oldLoc.x][oldLoc.y] <= maxKarbo && WorkerUtil.karboniteAround[oldLoc.x][oldLoc.y] > 0) {
                    target = oldLoc;
                }
                for (int i = 0; i < 9; ++i) {
                    AuxMapLocation newLoc = oldLoc.add(i);
                    if (canKarbo(myLoc, newLoc) && !visited[newLoc.x][newLoc.y]) {
                        q.add(newLoc);
                        visited[newLoc.x][newLoc.y] = true;
                    }
                }
            }
            WorkerUtil.decreaseKarboniteAround(target, 8);
            WorkerUtil.ExploreMap.put(unit.getID(), Data.encodeOcc(target.x, target.y));
            return target;
        } catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    //value = worker turns, capat a 50
    private Target getKarboniteTarget(){
        try {
            final int KARBONITE_MULTIPLIER = 1;
            Target ans = null;
            for (HashMap.Entry<Integer, Integer> entry : Data.karboniteAt.entrySet()) {
                AuxMapLocation mineLoc = Data.toLocation(entry.getKey());
                Danger.computeDanger(unit);
                if (Danger.DPS[8] > 0) continue; //nomes van a mines no perilloses
                int workerTurns = Math.min(50, entry.getValue()/ Data.harvestingPower);
                int value = workerTurns * KARBONITE_MULTIPLIER;
                if (WorkerUtil.buildingAt(mineLoc)) continue;
                double dist = unit.getMaplocation().distanceBFSTo(mineLoc);
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
            if (Data.blueprintsToBuild.containsKey(unit.getID())) {
                int bID = Data.blueprintsToBuild.get(unit.getID());

                int index = Data.allUnits.get(bID);
                AuxUnit blueprint = Data.myUnits[index];

                AuxMapLocation bLoc = blueprint.getMaplocation();
                double dist = Math.max(unit.getMaplocation().distanceBFSTo(bLoc) - 1, 0);
                //vida que li faltara quan arribi
                int remainingHP = (Wrapper.getMaxHealth(blueprint.getType()) - blueprint.getHealth())
                        - (int) (dist * WorkerUtil.getAdjacentWorkers(blueprint.getMaplocation()) * Data.buildingPower);
                if (remainingHP <= 0) return null;
                int workerTurns = remainingHP/ Data.buildingPower + 1;
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
            if (Data.structuresToRepair.containsKey(unit.getID())) {
                int sID = Data.structuresToRepair.get(unit.getID());

                int index = Data.allUnits.get(sID);
                AuxUnit structure = Data.myUnits[index];

                AuxMapLocation sLoc = structure.getMaplocation();
                double dist = Math.max(unit.getMaplocation().distanceBFSTo(sLoc) - 1, 0);
                int missingHP = (Wrapper.getMaxHealth(structure.getType()) - structure.getHealth())
                        - (int)(dist* WorkerUtil.getAdjacentWorkers(structure.getMaplocation())* Data.repairingPower);
                if (missingHP <= 0) return null;
                int workerTurns = missingHP/ Data.repairingPower + 1;
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
            return new Target(ROCKET_MULTIPLIER, unit.getMaplocation().distanceBFSTo(mLoc), mLoc, 0);
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