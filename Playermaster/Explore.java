import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Ivan on 1/21/2018.
 */
public class Explore {
    static final int areaSize = 5;
    static final double enemyBaseValue = -5;
    static final int exploreConstant = 1;
    static HashMap<Integer, Integer> objectiveArea;
    static HashMap<Integer, Integer> currentArea;
    static int[] areaToLocX;
    static int[] areaToLocY;
    static double[][] exploreGrid;
    static int exploreSizeX;
    static int exploreSizeY;
    static int[][] locToArea;

    public static void initTurn(){
        updateCurrentArea();
        if (Mapa.onMars()) sendLocationsEnemyRockets();
        if (Mapa.onEarth()) receiveLocationsEnemyRockets();
    }



    static AuxMapLocation areaToLocation(Integer area){
        try {
            int x = areaToLocX[Utils.decodeX(area)];
            int y = areaToLocY[Utils.decodeY(area)];
            return new AuxMapLocation(x, y);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static AuxMapLocation findExploreObjective(AuxUnit unit){
        try {
            unit.exploretarget = true;
            Integer current = currentArea.get(unit.getID());
            Integer obj = null;
            double minExplored = Const.INFS;
            AuxMapLocation myLoc = unit.getMapLocation();
            if (objectiveArea.containsKey(unit.getID()) && current.intValue() != objectiveArea.get(unit.getID()).intValue()) {
                return areaToLocation(objectiveArea.get(unit.getID()));
            }
            for (int i = 0; i < exploreSizeX; ++i) {
                for (int j = 0; j < exploreSizeY; ++j) {
                    if (current.intValue() == Utils.encode(i, j).intValue()) continue;
                    Integer area = Utils.encode(i, j);
                    AuxMapLocation areaLoc = areaToLocation(area);
                    if (myLoc.distanceBFSTo(areaLoc) >= Const.INFS) continue;
                    if (exploreGrid[i][j] < minExplored) {
                        minExplored = exploreGrid[i][j];
                        obj = area;
                    }
                }
            }
            if (obj != null) {
                Wrapper.addExploreGrid(obj, exploreConstant);
                objectiveArea.put(unit.getID(), obj);
            }
            if (obj != null) return areaToLocation(obj);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }


    static void updateCurrentArea(){
        try {
            for (AuxUnit unit : Units.myUnits) {
                //if (unit.isInGarrison()) continue;
                Integer current = Wrapper.locationToArea(unit.getMapLocation());
                if (currentArea.containsKey(unit.getID()) && currentArea.get(unit.getID()) == current) continue;
                currentArea.put(unit.getID(), current);
                Wrapper.addExploreGrid(current, exploreConstant);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void sendLocationsEnemyRockets() {
        for (AuxUnit unit:Units.enemies) {
            if (unit.getType() == UnitType.Rocket && !unit.isInSpace()) {
                if (Communication.getInstance().canSendMessage()) {
                    Communication.getInstance().sendRocketLocation(unit.getMapLocation());
                }
            }
        }
    }

    static void receiveLocationsEnemyRockets() {
        ArrayList<Message> messages = Communication.getInstance().getMessagesToRead();
        for (Message msg:messages) {
            if (msg.subject == Communication.ROCKET_LOC) {
                Rocket.enemyRocketLandingsLocs.add(msg.mapLoc);
                Rocket.enemyRocketLandingsCcs[MarsPlanning.cc[msg.mapLoc.x][msg.mapLoc.y]]++;
            }
        }
    }

}
