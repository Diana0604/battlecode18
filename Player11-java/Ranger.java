

import bc.UnitType;

import java.util.HashMap;

public class Ranger {
    final long INFL = 1000000000;
    final int INF = 1000000000;
    final double eps = 0.001;
    static Ranger instance = null;
    private UnitManager unitManager;

    HashMap<Integer, Integer> objectiveArea;

    static Ranger getInstance(){
        if (instance == null){
            instance = new Ranger();
        }
        return instance;
    }

    public Ranger(){
        unitManager = UnitManager.getInstance();
        objectiveArea = new HashMap();
    }

    void play(AuxUnit unit){
        attack(unit);
        move(unit);
        attack(unit);
    }

    AuxUnit getBestAttackTarget(AuxUnit A, AuxUnit B){
        if (A == null) return B;
        if (B == null) return A;
        if (A.getType() == B.getType()){
            if (A.getHealth() < B.getHealth()) return A;
            return B;
        }
        if (A.getType() == UnitType.Ranger) return A;
        if (B.getType() == UnitType.Ranger) return B;
        if (A.getType() == UnitType.Mage) return A;
        if (B.getType() == UnitType.Mage) return B;
        if (A.getType() == UnitType.Healer) return A;
        if (B.getType() == UnitType.Healer) return B;
        if (A.getType() == UnitType.Knight) return A;
        if (B.getType() == UnitType.Knight) return B;
        if (A.getType() == UnitType.Worker) return A;
        if (B.getType() == UnitType.Worker) return B;
        return B;
    }

    void attack(AuxUnit unit) {
        AuxUnit bestVictim = null;
        if(!unit.canAttack()) return;
        AuxMapLocation myLoc = unit.getMaplocation();
        AuxUnit[] canAttack = Wrapper.senseUnits(myLoc.x, myLoc.y, Wrapper.getAttackRange(unit.getType()), false);
        for(int i = 0; i < canAttack.length; ++i){
            AuxUnit victim = canAttack[i];
            if (Wrapper.canAttack(unit, victim)) {
                bestVictim = getBestAttackTarget(bestVictim, victim);
            }
        }
        if (bestVictim != null) Wrapper.attack(unit, bestVictim);
    }



    void move(AuxUnit unit){
        AuxMapLocation target = getBestTarget(unit);
        if (target != null) MovementManager.getInstance().moveTo(unit, target);
        else {
            ConstructionQueue queue = Data.queue;
            queue.requestUnit(UnitType.Rocket);
            explore(unit);
        }
    }

    AuxMapLocation getBestHealer(AuxMapLocation loc){
        double minDist = 100000;
        AuxMapLocation ans = null;
        for (int i = 0; i < Data.myUnits.length; ++i){
            AuxUnit u = Data.myUnits[i];
            if (u.getType() == UnitType.Healer){
                AuxMapLocation mLoc = u.getMaplocation();
                if (mLoc != null){
                    double d = loc.distanceBFSTo(mLoc);
                    if (d < minDist){
                        minDist = d;
                        ans = mLoc;
                    }
                }
            }
        }
        return ans;
    }

    AuxMapLocation getBestTarget(AuxUnit unit){
        if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
        if (unit.getHealth() < 100) {
            AuxMapLocation ans = getBestHealer(unit.getMaplocation());
            if (ans != null) return ans;
        }
        return getBestEnemy(unit.getMaplocation());
    }


    void goToBestEnemy(AuxUnit unit){
        AuxMapLocation myLoc = unit.getMaplocation();
        AuxMapLocation target = getBestEnemy(myLoc);
        if(target == null) return;
        MovementManager.getInstance().moveTo(unit, target);

    }

    AuxMapLocation getBestEnemy(AuxMapLocation myLoc){
        long minDist = INFL;
        AuxMapLocation target = null;
        for(int i = 0; i < Data.enemies.length; ++i){
            AuxMapLocation enemyLocation = Data.enemies[i].getMaplocation();
            long d = enemyLocation.distanceSquaredTo(myLoc);
            if(d < minDist){
                minDist = d;
                target = enemyLocation;
            }
        }
        return target;
    }

    static AuxMapLocation areaToLocation(Integer area){
        int x = Data.areaToLocX[Data.decodeX(area)];
        int y = Data.areaToLocY[Data.decodeY(area)];
        return new AuxMapLocation(x, y);
    }

    AuxMapLocation findExploreObjective(AuxUnit unit){
        Integer current = Data.currentArea.get(unit.getID());
        Integer obj = null;
        double minExplored = INF;
        AuxMapLocation myLoc = unit.getMaplocation();
        if(objectiveArea.containsKey(unit.getID()) && current.intValue() != objectiveArea.get(unit.getID()).intValue()) {
            return areaToLocation(objectiveArea.get(unit.getID()));
        }
        for(int i = 0; i < Data.exploreSizeX; ++i){
            for(int j = 0; j < Data.exploreSizeY; ++j){
                if(current.intValue() == Data.encode(i,j).intValue()) continue;
                Integer area = Data.encode(i,j);
                AuxMapLocation areaLoc = areaToLocation(area);
                if(myLoc.distanceBFSTo(areaLoc) >= INF) continue;
                if(Data.exploreGrid[i][j] < minExplored){
                    minExplored = Data.exploreGrid[i][j];
                    obj = area;
                }
            }
        }
        if(obj != null) {
            Data.addExploreGrid(obj, Data.exploreConstant);
            objectiveArea.put(unit.getID(), obj);
        }
        if (obj != null) return areaToLocation(obj);
        return null;
    }

    void explore(AuxUnit unit){
        AuxMapLocation obj = findExploreObjective(unit);
        if(obj != null) MovementManager.getInstance().moveTo(unit, obj);
    }
}