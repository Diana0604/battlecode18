import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import java.lang.Math.*;

public class UnitManager{

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};
    final double enemyBaseValue = -5;
    final int areaSize = 5;
    final int exploreConstant = 1;
    final int maxMapSize = 50;

    HashMap<Integer, Integer> currentArea;

    static UnitManager instance;
    static GameController gc;
    PlanetMap map;
    Team enemyTeam;

    //current area
    int W;
    int H;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    //Stuff available for all units
    //general
    MapLocation middle;
    long maxRadius;
    //danger
    int[][] dangerMatrix;
    //mines in map
    ArrayList<Integer> Xmines; //xpos
    ArrayList<Integer> Ymines; //ypos
    ArrayList<Integer> Qmines; //quantity
    //enemies list
    VecUnit enemyUnits;
    //enemy bases
    int[][] locToArea;
    int[] areaToLocX;
    int[] areaToLocY;
    double[][] exploreGrid;
    int exploreSizeX;
    int exploreSizeY;
    int INF = 1000000000;

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

    Integer locationToArea(MapLocation loc){
        int x = loc.getX();
        int y = loc.getY();
        return locToArea[x][y];
    }

    Integer encode(int i, int j){
        return i*maxMapSize+j;
    }

    int decodeX(Integer c){
        return c/maxMapSize;
    }

    int decodeY(Integer c){
        return c%maxMapSize;
    }

    MapLocation getAccesLocation(int xCenter, int yCenter){
        MapLocation realCenter = new MapLocation(gc.planet(), xCenter, yCenter);
        //TODO check apart from passable accessible from origin in earth
        if(map.isPassableTerrainAt(realCenter) > 0) return realCenter;
        for(int i = 0; i < allDirs.length; ++i){
            MapLocation fakeCenter = realCenter.add(allDirs[i]);
            if(map.isPassableTerrainAt(fakeCenter) > 0) return fakeCenter;
        }
        return null;
    }

    void createGrid(){
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
        if(gc.team() == Team.Blue) enemyTeam = Team.Red;
        else enemyTeam = Team.Blue;
        //danger matrix TODO implementation
        dangerMatrix = new int[W][H];
        //get location of enemy base
        getLocationEnemyBase();
    }

    public void getLocationEnemyBase(){
        VecUnit initialUnits = map.getInitial_units();
        for(int i = 0; i < initialUnits.size(); ++i){
            Unit possibleEnemy = initialUnits.get(i);
            if(possibleEnemy.team() == enemyTeam){
                Integer enemyArea = locationToArea(possibleEnemy.location().mapLocation());
                addExploreGrid(enemyArea, enemyBaseValue);
            }
        }
    }

    public void update() {
        //check enemy units
        enemyUnits = gc.senseNearbyUnitsByTeam(middle, maxRadius, enemyTeam);
        //check mines
        checkMines();
        //update areas explored
        updateCurrentArea();
    }


    void updateCurrentArea(){
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

    public void moveTo(Unit unit, MapLocation target){
        MovementManager.getInstance().moveTo(unit, target);
    }
}