import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import bc.UnitType;

/**
 * Created by Ivan on 1/23/2018.
 */
public class Overcharge {

    static ArrayList<Integer>[] adjMatrix;


    static void generateMatrix(){
        if (!Data.canOverCharge || Data.round%10 != 0) return;
        int n = Data.myUnits.length;

        adjMatrix = (ArrayList<Integer>[])new ArrayList[n];

        for (int i = 0; i < n; ++i) adjMatrix[i] = new ArrayList<>();

        int MAX_RANGE = 30;

        for (int i = 0; i < n; ++i){
            if (!Data.myUnits[i].isInGarrison()){
                if (Data.myUnits[i].getType() == UnitType.Ranger){
                    for (int j = 0; j < n; ++j){
                        if (Data.myUnits[j].isInGarrison()) continue;
                        if (Data.myUnits[j].getType() == UnitType.Healer){
                            if (!Data.myUnits[j].canUseAbility()) continue;
                            int l = (Data.myUnits[i].getMaplocation().distanceSquaredTo(Data.myUnits[j].getMaplocation()));
                            if (l <= MAX_RANGE) adjMatrix[i].add(j);
                        }
                    }
                }
            }
        }
    }

    static boolean getOvercharged(int i){
        if (!Data.canOverCharge || Data.round%10 != 0) return false;
        if (adjMatrix[i].size() <= 0) return false;
        int j = adjMatrix[i].get(0);
        if (!Data.myUnits[j].canUseAbility()){
            adjMatrix[i].remove(0);
            return false;
        }
        Wrapper.overcharge(Data.myUnits[j], Data.myUnits[i]);
        return true;
    }

    static boolean canGetOvercharged(int i){
        if (!Data.canOverCharge || Data.round%10 != 0) return false;
        while (adjMatrix[i].size() > 0 && !Data.myUnits[adjMatrix[i].get(0)].canUseAbility()){
            adjMatrix[i].remove(0);
        }
        return (adjMatrix[i].size() > 0);
    }
}
