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

    AuxUnit getBestAttackTarget(AuxUnit A, AuxUnit B){
        try {
            if (A == null) return B;
            if (B == null) return A;

            //prioritzem factories i knights
            if ((A.type == UnitType.Factory || A.type == UnitType.Knight) && B.type != UnitType.Factory && B.type != UnitType.Knight) {
                return A;
            }
            if ((B.type == UnitType.Factory || B.type == UnitType.Knight) && A.type != UnitType.Factory && A.type != UnitType.Knight) {
                return B;
            }

            //ataquem el que aguanta menys hits
            int dmgReceivedA = Const.knightDamage;
            int dmgReceivedB = Const.knightDamage;
            if (A.type == UnitType.Knight) dmgReceivedA -= Units.knightBlock;
            if (B.type == UnitType.Knight) dmgReceivedB -= Units.knightBlock;

            double hitsA = Math.ceil((double)A.health / (double)dmgReceivedA);
            double hitsB = Math.ceil((double)B.health / (double)dmgReceivedB);

            if (hitsA < hitsB) {
                return A;
            }
            return B;
        }catch(Exception e) {
            e.printStackTrace();
            return B;
        }
    }

    void attack(AuxUnit unit) {
        try {
            if (Units.stop()) return;
            AuxUnit bestVictim = null;
            AuxMapLocation myLoc = unit.getMapLocation();
            AuxUnit[] enemiesInRange = Wrapper.senseUnits(myLoc.x, myLoc.y, Units.getAttackRange(unit.getType()), false);
            for (AuxUnit u : enemiesInRange) {
                if (!Wrapper.canAttack(unit, u)) continue;
                bestVictim = getBestAttackTarget(bestVictim, u);
            }
            if (bestVictim == null) return;
            if (!unit.canAttack()){
                if (Overcharge.getOvercharged(unit)) attack(unit);
            }
            else{
                Wrapper.attack(unit, bestVictim);
                attack(unit);
            }
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