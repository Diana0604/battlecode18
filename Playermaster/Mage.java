

import bc.UnitType;

import java.util.*;

public class Mage {
    private final double min_group = 1.5;
    private static Mage instance = null;

    //aixo es pel bfs de overcharge
    private static final int MOVE = 1;
    private static final int BLINK = 2;
    private static final int OVERCHARGE = 3;

    static Mage getInstance(){
        if (instance == null){
            instance = new Mage();
        }
        return instance;
    }

    public Mage(){
    }

    /*------------ REGULAR ATTACK ------------*/

    private AuxUnit pickAttackTarget(AuxUnit mage){
        AuxUnit bestEnemy = null;
        double bestVal = 0;
        AuxUnit[] enemies = Wrapper.senseUnits(mage.getX(), mage.getY(), 30); //fa sense d'aliats tambe
        for (int i = 0; i < enemies.length; ++i){
            AuxUnit enemy = enemies[i];
            if (Target.mageHits[enemy.getX()][enemy.getY()] > bestVal){
                bestVal = Target.mageHits[enemy.getX()][enemy.getY()];
                bestEnemy = enemy;
            }
        }
        return bestEnemy;
    }

    private void regularAttack(AuxUnit mage) {
        try {
            System.out.println(Utils.round + "  " + mage.getMapLocation() + " enter regular attack, target " + mage.target);
            AuxUnit unitToAttack = pickAttackTarget(mage);
            if (unitToAttack == null) {
                System.out.println(Utils.round + "  " + mage.getMapLocation() + " unitToAttack null " + mage.target);
                return;
            }
            System.out.println(Utils.round + "  " + mage.getMapLocation() + " decides to attack " + unitToAttack.getMapLocation());
            AuxMapLocation location;
            if (mage.target == null) location = unitToAttack.getMapLocation();
            else location = mage.target;
            if (!mage.canAttack()) {
                System.out.println(Utils.round + "  " + mage.getMapLocation() + " can't attack, tries overcharging");
                Overcharge.getOvercharged(mage, location);
                if (mage.canAttack()) {
                    System.out.println(Utils.round + "  " + mage.getMapLocation() + " gets overcharged, tries to attack");
                    regularAttack(mage);
                }
            } else {
                System.out.println(Utils.round + "  " + mage.getMapLocation() + " can attack, attacks " + unitToAttack.getMapLocation());
                Wrapper.attack(mage,unitToAttack);
                regularAttack(mage);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }



    /*------------ EPIC COMBO ATTACK -----------*/


    private ArrayList<MageInfo> getNextStates(MageInfo state, boolean canMove, boolean canBlink){
        if (state == null) return null;
        int[][] minSuicides = new int[7][7];
        for (int i = 0; i < minSuicides.length; i++){
            for (int j = 0; j < minSuicides[0].length; j++){
                minSuicides[i][j] = 100;
            }
        }
        AuxMapLocation origin = new AuxMapLocation(state.location.x, state.location.y);

        HashMap<Integer, MageInfo> states = new HashMap<>();
        for (int i = 0; i < Vision.Mx[2].length; i++){
            if (!canMove) continue;
            int dx = Vision.Mx[2][i];
            int dy = Vision.My[2][i];
            AuxMapLocation moveLoc = new AuxMapLocation(origin.x + dx, origin.y + dy);
            if (!moveLoc.isMovable()) continue;
            int code = encodeMovement(MOVE, moveLoc);
            MageInfo moveState = new MageInfo(state, code);
            int suicideScore = 0;
            AuxUnit unit = moveLoc.getUnit();
            if (unit != null && unit.isRobot() && unit.myTeam){
                if (unit.canMove()) suicideScore = 1;
                else suicideScore = 3;
            }
            if (suicideScore < minSuicides[3 + dx][3 + dy]){
                minSuicides[3 + dx][3 + dy] = suicideScore;
                moveState.alliesSuicided = suicideScore/3;
                states.put(moveLoc.encode(), moveState);
            }

            for (int j = 0; j < Vision.Mx[8].length; j++){
                if (!canBlink) continue;
                int ddx = dx + Vision.Mx[8][j];
                int ddy = dy + Vision.My[8][j];
                AuxMapLocation moveBlinkLoc = new AuxMapLocation(origin.x + ddx, origin.y + ddy);
                if (!moveBlinkLoc.isMovable()) continue;
                int code2 = encodeMovement(BLINK, moveBlinkLoc);
                MageInfo moveBlinkState = new MageInfo(moveState, code2);
                int suicideScore2 = suicideScore;
                if (unit != null && unit.isRobot() && unit.myTeam){
                    if (unit.canMove()) suicideScore2 += 1;
                    else suicideScore2 += 3;
                }
                if (suicideScore2 < minSuicides[3 + ddx][3 + ddy]){
                    minSuicides[3 + ddx][3 + ddy] = suicideScore2;
                    moveBlinkState.alliesSuicided = suicideScore2/3;
                    states.put(moveBlinkLoc.encode(), moveBlinkState);
                }
            }
        }

        for (int i = 0; i < Vision.Mx[8].length; i++){
            if (!canBlink) continue;
            int dx = Vision.Mx[8][i];
            int dy = Vision.My[8][i];
            AuxMapLocation blinkLoc = new AuxMapLocation(origin.x + dx, origin.y + dy);
            if (!blinkLoc.isMovable()) continue;
            int code = encodeMovement(BLINK, blinkLoc);
            MageInfo blinkState = new MageInfo(state, code);
            int suicideScore;
            AuxUnit unit = blinkLoc.getUnit();
            if (unit != null && unit.isRobot() && unit.myTeam){
                if (unit.canMove()) suicideScore = 1;
                else suicideScore = 3;
            }else suicideScore = 0;
            if (suicideScore < minSuicides[3 + dx][3 + dy]){
                minSuicides[3 + dx][3 + dy] = suicideScore;
                blinkState.alliesSuicided = suicideScore/3;
                states.put(blinkLoc.encode(), blinkState);
            }

            for (int j = 0; j < Vision.Mx[2].length; j++){
                if (!canMove) continue;
                int ddx = dx + Vision.Mx[2][j];
                int ddy = dy + Vision.My[2][j];
                AuxMapLocation blinkMoveLoc = new AuxMapLocation(origin.x + ddx, origin.y + ddy);
                if (!blinkMoveLoc.isMovable()) continue;
                int code2 = encodeMovement(MOVE, blinkMoveLoc);
                MageInfo blinkMoveState = new MageInfo(blinkState, code2);
                int suicideScore2 = suicideScore;
                if (unit != null && unit.isRobot() && unit.myTeam){
                    if (unit.canMove()) suicideScore2 += 1;
                    else suicideScore2 += 3;
                }
                if (suicideScore2 < minSuicides[3 + ddx][3 + ddy]){
                    minSuicides[3 + ddx][3 + ddy] = suicideScore2;
                    blinkMoveState.alliesSuicided = suicideScore2/3;
                    states.put(blinkMoveLoc.encode(), blinkMoveState);
                }
            }
        }
        ArrayList<MageInfo> ret = new ArrayList<>();
        for (MageInfo mageInfo: states.values()){
            ret.add(mageInfo);
        }
        if (!canMove && !canBlink) ret.add(state);
        return ret;
    }


    private int encodeMovement(int type, AuxMapLocation newLoc){
        int n1 = type;
        int n2 = newLoc.encode();
        return (n1 << 12)|n2;
    }

    private int movementType(int code){
        return ((code >> 12)&0x3);
    }

    private AuxMapLocation movementDestination(int code){
        return new AuxMapLocation(code&0xFFF);
    }

    //sempre assumim que els mage infos ja han mogut i blinkejat
    private class MageInfo implements Comparable<MageInfo>{
        AuxUnit mage;
        HashSet<Integer> overchargesUsed;
        ArrayList<Integer> movesUsed;
        AuxMapLocation location;
        int alliesSuicided;
        int expectedOvercharges;
        int value;

        MageInfo(AuxUnit mage){
            this.mage = mage;
            overchargesUsed = new HashSet<>();
            movesUsed = new ArrayList<>();
            location = mage.getMapLocation();
            alliesSuicided = 0;
            expectedOvercharges = getExpectedOvercharges();
            value = getValue();
        }

        MageInfo(MageInfo prevState, int movement){
            mage = prevState.mage;
            overchargesUsed = new HashSet<>();
            overchargesUsed.addAll(prevState.overchargesUsed);
            ArrayList<Integer> newList = new ArrayList<>();
            newList.addAll(prevState.movesUsed);
            newList.add(movement);
            AuxMapLocation newLoc = movementDestination(movement);
            movesUsed = newList;
            location = new AuxMapLocation(newLoc.x, newLoc.y);
            alliesSuicided = prevState.alliesSuicided;
            expectedOvercharges = getExpectedOvercharges();
            value = getValue();
        }

        MageInfo(AuxUnit mage, HashSet<Integer> overchargesUsed, ArrayList<Integer> movesUsed,
                          AuxMapLocation location, int alliesSuicided){
            this.mage = mage;
            this.overchargesUsed = new HashSet<>();
            this.overchargesUsed.addAll(overchargesUsed);
            this.movesUsed = new ArrayList<>();
            this.movesUsed.addAll(movesUsed);
            this.location = new AuxMapLocation(location.x, location.y);
            this.alliesSuicided = alliesSuicided;
            expectedOvercharges = getExpectedOvercharges();
            value = getValue();
        }

        //fa overcharge amb el healer mes llunya del target
        MageInfo getOvercharged(){
            HashSet<Integer> healersIndex = Overcharge.overchargeMatrix.get(location.encode());
            int maxDist = -1;
            int maxIndex = -1;
            if (healersIndex == null) return null;
            for (int index: healersIndex){
                if (overchargesUsed.contains(index)) continue;
                AuxUnit healer = Units.myUnits.get(index);
                int dist = healer.getMapLocation().distanceSquaredTo(mage.target);
                if (dist > maxDist){
                    maxDist = dist;
                    maxIndex = index;
                }
            }
            if (maxIndex == -1) return null;
            HashSet<Integer> newOverchargesUsed = new HashSet<>();
            newOverchargesUsed.addAll(overchargesUsed);
            newOverchargesUsed.add(maxIndex);
            ArrayList<Integer> newMovesUsed = new ArrayList<>();
            newMovesUsed.addAll(movesUsed);
            int code = encodeMovement(OVERCHARGE, location);
            newMovesUsed.add(code);
            return new MageInfo(mage, newOverchargesUsed, newMovesUsed, location, alliesSuicided);
        }

        //com mes petit, millor
        public int getValue(){
            int n1 = overchargesUsed.size() + expectedOvercharges;
            int n2 = expectedOvercharges;
            int n3 = alliesSuicided;
            return (((n1 << 6) | n2) << 6) | n3;
        }

        public int compareTo(MageInfo other){
            if (value < other.value) return -1;
            if (value > other.value) return 1;
            return 0;
        }

        private int getExpectedOvercharges(){
            AuxMapLocation target = mage.target;
            if (target == null) return 50;
            int x1 = location.x;
            int y1 = location.y;
            int x2 = target.x;
            int y2 = target.y;
            int range = 3;
            return (Math.max(Math.abs(x1-x2), Math.abs(y1-y2)) + range - 1) / range;
        }
    }


    private MageInfo findOPSequence(){
        PriorityQueue<MageInfo> queue = new PriorityQueue<>();
        for (int index: Units.mages){
            AuxUnit mage = Units.myUnits.get(index);
            if (mage.isDead() || mage.isInGarrison() || mage.isInSpace()) continue;
            if (mage.target == null || mage.exploretarget) continue;
            if (!Units.canBlink && mage.getMapLocation().distanceSquaredTo(mage.target) > 90) continue;
            MageInfo mageInfo = new MageInfo(mage);
            ArrayList<MageInfo> states = getNextStates(mageInfo, mage.canMove(), mage.canUseAbility() && Units.canBlink);
            if (states != null) {
                for (MageInfo state : states) {
                    queue.offer(state);
                }
            }
        }
        int iterations = 0;
        while (!queue.isEmpty() && iterations++ < 1000){
            MageInfo state = queue.poll();
            int overchargesLeft = 0;
            HashSet<Integer> healerIndexs = Overcharge.overchargeMatrix.get(state.location.encode());
            if (healerIndexs != null) {
                for (int index : healerIndexs) {
                    if (!state.overchargesUsed.contains(index)) overchargesLeft++;
                }
            }
            int minOvercharges = 3;
            if (Research.getLevel(UnitType.Mage) == 0) minOvercharges = 3;
            if (Research.getLevel(UnitType.Mage) == 1) minOvercharges = 2;
            if (Research.getLevel(UnitType.Mage) == 2) minOvercharges = 2;
            if (Research.getLevel(UnitType.Mage) >= 3) minOvercharges = 1;
            if (state.location.distanceSquaredTo(state.mage.target) <= Const.mageAttackRange && overchargesLeft > minOvercharges){
                //he trobat una sequencia que arriba al target!!
                System.out.println("He trobat sequencia em deixa a " + state.location + " i ataco a " + state.mage.target + " overcharges left " + overchargesLeft);
                System.out.print("Overcharges used: ");
                for (int index: state.overchargesUsed){
                    AuxUnit u = Units.myUnits.get(index);
                    System.out.print(u.getMapLocation() + "  ");
                }
                System.out.print("Overcharges left: ");
                for (int index: Overcharge.overchargeMatrix.get(state.location.encode())){
                    AuxUnit u = Units.myUnits.get(index);
                    System.out.print(u.getMapLocation() + "  ");
                }
                return state;
            }
            ArrayList<MageInfo> nextStates = getNextStates(state.getOvercharged(), true, Units.canBlink);
            if (nextStates != null) {
                for (MageInfo newState : nextStates) {
                    queue.offer(newState);
                }
            }
        }
        //no ha trobat cap manera d'arribar a rang del target
        return null;
    }


    private void attackkk(){
        MageInfo state = findOPSequence();
        while (state != null){
            AuxUnit mage = state.mage;
            System.out.println("ATTACKKKK " + mage.getID());
            System.out.println(Utils.round + " ha trobat sequence! My loc " + mage.getMapLocation() + " target " + mage.target);
            ArrayList<Integer> moveSequence = state.movesUsed;
            for (Integer code: moveSequence){
                int enc = movementDestination(code).encode();
                Units.newOccupiedPositions.add(enc);
            }
            for (Integer code: moveSequence){
                int type = movementType(code);
                AuxMapLocation dest = movementDestination(code);
                if (type == MOVE){
                    System.out.println("    - Move to " + dest);
                    int dir = mage.getMapLocation().dirBFSTo(dest);
                    mage.immune = true;
                    AuxUnit molesta = dest.getUnit();
                    if (molesta != null && molesta.myTeam && molesta.isRobot()){
                        int moved = MovementManager.getInstance().move(molesta, MovementManager.FORCED);
                        if (moved == 8){
                            System.out.println("Didn't move :(");
                            Wrapper.disintegrate(molesta);
                        }
                    }
                    mage.immune = false;
                    Wrapper.moveRobot(mage, dir);
                }
                if (type == BLINK){
                    mage.immune = true;
                    AuxUnit molesta = dest.getUnit();
                    if (molesta != null && molesta.myTeam && molesta.isRobot()) {
                        int moved = MovementManager.getInstance().move(molesta, MovementManager.FORCED);
                        if (moved == 8){
                            System.out.println("Didn't move :(");
                            Wrapper.disintegrate(molesta);
                        }
                    }
                    System.out.println("    - Blink to " + dest);
                    mage.immune = false;
                    Wrapper.blink(mage, dest);
                }
                if (type == OVERCHARGE){
                    System.out.println("    - Overcharge");
                    HashSet<Integer> healerList = Overcharge.overchargeMatrix.get(mage.getMapLocation().encode());
                    int maxDist = -1;
                    int maxIndex = -1;
                    for (int index: healerList){
                        AuxUnit healer = Units.myUnits.get(index);
                        int dist = healer.getMapLocation().distanceSquaredTo(mage.target);
                        if (dist > maxDist){
                            maxDist = dist;
                            maxIndex = index;
                        }
                    }
                    if (maxDist != -1){
                        if (mage.canAttack()) regularAttack(mage); //ataca just abans de ferse overcharge
                        Wrapper.overcharge(Units.myUnits.get(maxIndex), mage);
                    }else{
                        System.out.println("INTENT DE OVERCHARGE PERO NO HI HA NINGU A RANG :(");
                        return;
                    }
                }
            }

            for (Integer code: moveSequence){
                int enc = movementDestination(code).encode();
                Units.newOccupiedPositions.remove(enc);
            }
            System.out.println("END ATTACKKKKK " + state.mage.getID());
            state.mage.target = getTarget(state.mage);
            regularAttack(state.mage);
            //state = findOPSequence();
            state = null;
        }
    }


    private void doAction(AuxUnit mage){
        try {
            if (mage.target != null && !mage.exploretarget && mage.target.distanceSquaredTo(mage.getMapLocation()) < 90) {
                //if (trySpecialMove()) return;
            }
            regularAttack(mage);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    void doActions(){
        attackkk();
        for(int index: Units.mages){
            AuxUnit mage = Units.myUnits.get(index);
            if (mage.isInGarrison() || mage.isInSpace() || mage.isDead()) continue;
            doAction(mage);
        }
    }

    private boolean isBetter(AuxMapLocation myLoc, AuxMapLocation A, AuxMapLocation B){
        try {
            if (A == null) return false;
            if (!A.isOnMap()) return false;
            double a = Target.mageHits[A.x][A.y];

            if (B == null) return true;
            if (!B.isOnMap()) return true;
            double b = Target.mageHits[B.x][B.y];

            if (Target.MIN_VALUE < a && Target.MIN_VALUE >= b) return true;
            if (Target.MIN_VALUE >= a && Target.MIN_VALUE < b) return false;
            if (Target.MIN_VALUE < a && Target.MIN_VALUE < b) return (a > b);

            return (myLoc.distanceBFSTo(A) < myLoc.distanceBFSTo(B));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private AuxMapLocation getBestEnemy(AuxMapLocation myLoc){
        try {
            AuxMapLocation bestTarget = null;
            for (AuxUnit enemy: Units.enemies) {
                if (enemy.isDead()) continue;
                AuxMapLocation enemyLocation = enemy.getMapLocation();
                if (isBetter(myLoc, enemyLocation, bestTarget)) bestTarget = enemyLocation;
            }
            return bestTarget;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private AuxMapLocation getBestTarget(AuxUnit mage){
        try {
            if (Rocket.callsToRocket.containsKey(mage.getID())) {
                mage.exploretarget = true;
                return Rocket.callsToRocket.get(mage.getID());
            }
            /*if (unit.getHealth() < 100) {
                AuxMapLocation ans = getBestHealer(unit.getMapLocation());
                if (ans != null) return ans;
            }*/
            return getBestEnemy(mage.getMapLocation());
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    AuxMapLocation getTarget(AuxUnit mage) {
        try {
            AuxMapLocation ans = getBestTarget(mage);
            if (ans != null) {
                System.out.println(Utils.round + "  " + mage.getMapLocation() + " ha trobat unit target " + ans);
                return ans;
            }
            mage.exploretarget = true;
            System.out.println(Utils.round + "  " + mage.getMapLocation() + " busca explorer target " + mage.target);
            return Explore.findExploreObjective(mage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}