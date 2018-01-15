import bc.*;
import java.util.ArrayList;
import java.util.ListIterator;

public class UnitManager{

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static UnitManager instance;
    static GameController gc;
    static ConstructionQueue queue;
    static PlanetMap map;
    static Team enemyTeam;

    int W;
    int H;

    static void initialize(GameController _gc, ConstructionQueue q){
        gc = _gc;
        queue = q;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    //Stuff available for all units
    //danger
    static int[][] dangerMatrix;
    static MapLocation middle;
    static long maxRadius;
    //mines in map
    static ArrayList<Integer> Xmines; //xpos
    static ArrayList<Integer> Ymines; //ypos
    static ArrayList<Integer> Qmines; //quantity
    //enemy factories
    static ArrayList<Integer> Xenemy; //xpos
    static ArrayList<Integer> Yenemy; //ypos
    static ArrayList<Integer> Henemy; //health
    static ArrayList<Integer> IdEnemy; //id
    int INF = 1000000000;

    static void addMine(int x, int y, int q) {
        Xmines.add(x);
        Ymines.add(y);
        Qmines.add(q);
    }

    static void addEnemy(int x, int y, int h, int id) {
        Xenemy.add(x);
        Yenemy.add(y);
        Henemy.add(h);
        IdEnemy.add(id);
    }

    UnitManager(){
        Xmines = new ArrayList<Integer>();
        Ymines = new ArrayList<Integer>();
        Qmines = new ArrayList<Integer>();
        Xenemy = new ArrayList<Integer>();
        Yenemy = new ArrayList<Integer>();
        Henemy = new ArrayList<Integer>();
        IdEnemy = new ArrayList<Integer>();
        if(gc.team() == Team.Blue) enemyTeam = Team.Red;
        else enemyTeam = Team.Blue;
        map = gc.startingMap(gc.planet());
        W = (int)map.getWidth();
        H = (int)map.getHeight();
        //danger matrix
        dangerMatrix = new int[W][H];
        middle = new MapLocation(gc.planet(), W/2, H/2);
        maxRadius = middle.distanceSquaredTo(new MapLocation(gc.planet(), 0, 0));
        //pathfinder
        Pathfinder.getInstance();
        //get location of enemy base
        getLocationEnemyBase();
    }

    public void getLocationEnemyBase(){
        Pathfinder pathfinder = Pathfinder.getInstance();
        VecUnit units = gc.myUnits();
        for(int i = 0; i < units.size(); ++i) {
            Unit unit = units.get(i);
            MapLocation myLoc = unit.location().mapLocation();
            int id = -1;
            //TODO. Untested:
            /*
            MapLocation enemyLoc = new MapLocation(gc.planet(), W - (int)myLoc.getX(), H - (int)myLoc.getY());
            if(gc.canSenseLocation(enemyLoc)){
                Unit enemy = gc.senseUnitAtLocation(enemyLoc);
                id = enemy.id();
            }*/
            addEnemy(W - (int)myLoc.getX(), H - (int)myLoc.getY(), (int)unit.health(), id);
        }
    }

    public void update() {
        checkEnemyUnits();
        checkMines();
        checkMyUnits();

    }

    void checkEnemyUnits(){
        //add new enemies
        VecUnit enemyVecUnits = gc.senseNearbyUnitsByTeam(middle, maxRadius, enemyTeam);
        ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
        for(int i = 0; i < enemyVecUnits.size(); ++i){
            enemyUnits.add(enemyVecUnits.get(i));
        }
        //update existent enemies
        for(int i = Xenemy.size() - 1; i >= 0; --i){
            int x = Xenemy.get(i);
            int y = Yenemy.get(i);
            if (gc.canSenseLocation(new MapLocation(gc.planet(), x, y))){
                MapLocation ml = new MapLocation(gc.planet(), x, y);
                //canviar-ho a gc.hasUnitAtLocation(ml) quan estigui arreglat TODO
                try {
                    Unit unit = gc.senseUnitAtLocation(ml);
                    //remove it from enemy units list
                    enemyUnits.remove(unit);
                    int id = unit.id();
                    long h = unit.health();
                    if(id == IdEnemy.get(i) || IdEnemy.get(i) == -1) {
                        if (h != Henemy.get(i)) Henemy.set(i, (int) h);
                        continue;
                    }
                    else {
                        Xenemy.remove(i);
                        Yenemy.remove(i);
                        Henemy.remove(i);
                        IdEnemy.remove(i);
                        addEnemy(x, y, (int)h, id);
                    }
                } catch (Throwable t){
                    Xenemy.remove(i);
                    Yenemy.remove(i);
                    Henemy.remove(i);
                    IdEnemy.remove(i);
                }
            }
        }
        //add new enemies
        for(int i = 0; i < enemyUnits.size(); ++i){
            Unit enemy = enemyUnits.get(i);
            MapLocation enemyLocation = enemy.location().mapLocation();
            addEnemy(enemyLocation.getX(), enemyLocation.getY(), (int)enemy.health(), enemy.id());
        }
        return;
    }

    public void checkMines(){
        for (int i = Xmines.size() - 1; i >= 0; --i) {
            int x = Xmines.get(i);
            int y = Ymines.get(i);
            if (gc.canSenseLocation(new MapLocation(gc.planet(), x, y))) {
                long q = gc.karboniteAt(new MapLocation(gc.planet(), x, y));
                if (q > INF) q = INF;
                if (q > 0) {
                    if (q != Qmines.get(i)) Qmines.set(i, (int) q);
                } else {
                    Xmines.remove(i);
                    Ymines.remove(i);
                    Qmines.remove(i);
                }
            }
        }
    }

    public void checkMyUnits(){
        VecUnit v = gc.myUnits();
        boolean factoryBuilt = false;
        boolean rocketBuilt = false;
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);
            if (u.unitType() == UnitType.Factory){
                factoryBuilt = true;
            }else if (u.unitType() == UnitType.Rocket){
                rocketBuilt = true;
            }
        }
        if (!factoryBuilt) queue.requestUnit(UnitType.Factory);
        if (!rocketBuilt) queue.requestUnit(UnitType.Rocket);

    }

    public void moveUnits(){
        VecUnit units = gc.myUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            Location myLoc = unit.location();
            if(myLoc.isInGarrison()) continue;
            if (unit.unitType() == UnitType.Worker) {
                Worker.getInstance().play(unit);
            }
            if(unit.unitType() == UnitType.Factory) {
                Factory.getInstance().play(unit);
            }
            if(unit.unitType() == UnitType.Ranger) {
                Ranger.getInstance().play(unit);
            }
        }
    }

    Direction dirTo(Unit unit, int destX, int destY) {
        MapLocation myLoc = unit.location().mapLocation();
        PathfinderNode myNode = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }

    Direction dirTo(Unit unit, MapLocation loc) {
        return dirTo(unit, loc.getX(), loc.getY());
    }

    void moveTo(Unit unit, MapLocation target) { //todo: edge cases
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