package Playermaster;

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
        //if it's still a blueprint return
        if(unit.structureIsBuilt() == 0) return;
        //I don't think we'll actually need a wait for factories
        wait = false;
        checkGarrison(unit);
        build(unit);
    }

    void checkGarrison(Unit unit){
        for(int i = 0; i < allDirs.length; ++i){
            if(gc.canUnload(unit.id(), allDirs[i])) {
                gc.unload(unit.id(), allDirs[i]);
            }
        }
    }

    void build(Unit unit){
        if(!gc.canProduceRobot(unit.id(), UnitType.Ranger)) return;
        gc.produceRobot(unit.id(),UnitType.Ranger);
    }
}
