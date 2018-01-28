

import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;

public class Mage {
    private final int min_group = 3;
    private static Mage instance = null;


    public AuxUnit unit;

    private int[][] multitargetArraY; //[i][j] = num. enemics - aliats que hi ha al quadrat 3x3
    private int[][] expandedTargetArray; //[i][j] = maxim score possible de disparar a rang <= 30

    private HashMap<Integer, Integer> objectiveArea; //todo borrar aixo??

    static Mage getInstance(){
        if (instance == null){
            instance = new Mage();
        }
        return instance;
    }

    //emplena les matrius (init turn)
    void computeMultiTarget(){
        try {
            multitargetArraY = new int[Mapa.W][Mapa.H];
            //todo donar diferents pesos a cada unit

            //per cada enemic, sumem 1 al quadrat 3x3
            for (AuxUnit enemy: Units.enemies) {
                if (enemy.getHealth() <= 0) continue;
                AuxMapLocation loc = enemy.getMapLocation();
                for (int j = 0; j < 9; ++j) {
                    AuxMapLocation newLoc = loc.add(j);
                    if (newLoc.isOnMap()) {
                        ++multitargetArraY[newLoc.x][newLoc.y];
                    }
                }
            }

            //per cada aliat, sumem 1 al quadrat 3x3
            for (AuxUnit unit: Units.myUnits) {
                if (unit.isInGarrison()) continue;
                AuxMapLocation loc = unit.getMapLocation();
                for (int j = 0; j < 9; ++j) {
                    AuxMapLocation newLoc = loc.add(j);
                    if (newLoc.isOnMap()) {
                        --multitargetArraY[newLoc.x][newLoc.y];
                    }
                }
            }

            expandedTargetArray = new int[Mapa.W][Mapa.H];
            for (AuxUnit enemy: Units.enemies){
                if (enemy.getHealth() <= 0) continue;
                AuxMapLocation loc = enemy.getMapLocation();
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
        }
    }

    public Mage(){
        objectiveArea = new HashMap<>();
    }

    AuxMapLocation getTarget(AuxUnit _unit){
        try {
            AuxMapLocation ans = getBestTarget(_unit);
            if (ans != null) return ans;
            _unit.exploretarget = true;
            return Explore.findExploreObjective(_unit);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void doAction(AuxUnit _unit){
        try {
            unit = _unit;
            if (unit.target != null && !unit.exploretarget && unit.target.distanceSquaredTo(unit.getMapLocation()) < 90) {
                if (trySpecialMove()) return;
            }
            attack();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private boolean trySpecialMove(){
        try {
            if ((Utils.round % 12) > 2) return false;
            if (!Units.canBlink) return false;
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

    private void perform(MageMovement m){
        try {
            if (m.moveFirst) {
                if (m.dir < 8) Wrapper.moveRobot(unit, m.dir);
                Wrapper.blink(unit, m.mloc.add(unit.getMapLocation()));
            } else {
                Wrapper.blink(unit, m.mloc.add(unit.getMapLocation()));
                if (m.dir < 8) Wrapper.moveRobot(unit, m.dir);
            }
            Wrapper.attack(unit, m.bestTarget);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private MageMovement bestMove(MageMovement a, MageMovement b){
        try {
            if (b == null) return a;
            AuxMapLocation finalLocA = a.mloc.add(a.dir);
            AuxMapLocation finalLocB = b.mloc.add(b.dir);

            finalLocA = finalLocA.add(unit.getMapLocation());
            finalLocB = finalLocB.add(unit.getMapLocation());

            if (a.getValue() > b.getValue()) return a;
            if (b.getValue() > a.getValue()) return b;

            if (finalLocA.distanceSquaredTo(unit.getMapLocation()) > finalLocB.distanceSquaredTo(unit.getMapLocation())) return a;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    private MageMovement getSpecialMove(AuxUnit unit){
        try {
            boolean[][] myMoves = new boolean[7][7];
            AuxMapLocation origin = new AuxMapLocation(0, 0);

            ArrayList<MageMovement> firstMoves = new ArrayList<>();
            for (int i = 0; i < 9; ++i) {
                if (!unit.canMove() && i != 8) continue;
                MageMovement m = new MageMovement(origin.add(i), i, true);
                if (unit.getMapLocation().add(i).isAccessible()){
                    firstMoves.add(m);
                    myMoves[3 + m.mloc.x][3 + m.mloc.y] = true;
                }
            }
            for (int i = -2; i <= 2; ++i) {
                for (int j = -2; j <= 2; ++j) {
                    AuxMapLocation newLoc = new AuxMapLocation(i, j);
                    if (!myMoves[3 + i][3 + j] && unit.getMapLocation().add(newLoc).isAccessible()) {
                        firstMoves.add(new MageMovement(new AuxMapLocation(i, j), 8, false));
                        myMoves[3 + i][3 + j] = true;
                    }
                }
            }

            //les guardo en array x si de cas
            ArrayList<MageMovement> secondMoves = new ArrayList<>();

            for (MageMovement firstMove : firstMoves) {
                if (firstMove.moveFirst) {
                    for (int i = -2; i <= 2; ++i) {
                        for (int j = -2; j <= 2; ++j) {
                            MageMovement m = new MageMovement();
                            m.mloc = new AuxMapLocation(i, j);
                            m.dir = firstMove.dir;
                            AuxMapLocation newLoc = m.mloc.add(m.dir);
                            m.moveFirst = firstMove.moveFirst;
                            if (!myMoves[3 + newLoc.x][3 + newLoc.y]) {
                                if (unit.getMapLocation().add(newLoc).isAccessible()) {
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
                        if (!myMoves[3 + newLoc.x][3 + newLoc.y]) {
                            if (unit.getMapLocation().add(newLoc).isAccessible()) {
                                secondMoves.add(m);
                                myMoves[3 + newLoc.x][3 + newLoc.y] = true;
                            }
                        }
                    }
                }
            }

            MageMovement move = null;
            for (MageMovement secondMove : secondMoves) {
                move = bestMove(secondMove, move);
            }

            return move;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    void attack() {
        try {
            if (!unit.canAttack()) return;
            MageMovement m = new MageMovement(new AuxMapLocation(0,0), 8, true);
            m.getValue();
            if (m.bestTarget != null) Wrapper.attack(unit, m.bestTarget);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    AuxMapLocation getBestHealer(AuxMapLocation loc){
        try {
            double minDist = 100000;
            AuxMapLocation ans = null;
            for (int index: Units.healers){
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

    private AuxMapLocation getBestTarget(AuxUnit _unit){
        try {
            if (Rocket.callsToRocket.containsKey(_unit.getID())) return Rocket.callsToRocket.get(_unit.getID());
            /*if (unit.getHealth() < 100) {
                AuxMapLocation ans = getBestHealer(unit.getMapLocation());
                if (ans != null) return ans;
            }*/
            return getBestEnemy(_unit.getMapLocation());
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isBetter(AuxMapLocation myLoc, AuxMapLocation A, AuxMapLocation B){
        try {
            if (B == null) return true;
            if (!A.isOnMap()) return false;
            if (!B.isOnMap()) return true;
            int a = multitargetArraY[A.x][A.y];
            int b = multitargetArraY[B.x][B.y];
            if (b < min_group && a > b) return true;
            if (a < min_group && a < b) return false;
            return (myLoc.distanceBFSTo(A) < myLoc.distanceBFSTo(B));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private AuxMapLocation getBestEnemy(AuxMapLocation myLoc){
        try {
            AuxMapLocation target = null;
            for (AuxUnit enemy: Units.enemies) {
                if (enemy.getHealth() <= 0) continue;
                AuxMapLocation enemyLocation = enemy.getMapLocation();
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

        MageMovement(AuxMapLocation loc, int d, boolean first){
            mloc = loc;
            dir = d;
            moveFirst = first;
        }

        MageMovement(){

        }

        public int getValue(){
            if (value == null) value = computeValue();
            return value;
        }

        int computeValue() {
            try {
                AuxMapLocation finalLoc = unit.getMapLocation().add(dir).add(mloc);
                if (expandedTargetArray[finalLoc.x][finalLoc.y] < min_group) return 0;
                int mostenemies = 0;
                for (int j = 0; j < Vision.Mx[30].length; ++j) {
                    AuxMapLocation newLoc = finalLoc.add(new AuxMapLocation(Vision.Mx[30][j], Vision.My[30][j]));
                    if (newLoc.isOnMap()) {
                        AuxUnit u = newLoc.getUnit(false); //TODO aixo no seria com fer newLoc.getUnit()?
                        if (u == null) u = newLoc.getUnit(true);
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
            catch(Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

    }

}