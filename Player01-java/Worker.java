import bc.*;

public class Worker {

    static Worker instance = null;
    static boolean factoryBuilt = false;
    boolean wait;
    static GameController gc;

    static Worker getInstance(){
        if (instance == null){
            instance = new Worker();
            gc = UnitManager.gc;
        }
        return instance;
    }

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};


    void play(Unit unit){
        wait = false;
        if(!factoryBuilt) blueprintFactory(unit);
        if(wait || unit.workerHasActed() != 0) return;
        buildFactory(unit);
        if(wait || unit.workerHasActed() != 0) return;
        move(unit);
    }

    void move(Unit unit){
        goToBestMine(unit);
    }

    void buildFactory(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        for(int i = 0; i < allDirs.length; ++i){
            MapLocation newLoc = myLoc.add(allDirs[i]);
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
            if (unit.workerHasActed() == 0 && gc.canHarvest(unit.id(), allDirs[dirIndex])) {
                gc.harvest(unit.id(), allDirs[dirIndex]);
            }
            return;
        }

        MapLocation target = getBestMine(myLoc);
        //System.out.println(target);
        if (target == null) {
            return; // what to do? xD
        }
        moveTo(unit, target);
        //Direction dir = dirTo(unit, target);
        //System.out.println(dir);
        //if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), dir))  gc.moveRobot(unit.id(), dir);
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
        //System.out.println("buscant mina");
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
        //System.out.println(ans.getX());
        //System.out.println(ans.getY());
        return ans;
    }

    void moveTo(Unit unit, MapLocation target){ //todo: edge cases
        if (!gc.isMoveReady(unit.id())) return;
        Direction dir = dirTo(unit, target);
        if (gc.canMove(unit.id(), dir)) {
            gc.moveRobot(unit.id(), dir);
            return;
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
        if (dir != null) gc.moveRobot(unit.id(), dir);
    }

}