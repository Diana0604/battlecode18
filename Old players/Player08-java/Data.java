

import bc.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

class Data {
    static GameController gc;
    static Research research;
    static MarsPlanning marsPlanning;
    static HashMap<MapLocation, Integer> karboniteAt;
    static Planet planet;
    static PlanetMap planetMap;
    static ConstructionQueue queue;

    static private int INF = 1000000000;
    static int round;

    static int W;
    static int H;
    static MapLocation mapCenter;
    static long maxRadius;

    static HashMap<Integer, Integer> currentArea;
    static int[] areaToLocX;
    static int[] areaToLocY;
    static double[][] exploreGrid;
    static int exploreSizeX;
    static int exploreSizeY;
    static private final int areaSize = 5;
    static private int[][] locToArea;
    static private final int maxMapSize = 50;
    static private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Team myTeam;
    static Team enemyTeam;

    static Integer[] asteroidRounds;
    static AsteroidStrike[] asteroidStrikes;
    static AsteroidPattern asteroidPattern;

    private static int[][] dangerMatrix;
    private static final double enemyBaseValue = -5;
    static boolean aggro;
    static final int exploreConstant = 1;

    static HashMap<Integer, Integer> allUnits;
    static VecUnit enemyUnits, units;
    static HashSet<Integer> structures;

    static Integer encode(int i, int j){
        return i*maxMapSize+j;
    }

    static private MapLocation getAccesLocation(int xCenter, int yCenter){
        MapLocation realCenter = new MapLocation(gc.planet(), xCenter, yCenter);
        //TODO check apart from passable accessible from origin in earth
        if(planetMap.isPassableTerrainAt(realCenter) > 0) return realCenter;
        for(int i = 0; i < allDirs.length; ++i){
            MapLocation fakeCenter = realCenter.add(allDirs[i]);
            if(planetMap.isPassableTerrainAt(fakeCenter) > 0) return fakeCenter;
        }
        return null;
    }

    private static void createGrid(){
        currentArea = new HashMap<>();
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

    private static Integer locationToArea(MapLocation loc){
        int x = loc.getX();
        int y = loc.getY();
        return locToArea[x][y];
    }

    static int decodeX(Integer c){
        return c/maxMapSize;
    }

    static int decodeY(Integer c){
        return c%maxMapSize;
    }

    static void addExploreGrid(Integer area, double value) {
        int x = decodeX(area);
        int y = decodeY(area);
        exploreGrid[x][y] += value;
    }

    private static void getLocationEnemyBase(){
        VecUnit initialUnits = planetMap.getInitial_units();
        for(int i = 0; i < initialUnits.size(); ++i){
            Unit possibleEnemy = initialUnits.get(i);
            if(possibleEnemy.team() == enemyTeam){
                Integer enemyArea = locationToArea(possibleEnemy.location().mapLocation());
                addExploreGrid(enemyArea, enemyBaseValue);
            }
        }
    }

    static void initGame(GameController _gc){
        gc = _gc;

        round = 1;
        aggro = false;
        Research.initialize(gc);
        research = Research.getInstance();
        research.yolo();

        MarsPlanning.initialize(gc); //calcula els asteroids
        marsPlanning = MarsPlanning.getInstance();
        asteroidPattern = gc.asteroidPattern();

        planet = gc.planet();
        planetMap = gc.startingMap(planet);

        W = (int)planetMap.getWidth();
        H = (int)planetMap.getHeight();
        mapCenter = new MapLocation(gc.planet(), W/2, H/2);
        maxRadius = mapCenter.distanceSquaredTo(new MapLocation(gc.planet(), 0, 0));

        karboniteAt = new HashMap<MapLocation, Integer>();

        queue = new ConstructionQueue();

        createGrid();


        myTeam = gc.team();
        if(myTeam == Team.Blue) enemyTeam = Team.Red;
        else enemyTeam = Team.Blue;
        //danger matrix TODO implementation
        dangerMatrix = new int[W][H];
        //get location of enemy base
        getLocationEnemyBase();
    }


    private static void updateMines(){
        Iterator<HashMap.Entry<MapLocation,Integer> > it = karboniteAt.entrySet().iterator();
        while (it.hasNext()){
            HashMap.Entry<MapLocation, Integer> entry = it.next();
            MapLocation location = entry.getKey();
            int value = entry.getValue();
            if (gc.canSenseLocation(location)){
                long quant = gc.karboniteAt(location);
                if (quant > INF) quant = INF;
                if (quant > 0){
                    if (quant != value) karboniteAt.put(location, (int) quant);
                }else it.remove();
            }
        }

        if (planet == Planet.Earth) return;
        if (!asteroidPattern.hasAsteroid(round)) return;
        AsteroidStrike strike = asteroidPattern.asteroid(round);
        MapLocation loc = strike.getLocation();
        int karbonite = (int) strike.getKarbonite();
        karboniteAt.put(loc,karbonite);
    }


    private static void updateCurrentArea(){
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


    private static void checkMyUnits(){
        allUnits = new HashMap<>();
        structures = new HashSet<>();
        VecUnit v = gc.myUnits();
        int MIN_KARBONITE_FOR_FACTORY = 200;
        int INITIAL_FACTORIES = 3;
        int factories = 0;
        boolean rocketBuilt = false;
        boolean workerBuilt = false;
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);
            allUnits.put(u.id(), i);
            UnitType type = u.unitType();
            if (type == UnitType.Factory){
                factories++;
                structures.add(i);
            }else if (type == UnitType.Rocket){
                rocketBuilt = true;
                structures.add(i);
            }else if (type == UnitType.Worker && !u.location().isInGarrison()) workerBuilt = true;
        }
        if (factories <= INITIAL_FACTORIES || gc.karbonite() > MIN_KARBONITE_FOR_FACTORY) queue.requestUnit(UnitType.Factory);
        if (!rocketBuilt && v.size() > 8 && gc.researchInfo().getLevel(UnitType.Rocket) > 0) { // aixo es super cutre, canviar!
            queue.requestUnit(UnitType.Rocket);
        }
        if (!workerBuilt) queue.requestUnit(UnitType.Worker);
        //System.out.println("Rocket requested: " + queue.needsUnit(UnitType.Rocket));
    }


    static void initTurn(){
        round++;
        //check enemy units
        enemyUnits = gc.senseNearbyUnitsByTeam(mapCenter, maxRadius, enemyTeam);
        units = gc.myUnits();
        //check mines
        updateMines();
        //update areas explored
        updateCurrentArea();
        //comprova si ha de construir factory o rocket
        checkMyUnits();
        //research.checkResearch();
        Rocket.initTurn();

        Danger.updateAttackers();
        if (!aggro && gc.researchInfo().getLevel(UnitType.Ranger) > 1) aggro = true;
    }



}
