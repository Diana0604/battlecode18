

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

     static PathfinderNode[][][][] Nodes;
    static int W;
    static int H;

    private static void bfs(int a,int b){
        try {
            Nodes[a][b] = new PathfinderNode[W][H];

            PriorityQueue<Integer> queue = new PriorityQueue<>();

            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    Nodes[a][b][x][y] = new PathfinderNode(8, Const.INF);
                }
            }

            Nodes[a][b][a][b].dist = 0;
            queue.add((a << AUX) | b);

            while (queue.size() > 0) {
                int data = queue.poll();
                int myPosX = (data >> AUX) & base;
                int myPosY = data & base;
                double dist = ((double) (data >> AUX2)) / distFactor;
                for (int i = 0; i < X.length; ++i) {
                    int newPosX = myPosX + X[i];
                    int newPosY = myPosY + Y[i];
                    double newDist = dist + dists[i];
                    int parsedDist = (int) Math.round(distFactor * newDist);
                    if (newPosX >= W || newPosX < 0 || newPosY >= H || newPosY < 0) continue;
                    if (newDist < Nodes[a][b][newPosX][newPosY].dist) {
                        if (passable[newPosX][newPosY]) queue.add((((parsedDist << AUX) | newPosX) << AUX) | newPosY);

                        Nodes[a][b][newPosX][newPosY].dist = newDist;
                        if (newDist < 1.8) Nodes[a][b][newPosX][newPosY].dir = i;
                        else Nodes[a][b][newPosX][newPosY].dir = Nodes[a][b][myPosX][myPosY].dir;
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    static PathfinderNode getNode(int x1, int y1, int x2, int y2){
        try{
            if(Nodes[x1][y1] == null) bfs(x1, y1);
            return Nodes[x1][y1][x2][y2];
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public PathfinderNode getNode(AuxMapLocation loc1, AuxMapLocation loc2) {
        try {
            return getNode(loc1.x, loc1.y, loc2.x, loc2.y);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
