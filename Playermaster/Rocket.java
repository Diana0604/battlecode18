

import bc.UnitType;
import bc.Planet;

import java.util.ArrayList;
import java.util.HashMap;

public class Rocket {

    static Rocket instance = null;
    private UnitManager unitManager;

    private static HashMap<Integer, RocketData> mapa;
    private final int[] firstRocket = {   2,      0,      6,      0,      0,      0,      0};
    //                                  Worker  Knight  Ranger  Mage    Healer  Factory Rocket
    boolean[] center = {false,false,false,false,false,false,false,false,true};
    boolean wait;
    public static HashMap<Integer, AuxMapLocation> callsToRocket; // hauria de ser Integer,Integer amb els ids, pero per minimitzar calls al gc...

    static Rocket getInstance(){
        if (instance == null){
            instance = new Rocket();
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

    void playFirst(AuxUnit unit){
        //if it's still a blueprint return
        if(!unit.getIsBuilt()) return;
        wait = false;
        mapa.computeIfAbsent(unit.getID(), k -> new RocketData(unit.getID()));

        if (unit.getLocation().isOnPlanet(Planet.Earth)) {
            ArrayList<Pair> sorted = getSorted(unit);
            RocketData data = mapa.get(unit.getID());
            data.voyagers = decideVoyagers();
            int[] remaining = getRemaining(unit, data);
            loadRobots(unit, sorted, remaining);
            aSopar(unit, data, sorted, remaining);
        }
        else if (unit.getLocation().isOnPlanet(Planet.Mars)) {
            if (unit.getGarrisonUnits().size() > 0) {
                for (int i =0; i < 8; ++i) {
                    if (Wrapper.canUnload(unit, i)) Wrapper.unload(unit, i);
                }
            }
        }

    }

    void play(AuxUnit unit) {
        //if it's still a blueprint return
        if(!unit.getIsBuilt()) return;
        //System.out.println("Rocket location " + unit.location() + " round " + gc.round());
        if (unit.getLocation().isOnPlanet(Planet.Earth)) {
            if (hasToLeaveByEggs(unit)) {
                launchRocket(unit);
                return;
            }
            RocketData data = mapa.get(unit.getID());
            checkLaunch(unit, data);
        }else{
            //System.out.println("Rocket " + unit.getID() + " a l'espai!");
        }
    }

    private void launchRocket(AuxUnit unit) {
        int arrivalRound = Wrapper.getArrivalRound(Data.round);
        AuxMapLocation arrivalLoc = MarsPlanning.getInstance().bestPlaceForRound(arrivalRound);
        Wrapper.launchRocket(unit, arrivalLoc);
    }

    private boolean hasToLeaveByEggs(AuxUnit unit) {
        Danger.computeDanger(unit);
        double danger = Danger.DPS[8];
        return unit.getHealth() <= 2*danger;
    }

    private ArrayList<Pair> getSorted(AuxUnit unit) {
        if (Data.planet != Planet.Earth) return new ArrayList<>();
        ArrayList<Pair> sorted = new ArrayList<>();
        for (int i = 0; i < Data.myUnits.length; ++i) {
            AuxUnit unit_i = Data.myUnits[i];
            if (unit_i.isInGarrison()) continue;
            double distance = unit.getMaplocation().distanceBFSTo(unit_i.getMaplocation());
            if (distance >= (double) Pathfinder.INF) continue;
            sorted.add(new Pair(distance, unit_i));
        }
        sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
        //if (sorted.size() > 2) System.out.println(sorted.get(0).dist + " " + sorted.get(1).dist + " " + sorted.get(2).dist);
        return sorted;
    }

    private int comparePairs(Pair a, Pair b){
        if (a.dist < b.dist) return -1;
        if (a.dist > b.dist) return 1;
        return 0;
    }

    private int[] getRemaining(AuxUnit unit, RocketData data) {
        int[] remaining = data.voyagers.clone();
        // check garrison: de moment nomes descomptem els que ja han arribat.
        // Si hi ha robots que no haurien d'estar quedaran numeros negatius.
        ArrayList<Integer> garrison = unit.getGarrisonUnits();
        //VecUnitID garrison = unit.structureGarrison();
        for (int i = 0; i < garrison.size(); ++i) {
            remaining[Wrapper.getIndex(Data.myUnits[Data.allUnits.get(garrison.get(i))].getType())]--;
        }
        return remaining;
    }

    private void aSopar(AuxUnit unit, RocketData data, ArrayList<Pair> sorted, int[] remaining) {
        // cridar els que faltin
        for (int i = 0; i < sorted.size(); ++i) {
            AuxUnit unit_i = sorted.get(i).unit;
            UnitType type = unit_i.getType();
            if(remaining[Wrapper.getIndex(type)] > 0) {
                if (!callsToRocket.containsKey(unit_i.getID())) {
                    callsToRocket.put(unit_i.getID(), unit.getMaplocation());
                    remaining[Wrapper.getIndex(type)]--;
                }
            }
        }
    }

    private void loadRobots(AuxUnit unit, ArrayList<Pair> sorted, int[] remaining) {
        for (Pair p:sorted) {
            if (p.dist > 2) break;
            AuxUnit unit_i = p.unit;
            if (remaining[Wrapper.getIndex(unit_i.getType())] > 0) {
                if (Wrapper.canLoad(unit, unit_i)) {
                    Wrapper.load(unit, unit_i);
                    remaining[Wrapper.getIndex(unit_i.getType())]--;
                }
            }
        }
    }

    private int[] decideVoyagers() {
        return firstRocket.clone();
    }

    private void checkLaunch(AuxUnit unit, RocketData data) {
        int[] remaining = getRemaining(unit, data);
        if (full(remaining)) {
            Danger.computeDanger(unit);
            double dps = Danger.DPS[8];
            boolean shouldWait = false;
            // calcula quantes rondes podria aguantar amb aquest dps i mira si surt a compte esperar-les
            // si no te dps mira si surt a compte esperar qualsevol numero de rondes
            if (dps > 0) {
                int rounds = (int)Math.round(Math.min(1000, Math.floor(unit.getHealth() / dps)));
                shouldWait = MarsPlanning.getInstance().shouldWaitToLaunchRocket(Data.round, rounds);
            }
            else shouldWait = MarsPlanning.getInstance().shouldWaitToLaunchRocket(Data.round);
            if (!shouldWait) {
                launchRocket(unit);
            }
        }
    }

    private boolean full(int[] remaining) {
        for (int r:remaining) {
            if (r != 0) return false;
        }
        return true;
    }

    private class Pair {
        double dist;
        AuxUnit unit;

        Pair(double dist, AuxUnit unit){
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

