import bc.*;

public class Worker {

    static Worker instance = null;
    static boolean factoryBuilt = false;
    static GameController gc;

    static Worker getInstance(){
        if (instance == null){
            instance = new Worker();
            gc = UnitManager.gc;
        }
        return instance;
    }

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest};


    void play(Unit unit){
        move(unit);
    }

    void move(Unit unit){
        if (!factoryBuilt) buildFactory(unit);
        goToBestMine(unit);
    }

    void buildFactory(Unit unit){
        for (int i = 0; i < allDirs.length; ++i){
            if (gc.canBlueprint(unit.id(), UnitType.Factory, allDirs[i])){
                gc.blueprint(unit.id(), UnitType.Factory, allDirs[i]);
                factoryBuilt = true;
                return;
            }
        }
    }

    void goToBestMine(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        long maxKarbo = 0;
        int dirIndex = -1;
        for (int i = 0; i < allDirs.length; ++i){
            MapLocation newLoc = myLoc.add(allDirs[i]);
            if (!UnitManager.map.onMap(newLoc)) continue;
            long k = gc.karboniteAt(newLoc);
            if (k > maxKarbo){
                maxKarbo = k;
                dirIndex = i;
            }
        }
        if (dirIndex >= 0){
            if (unit.workerHasActed() == 0 && gc.canHarvest(unit.id(), allDirs[dirIndex])) gc.harvest(unit.id(), allDirs[dirIndex]);
            return;
        }

        MapLocation target = getBestMine(myLoc);
        if (target == null) return; // what to do? xD
        Direction dir = dirTo(unit, myLoc);
        if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), dir))  gc.moveRobot(unit.id(), dir);
    }

    Direction dirTo(Unit unit, int destX, int destY){
        MapLocation myLoc = unit.location().mapLocation();
        PathfinderNode myNode = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }

    Direction dirTo(Unit unit, MapLocation loc){
        return dirTo(unit, loc.getX(), loc.getY());
    }

    MapLocation getBestMine(MapLocation loc){
        long minDist = 1000000;
        MapLocation ans = null;
        for (int i = 0; i < UnitManager.Xmines.size(); ++i){
            int x = UnitManager.Xmines.get(i);
            int y = UnitManager.Ymines.get(i);
            MapLocation mineLoc = new MapLocation(gc.planet(), x, y);
            long d = loc.distanceSquaredTo(mineLoc);
            if (d < minDist){
                minDist = d;
                ans = mineLoc;
            }
        }
        return ans;
    }

}