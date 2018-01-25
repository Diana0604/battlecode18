

import bc.UnitType;

import java.util.ArrayList;
import java.util.HashMap;

public class Factory {
    static Factory instance = null;
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
        units = 0;

        //rangers + healers + mags + knights
        //falta canviar-ho si hem aniquilat l'enemic
        diag = (int) Math.sqrt(Mapa.H* Mapa.H + Mapa.W* Mapa.W);
        maxRangers = (int) (1.25*diag);
    }


    void play(AuxUnit _unit){
        try {
            unit = _unit;
            if (!unit.isBuilt()) return;
            checkGarrison(unit);
            tryBuild();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void checkGarrison(AuxUnit unit){
        try {
            Danger.computeDanger(unit);
            ArrayList<Integer> units = unit.getGarrisonUnits();
            if (units.size() == 0) return;
            int bestDir = -1;
            double bestDist = 10000000;
            AuxUnit garrisonUnit = Units.getUnitByID(units.get(0));
            AuxMapLocation target = garrisonUnit.target;
            if (target == null) target = unit.getMapLocation();
            for (int j = 0; j < 8; ++j) {
                if (Wrapper.canUnload(unit, j)) {
                    AuxMapLocation newLoc = unit.getMapLocation().add(j);
                    double d = newLoc.distanceBFSTo(target);
                    if (d < bestDist) {
                        bestDist = d;
                        bestDir = j;
                    }
                }
            }

            if (bestDir != -1){
                Wrapper.unload(unit, bestDir);
                checkGarrison(unit);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void tryBuild(){
        try {
            if (!unit.canAttack()) return;
            if (unit.getGarrisonUnits().size() >= 8) return;

            UnitType type = chooseNextUnit();
            build(type);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private UnitType chooseNextUnit(){
        try {
            if (Utils.karbonite < 30) return null;
            HashMap<UnitType, Integer> typeCount = Units.unitTypeCount;
            int rangers = typeCount.get(UnitType.Ranger);
            int healers = typeCount.get(UnitType.Healer);
            int mages = typeCount.get(UnitType.Mage);
            int workers = typeCount.get(UnitType.Worker);

            if (workers < 2) return UnitType.Worker; //potser millor si workers < 2-3?

            int roundsEnemyUnseen = Utils.round - Units.lastRoundEnemySeen;
            if ((Utils.round > 250 && roundsEnemyUnseen > 10) || Utils.round >= ROCKET_RUSH) {
                if (workers < 5) return UnitType.Worker;
                if (rangers + healers + mages > 30) return null;
            }

            if (3 * healers < rangers - 1) return UnitType.Healer;
            if (rangers < maxRangers) return UnitType.Ranger;
            if (healers < 1.25 * rangers) return UnitType.Healer;
            return UnitType.Mage;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void build(UnitType type){
        try {
            if (type == null) return;
            if (Wrapper.canProduceUnit(unit, type)) {
                Wrapper.produceUnit(unit, type);
                //System.out.println("Built " + type);
                Units.unitTypeCount.put(type, Units.unitTypeCount.get(type) + 1);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private UnitType[] types = {UnitType.Worker, UnitType.Knight, UnitType.Ranger, UnitType.Mage, UnitType.Healer, UnitType.Rocket, UnitType.Factory};

    private boolean mustSaveMoney(){
        try {
            int money = 0;
            for (UnitType type : types) {
                //if (Units.queue.needsUnit(type)) money += Units.getCost(type);
            }
            return (Utils.karbonite < money + 20);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
