

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

    void heal(AuxUnit unit){
        try {
            if (!unit.canAttack()) return;
            //System.err.println("trying to heal");
            AuxUnit[] v = Wrapper.senseUnits(unit.getX(), unit.getY(), 30, true);
            long maxDiff = 0;
            AuxUnit healed = null;
            for (int i = 0; i < v.length; ++i) {
                if (v[i].getType() == UnitType.Factory || v[i].getType() == UnitType.Rocket) continue;
                AuxUnit u = v[i];
                long d = Units.getMaxHealth(u.getType()) - u.getHealth();
                if (d > maxDiff) {
                    maxDiff = d;
                    healed = u;
                }
            }
            if (healed != null) {
                Wrapper.heal(unit, healed);
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
                if (!(u.getType() == UnitType.Ranger)) continue;
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
        //if (d1 > d2) return unit1; //triem la tropa mes llunyana (?)
        if (d1 < d2) return unit1;
        return unit2;
    }
}