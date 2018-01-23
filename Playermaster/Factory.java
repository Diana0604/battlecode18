

import bc.UnitType;

import java.util.HashMap;

public class Factory {

    //private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Factory instance = null;
    static ConstructionQueue queue;
    int units;

    AuxUnit unit;

    static int maxRangers;
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
        if (typeCount.get(UnitType.Worker) == 0) return UnitType.Worker; //potser millor si workers < 2-3?

        int rangers = typeCount.get(UnitType.Ranger);
        int healers = typeCount.get(UnitType.Healer);

        if (3 * healers < rangers) return UnitType.Healer;
        if (rangers < maxRangers)  return UnitType.Ranger;
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
/*
    void build(AuxUnit unit){
        try {
            if (queue.needsUnit(UnitType.Worker) && Wrapper.canProduceUnit(unit, UnitType.Worker)) {
                Wrapper.produceUnit(unit, UnitType.Worker);
                queue.requestUnit(UnitType.Worker, false);
                return;
            }
            if (queue.needsUnit(UnitType.Ranger) && Wrapper.canProduceUnit(unit, UnitType.Ranger)) {
                Wrapper.produceUnit(unit, UnitType.Ranger);
                queue.requestUnit(UnitType.Ranger, false);
                return;
            }
            if (queue.needsUnit(UnitType.Mage) && Wrapper.canProduceUnit(unit, UnitType.Mage)) {
                Wrapper.produceUnit(unit, UnitType.Mage);
                queue.requestUnit(UnitType.Mage, false);
                return;
            }
            if (queue.needsUnit(UnitType.Knight) && Wrapper.canProduceUnit(unit, UnitType.Knight)) {
                Wrapper.produceUnit(unit, UnitType.Knight);
                queue.requestUnit(UnitType.Knight, false);
                return;
            }
            if (queue.needsUnit(UnitType.Healer) && Wrapper.canProduceUnit(unit, UnitType.Healer)) {
                Wrapper.produceUnit(unit, UnitType.Healer);
                queue.requestUnit(UnitType.Healer, false);
                return;
            }

            if (mustSaveMoney()) return;

            if (Data.mageDMG > 100) maxRangers = diag;

            UnitType type = UnitType.Ranger;
            if (Data.rangers > 3 * (Data.healers + 1)) {
                type = UnitType.Healer;
            }

            /*if (Data.round <= 170 && Data.rangers > 4 * (Data.knights + 1)) {
                type = UnitType.Knight;
            }

            if (type == UnitType.Ranger && Data.rangers > maxRangers ){
                type = UnitType.Mage;
            }
            if (type == UnitType.Healer && Data.healers > maxRangers/3 + 1 ){
                type = UnitType.Mage;
            }

            if (type == UnitType.Knight && Data.knights > maxRangers/4 + 1 ){
                type = UnitType.Mage;
            }

            if (!Wrapper.canProduceUnit(unit, type)) return;

            if (type == UnitType.Ranger) ++Data.rangers;
            if (type == UnitType.Healer) ++Data.healers;
            if (type == UnitType.Knight) ++Data.knights;

            Wrapper.produceUnit(unit, type);
            //units = (units+1)%4;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
*/
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
