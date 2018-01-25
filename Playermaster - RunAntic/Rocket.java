import bc.Planet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Rocket {

    static Rocket instance = null;

    public static HashMap<Integer, AuxMapLocation> callsToRocket; // hauria de ser Integer,Integer amb els ids, pero per minimitzar calls al gc...
    public static HashSet<AuxMapLocation> rocketTakeoffs;

    static int[] maxRobots = {1, 12, 12, 12, 12, 12, 12};

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
        try {
            if (!unit.getIsBuilt()) return;
            if (unit.isInSpace()) return;

            if (unit.getLocation().isOnPlanet(Planet.Earth)) {
                ArrayList<Pair> sorted = getSorted(unit);
                int[] robots = getRobotsInGarrison(unit);
                loadRobots(unit, sorted, robots, maxRobots);
                aSopar(unit, sorted, robots, maxRobots);
                boolean willLaunch = hasToLeaveByEggs(unit);
                if (!willLaunch) willLaunch = shouldLaunch(unit);
                if (willLaunch) rocketTakeoffs.add(unit.getMapLocation());
            } else if (unit.getLocation().isOnPlanet(Planet.Mars)) {
                if (unit.getGarrisonUnits().size() > 0) {
                    for (int i = 0; i < 8; ++i) {
                        if (Wrapper.canUnload(unit, i)) Wrapper.unload(unit, i);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    void play(AuxUnit unit) {
        try {
            //if it's still a blueprint return
            if (!unit.getIsBuilt()) return;
            if (unit.isInSpace()) return;
            //System.out.println("Rocket location " + unit.location() + " round " + gc.round());
            if (unit.getLocation().isOnPlanet(Planet.Earth)) {
                if (hasToLeaveByEggs(unit)) {
                    launchRocket(unit);
                    return;
                }
                if (shouldLaunch(unit)) launchRocket(unit);
            } else {
                //System.out.println("Rocket " + unit.getID() + " a l'espai!");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void launchRocket(AuxUnit unit) {
        try {
            int arrivalRound = Wrapper.getArrivalRound(Utils.round);
            AuxMapLocation arrivalLoc = MarsPlanning.bestPlaceForRound(arrivalRound);
            //System.out.println(arrivalLoc.x + " " + arrivalLoc.y);
            rocketLandingsLocs.add(arrivalLoc);
            rocketLandingsCcs[MarsPlanning.cc[arrivalLoc.x][arrivalLoc.y]]++;
            Wrapper.launchRocket(unit, arrivalLoc);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasToLeaveByEggs(AuxUnit unit) {
        try {
            if (Utils.round == 749) return true;
            if (unit.getGarrisonUnits().size() == 0) return false;
            Danger.computeDanger(unit);
            double danger = Danger.DPS[8];
            return unit.getHealth() <= 2.2 * danger;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private ArrayList<Pair> getSorted(AuxUnit unit) {
        try {
            //if (GC.planet != Planet.Earth) return new ArrayList<>();
            ArrayList<Pair> sorted = new ArrayList<>();
            for (int i = 0; i < Units.myUnits.length; ++i) {
                AuxUnit unit_i = Units.myUnits[i];
                if (unit_i.isInGarrison()) continue;
                double distance = unit.getMapLocation().distanceBFSTo(unit_i.getMapLocation());
                if (distance >= (double) Const.INF) continue;
                sorted.add(new Pair(distance, unit_i));
            }
            sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
            return sorted;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int[] getRobotsInGarrison(AuxUnit unit) {
        try {
            int[] ret = new int[7];
            for (int id : unit.getGarrisonUnits()) {
                AuxUnit unit_i = Units.myUnits[Units.allUnits.get(id)];
                ret[unit_i.getType().swigValue()]++;
            }
            return ret;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new int[0];
    }

    private void aSopar(AuxUnit unit, ArrayList<Pair> sorted, int[] robots, int[] maxRobots) {
        try {
            // cridar els que faltin
            int remaining = Units.rocketCapacity - unit.getGarrisonUnits().size();
            for (int i = 0; i < sorted.size(); ++i) {
                if (remaining == 0) break;
                AuxUnit unit_i = sorted.get(i).unit;
                int swig = unit_i.getType().swigValue();
                if (robots[swig] >= maxRobots[swig]) continue;
                if (!callsToRocket.containsKey(unit_i.getID())) {
                    callsToRocket.put(unit_i.getID(), unit.getMapLocation());
                    remaining--;
                    robots[swig]++;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRobots(AuxUnit unit, ArrayList<Pair> sorted, int[] robots, int[] maxRobots) {
        for (Pair p:sorted) {
            if (p.dist > 2 || unit.getGarrisonUnits().size() == Units.rocketCapacity) break;
            AuxUnit unit_i = p.unit;
            int swig = unit_i.getType().swigValue();
            if (robots[swig] >= maxRobots[swig]) continue;
            if (Wrapper.canLoad(unit, unit_i)) {
                Wrapper.load(unit, unit_i);
                robots[swig]++;
            }
        }
    }

    private boolean shouldLaunch(AuxUnit unit) {
        //System.out.println(GC.rocketCapacity + " " + unit.getGarrisonUnits().size());
        if (Units.rocketCapacity > unit.getGarrisonUnits().size()) return false;
        //Danger.computeDanger(unit);
        double dps = Danger.DPS[8];
        boolean shouldWait;
        // calcula quantes rondes podria aguantar amb aquest dps i mira si surt a compte esperar-les
        // si no te dps mira si surt a compte esperar qualsevol numero de rondes
        if (dps > 0) {
            int rounds = (int)Math.round(Math.min(1000, Math.floor(unit.getHealth() / dps)));
            shouldWait = MarsPlanning.shouldWaitToLaunchRocket(Utils.round, rounds);
        }
        else shouldWait = MarsPlanning.shouldWaitToLaunchRocket(Utils.round);
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

