

import bc.*;
import java.util.*;

public class Pathfinder{
    static boolean[][] passable; //si no hi ha muntanyes

    static final int AUX = 6;
    static final int AUX2 = 12;
    static final double distFactor = 100;
    static final int base = 0x3F;
    static final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    static final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};
    static final double[] dists = {1, 1, 1, 1, 1, 1, 1, 1};
    static double [][] distToWalls;

    static final int off = 4;
    static final int base_off = 0xF;

    static short[][][][] Nodes;
    static int W;
    static int H;

    static int[][] CC;

    static final int connected_components = 0;

    static int getDist(short a){
        return (a >> off)&0xFFFF;
    }

    static int getDir(short a){
        return (a&base_off)&0xFFFF;
    }

    static short encode(int dist, int dir){
        return (short)(((dist << off) | dir)&0xFFFF);
    }

    private static void bfs(int a,int b){
        try {
            Nodes[a][b] = new short[W][H];

            Queue<AuxMapLocation> queue = new LinkedList<>();

            short def = encode(Const.INFS, 8);
            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    Nodes[a][b][x][y] = def;
                }
            }
            Nodes[a][b][a][b] = 8;
            queue.add(new AuxMapLocation(a,b));
            while (queue.size() > 0) {
                AuxMapLocation loc = queue.poll();
                int dist = getDist(Nodes[a][b][loc.x][loc.y]) + 1;
                int dir = getDir(Nodes[a][b][loc.x][loc.y]);
                for (int i = 0; i < 8; ++i) {
                    AuxMapLocation newLoc = loc.add(i);
                    if (!newLoc.isOnMap()) continue;
                    if (getDist(Nodes[a][b][newLoc.x][newLoc.y]) >= Const.INFS) {
                        if (passable[newLoc.x][newLoc.y]) queue.add(newLoc);
                        if (dist == 1) Nodes[a][b][newLoc.x][newLoc.y] = encode(dist, i);
                        else Nodes[a][b][newLoc.x][newLoc.y] = encode(dist, dir);
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    static int getDist(int x1, int y1, int x2, int y2){
        try{
            if(Nodes[x1][y1] == null) bfs(x1, y1);
            return getDist(Nodes[x1][y1][x2][y2]);
        }catch(Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    static int getDir(int x1, int y1, int x2, int y2){
        try{
            if(Nodes[x1][y1] == null) bfs(x1, y1);
            return getDir(Nodes[x1][y1][x2][y2]);
        }catch(Exception e) {
            e.printStackTrace();
            return 8;
        }
    }

}
