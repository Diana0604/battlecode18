

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
        if (!unit.canAttack()) return;
        AuxUnit[] v = Wrapper.senseUnits(unit.getX(), unit.getY(), 30, true);
        long maxDiff = 0;
        AuxUnit healed = null;
        for (int i = 0; i < v.length; ++i){
            AuxUnit u = v[i];
            long d = Wrapper.getMaxHealth(unit.getType()) - u.getHealth();
            if (d > maxDiff){
                maxDiff = d;
                healed = u;
            }
        }
        if (healed != null){
            Wrapper.heal(unit, healed);
        }
    }

    void play(AuxUnit unit){
        heal(unit);
        move(unit);
        heal(unit);
    }

    //static void moveTo(Unit unit, MapLocation target){
        //MovementManager.getInstance().moveTo(unit, target);
    //}



    void move(AuxUnit unit){
        AuxMapLocation target = getBestTarget(unit);
        if (target != null) MovementManager.getInstance().moveTo(unit, target);
    }

    AuxMapLocation getBestTarget(AuxUnit unit){
        if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
        return getBestUnit(unit.getMaplocation());
    }

    AuxMapLocation getBestUnit(AuxMapLocation loc){
        double minDist = 500000;
        AuxMapLocation ans = null;
        for (int i = 0; i < Data.units.size(); ++i){
            AuxUnit u = Data.myUnits[i];
            if (!Data.structures.contains(i)){
                if (u.getHealth() < Wrapper.getMaxHealth(u.getType())){
                    AuxMapLocation mLoc = u.getMaplocation();
                    if (mLoc != null){
                        double d = mLoc.distanceBFSTo(loc);
                        if (d < minDist){
                            ans = mLoc;
                            minDist = d;
                        }
                    }
                }
            }
        }
        return ans;
    }
}