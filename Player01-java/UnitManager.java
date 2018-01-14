import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import static java.lang.Math.floor;

public class UnitManager{

    private static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};
    final static double enemyBaseValue = -5;
    final static int exploreSize = 5;

    static UnitManager instance;
    static GameController gc;
    static PlanetMap map;
    static Team enemyTeam;

    //current area
    HashMap<Integer, int[]> currentArea = new HashMap();
    static int W;
    static int H;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    //Stuff available for all units
    //general
    static MapLocation middle;
    static long maxRadius;
    //danger
    static int[][] dangerMatrix;
    //mines in map
    static ArrayList<Integer> Xmines; //xpos
    static ArrayList<Integer> Ymines; //ypos
    static ArrayList<Integer> Qmines; //quantity
    //enemies list
    static ArrayList<Integer> Xenemy; //xpos
    static ArrayList<Integer> Yenemy; //ypos
    static ArrayList<Integer> Henemy; //health
    static ArrayList<Integer> IdEnemy; //id
    //enemy bases
    static double[][] exploreGrid;
    static int areaSizeX;
    static int areaSizeY;
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

    static void addExploreGrid(int x, int y, double value) {
        if(x == W) x = W - 1;
        if(y == H) y = H - 1;
        exploreGrid[(int)floor(x*exploreSize/W)][(int)floor(y*exploreSize/H)] += value;
    }

    static MapLocation areaToLocation(int[] area){
        int xArea = area[0];
        int yArea = area[1];
        int xLocation = (int)xArea*W/exploreSize + exploreSize/2;
        int yLocation = (int)yArea*H/exploreSize + exploreSize/2;
        return new MapLocation(gc.planet(), xLocation, yLocation);
    }

    UnitManager(){
        //general
        middle = new MapLocation(gc.planet(), W/2, H/2);
        maxRadius = middle.distanceSquaredTo(new MapLocation(gc.planet(), 0, 0));
        //mines
        Xmines = new ArrayList<Integer>();
        Ymines = new ArrayList<Integer>();
        Qmines = new ArrayList<Integer>();
        //enemy bases
        exploreGrid = new double[exploreSize][exploreSize];
        areaSizeX = W/exploreSize;
        areaSizeY = H/exploreSize;
        if(gc.team() == Team.Blue) enemyTeam = Team.Red;
        else enemyTeam = Team.Blue;
        map = gc.startingMap(gc.planet());
        W = (int)map.getWidth();
        H = (int)map.getHeight();
        //danger matrix TODO implementation
        dangerMatrix = new int[W][H];
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
            addExploreGrid(W - (int)myLoc.getX(), H - (int)myLoc.getY(), enemyBaseValue);
            addExploreGrid((int)myLoc.getX(), H - (int)myLoc.getY(), enemyBaseValue);
            addExploreGrid(W - (int)myLoc.getX(), (int)myLoc.getY(), enemyBaseValue);
        }
    }

    public void update() {
        //check enemy units
        checkEnemyUnits();
        //check mines
        checkMines();
        //update areas explored
        updateExplored();
    }


    void updateExplored(){
        VecUnit units = gc.myUnits();
        for(int i = 0; i < units.size(); ++i) {
            Unit unit = units.get(i);
            Location loc = unit.location();
            if(loc.isInGarrison()) continue;
            MapLocation myLoc = loc.mapLocation();
            int[] myArea = getCurrentArea(unit);
            int id = unit.id();
            if (!currentArea.containsKey(id)) {
                currentArea.put(id, myArea);
                addExploreGrid(myLoc.getX(), myLoc.getY(), 1);
                continue;
            }
            if (!currentArea.get(id).equals(myArea)) {
                currentArea.put(id, myArea);
                addExploreGrid(myLoc.getX(), myLoc.getY(), 1);
            }
        }
    }

    void checkMines(){
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

    void checkEnemyUnits(){
        //set enemy stuff to empty
        Xenemy = new ArrayList<Integer>();
        Yenemy = new ArrayList<Integer>();
        Henemy = new ArrayList<Integer>();
        IdEnemy = new ArrayList<Integer>();
        //get all enemies I sense
        VecUnit enemyUnits = gc.senseNearbyUnitsByTeam(middle, maxRadius, enemyTeam);
        //make list of enemies
        for(int i = 0; i < enemyUnits.size(); ++i){
            Unit enemy = enemyUnits.get(i);
            MapLocation enemyLocation = enemy.location().mapLocation();
            addEnemy(enemyLocation.getX(), enemyLocation.getY(), (int)enemy.health(), enemy.id());
        }
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

    static Direction dirTo(Unit unit, int destX, int destY) {
        MapLocation myLoc = unit.location().mapLocation();
        PathfinderNode myNode = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }

    static Direction dirTo(Unit unit, MapLocation loc) {
        return dirTo(unit, loc.getX(), loc.getY());
    }

    static void moveTo(Unit unit, MapLocation target) { //todo: edge cases
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


    int[] getCurrentArea(Unit unit){
        int x = unit.location().mapLocation().getX();
        if(x == W) x = W - 1;
        int y = unit.location().mapLocation().getY();
        if(y == H) y = H -1;
        return new int[]{(int)floor(x*exploreSize/W), (int)floor(y*exploreSize/H)};

    }
}