import bc.UnitType;

import java.util.HashMap;

public class Ranger {
    private static Ranger instance = null;

    //todo esborrar aixo?
    private HashMap<Integer, Integer> objectiveArea;


    static Ranger getInstance(){
        if (instance == null){
            instance = new Ranger();
        }
        return instance;
    }

    public Ranger(){
        objectiveArea = new HashMap<>();
    }


    /*------------------ ATTACK -----------------*/

    private int typePriority(UnitType t){
        switch(t){
            case Mage: return 6;
            case Knight: return 5;
            case Healer: return 4;
            case Ranger: return 4;
            case Factory: return 3;
            case Worker: return 2;
            case Rocket: return 1;
            default: return 0;
        }
    }

    private int hitsLeft(AuxUnit unit){
        int dmgReceived = Const.rangerDamage;
        if(unit.type == UnitType.Knight) dmgReceived -= Units.knightBlock;
        return (int) Math.ceil((double)unit.health / (double)dmgReceived);
    }

    //todo: fer que prioritzi per unit
    private AuxUnit compareAttackTargets(AuxUnit ranger, AuxUnit A, AuxUnit B){
        try {
            if (A == null) return B;
            if (B == null) return A;

            if (typePriority(A.getType()) > typePriority(B.getType())) return A;
            if (typePriority(A.getType()) < typePriority(B.getType())) return B;

            int hitsA = hitsLeft(A);
            int hitsB = hitsLeft(B);
            if (hitsA < hitsB) return A;
            if (hitsA > hitsB) return B;

            if (ranger.getMapLocation().distanceBFSTo(A.getMapLocation()) < ranger.getMapLocation().distanceBFSTo(B.getMapLocation()))
                return A;
            return B;
        }catch(Exception e) {
            e.printStackTrace();
            return B;
        }
    }

    private AuxUnit pickAttackTarget(AuxUnit unit){
        AuxUnit bestVictim = null;
        AuxMapLocation myLoc = unit.getMapLocation();
        AuxUnit[] enemiesInRange = Wrapper.senseUnits(myLoc.x, myLoc.y, Units.getAttackRange(unit.getType()), false);
        for (AuxUnit u : enemiesInRange) {
            if (!Wrapper.canAttack(unit, u)) continue;
            bestVictim = compareAttackTargets(unit, bestVictim, u);
        }
        return bestVictim;
    }

    void attack(AuxUnit unit) {
        try {
            if (Units.canOverCharge && Utils.round%10 == 9) return;
            int posAtArray = Units.allUnits.get(unit.getID());

            AuxUnit targetUnit = pickAttackTarget(unit);
            if (targetUnit == null) return;

            if (!unit.canAttack() && !Overcharge.getOvercharged(posAtArray)) return;
            Wrapper.attack(unit, targetUnit);
            if (Overcharge.canGetOvercharged(posAtArray)) attack(unit);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    /*------------------ PICK TARGET -----------------*/

    //retorna el healer mes proper
    private AuxMapLocation getBestHealer(AuxMapLocation loc){
        try {
            //todo posar un limit de distancia maxima?
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

    private AuxMapLocation getBestEnemy(AuxMapLocation myLoc) {
        try {
            //todo prioritzar tropes abans que workers? idk
            double minDist = Const.INFL;
            AuxMapLocation target = null;
            for (AuxUnit enemy : Units.enemies) {
                if (enemy.getHealth() <= 0) continue;
                AuxMapLocation enemyLocation = enemy.getMapLocation();
                double d = enemyLocation.distanceBFSTo(myLoc);
                if (d < minDist) {
                    minDist = d;
                    target = enemyLocation;
                }
            }
            return target;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    AuxMapLocation getTarget(AuxUnit unit){
        try {
            final int MAX_HP_TO_RETREAT = 100; //todo: crec que es millor canviar aixo per 110 pq mages no facin oneshot
            if (Units.canOverCharge && Utils.round % 10 == 9) return null;
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
            if (unit.getHealth() < MAX_HP_TO_RETREAT) {
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
}