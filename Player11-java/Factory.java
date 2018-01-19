

import bc.UnitType;

public class Factory {

    //private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Factory instance = null;
    static ConstructionQueue queue;
    int units;

    static Factory getInstance(){
        if (instance == null){
            instance = new Factory();
        }
        return instance;
    }

    public Factory(){
        queue = Data.queue;
        units = 0;
    }

    void play(AuxUnit unit){
        //if it's still a blueprint return
        if(!unit.getIsBuilt()) return;
        //I don't think we'll actually need a wait for factories
        checkGarrison(unit);
        build(unit);
    }

    void checkGarrison(AuxUnit unit){
        Danger.computeDanger(unit);
        for(int i = 0; i < 9; ++i){
            //if (Danger.DPS[i] > 0) continue;
            if(Wrapper.canUnload(unit, i)) {
                Wrapper.unload(unit, i);
            }
        }
    }

    void build(AuxUnit unit){
        if (queue.needsUnit(UnitType.Worker) && Wrapper.canProduceUnit(unit,UnitType.Worker)){
            Wrapper.produceUnit(unit, UnitType.Worker);
            queue.requestUnit(UnitType.Worker,false);
            return;
        }
        UnitType type = UnitType.Ranger;
        ++Data.rangers;
        if (Data.rangers > 3*(Data.healers+1)){
            type = UnitType.Healer;
            --Data.rangers;
            ++Data.healers;
        }

        if(!Wrapper.canProduceUnit(unit, type)) return;
        Wrapper.produceUnit(unit,type);
        //units = (units+1)%4;
    }
}
