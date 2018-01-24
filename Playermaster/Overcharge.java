import java.util.ArrayList;

import bc.UnitType;

/**
 * Created by Ivan on 1/23/2018.
 */
public class Overcharge {

    static ArrayList<Integer>[] adjMatrix;


    static void generateMatrix(){
        try {
            if (!Units.canOverCharge || Utils.round % 10 != 0) return;
            int n = Units.myUnits.length;

            adjMatrix = (ArrayList<Integer>[]) new ArrayList[n];

            for (int i = 0; i < n; ++i) adjMatrix[i] = new ArrayList<>();

            int MAX_RANGE = 30;

            for (int i = 0; i < n; ++i) {
                if (!Units.myUnits[i].isInGarrison()) {
                    if (Units.myUnits[i].getType() == UnitType.Ranger) {
                        for (int j = 0; j < n; ++j) {
                            if (Units.myUnits[j].isInGarrison()) continue;
                            if (Units.myUnits[j].getType() == UnitType.Healer) {
                                if (!Units.myUnits[j].canUseAbility()) continue;
                                int l = (Units.myUnits[i].getMapLocation().distanceSquaredTo(Units.myUnits[j].getMapLocation()));
                                if (l <= MAX_RANGE) adjMatrix[i].add(j);
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean getOvercharged(int i){
        try {
            if (!Units.canOverCharge || Utils.round % 10 != 0) return false;
            if (adjMatrix[i].size() <= 0) return false;
            int j = adjMatrix[i].get(0);
            if (!Units.myUnits[j].canUseAbility()) {
                adjMatrix[i].remove(0);
                return false;
            }
            Wrapper.overcharge(Units.myUnits[j], Units.myUnits[i]);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean canGetOvercharged(int i){
        try {
            if (!Units.canOverCharge || Utils.round % 10 != 0) return false;
            while (adjMatrix[i].size() > 0 && !Units.myUnits[adjMatrix[i].get(0)].canUseAbility()) {
                adjMatrix[i].remove(0);
            }
            return (adjMatrix[i].size() > 0);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
