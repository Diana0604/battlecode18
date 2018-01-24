import bc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

class UnitManager{
    private static UnitManager instance;
    static GameController gc;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    UnitManager(){
        try{
            Data.initGame(gc);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    void moveUnits() {
        try {

            Danger.reset();

            Mage.getInstance().computeMultiTarget();
            for (int i = 0; i < Data.myUnits.length; i++) {
                AuxUnit unit = Data.myUnits[i];
                //if (unit.getType() == UnitType.Factory) System.err.println("I'm a factory! :D");
                if (unit.getType() == UnitType.Rocket) {
                    Rocket.getInstance().playFirst(unit);
                }
            }

            selectTargets();
            actUnits();
            moveAllUnits();
            actUnits2();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

// de moment aixo no cal pq ja ho fem en un altre lloc
    static void countUnits(){
        int[] count = {0,0,0,0,0,0,0};
        for (AuxUnit unit: Data.myUnits) count[unit.getType().swigValue()]++;
        HashMap<UnitType, Integer> mapa = new HashMap<UnitType, Integer>();
        for (UnitType type: UnitType.values()) mapa.put(type,count[type.swigValue()]);
        Data.unitTypeCount = mapa;
    }

    static void selectTargets(){
        for (int i = 0; i < Data.myUnits.length; i++) {
            //System.err.println("playing unit " + Data.myUnits[i].getType());
            AuxUnit unit = Data.myUnits[i];
            if (unit.isInGarrison()) continue;
            if (unit.getType() == UnitType.Worker) {
                unit.target = Worker.getInstance().getTarget(unit);
            }
            if (unit.getType() == UnitType.Ranger || unit.getType() == UnitType.Knight) { //LOLZ
                unit.target = Ranger.getInstance().getTarget(unit);
            }
            if (unit.getType() == UnitType.Healer) {
                unit.target = Healer.getInstance().getTarget(unit);
            }
            if (unit.getType() == UnitType.Mage) {
                unit.target = Mage.getInstance().getTarget(unit);
            }
        }
    }

    static int move(AuxUnit unit){
        /*
        if (unit.visited) return 8;
        unit.visited = true;

        int dirBFS = unit.getMaplocation().dirBFSTo(unit.target);
        if (dirBFS == 8) return 8;

        //can move :)
        if (Wrapper.canMove(unit, dirBFS)){
            Wrapper.moveRobot(unit, dirBFS);
            MovementManager.getInstance().reset(unit);
            return dirBFS;
        }

        AuxMapLocation newLoc = unit.getMaplocation().add(dirBFS);
        AuxUnit u = Data.getUnit(newLoc.x, newLoc.y, true);
        if (u != null){
            if (move(u) != 8){
                Wrapper.moveRobot(unit, dirBFS);
                MovementManager.getInstance().reset(unit);
                return dirBFS;
            }
        }

        int dir = MovementManager.getInstance().moveTo(unit, unit.target);
        if (dir != 8){
            Wrapper.moveRobot(unit, dir);
        }

        return dir;
        */
        MovementManager.getInstance().moveTo(unit);
        return 0;
    }

    static void moveAllUnits(){
        for (int i = 0; i < Data.myUnits.length; i++) {

            AuxUnit unit = Data.myUnits[i];
            if (unit.isInGarrison()) continue;
            if (!unit.canMove()) continue;
            if (unit.target == null) continue;

            if (unit.getType() == UnitType.Factory || unit.getType() == UnitType.Rocket) {
                continue;
            }

            move(unit);
        }
    }


    static void actUnits(){
        for (int i = 0; i < Data.myUnits.length; i++) {
            //System.err.println("playing unit " + Data.myUnits[i].getType());
            AuxUnit unit = Data.myUnits[i];
            if (unit.isInGarrison()) continue;
            if (unit.getType() == UnitType.Worker) {
                Worker.getInstance().doAction(unit);
            }
            if (unit.getType() == UnitType.Ranger || unit.getType() == UnitType.Knight) { //LOLZ
                Ranger.getInstance().attack(unit);
            }
            if (unit.getType() == UnitType.Healer) {
                Healer.getInstance().heal(unit);
            }
            if (unit.getType() == UnitType.Mage) {
                Mage.getInstance().doAction(unit);
            }
        }
    }

    static void actUnits2(){
        for (int i = 0; i < Data.myUnits.length; i++) {
            //System.err.println("playing unit " + Data.myUnits[i].getType());
            AuxUnit unit = Data.myUnits[i];
            if (unit.isInGarrison()) continue;
            if (unit.getType() == UnitType.Worker) {
                Worker.getInstance().doAction(unit);
            }
            if (unit.getType() == UnitType.Factory) {
                Factory.getInstance().play(unit);
            }
            if (unit.getType() == UnitType.Ranger || unit.getType() == UnitType.Knight) { //LOLZ
                Ranger.getInstance().attack(unit);
            }
            if (unit.getType() == UnitType.Rocket) {
                Rocket.getInstance().play(unit);
            }
            if (unit.getType() == UnitType.Healer) {
                Healer.getInstance().heal(unit);
            }
            if (unit.getType() == UnitType.Mage) {
                Mage.getInstance().doAction(unit);
            }
        }
    }


    static void initWorkerMaps(){
        try {
            Data.blueprintsToBuild = new HashMap<>();
            Data.blueprintsToPlace = new HashMap<>();
            Data.structuresToRepair = new HashMap<>();
            HashSet<AuxUnit> workers = new HashSet<>();
            for (AuxUnit unit : Data.myUnits) {
                if (unit.getType() == UnitType.Worker) workers.add(unit);
            }

            for (int index : Data.structures) {
                //Per cada blueprint, crida els 8 workers mes propers a construir-lo
                AuxUnit bp = Data.myUnits[index];
                if (bp.isMaxHealth()) continue;
                if (!bp.isBlueprint()) continue;

                final int WORKERS_TO_CALL = 6;
                ArrayList<Pair> sorted = new ArrayList<>();
                for (AuxUnit worker : workers) {
                    AuxMapLocation workerloc = worker.getMaplocation();
                    if (workerloc == null) continue;
                    Pair p = new Pair(bp.getMaplocation().distanceSquaredTo(worker.getMaplocation()), worker);
                    sorted.add(p);
                }
                sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
                int workersCalled = Math.min(sorted.size() - 1, Math.min(WORKERS_TO_CALL, sorted.size()));
                if (workersCalled <= 0) workersCalled = 1;
                List<Pair> cut = sorted.subList(0, workersCalled); //no se si funciona si hi ha menys de 8 workers total
                for (Pair p : cut) {
                    int key = p.unit.getID();
                    int value = bp.getID();
                    if (!Data.blueprintsToBuild.containsKey(key)) Data.blueprintsToBuild.put(key, value);
                }
            }

            for (int index : Data.structures) {
                //Per cada blueprint, crida els 8 workers mes propers a construir-lo
                AuxUnit s = Data.myUnits[index];
                if (s.isMaxHealth()) continue;
                if (s.isBlueprint()) continue;

                final int WORKERS_TO_CALL = 3;
                ArrayList<Pair> sorted = new ArrayList<>();
                for (AuxUnit worker : workers) {
                    //no fiquem workers si ja son cridats per un blueprint
                    if (Data.blueprintsToBuild.containsKey(worker.getID())) continue;
                    AuxMapLocation workerLoc = worker.getMaplocation();
                    if (workerLoc == null) continue;
                    Pair p = new Pair(s.getMaplocation().distanceSquaredTo(worker.getMaplocation()), worker);
                    sorted.add(p);
                }
                sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
                List<Pair> cut = sorted.subList(0, Math.min(WORKERS_TO_CALL, sorted.size())); //no se si funciona si hi ha menys de 8 workers total
                for (Pair p : cut) {
                    int key = p.unit.getID();
                    int value = s.getID();
                    if (!Data.structuresToRepair.containsKey(key)) Data.structuresToRepair.put(key, value);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    private static class Pair {
        double dist;
        AuxUnit unit;

        Pair(double dist, AuxUnit unit){
            this.dist = dist;
            this.unit = unit;
        }
    }

}
