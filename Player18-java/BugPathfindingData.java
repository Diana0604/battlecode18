

import bc.*;

public class BugPathfindingData {

    static final double INF = 1000000000;

    public AuxMapLocation obstacle = null;
    public AuxMapLocation target = null;
    public boolean left = true;
    public double minDist = INF;

    void reset(){
        target = null;
        obstacle = null;
        minDist = INF;
    }

    void soft_reset(AuxMapLocation m){
        obstacle = null;
        if (target != null) minDist = m.distanceBFSTo(target);
    }



}
