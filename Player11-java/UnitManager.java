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
        for (int i = 0; i < Data.myUnits.length; i++) {
            AuxUnit unit = Data.myUnits[i];
            if(unit.getType() == UnitType.Rocket) {
                Rocket.getInstance().playFirst(unit);
            }
        }
        for (int i = 0; i < Data.myUnits.length; i++) {
            AuxUnit unit = Data.myUnits[i];
            if(unit.isInGarrison()) continue;
            if (unit.getType() == UnitType.Worker) {
                Worker.getInstance().play(unit);
            }
            if(unit.getType() == UnitType.Factory) {
                Factory.getInstance().play(unit);
            }
            if(unit.getType() == UnitType.Ranger) {
                Ranger.getInstance().play(unit);
            }
            if(unit.getType() == UnitType.Rocket) {
                Rocket.getInstance().play(unit);
            }
            if(unit.getType() == UnitType.Healer){
                Healer.getInstance().play(unit);
            }
        }
    }
}
