

import bc.UnitType;

import java.util.HashMap;

public class Factory {

    //private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Factory instance = null;
    static ConstructionQueue queue;
    int units;

    static final int ROCKET_RUSH = 500;

    AuxUnit unit;

    static int maxRangers;
    static int maxUnits;
    private static int diag;

    static Factory getInstance(){
        if (instance == null){
            instance = new Factory();
        }
        return instance;
    }

    public Factory(){
        queue = Data.queue;
        units = 0;

        //rangers + healers + mags + knights
        //falta canviar-ho si hem aniquilat l'enemic
        diag = (int) Math.sqrt(Data.H*Data.H + Data.W*Data.W);
        maxRangers = (int) (1.25*diag);
    }


    void play(AuxUnit _unit){
        try {
            unit = _unit;
            if (!unit.getIsBuilt()) return;
            checkGarrison(unit);
            tryBuild();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void checkGarrison(AuxUnit unit){
        try {
            Danger.computeDanger(unit);
            for (int i = 0; i < 9; ++i) {
                //if (Danger.DPS[i] > 0) continue;
                if (Wrapper.canUnload(unit, i)) {
                    Wrapper.unload(unit, i);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void tryBuild(){
        if (!unit.canAttack()) return;
        if (unit.getGarrisonUnits().size() >= 8) return;

        UnitType type = chooseNextUnit();
        build(type);
    }

    private UnitType chooseNextUnit(){
        if (Data.getKarbonite() < 30) return null;
        HashMap<UnitType, Integer> typeCount = Data.unitTypeCount;
        int rangers = typeCount.get(UnitType.Ranger);
        int healers = typeCount.get(UnitType.Healer);
        int mages = typeCount.get(UnitType.Mage);
        int workers = typeCount.get(UnitType.Worker);

        if (workers < 2) return UnitType.Worker; //potser millor si workers < 2-3?

        int roundsEnemyUnseen = Data.round - Data.lastRoundEnemySeen;
        if ((Data.round > 250 && roundsEnemyUnseen > 10) || Data.round >= ROCKET_RUSH) {
            if (workers < 5) return UnitType.Worker;
            if (rangers + healers + mages > 30) return null;
        }

        if (3 * healers < rangers-1) return UnitType.Healer;
        if (rangers < maxRangers)  return UnitType.Ranger;
        int extra_healers = healers - (rangers/3);
        if (extra_healers > 5*mages) return UnitType.Mage;
        if (healers < 1.25*rangers) return UnitType.Healer;
        return UnitType.Mage;
    }

    void build(UnitType type){
        if (type == null) return;
        if (Wrapper.canProduceUnit(unit, type)){
            Wrapper.produceUnit(unit, type);
            //System.out.println("Built " + type);
            Data.unitTypeCount.put(type,Data.unitTypeCount.get(type) + 1);
        }
    }

    private UnitType[] types = {UnitType.Worker, UnitType.Knight, UnitType.Ranger, UnitType.Mage, UnitType.Healer, UnitType.Rocket, UnitType.Factory};

    private boolean mustSaveMoney(){
        try {
            int money = 0;
            for (UnitType type : types) {
                if (Data.queue.needsUnit(type)) money += Wrapper.cost(type);
            }
            return (Data.getKarbonite() < money + 20);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
