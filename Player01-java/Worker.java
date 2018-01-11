import bc.*;

public class Worker {

    static Worker instance = null;
    static boolean factoryBuilt = false;
    static GameController gc;

    static Worker getInstance(){
        if (instance == null){
            instance = new Worker();
            gc = UnitManager.currentUnitManager.gc;
        }
        return instance;
    }

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};


    void play(){
        move();
    }

    void move(){
        if (!factoryBuilt) buildFactory();
        goToBestMine();
    }

    void buildFactory(){
        for (int i = 0; i < allDirs.length; ++i){
            if (gc.canBlueprint(UnitManager.currentUnit.id(), UnitType.Factory, allDirs[i])){
                gc.blueprint(UnitManager.currentUnit.id(), UnitType.Factory, allDirs[i]);
                factoryBuilt = true;
                return;
            }
        }
    }

    void goToBestMine(){
        MapLocation myLoc = UnitManager.currentUnit.location().mapLocation();
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
            if (UnitManager.currentUnit.workerHasActed() == 0 && gc.canHarvest(UnitManager.currentUnit.id(), allDirs[dirIndex])) {
                gc.harvest(UnitManager.currentUnit.id(), allDirs[dirIndex]);
            }
            return;
        }

        MapLocation target = getBestMine(myLoc);
        if (target == null) {
            return; // what to do? xD
        }
        UnitManager.getInstance().moveTo(target);
    }

    MapLocation getBestMine(MapLocation loc){
        long minDist = 1000000;
        MapLocation ans = null;
        for (int i = 0; i < UnitManager.currentUnitManager.Xmines.size(); ++i){
            int x = UnitManager.currentUnitManager.Xmines.get(i);
            int y = UnitManager.currentUnitManager.Ymines.get(i);
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