public class BugPathfindingData {
    public AuxMapLocation obstacle = null;
    public AuxMapLocation target = null;
    public boolean left = true;
    public double minDist = Const.INFS;

    void reset(){
        target = null;
        obstacle = null;
        minDist = Const.INFS;
    }

    void soft_reset(AuxMapLocation m){
        obstacle = null;
        if (target != null) minDist = m.distanceBFSTo(target);
    }



}
