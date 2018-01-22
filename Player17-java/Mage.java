

import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;

public class Mage {
    final int min_group = 3;
    final long INFL = 1000000000;
    static Mage instance = null;
    AuxMapLocation bestEnemy;

    int[][] multitargetArraY;
    int[][] expandedTargetArray;

    HashMap<Integer, Integer> objectiveArea;

    static Mage getInstance(){
        if (instance == null){
            instance = new Mage();
        }
        return instance;
    }

    void computeMultiTarget(){
        try {
            multitargetArraY = new int[Data.W][Data.H];
            for (int i = 0; i < Data.enemies.length; ++i) {
                if (Data.enemies[i].getHealth() <= 0) continue;
                AuxMapLocation loc = Data.enemies[i].getMaplocation();
                for (int j = 0; j < 9; ++j) {
                    AuxMapLocation newLoc = loc.add(j);
                    if (newLoc.isOnMap()) {
                        ++multitargetArraY[newLoc.x][newLoc.y];
                    }
                }
            }
            for (int i = 0; i < Data.myUnits.length; ++i) {
                if (Data.myUnits[i].isInGarrison()) continue;
                AuxMapLocation loc = Data.myUnits[i].getMaplocation();
                for (int j = 0; j < 9; ++j) {
                    AuxMapLocation newLoc = loc.add(j);
                    if (newLoc.isOnMap()) {
                        --multitargetArraY[newLoc.x][newLoc.y];
                    }
                }
            }

            expandedTargetArray = new int[Data.W][Data.H];
            for (int i = 0; i < Data.enemies.length; ++i){
                if (Data.enemies[i].getHealth() <= 0) continue;
                AuxMapLocation loc = Data.enemies[i].getMaplocation();
                int a = multitargetArraY[loc.x][loc.y];
                for (int j = 0; j < Vision.Mx[30].length; ++j) {
                    AuxMapLocation newLoc = loc.add(new AuxMapLocation(Vision.Mx[30][j], Vision.My[30][j]));
                    if (newLoc.isOnMap()){
                        expandedTargetArray[newLoc.x][newLoc.y] = Math.max(expandedTargetArray[newLoc.x][newLoc.y], a);
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public Mage(){
        objectiveArea = new HashMap();
        multitargetArraY = new int[Data.W][Data.H];
    }

    void play(AuxUnit unit){
        bestEnemy = getBestEnemy(unit.getMaplocation());
        if (unit.canAttack() && (bestEnemy == null || bestEnemy.distanceSquaredTo(unit.getMaplocation()) < 90)){
            if (trySpecialMove(unit)) return;
        }
        MageMove(unit);
        attack(unit);
    }

    void MageMove(AuxUnit unit){
        if (!unit.canMove()) return;
        move(unit);
        return;
    }

    boolean trySpecialMove(AuxUnit unit){
        try {
            if (!Data.canBlink) return false;
            if (!unit.canMove() || !unit.canAttack()) return false;
            if (!unit.canUseAbility()) return false;
            MageMovement m = getSpecialMove(unit);

            if (m == null) return false;

            AuxMapLocation finalLoc = unit.getMaplocation().add(m.mloc).add(m.dir);

            AuxUnit bestunit = null;
            int mostenemies = min_group;


            for (int j = 0; j < Vision.Mx[30].length; ++j) {
                AuxMapLocation newLoc = finalLoc.add(new AuxMapLocation(Vision.Mx[30][j], Vision.My[30][j]));
                if (newLoc.isOnMap()) {
                    AuxUnit u = Data.getUnit(newLoc.x, newLoc.y, false);
                    if (u == null) u = Data.getUnit(newLoc.x, newLoc.y, true);
                    if (u == null) continue;
                    AuxUnit[] units = Wrapper.senseUnits(newLoc.x, newLoc.y, 2, false);
                    if (units.length > mostenemies) {
                        mostenemies = units.length;
                        bestunit = u;
                    }
                }
            }

            if (bestunit != null) {
                perform(unit, m, bestunit);
                return true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }


        //
        return false;
    }

    void perform (AuxUnit unit, MageMovement m, AuxUnit target){
        try {
            if (m.moveFirst) {
                Wrapper.moveRobot(unit, m.dir);
                Wrapper.blink(unit, m.mloc.add(unit.getMaplocation()));
            } else {
                Wrapper.blink(unit, m.mloc.add(unit.getMaplocation()));
                Wrapper.moveRobot(unit, m.dir);
            }
            Wrapper.attack(unit, target);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    MageMovement bestMove(AuxMapLocation loc, MageMovement a, MageMovement b){
        try {
            if (b == null) return a;
            AuxMapLocation finalLocA = a.mloc.add(a.dir);
            AuxMapLocation finalLocB = b.mloc.add(b.dir);

            finalLocA = finalLocA.add(loc);
            finalLocB = finalLocB.add(loc);

            if (expandedTargetArray[finalLocA.x][finalLocA.y] > expandedTargetArray[finalLocB.x][finalLocB.y]) return a;
            if (expandedTargetArray[finalLocA.x][finalLocA.y] < expandedTargetArray[finalLocB.x][finalLocB.y]) return b;

            if (finalLocA.distanceSquaredTo(loc) > finalLocB.distanceSquaredTo(loc)) return a;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    MageMovement getSpecialMove(AuxUnit unit){
        try {
            boolean[][] myMoves = new boolean[7][7];
            AuxMapLocation origin = new AuxMapLocation(0, 0);

            ArrayList<MageMovement> firstMoves = new ArrayList<>();
            for (int i = 0; i < 9; ++i) {
                MageMovement m = new MageMovement(origin.add(i), i, true);
                if (Wrapper.isAccessible(unit.getMaplocation().add(i))) firstMoves.add(m);
                myMoves[3 + m.mloc.x][3 + m.mloc.y] = true;
            }
            for (int i = -2; i <= 2; ++i) {
                for (int j = -2; j <= 2; ++j) {
                    AuxMapLocation newLoc = new AuxMapLocation(i, j);
                    if ((Math.abs(i) == 2 || Math.abs(j) == 2) && Wrapper.isAccessible(unit.getMaplocation().add(newLoc))) {
                        firstMoves.add(new MageMovement(new AuxMapLocation(i, j), 8, false));
                        myMoves[3 + i][3 + j] = true;
                    }
                }
            }

            //les guardo en array x si de cas
            ArrayList<MageMovement> secondMoves = new ArrayList<>();

            for (int t = 0; t < firstMoves.size(); ++t) {
                MageMovement firstMove = firstMoves.get(t);
                if (firstMove.moveFirst) {
                    for (int i = -2; i <= 2; ++i) {
                        for (int j = -2; j <= 2; ++j) {
                            MageMovement m = new MageMovement();
                            m.mloc = new AuxMapLocation(i, j);
                            m.dir = firstMove.dir;
                            AuxMapLocation newLoc = m.mloc.add(m.dir);
                            m.moveFirst = firstMove.moveFirst;
                            if (myMoves[3 + newLoc.x][3 + newLoc.y] == false) {
                                if (Wrapper.isAccessible(unit.getMaplocation().add(newLoc))) {
                                    secondMoves.add(m);
                                    myMoves[3 + newLoc.x][3 + newLoc.y] = true;
                                }
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < 9; ++i) {
                        MageMovement m = new MageMovement();
                        m.mloc = firstMove.mloc;
                        m.dir = i;
                        m.moveFirst = firstMove.moveFirst;
                        AuxMapLocation newLoc = m.mloc.add(m.dir);
                        if (myMoves[3 + newLoc.x][3 + newLoc.y] == false) {
                            if (Wrapper.isAccessible(unit.getMaplocation().add(newLoc))) {
                                secondMoves.add(m);
                                myMoves[3 + newLoc.x][3 + newLoc.y] = true;
                            }
                        }
                    }
                }
            }

            MageMovement move = null;
            for (int i = 0; i < secondMoves.size(); ++i) {
                move = bestMove(unit.getMaplocation(), secondMoves.get(i), move);
            }

            return move;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    AuxUnit getBestAttackTarget(AuxUnit A, AuxUnit B){
        try {
            if (A == null) return B;
            if (B == null) return A;
            if (A.getType() == B.getType()) {
                if (A.getHealth() < B.getHealth()) return A;
                return B;
            }
            if (A.getType() == UnitType.Mage) return A;
            if (B.getType() == UnitType.Mage) return B;
            if (A.getType() == UnitType.Healer) return A;
            if (B.getType() == UnitType.Healer) return B;
            if (A.getType() == UnitType.Ranger) return A;
            if (B.getType() == UnitType.Ranger) return B;
            if (A.getType() == UnitType.Knight) return A;
            if (B.getType() == UnitType.Knight) return B;
            if (A.getType() == UnitType.Worker) return A;
            if (B.getType() == UnitType.Worker) return B;
            return B;
        }catch(Exception e) {
            e.printStackTrace();
            return A;
        }
    }

    void attack(AuxUnit unit) {
        try {
            AuxUnit bestVictim = null;
            if (!unit.canAttack()) return;
            //if (Data.round >= 730) System.err.println("Trying to attack!");
            AuxMapLocation myLoc = unit.getMaplocation();
            AuxUnit[] canAttack = Wrapper.senseUnits(myLoc.x, myLoc.y, Wrapper.getAttackRange(unit.getType()), false);
            //if (Data.round >= 730)System.err.println(canAttack.length);
            for (int i = 0; i < canAttack.length; ++i) {
                AuxUnit victim = canAttack[i];
                if (Wrapper.canAttack(unit, victim)) {
                    bestVictim = getBestAttackTarget(bestVictim, victim);
                    // if (Data.round >= 730)System.err.println("Got a victim!! :)");
                }
            }
            if (bestVictim != null) Wrapper.attack(unit, bestVictim);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }



    void move(AuxUnit unit){
        try {
            AuxMapLocation target = getBestTarget(unit);
            if (target != null) MovementManager.getInstance().moveTo(unit, target);
            else {
                ConstructionQueue queue = Data.queue;
                queue.requestUnit(UnitType.Rocket);
                Explore.explore(unit);
            }
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

    AuxMapLocation getBestTarget(AuxUnit unit){
        try {
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
            /*if (unit.getHealth() < 100) {
                AuxMapLocation ans = getBestHealer(unit.getMaplocation());
                if (ans != null) return ans;
            }*/
            return bestEnemy;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    boolean isBetter(AuxMapLocation myLoc, AuxMapLocation A, AuxMapLocation B){
        if (B == null) return true;
        if (!A.isOnMap()) return false;
        if (!B.isOnMap()) return false;
        int a = multitargetArraY[A.x][A.y];
        int b = multitargetArraY[B.x][B.y];
        if (b < 3 && a > b) return true;
        if (a < 3 && a < b) return false;
        return (myLoc.distanceBFSTo(A) < myLoc.distanceBFSTo(B));
    }

    AuxMapLocation getBestEnemy(AuxMapLocation myLoc){
        try {
            AuxMapLocation target = null;
            for (int i = 0; i < Data.enemies.length; ++i) {
                if (Data.enemies[i].getHealth() <= 0) continue;
                AuxMapLocation enemyLocation = Data.enemies[i].getMaplocation();
                if (isBetter(myLoc, enemyLocation, target)) target = enemyLocation;
            }
            return target;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private class MageMovement{
        AuxMapLocation mloc;
        int dir;
        boolean moveFirst;

        public MageMovement(AuxMapLocation loc, int d, boolean first){
            mloc = loc;
            dir = d;
            moveFirst = first;
        }

        public MageMovement (){

        }

    }

}