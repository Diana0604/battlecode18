import bc.MapLocation;
import bc.UnitType;
import bc.VecUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Pau on 23/01/2018.
 */
public class Units {
    static AuxMapLocation mapCenter;
    static long maxRadius;
    static boolean firstRocket = true;
    static int rocketsLaunched = 0;
    static HashMap<Integer, Integer> allUnits; //allUnits.get(id) = index de myUnits
    static HashMap<UnitType, Integer> unitTypeCount;
    static HashSet<Integer> structures;
    static HashSet<Integer> rockets;
    static HashSet<Integer> workers;
    static HashSet<Integer> newUnits;
    static HashMap<Integer, Integer> blueprintsToBuild;
    static HashMap<Integer, Integer> structuresToRepair;
    static int[][] unitMap;
    static ArrayList<AuxUnit> myUnits; //myUnits.get(i) retorna una unit random meva
    static ArrayList<AuxUnit> enemies;
    static int lastRoundEnemySeen;
    static int lastRoundUnder100Karbo;
    static int healingPower;
    static int buildingPower;
    static int repairingPower;
    static int harvestingPower;
    private static int mageDamage;
    static boolean canBlink = false;
    static boolean canOverCharge = false;
    static int rocketCapacity;
    static boolean canBuildRockets;
    static HashSet<Integer> newOccupiedPositions;
    static ConstructionQueue queue; //delete?

    public static void initGame(){
        lastRoundEnemySeen = 1;
        lastRoundUnder100Karbo = 1;
        canBuildRockets = false;
        mapCenter = new AuxMapLocation(Mapa.W / 2 + 1, Mapa.H / 2 + 1);
        maxRadius = mapCenter.distanceSquaredTo(new AuxMapLocation(0, 0));
        allUnits = new HashMap<>();
        structures = new HashSet<>();
        rockets = new HashSet<>();
        workers = new HashSet<>();
        blueprintsToBuild = new HashMap<>();
        structuresToRepair = new HashMap<>();
        unitTypeCount = new HashMap<>();
        queue = new ConstructionQueue();
    }

    public static void initTurn(){
        newOccupiedPositions = new HashSet<>();
        allUnits = new HashMap<>();
        myUnits = new ArrayList<>();
        enemies = new ArrayList<>();
        rockets = new HashSet<>();
        structures = new HashSet<>();
        workers = new HashSet<>();
        unitTypeCount = new HashMap<>();
        unitMap = new int[Mapa.W][Mapa.H];
        if (Utils.karbonite < 100) lastRoundUnder100Karbo = Utils.round;
        updateUnitPowers();
        updateEnemyUnits();
        updateMyUnits();
        updateUnitTypeCount();
        updateStructures();
        updateWorkers();
        updateBlueprintsToBuild();
        updateStructuresToRepair();
    }

