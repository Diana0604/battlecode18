import bc.*;

import java.util.HashMap;

class UnitManager{
    static void moveUnits() {
        try {
            Mage.getInstance().computeMultiTarget();
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
        for (AuxUnit unit: Units.myUnits) count[unit.getType().swigValue()]++;
        HashMap<UnitType, Integer> mapa = new HashMap<UnitType, Integer>();
        for (UnitType type: UnitType.values()) mapa.put(type,count[type.swigValue()]);
        Units.unitTypeCount = mapa;
    }

    static void selectTargets(){
        for (int i = 0; i < Units.myUnits.length; i++) {
            //System.err.println("playing unit " + GC.myUnits[i].getType());
            AuxUnit unit = Units.myUnits[i];
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

        int dirBFS = unit.getMapLocation().dirBFSTo(unit.target);
        if (dirBFS == 8) return 8;

        //can move :)
        if (Wrapper.canMove(unit, dirBFS)){
            Wrapper.moveRobot(unit, dirBFS);
            MovementManager.getInstance().reset(unit);
            return dirBFS;
        }

        AuxMapLocation newLoc = unit.getMapLocation().add(dirBFS);
        AuxUnit u = GC.getUnit(newLoc.x, newLoc.y, true);
        if (u != null){
            if (move(u) != 8){
                Wrapper.moveRobot(unit, dirBFS);
                MovementManager.getInstance().reset(unit);
                return dirBFS;
            }
        }

        int dir = MovementManager.getInstance().move(unit, unit.target);
        if (dir != 8){
            Wrapper.moveRobot(unit, dir);
        }

        return dir;
        */
        MovementManager.getInstance().move(unit);
        return 0;
    }

    static void moveAllUnits(){
        for (int i = 0; i < Units.myUnits.length; i++) {

            AuxUnit unit = Units.myUnits[i];
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
        Overcharge.generateMatrix();
        for (int i = 0; i < Units.myUnits.length; i++) {
            //System.err.println("playing unit " + GC.myUnits[i].getType());
            AuxUnit unit = Units.myUnits[i];
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
        Overcharge.generateMatrix();
        for (int i = 0; i < Units.myUnits.length; i++) {
            //System.err.println("playing unit " + GC.myUnits[i].getType());
            AuxUnit unit = Units.myUnits[i];
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
}
