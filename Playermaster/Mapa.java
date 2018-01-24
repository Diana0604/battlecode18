import bc.MapLocation;
import bc.Planet;
import bc.PlanetMap;

/**
 * Created by Pau on 23/01/2018.
 */
public class Mapa {
    static Planet planet;
    static PlanetMap planetMap;
    static int W;
    static int H;

    public static void initGame(){
        planet = GC.gc.planet();
        planetMap = GC.gc.startingMap(Mapa.planet);
        W = (int) planetMap.getWidth();
        H = (int) planetMap.getHeight();
    }

    public static void initTurn(){

    }

    public static int getInitialKarbo(int x, int y){
        return (int) planetMap.initialKarboniteAt(new MapLocation(planet, x, y));
    }

    public static boolean onEarth(){ return planet == Planet.Earth;}

    public static boolean onMars(){ return !onEarth(); }
}
