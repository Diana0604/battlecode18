import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bc.UnitType;

public class Overcharge {

    static ArrayList<Integer>[] adjMatrix;
    static int[][] overchargeMatrix; //[i][j] = nº healers amb overcharge a rang de i,j
    static HashMap<Integer, HashSet<Integer>> overchargeInRange; //troop index -> healers indexs with overcharge in range

    static void initTurn(){
        updateOverchargeMatrix();
        updateOverchargeInRange();
    }

    private static void updateOverchargeMatrix(){
        overchargeMatrix = new int[Mapa.W][Mapa.H];
        if (Research.researchInfo.getLevel(UnitType.Healer) < 3) return;
        for (int index: Units.healers){
            AuxUnit healer = Units.myUnits.get(index);
            if (!healer.canUseAbility()) continue;
            int x = healer.getX();
            int y = healer.getY();
            int range = Const.overchargeRange;
            for (int i = 0; i < Vision.Mx[range].length; i++){
                int dx = Vision.Mx[range][i];
                int dy = Vision.My[range][i];
                AuxMapLocation loc = new AuxMapLocation(x+dx, y+dy);
                if (!loc.isOnMap()) continue;
                overchargeMatrix[x+dx][y+dy]++;
            }
        }
    }

    private static void updateOverchargeInRange(){
        overchargeInRange = new HashMap<>();
        for (int rangerIndex: Units.rangers){
            AuxUnit ranger = Units.myUnits.get(rangerIndex);
            overchargeInRange.put(rangerIndex, new HashSet<>());
            if (ranger.isInGarrison() || ranger.isInSpace()) continue;
            for (int healerIndex: Units.healers){
                AuxUnit healer = Units.myUnits.get(healerIndex);
                if (healer.isInGarrison() || healer.isInSpace()) continue;
                if (!healer.canUseAbility()) continue;
                int l = ranger.getMapLocation().distanceSquaredTo(healer.getMapLocation());
                if (l <= Const.overchargeRange) overchargeInRange.get(rangerIndex).add(healerIndex);
            }
        }
    }

    static void generateMatrix(){
        try {
            if (!Units.canOverCharge || Utils.round % 10 != 0) return;
            int n = Units.myUnits.size();

            adjMatrix = (ArrayList<Integer>[]) new ArrayList[n];

            for (int i = 0; i < n; ++i) adjMatrix[i] = new ArrayList<>();

            for (int index: Units.rangers){
                AuxUnit unit1 = Units.myUnits.get(index);
                if (unit1.isInGarrison()) continue;
                for (int index2: Units.healers){
                    AuxUnit unit2 = Units.myUnits.get(index2);
                    if (unit2.isInGarrison()) continue;
                    if (!unit2.canUseAbility()) continue;
                    int l = (unit1.getMapLocation().distanceSquaredTo(unit2.getMapLocation()));
                    if (l <= Const.overchargeRange) adjMatrix[index].add(index2);
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
            //if (!Units.canOverCharge || Utils.round % 10 != 0) return false;
            if (!Units.canOverCharge) return false;
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
