import bc.*;

import java.util.HashMap;

/**
 * Created by Ivan on 1/14/2018.
 * holi
 */
public class MovementManager {

    static MovementManager instance;
    private GameController gc;

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
        Direction dir = dirTo(unit, target);
        if (gc.canMove(unit.id(), dir)) {
            gc.moveRobot(unit.id(), dir);
            return true;
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
            gc.moveRobot(unit.id(), dir);
            return true;
        }
        return false;
    }

    public void moveTo(Unit unit, MapLocation target){
        /*sanity check*/
        if (target == null) return;
        if (!gc.isMoveReady(unit.id())) return;

        /*my data*/
        MapLocation myLoc = unit.location().mapLocation();
        BugPathfindingData myData;
        int id = unit.id();
        if (!bugpathData.keySet().contains(id)){
            myData = new BugPathfindingData();
        }
        else myData = bugpathData.get(id);

        /*reset if new target*/
        if (myData.target == null || !target.equals(myData.target)){
            myData.target = target;
            myData.soft_reset(myLoc);
        }
        if (target != null && myLoc.distanceSquaredTo(target) < myData.minDist){
            myData.soft_reset(myLoc);
        }

        /*if not an obstacle --->  BFS*/
        if (myData.obstacle == null){
            if (moveBFSTo(unit, target)) return;
        }

        BugPath(unit, myData);

    }

    public void BugPath(Unit unit, BugPathfindingData data){
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
            gc.moveRobot(unit.id(), dir);
        }
        bugpathData.put(unit.id(), data);
        return;
    }
}
