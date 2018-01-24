

import bc.UnitType;

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


    AuxMapLocation getBestUnit(AuxMapLocation loc){
        try {
            double minDist = 500000;
            AuxMapLocation ans = null;
            for (int i = 0; i < Units.myUnits.length; ++i) {
                AuxUnit u = Units.myUnits[i];
                if (!Units.structures.contains(i)) {
                    if (u.getHealth() < Units.getMaxHealth(u.getType())) {
                        AuxMapLocation mLoc = u.getMapLocation();
                        if (mLoc != null) {
                            double d = mLoc.distanceBFSTo(loc);
                            if (d < minDist) {
                                ans = mLoc;
                                minDist = d;
                            }
                        }
                    }
                }
            }
            return ans;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}