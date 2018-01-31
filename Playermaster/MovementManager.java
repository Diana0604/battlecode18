

import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Ivan on 1/14/2018.
 * holi
 */
public class MovementManager {

    static MovementManager instance;

    private BugPathfindingData data;
    AuxUnit unit;

    private AuxMapLocation myLoc;
    private int id;
    private boolean[] canMove;
    private long attackRange;
    private boolean attacker;

    static MovementManager getInstance(){
        if (instance == null) instance = new MovementManager();
        return instance;
    }

    HashMap<Integer, BugPathfindingData> bugpathData;

    public MovementManager(){
        bugpathData = new HashMap<>();
    }

    public void reset(AuxUnit unit){
        try {
            id = unit.getID();
            if (!bugpathData.keySet().contains(id)) {
                data = new BugPathfindingData();
            } else data = bugpathData.get(id);
            data.reset();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public int moveBFSTo(AuxMapLocation target) {
        try {
            int index = myLoc.dirBFSTo(target);
            //Direction dir = dirTo(unit, target);
            if (canMove[index]) {
                if (isSafe(index)) {
                    doMovement(unit, index);
                    return index;
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
                    doMovement(unit, index);
                    return index;
                }
            }
            return 8;
        }catch(Exception e) {
            e.printStackTrace();
            return 8;
        }
    }

    public int naiveMoveTo(AuxMapLocation target){
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
                int ans = moveBFSTo(target);
                if (ans != 8) return ans;
            }


            int ans = BugPath(data);

            if (ans != 8) {
                //bugpathData.put(id, data);
                return ans;
            }

            //bugpathData.put(id, data);
            return 8;
        }catch(Exception e) {
            e.printStackTrace();
            return 8;
        }
    }

