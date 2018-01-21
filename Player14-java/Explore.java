import java.util.HashMap;

/**
 * Created by Ivan on 1/21/2018.
 */
public class Explore {

    static final int INF = 1000000000;
    static HashMap<Integer, Integer> objectiveArea;



    static void initialize(){
        objectiveArea = new HashMap<>();
    }

    static void explore(AuxUnit unit) {
        try {
            AuxMapLocation obj = findExploreObjective(unit);
            if (obj != null) MovementManager.getInstance().moveTo(unit, obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static AuxMapLocation areaToLocation(Integer area){
        try {
            int x = Data.areaToLocX[Data.decodeX(area)];
            int y = Data.areaToLocY[Data.decodeY(area)];
            return new AuxMapLocation(x, y);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static AuxMapLocation findExploreObjective(AuxUnit unit){
        try {
            Integer current = Data.currentArea.get(unit.getID());
            Integer obj = null;
            double minExplored = INF;
            AuxMapLocation myLoc = unit.getMaplocation();
            if (objectiveArea.containsKey(unit.getID()) && current.intValue() != objectiveArea.get(unit.getID()).intValue()) {
                return areaToLocation(objectiveArea.get(unit.getID()));
            }
            for (int i = 0; i < Data.exploreSizeX; ++i) {
                for (int j = 0; j < Data.exploreSizeY; ++j) {
                    if (current.intValue() == Data.encode(i, j).intValue()) continue;
                    Integer area = Data.encode(i, j);
                    AuxMapLocation areaLoc = areaToLocation(area);
                    if (myLoc.distanceBFSTo(areaLoc) >= INF) continue;
                    if (Data.exploreGrid[i][j] < minExplored) {
                        minExplored = Data.exploreGrid[i][j];
                        obj = area;
                    }
                }
            }
            if (obj != null) {
                Data.addExploreGrid(obj, Data.exploreConstant);
                objectiveArea.put(unit.getID(), obj);
            }
            if (obj != null) return areaToLocation(obj);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
