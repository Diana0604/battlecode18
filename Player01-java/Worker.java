import bc.*;

public class Worker {

    static Worker instance = null;
    static boolean factoryBuilt = false;
    boolean wait;
    static GameController gc;

    static Worker getInstance(){
        if (instance == null){
            instance = new Worker();
            gc = UnitManager.getInstance().gc;
        }
        return instance;
    }

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};


    void play(Unit unit){
        wait = false;
        if(!factoryBuilt) blueprintFactory(unit);
        if(wait) return;
        buildFactory(unit);
        if(wait) return;
        move(unit);
    }

    void move(Unit unit){
        goToBestMine(unit);
    }

    void buildFactory(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        for(int i = 0; i < allDirs.length; ++i){
            MapLocation newLoc = myLoc.add(allDirs[i]);
            //canviar-ho a gc.hasUnitAtLocation(newLoc) quan estigui arreglat TODO
            try {
                Unit possibleFactory = gc.senseUnitAtLocation(newLoc);
                if(possibleFactory != null && possibleFactory.unitType() == UnitType.Factory && possibleFactory.health() != possibleFactory.maxHealth() && possibleFactory.team().equals(gc.team())) {
                    if (gc.canBuild(unit.id(), possibleFactory.id()))
                        gc.build(unit.id(), possibleFactory.id());
                    wait = true;
                }
            } catch(Throwable t){
                continue;
            }
        }
    }

    void blueprintFactory(Unit unit){
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
            if (gc.canHarvest(unit.id(), allDirs[dirIndex])) {
                gc.harvest(unit.id(), allDirs[dirIndex]);
            }
            return;
        }
        //if I can't move return
        if(!gc.isMoveReady(unit.id())) return;
        MapLocation target = getBestMine(myLoc);
        if (target == null) {
            return; // what to do? xD
        }
        UnitManager.getInstance().moveTo(unit, target);
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