

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

    static int savings;

    static int constructedKnights = 0, constructedMages = 0;

    static int[] roundsLeft;

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

    void initTurn(){
        roundsLeft = new int[16];
    }

    void computeSavings(){
        savings = 0;
        int waste = -10;
        for (int i = 0; i < 16; ++i){
            waste += 10;
            waste -= 40*Factory.roundsLeft[i];
            savings = Math.max(savings, -waste);
        }
    }


    void play(AuxUnit _unit){
        try {
            unit = _unit;
            if (!unit.isBuilt()) return;
            checkGarrison(unit);
            tryBuild();
            //++roundsLeft[Wrapper.factoryRoundsLeft(_unit)];
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

            MovementManager.getInstance().setData(garrisonUnit);
            int dir = MovementManager.getInstance().greedyMove(garrisonUnit);
            if (dir != 8){
               if (Wrapper.canUnload(unit, dir)){
                   Wrapper.unload(unit, dir);
                   checkGarrison(unit);
                   return;
               }
            }

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
            int karbo = Utils.karbonite;
            //if (karbo < Const.replicateCost) return null;

            //prioritzem fer rockets a units
            if (Build.canBuildRockets && karbo < Const.replicateCost + Const.rocketCost && Build.rocketRequest != null) return null;


            HashMap<UnitType, Integer> typeCount = Units.unitTypeCount;
            int rangers = typeCount.get(UnitType.Ranger);
            int healers = typeCount.get(UnitType.Healer);
            int mages   = typeCount.get(UnitType.Mage);
            int workers = typeCount.get(UnitType.Worker);
            int knights = typeCount.get(UnitType.Knight);

            if (workers < 2) return UnitType.Worker; //potser millor si workers < 2-3?

            boolean mageDetected = false;
            boolean shouldBuildKnight = false;
            AuxUnit[] enemies = Wrapper.senseUnits(unit.getMapLocation(), 50, false);
            for (int i = 0; i < enemies.length; ++i){
                if (!shouldBuildKnight && enemies[i].getType() != UnitType.Worker && enemies[i].getMapLocation().distanceBFSTo(unit.getMapLocation()) <= 5) shouldBuildKnight = true;
                if (enemies[i].getType() == UnitType.Mage) mageDetected = false;
            }

            if(shouldBuildKnight && !mageDetected) return UnitType.Knight;

            int minRushtroops = 0;
            if (Build.initDistToEnemy <= 20) minRushtroops = 6;
            else if (Build.initDistToEnemy <= 25) minRushtroops = 5;

            if (constructedMages + constructedKnights < minRushtroops){
                if (constructedKnights > constructedMages) return UnitType.Mage;
                return UnitType.Knight;
            }

            //int rushKnights = 0;
            //if (Build.initDistToEnemy <= 25) rushKnights = 5;

            //if (constructedKnights < rushKnights) return UnitType.Knight;

            //if (Danger.knightSeen && mages == 0) return UnitType.Mage;

            int roundsEnemyUnseen = Utils.round - Build.lastRoundEnemySeen;
            if ((Utils.round > 250 && roundsEnemyUnseen > 10) || Utils.round >= ROCKET_RUSH) {
                //focus only on getting to mars
                if (workers < 3) return UnitType.Worker;
                if (rangers + healers + mages +knights > 30 && (rangers+healers+mages+knights) > 8*typeCount.get(UnitType.Rocket)) return null;
            }


            int totalTroops = knights + rangers + mages;
            if (totalTroops < 4) return UnitType.Ranger;

            if (2*healers < totalTroops-1) return UnitType.Healer;
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
                if (type == UnitType.Knight) ++constructedKnights;
                if (type == UnitType.Mage) ++constructedMages;
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
