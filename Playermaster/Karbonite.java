import bc.AsteroidPattern;
import bc.AsteroidStrike;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Pau on 23/01/2018.
 */
public class Karbonite {

    static HashMap<Integer, Integer> karboniteAt;
    static HashMap<Integer, Integer> asteroidTasksLocs; //karbo location -> ID worker assignat
    static HashMap<Integer, Integer> asteroidTasksIDs;  //ID worker -> karbo location assignada
    static Integer[] asteroidRounds;
    //static AsteroidStrike[] asteroidStrikes;
    static AsteroidPattern asteroidPattern;
    static AuxMapLocation[] asteroidLocations;
    static Integer[] asteroidCarbo;
    static int[][] karboMap;

    public static void initGame(){
        asteroidPattern = GC.gc.asteroidPattern();
        karboniteAt = new HashMap<>(); //filled in pathfinder
        asteroidTasksLocs = new HashMap<>();
        asteroidTasksIDs = new HashMap<>();
    }

    public static void initTurn(){
        karboMap = new int[Mapa.W][Mapa.H];
        updateMines();
        fillKarboMap();
    }

    private static void updateKarboniteAt(){
        Iterator<HashMap.Entry<Integer, Integer>> it = karboniteAt.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<Integer, Integer> entry = it.next();
            AuxMapLocation location = new AuxMapLocation(entry.getKey());
            int karbonite = location.getKarbonite();
            if (karbonite != -1){
                int value = entry.getValue();
                if (karbonite > 0) {
                    if (karbonite != value) putMine(location.x, location.y, karbonite);
                } else it.remove();
            }
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
            putMine(loc.x, loc.y, karboniteAt.get(loc.encode()) + karbonite);
        else putMine(loc.x, loc.y, karbonite);
    }

    static void updateMines() {
        try {
            updateKarboniteAt();

            if (Mapa.onMars()) {
                updateAsteroidTasks();
                if (asteroidPattern.hasAsteroid(Utils.round)) {
                    AsteroidStrike strike = asteroidPattern.asteroid(Utils.round);
                    addAsteroid(strike);
                }
            }
            /*
            System.out.println("");
            System.out.println("====================== TASK ARRAY " + round + " ====================== ");
            for (Map.Entry<Integer,Integer> entry: asteroidTasksLocs.entrySet()){
                AuxMapLocation l = toLocation(entry.getKey());
                int id = entry.getValue();
                System.out.println("Location " + l.x + "," + l.y + " has worker " + id);
            }
            System.out.println("");
            for (Map.Entry<Integer,Integer> entry: asteroidTasksIDs.entrySet()){
                int id = entry.getKey();
                AuxMapLocation l = toLocation(entry.getValue());
                System.out.println("Worker " + id + " has location " + l.x + "," + l.y);
            }
            System.out.println("");*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static void fillKarboMap(){
        karboMap = new int[Mapa.W][Mapa.H];
        for (Integer a : karboniteAt.keySet()) {
            AuxMapLocation mloc = new AuxMapLocation(a);
            karboMap[mloc.x][mloc.y] = karboniteAt.get(a);
        }
    }

    static void putMine(AuxMapLocation loc, int value){
        int encoding = loc.encode();
        karboniteAt.put(encoding, value);
    }

    static void putMine(int x, int y, int value){
        putMine(new AuxMapLocation(x,y),value);
    }

    static int getValue(AuxMapLocation mloc){
        try {
            int encoding = mloc.encode();
            return karboniteAt.get(encoding);
        }catch(Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

}
