import bc.Planet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Rocket {

    static Rocket instance = null;

    public static HashMap<Integer, AuxMapLocation> callsToRocket; // hauria de ser Integer,Integer amb els ids, pero per minimitzar calls al gc...
    public static HashSet<AuxMapLocation> rocketTakeoffs;

    static HashSet<AuxMapLocation> rocketLandingsLocs;
    static int[] rocketLandingsCcs; // s'instancia a MarsPlanning despres de calculars les ccs

    static Rocket getInstance(){
        if (instance == null){
            instance = new Rocket();
        }
        return instance;
    }

    public Rocket() {
        rocketLandingsLocs = new HashSet<>();
    }

    static void initTurn() {
        callsToRocket = new HashMap<>();
        rocketTakeoffs = new HashSet<>();
    }

    void playFirst(AuxUnit unit){
        //if it's still a blueprint return
        if(!unit.getIsBuilt()) return;
        if (unit.isInSpace()) return;

        if (unit.getLocation().isOnPlanet(Planet.Earth)) {
            ArrayList<Pair> sorted = getSorted(unit);
            loadRobots(unit, sorted);
            aSopar(unit, sorted);
            boolean willLaunch = hasToLeaveByEggs(unit);
            if (!willLaunch) willLaunch = shouldLaunch(unit);
            if (willLaunch) rocketTakeoffs.add(unit.getMaplocation());
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
        if (unit.isInSpace()) return;
        //System.out.println("Rocket location " + unit.location() + " round " + gc.round());
        if (unit.getLocation().isOnPlanet(Planet.Earth)) {
            if (hasToLeaveByEggs(unit)) {
                launchRocket(unit);
                return;
            }
            if (shouldLaunch(unit)) launchRocket(unit);
        }else{
            //System.out.println("Rocket " + unit.getID() + " a l'espai!");
        }
    }

    private void launchRocket(AuxUnit unit) {
        int arrivalRound = Wrapper.getArrivalRound(Data.round);
        AuxMapLocation arrivalLoc = MarsPlanning.getInstance().bestPlaceForRound(arrivalRound);
        System.out.println(arrivalLoc.x + " " + arrivalLoc.y);
        rocketLandingsLocs.add(arrivalLoc);
        rocketLandingsCcs[MarsPlanning.getInstance().cc[arrivalLoc.x][arrivalLoc.y]]++;
        Wrapper.launchRocket(unit, arrivalLoc);
    }

    private boolean hasToLeaveByEggs(AuxUnit unit) {
        if (Data.round == 749) return true;
        if (unit.getGarrisonUnits().size() == 0) return false;
        Danger.computeDanger(unit);
        double danger = Danger.DPS[8];
        return unit.getHealth() <= 2.2*danger;
    }

    private ArrayList<Pair> getSorted(AuxUnit unit) {
        //if (Data.planet != Planet.Earth) return new ArrayList<>();
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

    private void aSopar(AuxUnit unit, ArrayList<Pair> sorted) {
        // cridar els que faltin
        int remaining = Data.rocketCapacity - unit.getGarrisonUnits().size();
        for (int i = 0; i < sorted.size(); ++i) {
            if (remaining == 0) break;
            AuxUnit unit_i = sorted.get(i).unit;
            if (!callsToRocket.containsKey(unit_i.getID())) {
                callsToRocket.put(unit_i.getID(), unit.getMaplocation());
                remaining--;
            }
        }
    }

    private void loadRobots(AuxUnit unit, ArrayList<Pair> sorted) {
        for (Pair p:sorted) {
            if (p.dist > 2 || unit.getGarrisonUnits().size() == Data.rocketCapacity) break;
            AuxUnit unit_i = p.unit;
            if (Wrapper.canLoad(unit, unit_i)) {
                Wrapper.load(unit, unit_i);
            }
        }
    }

    private boolean shouldLaunch(AuxUnit unit) {
        //System.out.println(Data.rocketCapacity + " " + unit.getGarrisonUnits().size());
        if (Data.rocketCapacity > unit.getGarrisonUnits().size()) return false;
        //Danger.computeDanger(unit);
        double dps = Danger.DPS[8];
        boolean shouldWait;
        // calcula quantes rondes podria aguantar amb aquest dps i mira si surt a compte esperar-les
        // si no te dps mira si surt a compte esperar qualsevol numero de rondes
        if (dps > 0) {
            int rounds = (int)Math.round(Math.min(1000, Math.floor(unit.getHealth() / dps)));
            shouldWait = MarsPlanning.getInstance().shouldWaitToLaunchRocket(Data.round, rounds);
        }
        else shouldWait = MarsPlanning.getInstance().shouldWaitToLaunchRocket(Data.round);
        return !shouldWait;
    }

    private class Pair {
        double dist;
        AuxUnit unit;

        Pair(double dist, AuxUnit unit){
            this.dist = dist;
            this.unit = unit;
        }
    }

}

