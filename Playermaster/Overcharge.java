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
            int n = Units.myUnits.size();

            adjMatrix = (ArrayList<Integer>[]) new ArrayList[n];

            for (int i = 0; i < n; ++i) adjMatrix[i] = new ArrayList<>();

            int MAX_RANGE = 30;

            for (int i = 0; i < n; ++i) {
                AuxUnit unit1 = Units.myUnits.get(i);
                if (!unit1.isInGarrison()) {
                    if (unit1.getType() == UnitType.Ranger) { //todo tenir arrays per healers i rangers
                        for (int j = 0; j < n; ++j) {
                            AuxUnit unit2 = Units.myUnits.get(j);
                            if (unit2.isInGarrison()) continue;
                            if (unit2.getType() == UnitType.Healer) {
                                if (!unit2.canUseAbility()) continue;
                                int l = (unit1.getMapLocation().distanceSquaredTo(unit2.getMapLocation()));
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
            if (!Units.myUnits.get(j).canUseAbility()) {
                adjMatrix[i].remove(0);
                return false;
            }
            Wrapper.overcharge(Units.myUnits.get(j), Units.myUnits.get(i));
            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean canGetOvercharged(int i){
        try {
            if (!Units.canOverCharge || Utils.round % 10 != 0) return false;
            while (adjMatrix[i].size() > 0 && !Units.myUnits.get(adjMatrix[i].get(0)).canUseAbility()) {
                adjMatrix[i].remove(0);
            }
            return (adjMatrix[i].size() > 0);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
