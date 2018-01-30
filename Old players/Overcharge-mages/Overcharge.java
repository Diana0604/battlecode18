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
    static int MAX_RANGE = 30;


    static void update(int i){
        if (!Data.canOverCharge || Data.round%10 != 0) return;
        adjMatrix[i] = new ArrayList<>();
        for (int j = 0; j < Data.myUnits.length; ++j){
            if (Data.myUnits[j].isInGarrison()) continue;
            if (Data.myUnits[j].getType() == UnitType.Healer){
                if (!Data.myUnits[j].canUseAbility()) continue;
                int l = (Data.myUnits[i].getMaplocation().distanceSquaredTo(Data.myUnits[j].getMaplocation()));
                if (l <= MAX_RANGE) adjMatrix[i].add(j);
            }
        }
    }

    static void generateMatrix(){
        if (!Data.canOverCharge || Data.round%10 != 0) return;
        int n = Data.myUnits.length;

        adjMatrix = (ArrayList<Integer>[])new ArrayList[n];

        for (int i = 0; i < n; ++i){
            if (!Data.myUnits[i].isInGarrison()){
                if (Data.myUnits[i].getType() == UnitType.Ranger || Data.myUnits[i].getType() == UnitType.Mage) {
                    update(i);
                }
            }
        }
    }

    static boolean isBetter(int i, int j, int k){
        if (Data.myUnits[i].target == null) return true;
        if (k == -1) return true;
        int d1 = Data.myUnits[i].target.distanceSquaredTo(Data.myUnits[j].getMaplocation());
        int d2 = Data.myUnits[i].target.distanceSquaredTo(Data.myUnits[k].getMaplocation());
        if (d2 >= d1) return true;
        return false;
    }

    static boolean getOvercharged(int i){
        if (!Data.canOverCharge || Data.round%10 != 0) return false;
        int n = adjMatrix[i].size();
        if (n <= 0) return false;
        int ans = -1;
        for (int k = n-1; k >= 0; --k){
            int j = adjMatrix[i].get(k);
            if (!Data.myUnits[j].canUseAbility()){
                adjMatrix[i].remove(k);
                continue;
            }
            if (isBetter(i, j, ans)) ans = j;
        }
        if (ans >= 0){
            Wrapper.overcharge(Data.myUnits[ans], Data.myUnits[i]);
            return true;
        }
        return false;
    }

    static int canGetOvercharged(int i){
        if (!Data.canOverCharge || Data.round%10 != 0) return 0;
        for (int j = adjMatrix[i].size()-1; j >= 0; --j){
            if (!Data.myUnits[adjMatrix[i].get(j)].canUseAbility()) adjMatrix[i].remove(j);
        }
        return (adjMatrix[i].size());
    }
}
