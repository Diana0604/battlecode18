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
        try {
            x = mloc.getX();
            y = mloc.getY();
        }catch(Exception e){
            System.out.println(e);
        }
    }

    public AuxMapLocation(int _x, int _y){
        x = _x;
        y = _y;
    }

    public AuxMapLocation add(int dir){
        try {
            return new AuxMapLocation(x + X[dir], y + Y[dir]);
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public int distanceSquaredTo(AuxMapLocation mloc){
        try {
            int dx = mloc.x - x;
            int dy = mloc.y - y;
            return dx * dx + dy * dy;
        }catch(Exception e) {
            System.out.println(e);
            return Integer.parseInt(null);
        }
    }

    public boolean isOnMap(){
        try {
            if (x < 0 || x >= Data.W) return false;
            if (y < 0 || y >= Data.H) return false;
            return true;
        } catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

    public double distanceBFSTo(AuxMapLocation mloc) {
        try {
            return Pathfinder.getInstance().getNode(x, y, mloc.x, mloc.y).dist;
        } catch (Exception e) {
            System.out.println(e);
            return Double.parseDouble(null);
        }
    }

    public int dirBFSTo (AuxMapLocation mloc) {
        try {
            return Pathfinder.getInstance().getNode(x, y, mloc.x, mloc.y).dir;
        } catch (Exception e) {
            System.out.println(e);
            return Integer.parseInt(null);
        }
    }

    @Override
    public boolean equals(Object o){
        try {
            if (this == o)
                return true;
            if (o == null)
                return false;
            if (getClass() != o.getClass())
                return false;
            AuxMapLocation mloc = (AuxMapLocation) o;
            return (this.x == mloc.x && this.y == mloc.y);
        }catch(Exception e) {
            System.out.println(e);
            return false;
        }
    }

}
