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

    private GameController gc;

    private final int AUX = 6;
    private final int AUX2 = 12;
    private final double distFactor = 100;
    private final int base = 0x3F;
    private final int INF = 1000000000;
    private final double sqrt2 = Math.sqrt(2);
    private final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    private final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};
    private final double[] dists = {1, sqrt2, 1, sqrt2, 1, sqrt2, 1, sqrt2};
    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest};


    private PathfinderNode[][][][] Nodes;
    private PlanetMap map;
    int W;
    int H;


    public Pathfinder(){
        gc = UnitManager.gc;
        map = gc.startingMap(gc.planet());
        W = (int)map.getWidth();
        H = (int)map.getHeight();

        Nodes  = new PathfinderNode[W][H][W][H];

        for(int x = 0; x < W; ++x){
            for(int y = 0; y < H; ++y){
                //add initial karbonite
                long a = map.initialKarboniteAt(new MapLocation(gc.planet(), x, y));
                if (a > INF) a = INF;
                if (a > 0) UnitManager.addMine(x,y,(int)a);
                //bfs
                bfs(x,y);
            }
        }
    }



    private  void bfs(int a,int b){
        //System.out.println("bfs");
        PriorityQueue<Integer> queue = new PriorityQueue<Integer>();

        for(int x = 0; x < W; ++x){
            for(int y = 0; y < H; ++y){
                Nodes[a][b][x][y] = new PathfinderNode(Direction.Center, INF);
            }
        }

        Nodes[a][b][a][b].dist = 0;
        queue.add((a << AUX) | b);

        while(queue.size() > 0){
            int data = queue.poll();
            int myPosX = (data >> AUX)&base;
            int myPosY = data&base;
            double dist = ((double)(data >> AUX2))/distFactor;
            for(int i = 0; i < X.length; ++i){
                int newPosX = myPosX + X[i];
                int newPosY = myPosY + Y[i];
                double newDist = dist + dists[i];
                int parsedDist = (int)Math.round(distFactor*newDist);
                if(newPosX >= W || newPosX < 0 || newPosY >= H || newPosY < 0) continue;
                if(newDist < Nodes[a][b][newPosX][newPosY].dist) {
                    if(map.isPassableTerrainAt(new MapLocation(gc.planet(), newPosX, newPosY)) > 0) queue.add((((parsedDist << AUX) | newPosX) << AUX) | newPosY);

                    Nodes[a][b][newPosX][newPosY].dist = newDist;
                    if(newDist < 1.8) Nodes[a][b][newPosX][newPosY].dir = allDirs[i];
                    else Nodes[a][b][newPosX][newPosY].dir = Nodes[a][b][myPosX][myPosY].dir;
                    //if(newPosX ==0 && newPosY == 0) System.out.println(Nodes[a][b][0][0].dir);
                }
            }
        }
    }


    public PathfinderNode getNode(int x1, int y1, int x2, int y2){
        return Nodes[x1][y1][x2][y2];
    }

    public PathfinderNode getNode(MapLocation l1, MapLocation l2) {
        return getNode(l1.getX(),l1.getY(),l2.getX(),l2.getY());
    }

    public PathfinderNode getNode(Unit u1, Unit u2){
        return getNode(u1.location().mapLocation(), u2.location().mapLocation());
    }
}