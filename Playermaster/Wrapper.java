import java.util.*;

import bc.*;
/**
 * Created by Ivan on 1/18/2018.
 */
public class Wrapper {
    static AuxUnit[] senseUnits(int x, int y, int r, boolean myTeam){ //Todo check if it is better to iterate over enemies
        try {
            ArrayList<AuxUnit> ans = new ArrayList<>();
            for (int i = 0; i < Vision.Mx[r].length; ++i) {
                AuxMapLocation newLoc = new AuxMapLocation(x + Vision.Mx[r][i], y + Vision.My[r][i]);
                AuxUnit unit = newLoc.getUnit(myTeam);
                if (unit != null) ans.add(unit);
            }
            return ans.toArray(new AuxUnit[ans.size()]);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static AuxUnit[] senseUnits(int x, int y, int r){ //Todo check if it is better to iterate over enemies
        try {
            ArrayList<AuxUnit> ans = new ArrayList<>();
            for (int i = 0; i < Vision.Mx[r].length; ++i) {
                AuxMapLocation newLoc = new AuxMapLocation(x + Vision.Mx[r][i], y + Vision.My[r][i]);
                AuxUnit unit = newLoc.getUnit();
                if (unit != null) ans.add(unit);
            }
            return ans.toArray(new AuxUnit[ans.size()]);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static AuxUnit[] senseUnits(AuxMapLocation loc, int r, boolean myTeam){
        try {
            return senseUnits(loc.x, loc.y, r, myTeam);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static boolean canUnload(AuxUnit unit, int dir) {
        try {
            if (unit.getGarrisonUnits().size() == 0) return false;
            if (!unit.getMapLocation().add(dir).isAccessible()) return false;
            if (!Units.getUnitByID(unit.getGarrisonUnits().get(0)).canMove()) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void unload (AuxUnit unit, int dir){
        try {
            int newID = unit.getGarrisonUnits().get(0);
            int posAtArray = Units.allUnits.get(newID);
            AuxMapLocation loc = unit.getMapLocation();
            AuxMapLocation newLoc = loc.add(dir);
            //GC.unitMap[newLoc.x][newLoc.y] = posAtArray + 1;
            //GC.myUnits[posAtArray].mloc = newLoc;

            //Units.newOccupiedPositions.add(newLoc.encode());
            unit.garrisonUnits.remove(0);

            //System.out.println("I should unload " + newID);
            //System.out.println("I unloaded " + GC.gc.senseUnitAtLocation(new MapLocation(Mapa.planet, newLoc.x, newLoc.y)).id());

            //System.out.println(GC.gc.unit(newID).location().isInGarrison());

            GC.gc.unload(unit.getID(), Const.allDirs[dir]);

            //System.out.println("I unloaded " + GC.gc.senseUnitAtLocation(new MapLocation(Mapa.planet, newLoc.x, newLoc.y)).id());


            //System.out.println(GC.gc.unit(newID).location().isInGarrison());

            AuxUnit unloadedUnit = Units.myUnits.get(posAtArray);
            unloadedUnit.canMove = false;
            unloadedUnit.garrison = false;
            unloadedUnit.loc = unloadedUnit.unit.location();
            unloadedUnit.mloc = newLoc;
            Units.unitMap[newLoc.x][newLoc.y] = posAtArray + 1;

        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    public static int getIndex(Direction dir) {
        switch (dir) {
            case North:
                return 0;
            case Northeast:
                return 1;
            case East:
                return 2;
            case Southeast:
                return 3;
            case South:
                return 4;
            case Southwest:
                return 5;
            case West:
                return 6;
            case Northwest:
                return 7;
            default:
                return 0;

        }
    }



    static boolean canProduceUnit(AuxUnit unit, UnitType type){
        try {
            if (!unit.canAttack()) return false;
            if (unit.getGarrisonUnits().size() >= 8) return false;
            return (Utils.karbonite >= Units.getCost(type));
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void produceUnit(AuxUnit unit, UnitType type){
        try {
            GC.gc.produceRobot(unit.getID(), type);
            Utils.karbonite = Utils.karbonite - Units.getCost(type);
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void heal(AuxUnit u1, AuxUnit u2){
        try {
            u2.getHealth();
            u2.health += Units.healingPower;
            int mh = Units.getMaxHealth(u2.getType());
            if (u2.health > mh) u2.health = mh;
            u1.canAttack = false;
            GC.gc.heal(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canMove(AuxUnit unit, int dir) {
        try {
            if (!unit.canMove()) return false;
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            return (newLoc.isAccessible());
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void build(AuxUnit unit, AuxUnit blueprint){
        try {
            blueprint.getHealth();
            //blueprint.isBlueprint();
            blueprint.health += Units.buildingPower;
            int maxHP = Units.getMaxHealth(blueprint.getType());
            if (blueprint.health > maxHP) blueprint.health = maxHP;
            if (blueprint.health == maxHP) {
                blueprint.built = true;
            }
            GC.gc.build(unit.getID(), blueprint.getID());
            //System.out.println("Remaining build health: " + (maxHP - blueprint.health) +  " max hp? " + blueprint.isMaxHealth());
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void repair(AuxUnit unit, AuxUnit structure){
        try {
            structure.getHealth();
            structure.health += Units.repairingPower;
            int maxHP = Units.getMaxHealth(structure.getType());
            if (structure.health > maxHP) structure.health = maxHP;
            //AuxMapLocation loc = unit.getMapLocation();
            //AuxMapLocation sloc = structure.getMapLocation();
            //int distance = loc.distanceSquaredTo(sloc);
            //System.out.println("DISTANCE BETWEEN " + loc.x + "," + loc.y + " AND " + sloc.x + "," + sloc.y + " = " + distance + "  " + unit.getID());
            //System.out.println(unit.unit.location().mapLocation().getX() + " " + unit.unit.location().mapLocation().getY());
            //Unit unitt = GC.gc.unit(structure.getID());
            GC.gc.repair(unit.getID(), structure.getID());
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void moveRobot(AuxUnit unit, int dir) {
        try {
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            //if (!isAccessible(newLoc)){
            //System.err.println("User error");
            //return;
            //}
            unit.canMove = false;
            unit.mloc = newLoc;
            Units.unitMap[mloc.x][mloc.y] = 0;
            Units.unitMap[newLoc.x][newLoc.y] = Units.allUnits.get(unit.getID()) + 1;
            GC.gc.moveRobot(unit.getID(), Const.allDirs[dir]);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void blink(AuxUnit unit, AuxMapLocation mloc) {
        try {
            AuxMapLocation loc = unit.getMapLocation();
            unit.canUseAbility = false;
            Units.unitMap[loc.x][loc.y] = 0;
            Units.unitMap[mloc.x][mloc.y] = Units.allUnits.get(unit.getID()) + 1;
            GC.gc.blink(unit.getID(), new MapLocation(Mapa.planet, mloc.x, mloc.y));
            unit.mloc = mloc;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canReplicate(AuxUnit unit, int dir){
        try {
            if (Utils.karbonite < Const.replicateCost) return false;
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            if (!newLoc.isAccessible()) return false;
            if (!unit.canUseAbility()) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static AuxUnit replicate(AuxUnit unit, int dir){
        try {
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            GC.gc.replicate(unit.getID(), Const.allDirs[dir]);
            Unit newWorker = GC.gc.senseUnitAtLocation(new MapLocation(Mapa.planet, newLoc.x, newLoc.y));
            Utils.karbonite -= Const.replicateCost;
            Units.newOccupiedPositions.add(newLoc.encode());
            unit.canUseAbility = false;
            unit.canAttack = false;
            return new AuxUnit(newWorker, true);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static boolean canPlaceBlueprint (AuxUnit unit, UnitType type, int dir){
        try {
            if (Mapa.planet == Planet.Mars) return false;
            if (Utils.karbonite < Units.getCost(type)) return false;
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            if (!newLoc.isAccessible()) return false;
            if (type == UnitType.Rocket && !Units.canBuildRockets) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void placeBlueprint(AuxUnit unit, UnitType type, int dir) {
        try {
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            GC.gc.blueprint(unit.getID(), type, Const.allDirs[dir]);

            unit.canAttack = false;
            Utils.karbonite -= Units.getCost(type);
            Units.newOccupiedPositions.add(newLoc.encode());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canHarvest(AuxUnit unit, int dir){
        try {
            AuxMapLocation loc = unit.getMapLocation();
            AuxMapLocation mineLoc = loc.add(dir);
            if (!mineLoc.isOnMap()) return false;
            return GC.gc.canHarvest(unit.getID(), Const.allDirs[dir]);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // retorna -1 si no fa harvest
    // si fa harvest, retorna la karbo que queda al lloc
    static int harvest(AuxUnit unit, int dir) {
        try {
            //System.out.println("Entra harvest ");
            AuxMapLocation loc = unit.getMapLocation();
            AuxMapLocation mineLoc = loc.add(dir);
            int karboAmount = Karbonite.karboMap[mineLoc.x][mineLoc.y];
            if (karboAmount == 0) return -1;
            GC.gc.harvest(unit.getID(), Const.allDirs[dir]);
            int newKarboAmount = karboAmount - Units.harvestingPower;
            WorkerUtil.totalKarboCollected += Math.min(karboAmount, Units.harvestingPower);
            if (newKarboAmount < 0) newKarboAmount = 0;
            if (newKarboAmount > 0) {
                Karbonite.karboniteAt.put(mineLoc.encode(), newKarboAmount);
            } else Karbonite.karboniteAt.remove(mineLoc);
            Karbonite.karboMap[mineLoc.x][mineLoc.y] = newKarboAmount;
            unit.canAttack = false;
            //System.out.println("Karbo after mining: " + newKarboAmount);
            return newKarboAmount;
        }catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    static boolean canAttack(AuxUnit unit, AuxUnit unit2){
        try {
            if (!unit.canAttack()) return false;
            int d = unit.getMapLocation().distanceSquaredTo(unit2.getMapLocation());
            if (unit.getType() == UnitType.Ranger && d <= Const.rangerMinAttackRange) return false;
            return (Units.getAttackRange(unit.getType()) >= d);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void attack(AuxUnit u1, AuxUnit u2){
        try {
            if (u1.getType() != UnitType.Mage) {
                u2.getHealth();
                u2.health -= (int) Units.getDamage(u1.getType());
                if (u2.health <= 0) Units.unitMap[u2.getX()][u2.getY()] = 0;
            } else {
                AuxMapLocation mloc = u2.getMapLocation();
                for (int i = 0; i < 9; ++i) {
                    AuxMapLocation newLoc = mloc.add(i);
                    AuxUnit unit2 = newLoc.getUnit();
                    if (unit2 != null) {
                        unit2.getHealth();
                        unit2.health -= (int) Units.getDamage(u1.getType());
                        if (unit2.health <= 0){
                            Units.unitMap[unit2.getMapLocation().x][unit2.getMapLocation().y] = 0;
                            unit2.canMove = false;
                            unit2.canAttack = false;
                            if (u2.myTeam) {
                                unit2.inSpace = true;
                                unit2.mloc = null;
                            }
                        }
                    }
                }
            }
            u1.canAttack = false;
            GC.gc.attack(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static int getArrivalRound(int round){
        try {
            return round + (int) GC.gc.orbitPattern().duration(round);
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    static void launchRocket(AuxUnit unit, AuxMapLocation loc){
        try {
            //System.out.println("Launching at " + unit.getX() + " " + unit.getY());
            ArrayList<Integer> IDs = unit.getGarrisonUnits();
            for (int i = 0; i < IDs.size(); ++i){
                AuxUnit u = Units.getUnitByID(IDs.get(i));
                u.garrison = false;
                u.inSpace = true;
            }

            GC.gc.launchRocket(unit.getID(), new MapLocation(Planet.Mars, loc.x, loc.y));
            AuxMapLocation mloc = unit.getMapLocation();
            Units.firstRocket = false;
            Units.rocketsLaunched++;
            Units.unitMap[mloc.x][mloc.y] = 0;
            Units.structures.remove(unit.getID());

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canLoad(AuxUnit u1, AuxUnit u2) {
        try {
            if (u1.getType() != UnitType.Factory && u1.getType() != UnitType.Rocket) return false;
            if (!u2.canMove()) return false;
            if (u1.getGarrisonUnits().size() >= 8) return false;
            if (u2.isInGarrison()) return false;
            if (u1.isBlueprint()) return false;
            return (u1.getMapLocation().distanceSquaredTo(u2.getMapLocation()) <= 2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void load(AuxUnit u1, AuxUnit u2){
        try {
            //System.out.println("Loading! at " + u2.getMapLocation().x + " " + u2.getMapLocation().y + " " + u2.getID());
            AuxMapLocation mloc = u2.getMapLocation();
            Units.unitMap[mloc.x][mloc.y] = 0;
            u2.garrison = true;
            u2.mloc = u1.getMapLocation();
            u2.canMove = false;
            u2.canAttack = false;
            u2.loc = u2.unit.location();
            u1.getGarrisonUnits().add(u2.getID());
            GC.gc.load(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void overcharge(AuxUnit u1, AuxUnit u2){
        u1.canUseAbility = false;
        u2.canMove = true;
        u2.canAttack = true;
        u2.canUseAbility = true;
        GC.gc.overcharge(u1.getID(), u2.getID());
    }

    static long getKarbonite(AuxMapLocation location){
        MapLocation karboLoc = new MapLocation(Mapa.planet, location.x, location.y);
        if (GC.gc.canSenseLocation(karboLoc)){
            return GC.gc.karboniteAt(karboLoc);
        }else return -1;
    }

    /*------------------ KARBONITE INIT GAME -----------------------*/


    public static void karboniteInitMap(){
        Karbonite.asteroidPattern = GC.gc.asteroidPattern();
        Karbonite.karboniteAt = new HashMap<>();
        Karbonite.asteroidTasksLocs = new HashMap<>();
        Karbonite.asteroidTasksIDs = new HashMap<>();
        addInitialKarbo();
        fillKarboMap();
    }

    static void addInitialKarbo(){
        //System.out.println("ok1");
        for (int x = 0; x < Mapa.W; ++x) {
            //System.out.println("ok2");
            for (int y = 0; y < Mapa.H; ++y) {
                //System.out.println("ok3");
                int karbonite = Mapa.getInitialKarbo(x,y);
                if (karbonite > Const.INF) karbonite = Const.INF;
                if (karbonite > 0) putMine(x, y, karbonite);
                //System.out.println("Afegeix karbo " + x + "," + y + ": " + karbonite);
            }
        }
    }

    static void fillKarboMap(){
        Karbonite.karboMap = new int[Mapa.W][Mapa.H];
        //System.out.println("In fillkarbomap");
        for (Integer a : Karbonite.karboniteAt.keySet()) {
            AuxMapLocation loc = new AuxMapLocation(a);
            Karbonite.karboMap[loc.x][loc.y] = Karbonite.karboniteAt.get(a);
            //System.out.println(loc + " contains karbo " + karboniteAt.get(a));
        }
    }

    static void putMine(AuxMapLocation loc, int value){
        Karbonite.karboniteAt.put(loc.encode(), value);
    }

    static void putMine(int x, int y, int value){
        putMine(new AuxMapLocation(x,y),value);
    }

    /*------------------ UNITS GAME -----------------------*/


    /*------------------ PATHFINDER GAME -----------------------*/

    public static void pathfinderInitMap(){
        try {
            Pathfinder.W = Mapa.W;
            Pathfinder.H = Mapa.H;

            Pathfinder.Nodes = new PathfinderNode[Pathfinder.W][Pathfinder.H][][];
            //init pathfinder nodes
            for (int x = 0; x < Pathfinder.W; ++x) {
                for (int y = 0; y < Pathfinder.H; ++y) {
                    Pathfinder.Nodes[x][y] = null;
                }
            }

            Pathfinder.passable = new boolean[Pathfinder.W][Pathfinder.H];

            for (int x = 0; x < Pathfinder.W; ++x) {
                for (int y = 0; y < Pathfinder.H; ++y) {
                    if (Mapa.planetMap.isPassableTerrainAt(new MapLocation(Mapa.planet, x, y)) > 0) {
                        Pathfinder.passable[x][y] = true;
                    } else Pathfinder.passable[x][y] = false;
                }
            }
            computeDistToWalls();

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void computeDistToWalls(){
        try {
            PriorityQueue<Integer> queue = new PriorityQueue<>();

            Pathfinder.distToWalls = new double[Pathfinder.W][Pathfinder.H];
            for (int i = 0; i < Pathfinder.W; ++i) {
                for (int j = 0; j < Pathfinder.H; ++j) {
                    if (!Pathfinder.passable[i][j]) {
                        Pathfinder.distToWalls[i][j] = 0;
                        queue.add((i << Pathfinder.AUX) | j);
                    } else Pathfinder.distToWalls[i][j] = 100000000;
                }
            }

            while (queue.size() > 0) {
                int data = queue.poll();
                int myPosX = (data >> Pathfinder.AUX) & Pathfinder.base;
                int myPosY = data & Pathfinder.base;
                double dist = ((double) (data >> Pathfinder.AUX2)) / Pathfinder.distFactor;
                for (int i = 0; i < Pathfinder.X.length; ++i) {
                    int newPosX = myPosX + Pathfinder.X[i];
                    int newPosY = myPosY + Pathfinder.Y[i];
                    double newDist = dist + Pathfinder.dists[i];
                    int parsedDist = (int) Math.round(Pathfinder.distFactor * newDist);
                    if (newPosX >= Pathfinder.W || newPosX < 0 || newPosY >= Pathfinder.H || newPosY < 0) continue;
                    if (newDist < Pathfinder.distToWalls[newPosX][newPosY]) {
                        queue.add((((parsedDist << Pathfinder.AUX) | newPosX) << Pathfinder.AUX) | newPosY);
                        Pathfinder.distToWalls[newPosX][newPosY] = newDist;
                    }
                }
            }

            for (int i = 0; i < Pathfinder.W; ++i){
                for (int j = 0; j < Pathfinder.H; ++j){
                    Pathfinder.distToWalls[i][j] = Math.min(Pathfinder.distToWalls[i][j], i+1);
                    Pathfinder.distToWalls[i][j] = Math.min(Pathfinder.distToWalls[i][j], Pathfinder.W-i);
                    Pathfinder.distToWalls[i][j] = Math.min(Pathfinder.distToWalls[i][j], j+1);
                    Pathfinder.distToWalls[i][j] = Math.min(Pathfinder.distToWalls[i][j], Pathfinder.H-j);
                    //System.err.print(Math.min(distToWalls[i][j], 9));
                }
                //System.err.println();
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*------------------ EXPLORE GAME -----------------------*/


    public static void exploreInitMap(){
        createGrid();
        Explore.objectiveArea = new HashMap<>();
        getLocationEnemyBase();
    }

    static AuxMapLocation getAccessLocation(int xCenter, int yCenter){
        try {
            AuxMapLocation realCenter = new AuxMapLocation(xCenter, yCenter);
            //TODO check apart from passable passable from origin in earth
            if (realCenter.isPassable()) return realCenter;
            for (int i = 0; i < Const.allDirs.length; ++i) {
                AuxMapLocation fakeCenter = realCenter.add(i);
                if (fakeCenter.isPassable()) return fakeCenter;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void createGrid(){
        try {
            Explore.currentArea = new HashMap<>();
            Explore.exploreSizeX = Mapa.W / Explore.areaSize;
            Explore.exploreSizeY = Mapa.H / Explore.areaSize;
            double auxiliarX = (double) Mapa.W / Explore.exploreSizeX;
            double auxiliarY = (double) Mapa.H / Explore.exploreSizeY;
            Explore.exploreGrid = new double[Explore.exploreSizeX][Explore.exploreSizeY];
            Explore.locToArea = new int[Mapa.W][Mapa.H];
            Explore.areaToLocX = new int[Explore.exploreSizeX];
            Explore.areaToLocY = new int[Explore.exploreSizeY];
            for (int i = 0; i < Explore.exploreSizeX; ++i) {
                for (int j = 0; j < Explore.exploreSizeY; ++j) {
                    for (int x = (int) Math.floor(i * auxiliarX); x < Math.floor((i + 1) * auxiliarX); ++x) {
                        for (int y = (int) Math.floor(j * auxiliarY); y < Math.floor((j + 1) * auxiliarY); ++y) {
                            Explore.locToArea[x][y] = Utils.encode(i, j);
                        }
                    }
                    int xCenter = (int) Math.floor(i * auxiliarX) + Explore.areaSize / 2;
                    int yCenter = (int) Math.floor(j * auxiliarY) + Explore.areaSize / 2;
                    AuxMapLocation centerArea = getAccessLocation(xCenter, yCenter);
                    if (centerArea != null) {
                        Explore.areaToLocX[i] = centerArea.x;
                        Explore.areaToLocY[j] = centerArea.y;
                        continue;
                    }
                    Explore.exploreGrid[i][j] = Const.INF;
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static Integer locationToArea(AuxMapLocation loc){
        try {
            return Explore.locToArea[loc.x][loc.y];
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void addExploreGrid(Integer area, double value) {
        try {
            int x = Utils.decodeX(area);
            int y = Utils.decodeY(area);
            Explore.exploreGrid[x][y] += value;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void getLocationEnemyBase(){
        try {
            VecUnit initialUnits = Mapa.planetMap.getInitial_units();
            for (int i = 0; i < initialUnits.size(); ++i) {
                Unit possibleEnemy = initialUnits.get(i);
                if (possibleEnemy.team() == Utils.enemyTeam) {
                    Integer enemyArea = locationToArea((new AuxUnit(possibleEnemy, false).getMapLocation()));
                    addExploreGrid(enemyArea, Explore.enemyBaseValue);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*------------------ WORKER UTIL GAME -----------------------*/


    public static void workerUtilInitMap(){
        WorkerUtil.safe = true;
        WorkerUtil.totalKarboCollected = 0;
        WorkerUtil.workersCreated = 0;
        WorkerUtil.workerActions = new int[Mapa.W][Mapa.H];

        preComputeConnectivity();
        computeApproxMapValue();
    }

    static void preComputeConnectivity(){
        WorkerUtil.connectivityArray = new boolean[(1 << 9)-1];
        for (int i = 0; i < (1 << 9) - 1; ++i){
            WorkerUtil.connectivityArray[i] = computeConnectivity(i);
        }

        //for (int i = 0; i < 32; ++i) System.out.println(connectivityArray[i]);

    }

    static boolean computeConnectivity(int s){
        Queue<Integer> q = new LinkedList<>();
        for (int i = 0; i < 8; ++i) {
            if (((s >> i)&1) > 0){
                q.add(i);
                s = s & (~(1 << i));
                break;
            }
        }
        while (!q.isEmpty()){
            int t = q.poll();
            int x = 2;
            if (t%2 == 1) x = 1;
            for (int i = -x; i <= x; ++i){
                int newT = (t+8-i)%8;
                if (((s >> newT)&1) > 0){
                    q.add(newT);
                    s = s &(~(1 << newT));
                }
            }
        }
        return s == 0;
    }

    static void computeApproxMapValue() {
        try {
            WorkerUtil.approxMapValue = 0;
            VecUnit v = Mapa.planetMap.getInitial_units();

            ArrayList<AuxMapLocation> initialPositions = new ArrayList<>();

            for (int i = 0; i < v.size(); ++i) {
                Location loc = v.get(i).location();
                if (!loc.isInGarrison()) {
                    MapLocation mLoc = loc.mapLocation();
                    int x = mLoc.getX();
                    int y = mLoc.getY();
                    AuxMapLocation mloc = new AuxMapLocation(x, y);
                    initialPositions.add(mloc);
                    if (i % 2 == 0){
                        ++WorkerUtil.min_nb_workers;
                        ++WorkerUtil.workersCreated;
                    }
                }
            }

            for (int i = 0; i < Mapa.W; ++i) {
                for (int j = 0; j < Mapa.H; ++j) {
                    AuxMapLocation mloc = new AuxMapLocation(i, j);
                    double mindist = 1000000;
                    for (int t = 0; t < initialPositions.size(); ++t) {
                        mindist = Math.min(mindist, initialPositions.get(t).distanceBFSTo(mloc));
                    }
                    WorkerUtil.approxMapValue += Karbonite.karboMap[i][j] * Math.pow(WorkerUtil.decrease_rate, mindist);
                }
            }

            WorkerUtil.approxMapValue /= 2;

            WorkerUtil.min_nb_workers = (int)Math.max(WorkerUtil.min_nb_workers, WorkerUtil.approxMapValue / WorkerUtil.worker_value);

            //return approxMapValue;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*------------------ INIT GAME -----------------------*/

    static void initMap(){
        Mapa.planet = GC.gc.planet();
        PlanetMap planetMap = GC.gc.startingMap(Mapa.planet);
        Mapa.planetMap = planetMap;
        Mapa.W = (int) Mapa.planetMap.getWidth();
        Mapa.H = (int) Mapa.planetMap.getHeight();
        karboniteInitMap(); //ha d'anar despres de Mapa
        Units.initGame(); //ha d'anar despres de Mapa. No cal portar la funcio a wrapper, no utilitza api calls
        pathfinderInitMap(); //ha d'anar despres de Mapa i Karbonite
        exploreInitMap(); //ha d'anar despres de Mapa
        workerUtilInitMap(); //ha d'anar despres de Mapa i Pathfinder
    }


}
