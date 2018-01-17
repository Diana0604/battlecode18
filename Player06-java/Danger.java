import bc.*;
/**
 * Created by Ivan on 1/16/2018.
 */
public class Danger {

    static double[] DPS;
    static int [] minDist;
    static final int INF = 1000000000;

    //MyLoc = position, canMove = directions you want to compute {9 is center}
    static void computeDanger(MapLocation myLoc, boolean[] canMove){

        DPS = new double[9];
        minDist = new int[9];
        for (int i = 0; i < 9; ++i){
            DPS[i] = 0;
            minDist[i] = INF;
        }

        VecUnit enemies = UnitManager.gc.senseNearbyUnitsByTeam(myLoc, 100, UnitManager.enemyTeam);
        for(int i = 0; i < enemies.size(); ++i){
            Unit enemy = enemies.get(i);
            double dps = 0;
            long ar = 0;
            if (MovementManager.getInstance().dangerousUnit(enemy)) {
                dps = (double) enemy.damage() / enemy.attackCooldown();
                ar = enemy.attackRange();
            }
            for (int j = 0; j < 9; ++j){
                if (!canMove[j]) continue;
                MapLocation newLoc = myLoc.add(MovementManager.allDirs[j]);
                long d = enemy.location().mapLocation().distanceSquaredTo(newLoc);
                if (dps > 0 && d <= ar) DPS[j] += dps;
                minDist[j] = Math.min(minDist[j], (int)d);
            }
        }
    }
}
