import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import bc.UnitType;

public class Overcharge {

    static HashMap<AuxMapLocation, HashSet<Integer>> overchargeMatrix; //[i][j] = nº healers amb overcharge a rang de i,j


    static void initTurn(){
        updateOverchargeMatrix();
    }


    private static void updateOverchargeMatrix(){
        overchargeMatrix = new HashMap<>();
        if (!Units.canOverCharge) return;
        for (int index: Units.healers){
            AuxUnit healer = Units.myUnits.get(index);
            if (!healer.canUseAbility()) continue;
            if (healer.isDead() || healer.isInGarrison() || healer.isInSpace()) continue;
            int x = healer.getX();
            int y = healer.getY();
            int range = Const.overchargeRange;
            for (int i = 0; i < Vision.Mx[range].length; i++){
                int dx = Vision.Mx[range][i];
                int dy = Vision.My[range][i];
                AuxMapLocation loc = new AuxMapLocation(x+dx, y+dy);
                if (!loc.isOnMap()) continue;
                overchargeMatrix.get(loc).add(index);
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

    //quants overcharges pot rebre la tropa a posicio loc
    static int overchargesAt(AuxMapLocation loc){
        HashSet<Integer> overcharges = overchargeMatrix.get(loc);
        if (overcharges == null) return 0;
        return overcharges.size();
    }

    //gasta un overcharge amb la tropa, del healer mes llunya al target
    static void getOvercharged(AuxUnit troop, AuxMapLocation target){
        HashSet<Integer> healerList = overchargeMatrix.get(troop.getMapLocation());
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
        AuxUnit healer = Units.myUnits.get(maxIndex);
        Wrapper.overcharge(healer, troop);
    }
}
