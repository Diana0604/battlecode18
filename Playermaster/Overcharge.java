import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import bc.UnitType;

public class Overcharge {

    static HashMap<Integer, HashSet<Integer>> overchargeMatrix; //[i][j] = nº healers amb overcharge a rang de i,j


    static void initTurn(){
        updateOverchargeMatrix();
    }


    private static void updateOverchargeMatrix(){
        overchargeMatrix = new HashMap<>();
        if (!Units.canOverCharge) return;
        for (int i = 0; i < Mapa.W; i++){
            for (int j = 0; j < Mapa.H; j++){
                AuxMapLocation loc = new AuxMapLocation(i, j);
                overchargeMatrix.put(loc.encode(), new HashSet<>());
            }
        }

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
                overchargeMatrix.computeIfAbsent(loc.encode(), k -> new HashSet<>());
                overchargeMatrix.get(loc.encode()).add(index);
            }
        }/*
        if (Utils.round < 225) return;
        System.out.println("OVERCHARGE MATRIX ROUND " + Utils.round);
        for (int i = 0; i < Mapa.W; i++){
            for (int j = 0; j < Mapa.H; j++){
                AuxMapLocation loc = new AuxMapLocation(i, j);
                if (overchargeMatrix.get(loc.encode()) == null) System.out.print(". ");
                else System.out.print(overchargeMatrix.get(loc.encode()).size() + " ");
            }
            System.out.println("");
        }*/
    }

    //quants overcharges pot rebre la tropa a posicio loc
    static int overchargesAt(AuxMapLocation loc){
        HashSet<Integer> overcharges = overchargeMatrix.get(loc.encode());
        if (overcharges == null) return 0;
        return overcharges.size();
    }

    //gasta un overcharge amb la tropa, del healer mes llunya al target
    static void getOvercharged(AuxUnit troop, AuxMapLocation target){
        HashSet<Integer> healerList = overchargeMatrix.get(troop.getMapLocation().encode());
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
