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
    private static AsteroidPattern asteroidPattern;
    static AuxMapLocation[] asteroidLocations;
    static Integer[] asteroidKarbo;
    static int[][] karboMap;

    public static void initGame(){
        asteroidPattern = GC.gc.asteroidPattern();
        karboniteAt = new HashMap<>();
        asteroidTasksLocs = new HashMap<>();
        asteroidTasksIDs = new HashMap<>();
        addInitialKarbo();
        fillKarboMap();
    }

    private static void addInitialKarbo(){
        //System.out.println("ok1");
        for (int x = 0; x < Mapa.W; ++x) {
            //System.out.println("ok2");
            for (int y = 0; y < Mapa.H; ++y) {
                //System.out.println("ok3");
                int karbonite = Mapa.getInitialKarbo(x,y);
                if (karbonite > Const.INF) karbonite = Const.INF;
                if (karbonite > 0) Karbonite.putMine(x, y, karbonite);
                //System.out.println("Afegeix karbo " + x + "," + y + ": " + karbonite);
            }
        }
    }

    public static void initTurn(){
        updateKarboniteAt();
        updateAsteroidStrikes();
        fillKarboMap();
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
                    putMine(location.x, location.y, (int) karbonite);
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
            putMine(loc.x, loc.y, karboniteAt.get(loc.encode()) + karbonite);
        else putMine(loc.x, loc.y, karbonite);
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

    private static void fillKarboMap(){
        karboMap = new int[Mapa.W][Mapa.H];
        //System.out.println("In fillkarbomap");
        for (Integer a : karboniteAt.keySet()) {
            AuxMapLocation loc = new AuxMapLocation(a);
            karboMap[loc.x][loc.y] = karboniteAt.get(a);
            //System.out.println(loc + " contains karbo " + karboniteAt.get(a));
        }
    }

    private static void putMine(AuxMapLocation loc, int value){
        karboniteAt.put(loc.encode(), value);
    }

    private static void putMine(int x, int y, int value){
        putMine(new AuxMapLocation(x,y),value);
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
