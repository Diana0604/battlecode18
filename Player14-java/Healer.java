

import bc.UnitType;

import java.util.HashMap;

public class Healer {
    final long INFL = 1000000000;
    final int INF = 1000000000;
    final double eps = 0.001;
    static Healer instance = null;
    private UnitManager unitManager;

    HashMap<Integer, Integer> objectiveArea;

    static Healer getInstance(){
        if (instance == null){
            instance = new Healer();
        }
        return instance;
    }

    public Healer(){
        unitManager = UnitManager.getInstance();
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
                long d = Wrapper.getMaxHealth(u.getType()) - u.getHealth();
                if (d > maxDiff) {
                    maxDiff = d;
                    healed = u;
                }
            }
            if (healed != null) {
                Wrapper.heal(unit, healed);
            }
        }catch(Exception e) {
            System.out.println(e);
        }
    }

    void play(AuxUnit unit){
        try {
            heal(unit);
            move(unit);
            heal(unit);
        }catch(Exception e) {
            System.out.println(e);
        }
    }



    void move(AuxUnit unit) {
        try {
            AuxMapLocation target = getBestTarget(unit);
            if (target != null) MovementManager.getInstance().moveTo(unit, target);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    AuxMapLocation getBestTarget(AuxUnit unit) {
        try {
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
            return getBestUnit(unit.getMaplocation());
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }


    AuxMapLocation getBestUnit(AuxMapLocation loc){
        try {
            double minDist = 500000;
            AuxMapLocation ans = null;
            for (int i = 0; i < Data.myUnits.length; ++i) {
                AuxUnit u = Data.myUnits[i];
                if (!Data.structures.contains(i)) {
                    if (u.getHealth() < Wrapper.getMaxHealth(u.getType())) {
                        AuxMapLocation mLoc = u.getMaplocation();
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
            System.out.println(e);
            return null;
        }
    }
}