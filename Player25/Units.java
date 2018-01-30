import bc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Units {
    static AuxMapLocation mapCenter;
    static long maxRadius;
    static HashMap<Integer, Integer> allUnits; //allUnits.get(id) = index de myUnits
    static HashMap<UnitType, Integer> unitTypeCount;
    static HashSet<Integer> structures;
    static HashSet<Integer> blueprints;
    static HashSet<Integer> rockets;
    static HashSet<Integer> factories;
    static HashSet<Integer> robots;
    static HashSet<Integer> workers;
    static HashSet<Integer> healers;
    static HashSet<Integer> rangers;
    static HashSet<Integer> mages;
    static HashSet<Integer> knights;
    static int[][] unitMap;
    static ArrayList<AuxUnit> myUnits; //myUnits.get(i) retorna una unit random meva
    static ArrayList<AuxUnit> enemies;
    static int healingPower;
    static int buildingPower;
    static int repairingPower;
    static int harvestingPower;
    static int knightBlock;
    private static int mageDamage;
    static boolean canBlink = false;
    static boolean canOverCharge = false;
    static int rocketCapacity;
    static HashSet<Integer> newOccupiedPositions;


    public static void initTurn(){
        try {
            declareArrays();
            updateUnitPowers();
            updateEnemyUnits();
            updateMyUnits();
            updateUnitTypeCount();
            updateStructures();
            updateWorkers();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void declareArrays(){
        try {
            allUnits = new HashMap<>();
            myUnits = new ArrayList<>();
            enemies = new ArrayList<>();
            unitTypeCount = new HashMap<>();

            workers = new HashSet<>();
            rangers = new HashSet<>();
            healers = new HashSet<>();
            knights = new HashSet<>();
            mages = new HashSet<>();
            robots = new HashSet<>();
            factories = new HashSet<>();
            rockets = new HashSet<>();
            blueprints = new HashSet<>();
            structures = new HashSet<>();

            Build.blueprintsToBuild = new HashMap<>();
            Build.structuresToRepair = new HashMap<>();

            newOccupiedPositions = new HashSet<>();

            unitMap = new int[Mapa.W][Mapa.H];
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateUnitPowers(){
        try {
            ResearchInfo researchInfo = Research.researchInfo;
            int workerLevel = (int) researchInfo.getLevel(UnitType.Worker);
            int healerLevel = (int) researchInfo.getLevel(UnitType.Healer);
            int rocketLevel = (int) researchInfo.getLevel(UnitType.Rocket);
            int mageLevel = (int) researchInfo.getLevel(UnitType.Mage);
            int knightLevel = (int) researchInfo.getLevel(UnitType.Knight);
            buildingPower = Const.buildingPowers[workerLevel];
            repairingPower = Const.repairingPowers[workerLevel];
            harvestingPower = Const.harvestingPowers[workerLevel];
            healingPower = Const.healingPowers[healerLevel];
            mageDamage = Const.mageDamages[mageLevel];
            knightBlock = Const.knightBlocks[knightLevel];
            if (mageLevel >= 4) canBlink = true;
            if (healerLevel >= 3) canOverCharge = true;
            rocketCapacity = Const.rocketCapacities[rocketLevel];
            if (rocketLevel > 0) Build.canBuildRockets = true;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static double priority(AuxUnit u){
        try {
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
                    return 4 + Worker.hasTarget(u);
                case Rocket:
                    return 5;
                case Factory:
                    return 6;
                default:
                    return 10;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static void updateEnemyUnits(){
        try {
            //check enemy units
            MapLocation center = new MapLocation(Mapa.planet, mapCenter.x, mapCenter.y);
            VecUnit enemyUnits = GC.gc.senseNearbyUnitsByTeam(center, maxRadius, Utils.enemyTeam);

            int size = (int) enemyUnits.size();
            for (int i = 0; i < size; ++i) enemies.add(new AuxUnit(enemyUnits.get(i)));

            enemyUnits.delete();

            for (int i = 0; i < enemies.size(); ++i) {
                AuxUnit enemy = enemies.get(i);
                if (enemy.getType() != UnitType.Worker) WorkerUtil.safe = false;
                unitMap[enemy.getX()][enemy.getY()] = -(i + 1);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateMyUnits(){
        try {
            VecUnit vecMyUnits = GC.gc.myUnits();

            int size = (int) vecMyUnits.size();
            for (int i = 0; i < size; ++i) {
                Unit u = vecMyUnits.get(i);
                if (u.location().isInSpace()) continue;
                myUnits.add(new AuxUnit(u));
            }

            vecMyUnits.delete();
            //First to last: Mage - Ranger - Healer - Knight - Worker - Rocket - Factory
            myUnits.sort((a, b) -> priority(a) < priority(b) ? -1 : priority(a) == priority(b) ? 0 : 1);

            for (int i = 0; i < myUnits.size(); ++i) {
                AuxUnit unit = myUnits.get(i);
                allUnits.put(unit.getID(), i);
                if (!unit.isInGarrison()) unitMap[unit.getX()][unit.getY()] = i + 1;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateStructures(){
        try {
            for (int i = 0; i < myUnits.size(); i++) {
                AuxUnit unit = myUnits.get(i);
                UnitType type = unit.getType();
                if (unit.isStructure()) {
                    structures.add(i);
                    if (type == UnitType.Rocket) rockets.add(i);
                    if (type == UnitType.Factory){
                        factories.add(i);
                        if (unit.isBuilt()) Build.firstFactory = true;
                    }
                    if (unit.isBlueprint()) blueprints.add(i);
                }else{
                    robots.add(i);
                    if (type == UnitType.Worker) workers.add(i);
                    if (type == UnitType.Knight) knights.add(i);
                    if (type == UnitType.Ranger) rangers.add(i);
                    if (type == UnitType.Healer) healers.add(i);
                    if (type == UnitType.Mage) mages.add(i);

                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateUnitTypeCount() {
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
                if (type == UnitType.Factory){
                    factories++;
                    if (u.isBuilt()) {
                        type = Wrapper.getBuildingUnit(u);
                        if (type != null){
                            switch(type){
                                case Worker: ++workers;
                                case Mage: ++mages;
                                case Healer: ++healers;
                                case Knight: ++knights;
                            }
                        }
                    }
                }
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

    private static void updateWorkers(){
        try {
            for (int i = 0; i < myUnits.size(); i++) {
                if (myUnits.get(i).getType() == UnitType.Worker) workers.add(i);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    static AuxUnit getUnitByID(int id){
        try {
            if (!allUnits.containsKey(id)){
                System.out.println("ERROR: a Units.getUnitByID, id no trobada: " + id);
            }
            int index = allUnits.get(id);
            return myUnits.get(index);
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static int getDamage(UnitType type){
        try {
            switch(type){
                case Ranger: return Const.rangerDamage;
                case Knight: return Const.knightDamage;
                case Mage: return mageDamage;
                default: return 0;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static double getAttackCooldown(UnitType type){
        try {
            switch(type){
                case Ranger: return Const.rangerAttackCooldown;
                case Knight: return Const.knightAttackCooldown;
                case Mage: return Const.mageAttackCooldown;
                default: return 1;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static int getAttackRange(UnitType type){
        try {
            switch(type){
                case Ranger: return Const.rangerAttackRange;
                case Knight: return Const.knightAttackRange;
                case Mage: return Const.mageAttackRange;
                default: return 0;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static int getAttackRangeSafe(UnitType type){
        try {
            switch(type){
                case Ranger: return Const.rangerSafeAttackRange;
                case Knight: return Const.knightSafeAttackRange;
                case Mage: return Const.mageSafeAttackRange;
                default: return 0;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static int getAttackRangeLong(UnitType type){
        try {
            switch(type){
                case Ranger: return Const.rangerLongAttackRange;
                case Knight: return Const.knightLongAttackRange;
                case Mage: return Const.mageLongAttackRange;
                default: return 0;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static int getAttackRangeExtra(UnitType type){
        try {
            switch(type){
                case Ranger: return Const.rangerExtraAttackRange;
                case Knight: return Const.knightExtraAttackRange;
                case Mage: return Const.mageExtraAttackRange;
                default: return 0;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static int getCost(UnitType type){
        try {
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
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static int getMaxHealth(UnitType type){
        try {
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
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    static int getIndex(UnitType type){
        try {
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
        }catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
