import bc.*;

/**
 * Created by Ivan on 1/19/2018.
 */
public class AuxMapLocation {

    static final int[] X = {0, 1, 1, 1, 0, -1, -1, -1, 0};
    static final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1, 0};
    //private static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};


    public int x;
    public int y;

    public AuxMapLocation(MapLocation mloc){
        x = mloc.getX();
        y = mloc.getY();
    }

    public AuxMapLocation(int _x, int _y){
        x = _x;
        y = _y;
    }

    public AuxMapLocation add(int dir){
        return new AuxMapLocation(x + X[dir], y+Y[dir]);
    }

    public int distanceSquaredTo(AuxMapLocation mloc){
        int dx = mloc.x - x;
        int dy = mloc.y - y;
        return dx*dx + dy*dy;
    }

    public boolean isOnMap(){
        if (x < 0 || x >= Data.W) return false;
        if (y < 0 || y >= Data.H) return false;
        return true;
    }

    public double distanceBFSTo(AuxMapLocation mloc){
        return Pathfinder.getInstance().getNode(x, y, mloc.x, mloc.y).dist;
    }

    public int dirBFSTo (AuxMapLocation mloc){
        return Pathfinder.getInstance().getNode(x, y, mloc.x, mloc.y).dir;
    }

    @Override
    public boolean equals(Object o){
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        AuxMapLocation mloc = (AuxMapLocation) o;
        return (this.x == mloc.x && this.y == mloc.y);
    }

}
