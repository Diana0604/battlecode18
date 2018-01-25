import bc.*;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Ivan on 1/18/2018.
 */
public class AuxUnit {
    Unit unit;
    Integer id;
    public Location loc;
    public Boolean garrison;
    public Boolean inSpace;
    public AuxMapLocation mloc;
    public int VisionRange;


    //private Integer x;
    //private Integer y;
    public Boolean built;
    public Boolean canMove;
    public Boolean canAttack; //for workers it counts harvest as action, and healers == heal
    public Boolean canUseAbility;
    public Boolean structure;
    public Boolean blueprint;
    public Boolean robot;
    public Boolean troop;
    public UnitType type;
    public boolean myTeam;

    public Integer health;
    public Integer maxHealth;

    public ArrayList<Integer> garrisonUnits;

    //MOVEMENT STUFF
    public AuxMapLocation target;
    public boolean exploretarget;
    public boolean visited;
    public boolean frontline;
    int depth;

    public AuxUnit(Unit _unit, boolean team){
        unit = _unit;
        id = null;
        loc = null;
        type = null;
        structure = null;
        troop = null;
        robot = null;
        blueprint = null;
        built = null;
        garrison = null;
        inSpace = null;
        mloc = null;
        canMove = null;
        canAttack = null;
        canUseAbility = null;
        garrisonUnits = null;
        health = null;
        maxHealth = null;
        exploretarget = false;
        target = null;
        visited = false;
        frontline = false;
        myTeam = team;
        depth = 0;
        VisionRange = -1;
    }

    public int getID(){
        try {
            if (id == null) id = unit.id();
            return id;
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    public Location getLocation(){
        try {
            if (loc == null) loc = unit.location();
            return loc;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isInGarrison(){
        try {
            if (garrison == null) garrison = getLocation().isInGarrison();
            return garrison;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isInSpace(){
        try {
            if (inSpace == null) inSpace = getLocation().isInSpace();
            return inSpace;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public AuxMapLocation getMapLocation(){
        try {
            if (isInSpace()) return null;
            if (mloc == null){
                if (isInGarrison()) mloc = Units.getUnitByID(getLocation().structure()).getMapLocation();
                else mloc = new AuxMapLocation(getLocation().mapLocation());
            }
            //System.out.println("map location " + mloc);
            return mloc;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getX(){
        try {
            return getMapLocation().x;
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    public int getY(){
        try {
            return getMapLocation().y;
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    public boolean canMove(){
        try {
            if (canMove == null) {
                if (getType() != UnitType.Factory && getType() != UnitType.Rocket)
                    canMove = GC.gc.isMoveReady(getID());
                else canMove = false;
            }
            return canMove;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean canAttack(){
        try {
            if (canAttack == null) {
                if (!(getType() == UnitType.Factory || getType() == UnitType.Rocket || getType() == UnitType.Worker || getType() == UnitType.Healer))
                    canAttack = GC.gc.isAttackReady(getID());
                else if (getType() == UnitType.Worker) canAttack = (unit.workerHasActed() == 0);
                else if (getType() == UnitType.Healer) canAttack = GC.gc.isHealReady(getID());
                else if (getType() == UnitType.Factory) canAttack = (unit.isFactoryProducing() == 0);
                else canAttack = false;
            }
            return canAttack;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean canUseAbility(){
        try {
            if (canUseAbility == null) canUseAbility = unit.abilityHeat() < 10;
            return canUseAbility;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public UnitType getType(){
        try {
            if (type == null) type = unit.unitType();
            return type;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public ArrayList<Integer> getGarrisonUnits(){
        try {
            if (garrisonUnits == null) {
                garrisonUnits = new ArrayList<>();
                VecUnitID v = unit.structureGarrison();
                for (int i = 0; i < v.size(); ++i) garrisonUnits.add(v.get(i));
            }
            return garrisonUnits;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Integer getHealth(){
        try {
            if (health == null) health = (int) unit.health();
            return health;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Integer getMaxHealth(){
        try {
            if (maxHealth == null) maxHealth = (int) unit.maxHealth();
            return maxHealth;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isMaxHealth() {
        try {
            return (getHealth() >= Units.getMaxHealth(getType())); //no se si funciona amb .equals()?
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isStructure(){
        if (structure == null) structure = (getType() == UnitType.Factory || getType() == UnitType.Rocket);
        return structure;
    }

    public boolean isRobot(){
        if (robot == null) robot = !isStructure();
        return robot;
    }

    public boolean isTroop(){
        if (troop == null) troop = isRobot() && getType() != UnitType.Worker;
        return troop;
    }

    public boolean isBlueprint(){
        if (blueprint == null) blueprint = isStructure() && unit.structureIsBuilt() == 0;
        return blueprint;
    }

    public boolean isBuilt(){
        if (built == null) built = isStructure() && unit.structureIsBuilt() > 0;
        return built;
    }


    public int getVisionRange(){
        try {
            if (VisionRange == -1) {
                UnitType myType = getType();
                switch(myType) {
                    case Ranger:
                        VisionRange = Const.rangerVisionRange;
                    case Healer:
                        VisionRange = Const.healerVisionRange;
                    case Mage:
                        VisionRange = Const.mageVisionRange;
                    case Worker:
                        VisionRange = Const.workerVisionRange;
                    case Knight:
                        VisionRange = Const.knightVisionRange;
                    case Rocket:
                        VisionRange = Const.rocketVisionRange;
                    case Factory:
                        VisionRange = Const.factoryVisionRange;
                }
            }
            return VisionRange;
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }


}
