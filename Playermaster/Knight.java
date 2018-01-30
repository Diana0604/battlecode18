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


    /*--------- ATTACK -----------*/

    private int attackTypePriority(UnitType t){
        switch(t){
            case Mage: return 6;
            case Knight: return 5;
            case Factory: return 3;
            case Healer: return 5;
            case Ranger: return 4;
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

    private AuxUnit compareAttackTargets(AuxUnit A, AuxUnit B, int attacks){
        try {
            if (A == null) return B;
            if (B == null) return A;

            if (A.getHealth() <= 0) return B; //si matem un tio, el posem que esta a l'espai
            if (B.getHealth() <= 0) return A;

            int hitsA = hitsLeft(A);
            int hitsB = hitsLeft(B);

            boolean canKillA = A.getType() != UnitType.Worker && attacks >= hitsA;
            boolean canKillB = B.getType() != UnitType.Worker && attacks >= hitsB;

            if (canKillA && !canKillB) return A;
            if (!canKillA && canKillB) return B;

            if (attackTypePriority(A.getType()) > attackTypePriority(B.getType())) return A;
            if (attackTypePriority(A.getType()) < attackTypePriority(B.getType())) return B;

            if (hitsA < hitsB) return A;
            return B;


        }catch(Exception e) {
            e.printStackTrace();
            return B;
        }
    }


    private AuxUnit pickAttackTarget(AuxUnit knight, int attacks){
        AuxUnit bestVictim = null;
        AuxMapLocation myLoc = knight.getMapLocation();
        AuxUnit[] enemiesInRange = Wrapper.senseUnits(myLoc.x, myLoc.y, Units.getAttackRange(knight.getType()), false);
        for (AuxUnit u : enemiesInRange) {
            if (!Wrapper.canAttack(knight, u)) continue;
            //if (u.getType() == UnitType.Knight) System.out.println("Can attack " + u.getX() + " " + u.getY() + " " + knight.getID());
            bestVictim = compareAttackTargets(bestVictim, u, attacks);
        }
        //if (bestVictim != null) System.out.println("Attacking " + bestVictim.getX() + " " + bestVictim.getY()+ " " + knight.getID());
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
            //System.out.println(Utils.round + " knight " + unit.getID() + " loc " + unit.getMapLocation() + " has target " + targetUnit.getMapLocation());
            //System.out.println("    Needs " + attacksToKill + " attacks, has " + attacks);

            if (attacks < attacksToKill){
                //cant kill, estalvia overcharge
                if (Wrapper.canAttack(unit, targetUnit)) Wrapper.attack(unit,targetUnit);
            }else{
                while (attacksToKill > 0){
                    //A POR ELLOS, OEEEEEEEEEEE
                    //System.out.println("    CAN KILL " + attacksToKill);
                    if (Wrapper.canAttack(unit, targetUnit)) {
                        Wrapper.attack(unit,targetUnit);
                        attacksToKill--;
                        attacks--;
                    }else{
                        Overcharge.getOvercharged(posAtArray, targetUnit.getMapLocation());
                    }
                }
                if (attacks > 0) attack(unit); //si mata el target i li queden atacs mira que no pugui matar mes
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*----------- PICK TARGET ------------*/

    private int moveTypePriority(UnitType t){
        switch(t){
            case Mage: return 8;
            case Ranger: return 8;
            case Factory: return 4;
            case Healer: return 7;
            case Knight: return 7;
            case Worker: return 2;
            case Rocket: return 1;
            default: return 0;
        }
    }

    private AuxUnit compareMoveTargets(AuxMapLocation myLoc, AuxUnit A, AuxUnit B){
        try {
            if (A == null) return B;
            if (B == null) return A;

            if (A.getHealth() <= 0) return B;
            if (B.getHealth() <= 0) return A;

            int distA = myLoc.distanceBFSTo(A.getMapLocation());
            int distB = myLoc.distanceBFSTo(B.getMapLocation());
            int priorityA = moveTypePriority(A.getType());
            int priorityB = moveTypePriority(B.getType());

            if (priorityA - distA > priorityB - distB) return A;

            if (distA < distB) return A;
            return B;

        }catch(Exception e) {
            e.printStackTrace();
            return B;
        }
    }

    private AuxUnit targetBestEnemy(AuxMapLocation myLoc){
        try {
            AuxUnit bestTarget = null;
            for (AuxUnit enemy: Units.enemies) {
                if (enemy.getType() == UnitType.Knight){
                    //System.out.println("Saw a knight at " + enemy.getX() + " " + enemy.getY());
                }
                if (enemy.getHealth() <= 0) continue;
                bestTarget = compareMoveTargets(myLoc, bestTarget, enemy);
            }
            //if (bestTarget != null) System.out.println("Enemic at " + bestTarget.getX() + " " + bestTarget.getY());
            return bestTarget;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //cridada des de unit manager
    AuxMapLocation updateTarget(AuxUnit unit){
        try {
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());

            AuxUnit target = targetBestEnemy(unit.getMapLocation());
            if (target != null) return target.getMapLocation();
            return Explore.findExploreObjective(unit);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}