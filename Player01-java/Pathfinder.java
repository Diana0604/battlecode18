import bc.*;
import java.util.*;

public class Pathfinder{

    private static Pathfinder pathfinderInstance = null;

    static Pathfinder getInstance(GameController _gc){
        if (pathfinderInstance == null) pathfinderInstance = new Pathfinder(_gc);
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


    private PathfinderNode[][][][] EarthDirs;
    private PathfinderNode[][][][] MarsDirs;
    private PlanetMap EarthMap;
    private PlanetMap MarsMap;
    private int WE;
    private int HE;
    private int WM;
    private int HM;


    public Pathfinder(GameController _gc){
        gc = _gc;
        EarthMap = gc.startingMap(Planet.Earth);
        MarsMap = gc.startingMap(Planet.Mars);
        WE = (int)EarthMap.getWidth();
        HE = (int)EarthMap.getHeight();
        WM = (int)MarsMap.getWidth();
        HM = (int)MarsMap.getHeight();

        EarthDirs = new PathfinderNode[WE][HE][WE][HE];
        MarsDirs = new PathfinderNode[WM][HM][WM][HM];

        for(int xe = 0; xe < WE; ++xe){
            for(int ye = 0; ye < HE; ++ye){
                bfsEarth(xe,ye);
            }
        }


        for(int xm = 0; xm < WM; ++xm){
            for(int ym = 0; ym < HM; ++ym){
                bfsMars(xm,ym);
            }
        }
    }



    private  void bfsEarth(int x,int y){
        PriorityQueue<Integer> queue = new PriorityQueue<Integer>();

        for(int xe = 0; xe < WE; ++xe){
            for(int ye = 0; ye < HE; ++ye){
                EarthDirs[x][y][xe][ye] = new PathfinderNode(Direction.Center, INF);
            }
        }

        EarthDirs[x][y][x][y].dist = 0;
        queue.add((x << AUX) | y);

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
                if(newPosX >= WE || newPosX < 0 || newPosY >= HE || newPosY < 0) continue;
                if(newDist < EarthDirs[x][y][newPosX][newPosY].dist) {
                    if (EarthDirs[x][y][newPosX][newPosY].dist == INF){
                        if(EarthMap.isPassableTerrainAt(new MapLocation(Planet.Earth, newPosX, newPosY)) > 0) queue.add((((parsedDist << AUX) | newPosX) << AUX) | newPosY);
                    }
                    EarthDirs[x][y][newPosX][newPosY].dist = newDist;
                    if(newDist < 1.8) EarthDirs[x][y][newPosX][newPosY].dir = allDirs[i];
                    else EarthDirs[x][y][newPosX][newPosY].dir = EarthDirs[x][y][myPosX][myPosY].dir;
                }
            }
        }
    }

    private  void bfsMars(int x,int y){
        PriorityQueue<Integer> queue = new PriorityQueue<Integer>();

        for(int xm = 0; xm < WE; ++xm){
            for(int ym = 0; ym < HE; ++ym){
                MarsDirs[x][y][xm][ym] = new PathfinderNode(Direction.Center, INF);
            }
        }

        MarsDirs[x][y][x][y].dist = 0;
        queue.add((x << AUX)&y);

        while(queue.size() > 0){
            int myPos = queue.poll();
            int myPosX = (myPos >> AUX)&base;
            int myPosY = myPos&base;
            double dist = ((double)(myPos >> AUX2))/distFactor;
            for(int i = 0; i < X.length; ++i){
                int newPosX = myPosX + X[i];
                int newPosY = myPosY + Y[i];
                double newDist = dist + dists[i];
                int parsedDist = (int)Math.round(distFactor*newDist);
                if(newPosX >= WM || newPosX < 0 || newPosY >= HM || newPosY < 0) continue;
                if(newDist < MarsDirs[x][y][newPosX][newPosY].dist) {
                    if (MarsDirs[x][y][newPosX][newPosY].dist == INF){
                        if(MarsMap.isPassableTerrainAt(new MapLocation(Planet.Mars, newPosX, newPosY)) > 0) queue.add((((parsedDist << AUX) & newPosX) << AUX) & newPosY);
                    }
                    MarsDirs[x][y][newPosX][newPosY].dist = newDist;
                    if(newDist < 1.8) MarsDirs[x][y][newPosX][newPosY].dir = allDirs[i];
                    else MarsDirs[x][y][newPosX][newPosY].dir = MarsDirs[x][y][myPosX][myPosY].dir;
                }
            }
        }
    }

    public PathfinderNode getNode(Planet pl, int x1, int y1, int x2, int y2){
        if(pl.equals(Planet.Earth)) return EarthDirs[x1][y1][x2][y2];
        return MarsDirs[x1][y1][x2][y2];
    }
}