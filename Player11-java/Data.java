import bc.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

class Data {
    static GameController gc;
    static Research research;
    static ResearchInfo researchInfo;
    static MarsPlanning marsPlanning;
    static HashMap<AuxMapLocation, Integer> karboniteAt;
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
    static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Team myTeam;
    static Team enemyTeam;

    static Integer[] asteroidRounds;
    static AsteroidStrike[] asteroidStrikes;
    private static AsteroidPattern asteroidPattern;

    private static int[][] dangerMatrix;
    private static final double enemyBaseValue = -5;
    static boolean aggro;
    static final int exploreConstant = 1;

    static HashMap<Integer, Integer> allUnits;
    //static VecUnit enemyUnits, units;
    static HashSet<Integer> structures;

    static int rangers;
    static int healers;

    static Integer karbonite;

    static int[][] unitMap;
    static int[][] karboMap;
    static AuxUnit[] myUnits;
    static AuxUnit[] enemies;
    static boolean[][] accessible;

    private static int[] healingPowers = {10, 12, 17, 17};
    static int healingPower;
    private static int[] buildingPowers = {5, 5, 6, 7, 10};
    static int buildingPower;
    private static int[] repairingPowers = {10, 10, 11, 12, 15};
    static int repairingPower;
    private static int[] harvestingPowers = {3, 4, 4, 4, 4};
    static int harvestingPower;
    private static int[] mageDamages = {60, 75, 90, 105};
    static int mageDMG;

    static boolean canBuildRockets;

    static int replicateCost = 15;



    static int getKarbonite(){
        if (karbonite == null){
            karbonite = (int)gc.karbonite();
        }
        return karbonite;
    }

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

