

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
        try {
            if (A == null) return B;
            if (B == null) return A;
            if (A.getType() == B.getType()) {
                if (A.getHealth() < B.getHealth()) return A;
                return B;
            }
            if (A.getType() == UnitType.Mage) return A;
            if (B.getType() == UnitType.Mage) return B;
            if (A.getType() == UnitType.Healer) return A;
            if (B.getType() == UnitType.Healer) return B;
            if (A.getType() == UnitType.Ranger) return A;
            if (B.getType() == UnitType.Ranger) return B;
            if (A.getType() == UnitType.Knight) return A;
            if (B.getType() == UnitType.Knight) return B;
            if (A.getType() == UnitType.Worker) return A;
            if (B.getType() == UnitType.Worker) return B;
            return B;
        }catch(Exception e) {
            System.out.println(e);
            return A;
        }
    }

    void attack(AuxUnit unit) {
        try {
            AuxUnit bestVictim = null;
            if (!unit.canAttack()) return;
            //if (Data.round >= 730) System.err.println("Trying to attack!");
            AuxMapLocation myLoc = unit.getMaplocation();
            AuxUnit[] canAttack = Wrapper.senseUnits(myLoc.x, myLoc.y, Wrapper.getAttackRange(unit.getType()), false);
            //if (Data.round >= 730)System.err.println(canAttack.length);
            for (int i = 0; i < canAttack.length; ++i) {
                AuxUnit victim = canAttack[i];
                if (Wrapper.canAttack(unit, victim)) {
                    bestVictim = getBestAttackTarget(bestVictim, victim);
                    // if (Data.round >= 730)System.err.println("Got a victim!! :)");
                }
            }
            if (bestVictim != null) Wrapper.attack(unit, bestVictim);
        }catch(Exception e) {
            System.out.println(e);
        }
    }



    void move(AuxUnit unit){
        try {
            AuxMapLocation target = getBestTarget(unit);
            if (target != null) MovementManager.getInstance().moveTo(unit, target);
            else {
                ConstructionQueue queue = Data.queue;
                queue.requestUnit(UnitType.Rocket);
                explore(unit);
            }
        }catch(Exception e) {
            System.out.println(e);
        }
    }

    AuxMapLocation getBestHealer(AuxMapLocation loc){
        try {
            double minDist = 100000;
            AuxMapLocation ans = null;
            for (int i = 0; i < Data.myUnits.length; ++i) {
                AuxUnit u = Data.myUnits[i];
                if (u.getType() == UnitType.Healer) {
                    AuxMapLocation mLoc = u.getMaplocation();
                    if (mLoc != null) {
                        double d = loc.distanceBFSTo(mLoc);
                        if (d < minDist) {
                            minDist = d;
                            ans = mLoc;
                        }
                    }
                }
            }
            return ans;
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    AuxMapLocation getBestTarget(AuxUnit unit){
        try {
            if (Rocket.callsToRocket.containsKey(unit.getID())) return Rocket.callsToRocket.get(unit.getID());
            if (unit.getHealth() < 100) {
                AuxMapLocation ans = getBestHealer(unit.getMaplocation());
                if (ans != null) return ans;
            }
            return getBestEnemy(unit.getMaplocation());
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    AuxMapLocation getBestEnemy(AuxMapLocation myLoc){
        try {
            long minDist = INFL;
            AuxMapLocation target = null;
            for (int i = 0; i < Data.enemies.length; ++i) {
                AuxMapLocation enemyLocation = Data.enemies[i].getMaplocation();
                long d = enemyLocation.distanceSquaredTo(myLoc);
                if (d < minDist) {
                    minDist = d;
                    target = enemyLocation;
                }
            }
            return target;
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    static AuxMapLocation areaToLocation(Integer area){
        try {
            int x = Data.areaToLocX[Data.decodeX(area)];
            int y = Data.areaToLocY[Data.decodeY(area)];
            return new AuxMapLocation(x, y);
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
    }

    AuxMapLocation findExploreObjective(AuxUnit unit){
        try {
            Integer current = Data.currentArea.get(unit.getID());
            Integer obj = null;
            double minExplored = INF;
            AuxMapLocation myLoc = unit.getMaplocation();
            if (objectiveArea.containsKey(unit.getID()) && current.intValue() != objectiveArea.get(unit.getID()).intValue()) {
                return areaToLocation(objectiveArea.get(unit.getID()));
            }
            for (int i = 0; i < Data.exploreSizeX; ++i) {
                for (int j = 0; j < Data.exploreSizeY; ++j) {
                    if (current.intValue() == Data.encode(i, j).intValue()) continue;
                    Integer area = Data.encode(i, j);
                    AuxMapLocation areaLoc = areaToLocation(area);
                    if (myLoc.distanceBFSTo(areaLoc) >= INF) continue;
                    if (Data.exploreGrid[i][j] < minExplored) {
                        minExplored = Data.exploreGrid[i][j];
                        obj = area;
                    }
                }
            }
            if (obj != null) {
                Data.addExploreGrid(obj, Data.exploreConstant);
                objectiveArea.put(unit.getID(), obj);
            }
            if (obj != null) return areaToLocation(obj);
        }catch(Exception e) {
            System.out.println(e);
            return null;
        }
        return null;
    }

    void explore(AuxUnit unit) {
        try {
            AuxMapLocation obj = findExploreObjective(unit);
            if (obj != null) MovementManager.getInstance().moveTo(unit, obj);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}