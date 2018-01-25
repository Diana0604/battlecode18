

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
        id = unit.getID();
        if (!bugpathData.keySet().contains(id)) {
            data = new BugPathfindingData();
        } else data = bugpathData.get(id);
        data.reset();
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
        BugPathfindingData data;
        if (!bugpathData.keySet().contains(unit.getID())) {
            data = new BugPathfindingData();
            bugpathData.put(unit.getID(), data);
        } else data = bugpathData.get(unit.getID());
        return data;
    }

    double raw_dist(int dir){
        return myLoc.add(dir).distanceBFSTo(unit.target);
    }


    public int move(AuxUnit unit){
        try {
            if (unit.visited) return 8;
            unit.visited = true;
            if (unit.target == null) return 8;
            if (!unit.canMove()) return 8;

            this.data = getData(unit);
            this.unit = unit;
            attackRange = Units.getAttackRange(unit.getType());
            attacker = dangerousUnit(unit.getType());

            myLoc = unit.getMapLocation();

            Danger.computeDanger(unit); //Todo aixo es pot borrar no?

            int dirGreedy = greedyMove(unit);
            if (dirGreedy != 8){
                doMovement(unit, dirGreedy);
                return dirGreedy;
            }

            int dirOP = unit.getMapLocation().dirBFSTo(unit.target);
            //System.out.println("Best dir: " + dirOP);

            ArrayList<Integer> directions = new ArrayList<>();
            for (int i = 0; i < 9; ++i){
                AuxMapLocation newLoc = myLoc.add(i);
                if (newLoc.isOnMap() && newLoc.isPassable() && isSafe(i)) directions.add(i);
            }

            directions.sort((a,b) -> raw_dist(a) < raw_dist(b) ? -1 : raw_dist(a) == raw_dist(b) ? 0 : 1);

            for (int i = 0; i < directions.size(); ++i){

                int dirBFS = directions.get(i);
                //System.out.println("Getting dir: " + dirBFS);

                if (dirBFS == 8) break;
                if (Wrapper.canMove(unit, dirBFS)){
                    doMovement(unit, dirBFS);
                    return dirBFS;
                }
                AuxMapLocation newLoc = unit.getMapLocation().add(dirBFS);
                AuxUnit u = newLoc.getUnit(true);
                if (u != null){
                    if (u.getType() == UnitType.Factory){
                        if (Wrapper.canLoad(u, unit)){
                            Wrapper.load(u, unit);
                            getData(unit).soft_reset(newLoc);
                            return dirBFS;
                        }
                    }
                    if (u.getType() != UnitType.Factory && u.getType() != UnitType.Rocket) {
                        if (u.getType() == unit.getType() && u.getType() == UnitType.Worker && u.canMove()){
                            if (u.target != null){
                                AuxMapLocation auxTarget = u.target;
                                u.target = unit.target;
                                unit.target = auxTarget;
                            }
                        }
                        if (move(u) != 8) {
                            doMovement(unit, dirBFS);
                            return dirBFS;
                        }
                    }
                }

                //if (unit.getType() == UnitType.Ranger || unit.getType() == UnitType.Mage) break;

            }

            myLoc = unit.getMapLocation();
            long d = myLoc.distanceSquaredTo(unit.target);
            if (d == 0) return 8;
            if (d <= 2) {
                int dir = myLoc.dirBFSTo(unit.target);
                //System.err.println(dir);
                //System.err.println(Pathfinder.getIndex(dir));
                if (Wrapper.canMove(unit, dir)){
                    doMovement(unit, dir);
                    return dir;
                }
                return 8;
            }

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
                return false;
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
        Wrapper.moveRobot(unit, dir);
        getData(unit).soft_reset(unit.getMapLocation());
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
        if (!Units.firstFactory || Units.unitTypeCount.get(UnitType.Worker) > 8) return true;
        return false;
    }

    int greedyMove(AuxUnit unit){
        if (unit.getType() == UnitType.Knight) return 8;
        if (unit.getType() == UnitType.Worker && kamikazeWorker()) return 8;
        try {
            int index = 8;
            for (int i = 0; i < 8; ++i) if (Wrapper.canMove(unit, i)) index = bestIndex(index, i);

            //System.err.println(index);

            if (index != 8) {
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



}
