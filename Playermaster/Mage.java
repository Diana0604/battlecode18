

import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;

public class Mage {
    final int min_group = 1;
    final long INFL = 1000000000;
    static Mage instance = null;


    public AuxUnit unit;

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
    }

    AuxMapLocation getTarget(AuxUnit _unit){
        if (Data.canOverCharge && Data.round%10 == 9) return null;
        AuxMapLocation ans = getBestTarget(_unit);
        if (ans != null) return ans;
        _unit.exploretarget = true;
        return Explore.findExploreObjective(_unit);
    }

    void doAction(AuxUnit _unit){
        unit = _unit;
        if (unit.target != null && !unit.exploretarget && unit.target.distanceSquaredTo(unit.getMaplocation()) < 90){
            if (trySpecialMove()) return;
            tryOverChargeMove();
        }
        attack();
    }

    int stepsTo(int d){
        if (d <= 30) return 0;
        if (d <= 45) return 1;
        if (d <= 65) return 2;
        if (d <= 89) return 3;
        return 4;
    }

    void tryOverChargeMove(){
        if (!Data.canOverCharge || Data.round%10 != 0) return;
        int posAtArray = Data.allUnits.get(unit.getID());
        int d = unit.target.distanceSquaredTo(unit.getMaplocation());
        if (unit.canMove() && d > 8) MovementManager.getInstance().straightMoveTo(unit);
        boolean atk = attack();
        Overcharge.update(posAtArray);
        d = unit.target.distanceSquaredTo(unit.getMaplocation());
        if (Overcharge.canGetOvercharged(posAtArray) < stepsTo(d)) return;
        if (!atk && unit.canMove()) return;
        if(Overcharge.getOvercharged(posAtArray)) tryOverChargeMove();
    }

    boolean trySpecialMove(){
        try {
            if ((Data.round % 12) > 2) return false;
            if (!Data.canBlink) return false;
            if (!unit.canAttack()) return false;
            if (!unit.canUseAbility()) return false;
            MageMovement m = getSpecialMove(unit);

            if (m == null) return false;

            if (m.getValue() < min_group) return false;

            perform(m);
        } catch(Exception e) {
            e.printStackTrace();
        }


        //
        return false;
    }

    void perform (MageMovement m){
        try {
            if (m.moveFirst) {
                if (m.dir < 8) Wrapper.moveRobot(unit, m.dir);
                Wrapper.blink(unit, m.mloc.add(unit.getMaplocation()));
            } else {
                Wrapper.blink(unit, m.mloc.add(unit.getMaplocation()));
                if (m.dir < 8) Wrapper.moveRobot(unit, m.dir);
            }
            Wrapper.attack(unit, m.bestTarget);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    MageMovement bestMove(MageMovement a, MageMovement b){
        try {
            if (b == null) return a;
            AuxMapLocation finalLocA = a.mloc.add(a.dir);
            AuxMapLocation finalLocB = b.mloc.add(b.dir);

            finalLocA = finalLocA.add(unit.getMaplocation());
            finalLocB = finalLocB.add(unit.getMaplocation());

            if (a.getValue() > b.getValue()) return a;
            if (b.getValue() > a.getValue()) return b;

            if (finalLocA.distanceSquaredTo(unit.getMaplocation()) > finalLocB.distanceSquaredTo(unit.getMaplocation())) return a;
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
                if (!unit.canMove() && i != 8) continue;
                MageMovement m = new MageMovement(origin.add(i), i, true);
                if (Wrapper.isAccessible(unit.getMaplocation().add(i))){
                    firstMoves.add(m);
                    myMoves[3 + m.mloc.x][3 + m.mloc.y] = true;
                }
            }
            for (int i = -2; i <= 2; ++i) {
                for (int j = -2; j <= 2; ++j) {
                    AuxMapLocation newLoc = new AuxMapLocation(i, j);
                    if (myMoves[3+i][3+j] == false && Wrapper.isAccessible(unit.getMaplocation().add(newLoc))) {
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
                        if (!unit.canMove() && i != 8) continue;
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
                move = bestMove(secondMoves.get(i), move);
            }

            return move;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    boolean attack() {
        try {
            if (Data.canOverCharge && Data.round%10 == 9) return false;
            if (!unit.canAttack()) return false;
            MageMovement m = new MageMovement(new AuxMapLocation(0,0), 8, true);
            m.getValue();
            if (m.bestTarget != null){
                Wrapper.attack(unit, m.bestTarget);
                unit.target = m.bestTarget.getMaplocation();
                return true;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
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

    AuxMapLocation getBestTarget(AuxUnit _unit){
        try {
            if (Rocket.callsToRocket.containsKey(_unit.getID())) return Rocket.callsToRocket.get(_unit.getID());
            /*if (unit.getHealth() < 100) {
                AuxMapLocation ans = getBestHealer(unit.getMaplocation());
                if (ans != null) return ans;
            }*/
            return getBestEnemy(_unit.getMaplocation());
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
        if (b < min_group && a > b) return true;
        if (a < min_group && a < b) return false;
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
        Integer value = null;
        AuxUnit bestTarget = null;

        public MageMovement(AuxMapLocation loc, int d, boolean first){
            mloc = loc;
            dir = d;
            moveFirst = first;
        }

        public MageMovement (){

        }

        public int getValue(){
            if (value == null) value = computeValue();
            return value;
        }

        public int computeValue() {
            AuxMapLocation finalLoc = unit.getMaplocation().add(dir).add(mloc);
            if (expandedTargetArray[finalLoc.x][finalLoc.y] < min_group) return 0;
            int mostenemies = 0;
            for (int j = 0; j < Vision.Mx[30].length; ++j) {
                AuxMapLocation newLoc = finalLoc.add(new AuxMapLocation(Vision.Mx[30][j], Vision.My[30][j]));
                if (newLoc.isOnMap()) {
                    AuxUnit u = Data.getUnit(newLoc.x, newLoc.y, false);
                    if (u == null) u = Data.getUnit(newLoc.x, newLoc.y, true);
                    if (u == null) continue;
                    AuxUnit[] units = Wrapper.senseUnits(newLoc.x, newLoc.y, 2, false);
                    if (units.length > mostenemies) {
                        mostenemies = units.length;
                        bestTarget = u;
                    }
                }
            }
            return mostenemies;
        }

    }

}