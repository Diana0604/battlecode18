

import bc.UnitType;

import java.util.*;

public class Mage {
    private final double min_group = 1.5;
    private static Mage instance = null;

    //aixo es pel bfs de overcharge
    private static final int MOVE = 1;
    private static final int BLINK = 2;
    private static final int OVERCHARGE = 3;

    public AuxUnit unit;

    private double[][] multitargetArraY; //[i][j] = num. enemics - aliats que hi ha al quadrat 3x3
    private double[][] expandedTargetArray; //[i][j] = maxim score possible de disparar a rang <= 30

    private HashMap<Integer, Integer> objectiveArea; //todo borrar aixo??

    static Mage getInstance(){
        if (instance == null){
            instance = new Mage();
        }
        return instance;
    }

    static double unitValue(UnitType type){
        switch (type){
            case Worker: return 0.45;
            case Knight: return 1;
            default: return 0.95;
        }
    }

    //emplena les matrius (init turn)
    void computeMultiTarget(){
        try {
            multitargetArraY = new double[Mapa.W][Mapa.H];
            //todo donar diferents pesos a cada unit

            //per cada enemic, sumem 1 al quadrat 3x3
            for (AuxUnit enemy: Units.enemies) {
                if (enemy.isDead()) continue;
                AuxMapLocation loc = enemy.getMapLocation();
                for (int j = 0; j < 9; ++j) {
                    AuxMapLocation newLoc = loc.add(j);
                    if (newLoc.isOnMap()) {
                        multitargetArraY[newLoc.x][newLoc.y] += unitValue(enemy.getType());
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
                        multitargetArraY[newLoc.x][newLoc.y] -= unitValue(unit.getType());
                    }
                }
            }

            expandedTargetArray = new double[Mapa.W][Mapa.H];
            for (AuxUnit enemy: Units.enemies){
                if (enemy.isDead()) continue;
                AuxMapLocation loc = enemy.getMapLocation();
                double a = multitargetArraY[loc.x][loc.y];
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


/*
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
            ArrayList<MageMovement> getNextStates = new ArrayList<>();

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
                                    getNextStates.add(m);
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
                                getNextStates.add(m);
                                myMoves[3 + newLoc.x][3 + newLoc.y] = true;
                            }
                        }
                    }
                }
            }

            MageMovement move = null;
            for (MageMovement secondMove : getNextStates) {
                move = bestMove(secondMove, move);
            }

            return move;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }

    }
*/

    /*------------ REGULAR ATTACK ------------*/

    private class MageMovement{
        AuxMapLocation mloc;
        int dir;
        boolean moveFirst;
        Double value = null;
        AuxUnit bestTarget = null;

        MageMovement(AuxMapLocation loc, int d, boolean first){
            mloc = loc;
            dir = d;
            moveFirst = first;
        }

        MageMovement(){

        }

        public double getValue(){
            if (value == null) value = computeValue();
            return value;
        }

        double getValue(AuxMapLocation loc){
            double ans = 0;
            AuxUnit[] units = Wrapper.senseUnits(loc.x, loc.y, 2);
            for (AuxUnit unit1 : units) {
                if (unit1.myTeam) ans -= unitValue(unit1.getType());
                else ans += unitValue(unit1.getType());
            }
            return ans;
        }

        double computeValue() {
            try {
                AuxMapLocation finalLoc = unit.getMapLocation().add(dir).add(mloc);
                if (expandedTargetArray[finalLoc.x][finalLoc.y] <= 0) return 0;
                double mostValue = 0;
                for (int j = 0; j < Vision.Mx[30].length; ++j) {
                    AuxMapLocation newLoc = finalLoc.add(new AuxMapLocation(Vision.Mx[30][j], Vision.My[30][j]));
                    if (newLoc.isOnMap()) {
                        AuxUnit u = newLoc.getUnit();
                        if (u == null) continue;
                        double x = getValue(newLoc);
                        if (x > mostValue){
                            mostValue = x;
                            bestTarget = u;
                        }
                    }
                }
                return mostValue;
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    private void regularAttack() {
        try {
            if (!unit.canAttack()) return;
            MageMovement m = new MageMovement(new AuxMapLocation(0,0), 8, true);
            m.getValue();
            if (m.bestTarget != null) Wrapper.attack(unit, m.bestTarget);
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
        return ret;
    }


    int encodeMovement(int type, AuxMapLocation newLoc){
        int n1 = type;
        int n2 = newLoc.encode();
        return (n1 << 12)|n2;
    }

    int movementType(int code){
        return ((code >> 12)&0x3);
    }

    AuxMapLocation movementDestination(int code){
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
            if (mage.target == null) continue;
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
            if (state.location.distanceSquaredTo(state.mage.target) <= Const.mageAttackRange){
                //he trobat una sequencia que arriba al target!!
                System.out.println("He trobat sequencia em deixa a " + state.location + " i ataco a " + state.mage.target);
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


    void attackkk(){
        MageInfo state = findOPSequence();
        while (state != null){
            System.out.println("ATTACKKKK " + state.mage.getID());
            AuxUnit mage = state.mage;
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
                        if (mage.canAttack()) regularAttack(); //ataca just abans de ferse overcharge
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
            state = findOPSequence();
        }
    }


    void doAction(AuxUnit _unit){
        try {
            unit = _unit;
            if (unit.target != null && !unit.exploretarget && unit.target.distanceSquaredTo(unit.getMapLocation()) < 90) {
                //if (trySpecialMove()) return;
            }
            regularAttack();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    void doActions(){
        attackkk();
        for(int index: Units.mages){
            AuxUnit mage = Units.myUnits.get(index);
            doAction(mage);
        }
    }
































/*
    private OverchargeMage getSuperEpicComboState(){
        if (unit.target == null) return null; //todo canviar aixo?
        long initTime = System.nanoTime();
        final int MAX_TIME = 30000000/Units.unitTypeCount.get(UnitType.Mage);
        //System.out.println("");
        //System.out.println("====== ROUND " + Utils.round + " SUPER COMBO UNIT " + unit.getMapLocation() + " WITH TARGET " + unit.target);
        OverchargeMage initState = new OverchargeMage(unit);
        OverchargeMage bestState = initState;
        PriorityQueue<OverchargeMage> queue = new PriorityQueue<>();
        queue.offer(initState);
        int count = 1;
        while (!queue.isEmpty()){
            if (System.nanoTime() - initTime > MAX_TIME) {
                //System.out.println("TIME OUT!!");
                break;
            }
            //System.out.println("    New state " + count++);
            OverchargeMage state = queue.poll();
            if (bestState.compareTo(state) == 1) {
                //System.out.println("    UPDATES BEST SPACE!! kills: " + bestState.kills + "vs" + state.kills + ", damage " + bestState.damageDealt + "vs" + state.damageDealt);
                bestState = state;
            }
            if (state.canAttack){
                //perque l'arbre no surti molt gran nomes mirem atacant enemics
                AuxUnit[] enemies = Wrapper.senseUnits(state.mageLoc, Const.mageAttackRange, false);
                if (enemies != null) {
                    for (AuxUnit enemy : enemies) {
                        AuxMapLocation enemyLoc = enemy.getMapLocation();
                        MageAction action = new MageAction(ATTACK, enemyLoc);
                        OverchargeMage newState = state.nextStep(action);
                        if (newState != null) {
                            //System.out.println("    fa push d'un atac a " + enemyLoc);
                            queue.offer(newState);
                        }
                    }
                }
            }
            if (state.canMove){
                for (int i = 0; i < 8; i++){
                    //pruning: nomes s'acosta al target
                    AuxMapLocation newLoc = state.mageLoc.add(i);
                    int oldDist = state.mageLoc.distanceSquaredTo(unit.target);
                    int newDist = newLoc.distanceSquaredTo(unit.target);
                    if (oldDist < newDist) continue;
                    MageAction action = new MageAction(MOVE, newLoc);
                    OverchargeMage newState = state.nextStep(action);
                    if (newState != null) {
                        //System.out.println("    fa push d'un move a " + newLoc);
                        queue.offer(newState);
                    }
                }
            }
            if (state.canBlink){
                int range = Const.blinkRange;
                AuxMapLocation mageLoc = state.mageLoc;
                for (int i = 0; i < Vision.Mx[range].length; i++){
                    int dx = Vision.Mx[range][i];
                    int dy = Vision.My[range][i];
                    AuxMapLocation newLoc = new AuxMapLocation(mageLoc.x + dx, mageLoc.y + dy);
                    int oldDist = state.mageLoc.distanceSquaredTo(unit.target);
                    int newDist = newLoc.distanceSquaredTo(unit.target);
                    if (oldDist < newDist) continue;
                    MageAction action = new MageAction(BLINK, newLoc);
                    OverchargeMage newState = state.nextStep(action);
                    if (newState != null) {
                        //System.out.println("    fa push d'un blink a " + newLoc);
                        queue.offer(newState);
                    }
                }
            }
            //overcharge: triem el healer mes llunya del target
            int maxDist = -1;
            int maxIndex = -1;
            HashSet<Integer> healers = Overcharge.overchargeMatrix.get(state.mageLoc.encode());
            if (healers == null) continue;
            //System.out.println("    healers available" + healers.size());
            for (int index: healers){
                AuxUnit healer = Units.myUnits.get(index);
                AuxMapLocation healerLoc = healer.getMapLocation();
                if (state.overchargesUsed.contains(healerLoc)) {
                    //System.out.println("    healer already used: " + healerLoc);
                    continue; //si ja hem gastat aquest overcharge
                }
                int dist = healerLoc.distanceSquaredTo(unit.target);
                if (dist > maxDist){
                    maxDist = dist;
                    maxIndex = index;
                }
            }
            if (maxDist > -1){
                AuxUnit healer = Units.myUnits.get(maxIndex);
                AuxMapLocation healerLoc = healer.getMapLocation();
                MageAction action = new MageAction(OVERCHARGE, healerLoc);
                OverchargeMage newState = state.nextStep(action);
                if (newState != null) {
                    //System.out.println("    fa push d'un overcharge a " + healerLoc);
                    queue.offer(newState);
                }
            }
        }
        return bestState;
    }

    //desfem la recursio
    private void executeState(OverchargeMage state){
        if (state.lastAction == null) return;
        executeState(state.previousStep);
        MageAction action = state.lastAction;
        //System.out.println("    Executes action: " + action);
        AuxMapLocation targetLoc = action.target;
        switch(action.actionType){
            case ATTACK:
                AuxUnit target = targetLoc.getUnit();
                Wrapper.attack(unit, target);
                break;
            case MOVE:
                Wrapper.moveRobot(unit, unit.getMapLocation().dirBFSTo(targetLoc));
                break;
            case BLINK:
                Wrapper.blink(unit, targetLoc);
                break;
            case OVERCHARGE:
                AuxUnit healer = targetLoc.getUnit();
                Wrapper.overcharge(healer, unit);
                break;
        }
    }



    public class OverchargeMage implements Comparable<OverchargeMage>{
        AuxMapLocation mageLoc;
        boolean canAttack;
        boolean canMove;
        boolean canBlink;
        HashSet<AuxMapLocation> overchargesUsed;
        HashSet<AuxMapLocation> killedUnits;
        HashMap<AuxMapLocation, Integer> damagedUnits;
        int damageDealt;
        int kills; //no compta workers, pa que
        OverchargeMage previousStep;
        MageAction lastAction;

        OverchargeMage(AuxUnit mage){
            mageLoc = mage.getMapLocation();
            canAttack = mage.canAttack();
            canMove = mage.canMove();
            canBlink = mage.canUseAbility() && Units.canBlink;
            overchargesUsed = new HashSet<>();
            killedUnits = new HashSet<>();
            damagedUnits = new HashMap<>();
            damageDealt = 0;
            kills = 0;
            previousStep = null;
            lastAction = null;
        }

        //pocs parametres i tal
        OverchargeMage(AuxMapLocation mageLoc, boolean canAttack, boolean canMove, boolean canBlink,
                       HashSet<AuxMapLocation> overchargesUsed, HashSet<AuxMapLocation> killedUnits,
                       HashMap<AuxMapLocation, Integer> damagedUnits, int damageDealt, int kills,
                       OverchargeMage previousStep, MageAction lastAction){
            this.mageLoc = new AuxMapLocation(mageLoc.x, mageLoc.y);
            this.canAttack = canAttack;
            this.canMove = canMove;
            this.canBlink = canBlink;
            this.overchargesUsed = new HashSet<>();
            this.overchargesUsed.addAll(overchargesUsed);
            this.killedUnits = new HashSet<>();
            this.killedUnits.addAll(killedUnits);
            this.damagedUnits = new HashMap<>();
            this.damagedUnits.putAll(damagedUnits);
            this.damageDealt = damageDealt;
            this.kills = kills;
            this.previousStep = previousStep;
            this.lastAction = lastAction;
        }

        //el que treu primer es el que dona -1 amb tots els altres
        //maximitzo el numero de kills, i despres el damage fet
        public int compareTo(OverchargeMage other){
            if (this.kills > other.kills) return -1;
            if (this.kills < other.kills) return 1;
            if (this.damageDealt > other.damageDealt) return -1;
            if (this.damageDealt < other.damageDealt) return 1;
            return 0;
        }

        //donat un state i una accio, retorna el seguent state (amb link a l'anterior)
        OverchargeMage nextStep(MageAction action){
            switch(action.actionType){
                case ATTACK:
                    AuxMapLocation attackTarget = action.target;
                    if (killedUnits.contains(attackTarget)) return null;
                    int damagePerAttack = Units.mageDamage;
                    int damageThisTurn = 0;
                    int killsThisTurn = 0;
                    HashSet<AuxMapLocation> newKilledUnits = new HashSet<>();
                    newKilledUnits.addAll(killedUnits);
                    HashMap<AuxMapLocation, Integer> newDamagedUnits = new HashMap<>();
                    newDamagedUnits.putAll(damagedUnits);
                    for (int i = 0; i < 9; i++){ //mira el quadrat 3x3
                        AuxMapLocation location = attackTarget.add(i);
                        AuxUnit hitUnit = location.getUnit();
                        if (hitUnit == null) continue;
                        if (newKilledUnits.contains(location)) continue; //si ja l'hem matat abans
                        int multiplier;
                        if (hitUnit.myTeam) multiplier = -1;
                        else multiplier = 1;

                        int previousDamageDealt = 0; //damage que li hem fet en atacs anteriors
                        if (newDamagedUnits.containsKey(location)) previousDamageDealt = newDamagedUnits.get(location);

                        int previousHealthRemaining = Math.max(0, hitUnit.getHealth() - previousDamageDealt); //vida que li queda al target

                        int realDamagePerAttack = damagePerAttack; //damage que li fem al target en un atac
                        if (hitUnit.getType() == UnitType.Knight) realDamagePerAttack -= Units.knightBlock;

                        int realDamageDealtThisAttack = Math.min(previousHealthRemaining, realDamagePerAttack);
                        damageThisTurn += multiplier * realDamageDealtThisAttack;

                        int totalDamageToUnit = previousDamageDealt + realDamageDealtThisAttack;
                        newDamagedUnits.put(location, totalDamageToUnit);

                        //we kill the unit
                        if (previousHealthRemaining <= realDamagePerAttack){
                            if (hitUnit.getType() != UnitType.Worker) killsThisTurn += multiplier;
                            newKilledUnits.add(location);
                        }
                    }

                    return new OverchargeMage(mageLoc, false, canMove, canBlink, overchargesUsed,
                            newKilledUnits, newDamagedUnits, damageDealt + damageThisTurn, kills + killsThisTurn,
                            this, action);
                case MOVE:
                    AuxMapLocation moveLoc = action.target;
                    if (!moveLoc.isAccessible()) return null;
                    return new OverchargeMage(moveLoc, canAttack, false, canBlink, overchargesUsed,
                            killedUnits, damagedUnits, damageDealt, kills, this, action);
                case BLINK:
                    AuxMapLocation blinkLoc = action.target;
                    if (!blinkLoc.isAccessible()) return null;
                    return new OverchargeMage(blinkLoc, canAttack, canMove, false, overchargesUsed,
                            killedUnits, damagedUnits, damageDealt, kills, this, action);
                case OVERCHARGE:
                    if (canAttack && canMove && canBlink) return null;
                    AuxMapLocation healerLoc = action.target;
                    HashSet<AuxMapLocation> newOverchargesUsed = new HashSet<>();
                    newOverchargesUsed.addAll(overchargesUsed);
                    newOverchargesUsed.add(healerLoc);
                    return new OverchargeMage(mageLoc, true, true, Units.canBlink,
                            newOverchargesUsed, killedUnits, damagedUnits, damageDealt, kills, this, action);
                default: return null;
            }
        }
    }


    private class MageAction{
        int actionType;
        AuxMapLocation target;

        MageAction(int type, AuxMapLocation target){
            actionType = type;
            this.target = target;
        }

        public String toString(){
            String type = "";
            if (actionType == ATTACK) type = "Attack";
            if (actionType == MOVE) type = "Move";
            if (actionType == BLINK) type = "Blink";
            if (actionType == OVERCHARGE) type = "Overcharge";
            return ("Action [" + type + " " + target + "]");
        }
    }
*/
/*
    void attack(){
        OverchargeMage state = getSuperEpicComboState();
        if (state == null || state.kills > 0 || state.damageDealt > 200) executeState(state); //si no fa gaire no volem gastar overcharges
        else regularAttack();
    }
*/

    /*---------- GET TARGET ------------*/
/*
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
*/


    private boolean isBetter(AuxMapLocation myLoc, AuxMapLocation A, AuxMapLocation B){
        try {
            if (A == null) return false;
            if (!A.isOnMap()) return false;
            double a = multitargetArraY[A.x][A.y];
            if (a < min_group) return false;

            if (B == null) return true;
            if (!B.isOnMap()) return true;
            double b = multitargetArraY[B.x][B.y];
            if (b < min_group) return true;
            if (a < b) return false;
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

    AuxMapLocation getTarget(AuxUnit _unit) {
        try {
            AuxMapLocation ans = getBestTarget(_unit);
            if (ans != null) return ans;
            _unit.exploretarget = true;
            return Explore.findExploreObjective(_unit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}