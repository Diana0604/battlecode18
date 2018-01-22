

import bc.UnitType;

import java.util.HashMap;

public class Factory {

    //private final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static Factory instance = null;
    static ConstructionQueue queue;
    int units;

    AuxUnit unit;

    private static int maxUnits;
    static int maxRangers = 50;

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
        maxUnits = 3 * (int)(Math.sqrt(2)*Math.max(Data.W, Data.H));
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
        if (Data.unitTypeCount.get(UnitType.Worker) == 0) return UnitType.Worker; //potser millor si workers < 2-3?
        //rangers : healers : mages : knights
        int[] ratio1 = {3, 1, 0, 0}; //before mages1
        int[] ratio2 = {9, 4, 2, 0}; //after mages1, before mages2
        int[] ratio3 = {9, 4, 9, 0}; //after mages2 (<175 turns to blink)
        int[] idealRatio;
        int mageLevel = (int) Data.researchInfo.getLevel(UnitType.Mage);
        if (mageLevel < 1) idealRatio = ratio1;
        else if (mageLevel < 2) idealRatio = ratio2;
        else idealRatio = ratio3;


        HashMap<UnitType, Integer> typeCount = Data.unitTypeCount;
        UnitType[] types = {UnitType.Ranger, UnitType.Healer, UnitType.Mage, UnitType.Knight};
        int[] counts = {0,0,0,0};
        int totalCount = 0;

        for (int i = 0; i < types.length; i++){
            int c = typeCount.get(types[i]);
            counts[i] = c;
            totalCount += c;
        }

        if (totalCount > maxUnits) return null;

        double[] ratios = {0,0,0,0};
        for (int i = 0; i < ratios.length; i++) {
            if (idealRatio[i] == 0) ratios[i] = 100000;
            else ratios[i] = (double)typeCount.get(types[i])/ (double) idealRatio[i];
        }
        double minRatio = 100000;
        int minIndex = -1;
        for (int i = 0; i < ratios.length; i++) {
            if (ratios[i] < minRatio){
                minRatio = ratios[i];
                minIndex = i;
            }
        }
        /*for (int i = 0; i < types.length; i++){
            UnitType type = types[i];
            System.out.println(type + " ideal " + idealRatio[i] + " count " + Data.unitTypeCount.get(types[i]) + " ratio " + ratios[i]);
        }
        System.out.println("Best type: " + types[minIndex]);*/
        return types[minIndex];
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
