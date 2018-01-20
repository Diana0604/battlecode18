import bc.UnitType;

import java.util.HashMap;

public class Worker {

    private static HashMap<Integer, WorkerData> mapa;
    private WorkerData data;
    private static Worker instance = null;
    private static ConstructionQueue queue;
    private static boolean DEBUG = false;
    private static boolean inDanger;

    // definicio dels target types, per ordre d'importancia
    private static final int TARGET_NONE = 0;
    private static final int TARGET_MINE = 1;
    private static final int TARGET_STRUCTURE = 2; //structure per reparar
    private static final int TARGET_BLUEPRINT = 3;
    private static final int TARGET_ROCKET = 4;

    // radi de busca de blueprints/structures
    private static final int SEARCH_RADIUS = 5;


    public Worker(){

    }

    static Worker getInstance(){
        if (instance == null){
            instance = new Worker();
            mapa = new HashMap<>();
            queue = Data.queue;
            //computeWorkerCap();
        }
        return instance;
    }

    void computeWorkerCap(){

    }

    private void initMemory(AuxUnit unit){
        int id = unit.getID();
        mapa.computeIfAbsent(id, k -> new WorkerData(id));
        data = mapa.get(id);
        data.loc = unit.getMaplocation();
        Danger.computeDanger(unit);
        inDanger = Danger.DPS[8] > 0;
        //System.out.println(unit.location() + "  " + unit.location().isInGarrison());
    }

    private void checkDanger(){
        //aixo es per quan tinguem matriu de perills etc. De moment fa com si no hi ha perill mai
        return;
    }

    private void deleteMine(AuxMapLocation location){
        Data.karboniteAt.remove(location);
    }

    private void resetTarget(){
        data.target_loc = null;
        data.target_type = TARGET_NONE;
        data.target_id = -1;
    }

    //Si esta en perill, fuig, si no, va al target
    private void move(AuxUnit unit) {
        //System.out.println("Worker can move? " + unit.canMove());
        if (!unit.canMove()) return;
        AuxMapLocation dest;
        if (data.target_type == TARGET_NONE) dest = data.loc; //si no tinc target, vaig al meu lloc (fuig sol)
        else dest = data.target_loc;
        //System.out.println("Worker moves to " + dest);
        MovementManager.getInstance().moveTo(unit,dest);
    }

    private boolean checkNeededForRocket(){
        HashMap<Integer, AuxMapLocation> mapa = Rocket.callsToRocket;
        AuxMapLocation loc = mapa.get(data.id);
        if (loc != null){
            data.target_loc = loc;
            data.target_type = TARGET_ROCKET;
            data.target_id = -1;
            return true;
        }
        return false;
    }

    //Busca un blueprint per construir que estigui a prop del worker
    private boolean searchNearbyBlueprint(AuxMapLocation location){
        for (Integer i : Data.structures){
            AuxUnit u = Data.myUnits[i];
            //UnitType type = u.unitType();
            //if (type != UnitType.Factory && type != UnitType.Rocket) continue;
            if (u.isBlueprint()) continue; //si no es un blueprint, sino una estructura
            AuxMapLocation loc = u.getMaplocation();
            PathfinderNode node = Pathfinder.getInstance().getNode(location, loc);
            if (node.dist > SEARCH_RADIUS) continue; //si esta massa lluny (ha de fer volta)

            //actualitza el target!
            data.target_id = u.getID();
            data.target_type = TARGET_BLUEPRINT;
            data.target_loc = loc;
            return true;
        }
        return false;
    }

    //Busca una structure per reparar que estigui a prop del worker
    private boolean searchNearbyStructure(AuxMapLocation location){
        for (Integer i : Data.structures){
            AuxUnit u = Data.myUnits[i];
            //UnitType type = u.unitType();
            //if (type != UnitType.Factory && type != UnitType.Rocket) continue;
            if (u.isBlueprint()) continue; //si es un blueprint, suda
            if (u.isMaxHealth()) continue; //si no cal reparar-ho
            AuxMapLocation loc = u.getMaplocation();
            PathfinderNode node = Pathfinder.getInstance().getNode(location, loc);
            if (node.dist > SEARCH_RADIUS) continue; //si esta massa lluny (ha de fer volta)

            //actualitza el target!
            data.target_id = u.getID();
            data.target_type = TARGET_STRUCTURE;
            data.target_loc = loc;
            return true;
        }
        return false;
    }

