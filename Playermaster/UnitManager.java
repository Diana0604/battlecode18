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
        try {
            int[] count = {0,0,0,0,0,0,0};
            for (AuxUnit unit: Units.myUnits) count[unit.getType().swigValue()]++;
            HashMap<UnitType, Integer> mapa = new HashMap<UnitType, Integer>();
            for (UnitType type: UnitType.values()) mapa.put(type,count[type.swigValue()]);
            Units.unitTypeCount = mapa;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void selectTargets(){
        try {
            for (AuxUnit unit: Units.myUnits) {
                if (unit.isInSpace()) continue;
                //System.err.println("playing unit " + GC.myUnits[i].getType());
                //if (unit.isInGarrison()) continue;
                if (unit.getType() == UnitType.Worker) {
                    unit.target = Worker.getTarget(unit);
                }
                if (unit.getType() == UnitType.Ranger) {
                    unit.target = Ranger.getInstance().getTarget(unit);
                }
                if (unit.getType() == UnitType.Knight) {
                    unit.target = Knight.getInstance().getTarget(unit);
                }
                if (unit.getType() == UnitType.Healer) {
                    unit.target = Healer.getInstance().getTarget(unit);
                }
                if (unit.getType() == UnitType.Mage) {
                    unit.target = Mage.getInstance().getTarget(unit);
                }



            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void move(AuxUnit unit){
        try {
            if (unit.isInGarrison() || unit.isInSpace()) return;
            if (unit.getType() == UnitType.Factory || unit.getType() == UnitType.Rocket) {
                return;
            }
            if (!unit.canMove()) return;
            if (unit.target == null) unit.target = unit.getMapLocation();
            MovementManager.getInstance().move(unit);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void moveAllUnits(){
        try {
            for (AuxUnit unit: Units.myUnits) {
                move(unit);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    static void actUnits(){
        try {
            Overcharge.generateMatrix();
            AuxUnit[] units = Units.myUnits.toArray(new AuxUnit[Units.myUnits.size()]);
            for (AuxUnit unit : units) {
                //System.err.println("playing unit " + GC.myUnits[i].getType());
                if (unit.isInGarrison() || unit.isInSpace()) continue;
                if (unit.getType() == UnitType.Factory) {
                    Factory.getInstance().play(unit);
                }
                if (unit.getType() == UnitType.Worker) {
                    //Worker.getInstance().doAction(unit, true);
                }
                if (unit.getType() == UnitType.Ranger) {
                    Ranger.getInstance().attack(unit);
                }
                if (unit.getType() == UnitType.Knight) {
                    Knight.getInstance().attack(unit);
                }
                if (unit.getType() == UnitType.Healer) {
                    Healer.getInstance().heal(unit);
                }
                if (unit.getType() == UnitType.Mage) {
                    Mage.getInstance().doAction(unit);
                }
            }
            WorkerUtil.doFirstActions();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void actUnits2(){
        try {
            Overcharge.generateMatrix();
            AuxUnit[] units = Units.myUnits.toArray(new AuxUnit[Units.myUnits.size()]);
            for (AuxUnit unit : units) {
                //System.err.println("playing unit " + GC.myUnits[i].getType());
                if (unit.isInGarrison() || unit.isInSpace()) continue;
                if (unit.getType() == UnitType.Factory) {
                    Factory.getInstance().play(unit);
                }
                if (unit.getType() == UnitType.Worker) {
                    Worker.doAction(unit, false);
                }
                if (unit.getType() == UnitType.Ranger) {
                    Ranger.getInstance().attack(unit);
                }
                if (unit.getType() == UnitType.Knight) {
                    Knight.getInstance().attack(unit);
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
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean factoriesFirst(){
        return true;
    }

}
