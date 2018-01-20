

import bc.*;

public class Factory {

    private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Factory instance = null;
    static GameController gc;
    static ConstructionQueue queue;
    int units;

    boolean wait;

    static Factory getInstance(){
        if (instance == null){
            instance = new Factory();
        }
        return instance;
    }

    public Factory(){
        gc = UnitManager.gc;
        queue = Data.queue;
        units = 0;
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
        boolean[] aux = new boolean[9];
        for (int i = 0; i < 9; ++i) aux[i] = true;
        Danger.computeDanger(unit.location().mapLocation(), aux);
        for(int i = 0; i < allDirs.length; ++i){
            if (Danger.DPS[i] > 0) continue;
            if(gc.canUnload(unit.id(), allDirs[i])) {
                gc.unload(unit.id(), allDirs[i]);
            }
        }
    }

    void build(Unit unit){
        int id = unit.id();
        if (queue.needsUnit(UnitType.Worker) && gc.canProduceRobot(id,UnitType.Worker)){
            gc.produceRobot(id, UnitType.Worker);
            queue.requestUnit(UnitType.Worker,false);
        }
        UnitType type = UnitType.Ranger;
        ++Data.rangers;
        if (Data.rangers > 3*(Data.healers+1)){
            type = UnitType.Healer;
            --Data.rangers;
            ++Data.healers;
        }

        if(!gc.canProduceRobot(id, type)) return;
        gc.produceRobot(unit.id(),type);
        //units = (units+1)%4;
    }
}
