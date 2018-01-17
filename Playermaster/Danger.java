

import bc.*;

import java.util.HashSet;
import java.util.*;

/**
 * Created by Ivan on 1/16/2018.
 */
public class Danger {



    static double[] DPS;
    static double[] DPSshort;
    static int [] minDist;
    static final int INF = 1000000000;
    static HashSet<Integer> attackers;

    static final double winningProportion = 1.15;

    static MapLocation[] locUnits, locEnemyUnits;
    static boolean[] dangUnits, dangEnemyUnits, visitedUnits, visitedEnemyUnits;

    //MyLoc = position, canMove = directions you want to compute {9 is center}
    static void computeDanger(MapLocation myLoc, boolean[] canMove){

        DPS = new double[9];
        DPSshort = new double[9];
        minDist = new int[9];
        for (int i = 0; i < 9; ++i){
            DPS[i] = 0;
            minDist[i] = INF;
            DPSshort[i] = 0;
        }

        VecUnit enemies = UnitManager.gc.senseNearbyUnitsByTeam(myLoc, 100, UnitManager.enemyTeam);
        for(int i = 0; i < enemies.size(); ++i){
            Unit enemy = enemies.get(i);
            double dps = 0;
            long ar = 0;
            long arshort = 0;
            if (MovementManager.getInstance().dangerousUnit(enemy)) {
                dps = (double) enemy.damage() / enemy.attackCooldown();
                ar = getMaxDanger(enemy);
                arshort = enemy.attackRange();
            }
            for (int j = 0; j < 9; ++j){
                if (!canMove[j] && j < 8) continue;
                MapLocation newLoc = myLoc.add(MovementManager.allDirs[j]);
                long d = enemy.location().mapLocation().distanceSquaredTo(newLoc);
                if (dps > 0 && d <= ar) DPS[j] += dps;
                if (dps > 0 && d <= arshort) DPS[j] += dps/1000;
                minDist[j] = Math.min(minDist[j], (int)d);
            }
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

        int n = (int) UnitManager.units.size();
        int m = (int) UnitManager.enemyUnits.size();

        locUnits = new MapLocation[n];
        locEnemyUnits = new MapLocation[m];

        dangUnits = new boolean[n];
        dangEnemyUnits = new boolean[m];

        visitedUnits = new boolean[n];
        visitedEnemyUnits = new boolean[m];

        attackers = new HashSet<>();

        for (int i = 0; i < n; ++i){
            Unit unit = UnitManager.units.get(i);
            dangUnits[i] = (MovementManager.getInstance().dangerousUnit(unit) && UnitManager.gc.isMoveReady(unit.id()));
            Location loc = unit.location();
            if (!loc.isInGarrison()) locUnits[i] = unit.location().mapLocation();
            else dangUnits[i] = false;
            visitedUnits[i] = false;
        }

        for (int i = 0; i < m; ++i){
            Unit unit = UnitManager.enemyUnits.get(i);
            locEnemyUnits[i] = unit.location().mapLocation();
            dangEnemyUnits[i] = MovementManager.getInstance().dangerousUnit(unit);
            visitedEnemyUnits[i] = false;
        }



        for (int i = 0; i < n; ++i){
            if (dangUnits[i]) BFS(i);
        }

    }


    static void BFS (int i){
        if (visitedUnits[i]) return;
        HashSet<Integer> possibleAttackers = new HashSet<Integer>();
        HashSet<Integer> possibleDefenders = new HashSet<Integer>();
        Queue<Integer> q = new LinkedList<Integer>();
        visitedUnits[i] = true;
        q.add(encode(i, 0));
        possibleAttackers.add(UnitManager.units.get(i).id());
        while (!q.isEmpty()) {
            int a = q.poll();
            int y = decodeY(a);
            int x = decodeX(a);
            if (y == 0){
                for (int j = 0; j < UnitManager.enemyUnits.size(); ++j){
                    if (!visitedEnemyUnits[j] && dangEnemyUnits[j] && locEnemyUnits[j].distanceSquaredTo(locUnits[x]) <= getMaxDanger(UnitManager.enemyUnits.get(j))){
                        q.add(encode(j, 1));
                        possibleDefenders.add(UnitManager.enemyUnits.get(j).id());
                        visitedEnemyUnits[j] = true;
                    }
                }
            }
            else{
                for (int j = 0; j < UnitManager.units.size(); ++j){
                    if (!visitedUnits[j] && dangUnits[j] && locUnits[j].distanceSquaredTo(locEnemyUnits[x]) <= getMaxDanger(UnitManager.units.get(j))){
                        q.add(encode(j, 0));
                        possibleAttackers.add(UnitManager.units.get(j).id());
                        visitedUnits[j] = true;
                    }
                }
            }
        }

        //System.err.println("Possible attackers");
        //System.err.println(possibleAttackers.size());
        //System.err.println("Possible defenders");
        //System.err.println(possibleDefenders.size());

        if ((double)possibleAttackers.size() > (double)possibleDefenders.size()*winningProportion){
            for (Integer a : possibleAttackers) attackers.add(a);
        }

    }

    static int getMaxDanger(Unit unit){
        if (UnitManager.aggro) {
            if (unit.unitType() == UnitType.Ranger) return 68;
            if (unit.unitType() == UnitType.Mage) return 65;
            if (unit.unitType() == UnitType.Knight) return 8;
        }
        else{
            if (unit.unitType() == UnitType.Ranger) return 50;
            if (unit.unitType() == UnitType.Mage) return 45;
            if (unit.unitType() == UnitType.Knight) return 2;
        }
        return 0;
    }

}