    public int BugPath(BugPathfindingData data){
        try {
            int dir;
            if (data.obstacle != null && !data.obstacle.isOnMap()) {
                data.soft_reset(myLoc);
                //System.err.println("User error: out of map obstacle bugpath");
            }
            if (data.obstacle == null) dir = myLoc.dirBFSTo(data.target);
            else dir = myLoc.dirBFSTo(data.obstacle);
            if (canMove[dir]) {
                //if (dir == 8) System.err.println("User error: self-obstacle bugpath");
                data.obstacle = null;
            } else {
                int cont = 0;
                while (!canMove[dir] && cont < 20) {
                    AuxMapLocation newLoc = myLoc.add(dir);
                    if (!newLoc.isOnMap()) {
                        data.left = !data.left;
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
                    return dir;
                }
            }
            return 8;
        }catch(Exception e) {
            e.printStackTrace();
            return 8;
        }
    }

    BugPathfindingData getData(AuxUnit unit){
        try {
            BugPathfindingData data;
            if (!bugpathData.keySet().contains(unit.getID())) {
                data = new BugPathfindingData();
                bugpathData.put(unit.getID(), data);
            } else data = bugpathData.get(unit.getID());
            return data;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    double raw_dist(int dir, int priority){
        try {
            double a = 0;
            if (dir == 8){
                if (priority == PLSMOVE) a += 0.001;
                else a -= 0.001;
            }
            return a + myLoc.add(dir).distanceBFSTo(unit.target);
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static int NOTFORCED = 0;
    static int PLSMOVE = 1;
    static int FORCED = 2;


    public int move(AuxUnit unit, int priority){
        try {

            if (unit.target == null) unit.target = unit.getMapLocation();
            if (unit == null) System.out.println("WTF is happening!");
            else if (priority == FORCED) System.out.println("Trying to move " + unit.getID() + " from " + unit.getX() + " " + unit.getY() + " to " + unit.target.x + " " + unit.target.y);

            if (unit.visited){
                return 8;
            }
            unit.visited = true;
            if (unit.target == null) unit.target = unit.getMapLocation();
            if (!unit.canMove()){
                if (priority == FORCED){
                    Wrapper.disintegrate(unit);
                    return 0;
                }
                return 8;
            }

            this.data = getData(unit);
            this.unit = unit;
            attackRange = Units.getAttackRange(unit.getType());
            attacker = dangerousUnit(unit.getType());

            myLoc = unit.getMapLocation();

            Danger.computeDanger(unit);

            int dirGreedy = greedyMove(unit, priority);
            if (dirGreedy != 8){
                doMovement(unit, dirGreedy);
                return dirGreedy;
            }

            long d = myLoc.distanceSquaredTo(unit.target);
            if (priority != FORCED && d == 0) return 8;


            ArrayList<Integer> directions = new ArrayList<>();
            for (int i = 0; i < 8; ++i){
                AuxMapLocation newLoc = myLoc.add(i);
                if (newLoc.isPushable() && (priority == FORCED || isSafe(i))) directions.add(i);
            }
            if (priority != FORCED) directions.add(8);

            directions.sort((a,b) -> raw_dist(a, priority) < raw_dist(b, priority) ? -1 : raw_dist(a, priority) == raw_dist(b, priority) ? 0 : 1);

            for (int i = 0; i < directions.size(); ++i) {

                int dirBFS = directions.get(i);

                if (dirBFS == 8) break;
                if (Wrapper.canMove(unit, dirBFS)) {
                    doMovement(unit, dirBFS);
                    return dirBFS;
                }
                AuxMapLocation newLoc = unit.getMapLocation().add(dirBFS);
                AuxUnit u = newLoc.getUnit(true);
                if (u != null) {
                    if (u.immune) continue;
                    if (u.getType() == UnitType.Factory && d > 2) {
                        if (Wrapper.canLoad(u, unit)) {
                            Wrapper.load(u, unit);
                            getData(unit).soft_reset(newLoc);
                            return dirBFS;
                        }
                    }
                    if (u.getType() != UnitType.Factory && u.getType() != UnitType.Rocket && u.canMove()) {
                        if (move(u, priority) != 8) {
                            doMovement(unit, dirBFS);
                            return dirBFS;
                        }
                    }
                }
            }

            if (priority == FORCED){
                Wrapper.disintegrate(unit);
                return 0;
            }

            if (d <= 2) return 8;

            myLoc = unit.getMapLocation();

            this.unit = unit;
            id = unit.getID();
            if (!bugpathData.keySet().contains(id)) {
                data = new BugPathfindingData();
            } else data = bugpathData.get(id);
            canMove = new boolean[9];
            for (int i = 0; i < 9; ++i) {
                if (Wrapper.canMove(unit, i)) {
                    canMove[i] = true;
                } else canMove[i] = false;
            }

            Danger.computeDanger(unit); //Todo aixo es pot borrar no?

            int ans =  naiveMoveTo(unit.target);

            return ans;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 8;
    }

    boolean shouldAggro(){
        try {
            if (!dangerousUnit(unit.getType())) return false;
            if (!unit.canAttack()) return false;
            if (unit.getType() == UnitType.Mage) {
                //if (!GC.canBlink) return false;
                //if (!unit.canUseAbility()) return false;
                return true;
            }
            if (Danger.attackers.contains(id)) return true;
            if (Mapa.onEarth() && Units.canBlink) return true;
            return false;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    void doMovement(AuxUnit unit, int dir){
        try {
            Wrapper.moveRobot(unit, dir);
            getData(unit).soft_reset(unit.getMapLocation());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    double danger(int i){
        try {
            if (unit.getType() == UnitType.Knight) return 0;
            if (unit.getType() == UnitType.Worker && kamikazeWorker()) return 0;
            if (unit.getType() == UnitType.Ranger) return Danger.DPS[i];
            if (unit.getType() == UnitType.Mage && Units.canBlink) return Danger.DPS[i];
            return Danger.DPSlong[i] + Danger.DPS[i];
        } catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    int bestIndex(int i, int j){
        try {
            if (i == -1) return j;
            if (shouldAggro()) {
                if (Danger.minDist[i] > attackRange && Danger.minDist[j] <= attackRange) return j;
                if (Danger.minDist[i] <= attackRange && Danger.minDist[j] > attackRange) return i;
                if (Danger.minDist[i] <= attackRange) {
                    if (danger(i) > danger(j)) return j;
                    if (danger(i) < danger(j)) return i;
                    if (Danger.minDist[i] >= Danger.minDist[j]) return i;
                    return j;
                } else {
                    if (i != 8) {
                        if (Danger.minDist[i] <= Danger.minDist[j]) return i;
                        return j;
                    }
                }
                return i;
            }
            //if (true){
            if (danger(i) >  danger(j)) return j;
            if (danger(i) <  danger(j)) return i;
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
        }catch(Exception e) {
            e.printStackTrace();
            return i;
        }
    }

    boolean kamikazeWorker(){
        try {
            if (!Build.firstFactory || Units.unitTypeCount.get(UnitType.Worker) > 8) return true;
            return false;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    int greedyMove(AuxUnit unit, int priority){
        try {
            if (unit.getType() == UnitType.Knight) return 8;
            if (unit.getType() == UnitType.Worker && kamikazeWorker()) return 8;
            int index = 8;
            if (priority == FORCED) index = -1;
            for (int i = 0; i < 8; ++i) if (Wrapper.canMove(unit, i)) index = bestIndex(index, i);

            if (index != -1) {
                return index;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 8;
    }

    boolean isSafe(int ind){
        try {
            return (danger(ind) <= 0);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean dangerousUnit(UnitType type) {
        try {
            return (type == UnitType.Knight || type == UnitType.Mage || type == UnitType.Ranger);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setData(AuxUnit unit){
        try {
            Danger.computeDanger(unit);
            this.unit = unit;
            attacker = dangerousUnit(unit.getType());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }



}
