import bc.*;

import java.util.*;

class Data {
    static GameController gc;
    static Research research;
    static ResearchInfo researchInfo;
    static MarsPlanning marsPlanning;
    static HashMap<Integer, Integer> karboniteAt;
    static HashMap<Integer, Integer> asteroidTasksLocs; //karbo location -> ID worker assignat
    static HashMap<Integer, Integer> asteroidTasksIDs;  //ID worker -> karbo location assignada
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
    static private final double neutralZoneDistance = 20;
    static private final double karboniteBfsDepth = 4;
    static private final double baseKarboniteMatrix = 0.75;
    static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Team myTeam;
    static Team enemyTeam;

    static Integer[] asteroidRounds;
    //static AsteroidStrike[] asteroidStrikes;
    private static AsteroidPattern asteroidPattern;
    static AuxMapLocation[] asteroidLocations;
    static Integer[] asteroidCarbo;

    private static int[][] dangerMatrix;
    private static double[][] initialDistToAllies;
    private static double[][] initialDistToEnemies;
    private static double[][] karboMatrix;
    private static final double enemyBaseValue = -5;
    static boolean aggro;
    static boolean firstRocket = true;
    static final int exploreConstant = 1;

    static HashMap<Integer, Integer> allUnits; //allUnits.get(id) = index de myUnits
    static HashMap<UnitType, Integer> unitTypeCount;
    static HashSet<Integer> structures;
    static HashMap<Integer, Integer> blueprintsToPlace;
    static HashMap<Integer, Integer> blueprintsToBuild;
    static HashMap<Integer, Integer> structuresToRepair;

    static boolean blueprint;

    static Integer karbonite;

    static int[][] unitMap;
    static int[][] karboMap;
    static AuxUnit[] myUnits; //myUnits[i] retorna una unit random meva
    static AuxUnit[] enemies;
    static int lastRoundEnemySeen;
    static boolean[][] accessible;
    static int lastRoundUnder100Karbo;

    private static int[] healingPowers = {10, 12, 17, 17};
    static int healingPower;
    static boolean canOverCharge = false;
    private static int[] buildingPowers = {5, 5, 6, 7, 10};
    static int buildingPower;
    private static int[] repairingPowers = {10, 10, 11, 12, 15};
    static int repairingPower;
    private static int[] harvestingPowers = {3, 4, 4, 4, 4};
    static int harvestingPower;
    private static int[] mageDamages = {60, 75, 90, 105, 105};
    static int mageDMG;
    static boolean canBlink = false;
    static int rocketCapacity;
    private static int[] rocketCapacities = {8, 8, 8, 12};

    static boolean canBuildRockets;



    static int replicateCost = 30;

