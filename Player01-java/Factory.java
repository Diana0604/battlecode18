import bc.*;

public class Factory {

    static Factory instance = null;
    static GameController gc;

    static Factory getInstance(){
        if (instance == null){
            instance = new Factory();
            gc = UnitManager.gc;
        }
        return instance;
    }

    void play(Unit unit){
        if(unit.structureIsBuilt() == 0) return; //if it's a blueprint return
        build(unit);
    }

    void build(Unit unit){
        if(!gc.canProduceRobot(unit.id(), UnitType.Ranger)) return;
        gc.produceRobot(unit.id(),UnitType.Ranger);
    }
}