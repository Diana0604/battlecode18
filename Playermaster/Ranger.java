import bc.UnitType;

import java.util.HashMap;
import java.util.HashSet;

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
    private AuxUnit compareAttackTargets(AuxUnit ranger, AuxUnit A, AuxUnit B, int attacks){
        try {
            if (A == null) return B;
            if (B == null) return A;

            if (A.isInSpace() || A.isInGarrison()) return B; //si matem un tio, el posem que esta a l'espai
            if (B.isInSpace() || B.isInGarrison()) return A;

            int hitsA = hitsLeft(A);
            int hitsB = hitsLeft(B);

            boolean canKillA = A.getType() != UnitType.Worker && attacks >= hitsA;
            boolean canKillB = B.getType() != UnitType.Worker && attacks >= hitsB;

            if (canKillA && !canKillB) return A;
            if (!canKillA && canKillB) return B;

            if (typePriority(A.getType()) > typePriority(B.getType())) return A;
            if (typePriority(A.getType()) < typePriority(B.getType())) return B;

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

    private AuxUnit pickAttackTarget(AuxUnit unit, int attacks){
        AuxUnit bestVictim = null;
        AuxMapLocation myLoc = unit.getMapLocation();
        AuxUnit[] enemiesInRange = Wrapper.senseUnits(myLoc.x, myLoc.y, Units.getAttackRange(unit.getType()), false);
        for (AuxUnit u : enemiesInRange) {
            if (!Wrapper.canAttack(unit, u)) continue;
            bestVictim = compareAttackTargets(unit, bestVictim, u, attacks);
        }
        return bestVictim;
    }

    void attack(AuxUnit unit) {
        try {
            /*
            System.out.println("ROUND " + Utils.round + " OVERCHARGE INFO FOR " + unit.getID() + ", ON " + unit.getMapLocation());
            System.out.println("    Healers with overcharge in range: " + Overcharge.overchargeMatrix[unit.getX()][unit.getY()]);
            HashSet<Integer> healers = Overcharge.overchargeInRange.get(Units.myUnits.indexOf(unit));
            for (int index: healers){
                AuxUnit healer = Units.myUnits.get(index);
                System.out.println("        " + healer.getID() + " loc " + healer.getMapLocation());
            }
*/
            //if (Units.canOverCharge && Utils.round%10 == 9) return;

            int posAtArray = Units.allUnits.get(unit.getID());
            int attacks = Overcharge.overchargesAt(unit.getMapLocation());
            if (unit.canAttack()) attacks++;
            if (attacks == 0) return;

            AuxUnit targetUnit = pickAttackTarget(unit, attacks);
            if (targetUnit == null) return;
            int attacksToKill = hitsLeft(targetUnit);
            //System.out.println(Utils.round + " ranger " + unit.getID() + " loc " + unit.getMapLocation() + " has target " + targetUnit.getMapLocation());
            //System.out.println("    Needs " + attacksToKill + " attacks, has " + attacks);

            if (attacks < attacksToKill){
                //cant kill, estalvia overcharge
                if (Wrapper.canAttack(unit, targetUnit)) Wrapper.attack(unit,targetUnit);
            }else{
                while (attacksToKill > 0){
                    //System.out.println("    CAN KILL " + attacksToKill);
                    if (Wrapper.canAttack(unit, targetUnit)) {
                        Wrapper.attack(unit,targetUnit);
                        attacksToKill--;
                        attacks--;
                    }else{
                        Overcharge.getOvercharged(posAtArray);
                    }
                }
                if (attacks > 0) attack(unit); //si mata el target i li queden atacs mira que no pugui matar mes
            }

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
                if (enemy.getType() == UnitType.Worker) d += 10;
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
            final int MAX_HP_TO_RETREAT = 110; //todo: crec que es millor canviar aixo per 110 pq mages no facin oneshot
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