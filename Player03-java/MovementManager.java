

import bc.*;

import java.util.HashMap;

/**
 * Created by Ivan on 1/14/2018.
 * holi
 */
public class MovementManager {

    static MovementManager instance;
    private GameController gc;
    public final int INF = 1000000000;

    private BugPathfindingData data;
    private MapLocation myLoc;
    private int id;
    private boolean[] canMove;
    private long attackRange;
    private boolean attacker;

    private VecUnit enemies;


    private static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static MovementManager getInstance(){
        if (instance == null) instance = new MovementManager();
        return instance;
    }

    HashMap<Integer, BugPathfindingData> bugpathData;

    public MovementManager(){
        gc = UnitManager.gc;
        bugpathData = new HashMap<>();
    }


    public Direction dirTo(int destX, int destY) {
        PathfinderNode myNode = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }

    public Direction dirTo(MapLocation loc) {
        return dirTo(loc.getX(), loc.getY());
    }

    public boolean moveBFSTo(MapLocation target) {
        int index = Pathfinder.getIndex(dirTo(target));
        //Direction dir = dirTo(unit, target);
        if (canMove[index]) {
            if (isSafe(allDirs[index])) {
                gc.moveRobot(id, allDirs[index]);
                return true;
            }
        }
        index = -1;
        double mindist = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , target.getX(), target.getY()).dist;
        for (int i = 0; i < allDirs.length; ++i){
            if (!canMove[i]) continue;
            MapLocation newLoc = myLoc.add(allDirs[i]);
            PathfinderNode node = Pathfinder.getInstance().getNode(newLoc.getX(), newLoc.getY(), target.getX(), target.getY());
            if (node.dist < mindist){
                mindist = node.dist;
                index = i;
            }
        }
        if (index >= 0){
            if (isSafe(allDirs[index])) {
                gc.moveRobot(id, allDirs[index]);
                return true;
            }
        }
        return false;
    }

    double distance (MapLocation loc1, MapLocation loc2){
        return Pathfinder.getInstance().getNode(loc1.getX(), loc1.getY(), loc2.getX(), loc2.getY()).dist;
    }

    public boolean naiveMoveTo(MapLocation target){
        if (!gc.isMoveReady(id)) return false;

        /*reset if new target*/
        if (data.target == null || !target.equals(data.target)){
            data.target = target;
            data.soft_reset(myLoc);
        }
        if (target != null && distance(myLoc, target) < data.minDist){
            data.soft_reset(myLoc);
        }

        /*if not an obstacle --->  BFS*/
        if (data.obstacle == null){
            if (moveBFSTo(target)){
                bugpathData.put(id, data);
                return true;
            }
        }

        if (BugPath(data)){
            bugpathData.put(id, data);
            return true;
        }
        return false;
    }

    public boolean BugPath(BugPathfindingData data){
        Direction dir;
        if (data.obstacle == null) dir = myLoc.directionTo(data.target);
        else dir = myLoc.directionTo(data.obstacle);
        int index = Pathfinder.getIndex(dir);
        if (canMove[index]){
            data.obstacle = null;
        }
        else {
            int cont = 0;
            while (!canMove[index] && cont < 20) {
                MapLocation newLoc = myLoc.add(allDirs[index]);
                if (!UnitManager.getInstance().map.onMap(newLoc)) data.left = !data.left;
                data.obstacle = newLoc;
                if (data.left) index = (index + 1)%8;
                else index = (index + 7)%8;
                ++cont;
            }
        }
        if (canMove[index]){
            if (isSafe(allDirs[index])){
                gc.moveRobot(id, allDirs[index]);
                return true;
            }
        }
        return false;
    }



    public void moveTo(Unit unit, MapLocation target){
        if (target == null) return;
        if (!gc.isMoveReady(unit.id())) return;

        id = unit.id();
        myLoc = unit.location().mapLocation();
        if (!bugpathData.keySet().contains(id)){
            data = new BugPathfindingData();
        }
        else data = bugpathData.get(id);
        attackRange = unit.attackRange();
        attacker = dangerousUnit(unit);
        enemies = gc.senseNearbyUnitsByTeam(myLoc, 70, UnitManager.enemyTeam);
        canMove = new boolean[9];
        for (int i = 0; i < 9; ++i){
            if (gc.canMove(id, allDirs[i])) {
                canMove[i] = true;
            }
            else canMove[i] = false;
        }

        UnitManager unitManager = UnitManager.getInstance();

        data.DPSreceived = new double[9];
        data.minDistToEnemy = new int[9];
        for (int i = 0; i < 9; ++i){
            data.DPSreceived[i] = 0;
            data.minDistToEnemy[i] = INF;
        }


        for(int i = 0; i < enemies.size(); ++i){
            Unit enemy = enemies.get(i);
            double dps = 0;
            long ar = 0;
            if (dangerousUnit(enemy)) {
                dps = (double) enemy.damage() / enemy.attackCooldown();
                ar = enemy.attackRange();
            }
            for (int j = 0; j < 9; ++j){
                if (!canMove[j]) continue;
                MapLocation newLoc = myLoc.add(allDirs[j]);
                long d = enemy.location().mapLocation().distanceSquaredTo(newLoc);
                if (dps > 0 && d <= ar) data.DPSreceived[j] += dps;
                data.minDistToEnemy[j] = Math.min(data.minDistToEnemy[j], (int)d);
            }
        }

        greedyMove();

        naiveMoveTo(target);

        //safeMoveTo(unit, target);

        bugpathData.put(id, data);
    }

    int bestIndex(int i, int j){
        if (data.DPSreceived[i] > data.DPSreceived[j]) return j;
        if (data.DPSreceived[i] < data.DPSreceived[j]) return i;
        if (attacker){
            if (data.minDistToEnemy[i] > attackRange && data.minDistToEnemy[j] < attackRange) return j;
            if (data.minDistToEnemy[i] < attackRange && data.minDistToEnemy[j] > attackRange) return i;
            if (data.minDistToEnemy[i] < attackRange){
                if (data.minDistToEnemy[i] >= data.minDistToEnemy[j]) return i;
                return j;
            }
        }
        return i;
    }

    void greedyMove(){
        if (!gc.isMoveReady(id)) return;
        int index = 8;
        for (int i = 0; i < 8; ++i) if (canMove[i]) index = bestIndex(index, i);

        if (index != 8){
            gc.moveRobot(id, allDirs[index]);
            data.soft_reset(myLoc);
        }
    }

    boolean isSafe(Direction dir){
        int ind = Pathfinder.getIndex(dir);
        return (data.DPSreceived[ind] <= 0);
    }

    boolean dangerousUnit(Unit unit){
        return (unit.unitType() == UnitType.Knight || unit.unitType() == UnitType.Mage || unit.unitType() == UnitType.Ranger);
    }



}