    private static Integer locationToArea(AuxMapLocation loc){
        return locToArea[loc.x][loc.y];
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
                Integer enemyArea = locationToArea((new AuxUnit(possibleEnemy)).getMaplocation());
                addExploreGrid(enemyArea, enemyBaseValue);
            }
        }
    }

    //ToDo check
    static void initGame(GameController _gc){
        gc = _gc;

        round = 1;
        aggro = false;

        research = Research.getInstance();
        research.yolo();
        researchInfo = gc.researchInfo();
        canBuildRockets = false;

        //MarsPlanning.initialize(gc); //calcula els asteroids
        marsPlanning = MarsPlanning.getInstance();
        asteroidPattern = gc.asteroidPattern();

        planet = gc.planet();
        planetMap = gc.startingMap(planet);

        W = (int)planetMap.getWidth();
        H = (int)planetMap.getHeight();
        mapCenter = new MapLocation(gc.planet(), W/2+1, H/2+1);
        maxRadius = mapCenter.distanceSquaredTo(new MapLocation(gc.planet(), 0, 0));

        karboniteAt = new HashMap<AuxMapLocation, Integer>();

        queue = new ConstructionQueue();

        createGrid();
        Vision.initialize();

        myTeam = gc.team();
        if(myTeam == Team.Blue) enemyTeam = Team.Red;
        else enemyTeam = Team.Blue;
        //danger matrix TODO implementation
        dangerMatrix = new int[W][H];
        //get location of enemy base
        getLocationEnemyBase();
    }


    private static void updateMines(){
        Iterator<HashMap.Entry<AuxMapLocation,Integer> > it = karboniteAt.entrySet().iterator();
        while (it.hasNext()){
            HashMap.Entry<AuxMapLocation, Integer> entry = it.next();
            AuxMapLocation location = entry.getKey();
            MapLocation mapLocation = new MapLocation(planet, location.x, location.y);
            int value = entry.getValue();
            if (gc.canSenseLocation(mapLocation)){
                long quant = gc.karboniteAt(mapLocation);
                if (quant > INF) quant = INF;
                if (quant > 0){
                    if (quant != value) karboniteAt.put(location, (int) quant);
                    karboMap[mapLocation.getX()][mapLocation.getY()] = (int)quant;
                }else it.remove();
            }
            else karboMap[mapLocation.getX()][mapLocation.getY()] = karboniteAt.get(location);
        }

        if (planet == Planet.Earth) return;
        if (!asteroidPattern.hasAsteroid(round)) return;
        AsteroidStrike strike = asteroidPattern.asteroid(round);
        AuxMapLocation loc = new AuxMapLocation(strike.getLocation());
        int karbonite = (int) strike.getKarbonite();
        if (!karboniteAt.containsKey(loc)) karboniteAt.put(loc,karbonite);
        else karboniteAt.put(loc,karbonite + karboniteAt.get(loc));
    }


    private static void updateCurrentArea(){
        for(int i = 0; i < myUnits.length; ++i){
            AuxUnit unit = myUnits[i];
            if(unit.isInGarrison()) continue;
            Integer current = locationToArea(unit.getMaplocation());
            if(currentArea.containsKey(unit.getID()) && currentArea.get(unit.getID()) == current) continue;
            currentArea.put(unit.getID(),current);
            addExploreGrid(current,exploreConstant);
        }
    }


    private static void checkMyUnits(){
        allUnits = new HashMap<>();
        structures = new HashSet<>();
        rangers = 0;
        healers = 0;
        int MIN_KARBONITE_FOR_FACTORY = 200;
        int INITIAL_FACTORIES = 3;
        int factories = 0;
        boolean rocketBuilt = false;
        boolean workerBuilt = false;
        for (int i = 0; i < myUnits.length; i++){
            AuxUnit u = myUnits[i];
            allUnits.put(u.getID(), i);
            UnitType type = u.getType();
            if (type == UnitType.Factory){
                factories++;
                structures.add(i);
            }else if (type == UnitType.Rocket){
                rocketBuilt = true;
                structures.add(i);
            }else if (type == UnitType.Worker && !u.isInGarrison()) workerBuilt = true;
            else if (type == UnitType.Ranger){
                ++rangers;
            } else if (type == UnitType.Healer) {
                ++healers;
            }
        }
        //Todo: wrappejar gc.karbonite()??
        if (factories < INITIAL_FACTORIES || getKarbonite() > MIN_KARBONITE_FOR_FACTORY) queue.requestUnit(UnitType.Factory);
        if (!rocketBuilt && myUnits.length > 8 && researchInfo.getLevel(UnitType.Rocket) > 0) { // aixo es super cutre, canviar!
            queue.requestUnit(UnitType.Rocket);
        }
        if (!workerBuilt) queue.requestUnit(UnitType.Worker);
        //System.out.println(round + " Factory requested: " + queue.needsUnit(UnitType.Factory));
    }


    static AuxUnit getUnit(int x, int y, boolean myTeam){
        if (x < 0 || x >= W) return null;
        if (y < 0 || y >= H) return null;
        int i = unitMap[x][y];
        if (myTeam) {
            if (i > 0) return myUnits[i - 1];
            return null;
        }
        if (i < 0) return enemies[-i-1];
        return null;
    }

    static boolean isOccupied(int x, int y){
        return getUnit(x, y, false) != null;
    }

    static boolean isOccupied(AuxMapLocation location){
        return isOccupied(location.x, location.y);
    }

    static void initTurn() {
        round++;
        unitMap = new int[W][H];
        for (int i = 0; i < W; ++i) {
            for (int j = 0; j < H; ++j) unitMap[i][j] = 0; //ToDO really needed?
        }
        karboMap = new int[W][H];
        for (int i = 0; i < W; ++i) {
            for (int j = 0; j < H; ++j) karboMap[i][j] = 0; //ToDO really needed?
        }
        //check enemy units
        VecUnit enemyUnits = gc.senseNearbyUnitsByTeam(mapCenter, maxRadius, enemyTeam);
        enemies = new AuxUnit[(int) enemyUnits.size()];
        for (int i = 0; i < enemies.length; ++i) {
            enemies[i] = new AuxUnit(enemyUnits.get(i));
            unitMap[enemies[i].getX()][enemies[i].getY()] = -i - 1;
        }
        VecUnit units = gc.myUnits();
        myUnits = new AuxUnit[(int) units.size()];
        for (int i = 0; i < myUnits.length; ++i) {
            myUnits[i] = new AuxUnit(units.get(i));
            unitMap[myUnits[i].getX()][myUnits[i].getY()] = i + 1;
        }
        researchInfo = gc.researchInfo();
        karbonite = null;
        //check mines
        updateMines();
        //update areas explored
        updateCurrentArea();
        //comprova si ha de construir factory o rocket
        checkMyUnits();

        Rocket.initTurn();


        Danger.updateAttackers();
        if (!aggro && researchInfo.getLevel(UnitType.Ranger) > 1) aggro = true;

        int workerLevel = (int) researchInfo.getLevel(UnitType.Worker);
        int healerLevel = (int) researchInfo.getLevel(UnitType.Healer);
        int rocketLevel = (int) researchInfo.getLevel(UnitType.Rocket);
        int mageLevel = (int) researchInfo.getLevel(UnitType.Mage);
        buildingPower = buildingPowers[workerLevel];
        repairingPower = repairingPowers[workerLevel];
        harvestingPower = harvestingPowers[workerLevel];
        healingPower = healingPowers[healerLevel];
        mageDMG = mageDamages[mageLevel];
        if (rocketLevel > 0) canBuildRockets = true;
    }

    public static boolean onEarth(){ return planet == Planet.Earth;}

    public static boolean onMars(){ return planet == Planet.Mars;}

    public static boolean onTheMap(AuxMapLocation location){
        int x = location.x;
        int y = location.y;
        return x >= 0 && x < W && y >= 0  && y < H;
    }

}
