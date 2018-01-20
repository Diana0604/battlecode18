import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;

public class Worker {

    static Worker instance = null;

    static Worker getInstance(){
        if (instance == null) instance = new Worker();
        return instance;
    }

    public Worker(){
        WorkerUtil.computeApproxMapValue();
    }

    private AuxUnit unit;
    boolean danger;
    boolean wait;

    void play(AuxUnit _unit){
        try {
            //System.out.println(Data.workers);
            unit = _unit;
            wait = false;
            Danger.computeDanger(unit);
            danger = (Danger.DPS[8] > 0);
            boolean acted = doAction();
            if (!wait && unit.canMove()) {
                move();
                if (!acted) doAction();
            }
        }catch(Exception e) {
            System.out.println(e);
        }
    }

    /*----------- ACTIONS ------------*/

    boolean doAction(){
        try {
            if (!unit.canAttack()) return true;
            if (tryReplicate()) return true;
            if (tryBuildAndRepair()) return true;
            if (tryPlaceBlueprint()) {
                wait = true;
                return true;
            }
            if (tryMine()) return true;
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    boolean tryReplicate(){
        try {
            if (Data.queue.needsUnit(UnitType.Worker) || shouldReplicate()) {
                for (int i = 0; i < 8; ++i) {
                    if (Wrapper.canReplicate(unit, i)) {
                        Wrapper.replicate(unit, i);
                        Data.workers++;
                        WorkerUtil.extra_workers++;
                        Data.queue.requestUnit(UnitType.Worker, false);
                        return true;
                    }
                }
            }
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    boolean shouldReplicate(){
        try {
            if (danger) return false;
            int nb_actions = WorkerUtil.getWorkerActions(unit.getMaplocation(), 30);
            if (Data.onMars() || Data.workers < WorkerUtil.min_nb_workers) return (nb_actions >= 12);
            return (nb_actions >= 30);
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    boolean tryBuildAndRepair(){
        try {
            int minDif = 1000;
            int minDifIndex = -1;
            int minHP = 1000;
            int minHPIndex = -1;
            AuxUnit[] adjUnits = Wrapper.senseUnits(unit.getMaplocation(), 2, true);
            for (int i = 0; i < adjUnits.length; ++i) {
                AuxUnit u = adjUnits[i];
                if (!(u.getType() == UnitType.Factory || u.getType() == UnitType.Rocket)) continue;
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
                if (structure.getHealth() < Wrapper.getMaxHealth(structure.getType())) wait = true;
                return true;
            }
            if (minHPIndex >= 0) {
                AuxUnit structure = adjUnits[minHPIndex];
                Wrapper.repair(unit, adjUnits[minHPIndex]);
                if (structure.getHealth() < Wrapper.getMaxHealth(structure.getType())) wait = true;
                return true;
            }
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    boolean tryPlaceBlueprint(){
        try {
            if (danger) return false;
            UnitType type = null;
            if (Data.researchInfo.getLevel(UnitType.Rocket) > 0 && Data.queue.needsUnit(UnitType.Rocket))
                type = UnitType.Rocket;
            if (type == null && Data.queue.needsUnit(UnitType.Factory)) type = UnitType.Factory;
            if (type == null) return false;
            for (int i = 0; i < 8; ++i) {
                if (Danger.DPS[i] > 0) continue;
                if (!Wrapper.canPlaceBlueprint(unit, type, i)) continue;
                Wrapper.placeBlueprint(unit, type, i);
                Data.queue.requestUnit(type, false);
                wait = true;
                return true;
            }
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    boolean tryMine(){
        try {
            System.out.println("Trying to mine! " + unit.getID());
            int dir = WorkerUtil.getMostKarboLocation(unit.getMaplocation());
            AuxMapLocation newLoc = unit.getMaplocation().add(dir);
            if (Data.karboMap[newLoc.x][newLoc.y] > 0 && Wrapper.canHarvest(unit, dir)) {
                Wrapper.harvest(unit, dir);
                System.out.println(newLoc.x + " " + newLoc.y);
                return true;
            }
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }



    /*-------------- MOVEMENT ---------------*/

    void move(){
        try {
            if (!unit.canMove()) return;

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


            if (targets.size() > 0) {
                Target bestTarget = targets.get(0);
                MovementManager.getInstance().moveTo(unit, bestTarget.mloc);
            }
        }catch(Exception e) {
            System.out.println(e);
        }
    }

    Target getKarboniteTarget(){
        try {
            Target ans = null;
            for (HashMap.Entry<Integer, Integer> entry : Data.karboniteAt.entrySet()) {
                AuxMapLocation mineLoc = Data.toLocation(entry.getKey());
                if (WorkerUtil.blockingBuilding(mineLoc)) continue;
                double d = unit.getMaplocation().distanceBFSTo(mineLoc);
                if (ans == null) {
                    ans = new Target(Math.min(1000, entry.getValue()), d, mineLoc, 3);
                    continue;
                }
                Target aux = new Target(Math.min(1000, entry.getValue()), d, mineLoc, 3);
                if (targetEval(aux) < targetEval(ans)) ans = aux;
            }
            return ans;
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    Target getBuildTarget(){
        try {
            double minDist = 1000000;
            Target ans = null;
            for (int a : Data.structures) {
                AuxUnit u = Data.myUnits[a];
                if (!u.isBlueprint()) continue;
                AuxMapLocation mloc = u.getMaplocation();
                double d = Math.max(unit.getMaplocation().distanceBFSTo(mloc) - 1, 0);
                int dif = (Wrapper.getMaxHealth(u.getType()) - u.getHealth()) - (int) (d * WorkerUtil.senseWorkers(u.getMaplocation()) * Data.buildingPower);
                if (dif > 0 && d < minDist) {
                    minDist = d;
                    ans = new Target(dif * 10 / Data.buildingPower, d, mloc, 2);
                }
            }
            return ans;
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    Target getRepairTarget(){
        try {
            double minDist = 1000000;
            Target ans = null;
            for (int a : Data.structures) {
                AuxUnit u = Data.myUnits[a];
                if (u.isBlueprint()) continue;
                AuxMapLocation mloc = u.getMaplocation();
                double d = Math.max(unit.getMaplocation().distanceBFSTo(mloc) - 1, 0);
                int dif = (Wrapper.getMaxHealth(u.getType()) - u.getHealth()) - (int) (d * WorkerUtil.senseWorkers(u.getMaplocation()) * Data.repairingPower);
                if (dif > 0 && d < minDist) {
                    minDist = d;
                    ans = new Target(dif * 2 / Data.repairingPower, d, mloc, 1);
                }
            }
            return ans;
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    Target getRocketTarget() {
        try {
            AuxMapLocation mloc = Rocket.callsToRocket.get(unit.getID());
            if (mloc == null) return null;
            return new Target(10000000, unit.getMaplocation().distanceBFSTo(mloc), mloc, 0);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }



    double targetEval(Target a){
        try {
            return -(a.value / (a.dist + 10));
        }catch(Exception e) {
            System.out.println(e);
            return Double.parseDouble(null);
        }
    }

    private class Target {
        double value;
        double dist;
        AuxMapLocation mloc;
        int type;

        public Target(double v, double d, AuxMapLocation loc, int _type){
            value = v;
            dist = d;
            mloc = loc;
            type = _type;
        }

    }

}