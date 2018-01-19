import java.util.ArrayList;
import bc.*;
/**
 * Created by Ivan on 1/18/2018.
 */
public class Wrapper {

    static double getDamage(UnitType type){ //ToDo
        switch(type){
            case Ranger:
                return 40;
            case Knight:
                return 60;
            case Mage:
                return 105;
            default:
                return 0;
        }
    }

    static double getAttackCooldown(UnitType type){ //ToDo
        switch(type){
            case Ranger:
                return 2;
            case Knight:
                return 2;
            case Mage:
                return 2;
            default:
                return 1;
        }
    }

    static int getAttackRange(UnitType type){ //ToDo
        switch(type){
            case Ranger:
                return 50;
            case Knight:
                return 2;
            case Mage:
                return 45;
            default:
                return 0;
        }
    }

    static boolean isAccessible(AuxMapLocation loc){
        if (!loc.isOnMap()) return false;
        if (!Data.accessible[loc.x][loc.y]) return false;
        if (Data.unitMap[loc.x][loc.y] != 0) return false;
        return true;
    }

    static AuxUnit[] senseUnits(int x, int y, int r, boolean myTeam){ //Todo check if it is better to iterate over enemies
        ArrayList<AuxUnit> ans = new ArrayList<>();
        for (int i = 0; i < Vision.Mx[r].length; ++i){
            AuxUnit unit = Data.getUnit(x+Vision.Mx[r][i], y + Vision.Mx[r][i], myTeam);
            if (unit != null) ans.add(unit);
        }
        return (AuxUnit[])ans.toArray();
    }

    static AuxUnit[] senseUnits(AuxMapLocation loc, int r, boolean myTeam){
        return senseUnits(loc.x, loc.y, r, myTeam);
    }

    static boolean canUnload(AuxUnit unit, int dir) {
        if (unit.getGarrisonUnits().size() == 0) return false;
        if (!isAccessible(unit.getMaplocation().add(dir))) return false;
        return true;
    }

    static void unload (AuxUnit unit, int dir){
        int newID = unit.getGarrisonUnits().get(0);
        int posAtArray = Data.allUnits.get(newID);
        AuxMapLocation loc = unit.getMaplocation();
        AuxMapLocation newLoc = loc.add(dir);
        Data.unitMap[newLoc.x][newLoc.y] = posAtArray+1;
        unit.garrisonUnits.remove(0);
        Data.gc.unload(unit.getID(), Data.allDirs[dir]);

        Data.myUnits[posAtArray].canMove = false;
        Data.myUnits[posAtArray].garrison = false;
        Data.myUnits[posAtArray].mloc = newLoc;

    }

    static int cost(UnitType type){
        switch(type){
            case Factory:
                return 100;
            case Worker:
                return 25;
            case Ranger:
                return 20;
            case Knight:
                return 20;
            case Mage:
                return 20;
            case Rocket:
                return 75;
            case Healer:
                return 20;
            default:
                return 100;
        }
    }

    static int getMaxHealth(UnitType type){
        switch(type){
            case Factory:
                return 300;
            case Worker:
                return 100;
            case Ranger:
                return 200;
            case Knight:
                return 250;
            case Mage:
                return 80;
            case Rocket:
                return 200;
            case Healer:
                return 100;
            default:
                return 1;
        }
    }

    static boolean canProduceUnit(AuxUnit unit, UnitType type){ //IT DOESNT CHECK IF ALREADY BUILT
        return (Data.karbonite >= cost(type));
    }

    static void produceUnit(AuxUnit unit, UnitType type){
        Data.gc.produceRobot(unit.getID(), type);
        Data.karbonite -= cost(type);
    }

    static void heal(AuxUnit u1, AuxUnit u2){
        u2.health += Data.healingPower;
        int mh = getMaxHealth(u2.getType());
        if (u2.health > mh) u2.health = mh;
        u1.canAttack = false;
        Data.gc.heal(u1.getID(), u2.getID());
    }

    static boolean canMove(AuxUnit unit, int dir) {
        if (!unit.canMove()) return false;
        AuxMapLocation mloc = unit.getMaplocation();
        AuxMapLocation newLoc = mloc.add(dir);
        return (isAccessible(newLoc));
    }

    static void build(AuxUnit unit, AuxUnit blueprint){
        blueprint.health += Data.buildingPower;
        int maxHP = getMaxHealth(blueprint.getType());
        if (blueprint.health > maxHP) blueprint.health = maxHP;
        if (blueprint.health == maxHP) blueprint.blueprint = false;
        Data.gc.build(unit.getID(), blueprint.getID());
    }

    static void repair(AuxUnit unit, AuxUnit structure){
        structure.health += Data.repairingPower;
        int maxHP = getMaxHealth(structure.getType());
        if (structure.health > maxHP) structure.health = maxHP;
        Data.gc.build(unit.getID(), structure.getID());
    }

    static void moveRobot(AuxUnit unit, int dir){
        AuxMapLocation mloc = unit.getMaplocation();
        AuxMapLocation newLoc = mloc.add(dir);
        unit.canMove = false;
        unit.mloc = newLoc;
        Data.unitMap[mloc.x][mloc.y] = 0;
        Data.unitMap[newLoc.x][newLoc.y] = Data.allUnits.get(unit.getID());
        Data.gc.moveRobot(unit.getID(), Data.allDirs[dir]);
    }

    static boolean canReplicate(AuxUnit unit, int dir){
        if (Data.karbonite < Data.replicateCost) return false;
        AuxMapLocation mloc = unit.getMaplocation();
        AuxMapLocation newLoc = mloc.add(dir);
        if (newLoc.isOnMap()) return false;
        if (!Data.accessible[newLoc.x][newLoc.y]) return false;
        if (Data.isOccupied(newLoc)) return false;
        return true;
    }

    static void replicate(AuxUnit unit, int dir){
        Data.gc.replicate(unit.getID(), Direction.values()[dir]);
        Data.karbonite -= Data.replicateCost;
    }

    static boolean canPlaceBlueprint (AuxUnit unit, UnitType type, int dir){
        if (Data.karbonite < cost(type)) return false;
        AuxMapLocation mloc = unit.getMaplocation();
        AuxMapLocation newLoc = mloc.add(dir);
        if (newLoc.isOnMap()) return false;
        if (!Data.accessible[newLoc.x][newLoc.y]) return false;
        if (Data.isOccupied(newLoc)) return false;
        if (type == UnitType.Rocket && !Data.canBuildRockets) return false;
        return true;
    }

    static void placeBlueprint(AuxUnit unit, UnitType type, int dir){
        Data.gc.blueprint(unit.getID(), type, Direction.values()[dir]);
        Data.karbonite -= cost(type);
    }

    // retorna -1 si no fa harvest
    // si fa harvest, retorna la karbo que queda al lloc
    static int harvest(AuxUnit unit, int dir){
        AuxMapLocation loc = unit.getMaplocation();
        AuxMapLocation mineLoc = loc.add(dir);
        if (!mineLoc.isOnMap()) return -1;
        int karboAmount = Data.karboniteAt.get(mineLoc);
        if (karboAmount == 0) return -1;
        Data.gc.harvest(unit.getID(), Direction.values()[dir]);
        int newKarboAmount = karboAmount -= Data.harvestingPower;
        if (newKarboAmount < 0) newKarboAmount = 0;
        if (newKarboAmount > 0){
            Data.karboniteAt.put(mineLoc, newKarboAmount);
        }else Data.karboniteAt.remove(mineLoc);
        Data.karboMap[mineLoc.x][mineLoc.y] = newKarboAmount;
        return newKarboAmount;
    }
}
