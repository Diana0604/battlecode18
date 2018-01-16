
import bc.*;

import java.util.HashMap;

public class Worker {

    private static HashMap<Integer, WorkerData> mapa;
    private WorkerData data;
    private static Worker instance = null;
    private static GameController gc;
    private static ConstructionQueue queue;
    private static boolean DEBUG = false;

    // definicio dels target types, per ordre d'importancia
    private static final int TARGET_NONE = 0;
    private static final int TARGET_MINE = 1;
    private static final int TARGET_STRUCTURE = 2; //structure per reparar
    private static final int TARGET_BLUEPRINT = 3;

    public Worker(){
        mapa = new HashMap<>();
    }

    static Worker getInstance(){
        if (instance == null){
            instance = new Worker();
            gc = UnitManager.getInstance().gc;
            queue = UnitManager.queue;
        }
        return instance;
    }

    private Direction checkDanger(){
        //aixo es per quan tinguem matriu de perills etc. De moment fa com si no hi ha perill mai
        return null;
    }

    private void deleteMineFromArray(int index){
        UnitManager.Xmines.set(index, -1);
        UnitManager.Ymines.set(index, -1);
        UnitManager.Qmines.set(index, -1);
    }

    private void resetTarget(){
        data.target_loc = null;
        data.target_type = TARGET_NONE;
        data.target_id = -1;
        data.karbonite_index = -1;
    }

    //Si esta en perill, fuig, si no, va al target
    private void move(Unit unit) {
        if (!gc.isMoveReady(unit.id())) return;
        if (data.safest_direction != null) {
            gc.moveRobot(unit.id(), data.safest_direction);
            if (DEBUG)System.out.println("Worker " + unit.id() + " moves " + data.safest_direction + " (in danger)");
            return;
        }
        if (data.target_type == TARGET_NONE) return;
        MovementManager.getInstance().moveTo(unit,data.target_loc);
    }


    //Busca un blueprint per construir que estigui a prop del worker
    private boolean searchNearbyBlueprint(MapLocation location){
        final int SEARCH_RADIUS = 5;
        VecUnit v = gc.senseNearbyUnitsByTeam(location,SEARCH_RADIUS*SEARCH_RADIUS,gc.team()); //radius * arrel(2)?
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);

            if (u.unitType() != UnitType.Factory && u.unitType() != UnitType.Rocket) continue;
            if (u.structureIsBuilt() != 0) continue; //si no es un blueprint, sino una estructura
            PathfinderNode node = Pathfinder.getInstance().getNode(location, u.location().mapLocation());
            if (node.dist > SEARCH_RADIUS) continue; //si esta massa lluny (ha de fer volta)

