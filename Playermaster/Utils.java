package Playermaster;

import bc.*;

class Utils {

    static boolean onTheMap(MapLocation location, GameController gc){
        PlanetMap planetMap = gc.startingMap(gc.planet());
        //System.out.println("Location " + location + " on the map? " + planetMap.onMap(location));
        return planetMap.onMap(location);
    }
}
