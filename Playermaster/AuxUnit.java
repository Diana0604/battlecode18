import bc.*;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Ivan on 1/18/2018.
 */
public class AuxUnit {
    Integer id;
    public Boolean garrison;
    public Boolean inSpace;
    public AuxMapLocation mloc;


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

    public ArrayList<Integer> garrisonUnits;

    //MOVEMENT STUFF
    public AuxMapLocation target;
    public boolean exploretarget;
    public boolean visited;
    public boolean frontline;
    int depth;

    public AuxUnit(Unit u, boolean team) {
        id = u.id();
        type = u.unitType();
        structure = type == UnitType.Factory || type == UnitType.Rocket;
        blueprint = structure && u.structureIsBuilt() == 0;
        built = structure && !blueprint;
        robot = !structure;
        troop = robot && type != UnitType.Worker;

        Location l = u.location();
        garrison = l.isInGarrison();
        inSpace = l.isInSpace();
        if (inSpace) mloc = null;
        else if (garrison) mloc = new AuxMapLocation(GC.gc.unit(l.structure()).location().mapLocation());
        else mloc = new AuxMapLocation(l.mapLocation());
        l.delete();

        canMove = robot && GC.gc.isMoveReady(getID());

        if (type == UnitType.Ranger || type == UnitType.Knight || type == UnitType.Mage) canAttack = GC.gc.isAttackReady(getID());
        else if (getType() == UnitType.Worker) canAttack = (u.workerHasActed() == 0);
        else if (getType() == UnitType.Healer) canAttack = GC.gc.isHealReady(getID());
        else if (getType() == UnitType.Factory) canAttack = (u.isFactoryProducing() == 0);
        else canAttack = false;

        canUseAbility = robot && u.abilityHeat() < 10;

        garrisonUnits = new ArrayList<>();
        if (structure) {
            VecUnitID v = u.structureGarrison();
            for (int i = 0; i < v.size(); ++i) garrisonUnits.add(v.get(i));
            v.delete();
        }

        health = (int) u.health();
        exploretarget = false;
        target = null;
        visited = false;
        frontline = false;
        myTeam = team;
        depth = 0;
    }

    public int getID(){
        return id;
    }

    public boolean isInGarrison(){
        return garrison;
    }

    public boolean isInSpace(){
        return inSpace;
    }


    public AuxMapLocation getMapLocation(){
        return mloc;
    }

    public int getX(){
        return mloc.x;
    }

    public int getY(){
        return mloc.y;
    }

    public boolean canMove(){
        return canMove;
    }

    public boolean canAttack(){
        return canAttack;
    }

    public boolean canUseAbility(){
        return canUseAbility;
    }

    public UnitType getType(){
        return type;
    }


    public ArrayList<Integer> getGarrisonUnits(){
        return garrisonUnits;
    }

    public Integer getHealth(){
        return health;
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
        return structure;
    }

    public boolean isRobot(){
        return robot;
    }

    public boolean isTroop(){
        return troop;
    }

    public boolean isBlueprint(){
        return blueprint;
    }

    public boolean isBuilt(){
        return built;
    }


}
