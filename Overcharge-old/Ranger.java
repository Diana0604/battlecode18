

import bc.UnitType;

import java.util.HashMap;

public class    Ranger {
    final long INFL = 1000000000;
    static Ranger instance = null;

    HashMap<Integer, Integer> objectiveArea;

    static Ranger getInstance(){
        if (instance == null){
            instance = new Ranger();
        }
        return instance;
    }

    public Ranger(){
        objectiveArea = new HashMap();
    }

    AuxUnit getBestAttackTarget(AuxUnit A, AuxUnit B){
        try {
            if (A == null) return B;
            if (B == null) return A;
            if (A.getHealth() < B.getHealth()) return A;
            return B;
        }catch(Exception e) {
            e.printStackTrace();
            return B;
        }
    }

    void attack(AuxUnit unit) {
        try {
            AuxUnit bestVictim = null;
            AuxMapLocation myLoc = unit.getMaplocation();
            AuxUnit healer = null;
            AuxUnit[] canAttack = Wrapper.senseUnits(myLoc.x, myLoc.y, Wrapper.getAttackRange(unit.getType()));
            //if (Data.round >= 730)System.err.println(canAttack.length);
            for (int i = 0; i < canAttack.length; ++i) {
                AuxUnit u = canAttack[i];
                if (Data.canOverCharge && u.myTeam){
                    if (u.getType() == UnitType.Healer && u.canUseAbility()){
                        if (u == null) System.out.println("null u");
                        if (healer != null && healer.getMaplocation() == null) System.out.println("null healer maploc");
                        if (u != null && u.getMaplocation() == null) System.out.println("null u maploc!!");
                        if (healer == null || myLoc.distanceSquaredTo(healer.getMaplocation()) > myLoc.distanceSquaredTo(u.getMaplocation())) healer = u;
                    }
                }
                else if (!u.myTeam) bestVictim = getBestAttackTarget(bestVictim, u);
                    // if (Data.round >= 730)System.err.println("Got a victim!! :)");
                //}
            }
            if (bestVictim == null) return;
            if (!unit.canAttack() && (healer == null || myLoc.distanceSquaredTo(healer.getMaplocation()) > Wrapper.getAttackRange(UnitType.Healer))) return;
            if (!unit.canAttack()) Wrapper.overcharge(healer, unit);
            Wrapper.attack(unit, bestVictim);
            if (healer != null) attack(unit);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    AuxMapLocation getBestHealer(AuxMapLocation loc){
        try {
            double minDist = 100000;
            AuxMapLocation ans = null;
            for (int i = 0; i < Data.myUnits.length; ++i) {
                AuxUnit u = Data.myUnits[i];
                if (u.getType() == UnitType.Healer) {
                    AuxMapLocation mLoc = u.getMaplocation();
                    if (mLoc != null) {
                        double d = loc.distanceBFSTo(mLoc);
                        if (d < minDist) {
                            minDist = d;
                            ans = mLoc;
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

    AuxMapLocation getTarget(AuxUnit unit){
        try {
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
            if (unit.getHealth() < 100) {
                AuxMapLocation ans = getBestHealer(unit.getMaplocation());
                if (ans != null) return ans;
            }
            AuxMapLocation ans = getBestEnemy(unit.getMaplocation());
            if (ans != null) return ans;
            return Explore.findExploreObjective(unit);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    AuxMapLocation getBestEnemy(AuxMapLocation myLoc){
        try {
            double minDist = INFL;
            AuxMapLocation target = null;
            for (int i = 0; i < Data.enemies.length; ++i) {
                if (Data.enemies[i].getHealth() <= 0) continue;
                AuxMapLocation enemyLocation = Data.enemies[i].getMaplocation();
                double d = enemyLocation.distanceBFSTo(myLoc);
                if (d < minDist) {
                    minDist = d;
                    target = enemyLocation;
                }
            }
            return target;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}