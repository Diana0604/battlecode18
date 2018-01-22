

import bc.*;

import java.util.HashMap;

public class Healer {
    final long INFL = 1000000000;
    final int INF = 1000000000;
    final double eps = 0.001;
    static Healer instance = null;
    static GameController gc;
    private UnitManager unitManager;

    HashMap<Integer, Integer> objectiveArea;

    static Healer getInstance(){
        if (instance == null){
            instance = new Healer();
        }
        return instance;
    }

    public Healer(){
        unitManager = UnitManager.getInstance();
        gc = unitManager.gc;
        objectiveArea = new HashMap();
    }

    void heal(Unit unit){
        int ID = unit.id();
        if (!gc.isHealReady(ID)) return;
        VecUnit v = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 30, unit.team());
        long maxDiff = 0;
        Integer id = null;
        for (int i = 0; i < v.size(); ++i){
            Unit u = v.get(i);
            long d = u.maxHealth() - u.health();
            if (d > maxDiff){
                maxDiff = d;
                id = u.id();
            }
        }
        if (id != null){
            if (gc.canHeal(ID, id)) gc.heal(ID, id);
        }
    }

    void play(Unit unit){
        System.out.println("Heya I'm playing");
        heal(unit);
        move(unit);
        heal(unit);
    }

    static void moveTo(Unit unit, MapLocation target){
        MovementManager.getInstance().moveTo(unit, target);
    }

    Unit getBestAttackTarget(Unit A, Unit B){
        if (A == null) return B;
        if (B == null) return A;
        if (A.unitType() == B.unitType()){
            if (A.health() < B.health()) return A;
            return B;
        }
        if (A.unitType() == UnitType.Ranger) return A;
        if (B.unitType() == UnitType.Ranger) return B;
        if (A.unitType() == UnitType.Mage) return A;
        if (B.unitType() == UnitType.Mage) return B;
        if (A.unitType() == UnitType.Healer) return A;
        if (B.unitType() == UnitType.Healer) return B;
        if (A.unitType() == UnitType.Knight) return A;
        if (B.unitType() == UnitType.Knight) return B;
        if (A.unitType() == UnitType.Worker) return A;
        if (B.unitType() == UnitType.Worker) return B;
        return B;
    }



    void move(Unit unit){
        MapLocation target = getBestTarget(unit);
        if (target != null) MovementManager.getInstance().moveTo(unit, target);
    }

    MapLocation getBestTarget(Unit unit){
        if (Rocket.callsToRocket.containsKey(unit.id())) return Rocket.callsToRocket.get(unit.id());
        return getBestUnit(unit.location().mapLocation());
    }

    MapLocation getBestUnit(MapLocation loc){
        double minDist = 500000;
        MapLocation ans = null;
        for (int i = 0; i < Data.units.size(); ++i){
            Unit u = Data.units.get(i);
            if (!Data.structures.contains(i)){
                if (u.health() < u.maxHealth()){
                    Location l = u.location();
                    if (!l.isInGarrison()){
                        MapLocation mLoc = l.mapLocation();
                        double d = MovementManager.getInstance().distance(mLoc, loc);
                        if (d < minDist){
                            ans = mLoc;
                            minDist = d;
                        }
                    }
                }
            }
        }
        return ans;
    }

    MapLocation getBestEnemy(MapLocation myLoc){
        long minDist = INFL;
        MapLocation target = null;
        for(int i = 0; i < Data.enemyUnits.size(); ++i){
            MapLocation enemyLocation = Data.enemyUnits.get(i).location().mapLocation();
            long d = enemyLocation.distanceSquaredTo(myLoc);
            if(d < minDist){
                minDist = d;
                target = enemyLocation;
            }
        }
        return target;
    }

    static MapLocation areaToLocation(Integer area){
        int x = Data.areaToLocX[Data.decodeX(area)];
        int y = Data.areaToLocY[Data.decodeY(area)];
        return new MapLocation(gc.planet(), x, y);
    }

    MapLocation findExploreObjective(Unit unit){
        int id = unit.id();
        Integer current = Data.currentArea.get(id);
        Integer obj = null;
        double minExplored = INF;
        MapLocation myLoc = unit.location().mapLocation();
        if(objectiveArea.containsKey(id) && current.intValue() != objectiveArea.get(id).intValue()) {
            return areaToLocation(objectiveArea.get(id));
        }
        for(int i = 0; i < Data.exploreSizeX; ++i){
            for(int j = 0; j < Data.exploreSizeY; ++j){
                if(current.intValue() == Data.encode(i,j).intValue()) continue;
                Integer area = Data.encode(i,j);
                MapLocation areaLoc = areaToLocation(area);
                if(Pathfinder.getInstance().getNode(myLoc.getX(), myLoc.getY(), areaLoc.getX(), areaLoc.getY()).dist >= INF) continue;
                if(Data.exploreGrid[i][j] < minExplored){
                    minExplored = Data.exploreGrid[i][j];
                    obj = area;
                }
            }
        }
        if(obj != null) {
            Data.addExploreGrid(obj, Data.exploreConstant);
            objectiveArea.put(id, obj);
        }
        if (obj != null) return areaToLocation(obj);
        return null;
    }

    void explore(Unit unit){
        MapLocation obj = findExploreObjective(unit);
        if(obj != null) moveTo(unit, obj);

    }
}