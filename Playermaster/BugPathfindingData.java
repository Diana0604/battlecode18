public class BugPathfindingData {
    public AuxMapLocation obstacle = null;
    public AuxMapLocation target = null;
    public boolean left = true;
    public double minDist = Const.INF;

    void reset(){
        target = null;
        obstacle = null;
        minDist = Const.INF;
    }

    void soft_reset(AuxMapLocation m){
        obstacle = null;
        if (target != null) minDist = m.distanceBFSTo(target);
    }



}
