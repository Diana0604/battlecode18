

import bc.*;
import java.util.*;

public class Pathfinder{
    static boolean[][] passable; //si no hi ha muntanyes

    private static final int AUX = 6;
    private static final int AUX2 = 12;
    private static final double distFactor = 100;
    private static final int base = 0x3F;
    //private final double sqrt2 = Math.sqrt(2);
    private static final double sqrt2 = 1; //Todo wtf
    private static final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final double[] dists = {1, sqrt2, 1, sqrt2, 1, sqrt2, 1, sqrt2};
    static double [][] distToWalls;

    private static PathfinderNode[][][][] Nodes;
    static int W;
    static int H;

    public static void initGame(){
        try {
            W = Mapa.W;
            H = Mapa.H;

            Nodes = new PathfinderNode[W][H][W][H];
            passable = new boolean[W][H];

            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    if (Mapa.planetMap.isPassableTerrainAt(new MapLocation(Mapa.planet, x, y)) > 0) {
                        passable[x][y] = true;
                    } else passable[x][y] = false;
                }
            }

            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    //bfs
                    bfs(x, y);
                }
            }
            computeDistToWalls();

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void bfs(int a,int b){
        try {
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

    private static void computeDistToWalls(){
        try {
            PriorityQueue<Integer> queue = new PriorityQueue<>();

            distToWalls = new double[W][H];
            for (int i = 0; i < W; ++i) {
                for (int j = 0; j < H; ++j) {
                    if (!passable[i][j]) {
                        distToWalls[i][j] = 0;
                        queue.add((i << AUX) | j);
                    } else distToWalls[i][j] = 100000000;
                }
            }

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
                    if (newDist < distToWalls[newPosX][newPosY]) {
                        queue.add((((parsedDist << AUX) | newPosX) << AUX) | newPosY);
                        distToWalls[newPosX][newPosY] = newDist;
                    }
                }
            }

            for (int i = 0; i < W; ++i){
                for (int j = 0; j < H; ++j){
                    distToWalls[i][j] = Math.min(distToWalls[i][j], i+1);
                    distToWalls[i][j] = Math.min(distToWalls[i][j], W-i);
                    distToWalls[i][j] = Math.min(distToWalls[i][j], j+1);
                    distToWalls[i][j] = Math.min(distToWalls[i][j], H-j);
                    //System.err.print(Math.min(distToWalls[i][j], 9));
                }
                //System.err.println();
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    static PathfinderNode getNode(int x1, int y1, int x2, int y2){
        try{
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
