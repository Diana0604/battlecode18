import bc.UnitType;

import java.util.HashMap;

public class Knight {
    static Knight instance = null;

    HashMap<Integer, Integer> objectiveArea;

    HashMap<Integer, Integer> distToTarget;

    static Knight getInstance(){
        if (instance == null){
            instance = new Knight();
        }
        return instance;
    }

    public Knight(){
        objectiveArea = new HashMap();
    }



    private int typePriority(UnitType t){
        switch(t){
            case Mage: return 7;
            case Knight: return 6;
            case Healer: return 5;
            case Ranger: return 4;
            case Factory: return 3;
            case Worker: return 2;
            case Rocket: return 1;
            default: return 0;
        }
    }

    private int hitsLeft(AuxUnit unit){
        int dmgReceived = Const.knightDamage;
        if(unit.type == UnitType.Knight) dmgReceived -= Units.knightBlock;
        return (int) Math.ceil((double)unit.health / (double)dmgReceived);
    }

    AuxUnit getBestAttackTarget(AuxUnit knight, AuxUnit A, AuxUnit B){
        try {
            if (A == null) return B;
            if (B == null) return A;

            if (typePriority(A.getType()) > typePriority(B.getType())) return A;
            if (typePriority(A.getType()) < typePriority(B.getType())) return B;

            int hitsA = hitsLeft(A);
            int hitsB = hitsLeft(B);
            if (hitsA < hitsB) return A;
            if (hitsA > hitsB) return B;

            if (knight.getMapLocation().distanceBFSTo(A.getMapLocation()) < knight.getMapLocation().distanceBFSTo(B.getMapLocation()))
                return A;
            return B;
        }catch(Exception e) {
            e.printStackTrace();
            return B;
        }
    }

    void attack(AuxUnit unit) {
        try {
            if (Units.canOverCharge && Utils.round%10 == 9) return;
            int posAtArray = Units.allUnits.get(unit.getID());
            AuxUnit bestVictim = null;
            AuxMapLocation myLoc = unit.getMapLocation();
            AuxUnit[] enemiesInRange = Wrapper.senseUnits(myLoc.x, myLoc.y, Units.getAttackRange(unit.getType()), false);
            for (AuxUnit u : enemiesInRange) {
                if (!Wrapper.canAttack(unit, u)) continue;
                bestVictim = getBestAttackTarget(unit, bestVictim, u);

            }
            if (bestVictim == null) return;
            if (!unit.canAttack()){
                if (!Overcharge.getOvercharged(posAtArray)) return;
            }
            Wrapper.attack(unit, bestVictim);
            if (Overcharge.canGetOvercharged(posAtArray)) attack(unit);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    AuxMapLocation getBestHealer(AuxMapLocation loc){
        try {
            double minDist = 100000;
            AuxMapLocation ans = null;
            for (int index: Units.healers) {
                AuxUnit u = Units.myUnits.get(index);
                AuxMapLocation mLoc = u.getMapLocation();
                if (mLoc != null) {
                    double d = loc.distanceBFSTo(mLoc);
                    if (d < minDist) {
                        minDist = d;
                        ans = mLoc;
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
            if (Units.canOverCharge && Utils.round%10 == 9) return null;
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
            if (unit.getHealth() < 100) {
                AuxMapLocation ans = getBestHealer(unit.getMapLocation());
                if (ans != null) return ans;
            }
            AuxMapLocation ans = getBestEnemy(unit.getMapLocation());
            if (ans != null) return ans;
            return Explore.findExploreObjective(unit);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    AuxMapLocation getBestEnemy(AuxMapLocation myLoc){
        try {
            double minDist = Const.INFL;
            AuxMapLocation target = null;
            for (AuxUnit enemy: Units.enemies) {
                if (enemy.getHealth() <= 0) continue;
                AuxMapLocation enemyLocation = enemy.getMapLocation();
                double d = enemyLocation.distanceBFSTo(myLoc);
                if (enemy.type == UnitType.Factory && d <= 5) d -= 10; //si tenim factory a d <= 5 hi anem segur
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