

import bc.*;

class UnitManager{
    private static UnitManager instance;
    static GameController gc;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    UnitManager(){
        Data.initGame(gc);
    }

    void moveUnits(){
        VecUnit units = gc.myUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if(unit.unitType() == UnitType.Rocket) {
                Rocket.getInstance().playFirst(unit);
            }
        }
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            Location myLoc = unit.location();
            if(myLoc.isInGarrison()) continue;
            if (unit.unitType() == UnitType.Worker) {
                Worker.getInstance().play(unit);
            }
            if(unit.unitType() == UnitType.Factory) {
                Factory.getInstance().play(new AuxUnit(unit));
            }
            if(unit.unitType() == UnitType.Ranger) {
                Ranger.getInstance().play(unit);
            }
            if(unit.unitType() == UnitType.Rocket) {
                Rocket.getInstance().play(unit);
            }
            if(unit.unitType() == UnitType.Healer){
                Healer.getInstance().play(unit);
            }
        }
    }
}
