import bc.*;

import java.util.HashMap;

public class Worker {

    private static HashMap<Integer, WorkerData> mapa;
    private WorkerData data;
    private static Worker instance = null;
    private static GameController gc;
    private static ConstructionQueue queue;

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
            gc = UnitManager.gc;
            queue = UnitManager.queue;
        }
        return instance;
    }

    private Direction checkDanger(){
        //aixo es per quan tinguem matriu de perills etc. De moment fa com si no hi ha perill mai
        return null;
    }

    private void resetTarget(){
        data.target_loc = null;
        data.target_type = TARGET_NONE;
        data.target_id = -1;
    }

    private void move(Unit unit) {
        if (!gc.isMoveReady(unit.id())) return;
        if (data.safest_direction != null) {
            gc.moveRobot(unit.id(), data.safest_direction);
            System.out.println("Worker " + unit.id() + " moves " + data.safest_direction + " (in danger)");
            return;
        }
        if (data.target_type == TARGET_NONE) return;
        MapLocation myLoc = unit.location().mapLocation();
        MapLocation dest = data.target_loc;
        PathfinderNode node = Pathfinder.getInstance().getNode(myLoc.getX(),myLoc.getY(),dest.getX(),dest.getY());
        Direction targetDir = node.dir;
        if (gc.canMove(unit.id(),targetDir)) {
            gc.moveRobot(unit.id(), targetDir);
            System.out.println("Worker " + unit.id() + " moves " + targetDir + " (to target)");
        }
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

    private void searchKarbonite(MapLocation location){
        long minDist = 1000000;
        MapLocation ans = null;
        for (int i = 0; i < UnitManager.Xmines.size(); ++i){
            int x = UnitManager.Xmines.get(i);
            int y = UnitManager.Ymines.get(i);

            MapLocation mineLoc = new MapLocation(gc.planet(), x, y);
            long d = location.distanceSquaredTo(mineLoc);
            if (d < minDist){
                minDist = d;
                ans = mineLoc;
            }
        }
        data.target_id = -1;
        data.target_type = TARGET_MINE;
        data.target_loc = ans;
    }

    private void updateTarget(Unit unit){
        if (data.safest_direction != null) return; //no fas update si estas en perill
        int type = data.target_type;
        if (type == TARGET_BLUEPRINT) return; //no fas update si ja tens un blueprint perque es lo mes important
        boolean found = searchNearbyBlueprint(unit.location().mapLocation());
        if (found || type == TARGET_STRUCTURE) return;
        found = searchNearbyStructure(unit.location().mapLocation());
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
            System.out.println("Worker " + unit.id() + "  " + unit.location().mapLocation() + " builds blueprint " + str.id());
        }else{
            //si es structure
            gc.repair(unit.id(),str.id());
            if (str.health() + unit.workerRepairHealth() >= str.maxHealth()) resetTarget();
            System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() +" repairs structure " + str.id() + ": " + str.health() + "/" + str.maxHealth());
        }
        return true;
    }

    private boolean tryReplicate(Unit unit){
        if (!gc.isAttackReady(unit.id())) return false; //no tinc clar que attack sigui lo correcte aqui pero idk
        if (!queue.needsUnit(UnitType.Worker)) return false;
        if (data.safest_direction != null) return false;
        for (Direction d: Direction.values()){
            if (gc.canReplicate(unit.id(), d)) {
                gc.replicate(unit.id(),d);
                System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() +" replicates");
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
                System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() +" places blueprint " + d);
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
                System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() + " harvests karbonite " + d);
                return;
            }
        }
    }

    //Aqui decideix si construeix/mina/etc
    private void doAction(Unit unit){
        //teoricament el hasMoved no cal
        boolean hasMoved = tryRepairBuild(unit);
        if (!hasMoved) hasMoved = tryReplicate(unit);
        if (!hasMoved) hasMoved = tryPlaceBlueprint(unit);
        if (!hasMoved) tryMine(unit);
    }

    void play(Unit unit){
        mapa.computeIfAbsent(unit.id(), k -> new WorkerData(unit.id()));
        data = mapa.get(unit.id());

        System.out.println("Worker " + unit.id() +  "  " + unit.location().mapLocation() + " has target " + data.target_loc);
        data.safest_direction = checkDanger();
        updateTarget(unit);
        doAction(unit);
        move(unit);
    }

/*
    void buildFactory(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        for(int i = 0; i < allDirs.length; ++i){
            MapLocation newLoc = myLoc.add(allDirs[i]);
            //canviar-ho a gc.hasUnitAtLocation(newLoc) quan estigui arreglat TODO
            try {
                Unit possibleFactory = gc.senseUnitAtLocation(newLoc);
                if(possibleFactory != null && possibleFactory.unitType() == UnitType.Factory && possibleFactory.health() != possibleFactory.maxHealth() && possibleFactory.team().equals(gc.team())) {
                    if (gc.canBuild(unit.id(), possibleFactory.id()))
                        gc.build(unit.id(), possibleFactory.id());
                    wait = true;
                }
            } catch(Throwable t){
                continue;
            }
        }
    }

    void blueprintFactory(Unit unit){
        for (int i = 0; i < allDirs.length; ++i){
            if (gc.canBlueprint(unit.id(), UnitType.Factory, allDirs[i])){
                gc.blueprint(unit.id(), UnitType.Factory, allDirs[i]);
                factoryBuilt = true;
                return;
            }
        }
    }

    void goToBestMine(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        long maxKarbo = 0;
        int dirIndex = -1;
        for (int i = 0; i < allDirs.length; ++i){
            MapLocation newLoc = myLoc.add(allDirs[i]);
            if (!UnitManager.map.onMap(newLoc)) continue;
            long k = gc.karboniteAt(newLoc);
            if (k > maxKarbo){
                maxKarbo = k;
                dirIndex = i;
            }
        }
        if (dirIndex >= 0){
            if (unit.workerHasActed() == 0 && gc.canHarvest(unit.id(), allDirs[dirIndex])) {
                gc.harvest(unit.id(), allDirs[dirIndex]);
            }
            return;
        }

        MapLocation target = getBestMine(myLoc);
        if (target == null) {
            return; // what to do? xD
        }
        UnitManager.getInstance().moveTo(unit, target);
    }

    MapLocation getBestMine(MapLocation loc){
        long minDist = 1000000;
        MapLocation ans = null;
        for (int i = 0; i < UnitManager.Xmines.size(); ++i){
            int x = UnitManager.Xmines.get(i);
            int y = UnitManager.Ymines.get(i);
            MapLocation mineLoc = new MapLocation(gc.planet(), x, y);
            long d = loc.distanceSquaredTo(mineLoc);
            if (d < minDist){
                minDist = d;
                ans = mineLoc;
            }
        }
        return ans;
    }
*/
}