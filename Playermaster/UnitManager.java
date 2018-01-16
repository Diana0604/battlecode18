import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import java.lang.Math.*;

class UnitManager{

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};
    private final double enemyBaseValue = -5;
    private final int areaSize = 5;
    final int exploreConstant = 1;
    private final int maxMapSize = 50;

    HashMap<Integer, Integer> currentArea;

    static UnitManager instance;
    static GameController gc;
    static ConstructionQueue queue;
    static PlanetMap map;
    static Team myTeam;
    static Team enemyTeam;

    //current area
    private int W;
    private int H;

    static void initialize(GameController _gc, ConstructionQueue q){
        gc = _gc;
        queue = q;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    //Stuff available for all units
    //general
    private MapLocation middle;
    private long maxRadius;
    //danger
    private int[][] dangerMatrix;
    //mines in map
    static ArrayList<Integer> Xmines; //xpos
    static ArrayList<Integer> Ymines; //ypos
    static ArrayList<Integer> Qmines; //quantity
    //enemies list
    VecUnit enemyUnits;
    //enemy bases
    private int[][] locToArea;
    private int[] areaToLocX;
    private int[] areaToLocY;
    double[][] exploreGrid;
    int exploreSizeX;
    int exploreSizeY;
    private int INF = 1000000000;

    void addMine(int x, int y, int q) {
        Xmines.add(x);
        Ymines.add(y);
        Qmines.add(q);
    }

    void addExploreGrid(Integer area, double value) {
        int x = decodeX(area);
        int y = decodeY(area);
        exploreGrid[x][y] += value;
    }

    MapLocation areaToLocation(Integer area){
        int x = areaToLocX[decodeX(area)];
        int y = areaToLocY[decodeY(area)];
        return new MapLocation(gc.planet(), x, y);
    }

    private Integer locationToArea(MapLocation loc){
        int x = loc.getX();
        int y = loc.getY();
        return locToArea[x][y];
    }

    Integer encode(int i, int j){
        return i*maxMapSize+j;
    }

    private int decodeX(Integer c){
        return c/maxMapSize;
    }

    private int decodeY(Integer c){
        return c%maxMapSize;
    }

    private MapLocation getAccesLocation(int xCenter, int yCenter){
        MapLocation realCenter = new MapLocation(gc.planet(), xCenter, yCenter);
        //TODO check apart from passable accessible from origin in earth
        if(map.isPassableTerrainAt(realCenter) > 0) return realCenter;
        for(int i = 0; i < allDirs.length; ++i){
            MapLocation fakeCenter = realCenter.add(allDirs[i]);
            if(map.isPassableTerrainAt(fakeCenter) > 0) return fakeCenter;
        }
        return null;
    }

    private void createGrid(){
        currentArea = new HashMap();
        exploreSizeX = W/areaSize;
        exploreSizeY = H/areaSize;
        double auxiliarX = (double)W/exploreSizeX;
        double auxiliarY = (double)H/exploreSizeY;
        exploreGrid = new double[exploreSizeX][exploreSizeY];
        locToArea = new int[W][H];
        areaToLocX = new int[exploreSizeX];
        areaToLocY = new int[exploreSizeY];
        for(int i = 0; i < exploreSizeX; ++i){
            for(int j  = 0; j < exploreSizeY; ++j){
                for(int x = (int)Math.floor(i*auxiliarX); x < Math.floor((i+1)*auxiliarX); ++x){
                    for(int y = (int)Math.floor(j*auxiliarY); y < Math.floor((j+1)*auxiliarY); ++y){
                        locToArea[x][y] = encode(i,j);
                    }
                }
                int xCenter = (int)Math.floor(i*auxiliarX) + areaSize/2;
                int yCenter = (int)Math.floor(j*auxiliarY) + areaSize/2;
                MapLocation centerArea = getAccesLocation(xCenter, yCenter);
                if(centerArea != null) {
                    areaToLocX[i] = centerArea.getX();
                    areaToLocY[j] = centerArea.getY();
                    continue;
                }
                exploreGrid[i][j] = INF;
            }
        }
    }

    UnitManager(){
        //general
        map = gc.startingMap(gc.planet());
        W = (int)map.getWidth();
        H = (int)map.getHeight();
        middle = new MapLocation(gc.planet(), W/2, H/2);
        maxRadius = middle.distanceSquaredTo(new MapLocation(gc.planet(), 0, 0));
        //mines
        Xmines = new ArrayList<Integer>();
        Ymines = new ArrayList<Integer>();
        Qmines = new ArrayList<Integer>();
        //explore grid
        createGrid();
        //other
        myTeam = gc.team();
        if(myTeam == Team.Blue) enemyTeam = Team.Red;
        else enemyTeam = Team.Blue;
        //danger matrix TODO implementation
        dangerMatrix = new int[W][H];
        //get location of enemy base
        getLocationEnemyBase();
    }

    private void getLocationEnemyBase(){
        VecUnit initialUnits = map.getInitial_units();
        for(int i = 0; i < initialUnits.size(); ++i){
            Unit possibleEnemy = initialUnits.get(i);
            if(possibleEnemy.team() == enemyTeam){
                Integer enemyArea = locationToArea(possibleEnemy.location().mapLocation());
                addExploreGrid(enemyArea, enemyBaseValue);
            }
        }
    }

    void update() {
        //check enemy units
        enemyUnits = gc.senseNearbyUnitsByTeam(middle, maxRadius, enemyTeam);
        //check mines
        //checkMines(); //de moment ho trec (Pau) perque no vull que s'esborrin posicions de la llista
        //update areas explored
        updateCurrentArea();
        //comprova si ha de construir factory o rocket
        checkMyUnits();
    }


    private void updateCurrentArea(){
        VecUnit units = gc.myUnits();
        for(int i = 0; i < units.size(); ++i){
            Unit unit = units.get(i);
            if(unit.location().isInGarrison()) continue;
            int id = unit.id();
            Integer current = locationToArea(unit.location().mapLocation());
            if(currentArea.containsKey(id) && currentArea.get(id) == current) continue;
            currentArea.put(id,current);
            addExploreGrid(current,exploreConstant);
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

    private void checkMyUnits(){
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
        //if (!rocketBuilt) queue.requestUnit(UnitType.Rocket);
    }


    void moveUnits(){
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

    void moveTo(Unit unit, MapLocation target){
        MovementManager.getInstance().moveTo(unit, target);
    }
}