    //Busca la mina mes propera
    //Todo es pot optimitzar
    private void searchKarbonite(AuxMapLocation location){
        long minDist = 1000000;
        AuxMapLocation ans = null;
        for (HashMap.Entry<AuxMapLocation, Integer> entry : Data.karboniteAt.entrySet()) {
            AuxMapLocation mineLoc = entry.getKey();
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
        if (inDanger) return; //no fa update si esta en perill
        int type = data.target_type;

        if (type != TARGET_NONE && type != TARGET_MINE) resetTarget();

        //Si te una mina de target, mira que encara hi quedi karbonite. Si no, reseteja target i elimina la mina de l'array
        if (type == TARGET_MINE && Data.karboniteAt.get(data.target_loc) == null) resetTarget();
        if (type == TARGET_MINE) return;
        searchKarbonite(data.loc);
    }

    private void updateTargetEarth(){
        if (inDanger) return; //no fa update si esta en perill
        int type = data.target_type;

        //si un rocket el crida, fa return perque ja te el target mes important
        if (checkNeededForRocket()) return;
        if (type == TARGET_ROCKET) resetTarget();

        boolean found = false;

        // mira si hi ha una unit a la target location. Aixo es fa servir per si el target es structure/blueprint
        AuxUnit target_unit = null;
        if (data.target_id != -1 && Data.allUnits.keySet().contains(data.target_id)) target_unit = Data.myUnits[Data.allUnits.get(data.target_id)];

        //si te un blueprint de target, mira si el blueprint ja esta construit. Si esta construit, reseteja target.
        if (type == TARGET_BLUEPRINT && target_unit != null) {
            if (target_unit.isMaxHealth()) resetTarget();
            UnitType targetType = target_unit.getType();
            if (targetType != UnitType.Factory && targetType != UnitType.Rocket) resetTarget();
        }
        if (type == TARGET_BLUEPRINT) return;//no fas update si ja tens un blueprint perque es lo mes important
        found = searchNearbyBlueprint(data.loc);

        //Si te una structure de target, mira que la structure no estigui full vida. Si ho esta, reseteja
        if (type == TARGET_STRUCTURE && target_unit != null) {
            if (target_unit.isMaxHealth()) resetTarget();
            UnitType targetType = target_unit.getType();
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
        if (Data.onEarth()) updateTargetEarth();
        else updateTargetMars();
    }

    //busca de les 8 caselles adjacents l'estructura amb menys vida, o si no el blueprint amb mes
    private AuxUnit findAdjacentStructure(AuxMapLocation location){
        AuxUnit[] v = Wrapper.senseUnits(location,3, true);
               // gc.senseNearbyUnitsByTeam(location,3,myTeam); //radius * arrel(2)?
        AuxUnit bestStr = null; //volem trobar l'estructura amb menys vida, o el blueprint amb mes
        int bestStrHP = 1000000;
        AuxUnit bestBlp = null;
        int bestBlpRemainingHP = 1000000;
        for (AuxUnit u: v){
            UnitType type = u.getType();
            if (type != UnitType.Factory && type != UnitType.Rocket) continue;
            int hp = u.getHealth();
            int maxHp = Wrapper.getMaxHealth(type);
            int missingHp = maxHp - hp;
            if (u.isBlueprint()){
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

    private boolean tryRepairBuild(AuxUnit unit){
        if (!Data.onEarth()) return false;
        AuxUnit str = findAdjacentStructure(data.loc);
        if (str == null) return false;
        if (str.isBlueprint()){
            //si es blueprint
            Wrapper.build(unit, str);
            if (str.isMaxHealth()) resetTarget();
            if (DEBUG)System.out.println("Worker " + data.id + "  " + data.loc + " builds blueprint " + str.getID());
        }else{
            //si es structure
            Wrapper.repair(unit,str);
            if (str.isMaxHealth()) resetTarget();
            if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc +" repairs structure " + str.getID() + ": " + str.getHealth() + "/" + str.getMaxHealth());
        }
        return true;
    }

    private boolean shouldReplicate(AuxUnit unit){
        if (inDanger) return false;
        int search_radius = 8;
        int miningwork = 0; //turns of work
        int buildingwork = 0;
        int workers = 0;
        final int MIN_WORK = 30;
        AuxMapLocation myPos = data.loc;
        for (HashMap.Entry<AuxMapLocation,Integer> entry : Data.karboniteAt.entrySet()){
            AuxMapLocation loc = entry.getKey();
            if (myPos.distanceSquaredTo(loc) > search_radius) continue;
            int karbonite = entry.getValue();
            if (karbonite > 0) miningwork += karbonite/Data.harvestingPower + 1;
        }
        AuxUnit[] v = Wrapper.senseUnits(myPos,8, true);
        for (AuxUnit u: v){
            UnitType type = u.getType();
            int missingHealth = (int) (u.getMaxHealth() - u.getHealth());
            if ((type == UnitType.Rocket || type == UnitType.Factory) && missingHealth > 0){
                int workperturn;
                if (u.isBlueprint()) workperturn = Data.buildingPower;
                else workperturn = Data.repairingPower;
                buildingwork += missingHealth/workperturn + 1;
            }
        }
        for (int i = 0; i < Data.myUnits.length; i++){
            AuxUnit u = Data.myUnits[i];
            UnitType type = u.getType();
            if (type == UnitType.Worker) {
                //workers_total++;
                if (u.isInGarrison()) continue;
                if (myPos.distanceSquaredTo(u.getMaplocation()) <= search_radius) workers++;
            }
        }
        if (workers == 0) workers = 1;
        //if (workers_total == 0) workers_total = 1;
        double work_local = buildingwork / workers;
        double work_total = miningwork / workers;
        return (work_local + work_total) > MIN_WORK;
        //return (work_local > MIN_WORK_LOCAL) || (work_total > MIN_WORK_TOTAL);
    }

    private void tryReplicate(AuxUnit unit){
        if (inDanger) return;
        if (!unit.canUseAbility()) return;
        boolean should = shouldReplicate(unit);
        //System.out.println(unit.location().mapLocation() + " Should replicate? " + should);
        if (!queue.needsUnit(UnitType.Worker) && !should) return;
        for (int i = 0; i < 9; ++i){
            if (Wrapper.canReplicate(unit, i)) {
                Wrapper.replicate(unit,i);
                if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc +" replicates");
                queue.requestUnit(UnitType.Worker, false);
                return;
            }
        }
    }

    private boolean tryPlaceBlueprint(AuxUnit unit){
        if (inDanger) return false;
        UnitType type = null;
        if (Data.researchInfo.getLevel(UnitType.Rocket) > 0 && queue.needsUnit(UnitType.Rocket)) type = UnitType.Rocket;
        if (queue.needsUnit(UnitType.Factory)) type = UnitType.Factory;
        if (type == null) return false;
        //System.out.println(Data.round + " type to build: " + type);
        boolean[] aux = new boolean[9];
        for (int i = 0; i < 9; ++i) aux[i] = true;
        Danger.computeDanger(unit);
        for (int i = 0; i < 9; ++i){
            if (Danger.DPS[i] > 0) continue;
            if (!Wrapper.canPlaceBlueprint(unit, type, i)) continue;
            Wrapper.placeBlueprint(unit, type, i);
            if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc +" places blueprint " + i);
            queue.requestUnit(type, false);
            return true;

        }
        return false;
    }

    private boolean tryMine(AuxUnit unit){
        //System.err.println("Trying to mine!");
        for(int i = 0; i < 9; ++i){
            AuxMapLocation karboLoc = unit.getMaplocation().add(i);
            //System.out.println("OK");
            if (!karboLoc.isOnMap()) continue;
            HashMap<AuxMapLocation, Integer> mapa = Data.karboniteAt;
            if (!mapa.containsKey(karboLoc)) continue;
            int karboAmount = mapa.get(karboLoc);
            if (karboAmount > 0){
                //System.out.println("Unit location, dir: " + data.loc + "   " + d);
                int karboLeft = Wrapper.harvest(unit,i);
                if (karboLeft == -1) continue; //si no ha pogut minar
                if (karboLeft == 0) resetTarget();
                if (DEBUG)System.out.println("Worker " + data.id +  "  " + data.loc + " harvests karbonite " + i);
                return true;
            }
        }
        return false;
    }

    //Aqui decideix si construeix/mina/etc
    private boolean doAction(AuxUnit unit){
        if (!unit.canAttack()) return true;
        if (tryRepairBuild(unit)) return true;
        if (tryPlaceBlueprint(unit)) return true;
        if (tryMine(unit)) return true;
        return false;
    }

    void play(AuxUnit unit){
        try {
            //System.out.println("Worker " + data.id + " start round " + gc.round());
            initMemory(unit);
            if (unit.isInGarrison() || unit.isInSpace()) return;

            int id = data.id;
            boolean repl = doAction(unit);
            tryReplicate(unit);
            updateTarget();
            move(unit);
            if (!repl) doAction(unit);

            if (DEBUG) System.out.println("Worker " + id + "  " + data.loc + " has target " + data.target_loc + ", " + data.target_type);
            //System.out.println("Worker " + id + " end round " + gc.round());
        }catch(Exception e){
            System.out.println("CUIDADUUU!!!! Excepcio a worker, probablement perque un worker de dintre un garrison ha intentat fer una accio");
            System.out.println(Data.round);
            e.printStackTrace();
        }
    }
}
