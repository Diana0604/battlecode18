

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


    static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

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

        /*reset if new target*/
        if (data.target == null || (target != null && target.distanceSquaredTo(data.target) > 0)){
            if (data.target != null && target != null && target.distanceSquaredTo(data.target) <= 2){
                data.minDist = myLoc.distanceSquaredTo(target);
            }
            else data.soft_reset(myLoc);
        }

        //else if (data.target != null && target != null && target.distanceSquaredTo(data.target) > 0 && target.distanceSquaredTo(data.target) <= 8){
            //data.minDist = target.distanceSquaredTo(myLoc);
        //}
        data.target = target;

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
        canMove = new boolean[9];
        for (int i = 0; i < 9; ++i){
            if (gc.canMove(id, allDirs[i])) {
                canMove[i] = true;
            }
            else canMove[i] = false;
        }

        Danger.computeDanger(myLoc, canMove);

        greedyMove();


        if (!gc.isMoveReady(id)) return;

        //for (int i = 0; i < 9; ++i) if(Danger.DPS[i] > 0) canMove[i] = false;

        long d = myLoc.distanceSquaredTo(target);
        if (d == 0) return;
        if (d <= 2){
            Direction dir = myLoc.directionTo(target);
            System.err.println(dir);
            System.err.println(Pathfinder.getIndex(dir));
            if (canMove[Pathfinder.getIndex(dir)]) gc.moveRobot(id, dir);
            return;
        }

        naiveMoveTo(target);

        //safeMoveTo(unit, target);

        bugpathData.put(id, data);
    }

    int bestIndex(int i, int j){
        if (!Danger.attackers.contains(id)) {
            if (Danger.DPS[i] > Danger.DPS[j]) return j;
            if (Danger.DPS[i] < Danger.DPS[j]) return i;
            if (attacker) {
                if (Danger.minDist[i] > attackRange && Danger.minDist[j] <= attackRange) return j;
                if (Danger.minDist[i] <= attackRange && Danger.minDist[j] > attackRange) return i;
                if (Danger.minDist[i] <= attackRange) {
                    if (Danger.minDist[i] >= Danger.minDist[j]) return i;
                    return j;
                }
            }
            return i;
        }
        if (Danger.minDist[i] > attackRange && Danger.minDist[j] <= attackRange) return j;
        if (Danger.minDist[i] <= attackRange && Danger.minDist[j] > attackRange) return i;
        if (Danger.minDist[i] <= attackRange){
            if (Danger.DPS[i] > Danger.DPS[j]) return j;
            if (Danger.DPS[i] < Danger.DPS[j]) return i;
            if (Danger.minDist[i] >= Danger.minDist[j]) return i;
            return j;
        }
        return i;
    }

    void greedyMove(){
        if (!gc.isMoveReady(id)) return;
        int index = 8;
        for (int i = 0; i < 8; ++i) if (canMove[i]) index = bestIndex(index, i);

        //System.err.println(index);

        if (index != 8){
            gc.moveRobot(id, allDirs[index]);
            data.soft_reset(myLoc);
        }
    }

    boolean isSafe(Direction dir){
        int ind = Pathfinder.getIndex(dir);
        //if (Danger.attackers.contains(id)) return (Danger.DPSshort[ind] <= 0);
        return (Danger.DPS[ind] <= 0);
    }

    public boolean dangerousUnit(Unit unit){
        return (unit.unitType() == UnitType.Knight || unit.unitType() == UnitType.Mage || unit.unitType() == UnitType.Ranger);
    }



}
