

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


    public Direction dirTo(Unit unit, int destX, int destY) {
        MapLocation myLoc = unit.location().mapLocation();
        PathfinderNode myNode = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }

    public Direction dirTo(Unit unit, MapLocation loc) {
        return dirTo(unit, loc.getX(), loc.getY());
    }

    public boolean moveBFSTo(Unit unit, MapLocation target) {
        BugPathfindingData data = bugpathData.get(unit);
        Direction dir = dirTo(unit, target);
        if (gc.canMove(unit.id(), dir)) {
            if (isSafe(dir)) {
                gc.moveRobot(unit.id(), dir);
                return true;
            }
        }
        MapLocation myLoc = unit.location().mapLocation();
        dir = null;
        double mindist = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , target.getX(), target.getY()).dist;
        for (int i = 0; i < allDirs.length; ++i){
            if (!gc.canMove(unit.id(), allDirs[i])) continue;
            MapLocation newLoc = myLoc.add(allDirs[i]);
            PathfinderNode node = Pathfinder.getInstance().getNode(newLoc.getX(), newLoc.getY(), target.getX(), target.getY());
            if (node.dist < mindist){
                mindist = node.dist;
                dir = allDirs[i];
            }
        }
        if (dir != null){
            if (isSafe(dir)) {
                gc.moveRobot(unit.id(), dir);
                return true;
            }
        }
        return false;
    }

    double distance (MapLocation loc1, MapLocation loc2){
        return Pathfinder.getInstance().getNode(loc1.getX(), loc1.getY(), loc2.getX(), loc2.getY()).dist;
    }

    public boolean naiveMoveTo(Unit unit, MapLocation target){
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
            if (moveBFSTo(unit, target)){
                bugpathData.put(unit.id(), data);
                return true;
            }
        }

        if (BugPath(unit, data)){
            bugpathData.put(unit.id(), data);
            return true;
        }
        return false;
    }

    public boolean BugPath(Unit unit, BugPathfindingData data){
        Direction dir;
        MapLocation myLoc = unit.location().mapLocation();
        if (data.obstacle == null) dir = myLoc.directionTo(data.target);
        else dir = myLoc.directionTo(data.obstacle);
        if (gc.canMove(unit.id(), dir)){
            data.obstacle = null;
        }
        else {
            int cont = 0;
            while (!gc.canMove(unit.id(), dir) && cont < 20) {
                MapLocation newLoc = myLoc.add(dir);
                if (!UnitManager.getInstance().map.onMap(newLoc)) data.left = !data.left;
                data.obstacle = newLoc;
                if (data.left) dir = Pathfinder.rotateLeft(dir);
                else dir = Pathfinder.rotateRight(dir);
                ++cont;
            }
        }
        if (gc.canMove(unit.id(), dir)){ //Todo: add safety
            if (isSafe(dir)){
                gc.moveRobot(unit.id(), dir);
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

        UnitManager unitManager = UnitManager.getInstance();

        data.DPSreceived = new double[9];
        data.minDistToEnemy = new int[9];
        for (int i = 0; i < 9; ++i){
            data.DPSreceived[i] = 0;
            data.minDistToEnemy[i] = INF;
        }


        for(int i = 0; i < unitManager.enemyUnits.size(); ++i){
            Unit enemy = unitManager.enemyUnits.get(i);
            for (int j = 0; j < 9; ++j){
                MapLocation newLoc = myLoc.add(allDirs[j]);
                long d = enemy.location().mapLocation().distanceSquaredTo(newLoc);
                if (dangerousUnit(enemy) && d <= enemy.attackRange()) data.DPSreceived[j] += (double)enemy.damage()/enemy.attackCooldown();
                data.minDistToEnemy[j] = Math.min(data.minDistToEnemy[j], (int)d);
            }
        }

        greedyMove(unit, target);

        naiveMoveTo(unit, target);

        //safeMoveTo(unit, target);

        bugpathData.put(id, data);


        if (unit.damage() > 0) greedyMove(unit, target);
    }

    int bestIndex(int i, int j, long ar, int dmg){
        if (data.DPSreceived[i] > data.DPSreceived[j]) return j;
        if (data.DPSreceived[i] < data.DPSreceived[j]) return i;
        if (dmg > 0){
            if (data.minDistToEnemy[i] > ar && data.minDistToEnemy[j] < ar) return j;
            if (data.minDistToEnemy[i] < ar && data.minDistToEnemy[j] > ar) return i;
            if (data.minDistToEnemy[i] < ar){
                if (data.minDistToEnemy[i] >= data.minDistToEnemy[j]) return i;
                return j;
            }
        }
        return i;
    }

    void greedyMove(Unit unit, MapLocation target){
        if (!gc.isMoveReady(id)) return;
        if (unit.damage() <= 0) return;
        int index = 8;
        for (int i = 0; i < 8; ++i) if (gc.canMove(id, allDirs[i])) index = bestIndex(index, i, unit.attackRange(), unit.damage());

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
