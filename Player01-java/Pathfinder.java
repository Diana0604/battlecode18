import bc*;
import java.util.*;

class Pathfinder{

    final int AUX = 6;
    final int AUX2 = 12;
    final int distFactor = 100;
    final int base = 0x3F;
    final int INF = 1000000000;
    final double sqrt2 = Math.sqrt(2);
    final int[] X = {0, 1, 1, 1, 0, -1, -1, -1};
    final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1};
    final double[] dists = {1, sqrt2, 1, sqrt2, 1, sqrt2, 1, sqrt2};


    PathfinderNode[][][][] EarthDirs;
    PathfinderNode[][][][] MarsDirs;
    PlanetMap EarthMap = GameMap.getEarth_map();
    PlanetMap MarsMap = GameMap.getMars_map();
    int WE = EarthMap.getWidth();
    int HE = EarthMap.getHeight();
    int WM = MarsMap.getWidth();
    int HM = MarsMap.getHeight();


    Pathfinder(){
        EarthDirs = new PathfinderNode[WE][HE][WE][HE];
        MarsDirs = new PathfinderNode[WE][HE][WE][HE];

        for(int xe = 0; xe < WE; ++xe){
            for(int ye = 0; ye < HE; ++ye){
                bfsEarth(xe,ye);
            }
        }


        for(int me = 0; me < WM; ++xm){
            for(int ym = 0; ym < HM; ++ym){
                bfsMars(xm,ym);
            }
        }
    }



    private void bfsEarth(x, y){
        PriorityQueue<Integer> queue = new PriorityQueue<Integer>();

        for(int xe = 0; xe < WE; ++xe){
            for(int ye = 0; ye < HE; ++ye){
                EarthDirs[x][y][xe][ye] = new PathfinderNode(Direction.Center, INF);
            }
        }

        EarthDirs[x][y].dist = 0;
        queue.add((x << AUX)&y);

        while(queue.size() > 0){
            int myPos = queue.poll();
            int myPosX = (myPos >> AUX)&base;
            int myPosY = myPos&base;
            int dist = (myPos >> AUX2);
            for(int i = 0; i < X.length; ++i){
                int newPosX = myPosX + X[i];
                int newPosY = myPosY + Y[i];
                int newDist = dist + dists[i];
                if (EarthDirs[newPosX][newPosY] == INF){
                    queue.add((((newDist << AUX) & newPosX) << AUX) & newPosY);
                }
            }
        }
    }
}