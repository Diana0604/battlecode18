import bc.*;
import java.util.ArrayList;
import java.util.ListIterator;

public class UnitManager{

    static UnitManager instance;
    public static GameController gc;
    public static PlanetMap map;
    public static Unit currentUnit;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

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
            if (gc.canSenseLocation(new MapLocation(gc.planet(), x, y))){
                long q = gc.karboniteAt(new MapLocation(gc.planet(), x, y));
                if (q > INF) q = INF;
                if (q > 0){
                    if (q != Qmines.get(i)) Qmines.set(i, (int)q);
                }
                else{
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
            currentUnit = unit;
            if (unit.unitType() == UnitType.Worker) {
                Worker.getInstance().play();
            }
        }
    }


    /*AUX FUNCTIONS*/

    Direction dirTo(int destX, int destY){
        MapLocation myLoc = currentUnit.location().mapLocation();
        PathfinderNode myNode = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }

    Direction dirTo(MapLocation loc){
        return dirTo(loc.getX(), loc.getY());
    }

    void moveTo(MapLocation target){ //todo: edge cases
        if (!gc.isMoveReady(currentUnit.id())) return;
        Direction dir = dirTo(target);
        if (gc.canMove(currentUnit.id(), dir)) {
            gc.moveRobot(currentUnit.id(), dir);
            return;
        }
        MapLocation myLoc = currentUnit.location().mapLocation();
        dir = null;
        double mindist = Pathfinder.getInstance().getNode(myLoc.getX() ,myLoc.getY() , target.getX(), target.getY()).dist;
        for (int i = 0; i < allDirs.length; ++i){
            if (!gc.canMove(currentUnit.id(), allDirs[i])) continue;
            MapLocation newLoc = myLoc.add(allDirs[i]);
            PathfinderNode node = Pathfinder.getInstance().getNode(newLoc.getX(), newLoc.getY(), target.getX(), target.getY());
            if (node.dist < mindist){
                mindist = node.dist;
                dir = allDirs[i];
            }
        }
        if (dir != null) gc.moveRobot(currentUnit.id(), dir);
    }

}