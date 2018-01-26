import bc.*;

/**
 * Created by Ivan on 1/19/2018.
 */
public class AuxMapLocation {

    static final int[] X = {0, 1, 1, 1, 0, -1, -1, -1, 0};
    static final int[] Y = {1, 1, 0, -1, -1, -1, 0, 1, 0};

    public int x;
    public int y;

    public AuxMapLocation(MapLocation mloc){
        try {
            x = mloc.getX();
            y = mloc.getY();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public AuxMapLocation(int _x, int _y){
        x = _x;
        y = _y;
    }


    public AuxMapLocation(int a){
        x = a >> 12;
        y = a & 0xFFF;
    }

    public int encode(){
        return ((x << 12) | y);
    }

    public int encode(int _x, int _y){
        return ((_x << 12) | _y);
    }

    public AuxMapLocation add(int dir){
        try {
            return new AuxMapLocation(x + X[dir], y + Y[dir]);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int distanceSquaredTo(AuxMapLocation mloc){
        try {
            int dx = mloc.x - x;
            int dy = mloc.y - y;
            return dx * dx + dy * dy;
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    public boolean isOnMap(){
        try {
            if (x < 0 || x >= Mapa.W) return false;
            if (y < 0 || y >= Mapa.H) return false;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int distanceBFSTo(AuxMapLocation mloc) {
        try {
            return Pathfinder.getDist(x, y, mloc.x, mloc.y);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int dirBFSTo (AuxMapLocation mloc) {
        try {
            return Pathfinder.getDir(x, y, mloc.x, mloc.y);
        } catch (Exception e) {
            e.printStackTrace();
            return 8;
        }
    }

    public AuxMapLocation add(AuxMapLocation loc){
        try {
            return new AuxMapLocation(loc.x + this.x, loc.y + this.y);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getKarbonite(){
        try {
            if (!isOnMap()) return 0;
            return Karbonite.karboMap[x][y];
        } catch (Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }


    public AuxUnit getUnit(){
        try {
            if (x < 0 || x >= Mapa.W) return null;
            if (y < 0 || y >= Mapa.H) return null;
            int i = Units.unitMap[x][y];
            if (i > 0) return Units.myUnits.get(i-1);
            if (i < 0) return Units.enemies.get(-(i + 1));
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public AuxUnit getUnit(boolean myTeam){
        try {
            AuxUnit unit = getUnit();
            if (unit == null) return null;
            if (unit.myTeam == myTeam) return unit;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //si es pot passar pel terreny (no hi ha muntanya)
    public boolean isPassable(){
        return Pathfinder.passable[x][y];
    }

    //si la posicio esta ocupada per una unitat
    public boolean isOccupiedByUnit(){
        return Units.unitMap[x][y] != 0;
    }

    //si no hi ha res a la posicio
    public boolean isAccessible(){
        if (!isOnMap()) return false;
        if (!isPassable()) return false;
        if (isOccupiedByUnit()) return false;
        if (Units.newOccupiedPositions.contains(encode())) return false;
        return true;
    }

    public boolean isDangerousForWorker(){
        return (Danger.myDist[x][y] -10 < Danger.enemyDist[x][y]);
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
            e.printStackTrace();
            return false;
        }
    }

    public String toString(){
        return "[" + x + "," + y + "]";
    }

}
