

import bc.*;

import java.util.HashMap;

public class Worker {

    private static HashMap<Integer, WorkerData> mapa;
    private WorkerData data;
    private static Worker instance = null;
    private static GameController gc;
    private static ConstructionQueue queue;
    private static boolean DEBUG = false;
    private static Team myTeam;
    private static Planet myPlanet;

    // definicio dels target types, per ordre d'importancia
    private static final int TARGET_NONE = 0;
    private static final int TARGET_MINE = 1;
    private static final int TARGET_STRUCTURE = 2; //structure per reparar
    private static final int TARGET_BLUEPRINT = 3;
    private static final int TARGET_ROCKET = 4;

    // radi de busca de blueprints/structures
    private static final int SEARCH_RADIUS = 5;

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};


    public Worker(){

    }

    static Worker getInstance(){
        if (instance == null){
            instance = new Worker();
            mapa = new HashMap<>();
            gc = UnitManager.getInstance().gc;
            queue = Data.queue;
            myTeam = Data.myTeam;
            myPlanet = gc.planet();
        }
        return instance;
    }

    private void initMemory(Unit unit){
        int id = unit.id();
        mapa.computeIfAbsent(id, k -> new WorkerData(id));
        data = mapa.get(id);
        data.loc = unit.location().mapLocation();
        //System.out.println(unit.location() + "  " + unit.location().isInGarrison());
    }

    private Direction checkDanger(){
        //aixo es per quan tinguem matriu de perills etc. De moment fa com si no hi ha perill mai
        return null;
    }

    private void deleteMine(MapLocation location){
        Data.karboniteAt.remove(location);
    }

    private void resetTarget(){
        data.target_loc = null;
        data.target_type = TARGET_NONE;
        data.target_id = -1;
    }

    //Si esta en perill, fuig, si no, va al target
    private void move(Unit unit) {
        if (!gc.isMoveReady(data.id)) return;
        if (data.safest_direction != null) {
            gc.moveRobot(data.id, data.safest_direction);
            if (DEBUG)System.out.println("Worker " + data.id + " moves " + data.safest_direction + " (in danger)");
            return;
        }
        MapLocation dest;
        if (data.target_type == TARGET_NONE) dest = data.loc; //si no tinc target, vaig al meu lloc (fuig sol)
        else dest = data.target_loc;
        MovementManager.getInstance().moveTo(unit,dest);
    }

    private boolean checkNeededForRocket(){
        HashMap<Integer, MapLocation> mapa = Rocket.callsToRocket;
        MapLocation loc = mapa.get(data.id);
        if (loc != null){
            data.target_loc = loc;
            data.target_type = TARGET_ROCKET;
            data.target_id = -1;
            return true;
        }
        return false;
    }

    //Busca un blueprint per construir que estigui a prop del worker
    private boolean searchNearbyBlueprint(MapLocation location){
        for (Integer i : Data.structures){
            Unit u = Data.units.get(i);
            //UnitType type = u.unitType();
            //if (type != UnitType.Factory && type != UnitType.Rocket) continue;
            if (u.structureIsBuilt() != 0) continue; //si no es un blueprint, sino una estructura
            MapLocation loc = u.location().mapLocation();
            PathfinderNode node = Pathfinder.getInstance().getNode(location, loc);
            if (node.dist > SEARCH_RADIUS) continue; //si esta massa lluny (ha de fer volta)

            //actualitza el target!
            data.target_id = u.id();
            data.target_type = TARGET_BLUEPRINT;
            data.target_loc = loc;
            return true;
        }
        return false;
    }

    //Busca una structure per reparar que estigui a prop del worker
    private boolean searchNearbyStructure(MapLocation location){
        for (Integer i : Data.structures){
            Unit u = Data.units.get(i);
            //UnitType type = u.unitType();
            //if (type != UnitType.Factory && type != UnitType.Rocket) continue;
            if (u.structureIsBuilt() == 0) continue; //si es un blueprint, suda
            if (u.health() == u.maxHealth()) continue; //si no cal reparar-ho
            MapLocation loc = u.location().mapLocation();
            PathfinderNode node = Pathfinder.getInstance().getNode(location, loc);
            if (node.dist > SEARCH_RADIUS) continue; //si esta massa lluny (ha de fer volta)

            //actualitza el target!
            data.target_id = u.id();
            data.target_type = TARGET_STRUCTURE;
            data.target_loc = loc;
            return true;
        }
        return false;
    }

    //Busca la mina mes propera
    //Todo es pot optimitzar
    private void searchKarbonite(MapLocation location){
        long minDist = 1000000;
        MapLocation ans = null;
        for (HashMap.Entry<MapLocation, Integer> entry : Data.karboniteAt.entrySet()) {
            MapLocation mineLoc = entry.getKey();
            //int value = entry.getValue();
            long d = location.distanceSquaredTo(mineLoc);
            if (d < minDist){
                minDist = d;
                ans = mineLoc;
            }
        }

        if (ans == null) return;
        data.target_id = -1;
        data.target_type = TARGET_MINE;
        data.target_loc = ans;
    }

    private void updateTargetMars(){
        if (data.safest_direction != null) return; //no fa update si esta en perill
        int type = data.target_type;

        if (type != TARGET_NONE && type != TARGET_MINE) resetTarget();

        //Si te una mina de target, mira que encara hi quedi karbonite. Si no, reseteja target i elimina la mina de l'array
        if (type == TARGET_MINE && Data.karboniteAt.get(data.target_loc) == null) resetTarget();
        if (type == TARGET_MINE) return;
        searchKarbonite(data.loc);
    }

    private void updateTargetEarth(){
        if (data.safest_direction != null) return; //no fa update si esta en perill
        int type = data.target_type;

        //si un rocket el crida, fa return perque ja te el target mes important
        if (checkNeededForRocket()) return;
        if (type == TARGET_ROCKET) resetTarget();

        boolean found = false;

        // mira si hi ha una unit a la target location. Aixo es fa servir per si el target es structure/blueprint
        Unit target_unit = null;
        if (data.target_id != -1 && Data.allUnits.keySet().contains(data.target_id)) target_unit = Data.units.get(Data.allUnits.get(data.target_id));

        //si te un blueprint de target, mira si el blueprint ja esta construit. Si esta construit, reseteja target.
        if (type == TARGET_BLUEPRINT && target_unit != null) {
            if (target_unit.health() == target_unit.maxHealth()) resetTarget();
            UnitType targetType = target_unit.unitType();
            if (targetType != UnitType.Factory && targetType != UnitType.Rocket) resetTarget();
        }
        if (type == TARGET_BLUEPRINT) return;//no fas update si ja tens un blueprint perque es lo mes important
        found = searchNearbyBlueprint(data.loc);

        //Si te una structure de target, mira que la structure no estigui full vida. Si ho esta, reseteja
        if (type == TARGET_STRUCTURE && target_unit != null) {
            if (target_unit.health() == target_unit.maxHealth()) resetTarget();
            UnitType targetType = target_unit.unitType();
            if (targetType != UnitType.Factory && targetType != UnitType.Rocket) resetTarget();
        }
        if (found || type == TARGET_STRUCTURE) return;
        found = searchNearbyStructure(data.loc);

        //Si te una mina de target, mira que encara hi quedi karbonite. Si no, reseteja target i elimina la mina de l'array
        if (type == TARGET_MINE && Data.karboniteAt.get(data.target_loc) == null) resetTarget();
        if (found || type == TARGET_MINE) return;
        searchKarbonite(data.loc);
    }

    //cada torn, el worker mira si canvia de target
    private void updateTarget(){
        if (gc.planet() == Planet.Earth) updateTargetEarth();
        else updateTargetMars();
    }

    //busca de les 8 caselles adjacents l'estructura amb menys vida, o si no el blueprint amb mes
    private Unit findAdjacentStructure(MapLocation location){
        VecUnit v = gc.senseNearbyUnitsByTeam(location,3,myTeam); //radius * arrel(2)?
        Unit bestStr = null; //volem trobar l'estructura amb menys vida, o el blueprint amb mes
        int bestStrHP = 1000000;
        Unit bestBlp = null;
        int bestBlpRemainingHP = 1000000;
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);
            UnitType type = u.unitType();
            if (type != UnitType.Factory && type != UnitType.Rocket) continue;
            int hp = (int) u.health();
            int maxHp = (int) u.maxHealth();
            int missingHp = maxHp - hp;
            if (u.structureIsBuilt() == 0){
                //si es blueprint
                if (bestStr != null) continue; //si he trobat una structure, ja no fare un blueprint
                if (missingHp < bestBlpRemainingHP){
                    bestBlp = u;
                    bestBlpRemainingHP = missingHp;
                }
            }else{
                //si es structure
                if (hp < bestStrHP && missingHp > 0){
                    bestStr = u;
                    bestStrHP = hp;
                }
            }
        }
        if (bestStr != null) return bestStr;
        return bestBlp;
    }

    private boolean tryRepairBuild(Unit unit){
        if (gc.planet() != Planet.Earth) return false;
        Unit str = findAdjacentStructure(data.loc);
        if (str == null) return false;
        if (str.structureIsBuilt() == 0){
            //si es blueprint
            gc.build(data.id,str.id());
            if (str.health() + unit.workerBuildHealth() >= str.maxHealth()) resetTarget();
            if (DEBUG)System.out.println("Worker " + data.id + "  " + data.loc + " builds blueprint " + str.id());
        }else{
            //si es structure
            gc.repair(data.id,str.id());
            if (str.health() + unit.workerRepairHealth() >= str.maxHealth()) resetTarget();
            if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc +" repairs structure " + str.id() + ": " + str.health() + "/" + str.maxHealth());
        }
        return true;
    }

    private boolean shouldReplicate(Unit unit){
        if (data.safest_direction != null) return false;
        int search_radius = 8;
        int miningwork = 0; //turns of work
        int buildingwork = 0;
        //int workers_local = 0;
        //int workers_total = 0;
        int workers = 0;
        //final int MIN_WORK_LOCAL = 20;
        //final int MIN_WORK_TOTAL = 30;
        final int MIN_WORK = 30;
        MapLocation myPos = data.loc;
        for (HashMap.Entry<MapLocation,Integer> entry :Data.karboniteAt.entrySet()){
            MapLocation loc = entry.getKey();
            if (myPos.distanceSquaredTo(loc) > search_radius) continue;
            int karbonite = entry.getValue();
            if (karbonite > 0) miningwork += karbonite/unit.workerHarvestAmount() + 1;
        }
        VecUnit v = gc.senseNearbyUnitsByTeam(myPos,8, myTeam);
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);
            UnitType type = u.unitType();
            int missingHealth = (int) (u.maxHealth() - u.health());
            if ((type == UnitType.Rocket || type == UnitType.Factory) && missingHealth > 0){
                int workperturn;
                if (u.structureIsBuilt() == 0) workperturn = (int) unit.workerBuildHealth();
                else workperturn = (int) unit.workerRepairHealth();
                buildingwork += missingHealth/workperturn + 1;
            }
        }
        for (int i = 0; i < Data.units.size(); i++){
            Unit u = Data.units.get(i);
            UnitType type = u.unitType();
            if (type == UnitType.Worker) {
                //workers_total++;
                if (myPos.distanceSquaredTo(u.location().mapLocation()) <= search_radius) workers++;
            }
        }
        if (workers == 0) workers = 1;
        //if (workers_total == 0) workers_total = 1;
        double work_local = buildingwork / workers;
        double work_total = miningwork / workers;
        return (work_local + work_total) > MIN_WORK;
        //return (work_local > MIN_WORK_LOCAL) || (work_total > MIN_WORK_TOTAL);
    }

    private void tryReplicate(Unit unit){
        if (data.safest_direction != null) return;
        boolean should = shouldReplicate(unit);
        //System.out.println(unit.location().mapLocation() + " Should replicate? " + should);
        if (!queue.needsUnit(UnitType.Worker) && !should) return;
        for (int i = 0; i < 9; ++i){
            Direction d = allDirs[i];
            if (gc.canReplicate(data.id, d)) {
                gc.replicate(data.id,d);
                if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc +" replicates");
                queue.requestUnit(UnitType.Worker, false);
                return;
            }
        }
    }

    private boolean tryPlaceBlueprint(Unit unit){
        if (data.safest_direction != null) return false;
        UnitType type = null;
        if (Data.researchInfo.getLevel(UnitType.Rocket) > 0 && queue.needsUnit(UnitType.Rocket)) type = UnitType.Rocket;
        if (queue.needsUnit(UnitType.Factory)) type = UnitType.Factory;
        if (type == null) return false;
        System.out.println(Data.round + " type to build: " + type);
        boolean[] aux = new boolean[9];
        for (int i = 0; i < 9; ++i) aux[i] = true;
        Danger.computeDanger(data.loc, aux);
        for (int i = 0; i < 9; ++i){
            Direction d = allDirs[i];
            if (Danger.DPS[i] > 0) continue;
            if (!gc.canBlueprint(data.id, type, d)) continue;

            gc.blueprint(data.id, type, d);
            if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc +" places blueprint " + d);
            queue.requestUnit(type, false);
            return true;

        }
        return false;
    }

    private boolean tryMine(Unit unit){
        for(int i = 0; i < 9; ++i){
            Direction d = allDirs[i];
            MapLocation karboLoc = data.loc.add(d);
            if (!Utils.onTheMap(karboLoc,gc)) continue;
            int karboAmount = (int) gc.karboniteAt(karboLoc);
            if (karboAmount > 0){
                //System.out.println("AAAAA " + unit.location() + "  " + unit.location().isInGarrison());
                //System.out.println("Unit location, dir: " + data.loc + "   " + d);
                gc.harvest(data.id, d);
                if (karboAmount <= unit.workerHarvestAmount()) resetTarget();
                if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc + " harvests karbonite " + d);
                return true;
            }
        }
        return false;
    }

    //Aqui decideix si construeix/mina/etc
    private boolean doAction(Unit unit){
        if (unit.workerHasActed() > 0) return true;
        if (tryRepairBuild(unit)) return true;
        if (tryPlaceBlueprint(unit)) return true;
        if (tryMine(unit)) return true;
        return false;
    }

    void play(Unit unit){
        try {
            //System.out.println("Worker " + data.id + " start round " + gc.round());
            initMemory(unit);
            if (unit.location().isInGarrison() || unit.location().isInSpace()) return;

            int id = data.id;
            data.safest_direction = checkDanger();
            boolean repl = doAction(unit);
            tryReplicate(unit);
            updateTarget();
            move(unit);
            if (!repl) doAction(unit);

            if (DEBUG) System.out.println("Worker " + id + "  " + data.loc + " has target " + data.target_loc + ", " + data.target_type);
            //System.out.println("Worker " + id + " end round " + gc.round());
        }catch(Exception e){
            System.out.println("CUIDADUUU!!!! Excepcio a worker, probablement perque un worker de dintre un garrison ha intentat fer una accio");
            e.printStackTrace();
        }
    }
}