    static HashSet<Integer> occupiedPositions;


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
        try {
            MapLocation realCenter = new MapLocation(gc.planet(), xCenter, yCenter);
            //TODO check apart from passable accessible from origin in earth
            if (planetMap.isPassableTerrainAt(realCenter) > 0) return realCenter;
            for (int i = 0; i < allDirs.length; ++i) {
                MapLocation fakeCenter = realCenter.add(allDirs[i]);
                if (planetMap.isPassableTerrainAt(fakeCenter) > 0) return fakeCenter;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void createGrid(){
        try {
            currentArea = new HashMap<>();
            exploreSizeX = W / areaSize;
            exploreSizeY = H / areaSize;
            double auxiliarX = (double) W / exploreSizeX;
            double auxiliarY = (double) H / exploreSizeY;
            exploreGrid = new double[exploreSizeX][exploreSizeY];
            locToArea = new int[W][H];
            areaToLocX = new int[exploreSizeX];
            areaToLocY = new int[exploreSizeY];
            for (int i = 0; i < exploreSizeX; ++i) {
                for (int j = 0; j < exploreSizeY; ++j) {
                    for (int x = (int) Math.floor(i * auxiliarX); x < Math.floor((i + 1) * auxiliarX); ++x) {
                        for (int y = (int) Math.floor(j * auxiliarY); y < Math.floor((j + 1) * auxiliarY); ++y) {
                            locToArea[x][y] = encode(i, j);
                        }
                    }
                    int xCenter = (int) Math.floor(i * auxiliarX) + areaSize / 2;
                    int yCenter = (int) Math.floor(j * auxiliarY) + areaSize / 2;
                    MapLocation centerArea = getAccesLocation(xCenter, yCenter);
                    if (centerArea != null) {
                        areaToLocX[i] = centerArea.getX();
                        areaToLocY[j] = centerArea.getY();
                        continue;
                    }
                    exploreGrid[i][j] = INF;
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static Integer locationToArea(AuxMapLocation loc){
        try {
            return locToArea[loc.x][loc.y];
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static int decodeX(Integer c){
        return c/maxMapSize;
    }

    static int decodeY(Integer c){
        return c%maxMapSize;
    }

    static void addExploreGrid(Integer area, double value) {
        try {
            int x = decodeX(area);
            int y = decodeY(area);
            exploreGrid[x][y] += value;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void getLocationEnemyBase(){
        try {
            VecUnit initialUnits = planetMap.getInitial_units();
            for (int i = 0; i < initialUnits.size(); ++i) {
                Unit possibleEnemy = initialUnits.get(i);
                if (possibleEnemy.team() == enemyTeam) {
                    Integer enemyArea = locationToArea((new AuxUnit(possibleEnemy, false).getMaplocation()));
                    addExploreGrid(enemyArea, enemyBaseValue);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    //ToDo check
    static void initGame(GameController _gc){
        try {
            gc = _gc;

            round = 1;
            lastRoundEnemySeen = 1;
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

            W = (int) planetMap.getWidth();
            H = (int) planetMap.getHeight();
            mapCenter = new MapLocation(gc.planet(), W / 2 + 1, H / 2 + 1);
            maxRadius = mapCenter.distanceSquaredTo(new MapLocation(gc.planet(), 0, 0));

            karboniteAt = new HashMap<>(); //filled in pathfinder
            asteroidTasksLocs = new HashMap<>();
            asteroidTasksIDs = new HashMap<>();
            unitTypeCount = new HashMap<>();

            queue = new ConstructionQueue();

            WorkerUtil.workerActions = new int[W][H];

            createGrid();

            Explore.initialize();
            myTeam = gc.team();
            if (myTeam == Team.Blue) enemyTeam = Team.Red;
            else enemyTeam = Team.Blue;
            //danger matrix TODO implementation
            dangerMatrix = new int[W][H];
            //get location of enemy base
            getLocationEnemyBase();

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void initGameAfterPathfinder() {
        if (planet == Planet.Earth) {
            computeInitialDists();
            computeKarboMatrix();
        }
    }

    private static void computeInitialDists() {
        initialDistToAllies = new double[W][H];
        initialDistToEnemies = new double[W][H];
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                initialDistToAllies[x][y] = -1;
                initialDistToEnemies[x][y] = -1;
            }
        }
        VecUnit vec = planetMap.getInitial_units();
        for (int i = 0; i < vec.size(); ++i) {
            Unit unit_i = vec.get(i);
            AuxMapLocation loc = new AuxMapLocation(unit_i.location().mapLocation());
            if (unit_i.team().equals(myTeam)) {
                for (int x = 0; x < W; ++x) {
                    for (int y = 0; y < H; ++y) {
                        double dist = loc.dirBFSTo(new AuxMapLocation(x,y));
                        if (initialDistToAllies[x][y] == -1 || dist < initialDistToAllies[x][y]) {
                            initialDistToAllies[x][y] = dist;
                        }
                    }
                }
            }
            else {
                for (int x = 0; x < W; ++x) {
                    for (int y = 0; y < H; ++y) {
                        double dist = loc.dirBFSTo(new AuxMapLocation(x,y));
                        if (initialDistToEnemies[x][y] == -1 || dist < initialDistToEnemies[x][y]) {
                            initialDistToEnemies[x][y] = dist;
                        }
                    }
                }
            }
        }
    }

    private static void addValue(double[][] mat, double value, AuxMapLocation initLoc, double depth) {
        HashSet<Integer> seen = new HashSet<>();
        Queue<AuxMapLocation> queue = new LinkedList<>();
        seen.add(initLoc.x << 6 | initLoc.y);
        queue.offer(initLoc);
        while (!queue.isEmpty()) {
            AuxMapLocation loc = queue.poll();
            for (int d = 0; d < 8; ++d) {
                AuxMapLocation newLoc = loc.add(d);
                if (!onTheMap(newLoc)) continue;
                if (!accessible[newLoc.x][newLoc.y]) continue;
                double dist = initLoc.dirBFSTo(newLoc);
                if (dist > depth) continue;
                if (seen.contains(newLoc.x << 6 | newLoc.y)) continue;
                seen.add(newLoc.x << 6 | newLoc.y);
                queue.add(newLoc);
                mat[newLoc.x][newLoc.y] += value*Math.pow(baseKarboniteMatrix, dist);
            }
        }
    }

    private static void computeKarboMatrix() {
        karboMatrix = new double[W][H];
        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                if (initialDistToEnemies[x][y] - initialDistToAllies[x][y] <= neutralZoneDistance) continue;
                double value = karboMap[x][y];
                addValue(karboMatrix, value, new AuxMapLocation(x,y), karboniteBfsDepth);
            }
        }

    }

    static AuxMapLocation toLocation(int x){
        try {
            return new AuxMapLocation(x >> 12, x & 0xFFF);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void putValue(int x, int y, int v){
        try {
            int encoding = encodeOcc(x, y);
            karboniteAt.put(encoding, v);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static Integer getValue(AuxMapLocation mloc){
        try {
            int encoding = encodeOcc(mloc.x, mloc.y);
            return karboniteAt.get(encoding);
        }catch(Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static void updateMines() {
        try {
            Iterator<HashMap.Entry<Integer, Integer>> it = karboniteAt.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry<Integer, Integer> entry = it.next();
                AuxMapLocation location = toLocation(entry.getKey());
                MapLocation mapLocation = new MapLocation(planet, location.x, location.y);
                int value = entry.getValue();
                if (gc.canSenseLocation(mapLocation)) {
                    long quant = gc.karboniteAt(mapLocation);
                    if (quant > INF) quant = INF;
                    if (quant > 0) {
                        if (quant != value) putValue(location.x, location.y, (int) quant);
                    } else it.remove();
                }
                //else karboMap[location.x][location.y] = karboniteAt.get(location);
                /*
                AuxUnit unitOnKarbo = getUnit(location.x,location.y,false);
                if (unitOnKarbo == null) unitOnKarbo = getUnit(location.x, location.y, true);
                if (unitOnKarbo != null){
                    if (unitOnKarbo.getType() == UnitType.Factory || unitOnKarbo.getType() == UnitType.Rocket)
                        karboMap[location.x][location.y] = 0;
                }*/
            }

            //mars stuff
            if (planet == Planet.Earth) return;
            Iterator<Map.Entry<Integer, Integer>> it2 = asteroidTasksIDs.entrySet().iterator();
            while (it2.hasNext()) {
                //esborra workers morts del hashmap
                Map.Entry<Integer, Integer> entry = it2.next();
                int assignedID = entry.getKey();
                if (!Data.allUnits.containsKey(assignedID)) it2.remove();
            }
            if (!asteroidPattern.hasAsteroid(round)) return;
            AsteroidStrike strike = asteroidPattern.asteroid(round);
            AuxMapLocation loc = new AuxMapLocation(strike.getLocation());
            boolean canAccess = false;
            for (int i = 0; i < 8; i++){
                //si el meteorit cau al mig de la muntanya, suda d'afegir-lo
                AuxMapLocation adjLoc = loc.add(i);
                if (adjLoc.isOnMap() && accessible[adjLoc.x][adjLoc.y]) canAccess = true;
            }
            if (!canAccess) return;
            asteroidTasksLocs.put(encodeOcc(loc.x, loc.y), -1);
            int karbonite = (int) strike.getKarbonite();
            if (karboniteAt.containsKey(encodeOcc(loc.x, loc.y)))
                putValue(loc.x, loc.y, karboniteAt.get(encodeOcc(loc.x, loc.y)) + karbonite);
            else putValue(loc.x, loc.y, karbonite);
/*
            System.out.println("");
            System.out.println("====================== TASK ARRAY " + round + " ====================== ");
            for (Map.Entry<Integer,Integer> entry: asteroidTasksLocs.entrySet()){
                AuxMapLocation l = toLocation(entry.getKey());
                int id = entry.getValue();
                System.out.println("Location " + l.x + "," + l.y + " has worker " + id);
            }
            System.out.println("");
            for (Map.Entry<Integer,Integer> entry: asteroidTasksIDs.entrySet()){
                int id = entry.getKey();
                AuxMapLocation l = toLocation(entry.getValue());
                System.out.println("Worker " + id + " has location " + l.x + "," + l.y);
            }
            System.out.println("");*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void updateCurrentArea(){
        try {
            for (AuxUnit unit : myUnits) {
                if (unit.isInGarrison()) continue;
                Integer current = locationToArea(unit.getMaplocation());
                if (currentArea.containsKey(unit.getID()) && currentArea.get(unit.getID()) == current) continue;
                currentArea.put(unit.getID(), current);
                addExploreGrid(current, exploreConstant);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    private static void checkMyUnits() {
        try {
            allUnits = new HashMap<>();
            structures = new HashSet<>();
            blueprint = false;
            int rangers = 0;
            int healers = 0;
            int workers = 0;
            int mages = 0;
            int knights = 0;
            int factories = 0;
            int rockets = 0;
            for (int i = 0; i < myUnits.length; i++) {
                AuxUnit u = myUnits[i];
                allUnits.put(u.getID(), i);
                UnitType type = u.getType();
                if (type == UnitType.Factory) {
                    factories++;
                    structures.add(i);
                } else if (type == UnitType.Rocket) {
                    if (u.isBlueprint()) blueprint = true;
                    rockets++;
                    structures.add(i);
                } else if (type == UnitType.Worker) {
                    //if (!u.isInGarrison()) workerBuilt = true;
                    ++workers;
                } else if (type == UnitType.Ranger) {
                    ++rangers;
                } else if (type == UnitType.Healer) {
                    ++healers;
                } else if (type == UnitType.Mage) {
                    ++mages;
                } else if (type == UnitType.Knight) {
                    ++knights;
                }
            }
            unitTypeCount.put(UnitType.Factory,factories);
            unitTypeCount.put(UnitType.Rocket, rockets);
            unitTypeCount.put(UnitType.Worker, workers);
            unitTypeCount.put(UnitType.Ranger, rangers);
            unitTypeCount.put(UnitType.Healer, healers);
            unitTypeCount.put(UnitType.Mage,   mages);
            unitTypeCount.put(UnitType.Knight, knights);

            //System.out.println(round + " Factory requested: " + queue.needsUnit(UnitType.Factory));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static AuxUnit getUnit(int x, int y, boolean myTeam){
        try {
            if (x < 0 || x >= W) return null;
            if (y < 0 || y >= H) return null;
            int i = unitMap[x][y];
            if (myTeam) {
                if (i > 0) return myUnits[i - 1];
                return null;
            }
            if (i < 0) return enemies[-(i + 1)];
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    static AuxUnit getUnit(int x, int y){
        try {
            if (x < 0 || x >= W) return null;
            if (y < 0 || y >= H) return null;
            int i = unitMap[x][y];
            if (i > 0) return myUnits[i - 1];
            if (i < 0) return enemies[-(i + 1)];
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    static boolean isOccupied(int x, int y){
        try {
            return unitMap[x][y] != 0;
        }catch(Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    static boolean isOccupied(AuxMapLocation location){
        try {
            return isOccupied(location.x, location.y);
        }catch(Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    static int priority(AuxUnit u){
        switch (u.getType()){
            case Mage:
                return 0;
            case Ranger:
                return 1;
            case Healer:
                return 2;
            case Knight:
                return 3;
            case Worker:
                return 4;
            case Rocket:
                return 5;
            case Factory:
                return 6;
            default:
                return 10;
        }
    }

    static void initTurn() {
        try {
            occupiedPositions = new HashSet<>();
            round++;
            unitMap = new int[W][H];
            for (int i = 0; i < W; ++i) {
                for (int j = 0; j < H; ++j) unitMap[i][j] = 0; //ToDO really needed?
            }
            karboMap = new int[W][H];
            //check enemy units
            VecUnit enemyUnits = gc.senseNearbyUnitsByTeam(mapCenter, maxRadius, enemyTeam);


            int ensiz = (int) enemyUnits.size();
            ArrayList<AuxUnit> auxenemyunits = new ArrayList<>();
            for (int i = 0; i < ensiz; ++i) auxenemyunits.add(new AuxUnit(enemyUnits.get(i), false));
            auxenemyunits.sort((a, b) -> priority(a) < priority(b) ? -1 : priority(a) == priority(b) ? 0 : 1);
            enemies = auxenemyunits.toArray(new AuxUnit[ensiz]);

            //enemies = new AuxUnit[(int) enemyUnits.size()];
            for (int i = 0; i < enemies.length; ++i) {
                enemies[i] = new AuxUnit(enemyUnits.get(i), false);
                unitMap[enemies[i].getX()][enemies[i].getY()] = -(i + 1);
            }
            if (enemyUnits.size() != 0) lastRoundEnemySeen = round;
            VecUnit units = gc.myUnits();

            int siz = (int) units.size();
            ArrayList<AuxUnit> auxunits = new ArrayList<>();
            for (int i = 0; i < siz; ++i) auxunits.add(new AuxUnit(units.get(i), true));
            auxunits.sort((a, b) -> priority(a) < priority(b) ? -1 : priority(a) == priority(b) ? 0 : 1);
            myUnits = auxunits.toArray(new AuxUnit[siz]);

            //myUnits = new AuxUnit[(int) units.size()];
            for (int i = 0; i < myUnits.length; ++i) {
                myUnits[i] = new AuxUnit(units.get(i), true);
                if (!myUnits[i].isInGarrison()) unitMap[myUnits[i].getX()][myUnits[i].getY()] = i + 1;
            }
            researchInfo = gc.researchInfo();
            karbonite = null;
            //check mines
            updateMines();
            for (int i = 0; i < W; ++i) {
                for (int j = 0; j < H; ++j) karboMap[i][j] = 0;
            }
            for (Integer a : karboniteAt.keySet()) {
                AuxMapLocation mloc = toLocation(a);
                karboMap[mloc.x][mloc.y] = karboniteAt.get(a);
            }
            //update areas explored
            updateCurrentArea();
            //comprova si ha de construir factory o rocket
            checkMyUnits();
            UnitManager.initWorkerMaps();

            Rocket.initTurn();

            karbonite = (int)gc.karbonite();
            if (karbonite < 100) lastRoundUnder100Karbo = round;

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
            if (mageLevel >= 4) canBlink = true;
            if (healerLevel >= 3) canOverCharge = true;
            rocketCapacity = rocketCapacities[rocketLevel];
            if (rocketLevel > 0) canBuildRockets = true;

            WorkerUtil.fillWorkerActions();

            //if (Data.round >= 746) printData();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void printData(){
        try {
            for (int i = 0; i < H; ++i) {
                for (int j = 0; j < H; ++j) {
                    if (unitMap[i][j] > 0) System.err.print("1");
                    else if (unitMap[i][j] < 0) System.err.print("2");
                    else System.err.print("0");
                }
                System.err.println();
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean onEarth(){ return planet == Planet.Earth;}

    public static boolean onMars(){ return planet == Planet.Mars;}

    public static boolean onTheMap(AuxMapLocation location){
        try {
            int x = location.x;
            int y = location.y;
            return x >= 0 && x < W && y >= 0 && y < H;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static int encodeOcc(int x, int y){
        return ((x << 12) | y);
    }

}
