import bc.*;

public class Ranger {

    //does it change with research? will have to change this
    final long attackRange = 50;

    static Ranger instance = null;
    static GameController gc;
    boolean wait;

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
        goToBestEnemy(unit);
    }

    void goToBestEnemy(Unit unit){
        MapLocation myLoc = unit.location().mapLocation();
        MapLocation target = getBestEnemy(myLoc);
        if(target == null) return; // explorar TODO
        UnitManager.getInstance().moveTo(unit, target);
    }

    MapLocation getBestEnemy(MapLocation loc){
        long minDist = 1000000;
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
}