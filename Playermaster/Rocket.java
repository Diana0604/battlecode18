import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Rocket {

    public static HashMap<Integer, AuxMapLocation> callsToRocket;
    public static HashSet<AuxMapLocation> rocketTakeoffs;
    public static HashMap<Integer,HashSet<AuxMapLocation>> rocketLandingsByRound;

    // worker - knight - ranger - mage - healer - factory - rocket
    static int[] maxUnitTypes = {1, 12, 12, 5, 2, 0, 0};
    static final int MAX_ROUNDS_IDDLE = 50;

    static HashSet<AuxMapLocation> allyRocketLandingsLocs;
    static HashSet<AuxMapLocation> enemyRocketLandingsLocs;
    static int[] allyRocketLandingsCcs; // s'instancia a MarsPlanning despres de calculars les ccs
    static int[] enemyRocketLandingsCcs; // idem
    static HashMap<Integer, RocketData> rocketDatas;

    static void initGame(){
        allyRocketLandingsLocs = new HashSet<>();
        enemyRocketLandingsLocs = new HashSet<>();
        rocketDatas = new HashMap<>();
        rocketLandingsByRound = new HashMap<>();
    }

    static void initTurn() {
        try {
            callsToRocket = new HashMap<>();
            rocketTakeoffs = new HashSet<>();
            for (int index: Units.rockets){
                AuxUnit unit = Units.myUnits.get(index);
                initTurn(unit);
            }
            /*
            System.out.println("");
            System.out.println("====================== ROCKET CALLS " + Utils.round + " ====================== ");
            for (HashMap.Entry<Integer,AuxMapLocation> entry: callsToRocket.entrySet()){
                int id = entry.getKey();
                AuxUnit unit = Units.getUnitByID(id);
                AuxMapLocation target = entry.getValue();
                System.out.println(unit.getType() + " with ID " + id + " location " + unit.getMapLocation() + " has target " + target);
            }
            */
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void initTurn(AuxUnit rocket){
        try {
            if (!rocket.isBuilt()) return;
            if (rocket.isInSpace()) return;
            if (Mapa.onEarth()) {
                if (!rocketDatas.containsKey(rocket.getID())) rocketDatas.put(rocket.getID(), new RocketData());
                rocketDatas.get(rocket.getID()).roundsIdle++;
                ArrayList<Pair> sortedUnits = getSortedUnits(rocket);
                int[] unitTypes = getUnitTypesGarrison(rocket);
                int[] _maxUnitTypes = maxUnitTypes;
                if (Build.rocketsLaunched == 0 && Build.oneWorkerToMars) _maxUnitTypes = new int[]{1, 0, 0, 0, 0, 0, 0};
                loadUnits(rocket, sortedUnits, unitTypes, _maxUnitTypes);
                callRemainingRobots(rocket, sortedUnits, unitTypes, _maxUnitTypes);
                boolean willLaunch = shouldLaunch(rocket);
                if (willLaunch) rocketTakeoffs.add(rocket.getMapLocation());
            } else if (Mapa.onMars()) {
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
    private static ArrayList<Pair> getSortedUnits(AuxUnit rocket) {
        try {
            //if (GC.planet != Planet.Earth) return new ArrayList<>();
            ArrayList<Pair> sorted = new ArrayList<>();
            for (AuxUnit unit: Units.myUnits) {
                if (unit.isInGarrison()) continue;
                double distance = rocket.getMapLocation().distanceBFSTo(unit.getMapLocation());
                if (distance >= Const.INFS) continue;
                sorted.add(new Pair(distance, unit));
            }
            sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
            return sorted;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int[] getUnitTypesGarrison(AuxUnit rocket) {
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
    private static void callRemainingRobots(AuxUnit rocket, ArrayList<Pair> sorted, int[] unitTypes, int[] maxUnitTypes) {
        aSopar(rocket, sorted, unitTypes, maxUnitTypes);
    }

    // joder mama, un altre cop verdura??
    private static void aSopar(AuxUnit rocket, ArrayList<Pair> sorted, int[] unitTypes, int[] maxUnitTypes) {
        try {
            int remaining = Units.rocketCapacity - rocket.getGarrisonUnits().size();
            for (int i = 0; i < sorted.size(); ++i) {
                if (remaining == 0) return;
                AuxUnit unit = sorted.get(i).unit;
                int swig = unit.getType().swigValue();
                AuxUnit structure = Wrapper.getStructure(unit);
                if (structure != null && structure.getType() == UnitType.Rocket) continue;
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
    private static void loadUnits(AuxUnit rocket, ArrayList<Pair> sorted, int[] unitTypes, int[] maxUnitTypes) {
        try {
            for (Pair p: sorted) {
                if (p.dist > 2 || rocket.getGarrisonUnits().size() == Units.rocketCapacity) return;
                AuxUnit unit = p.unit;
                int swig = unit.getType().swigValue();
                if (unitTypes[swig] >= maxUnitTypes[swig]) continue;
                if (Wrapper.canLoad(rocket, unit)) {
                    Wrapper.load(rocket, unit);
                    unitTypes[swig]++;
                    rocketDatas.get(rocket.getID()).roundsIdle = 0;
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean shouldLaunch(AuxUnit rocket){
        try {
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
            if (garrisonSize == 1 && Build.rocketsLaunched == 0 && Build.oneWorkerToMars) return true; //first rocket ple
            if (garrisonSize == Units.rocketCapacity) return true; //rocket ple

            if (rocketDatas.get(rocket.getID()).roundsIdle > MAX_ROUNDS_IDDLE) return true;
            return false;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    static void play(AuxUnit rocket) {
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

    private static void launchRocket(AuxUnit rocket) {
        try {
            int arrivalRound = Wrapper.getArrivalRound(Utils.round);
            AuxMapLocation arrivalLoc = MarsPlanning.bestPlaceForRound(arrivalRound);
            allyRocketLandingsLocs.add(arrivalLoc);
            allyRocketLandingsCcs[MarsPlanning.cc[arrivalLoc.x][arrivalLoc.y]]++;
            Communication.getInstance().sendRocketLanding(arrivalRound, arrivalLoc);
            Wrapper.launchRocket(rocket, arrivalLoc);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static class Pair {
        double dist;
        AuxUnit unit;

        Pair(double dist, AuxUnit unit){
            this.dist = dist;
            this.unit = unit;
        }
    }

    private static class RocketData {
        int roundsIdle;
        RocketData() {
            roundsIdle = 0;
        }
    }

}

