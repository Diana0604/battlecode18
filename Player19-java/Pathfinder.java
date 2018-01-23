

import bc.*;
import java.util.*;

public class Pathfinder{

    private static Pathfinder pathfinderInstance = null;

    static Pathfinder getInstance(){
        if (pathfinderInstance == null){
            pathfinderInstance = new Pathfinder();

        }
        return pathfinderInstance;
    }

    private final int AUX = 6;
    private final int AUX2 = 12;
    private final double distFactor = 100;
    private final int base = 0x3F;
    public static final int INF = 1000000000;
    private final double sqrt2 = Math.sqrt(2);
    private final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    private final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};
    private final double[] dists = {1, sqrt2, 1, sqrt2, 1, sqrt2, 1, sqrt2};
    static boolean[][] accessible;
    static double [][] distToWalls;




    private PathfinderNode[][][][] Nodes;
    int W;
    int H;

    public Pathfinder(){
        try {
            W = Data.W;
            H = Data.H;

            Nodes = new PathfinderNode[W][H][W][H];
            accessible = new boolean[W][H];

            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    if (Data.planetMap.isPassableTerrainAt(new MapLocation(Data.planet, x, y)) > 0) {
                        accessible[x][y] = true;
                    } else accessible[x][y] = false;
                }
            }

            Data.accessible = accessible;

            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    //add initial karbonite
                    long a = Data.planetMap.initialKarboniteAt(new MapLocation(Data.planet, x, y));
                    if (a > INF) a = INF;
                    if (a > 0) addMine(x, y, (int) a);
                    //bfs
                    bfs(x, y);
                }
            }
            computeDistToWalls();

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void addMine(int x, int y, int q) {
        try {
            Data.putValue(x, y, q);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private  void bfs(int a,int b){
        try {
            PriorityQueue<Integer> queue = new PriorityQueue<Integer>();

            for (int x = 0; x < W; ++x) {
                for (int y = 0; y < H; ++y) {
                    Nodes[a][b][x][y] = new PathfinderNode(8, INF);
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
                        if (accessible[newPosX][newPosY]) queue.add((((parsedDist << AUX) | newPosX) << AUX) | newPosY);

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

    private void computeDistToWalls(){
        try {
            PriorityQueue<Integer> queue = new PriorityQueue<Integer>();

            distToWalls = new double[W][H];
            for (int i = 0; i < W; ++i) {
                for (int j = 0; j < H; ++j) {
                    if (!accessible[i][j]) {
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


    public PathfinderNode getNode(int x1, int y1, int x2, int y2){
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
