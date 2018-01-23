

import bc.UnitType;

import java.util.HashSet;
import java.util.*;

/**
 * Created by Ivan on 1/16/2018.
 */
public class Danger {



    static double[] DPS;
    static double[] DPSlong;
    static int [] minDist;
    static final int INF = 1000000000;
    static HashSet<Integer> attackers;

    static HashMap<Integer, DangerData> dangerData;

    static final double winningProportion = 1.05;

    static AuxMapLocation[] locUnits, locEnemyUnits;
    static boolean[] dangUnits, dangEnemyUnits, visitedUnits, visitedEnemyUnits;

    static void reset(){
        dangerData = new HashMap<>();
    }

    //MyLoc = position, canMove = directions you want to compute {9 is center}
    static void computeDanger(AuxUnit unit){
        try {
            AuxMapLocation myLoc = unit.getMaplocation();
            if (myLoc == null) return;

            int enc = Data.encodeOcc(myLoc.x, myLoc.y);

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
                data.minDist[i] = INF;
                data.DPSlong[i] = 0;
            }

            AuxUnit[] enemies = Wrapper.senseUnits(myLoc.x, myLoc.y, 100, false);
            for (int i = 0; i < enemies.length; ++i) {
                AuxUnit enemy = enemies[i];
                double dps = 0;
                long arshort = 0;
                long arslong = 0;
                if (MovementManager.getInstance().dangerousUnit(enemy.getType())) {
                    dps = Wrapper.getDamage(enemy.getType()) / Wrapper.getAttackCooldown(enemy.getType());
                    arslong = Wrapper.getAttackRangeLong(enemy.getType());
                    arshort = Wrapper.getAttackRangeSafe(enemy.getType());
                }
                for (int j = 0; j < 9; ++j) {
                    if (!Wrapper.canMove(unit, j) && j < 8) continue;
                    AuxMapLocation newLoc = myLoc.add(j);
                    long d = enemy.getMaplocation().distanceSquaredTo(newLoc);
                    if (dps > 0 && d <= arshort) data.DPS[j] += dps;
                    if (dps > 0 && d <= arslong) data.DPSlong[j] += dps/(d+1);
                    data.minDist[j] = Math.min(data.minDist[j], (int) d);
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


    static int encode(int x, int y){
        return (x << 12) | y;
    }

    static int decodeX(int code){
        return (code >> 12)&0xFFF;
    }

    static int decodeY(int code){
        return code&0xFFF;
    }

    static void updateAttackers(){
        try {
            int n = Data.myUnits.length;
            int m = Data.enemies.length;

            locUnits = new AuxMapLocation[n];
            locEnemyUnits = new AuxMapLocation[m];

            dangUnits = new boolean[n];
            dangEnemyUnits = new boolean[m];

            visitedUnits = new boolean[n];
            visitedEnemyUnits = new boolean[m];

            attackers = new HashSet<>();

            for (int i = 0; i < n; ++i) {
                AuxUnit unit = Data.myUnits[i];
                dangUnits[i] = (MovementManager.getInstance().dangerousUnit(unit.getType()) && Data.gc.isMoveReady(unit.getID()) && Data.gc.isAttackReady(unit.getID()));
                locUnits[i] = unit.getMaplocation();
                if (locUnits[i] == null) dangUnits[i] = false;
                visitedUnits[i] = false;
            }

            for (int i = 0; i < m; ++i) {
                AuxUnit unit = Data.enemies[i];
                locEnemyUnits[i] = unit.getMaplocation();
                dangEnemyUnits[i] = MovementManager.getInstance().dangerousUnit(unit.getType());
                visitedEnemyUnits[i] = false;
            }


            for (int i = 0; i < n; ++i) {
                if (dangUnits[i]) BFS(i);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }

    }


    static void BFS (int i){
        try {
            if (visitedUnits[i]) return;
            HashSet<Integer> possibleAttackers = new HashSet<Integer>();
            HashSet<Integer> possibleDefenders = new HashSet<Integer>();
            Queue<Integer> q = new LinkedList<Integer>();
            visitedUnits[i] = true;
            q.add(encode(i, 0));
            possibleAttackers.add(Data.myUnits[i].getID());
            while (!q.isEmpty()) {
                int a = q.poll();
                int y = decodeY(a);
                int x = decodeX(a);
                if (y == 0) {
                    for (int j = 0; j < Data.enemies.length; ++j) {
                        if (!visitedEnemyUnits[j] && dangEnemyUnits[j] && locEnemyUnits[j].distanceSquaredTo(locUnits[x]) <= Wrapper.getAttackRangeExtra(Data.enemies[j].getType())) {
                            q.add(encode(j, 1));
                            possibleDefenders.add(Data.enemies[j].getID());
                            visitedEnemyUnits[j] = true;
                        }
                    }
                } else {
                    for (int j = 0; j < Data.myUnits.length; ++j) {
                        if (Data.myUnits[j].isInGarrison()) continue;
                        if (!visitedUnits[j] && dangUnits[j] && locUnits[j].distanceSquaredTo(locEnemyUnits[x]) <= Wrapper.getAttackRangeExtra(Data.myUnits[j].getType())) {
                            q.add(encode(j, 0));
                            possibleAttackers.add(Data.myUnits[j].getID());
                            visitedUnits[j] = true;
                        }
                    }
                }
            }
            if ((double) possibleAttackers.size() > (double) possibleDefenders.size()) {
                for (Integer a : possibleAttackers) attackers.add(a);
            }
            Factory.maxRangers = Math.max(Factory.maxRangers, possibleDefenders.size() + 15);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean dangerousUnit(UnitType type) {
        try {
            return (type == UnitType.Knight || type == UnitType.Mage || type == UnitType.Ranger);
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
