import bc.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Karbonite {

    static HashMap<Integer, Integer> karboniteAt;
    static HashMap<Integer, Integer> asteroidTasksLocs; //karbo location -> ID worker assignat
    static HashMap<Integer, Integer> asteroidTasksIDs;  //ID worker -> karbo location assignada
    static Integer[] asteroidRounds;
    //static AsteroidStrike[] asteroidStrikes;
    static AsteroidPattern asteroidPattern;
    static AuxMapLocation[] asteroidLocations;
    static Integer[] asteroidKarbo;
    static int[][] karboMap;

    public static void initTurn(){
        updateKarboniteAt();
        updateAsteroidStrikes();
        Wrapper.fillKarboMap();
    }

    private static void updateKarboniteAt(){
        Iterator<HashMap.Entry<Integer, Integer>> it = karboniteAt.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<Integer, Integer> entry = it.next();
            int value = entry.getValue();
            AuxMapLocation location = new AuxMapLocation(entry.getKey());

            long karbonite = Wrapper.getKarbonite(location);
            if (karbonite == -1) continue;
            if (karbonite > 0) {
                if (karbonite != value){
                    Wrapper.putMine(location.x, location.y, (int) karbonite);
                }
            } else it.remove();
        }
    }


    private static void updateAsteroidTasks(){
        Iterator<Map.Entry<Integer, Integer>> it2 = asteroidTasksIDs.entrySet().iterator();
        while (it2.hasNext()) {
            //esborra workers morts del hashmap
            Map.Entry<Integer, Integer> entry = it2.next();
            int assignedID = entry.getKey();
            if (!Units.allUnits.containsKey(assignedID)) it2.remove();
        }
    }

    private static void addAsteroid(AsteroidStrike strike){
        AuxMapLocation loc = new AuxMapLocation(strike.getLocation());
        boolean canAccess = false;
        for (int i = 0; i < 8; i++){
            //si el meteorit cau al mig de la muntanya, suda d'afegir-lo
            AuxMapLocation adjLoc = loc.add(i);
            if (adjLoc.isOnMap() && Pathfinder.passable[adjLoc.x][adjLoc.y]) canAccess = true;
        }
        if (!canAccess) return;
        asteroidTasksLocs.put(loc.encode(), -1);
        int karbonite = (int) strike.getKarbonite();
        if (karboniteAt.containsKey(loc.encode()))
            Wrapper.putMine(loc.x, loc.y, karboniteAt.get(loc.encode()) + karbonite);
        else Wrapper.putMine(loc.x, loc.y, karbonite);
    }

    private static void updateAsteroidStrikes(){
        try {
            if (Mapa.onMars()) {
                updateAsteroidTasks();
                if (asteroidPattern.hasAsteroid(Utils.round)) {
                    AsteroidStrike strike = asteroidPattern.asteroid(Utils.round);
                    addAsteroid(strike);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int getValue(AuxMapLocation mloc){
        try {
            return karboniteAt.get(mloc.encode());
        }catch(Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static void printTaskArray(){
        System.out.println("");
        System.out.println("====================== TASK ARRAY " + Utils.round + " ====================== ");
        for (Map.Entry<Integer,Integer> entry: asteroidTasksLocs.entrySet()){
            AuxMapLocation l = new AuxMapLocation(entry.getKey());
            int id = entry.getValue();
            System.out.println("Location " + l.x + "," + l.y + " has worker " + id);
        }
        System.out.println("");
        for (Map.Entry<Integer,Integer> entry: asteroidTasksIDs.entrySet()){
            int id = entry.getKey();
            AuxMapLocation l = new AuxMapLocation(entry.getValue());
            System.out.println("Worker " + id + " has location " + l.x + "," + l.y);
        }
        System.out.println("");
    }

}
