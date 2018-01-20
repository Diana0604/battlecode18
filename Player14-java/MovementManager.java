

import bc.UnitType;

import java.util.HashMap;

/**
 * Created by Ivan on 1/14/2018.
 * holi
 */
public class MovementManager {

    static MovementManager instance;
    public final int INF = 1000000000;

    private BugPathfindingData data;
    AuxUnit unit;

    private AuxMapLocation myLoc;
    private int id;
    private boolean[] canMove;
    private long attackRange;
    private boolean attacker;


    //static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static MovementManager getInstance(){
        if (instance == null) instance = new MovementManager();
        return instance;
    }

    HashMap<Integer, BugPathfindingData> bugpathData;

    public MovementManager(){
        bugpathData = new HashMap<>();
    }

    public boolean moveBFSTo(AuxMapLocation target) {
        try {
            int index = myLoc.dirBFSTo(target);
            //Direction dir = dirTo(unit, target);
            if (canMove[index]) {
                if (isSafe(index)) {
                    Wrapper.moveRobot(unit, index);
                    return true;
                }
            }
            index = -1;
            double mindist = myLoc.distanceBFSTo(target);
            for (int i = 0; i < 9; ++i) {
                if (!canMove[i]) continue;
                AuxMapLocation newLoc = myLoc.add(i);
                double d = myLoc.distanceBFSTo(target);
                if (d < mindist) {
                    mindist = d;
                    index = i;
                }
            }
            if (index >= 0) {
                if (isSafe(index)) {
                    Wrapper.moveRobot(unit, index);
                    return true;
                }
            }
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    public boolean naiveMoveTo(AuxMapLocation target){
        try {
            /*reset if new target*/
            if (data.target == null || (target != null && target.distanceSquaredTo(data.target) > 0)) {
                if (data.target != null && target != null && target.distanceSquaredTo(data.target) <= 2) {
                    data.minDist = myLoc.distanceBFSTo(target);
                } else data.soft_reset(myLoc);
            }

            //else if (data.target != null && target != null && target.distanceSquaredTo(data.target) > 0 && target.distanceSquaredTo(data.target) <= 8){
            //data.minDist = target.distanceSquaredTo(myLoc);
            //}
            data.target = target;

            if (target != null && myLoc.distanceBFSTo(target) < data.minDist) {
                data.soft_reset(myLoc);
            }

            /*if not an obstacle --->  BFS*/
            if (data.obstacle == null) {
                if (moveBFSTo(target)) {
                    //bugpathData.put(id, data);
                    return true;
                }
            }

            if (BugPath(data)) {
                //bugpathData.put(id, data);
                return true;
            }

            //bugpathData.put(id, data);
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    public boolean BugPath(BugPathfindingData data){
        try {
            int dir;
            if (data.obstacle != null && !data.obstacle.isOnMap()) {
                data.soft_reset(myLoc);
                System.err.println("User error: out of map obstacle bugpath");
            }
            if (data.obstacle == null) dir = myLoc.dirBFSTo(data.target);
            else dir = myLoc.dirBFSTo(data.obstacle);
            if (canMove[dir]) {
                if (dir == 8) System.err.println("User error: self-obstacle bugpath");
                data.obstacle = null;
            } else {
                int cont = 0;
                while (!canMove[dir] && cont < 20) {
                    AuxMapLocation newLoc = myLoc.add(dir);
                    if (!newLoc.isOnMap()) {
                        data.left = !data.left;
                        --cont;
                    }
                    data.obstacle = newLoc;
                    if (data.left) dir = (dir + 1) % 8;
                    else dir = (dir + 7) % 8;
                    ++cont;
                }
            }
            if (canMove[dir]) {
                if (isSafe(dir)) {
                    Wrapper.moveRobot(unit, dir);
                    return true;
                }
            }
            return false;
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }



    public void moveTo(AuxUnit _unit, AuxMapLocation target){
        try {
            if (target == null) return;
            if (!_unit.canMove()) return;
            unit = _unit;


            id = unit.getID();
            myLoc = unit.getMaplocation();
            if (!bugpathData.keySet().contains(id)) {
                data = new BugPathfindingData();
            } else data = bugpathData.get(id);
            attackRange = Wrapper.getAttackRange(unit.getType());
            attacker = dangerousUnit(unit.getType());
            canMove = new boolean[9];
            for (int i = 0; i < 9; ++i) {
                if (Wrapper.canMove(unit, i)) {
                    canMove[i] = true;
                } else canMove[i] = false;
            }

            Danger.computeDanger(unit);

            greedyMove();


            if (!unit.canMove()) return;

            //for (int i = 0; i < 9; ++i) if(Danger.DPS[i] > 0) canMove[i] = false;

            long d = myLoc.distanceSquaredTo(target);
            if (d == 0) return;
            if (d <= 2) {
                int dir = myLoc.dirBFSTo(target);
                //System.err.println(dir);
                //System.err.println(Pathfinder.getIndex(dir));
                if (canMove[dir]) Wrapper.moveRobot(unit, dir);
                return;
            }

            naiveMoveTo(target);

            //safeMoveTo(unit, target);

            bugpathData.put(id, data);
        }catch(Exception e) {
            System.out.println(e);
        }
    }

    int bestIndex(int i, int j){
        try {
            if (!Danger.attackers.contains(id)) {
                //if (true){
                if (Danger.DPS[i] > Danger.DPS[j]) return j;
                if (Danger.DPS[i] < Danger.DPS[j]) return i;
                if (attacker) {
                    if (Danger.minDist[i] > attackRange && Danger.minDist[j] <= attackRange) return j;
                    if (Danger.minDist[i] <= attackRange && Danger.minDist[j] > attackRange) return i;
                    if (Danger.minDist[i] <= attackRange) {
                        if (Danger.minDist[i] >= Danger.minDist[j]) return i;
                        return j;
                    } else {
                        if (i != 8) {
                            if (Danger.minDist[i] <= Danger.minDist[j]) return i;
                            return j;
                        }
                    }
                }
                return i;
            }
            if (Danger.minDist[i] > attackRange && Danger.minDist[j] <= attackRange) return j;
            if (Danger.minDist[i] <= attackRange && Danger.minDist[j] > attackRange) return i;
            if (Danger.minDist[i] <= attackRange) {
                if (Danger.DPS[i] > Danger.DPS[j]) return j;
                if (Danger.DPS[i] < Danger.DPS[j]) return i;
                if (Danger.minDist[i] >= Danger.minDist[j]) return i;
                return j;
            } else {
                if (i != 8) {
                    if (Danger.minDist[i] <= Danger.minDist[j]) return i;
                    return j;
                }
            }
            return i;
        }catch(Exception e) {
            System.out.println(e);
            return Integer.parseInt(null);
        }
    }

    void greedyMove(){
        try {
            if (!unit.canMove()) return;
            int index = 8;
            for (int i = 0; i < 8; ++i) if (canMove[i]) index = bestIndex(index, i);

            //System.err.println(index);

            if (index != 8) {
                Wrapper.moveRobot(unit, index);
                data.soft_reset(myLoc);
            }
        }catch(Exception e) {
            System.out.println(e);
        }
    }

    boolean isSafe(int ind){
        try {
            return (Danger.DPS[ind] <= 0);
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    public boolean dangerousUnit(UnitType type) {
        try {
            return (type == UnitType.Knight || type == UnitType.Mage || type == UnitType.Ranger);
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }



}
