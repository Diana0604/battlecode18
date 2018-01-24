

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

    void attack(AuxUnit unit, boolean firstTime) {
        try {
            if (Data.canOverCharge && Data.round%10 == 9) return;
            int posAtArray = Data.allUnits.get(unit.getID());
            if (firstTime) Overcharge.update(posAtArray);
            AuxUnit bestVictim = null;
            AuxMapLocation myLoc = unit.getMaplocation();
            AuxUnit[] canAttack = Wrapper.senseUnits(myLoc.x, myLoc.y, Wrapper.getAttackRange(unit.getType()), false);
            for (int i = 0; i < canAttack.length; ++i) {
                AuxUnit u = canAttack[i];
                bestVictim = getBestAttackTarget(bestVictim, u);
            }
            if (bestVictim == null) return;
            if (!unit.canAttack()){
                if (!Overcharge.getOvercharged(posAtArray)) return;
            }
            Wrapper.attack(unit, bestVictim);
            if (Overcharge.canGetOvercharged(posAtArray) > 0) attack(unit, false);
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
            if (Data.canOverCharge && Data.round%10 == 9) return null;
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