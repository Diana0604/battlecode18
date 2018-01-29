

import bc.UnitType;

import javax.naming.AuthenticationNotSupportedException;
import java.util.HashMap;

public class Healer {
    static Healer instance = null;

    HashMap<Integer, Integer> objectiveArea;

    static Healer getInstance(){
        if (instance == null){
            instance = new Healer();
        }
        return instance;
    }

    public Healer(){
        objectiveArea = new HashMap();
    }


    private int typePriority(UnitType t){
        switch(t){
            case Mage: return 4;
            case Knight: return 3;
            case Ranger: return 2;
            case Healer: return 2;
            case Worker: return 1;
            default: return 0;
        }
    }

    private AuxUnit compareTargets(AuxUnit A, AuxUnit B){
        try {
            if (A == null) return B;
            if (B == null) return A;

            boolean maxHealth1 = A.isMaxHealth(), maxHealth2 = B.isMaxHealth();
            if (maxHealth1 && !maxHealth2) return B;
            if (maxHealth2 && !maxHealth1) return A;

            if (A.frontline && !B.frontline) return A;
            if (!A.frontline && B.frontline) return B;

            if (typePriority(A.getType()) > typePriority(B.getType())) return A;
            if (typePriority(A.getType()) < typePriority(B.getType())) return B;

            if (A.getHealth() < B.getHealth()) return A;
            return B;
        }catch(Exception e) {
            e.printStackTrace();
            return B;
        }
    }

    void heal(AuxUnit healer){
        try {
            if (!healer.canAttack()) return;
            //System.err.println("trying to heal");
            AuxUnit[] v = Wrapper.senseUnits(healer.getX(), healer.getY(), 30, true);
            AuxUnit healed = null;
            for (AuxUnit unit : v) {
                if (unit.isStructure()) continue;
                healed = compareTargets(healed, unit);
            }
            if (healed != null) {
                Wrapper.heal(healer, healed);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    AuxMapLocation getTarget(AuxUnit unit) {
        try {
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
            AuxMapLocation ans = getBestUnit(unit.getMapLocation());
            if (ans != null) return ans;
            return Explore.findExploreObjective(unit);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private AuxMapLocation getBestUnit(AuxMapLocation loc){
        try {
            AuxUnit bestUnit = null;
            for (int index: Units.robots){
                AuxUnit u = Units.myUnits.get(index);
                if (!u.frontline) continue;
                if (!(u.getType() == UnitType.Ranger) && !(u.getType() == UnitType.Mage) && !(u.getType() == UnitType.Knight)) continue;
                //if (bestUnit == null || loc.distanceBFSTo(bestUnit.getMapLocation()) > loc.distanceBFSTo(u.getMapLocation())) bestUnit = u;
                bestUnit = compareUnits(loc, bestUnit, u);
            }
            if (bestUnit == null) return null;
            return bestUnit.getMapLocation();
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private AuxUnit compareUnits(AuxMapLocation myLoc, AuxUnit unit1, AuxUnit unit2){
        try {
            if (unit1 == null) return unit2;
            if (unit2 == null) return unit1;

            if (unit1.frontline && !unit2.frontline) return unit1;
            if (!unit1.frontline && unit2.frontline) return unit2;


            boolean maxHealth1 = unit1.isMaxHealth(), maxHealth2 = unit2.isMaxHealth();
            if (maxHealth1 && !maxHealth2) return unit2;
            if (maxHealth2 && !maxHealth1) return unit1;

            if (unit1.isTroop() && !unit2.isTroop()) return unit1;
            if (!unit1.isTroop() && unit2.isTroop()) return unit2;

            double d1 = myLoc.distanceBFSTo(unit1.getMapLocation());
            double d2 = myLoc.distanceBFSTo(unit2.getMapLocation());
            if (d1 < d2) return unit1;
            return unit2;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}