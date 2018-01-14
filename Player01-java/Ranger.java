import bc.*;

import java.util.HashMap;

import static java.lang.Math.floor;

public class Ranger {


    final long attackRange = 50; //TODO there's a "get range" o algo aixi method
    final long INFL = 1000000;
    final int INF = 1000000;
    final double eps = 0.001;
    static Ranger instance = null;
    static GameController gc;
    boolean wait;

    HashMap<Integer, int[]> objectiveArea = new HashMap();

    static Ranger getInstance(){
        if (instance == null){
            instance = new Ranger();
            gc = UnitManager.gc;
        }
        return instance;
    }

    void play(Unit unit){
        wait = false;
        attack(unit);
        if(wait) return;
        move(unit);
        if(wait) return;
    }

    void attack(Unit unit) {
        MapLocation myLoc = unit.location().mapLocation();
        VecUnit canAttack = gc.senseNearbyUnitsByTeam(myLoc, attackRange, UnitManager.getInstance().enemyTeam);
        for(int i = 0; i < canAttack.size(); ++i){
            Unit victim = canAttack.get(i);
            wait = true;
            if(!gc.isAttackReady(unit.id())) return;
            if (gc.canAttack(unit.id(), victim.id())) {
                gc.attack(unit.id(), victim.id());
                return;
            }
        }
    }



    void move(Unit unit){
        if(!gc.isMoveReady(unit.id())) return;
        //goToBestEnemy(unit);
        if(wait) return;
        explore(unit);
    }

    void goToBestEnemy(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        MapLocation target = getBestEnemy(myLoc);
        if(target == null) return;
        wait = true;
        UnitManager.getInstance().moveTo(unit, target);
    }

    MapLocation getBestEnemy(MapLocation loc){
        long minDist = INFL;
        MapLocation ans = null;
        for(int i = 0; i < UnitManager.Xenemy.size(); ++i){
            int x = UnitManager.Xenemy.get(i);
            int y = UnitManager.Yenemy.get(i);
            MapLocation enemyLoc = new MapLocation(gc.planet(), x, y);
            long d = loc.distanceSquaredTo(enemyLoc);
            if(d < minDist){
                minDist = d;
                ans = enemyLoc;
            }
        }
        return ans;
    }


    void updateExploreObjective(Unit unit){
        UnitManager um = UnitManager.getInstance();
        int id = unit.id();
        if(objectiveArea.containsKey(id) && !um.currentArea.get(id).equals(objectiveArea.get(id))) return;
        int[] obj = new int[2];
        double notExplored = INF;
        for(int i = 0; i < um.exploreSize; ++i){
            for(int j = 0; j < um.exploreSize; ++j){
                if(um.exploreGrid[i][j] < notExplored){
                    obj[0] = i;
                    obj[1] = j;
                    notExplored = um.exploreGrid[i][j];
                }
            }
        }
        MapLocation exploring = um.areaToLocation(obj);
        um.addExploreGrid(exploring.getX(), exploring.getY(),eps);
        objectiveArea.put(id, obj);
    }

    void explore(Unit unit){
        UnitManager um = UnitManager.getInstance();
        updateExploreObjective(unit);
        UnitManager.moveTo(unit, um.areaToLocation(objectiveArea.get(unit.id())));
    }
}