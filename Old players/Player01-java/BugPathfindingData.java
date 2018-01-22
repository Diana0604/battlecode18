import bc.*;

public class BugPathfindingData {

    static final double INF = 1000000000;

    MapLocation obstacle = null;
    MapLocation target = null;
    boolean left = true;
    double minDist = INF;

    void reset(){
        target = null;
        obstacle = null;
        minDist = INF;
    }

    void soft_reset(MapLocation m){
        obstacle = null;
        if (target != null) minDist = Pathfinder.getInstance().getNode(m.getX(), m.getY(), target.getX(), target.getY()).dist;
    }



}
