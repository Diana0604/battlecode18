import bc.*;
import java.util.ArrayList;
import java.util.ListIterator;

public class UnitManager{

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static UnitManager instance;
    static GameController gc;
    static PlanetMap map;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    //Stuff available for all units
    static ArrayList<Integer> Xmines; //xpos
    static ArrayList<Integer> Ymines; //ypos
    static ArrayList<Integer> Qmines; //quantity
    int INF = 1000000000;

    static void add(int x, int y, int q) {
        Xmines.add(x);
        Ymines.add(y);
        Qmines.add(q);
    }

    UnitManager(){
        Xmines = new ArrayList<Integer>();
        Ymines = new ArrayList<Integer>();
        Qmines = new ArrayList<Integer>();
        map = gc.startingMap(gc.planet());
        Pathfinder.getInstance();
    }


    public void update(){
        //check mines
        for(int i = Xmines.size() - 1; i >= 0; --i){
            int x = Xmines.get(i);
            int y = Ymines.get(i);
            //if(x == 1 && y ==1) System.out.println("estem a la 1");
            if (gc.canSenseLocation(new MapLocation(gc.planet(), x, y))){
                long q = gc.karboniteAt(new MapLocation(gc.planet(), x, y));
                if (q > INF) q = INF;
                if (q > 0){
                    if (q != Qmines.get(i)) Qmines.set(i, (int)q);
                }
                else{
                    //if(x == 1 && y == 1) System.out.println("removed");
                    Xmines.remove(i);
                    Ymines.remove(i);
                    Qmines.remove(i);
                }
            }
        }
        return;
    }

    public void moveUnits(){
        VecUnit units = gc.myUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.unitType() == UnitType.Worker) {
                Worker.getInstance().play(unit);
            }
            if(unit.unitType() == UnitType.Factory) {
                Factory.getInstance().play(unit);
            }
        }
    }

    Direction dirTo(Unit unit, int destX, int destY) {
        MapLocation myLoc = unit.location().mapLocation();
        PathfinderNode myNode = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }

    Direction dirTo(Unit unit, MapLocation loc) {
        return dirTo(unit, loc.getX(), loc.getY());
    }

    void moveTo(Unit unit, MapLocation target) { //todo: edge cases
        if (!gc.isMoveReady(unit.id())) return;
        Direction dir = dirTo(unit, target);
        if (gc.canMove(unit.id(), dir)) {
            gc.moveRobot(unit.id(), dir);
            return;
        }
        MapLocation myLoc = unit.location().mapLocation();
        dir = null;
        double mindist = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , target.getX(), target.getY()).dist;
        for (int i = 0; i < allDirs.length; ++i){
            if (!gc.canMove(unit.id(), allDirs[i])) continue;
            MapLocation newLoc = myLoc.add(allDirs[i]);
            PathfinderNode node = Pathfinder.getInstance().getNode(newLoc.getX(), newLoc.getY(), target.getX(), target.getY());
            if (node.dist < mindist){
                mindist = node.dist;
                dir = allDirs[i];
            }
        }
        if (dir != null) gc.moveRobot(unit.id(), dir);
    }
}