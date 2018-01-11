import bc.*;
import java.util.ArrayList;
import java.util.ListIterator;

public class UnitManager{

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
    }


    public void update(){
        //check mines
        ListIterator<Integer> iterator = Xmines.listIterator();
        if (iterator.hasNext()){
            int i = iterator.nextIndex();
            int x = iterator.next();
            int y = Ymines.get(i);
            if (gc.canSenseLocation(new MapLocation(gc.planet(), x, y))){
                long q = map.initialKarboniteAt(new MapLocation(gc.planet(), x, y));
                if (q > INF) q = INF;
                if (q > 0){
                    if (q != Qmines.get(i)) Qmines.set(i, (int)q);
                }
                else{
                    iterator.remove();
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
        }
    }
}