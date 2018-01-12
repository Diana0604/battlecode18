import bc.*;

public class Factory {

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Factory instance = null;
    static GameController gc;

    boolean wait;

    static Factory getInstance(){
        if (instance == null){
            instance = new Factory();
            gc = UnitManager.gc;
        }
        return instance;
    }

    void play(Unit unit){
        wait = false;
        checkGarrison(unit);
        if(wait) return;
        build(unit);
    }

    void build(Unit unit){
        if(unit.structureIsBuilt() == 0 || !gc.canProduceRobot(unit.id(), UnitType.Ranger)) return;
        gc.produceRobot(unit.id(),UnitType.Ranger);
    }

    void checkGarrison(Unit unit){
        for(int i = 0; i < allDirs.length; ++i){
            if(gc.canUnload(unit.id(), allDirs[i])) {
                wait = true;
                gc.unload(unit.id(), allDirs[i]);
            }
        }
    }
}