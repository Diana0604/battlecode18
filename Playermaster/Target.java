import bc.UnitType;

import java.util.*;

public class Target {

    static int[][] rangerTargets;
    static double[][] mageHits;
    static double[][] mageHitsExpanded;

    static void initTurn() {
        initMatrixs();
    }

    static void initMatrixs() {
        rangerTargets = new int[Mapa.W][Mapa.H];
        mageHits = new double[Mapa.W][Mapa.H];
        updateRangeMatrix(rangerTargets, UnitType.Ranger);
        updateMageMatrices();
    }

    static void updateRangeMatrix(int[][] targets, UnitType unitType) {
        int range = Units.getAttackRange(unitType);
        Queue<AuxMapLocation> queue = new LinkedList<>();
        for (AuxUnit unit : Units.enemies){
            if (unit.getType() == UnitType.Worker) continue;
            AuxMapLocation loc = unit.mloc;
            for (int i = 0; i < Vision.Mx[range].length; ++i) {
                AuxMapLocation newLoc = loc.add(new AuxMapLocation(Vision.Mx[range][i], Vision.My[range][i]));
                if (newLoc.isOnMap() && targets[newLoc.x][newLoc.y] == 0 && newLoc.isAccessible()) {
                    targets[newLoc.x][newLoc.y] = 1;
                    queue.add(newLoc);
                }
            }
        }
        while (!queue.isEmpty()){
            AuxMapLocation loc = queue.poll();
            int d = rangerTargets[loc.x][loc.y];
            for (int i = 0; i < 8; ++i){
                AuxMapLocation newLoc = loc.add(i);
                if (newLoc.isOnMap() && targets[newLoc.x][newLoc.y] == 0 && newLoc.isAccessible()){
                    targets[newLoc.x][newLoc.y] = d+1;
                    queue.add(newLoc);
                }
            }
        }
    }

    static int dist(AuxMapLocation loc){
        int a = rangerTargets[loc.x][loc.y];
        if (a == 0) return Const.INF;
        return a-1;
    }

    static int encode (int val, AuxMapLocation loc, int dir){
        return (((val << 12) | loc.encode()) << 4)|dir;
    }

    static AuxMapLocation getLoc(int a){
        int enc = (a >> 4)&0xFFF;
        return new AuxMapLocation(enc);
    }

    static int getDir(int a){
        return a&0xF;
    }

    static int getBestDirection(AuxMapLocation loc){

        if (dist(loc) <= 0) return 8;

        int MAX_ITER = Math.min(500, 1000/Units.unitTypeCount.get(UnitType.Ranger));

        HashMap<Integer, Integer> visited = new HashMap<>();
        PriorityQueue<Integer> queue = new PriorityQueue<>();

        visited.put(loc.encode(), 0);

        queue.add(encode(dist(loc), loc, 8));

        int cont  = 0;
        while (!queue.isEmpty() && cont < MAX_ITER){
            ++cont;
            int a = queue.poll();
            AuxMapLocation mapLoc = getLoc(a);
            int d = visited.get(mapLoc.encode());
            int dir = getDir(a);
            for (int i = 0; i < 8; ++i){
                AuxMapLocation newLoc = mapLoc.add(i);
                if (newLoc.isOnMap() && newLoc.isPassable() && newLoc.getUnit() == null){
                    int enc = newLoc.encode();
                    if (!visited.containsKey(enc) || visited.get(enc) > d+1){
                        int newDir = dir;
                        if (dir == 8) newDir = i;
                        int extraVal = dist(newLoc);
                        if (extraVal == 0) return newDir;
                        int newVal = d+1+extraVal;
                        visited.put(enc, d+1);
                        queue.add(encode(newVal, newLoc, newDir));
                    }
                }
            }
        }
        return -1;
    }


    /*---------------------------- MAGES *-------------------------------------------------*/


    static double MIN_VALUE = 1.5;

    static void updateMageMatrices(){
        for (AuxUnit enemy: Units.enemies){
            putUnit(enemy.getType(), enemy.getMapLocation(), true);
        }
    }

    static double unitValue(UnitType type){
        switch (type){
            case Worker: return 0.45;
            case Knight: return 1;
            default: return 0.95;
        }
    }

    static void putUnit(UnitType type, AuxMapLocation loc, boolean in){
        double val = unitValue(type);
        if (!in) val = -val;
        for (int i = 0; i < 9; ++i){
            AuxMapLocation newLoc = loc.add(i);
            if (newLoc.isOnMap()) {
                mageHits[newLoc.x][newLoc.y] += val;
            }
        }
    }
}
