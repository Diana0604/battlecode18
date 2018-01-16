

import bc.*;

import java.util.HashMap;

import static java.lang.Math.floor;

public class Ranger {
    final long INFL = 1000000000;
    final int INF = 1000000000;
    final double eps = 0.001;
    static Ranger instance = null;
    static GameController gc;
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
        gc = unitManager.gc;
        objectiveArea = new HashMap();
    }

    void play(Unit unit){
        attack(unit);
        move(unit);
        attack(unit);
    }

    void attack(Unit unit) {
        if(!gc.isAttackReady(unit.id())) return;
        MapLocation myLoc = unit.location().mapLocation();
        VecUnit canAttack = gc.senseNearbyUnitsByTeam(myLoc, unit.attackRange(), unitManager.enemyTeam);
        for(int i = 0; i < canAttack.size(); ++i){
            Unit victim = canAttack.get(i);
            if (gc.canAttack(unit.id(), victim.id())) {
                gc.attack(unit.id(), victim.id());
                return;
            }
        }
    }



    void move(Unit unit){
        MapLocation target = getBestTarget(unit);
        if (target != null) MovementManager.getInstance().moveTo(unit, target);
        else explore(unit);
    }

    MapLocation getBestTarget(Unit unit){
        return getBestEnemy(unit.location().mapLocation());
    }


    void goToBestEnemy(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        MapLocation target = getBestEnemy(myLoc);
        if(target == null) return;
        UnitManager.getInstance().moveTo(unit, target);

    }

    MapLocation getBestEnemy(MapLocation myLoc){
        long minDist = INFL;
        MapLocation target = null;
        for(int i = 0; i < unitManager.enemyUnits.size(); ++i){
            MapLocation enemyLocation = unitManager.enemyUnits.get(i).location().mapLocation();
            long d = enemyLocation.distanceSquaredTo(myLoc);
            if(d < minDist){
                minDist = d;
                target = enemyLocation;
            }
        }
        return target;
    }


    MapLocation findExploreObjective(Unit unit){
        int id = unit.id();
        Integer current = unitManager.currentArea.get(id);
        Integer obj = null;
        double minExplored = INF;
        MapLocation myLoc = unit.location().mapLocation();
        if(objectiveArea.containsKey(id) && current.intValue() != objectiveArea.get(id).intValue()) {
            return unitManager.areaToLocation(objectiveArea.get(id));
        }
        for(int i = 0; i < unitManager.exploreSizeX; ++i){
            for(int j = 0; j < unitManager.exploreSizeY; ++j){
                if(current.intValue() == unitManager.encode(i,j).intValue()) continue;
                Integer area = unitManager.encode(i,j);
                MapLocation areaLoc = unitManager.areaToLocation(area);
                if(Pathfinder.getInstance().getNode(myLoc.getX(), myLoc.getY(), areaLoc.getX(), areaLoc.getY()).dist >= INF) continue;
                if(unitManager.exploreGrid[i][j] < minExplored){
                    minExplored = unitManager.exploreGrid[i][j];
                    obj = area;
                }
            }
        }
        if(obj != null) {
            unitManager.addExploreGrid(obj, unitManager.exploreConstant);
            objectiveArea.put(id, obj);
        }
        return unitManager.areaToLocation(obj);
    }

    void explore(Unit unit){
        MapLocation obj = findExploreObjective(unit);
        if(obj != null) unitManager.moveTo(unit, obj);

    }
}