    static void updateUnitPowers(){
        int workerLevel = (int) Research.researchInfo.getLevel(UnitType.Worker);
        int healerLevel = (int) Research.researchInfo.getLevel(UnitType.Healer);
        int rocketLevel = (int) Research.researchInfo.getLevel(UnitType.Rocket);
        int mageLevel = (int) Research.researchInfo.getLevel(UnitType.Mage);
        buildingPower = Const.buildingPowers[workerLevel];
        repairingPower = Const.repairingPowers[workerLevel];
        harvestingPower = Const.harvestingPowers[workerLevel];
        healingPower = Const.healingPowers[healerLevel];
        mageDamage = Const.mageDamages[mageLevel];
        if (mageLevel >= 4) canBlink = true;
        if (healerLevel >= 3) canOverCharge = true;
        rocketCapacity = Const.rocketCapacities[rocketLevel];
        if (rocketLevel > 0) canBuildRockets = true;
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

    static void updateEnemyUnits(){
        //check enemy units
        MapLocation center = new MapLocation(Mapa.planet, mapCenter.x, mapCenter.y);
        VecUnit enemyUnits = GC.gc.senseNearbyUnitsByTeam(center, maxRadius, Utils.enemyTeam);

        int size = (int) enemyUnits.size();
        enemies = new ArrayList<>();
        for (int i = 0; i < size; ++i) enemies.add(new AuxUnit(enemyUnits.get(i), false));


        for (int i = 0; i < enemies.size(); ++i) {
            AuxUnit enemy = enemies.get(i);
            //allUnits.put(enemy.getID(), i);
            unitMap[enemy.getX()][enemy.getY()] = -(i + 1);
        }
        if (enemyUnits.size() != 0) lastRoundEnemySeen = Utils.round;
    }

    static void updateMyUnits(){
        VecUnit vecMyUnits = GC.gc.myUnits();

        int size = (int) vecMyUnits.size();
        ArrayList<AuxUnit> sortedUnits = new ArrayList<>();
        for (int i = 0; i < size; ++i) sortedUnits.add(new AuxUnit(vecMyUnits.get(i), true));
        //First to last: Mage - Ranger - Healer - Knight - Worker - Rocket - Factory
        sortedUnits.sort((a, b) -> priority(a) < priority(b) ? -1 : priority(a) == priority(b) ? 0 : 1);
        myUnits = sortedUnits;

        for (int i = 0; i < myUnits.size(); ++i) {
            AuxUnit unit = myUnits.get(i);
            allUnits.put(unit.getID(), i);
            if (!unit.isInGarrison()) unitMap[unit.getX()][unit.getY()] = i + 1;
        }
    }

    static void updateStructures(){
        for (int i = 0; i < myUnits.size(); i++) {
            UnitType type = myUnits.get(i).getType();
            if (type == UnitType.Rocket || type == UnitType.Factory) structures.add(i);
            if (type == UnitType.Rocket) rockets.add(i);
        }
    }

    static void updateUnitTypeCount() {
        try {
            int rangers = 0;
            int healers = 0;
            int workers = 0;
            int mages = 0;
            int knights = 0;
            int factories = 0;
            int rockets = 0;
            for (AuxUnit u: myUnits) {
                UnitType type = u.getType();
                if (type == UnitType.Factory) factories++;
                else if (type == UnitType.Rocket) rockets++;
                else if (type == UnitType.Worker) ++workers;
                else if (type == UnitType.Ranger) ++rangers;
                else if (type == UnitType.Healer) ++healers;
                else if (type == UnitType.Mage) ++mages;
                else if (type == UnitType.Knight) ++knights;
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

    static void updateWorkers(){
        workers = new HashSet<>();
        for (int i = 0; i < myUnits.size(); i++) {
            if (myUnits.get(i).getType() == UnitType.Worker) workers.add(i);
        }
    }

    static void updateBlueprintsToBuild(){
        final int MAX_WORKERS_TO_CALL = 6;
        blueprintsToBuild = new HashMap<>();

        for (int index : structures) {
            //Per cada blueprint, crida els 6 workers mes propers a construir-lo
            AuxUnit bp = myUnits.get(index);
            if (bp.isBuilt()) continue;
            if (bp.isMaxHealth()) continue;

            ArrayList<Pair> sorted = new ArrayList<>();
            for (int index2 : workers) {
                AuxUnit worker = myUnits.get(index2);
                AuxMapLocation workerLoc = worker.getMapLocation();
                if (workerLoc == null) continue;
                Pair p = new Pair(bp.getMapLocation().distanceSquaredTo(worker.getMapLocation()), worker);
                sorted.add(p);
            }
            sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
            int workersToCall =  Math.min(MAX_WORKERS_TO_CALL, sorted.size() - 1);
            if (workersToCall == 0) workersToCall = 1;
            List<Pair> cut = sorted.subList(0, workersToCall);
            for (Pair p : cut) {
                int key = p.unit.getID();
                int value = bp.getID();
                if (!blueprintsToBuild.containsKey(key)) blueprintsToBuild.put(key, value);
            }
        }
    }

    static void updateStructuresToRepair(){
        structuresToRepair = new HashMap<>();
        final int MAX_WORKERS_TO_CALL = 3;

        for (int index : structures) {
            //Per cada blueprint, crida els 8 workers mes propers a construir-lo
            AuxUnit s = myUnits.get(index);
            if (!s.isBuilt()) continue;
            if (s.isMaxHealth()) continue;

            ArrayList<Pair> sorted = new ArrayList<>();
            for (int index2 : workers) {
                AuxUnit worker = myUnits.get(index2);
                //no fiquem workers si ja son cridats per un blueprint
                if (blueprintsToBuild.containsKey(worker.getID())) continue;
                AuxMapLocation workerLoc = worker.getMapLocation();
                if (workerLoc == null) continue;
                Pair p = new Pair(s.getMapLocation().distanceSquaredTo(worker.getMapLocation()), worker);
                sorted.add(p);
            }
            sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
            int workersToCall =  Math.min(MAX_WORKERS_TO_CALL, sorted.size() - 1);
            if (workersToCall == 0) workersToCall = 1;
            if (workersToCall <= 0) workersToCall = 0;
            List<Pair> cut = sorted.subList(0, workersToCall);
            for (Pair p : cut) {
                int key = p.unit.getID();
                int value = s.getID();
                if (!structuresToRepair.containsKey(key)) structuresToRepair.put(key, value);
            }
        }
    }

    private static class Pair {
        double dist;
        AuxUnit unit;

        Pair(double dist, AuxUnit unit){
            this.dist = dist;
            this.unit = unit;
        }
    }

    public static AuxUnit getUnitByID(int id){
        if (!allUnits.containsKey(id)){
            System.out.println("ERROR: a Units.getUnitByID, id no trobada: " + id);
        }
        int index = allUnits.get(id);
        return myUnits.get(index);
    }

    public static int getDamage(UnitType type){
        switch(type){
            case Ranger: return Const.rangerDamage;
            case Knight: return Const.knightDamage;
            case Mage: return mageDamage;
            default: return 0;
        }
    }

    static double getAttackCooldown(UnitType type){
        switch(type){
            case Ranger: return Const.rangerAttackCooldown;
            case Knight: return Const.knightAttackCooldown;
            case Mage: return Const.mageAttackCooldown;
            default: return 1;
        }
    }

    static int getAttackRange(UnitType type){
        switch(type){
            case Ranger: return Const.rangerAttackRange;
            case Knight: return Const.knightAttackRange;
            case Mage: return Const.mageAttackRange;
            default: return 0;
        }
    }

    static int getAttackRangeSafe(UnitType type){
        switch(type){
            case Ranger: return Const.rangerSafeAttackRange;
            case Knight: return Const.knightSafeAttackRange;
            case Mage: return Const.mageSafeAttackRange;
            default: return 0;
        }
    }

    static int getAttackRangeLong(UnitType type){
        switch(type){
            case Ranger: return Const.rangerLongAttackRange;
            case Knight: return Const.knightLongAttackRange;
            case Mage: return Const.mageLongAttackRange;
            default: return 0;
        }
    }

    static int getAttackRangeExtra(UnitType type){
        switch(type){
            case Ranger: return Const.rangerExtraAttackRange;
            case Knight: return Const.knightExtraAttackRange;
            case Mage: return Const.mageExtraAttackRange;
            default: return 0;
        }
    }

    static int getCost(UnitType type){
        switch(type){
            case Factory:
                return Const.factoryCost;
            case Worker:
                return Const.workerCost;
            case Ranger:
                return Const.rangerCost;
            case Knight:
                return Const.knightCost;
            case Mage:
                return Const.mageCost;
            case Rocket:
                return Const.rocketCost;
            case Healer:
                return Const.healerCost;
            default:
                return -1;
        }
    }

    static int getMaxHealth(UnitType type){
        switch(type){
            case Factory:
                return Const.factoryMaxHealth;
            case Worker:
                return Const.workerMaxHealth;
            case Ranger:
                return Const.rangerMaxHealth;
            case Knight:
                return Const.knightMaxHealth;
            case Mage:
                return Const.mageMaxHealth;
            case Rocket:
                return Const.rocketMaxHealth;
            case Healer:
                return Const.healerMaxHealth;
            default:
                return 1;
        }
    }


    static int getIndex(UnitType type){
        switch(type){
            case Factory:
                return 5;
            case Worker:
                return 0;
            case Ranger:
                return 2;
            case Knight:
                return 1;
            case Mage:
                return 3;
            case Rocket:
                return 6;
            case Healer:
                return 4;
            default:
                return 0;
        }
    }
}
