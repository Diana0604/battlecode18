import bc.UnitType;

import java.util.HashMap;
import java.util.Map;

public class Worker {
    private static final int dist_offset = 5;
    static final int MAX_FACTORIES = 5;
    private static HashMap<Integer, Double> targets = new HashMap<>();
    private static final int MAX_WORKERS_EARTH = 40;
    private static final int MAX_WORKERS_MARS = 15;
    private static final int MAX_DISTANCE_REPLICATE_MARS = 20; //max distance from asteroid


    /*----------- INIT TURN ------------*/


    static double hasTarget(AuxUnit unit){
        if (!targets.containsKey(unit.getID())) return 0;
        return 0.5 - 0.001*targets.get(unit.getID());
    }



    /*----------- ACTIONS ------------*/

    public static void doAction(AuxUnit unit, boolean firstTime){
        try {
            boolean acted;
            //boolean willReplicate = shouldReplicate(unit) && canReplicate(unit);
            if (unit.canAttack()) {
                acted = tryBuildAndRepair(unit);
                if (!acted && firstTime) acted = tryPlaceBlueprint(unit);
                if (!acted) tryMine(unit);
            }
            if (!firstTime) tryReplicate(unit);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*----------- REPLICATE ------------*/


    private static boolean tryReplicate(AuxUnit unit){
        try {
            if (!unit.canUseAbility()) return false;
            if (shouldReplicate(unit)) {
                int dir = 0;
                if (unit.target != null){
                    dir = unit.getMapLocation().dirBFSTo(unit.target);
                }
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

    private static boolean tryReplicateDir(AuxUnit unit, int dir){
        try {
            if (Wrapper.canReplicate(unit, dir)) {

                AuxUnit newWorker = Wrapper.replicate(unit, dir);
                Units.unitTypeCount.put(UnitType.Worker, Units.unitTypeCount.get(UnitType.Worker) + 1);
                WorkerUtil.extra_workers++;
                WorkerUtil.workersCreated++;

                int index = Units.myUnits.size();
                int newID = newWorker.getID();
                Units.myUnits.add(newWorker);
                Units.allUnits.put(newID, index);
                Units.robots.add(index);
                Units.workers.add(index);
                Units.unitMap[newWorker.getX()][newWorker.getY()] = index + 1;
                WorkerUtil.hasReplicated = true;
                newWorker.target = unit.target;
                if (newWorker.target == null) newWorker.target = getTarget(newWorker);
                doAction(newWorker, true);
                UnitManager.move(newWorker);
                doAction(newWorker, false);
                return true;
            }
            return false;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private boolean canReplicate(AuxUnit unit){
        try {
            for (int i = 0; i < 8; ++i) {
                if (Wrapper.canReplicate(unit, i)) return true;
            }
            return false;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private static boolean shouldReplicate(AuxUnit unit){
        try {
            if (Mapa.onEarth()) return shouldReplicateEarth(unit);
            else return shouldReplicateMars(unit);
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private static boolean shouldReplicateEarth(AuxUnit unit){
        try {
            if (WorkerUtil.hasReplicated) return false;
            if (Units.workers.size() > MAX_WORKERS_EARTH) return false; //evita timeout
            Danger.computeDanger(unit);
            boolean danger = (Danger.DPSlong[8] > 0);
            if (danger) return false;
            int nb_actions = WorkerUtil.getWorkerActions(unit.getMapLocation(), 30);
            if (WorkerUtil.workersCreated < WorkerUtil.min_nb_workers) return true;
            if (WorkerUtil.workersCreated <  WorkerUtil.min_nb_workers1) return (nb_actions >= 15);
            return nb_actions >= 35;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean shouldReplicateMars(AuxUnit unit){
        try {
            if (Utils.round > 750) return true;
            if (Utils.karbonite > 400) return true;
            if (Units.workers.size() >= MAX_WORKERS_MARS) return false;
            //if (Utils.karbonite < 40) return false; //no se si aixo va be o no
            HashMap<Integer, Integer> tasks = Karbonite.asteroidTasksLocs;
            HashMap<Integer, Integer> karboAt = Karbonite.karboniteAt;
            for(Integer encoding: karboAt.keySet()){
                if (!tasks.containsKey(encoding)) continue; //no hauria de passar mai pero just in case
                int assignedID = tasks.get(encoding);
                AuxMapLocation loc = new AuxMapLocation(encoding);
                int dist = unit.getMapLocation().distanceBFSTo(loc);
                if (!Units.allUnits.containsKey(assignedID) && dist < MAX_DISTANCE_REPLICATE_MARS) return true; //ha trobat una mina sense assignar, crea worker per enviar-li
            }
            return false;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /*----------- BUILD/REPAIR ------------*/


    //Intenta reparar o construir una structure adjacent
    private static boolean tryBuildAndRepair(AuxUnit unit){
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
                unit.canMove = false; // ho he mogut perque quan un worker acaba un rocket tampoc es mogui (aixi se l'emporta a mars)
                if (structure.getHealth() < Units.getMaxHealth(structure.getType())){
                    targets.put(unit.getID(), 100.0);
                }
                return true;
            }
            if (minHPIndex >= 0) {
                AuxUnit structure = adjUnits[minHPIndex];
                Wrapper.repair(unit, adjUnits[minHPIndex]);
                if (structure.getHealth() < Units.getMaxHealth(structure.getType())){
                    unit.canMove = false;
                    targets.put(unit.getID(), 100.0);
                }
                return true;
            }
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /*----------- PLACE BLUEPRINT ------------*/


    private static boolean canWait(){
        try {
            if (Mapa.onMars()) return true;
            if (!WorkerUtil.safe) return false;
            if (Units.unitTypeCount.get(UnitType.Worker) < WorkerUtil.min_nb_workers) return true;
            if (Utils.round < WorkerUtil.minSafeTurns) {
                if (Danger.enemySeen) return false;
                if (WorkerUtil.totalKarboCollected < 0.8*WorkerUtil.approxMapValue) return true;
            }

            return false;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    //Posen un blueprint en una posicio adjacent (aixo s'ha de canviar quan ho fem global)
    public static boolean tryPlaceBlueprint(AuxUnit unit){
        try {
            if (!unit.canAttack()) return false;
            if (canWait()) return false;
            UnitType type = Build.nextStructureType;
            if (type == null) return false;
            if (Utils.karbonite < Units.getCost(type)) return false;
            AuxMapLocation myLoc = unit.getMapLocation();
            AuxMapLocation blueprintLoc = Build.nextStructureLocation;
            if (myLoc.distanceSquaredTo(blueprintLoc) > 2) return false;
            AuxUnit rip = blueprintLoc.getUnit();
            if (rip != null && MovementManager.getInstance().move(rip) == 8) Wrapper.disintegrate(rip);
            int dir = myLoc.dirBFSTo(blueprintLoc);
            //if (!Wrapper.canPlaceBlueprint(unit, type, dir)) return false;
            Wrapper.placeBlueprint(unit, type, dir);
            Units.unitTypeCount.put(type, Units.unitTypeCount.get(type)+1);
            unit.canMove = false;
            targets.put(unit.getID(), 100.0);
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /*----------- MINE ------------*/


    //minen una mina adjacent
    private static boolean tryMine(AuxUnit unit){
        try {
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


    /*-------------- CHOOSE TARGET ---------------*/

    static AuxMapLocation getTarget(AuxUnit unit){
        if (Mapa.onEarth()) return earthTarget(unit);
        return marsTarget(unit);
    }

    private static AuxMapLocation earthTarget(AuxUnit unit){
        try {
            AuxMapLocation targetLoc = null;
            double priority = 0;
            for (int i = 0; i < WorkerUtil.importantLocations.size(); ++i){
                AuxMapLocation loc = WorkerUtil.importantLocations.get(i);
                int dist = loc.distanceBFSTo(unit.getMapLocation());
                if (dist > 10000) continue;
                double newPriority = WorkerUtil.workerActionsExpanded[loc.x][loc.y];
                newPriority /= (WorkerUtil.workersDeployed[loc.x][loc.y]+1);
                newPriority /= (dist + dist_offset);
                if (newPriority > priority) {
                    if (Utils.round < 150 || MovementManager.getInstance().kamikazeWorker() || !loc.isDangerousForWorker()) {

                        priority = newPriority;
                        targetLoc = loc;
                    }

                    else{
                        //System.out.println("Cannot go to " + loc.x + " " + loc.y);
                        //System.out.println("My Distance: " + Danger.myDist[loc.x][loc.y]);
                        //System.out.println("Enemy Distance: " + Danger.enemyDist[loc.x][loc.y]);
                    }

                }
            }

            if (targetLoc != null){
                WorkerUtil.addWorkers(targetLoc, 1);
                WorkerUtil.workerCont++;
                //targets.put(unit.getID(), (double)unit.getMapLocation().distanceBFSTo(targetLoc));
                targets.put(unit.getID(), priority);
                return targetLoc;
            }

            targetLoc = getBackLine();
            return targetLoc;

            //System.out.println("I got target " + targetLoc.x + " " + targetLoc.y);


        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static AuxMapLocation getBackLine(){
        try {
            double mostDanger = 0;
            AuxMapLocation ans = null;

            for (int i = 0; i < Units.myUnits.size(); ++i){
                AuxUnit unit = Units.myUnits.get(i);
                if (unit.isInSpace() || unit.isInGarrison()) continue;
                AuxMapLocation loc = Units.myUnits.get(i).getMapLocation();
                double dang = loc.getDanger();
                if (!loc.isDangerousForWorker() && dang > mostDanger){
                    mostDanger = dang;
                    ans = loc;
                }
            }
            return ans;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static AuxMapLocation marsTarget(AuxUnit unit){
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
                    if (dist >= Const.INFS) continue; //si no esta passable
                    if (dist < minDist) {
                        minDist = dist;
                        minLoc = loc;
                    }
                }
                if (minLoc == null || minDist > 20){
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
                    //ha trobat una mina buida a distancia < 20, se l'assigna
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
}