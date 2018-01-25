import java.util.ArrayList;
import bc.*;
/**
 * Created by Ivan on 1/18/2018.
 */
public class Wrapper {
    static AuxUnit[] senseUnits(int x, int y, int r, boolean myTeam){ //Todo check if it is better to iterate over enemies
        try {
            ArrayList<AuxUnit> ans = new ArrayList<>();
            for (int i = 0; i < Vision.Mx[r].length; ++i) {
                AuxMapLocation newLoc = new AuxMapLocation(x + Vision.Mx[r][i], y + Vision.My[r][i]);
                AuxUnit unit = newLoc.getUnit(myTeam);
                if (unit != null) ans.add(unit);
            }
            return ans.toArray(new AuxUnit[ans.size()]);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static AuxUnit[] senseUnits(int x, int y, int r){ //Todo check if it is better to iterate over enemies
        try {
            ArrayList<AuxUnit> ans = new ArrayList<>();
            for (int i = 0; i < Vision.Mx[r].length; ++i) {
                AuxMapLocation newLoc = new AuxMapLocation(x + Vision.Mx[r][i], y + Vision.My[r][i]);
                AuxUnit unit = newLoc.getUnit();
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
            if (!unit.getMapLocation().add(dir).isAccessible()) return false;
            if (!Units.myUnits[Units.allUnits.get(unit.getGarrisonUnits().get(0))].canMove()) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void unload (AuxUnit unit, int dir){
        try {
            int newID = unit.getGarrisonUnits().get(0);
            int posAtArray = Units.allUnits.get(newID);
            AuxMapLocation loc = unit.getMapLocation();
            AuxMapLocation newLoc = loc.add(dir);
            //GC.unitMap[newLoc.x][newLoc.y] = posAtArray + 1;
            //GC.myUnits[posAtArray].mloc = newLoc;

            Units.newOccupiedPositions.add(newLoc.encode());
            unit.garrisonUnits.remove(0);

            GC.gc.unload(unit.getID(), Const.allDirs[dir]);

            /*
            AuxUnit unloadedUnit = GC.myUnits[posAtArray];
            unloadedUnit.canMove = false;
            unloadedUnit.garrison = false;
            unloadedUnit.loc = unloadedUnit.unit.location();
            */

        }catch(Exception e) {
            e.printStackTrace();
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
            return (Utils.karbonite >= Units.getCost(type));
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void produceUnit(AuxUnit unit, UnitType type){
        try {
            GC.gc.produceRobot(unit.getID(), type);
            Utils.karbonite = Utils.karbonite - Units.getCost(type);
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void heal(AuxUnit u1, AuxUnit u2){
        try {
            u2.getHealth();
            u2.health += Units.healingPower;
            int mh = Units.getMaxHealth(u2.getType());
            if (u2.health > mh) u2.health = mh;
            u1.canAttack = false;
            GC.gc.heal(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canMove(AuxUnit unit, int dir) {
        try {
            if (!unit.canMove()) return false;
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            return (newLoc.isAccessible());
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void build(AuxUnit unit, AuxUnit blueprint){
        try {
            blueprint.getHealth();
            blueprint.isBlueprint();
            blueprint.health += Units.buildingPower;
            int maxHP = Units.getMaxHealth(blueprint.getType());
            if (blueprint.health > maxHP) blueprint.health = maxHP;
            if (blueprint.health == maxHP) blueprint.blueprint = false;
            GC.gc.build(unit.getID(), blueprint.getID());
            //System.out.println("Remaining build health: " + (maxHP - blueprint.health) +  " max hp? " + blueprint.isMaxHealth());
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void repair(AuxUnit unit, AuxUnit structure){
        try {
            structure.getHealth();
            structure.health += Units.repairingPower;
            int maxHP = Units.getMaxHealth(structure.getType());
            if (structure.health > maxHP) structure.health = maxHP;
            //AuxMapLocation loc = unit.getMapLocation();
            //AuxMapLocation sloc = structure.getMapLocation();
            //int distance = loc.distanceSquaredTo(sloc);
            //System.out.println("DISTANCE BETWEEN " + loc.x + "," + loc.y + " AND " + sloc.x + "," + sloc.y + " = " + distance + "  " + unit.getID());
            //System.out.println(unit.unit.location().mapLocation().getX() + " " + unit.unit.location().mapLocation().getY());
            //Unit unitt = GC.gc.unit(structure.getID());
            GC.gc.repair(unit.getID(), structure.getID());
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void moveRobot(AuxUnit unit, int dir) {
        try {
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            //if (!isAccessible(newLoc)){
            //System.err.println("User error");
            //return;
            //}
            unit.canMove = false;
            unit.mloc = newLoc;
            Units.unitMap[mloc.x][mloc.y] = 0;
            Units.unitMap[newLoc.x][newLoc.y] = Units.allUnits.get(unit.getID()) + 1;
            GC.gc.moveRobot(unit.getID(), Const.allDirs[dir]);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void blink(AuxUnit unit, AuxMapLocation mloc) {
        try {
            AuxMapLocation loc = unit.getMapLocation();
            unit.canUseAbility = false;
            Units.unitMap[loc.x][loc.y] = 0;
            Units.unitMap[mloc.x][mloc.y] = Units.allUnits.get(unit.getID()) + 1;
            GC.gc.blink(unit.getID(), new MapLocation(Mapa.planet, mloc.x, mloc.y));
            unit.mloc = mloc;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canReplicate(AuxUnit unit, int dir){
        try {
            if (Utils.karbonite < Const.replicateCost) return false;
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            if (!newLoc.isAccessible()) return false;
            if (!unit.canUseAbility()) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void replicate(AuxUnit unit, int dir){
        try {
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            GC.gc.replicate(unit.getID(), Const.allDirs[dir]);
            Utils.karbonite -= Const.replicateCost;
            Units.newOccupiedPositions.add(newLoc.encode());
            unit.canUseAbility = false;
            unit.canAttack = false;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canPlaceBlueprint (AuxUnit unit, UnitType type, int dir){
        try {
            if (Mapa.planet == Planet.Mars) return false;
            if (Utils.karbonite < Units.getCost(type)) return false;
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            if (!newLoc.isAccessible()) return false;
            if (type == UnitType.Rocket && !Units.canBuildRockets) return false;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void placeBlueprint(AuxUnit unit, UnitType type, int dir) {
        try {
            AuxMapLocation mloc = unit.getMapLocation();
            AuxMapLocation newLoc = mloc.add(dir);
            GC.gc.blueprint(unit.getID(), type, Const.allDirs[dir]);
            Utils.karbonite -= Units.getCost(type);
            Units.newOccupiedPositions.add(newLoc.encode());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canHarvest(AuxUnit unit, int dir){
        try {
            AuxMapLocation loc = unit.getMapLocation();
            AuxMapLocation mineLoc = loc.add(dir);
            if (!mineLoc.isOnMap()) return false;
            return GC.gc.canHarvest(unit.getID(), Const.allDirs[dir]);
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
            AuxMapLocation loc = unit.getMapLocation();
            AuxMapLocation mineLoc = loc.add(dir);
            int karboAmount = Karbonite.karboMap[mineLoc.x][mineLoc.y];
            if (karboAmount == 0) return -1;
            GC.gc.harvest(unit.getID(), Const.allDirs[dir]);
            int newKarboAmount = karboAmount - Units.harvestingPower;
            WorkerUtil.totalKarboCollected += Math.min(karboAmount, Units.harvestingPower);
            if (newKarboAmount < 0) newKarboAmount = 0;
            if (newKarboAmount > 0) {
                Karbonite.karboniteAt.put(mineLoc.encode(), newKarboAmount);
            } else Karbonite.karboniteAt.remove(mineLoc);
            Karbonite.karboMap[mineLoc.x][mineLoc.y] = newKarboAmount;
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
            int d = unit.getMapLocation().distanceSquaredTo(unit2.getMapLocation());
            if (unit.getType() == UnitType.Ranger && d <= 10) return false;
            return (Units.getAttackRange(unit.getType()) >= d);
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void attack(AuxUnit u1, AuxUnit u2){
        try {
            if (u1.getType() != UnitType.Mage) {
                u2.getHealth();
                u2.health -= (int) Units.getDamage(u1.getType());
                if (u2.health <= 0) Units.unitMap[u2.getX()][u2.getY()] = 0;
            } else {
                AuxMapLocation mloc = u2.getMapLocation();
                for (int i = 0; i < 9; ++i) {
                    AuxMapLocation newLoc = mloc.add(i);
                    AuxUnit unit2 = newLoc.getUnit();
                    if (unit2 != null) {
                        unit2.getHealth();
                        unit2.health -= (int) Units.getDamage(u1.getType());
                        if (unit2.health <= 0){
                            Units.unitMap[unit2.getMapLocation().x][unit2.getMapLocation().y] = 0;
                            unit2.canMove = false;
                            unit2.canAttack = false;
                            if (u2.myTeam) {
                                unit2.garrison = true;
                                unit2.mloc = null;
                            }
                        }
                    }
                }
            }
            u1.canAttack = false;
            GC.gc.attack(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static int getArrivalRound(int round){
        try {
            return (int) GC.gc.orbitPattern().duration(round);
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    static void launchRocket(AuxUnit unit, AuxMapLocation loc){
        try {
            //System.out.println("Launching at " + unit.getX() + " " + unit.getY());
            GC.gc.launchRocket(unit.getID(), new MapLocation(Planet.Mars, loc.x, loc.y));
            AuxMapLocation mloc = unit.getMapLocation();
            Units.firstRocket = false;
            Units.unitMap[mloc.x][mloc.y] = 0;
            Units.structures.remove(unit.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static boolean canLoad(AuxUnit u1, AuxUnit u2) {
        try {
            if (u1.getType() != UnitType.Factory && u1.getType() != UnitType.Rocket) return false;
            if (!u2.canMove()) return false;
            if (u1.getGarrisonUnits().size() >= 8) return false;
            if (u2.isInGarrison()) return false;
            if (u1.isBlueprint()) return false;
            return (u1.getMapLocation().distanceSquaredTo(u2.getMapLocation()) <= 2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void load(AuxUnit u1, AuxUnit u2){
        try {
            //System.out.println("Loading! at " + u2.getMapLocation().x + " " + u2.getMapLocation().y + " " + u2.getID());
            AuxMapLocation mloc = u2.getMapLocation();
            Units.unitMap[mloc.x][mloc.y] = 0;
            u2.garrison = true;
            u2.mloc = null;
            u2.canMove = false;
            u2.canAttack = false;
            u1.getGarrisonUnits().add(u2.getID());
            GC.gc.load(u1.getID(), u2.getID());
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void overcharge(AuxUnit u1, AuxUnit u2){
        u1.canUseAbility = false;
        u2.canMove = true;
        u2.canAttack = true;
        u2.canUseAbility = true;
        GC.gc.overcharge(u1.getID(), u2.getID());
    }
}
