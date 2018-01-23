import java.util.ArrayList;
import bc.*;
/**
 * Created by Ivan on 1/18/2018.
 */
public class Wrapper {

    static double getDamage(UnitType type){ //ToDo
        switch(type){
            case Ranger:
                return 30;
            case Knight:
                return 80;
            case Mage:
                return Data.mageDMG;
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
                return 30;
            default:
                return 0;
        }
    }

    static int getAttackRangeSafe(UnitType type){ //ToDo
        switch(type){
            case Ranger:
                return 50;
            case Knight:
                return 8;
            case Mage:
                return 30;
            default:
                return 0;
        }
    }

    static int getAttackRangeLong(UnitType type){ //ToDo
        switch(type){
            case Ranger:
                return 100;
            case Knight:
                return 68;
            case Mage:
                return 65;
            default:
                return 0;
        }
    }

    static int getAttackRangeExtra(UnitType type){ //ToDo
        switch(type){
            case Ranger:
                return 68;
            case Knight:
                return 8;
            case Mage:
                return 65;
            default:
                return 0;
        }
    }

    static boolean isAccessible(AuxMapLocation loc){
        try {
            if (!loc.isOnMap()) return false;
            if (!Data.accessible[loc.x][loc.y]) return false;
            if (Data.unitMap[loc.x][loc.y] != 0) return false;
            if (Data.occupiedPositions.contains(Data.encodeOcc(loc.x, loc.y))) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static AuxUnit[] senseUnits(int x, int y, int r, boolean myTeam){ //Todo check if it is better to iterate over enemies
        try {
            ArrayList<AuxUnit> ans = new ArrayList<>();
            for (int i = 0; i < Vision.Mx[r].length; ++i) {
                AuxUnit unit = Data.getUnit(x + Vision.Mx[r][i], y + Vision.My[r][i], myTeam);
                if (unit != null) ans.add(unit);
            }
            return ans.toArray(new AuxUnit[ans.size()]);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static AuxUnit[] senseUnits(AuxMapLocation loc, int r, boolean myTeam){
        try {
            return senseUnits(loc.x, loc.y, r, myTeam);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static boolean canUnload(AuxUnit unit, int dir) {
        try {
            if (unit.getGarrisonUnits().size() == 0) return false;
            if (!isAccessible(unit.getMaplocation().add(dir))) return false;
            if (!Data.myUnits[Data.allUnits.get(unit.getGarrisonUnits().get(0))].canMove()) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void unload (AuxUnit unit, int dir){
        try {
            int newID = unit.getGarrisonUnits().get(0);
            int posAtArray = Data.allUnits.get(newID);
            AuxMapLocation loc = unit.getMaplocation();
            AuxMapLocation newLoc = loc.add(dir);
            Data.unitMap[newLoc.x][newLoc.y] = posAtArray + 1;
            //Data.myUnits[posAtArray].mloc = newLoc;
            unit.garrisonUnits.remove(0);
            Data.gc.unload(unit.getID(), Data.allDirs[dir]);
        }catch(Exception e) {
            e.printStackTrace();
        }
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

    static int getIndex(UnitType type){
        switch(type){
            case Factory:
                return 5;
            case Worker:
                return 0;
            case Ranger:
                return 2;
            case Knight:
                return 1;
            case Mage:
                return 3;
            case Rocket:
                return 6;
            case Healer:
                return 4;
            default:
                return 0;
        }
    }

    public static int getIndex(Direction dir) {
        switch (dir) {
            case North:
                return 0;
            case Northeast:
                return 1;
            case East:
                return 2;
            case Southeast:
                return 3;
            case South:
                return 4;
            case Southwest:
                return 5;
            case West:
                return 6;
            case Northwest:
                return 7;
            default:
                return 0;

        }
    }



    static boolean canProduceUnit(AuxUnit unit, UnitType type){
        try {
            if (!unit.canAttack()) return false;
            if (unit.getGarrisonUnits().size() >= 8) return false;
            return (Data.getKarbonite() >= cost(type));
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void produceUnit(AuxUnit unit, UnitType type){
        try {
            Data.gc.produceRobot(unit.getID(), type);
            Data.karbonite = Data.getKarbonite() - cost(type);
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void heal(AuxUnit u1, AuxUnit u2){
        try {
            u2.getHealth();
            u2.health += Data.healingPower;
            int mh = getMaxHealth(u2.getType());
            if (u2.health > mh) u2.health = mh;
            u1.canAttack = false;
            Data.gc.heal(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canMove(AuxUnit unit, int dir) {
        try {
            if (!unit.canMove()) return false;
            AuxMapLocation mloc = unit.getMaplocation();
            AuxMapLocation newLoc = mloc.add(dir);
            return (isAccessible(newLoc));
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void build(AuxUnit unit, AuxUnit blueprint){
        try {
            blueprint.getHealth();
            blueprint.isBlueprint();
            blueprint.health += Data.buildingPower;
            int maxHP = getMaxHealth(blueprint.getType());
            if (blueprint.health > maxHP) blueprint.health = maxHP;
            if (blueprint.health == maxHP) blueprint.blueprint = false;
            Data.gc.build(unit.getID(), blueprint.getID());
            //System.out.println("Remaining build health: " + (maxHP - blueprint.health) +  " max hp? " + blueprint.isMaxHealth());
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void repair(AuxUnit unit, AuxUnit structure){
        try {
            structure.getHealth();
            structure.health += Data.repairingPower;
            int maxHP = getMaxHealth(structure.getType());
            if (structure.health > maxHP) structure.health = maxHP;
            //AuxMapLocation loc = unit.getMaplocation();
            //AuxMapLocation sloc = structure.getMaplocation();
            //int distance = loc.distanceSquaredTo(sloc);
            //System.out.println("DISTANCE BETWEEN " + loc.x + "," + loc.y + " AND " + sloc.x + "," + sloc.y + " = " + distance + "  " + unit.getID());
            //System.out.println(unit.unit.location().mapLocation().getX() + " " + unit.unit.location().mapLocation().getY());
            //Unit unitt = Data.gc.unit(structure.getID());
            Data.gc.repair(unit.getID(), structure.getID());
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void moveRobot(AuxUnit unit, int dir) {
        try {
            AuxMapLocation mloc = unit.getMaplocation();
            AuxMapLocation newLoc = mloc.add(dir);
            //if (!isAccessible(newLoc)){
            //System.err.println("User error");
            //return;
            //}
            unit.canMove = false;
            unit.mloc = newLoc;
            Data.unitMap[mloc.x][mloc.y] = 0;
            Data.unitMap[newLoc.x][newLoc.y] = Data.allUnits.get(unit.getID()) + 1;
            Data.gc.moveRobot(unit.getID(), Data.allDirs[dir]);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void blink(AuxUnit unit, AuxMapLocation mloc) {
        try {
            AuxMapLocation loc = unit.getMaplocation();
            unit.canUseAbility = false;
            Data.unitMap[loc.x][loc.y] = 0;
            Data.unitMap[mloc.x][mloc.y] = Data.allUnits.get(unit.getID()) + 1;
            Data.gc.blink(unit.getID(), new MapLocation(Data.planet, mloc.x, mloc.y));
            unit.mloc = mloc;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canReplicate(AuxUnit unit, int dir){
        try {
            if (Data.getKarbonite() < Data.replicateCost) return false;
            AuxMapLocation mloc = unit.getMaplocation();
            AuxMapLocation newLoc = mloc.add(dir);
            if (!isAccessible(newLoc)) return false;
            if (!unit.canUseAbility()) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void replicate(AuxUnit unit, int dir){
        try {
            AuxMapLocation mloc = unit.getMaplocation();
            AuxMapLocation newLoc = mloc.add(dir);
            Data.gc.replicate(unit.getID(), Data.allDirs[dir]);
            Data.karbonite = Data.getKarbonite() - Data.replicateCost;
            Data.occupiedPositions.add(Data.encodeOcc(newLoc.x, newLoc.y));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canPlaceBlueprint (AuxUnit unit, UnitType type, int dir){
        try {
            if (Data.planet == Planet.Mars) return false;
            if (Data.getKarbonite() < cost(type)) return false;
            AuxMapLocation mloc = unit.getMaplocation();
            AuxMapLocation newLoc = mloc.add(dir);
            if (!isAccessible(newLoc)) return false;
            if (type == UnitType.Rocket && !Data.canBuildRockets) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void placeBlueprint(AuxUnit unit, UnitType type, int dir) {
        try {
            AuxMapLocation mloc = unit.getMaplocation();
            AuxMapLocation newLoc = mloc.add(dir);
            Data.gc.blueprint(unit.getID(), type, Data.allDirs[dir]);
            Data.karbonite = Data.getKarbonite() - cost(type);
            Data.occupiedPositions.add(Data.encodeOcc(newLoc.x, newLoc.y));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canHarvest(AuxUnit unit, int dir){
        try {
            AuxMapLocation loc = unit.getMaplocation();
            AuxMapLocation mineLoc = loc.add(dir);
            if (!mineLoc.isOnMap()) return false;
            return Data.gc.canHarvest(unit.getID(), Data.allDirs[dir]);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // retorna -1 si no fa harvest
    // si fa harvest, retorna la karbo que queda al lloc
    static int harvest(AuxUnit unit, int dir) {
        try {
            //System.out.println("Entra harvest ");
            AuxMapLocation loc = unit.getMaplocation();
            AuxMapLocation mineLoc = loc.add(dir);
            int karboAmount = Data.karboMap[mineLoc.x][mineLoc.y];
            if (karboAmount == 0) return -1;
            Data.gc.harvest(unit.getID(), Data.allDirs[dir]);
            int newKarboAmount = karboAmount - Data.harvestingPower;
            if (newKarboAmount < 0) newKarboAmount = 0;
            if (newKarboAmount > 0) {
                Data.karboniteAt.put(Data.encodeOcc(mineLoc.x, mineLoc.y), newKarboAmount);
            } else Data.karboniteAt.remove(mineLoc);
            Data.karboMap[mineLoc.x][mineLoc.y] = newKarboAmount;
            unit.canAttack = false;
            //System.out.println("Karbo after mining: " + newKarboAmount);
            return newKarboAmount;
        }catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    static boolean canAttack(AuxUnit unit, AuxUnit unit2){
        try {
            if (!unit.canAttack()) return false;
            int d = unit.getMaplocation().distanceSquaredTo(unit2.getMaplocation());
            if (unit.getType() == UnitType.Ranger && d <= 10) return false;
            return (getAttackRange(unit.getType()) >= d);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void attack(AuxUnit u1, AuxUnit u2){
        try {
            if (u1.getType() != UnitType.Mage) {
                u2.getHealth();
                u2.health -= (int) getDamage(u1.getType());
                if (u2.health <= 0) Data.unitMap[u2.getX()][u2.getY()] = 0;
            } else {
                AuxMapLocation mloc = u2.getMaplocation();
                for (int i = 0; i < 9; ++i) {
                    AuxMapLocation newLoc = mloc.add(i);
                    AuxUnit unit2 = Data.getUnit(newLoc.x, newLoc.y, false);
                    if (unit2 != null) {
                        unit2.getHealth();
                        unit2.health -= (int) getDamage(u1.getType());
                        if (unit2.health <= 0) Data.unitMap[unit2.getMaplocation().x][unit2.getMaplocation().y] = 0;
                    }
                }
            }
            u1.canAttack = false;
            Data.gc.attack(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static int getArrivalRound(int round){
        try {
            return (int) Data.gc.orbitPattern().duration(round);
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    static void launchRocket(AuxUnit unit, AuxMapLocation loc){
        try {
            //System.out.println("Launching at " + unit.getX() + " " + unit.getY());
            Data.gc.launchRocket(unit.getID(), new MapLocation(Planet.Mars, loc.x, loc.y));
            AuxMapLocation mloc = unit.getMaplocation();
            Data.unitMap[mloc.x][mloc.y] = 0;
            Data.structures.remove(unit.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canLoad(AuxUnit u1, AuxUnit u2) {
        try {
            if (!u2.canMove()) return false;
            if (u1.getGarrisonUnits().size() >= 8) return false;
            if (u2.isInGarrison()) return false;
            return (u1.getMaplocation().distanceSquaredTo(u2.getMaplocation()) <= 2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void load(AuxUnit u1, AuxUnit u2){
        try {
            //System.out.println("Loading! at " + u2.getMaplocation().x + " " + u2.getMaplocation().y + " " + u2.getID());
            AuxMapLocation mloc = u2.getMaplocation();
            Data.unitMap[mloc.x][mloc.y] = 0;
            u2.garrison = true;
            u2.mloc = null;
            u2.canMove = false;
            u2.canAttack = false;
            u1.getGarrisonUnits().add(u2.getID());
            Data.gc.load(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

}
