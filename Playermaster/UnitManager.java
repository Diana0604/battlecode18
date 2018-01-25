import bc.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
        for (AuxUnit unit: Units.myUnits) {
            if (unit.isInSpace()) continue;
            //System.err.println("playing unit " + GC.myUnits[i].getType());
            //if (unit.isInGarrison()) continue;
            if (unit.getType() == UnitType.Worker) {
                unit.target = Worker.getTarget(unit);
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

    static void move(AuxUnit unit){
        if (unit.isInGarrison() || unit.isInSpace()) return;
        if (unit.getType() == UnitType.Factory || unit.getType() == UnitType.Rocket) {
            return;
        }
        if (!unit.canMove()) return;
        if (unit.target == null){
            for (int i = 0; i < 8; ++i){
                if (Wrapper.canMove(unit, i)){
                    Wrapper.moveRobot(unit, i);
                    return;
                }
            }
            return;
        }
        MovementManager.getInstance().move(unit);
    }

    static void moveAllUnits(){
        for (AuxUnit unit: Units.myUnits) move(unit);
    }


    static void actUnits(){
        Overcharge.generateMatrix();
        AuxUnit[] units = Units.myUnits.toArray(new AuxUnit[Units.myUnits.size()]);
        for (AuxUnit unit : units) {
            //System.err.println("playing unit " + GC.myUnits[i].getType());
            if (unit.isInGarrison() || unit.isInSpace()) continue;
            if (unit.getType() == UnitType.Worker) {
                Worker.doAction(unit, true);
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
        AuxUnit[] units = Units.myUnits.toArray(new AuxUnit[Units.myUnits.size()]);
        for (AuxUnit unit : units) {
            //System.err.println("playing unit " + GC.myUnits[i].getType());
            if (unit.isInGarrison() || unit.isInSpace()) continue;
            if (unit.getType() == UnitType.Worker) {
                Worker.doAction(unit, false);
            }
            if (unit.getType() == UnitType.Factory) {
                Factory.getInstance().play(unit);
            }
            if (unit.getType() == UnitType.Ranger || unit.getType() == UnitType.Knight) { //LOLZ
                Ranger.getInstance().attack(unit);
            }
            if (unit.getType() == UnitType.Rocket) {
                Rocket.play(unit);
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
