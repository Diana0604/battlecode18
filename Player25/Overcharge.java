import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import bc.UnitType;

public class Overcharge {

    static int[][] overchargeMatrix; //[i][j] = nº healers amb overcharge a rang de i,j
    static HashMap<Integer, HashSet<Integer>> overchargeInRange; //troop index -> healers indexs with overcharge in range

    static void initTurn(){
        updateOverchargeMatrix();
        updateOverchargeInRange();
    }


    private static void updateOverchargeMatrix(){
        overchargeMatrix = new int[Mapa.W][Mapa.H];
        if (!Units.canOverCharge) return;
        for (int index: Units.healers){
            AuxUnit healer = Units.myUnits.get(index);
            if (!healer.canUseAbility()) continue;
            if (healer.isInGarrison() || healer.isInSpace()) continue;
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
            /*
            if (Utils.round < 225) return;
            System.out.println("OVERCHARGE MATRIX ROUND " + Utils.round + " AFTER HEALER " + healer.getMapLocation());
            for (int i = 0; i < Mapa.W; i++){
                for (int j = 0; j < Mapa.H; j++){
                    System.out.print(overchargeMatrix[i][j] + " ");
                }
                System.out.println("");
            }*/
        }
    }


    private static void updateOverchargeInRange(){
        overchargeInRange = new HashMap<>();
        auxUpdateOverchargeInRange(Units.rangers);
        auxUpdateOverchargeInRange(Units.mages);
        auxUpdateOverchargeInRange(Units.knights);
    }

    private static void auxUpdateOverchargeInRange(HashSet<Integer> troops){
        for (int troopIndex: troops){
            AuxUnit troop = Units.myUnits.get(troopIndex);
            HashSet<Integer> healerList = new HashSet<>();
            if (troop.isInGarrison() || troop.isInSpace()) continue;
            for (int healerIndex: Units.healers){
                AuxUnit healer = Units.myUnits.get(healerIndex);
                if (healer.isInGarrison() || healer.isInSpace()) continue;
                if (!healer.canUseAbility()) continue;
                int l = troop.getMapLocation().distanceSquaredTo(healer.getMapLocation());
                if (l <= Const.overchargeRange) healerList.add(healerIndex);
            }
            overchargeInRange.put(troopIndex, healerList);
        }
    }

    //quants overcharges pot rebre la tropa a posicio loc
    static int overchargesAt(AuxMapLocation loc){
        return overchargeMatrix[loc.x][loc.y];
    }

    //gasta un overcharge amb la tropa
    static void getOvercharged(int troopIndex, AuxMapLocation target){
        HashSet<Integer> healerList = overchargeInRange.get(troopIndex);
        int maxDist = -1;
        int maxIndex = -1;
        for (int healerIndex: healerList){
            AuxUnit healer = Units.myUnits.get(healerIndex);
            int dist = healer.getMapLocation().distanceSquaredTo(target);
            if (dist > maxDist){
                maxDist = dist;
                maxIndex = healerIndex;
            }
        }
        AuxUnit troop = Units.myUnits.get(troopIndex);
        AuxUnit healer = Units.myUnits.get(maxIndex);
        Wrapper.overcharge(healer, troop);
    }
}