            //actualitza el target!
            data.target_id = u.id();
            data.target_type = TARGET_BLUEPRINT;
            data.target_loc = u.location().mapLocation();
            return true;
        }
        return false;
    }

    //Busca una structure per reparar que estigui a prop del worker
    private boolean searchNearbyStructure(MapLocation location){
        final int SEARCH_RADIUS = 5;
        VecUnit v = gc.senseNearbyUnitsByTeam(location,SEARCH_RADIUS*SEARCH_RADIUS,gc.team()); //radius * arrel(2)?
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);

            if (u.unitType() != UnitType.Factory && u.unitType() != UnitType.Rocket) continue;
            if (u.structureIsBuilt() == 0) continue; //si es un blueprint, suda
            if (u.health() == u.maxHealth()) continue; //si no cal reparar-ho
            PathfinderNode node = Pathfinder.getInstance().getNode(location, u.location().mapLocation());
            if (node.dist > SEARCH_RADIUS) continue; //si esta massa lluny (ha de fer volta)

            //actualitza el target!
            data.target_id = u.id();
            data.target_type = TARGET_STRUCTURE;
            data.target_loc = u.location().mapLocation();
            return true;
        }
        return false;
    }

    //Busca la mina mes propera
    private void searchKarbonite(MapLocation location){
        long minDist = 1000000;
        MapLocation ans = null;
        int index = -2;
        for (int i = 0; i < UnitManager.Xmines.size(); ++i){
            if (UnitManager.Qmines.get(i) == -1) continue;
            int x = UnitManager.Xmines.get(i);
            int y = UnitManager.Ymines.get(i);
            MapLocation mineLoc = new MapLocation(gc.planet(), x, y);
            if (gc.canSenseLocation(mineLoc) && gc.karboniteAt(mineLoc) == 0){
                deleteMineFromArray(i);
                continue;
            }
            long d = location.distanceSquaredTo(mineLoc);
            if (d < minDist){
                minDist = d;
                ans = mineLoc;
                index = i;
            }
        }
        if (ans == null) return;
        data.target_id = -1;
        data.target_type = TARGET_MINE;
        data.target_loc = ans;
        data.karbonite_index = index;
    }

    private void updateTarget(Unit unit){
        if (data.safest_direction != null) return; //no fas update si estas en perill
        int type = data.target_type;
        if (type == TARGET_BLUEPRINT) {
            Unit target_unit = gc.unit(data.target_id);
            if (target_unit != null && target_unit.health() == target_unit.maxHealth()) resetTarget();
        }
        if (type == TARGET_BLUEPRINT) return;//no fas update si ja tens un blueprint perque es lo mes important
        boolean found = searchNearbyBlueprint(unit.location().mapLocation());
        if (type == TARGET_STRUCTURE) {
            Unit target_unit = gc.unit(data.target_id);
            if (target_unit != null && target_unit.health() == target_unit.maxHealth()) resetTarget();
        }
        if (found || type == TARGET_STRUCTURE) return;
        found = searchNearbyStructure(unit.location().mapLocation());
        if (type == TARGET_MINE){
            if (gc.canSenseLocation(data.target_loc) && gc.karboniteAt(data.target_loc) == 0) {
                deleteMineFromArray(data.karbonite_index);
                resetTarget();
            }
        }
        if (found || type == TARGET_MINE) return;
        searchKarbonite(unit.location().mapLocation());
    }

    //busca de les 8 caselles adjacents l'estructura amb menys vida, o si no el blueprint amb mes
    private Unit findAdjacentStructure(MapLocation location){
        VecUnit v = gc.senseNearbyUnitsByTeam(location,3,gc.team()); //radius * arrel(2)?
        Unit bestStr = null; //volem trobar l'estructura amb menys vida, o el blueprint amb mes
        int bestStrHP = 1000000;
        Unit bestBlp = null;
        int bestBlpRemainingHP = 1000000;
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);
            if (u.unitType() != UnitType.Factory && u.unitType() != UnitType.Rocket) continue;
            if (u.structureIsBuilt() == 0){
                //si es blueprint
                if (bestStr != null) continue; //si he trobat una structure, ja no fare un blueprint
                if (u.maxHealth() - u.health() < bestBlpRemainingHP){
                    bestBlp = u;
                    bestBlpRemainingHP = (int) (u.maxHealth() - u.health());
                }
            }else{
                //si es structure
                if (u.health() < bestStrHP && u.health() < u.maxHealth()){
                    bestStr = u;
                    bestStrHP = (int) u.health();
                }
            }
        }
        if (bestStr != null) return bestStr;
        return bestBlp;
    }

    private boolean tryRepairBuild(Unit unit){
        if (!gc.isAttackReady(unit.id())) return false; //no tinc clar que attack sigui lo correcte aqui pero idk
        Unit str = findAdjacentStructure(unit.location().mapLocation());
        if (str == null) return false;
        if (str.structureIsBuilt() == 0){
            //si es blueprint
            gc.build(unit.id(),str.id());
            if (str.health() + unit.workerBuildHealth() >= str.maxHealth()) resetTarget();
            if (DEBUG)System.out.println("Worker " + unit.id() + "  " + unit.location().mapLocation() + " builds blueprint " + str.id());
        }else{
            //si es structure
            gc.repair(unit.id(),str.id());
            if (str.health() + unit.workerRepairHealth() >= str.maxHealth()) resetTarget();
            if (DEBUG)System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() +" repairs structure " + str.id() + ": " + str.health() + "/" + str.maxHealth());
        }
        return true;
    }

    private boolean shouldReplicate(Unit unit){
        if (data.safest_direction != null) return false;
        int search_radius = 2;
        int work = 0; //turns of work in the area (mining+building+repairing)
        int workers = 0;
        final int WORKPERWORKER = 20;
        MapLocation myPos = unit.location().mapLocation();
        for (int i = -search_radius; i <= search_radius; i++) {
            for (int j = -search_radius; j <= search_radius; j++) {
                MapLocation loc = myPos.translate(i, j);
                if (!Utils.onTheMap(loc, gc)) continue;
                int karbonite = (int) gc.karboniteAt(loc);
                if (karbonite > 0) work += karbonite / unit.workerHarvestAmount() + 1;
            }
        }
        VecUnit v = gc.senseNearbyUnitsByTeam(myPos,8, unit.team());
        for (int i = 0; i < v.size(); i++){
            Unit u = v.get(i);
            UnitType type = u.unitType();
            if (type == UnitType.Worker) workers++;
            if ((type == UnitType.Rocket || type == UnitType.Factory) && u.health() < u.maxHealth()){
                int workperturn;
                if (u.structureIsBuilt() == 0) workperturn = (int) unit.workerBuildHealth();
                else workperturn = (int) unit.workerRepairHealth();
                work += (u.maxHealth() - u.health())/workperturn + 1;
            }
        }
        return work / workers > WORKPERWORKER;
    }

    private boolean tryReplicate(Unit unit){
        if (!gc.isAttackReady(unit.id())) return false; //no tinc clar que attack sigui lo correcte aqui pero idk
        if (data.safest_direction != null) return false;
        if (!queue.needsUnit(UnitType.Worker) && !shouldReplicate(unit)) return false;
        for (Direction d: Direction.values()){
            if (gc.canReplicate(unit.id(), d)) {
                gc.replicate(unit.id(),d);
                if (DEBUG)System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() +" replicates");
                queue.requestUnit(UnitType.Worker, false);
                return true;
            }
        }
        return false;
    }

    private boolean tryPlaceBlueprint(Unit unit){
        if (!gc.isAttackReady(unit.id())) return false; //no tinc clar que attack sigui lo correcte aqui pero idk
        if (data.safest_direction != null) return false;
        UnitType type = null;
        if (queue.needsUnit(UnitType.Rocket)) type = UnitType.Rocket;
        if (queue.needsUnit(UnitType.Factory)) type = UnitType.Factory;
        if (type == null) return false;
        for (Direction d: Direction.values()){
            if (gc.canBlueprint(unit.id(), type, d)) {
                gc.blueprint(unit.id(), type, d);
                if (DEBUG)System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() +" places blueprint " + d);
                queue.requestUnit(type, false);
                return true;
            }
        }
        return false;
    }

    private void tryMine(Unit unit){
        if (!gc.isAttackReady(unit.id())) return; //no tinc clar que attack sigui lo correcte aqui pero idk
        for(Direction d: Direction.values()){
            MapLocation karboLoc = unit.location().mapLocation().add(d);
            if (!Utils.onTheMap(karboLoc,gc)) continue;
            if (gc.karboniteAt(karboLoc) > 0){
                //System.out.println("Unit location, dir: " + unit.location().mapLocation() + "   " + d);
                gc.harvest(unit.id(), d);
                if (gc.karboniteAt(karboLoc) <= unit.workerHarvestAmount()) resetTarget();
                if (DEBUG)System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() + " harvests karbonite " + d);
                return;
            }
        }
    }

    //Aqui decideix si construeix/mina/etc
    private void doAction(Unit unit){
        //teoricament el hasMoved no cal
        boolean hasMoved = tryReplicate(unit);
        if (!hasMoved) hasMoved = tryRepairBuild(unit);
        if (!hasMoved) hasMoved = tryPlaceBlueprint(unit);
        if (!hasMoved) tryMine(unit);
    }

    void play(Unit unit){
        System.out.println("Worker " + unit.id() + " start round " + gc.round());
        mapa.computeIfAbsent(unit.id(), k -> new WorkerData(unit.id()));
        data = mapa.get(unit.id());
        System.out.println(unit.id() + " ok 1");
        data.safest_direction = checkDanger();
        System.out.println(unit.id() + " ok 2");
        updateTarget(unit);
        System.out.println(unit.id() + " ok 3");
        if (DEBUG)System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() + " has target " + data.target_loc + ", " + data.target_type);
        doAction(unit);
        System.out.println(unit.id() + " ok 4");
        move(unit);
        System.out.println("Worker " + unit.id() + " end round " + gc.round());
    }
}
