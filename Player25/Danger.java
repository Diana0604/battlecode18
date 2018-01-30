import bc.UnitType;

import java.util.HashSet;
import java.util.*;

class Danger {
    static double[] DPS;
    static double[] DPSlong;
    static int [] minDist;
    static HashSet<Integer> attackers;

    static int[][] myDist;
    static int[][] enemyDist;

    static boolean enemySeen = false;
    static boolean knightSeen = false;

    private static HashMap<Integer, DangerData> dangerData;

    static final double winningProportion = 1.05;

    private static AuxMapLocation[] locUnits, locEnemyUnits;
    private static boolean[] dangUnits, dangEnemyUnits, visitedUnits, visitedEnemyUnits;

    static Queue<Integer> myQueue, enemyQueue;

    static void initTurn(){
        updateAttackers();
        updateDangerMatrix();
        dangerData = new HashMap<>();
    }

    static void updateDangerMatrix(){
        try {
            while (!myQueue.isEmpty()){
                int a = myQueue.poll();
                AuxMapLocation loc = new AuxMapLocation(a);
                int d = myDist[loc.x][loc.y];
                for (int i = 0; i < 8; ++i){
                    AuxMapLocation newLoc = loc.add(i);
                    if (newLoc.isOnMap()){
                        if (myDist[newLoc.x][newLoc.y] == 0){
                            myDist[newLoc.x][newLoc.y] = d+1;
                            if (newLoc.isPassable()) myQueue.add(newLoc.encode());
                        }
                    }
                }
            }

            while (!enemyQueue.isEmpty()){
                int a = enemyQueue.poll();
                AuxMapLocation loc = new AuxMapLocation(a);
                int d = enemyDist[loc.x][loc.y];
                for (int i = 0; i < 8; ++i){
                    AuxMapLocation newLoc = loc.add(i);
                    if (newLoc.isOnMap()){
                        if (enemyDist[newLoc.x][newLoc.y] == 0){
                            enemyDist[newLoc.x][newLoc.y] = d+1;
                            if (newLoc.isPassable()) enemyQueue.add(newLoc.encode());
                        }
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    //MyLoc = position, canMove = directions you want to compute {9 is center}
    static void computeDanger(AuxUnit unit){
        try {
            AuxMapLocation myLoc = unit.getMapLocation();
            if (myLoc == null) return;

            int enc = myLoc.encode();

            if (dangerData.containsKey(enc)){
                DangerData data = dangerData.get(enc);
                DPS = data.DPS;
                DPSlong = data.DPSlong;
                minDist = data.minDist;
                return;
            }

            DangerData data = new DangerData();

            data.DPS = new double[9];
            data.DPSlong = new double[9];
            data.minDist = new int[9];
            for (int i = 0; i < 9; ++i) {
                data.DPS[i] = 0;
                data.minDist[i] = Const.INFS;
                data.DPSlong[i] = 0;
            }

            AuxUnit[] enemies = Wrapper.senseUnits(myLoc.x, myLoc.y, 100, false);
            for (AuxUnit enemy : enemies) {
                double dps = 0;
                long arshort = 0;
                long arslong = 0;
                if (unit.getType() == UnitType.Worker || MovementManager.getInstance().dangerousUnit(enemy.getType())) {
                    dps = Units.getDamage(enemy.getType()) / Units.getAttackCooldown(enemy.getType());
                    if (unit.getType() == UnitType.Worker && enemy.getType() != UnitType.Worker && enemy.getType() != UnitType.Healer && enemy.getType() != UnitType.Rocket) dps += 0.0001;
                    arslong = Units.getAttackRangeLong(enemy.getType());
                    arshort = Units.getAttackRangeSafe(enemy.getType());
                }
                for (int j = 0; j < 9; ++j) {
                    if (!Wrapper.canMove(unit, j) && j < 8) continue;
                    AuxMapLocation newLoc = myLoc.add(j);
                    long d = enemy.getMapLocation().distanceSquaredTo(newLoc);
                    if (dps > 0 && d <= arshort) data.DPS[j] += dps;
                    if (dps > 0 && d <= arslong) data.DPSlong[j] += dps / (d + 1);
                    if (unit.getType() == UnitType.Ranger){
                        if (Utils.round >= 150 && d <= 10) data.minDist[j] = Math.min(data.minDist[j], (int) d);
                        else if (!(enemy.getType() == UnitType.Worker)) data.minDist[j] = Math.min(data.minDist[j], (int) d);
                    }
                    else if (enemy.getType() != UnitType.Worker) data.minDist[j] = Math.min(data.minDist[j], (int) d);
                }
            }

            dangerData.put(enc, data);
            DPS = data.DPS;
            DPSlong = data.DPSlong;
            minDist = data.minDist;

        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    private static int encode(int x, int y){
        return (x << 12) | y;
    }

    private static int decodeX(int code){
        return (code >> 12)&0xFFF;
    }

    private static int decodeY(int code){
        return code&0xFFF;
    }

    private static void updateAttackers(){
        try {

            knightSeen = false;
            if (Units.enemies.size() > 0) enemySeen = true;
            myQueue = new LinkedList<>();
            enemyQueue = new LinkedList<>();
            int n = Units.myUnits.size();
            int m = Units.enemies.size();

            locUnits = new AuxMapLocation[n];
            locEnemyUnits = new AuxMapLocation[m];

            dangUnits = new boolean[n];
            dangEnemyUnits = new boolean[m];

            visitedUnits = new boolean[n];
            visitedEnemyUnits = new boolean[m];

            attackers = new HashSet<>();

            myDist = new int[Mapa.W][Mapa.H];
            enemyDist = new int[Mapa.W][Mapa.H];

            for (int i = 0; i < n; ++i) {
                AuxUnit unit = Units.myUnits.get(i);
                dangUnits[i] = dangerousUnit(unit);
                locUnits[i] = unit.getMapLocation();
                if (locUnits[i] == null) dangUnits[i] = false;
                visitedUnits[i] = false;
                if (unit.getType() != UnitType.Worker){
                    myQueue.add(locUnits[i].encode());
                    myDist[locUnits[i].x][locUnits[i].y] = 1;
                }
            }

            for (int i = 0; i < m; ++i) {
                AuxUnit unit = Units.enemies.get(i);
                locEnemyUnits[i] = unit.getMapLocation();
                dangEnemyUnits[i] = dangerousUnit(unit);
                visitedEnemyUnits[i] = false;
                if (unit.getType() != UnitType.Worker){
                    enemyQueue.add(locEnemyUnits[i].encode());
                    enemyDist[locEnemyUnits[i].x][locEnemyUnits[i].y] = 1;
                }
                if (Units.enemies.get(i).getType() == UnitType.Knight) knightSeen = true;
            }


            for (int i = 0; i < n; ++i) {
                if (dangUnits[i]) BFS(i);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }

    }


    private static void BFS(int i){
        try {
            if (visitedUnits[i]) return;
            HashSet<Integer> possibleAttackers = new HashSet<>();
            HashSet<Integer> possibleDefenders = new HashSet<>();
            Queue<Integer> q = new LinkedList<>();
            visitedUnits[i] = true;
            q.add(encode(i, 0));
            AuxUnit unit = Units.myUnits.get(i);
            possibleAttackers.add(unit.getID());
            while (!q.isEmpty()) {
                int a = q.poll();
                int y = decodeY(a);
                int x = decodeX(a);
                if (y == 0) {
                    for (int j = 0; j < Units.enemies.size(); ++j) {
                        AuxUnit enemy = Units.enemies.get(j);
                        if (!visitedEnemyUnits[j] && dangEnemyUnits[j] && locEnemyUnits[j].distanceSquaredTo(locUnits[x]) <= Units.getAttackRangeExtra(enemy.getType())) {
                            q.add(encode(j, 1));
                            possibleDefenders.add(enemy.getID());
                            visitedEnemyUnits[j] = true;
                        }
                    }
                    /*for (int j = 0; j < Units.myUnits.size(); ++j){
                        AuxUnit ally = Units.myUnits.get(j);
                        if (!visitedUnits[j] && ally.getType() == UnitType.Healer && locUnits[j].distanceSquaredTo(locUnits[x]) <= 30){
                            possibleAttackers.add(ally.getID());
                            visitedUnits[j] = true;
                        }
                    }*/
                } else {
                    for (int j = 0; j < Units.myUnits.size(); ++j) {
                        AuxUnit unit2 = Units.myUnits.get(j);
                        if (unit2.isInGarrison()) continue;
                        unit2.frontline = true;
                        if(!unit2.canMove() || !unit2.canAttack()) continue;
                        if (!(locUnits[j].distanceSquaredTo(locEnemyUnits[x]) <= Units.getAttackRangeExtra(unit2.getType()))) continue;
                        if (!visitedUnits[j] && dangUnits[j]) {
                            q.add(encode(j, 0));
                            possibleAttackers.add(unit2.getID());
                            visitedUnits[j] = true;
                        }
                    }
                    /*
                    for (int j = 0; j < Units.enemies.size(); ++j){
                        AuxUnit enemy = Units.enemies.get(j);
                        if (!visitedEnemyUnits[j] && enemy.getType() == UnitType.Healer && locEnemyUnits[j].distanceSquaredTo(locEnemyUnits[x]) <= 30){
                            possibleDefenders.add(enemy.getID());
                            visitedEnemyUnits[j] = true;
                        }
                    }*/
                }
            }
            int siz = possibleDefenders.size();
            if (siz > 0) unit.frontline = true;
            if ((double) possibleAttackers.size() > (double) possibleDefenders.size()) {
                for (Integer a : possibleAttackers){
                    attackers.add(a);
                }
            }
            Factory.maxRangers = Math.max(Factory.maxRangers, possibleDefenders.size() + 15);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean dangerousUnit(AuxUnit unit) {
        try {
            UnitType type = unit.getType();

            if (type == UnitType.Knight || type == UnitType.Mage || type == UnitType.Ranger) return true;
            if (Units.canOverCharge && type == UnitType.Healer && unit.canUseAbility()) return true;
            return false;
        }catch(Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    static class DangerData{
        double[] DPS;
        double[] DPSlong;
        int[] minDist;
    }

}
