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


    public static boolean onEarth(){ return planet == Planet.Earth;}

    public static boolean onMars(){ return !onEarth(); }
}
