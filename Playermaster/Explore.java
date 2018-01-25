import bc.Unit;
import bc.VecUnit;

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

    public static void initGame(){
        createGrid();
        objectiveArea = new HashMap<>();
        getLocationEnemyBase();
    }

    static private AuxMapLocation getAccessLocation(int xCenter, int yCenter){
        try {
            AuxMapLocation realCenter = new AuxMapLocation(xCenter, yCenter);
            //TODO check apart from passable passable from origin in earth
            if (realCenter.isPassable()) return realCenter;
            for (int i = 0; i < Const.allDirs.length; ++i) {
                AuxMapLocation fakeCenter = realCenter.add(i);
                if (fakeCenter.isPassable()) return fakeCenter;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void createGrid(){
        try {
            currentArea = new HashMap<>();
            exploreSizeX = Mapa.W / areaSize;
            exploreSizeY = Mapa.H / areaSize;
            double auxiliarX = (double) Mapa.W / exploreSizeX;
            double auxiliarY = (double) Mapa.H / exploreSizeY;
            exploreGrid = new double[exploreSizeX][exploreSizeY];
            locToArea = new int[Mapa.W][Mapa.H];
            areaToLocX = new int[exploreSizeX];
            areaToLocY = new int[exploreSizeY];
            for (int i = 0; i < exploreSizeX; ++i) {
                for (int j = 0; j < exploreSizeY; ++j) {
                    for (int x = (int) Math.floor(i * auxiliarX); x < Math.floor((i + 1) * auxiliarX); ++x) {
                        for (int y = (int) Math.floor(j * auxiliarY); y < Math.floor((j + 1) * auxiliarY); ++y) {
                            locToArea[x][y] = encode(i, j);
                        }
                    }
                    int xCenter = (int) Math.floor(i * auxiliarX) + areaSize / 2;
                    int yCenter = (int) Math.floor(j * auxiliarY) + areaSize / 2;
                    AuxMapLocation centerArea = getAccessLocation(xCenter, yCenter);
                    if (centerArea != null) {
                        areaToLocX[i] = centerArea.x;
                        areaToLocY[j] = centerArea.y;
                        continue;
                    }
                    exploreGrid[i][j] = Const.INF;
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static Integer locationToArea(AuxMapLocation loc){
        try {
            return locToArea[loc.x][loc.y];
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void addExploreGrid(Integer area, double value) {
        try {
            int x = decodeX(area);
            int y = decodeY(area);
            exploreGrid[x][y] += value;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void getLocationEnemyBase(){
        try {
            VecUnit initialUnits = Mapa.planetMap.getInitial_units();
            for (int i = 0; i < initialUnits.size(); ++i) {
                Unit possibleEnemy = initialUnits.get(i);
                if (possibleEnemy.team() == Utils.enemyTeam) {
                    Integer enemyArea = locationToArea((new AuxUnit(possibleEnemy, false).getMapLocation()));
                    addExploreGrid(enemyArea, enemyBaseValue);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void initTurn(){
        updateCurrentArea();
    }



    static AuxMapLocation areaToLocation(Integer area){
        try {
            int x = areaToLocX[decodeX(area)];
            int y = areaToLocY[decodeY(area)];
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
            double minExplored = Const.INF;
            AuxMapLocation myLoc = unit.getMapLocation();
            if (objectiveArea.containsKey(unit.getID()) && current.intValue() != objectiveArea.get(unit.getID()).intValue()) {
                return areaToLocation(objectiveArea.get(unit.getID()));
            }
            for (int i = 0; i < exploreSizeX; ++i) {
                for (int j = 0; j < exploreSizeY; ++j) {
                    if (current.intValue() == encode(i, j).intValue()) continue;
                    Integer area = encode(i, j);
                    AuxMapLocation areaLoc = areaToLocation(area);
                    if (myLoc.distanceBFSTo(areaLoc) >= Const.INF) continue;
                    if (exploreGrid[i][j] < minExplored) {
                        minExplored = exploreGrid[i][j];
                        obj = area;
                    }
                }
            }
            if (obj != null) {
                addExploreGrid(obj, exploreConstant);
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
                Integer current = locationToArea(unit.getMapLocation());
                if (currentArea.containsKey(unit.getID()) && currentArea.get(unit.getID()) == current) continue;
                currentArea.put(unit.getID(), current);
                addExploreGrid(current, exploreConstant);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static Integer encode(int i, int j){
        return i* Const.maxMapSize+j;
    }

    static int decodeX(Integer c){
        return c/ Const.maxMapSize;
    }

    static int decodeY(Integer c){
        return c% Const.maxMapSize;
    }

}
