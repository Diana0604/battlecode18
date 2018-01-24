import bc.Planet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Rocket {

    static Rocket instance = null;

    public static HashMap<Integer, AuxMapLocation> callsToRocket; // hauria de ser Integer,Integer amb els ids, pero per minimitzar calls al gc...
    public static HashSet<AuxMapLocation> rocketTakeoffs;

    static int[] maxUnitTypes = {1, 12, 12, 12, 12, 12, 12};

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
        for (int index: Units.rockets){
            AuxUnit unit = Units.myUnits[index];
            instance.initTurn(unit);
        }
    }

    void initTurn(AuxUnit rocket){
        try {
            if (!rocket.isBuilt()) return;
            if (rocket.isInSpace()) return;
            if (Mapa.onEarth()) {
                ArrayList<Pair> sortedUnits = getSortedUnits(rocket);
                int[] unitTypes = getUnitTypesGarrison(rocket);
                loadUnits(rocket, sortedUnits, unitTypes, maxUnitTypes);
                callRemainingRobots(rocket, sortedUnits, unitTypes, maxUnitTypes);
                boolean willLaunch = shouldLaunch(rocket);
                if (willLaunch) rocketTakeoffs.add(rocket.getMapLocation());
            } else if (rocket.getLocation().isOnPlanet(Planet.Mars)) {
                if (rocket.getGarrisonUnits().size() > 0) {
                    for (int i = 0; i < 8; ++i) {
                        if (Wrapper.canUnload(rocket, i)) Wrapper.unload(rocket, i);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Retorna totes les meves unitats ordenades per distancia a mi
    private ArrayList<Pair> getSortedUnits(AuxUnit rocket) {
        try {
            //if (GC.planet != Planet.Earth) return new ArrayList<>();
            ArrayList<Pair> sorted = new ArrayList<>();
            for (int i = 0; i < Units.myUnits.length; ++i) {
                AuxUnit unit = Units.myUnits[i]; //Todo fer tots els garrisons al principi de torn
                if (unit.isInGarrison()) continue;
                double distance = rocket.getMapLocation().distanceBFSTo(unit.getMapLocation());
                if (distance >= Const.INF) continue;
                sorted.add(new Pair(distance, unit));
            }
            sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
            return sorted;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int[] getUnitTypesGarrison(AuxUnit rocket) {
        try {
            int[] ret = new int[7];
            for (int id : rocket.getGarrisonUnits()) {
                AuxUnit unit = Units.getUnitByID(id);
                ret[unit.getType().swigValue()]++;
            }
            return ret;
        } catch(Exception e) {
            e.printStackTrace();
            return new int[0];
        }
    }

    // crida els robots que falten perque vinguin
    private void callRemainingRobots(AuxUnit rocket, ArrayList<Pair> sorted, int[] unitTypes, int[] maxUnitTypes) {
        aSopar(rocket, sorted, unitTypes, maxUnitTypes);
    }

    // joder mama, un altre cop verdura??
    private void aSopar(AuxUnit rocket, ArrayList<Pair> sorted, int[] unitTypes, int[] maxUnitTypes) {
        try {
            int remaining = Units.rocketCapacity - rocket.getGarrisonUnits().size();
            for (int i = 0; i < sorted.size(); ++i) {
                if (remaining == 0) return;
                AuxUnit unit = sorted.get(i).unit;
                int swig = unit.getType().swigValue();
                if (unitTypes[swig] >= maxUnitTypes[swig]) continue;
                if (!callsToRocket.containsKey(unit.getID())) {
                    callsToRocket.put(unit.getID(), rocket.getMapLocation());
                    remaining--;
                    unitTypes[swig]++;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // fa load de totes les units que te al voltant, si son del tipus adequat
    private void loadUnits(AuxUnit rocket, ArrayList<Pair> sorted, int[] unitTypes, int[] maxUnitTypes) {
        for (Pair p: sorted) {
            if (p.dist > 2 || rocket.getGarrisonUnits().size() == Units.rocketCapacity) return;
            AuxUnit unit = p.unit;
            int swig = unit.getType().swigValue();
            if (unitTypes[swig] >= maxUnitTypes[swig]) continue;
            if (Wrapper.canLoad(rocket, unit)) {
                Wrapper.load(rocket, unit);
                unitTypes[swig]++;
            }
        }
    }

    private boolean shouldLaunch(AuxUnit rocket){
        if (Utils.round == 749) return true; //si es l'ultim torn
        if (rocket.getGarrisonUnits().size() == 0) return false; //si esta buit
        Danger.computeDanger(rocket);
        double dps = Danger.DPS[8];
        if(rocket.getHealth() <= 2.2 * dps) return true; //si esta en perill de mort
        //hasToLeaveByEggs() never forget </3 sorry david :(

        // calcula quantes rondes podria aguantar amb aquest dps i mira si surt a compte esperar-les
        // si no te dps mira si surt a compte esperar qualsevol numero de rondes
        boolean shouldWait;
        if (dps > 0) {
            int rounds = (int)Math.round(Math.min(1000, Math.floor(rocket.getHealth() / dps)));
            shouldWait = MarsPlanning.shouldWaitToLaunchRocket(Utils.round, rounds);
        }
        else shouldWait = MarsPlanning.shouldWaitToLaunchRocket(Utils.round);
        if (shouldWait) return false;

        int garrisonSize = rocket.getGarrisonUnits().size();
        if (garrisonSize == 2 && Units.rocketsLaunched == 0) return true; //first rocket ple
        if (garrisonSize == Units.rocketCapacity) return true; //rocket ple

        return false; //s'hauria de fer que si no ha fet load durant 50 torns marxi...
    }


    void play(AuxUnit rocket) {
        try {
            //if it's still a blueprint return
            if (!rocket.isBuilt()) return;
            if (rocket.isInSpace()) return;
            //System.out.println("Rocket location " + rocket.location() + " round " + gc.round());
            if (Mapa.onEarth() && rocketTakeoffs.contains(rocket.getMapLocation()))
                launchRocket(rocket);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void launchRocket(AuxUnit rocket) {
        try {
            int arrivalRound = Wrapper.getArrivalRound(Utils.round);
            AuxMapLocation arrivalLoc = MarsPlanning.bestPlaceForRound(arrivalRound);
            //System.out.println(arrivalLoc.x + " " + arrivalLoc.y);
            rocketLandingsLocs.add(arrivalLoc);
            rocketLandingsCcs[MarsPlanning.cc[arrivalLoc.x][arrivalLoc.y]]++;
            Wrapper.launchRocket(rocket, arrivalLoc);
        } catch(Exception e) {
            e.printStackTrace();
        }
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

