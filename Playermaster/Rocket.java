import bc.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Rocket {

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Rocket instance = null;
    static GameController gc;
    private UnitManager unitManager;

    private static HashMap<Integer, RocketData> mapa;
    private final int[] firstRocket = {   2,      0,      6,      0,      0,      0,      0};
    //                                  Worker  Knight  Ranger  Mage    Healer  Factory Rocket
    boolean[] center = {false,false,false,false,false,false,false,false,true};
    boolean wait;
    public static HashMap<Integer, MapLocation> callsToRocket; // hauria de ser Integer,Integer amb els ids, pero per minimitzar calls al gc...

    static Rocket getInstance(){
        if (instance == null){
            instance = new Rocket();
            gc = UnitManager.gc;
        }
        return instance;
    }

    public Rocket() {
        unitManager = UnitManager.getInstance();
        mapa = new HashMap<>();
    }

    static void initTurn() {
        callsToRocket = new HashMap<>();
    }

    void playFirst(Unit unit){
        //if it's still a blueprint return
        if(unit.structureIsBuilt() == 0) return;
        wait = false;
        mapa.computeIfAbsent(unit.id(), k -> new RocketData(unit.id()));

        if (unit.location().isOnPlanet(Planet.Earth)) {
            RocketData data = mapa.get(unit.id());
            ArrayList<Pair> sorted = getSorted(unit);
            int[] remaining = getRemaining(unit, data);
            loadRobots(unit, sorted, remaining);
            aSopar(unit, data, sorted, remaining);
        }

    }

    void play(Unit unit) {
        //if it's still a blueprint return
        if(unit.structureIsBuilt() == 0) return;
        if (hasToLeaveByEggs(unit)) {
            launchRocket(unit);
            return;
        }
        checkLaunch(unit);
    }

    private void launchRocket(Unit unit) {

    }

    private boolean hasToLeaveByEggs(Unit unit) {
        Danger.computeDanger(unit.location().mapLocation(), center);
        double danger = Danger.DPS[8];
        return unit.health() <= danger;
    }

    private ArrayList<Pair> getSorted(Unit unit) {
        // get units sorted by proximity
        VecUnit myUnits = gc.myUnits();
        ArrayList<Pair> sorted = new ArrayList<>();
        for (int i = 0; i < myUnits.size(); ++i) {
            Unit unit_i = myUnits.get(i);
            if (!unit_i.location().isOnMap() || !unit_i.location().isOnPlanet(Planet.Earth)) continue;
            double distance = Pathfinder.getInstance().getNode(unit.location().mapLocation(), unit_i.location().mapLocation()).dist;
            if (distance == (double)Pathfinder.INF) continue;
            sorted.add(new Pair(distance, unit_i));
        }
        sorted.sort((p1, p2) -> (p1.dist < p2.dist)?-1:1);
        if (sorted.size() > 2) System.out.println(sorted.get(0).dist + " " + sorted.get(1).dist + " " + sorted.get(2).dist);
        return sorted;
    }

    private int[] getRemaining(Unit unit, RocketData data) {
        data.voyagers = decideVoyagers();
        int[] remaining = data.voyagers.clone();
        // check garrison: de moment nomes descomptem els que ja han arribat.
        // en realitat tambe s'haurien de xutar els que sobrin en cas que canvies la composicio de voyagers
        VecUnitID garrison = unit.structureGarrison();
        for (int i = 0; i < garrison.size(); ++i) {
            remaining[gc.unit(garrison.get(i)).unitType().swigValue()]--;
        }
        return remaining;
    }

    private void aSopar(Unit unit, RocketData data, ArrayList<Pair> sorted, int[] remaining) {
        // cridar els que faltin
        for (int i = 0; i < sorted.size(); ++i) {
            Unit unit_i = sorted.get(i).unit;
            UnitType type = unit_i.unitType();
            if(remaining[type.swigValue()] > 0) {
                if (!callsToRocket.containsKey(unit_i.id())) {
                    callsToRocket.put(unit_i.id(), unit.location().mapLocation());
                    remaining[type.swigValue()]--;
                }
            }
        }
    }

    private void loadRobots(Unit unit, ArrayList<Pair> sorted, int[] remaining) {
        for (Pair p:sorted) {
            if (p.dist > 2) break;
            Unit unit_i = p.unit;
            if (remaining[unit_i.unitType().swigValue()] > 0) {
                if (gc.canLoad(unit.id(), unit_i.id())) {
                    gc.load(unit.id(), unit_i.id());
                    remaining[unit_i.unitType().swigValue()]--;
                }
            }
        }
    }

    private int[] decideVoyagers() {
        return firstRocket.clone();
    }

    private void checkLaunch(Unit unit) {

    }

    private class Pair {
        double dist;
        Unit unit;

        Pair(double dist, Unit unit){
            this.dist = dist;
            this.unit = unit;
        }
    }

    private class RocketData {
        int[] voyagers;
        int id;

        RocketData(int id) {
            this.id = id;
        }
    }

}

