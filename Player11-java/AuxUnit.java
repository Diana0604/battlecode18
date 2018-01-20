import bc.*;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Ivan on 1/18/2018.
 */
public class AuxUnit {
    Unit unit;
    public Integer id;
    public Location loc;
    public Boolean garrison;
    public Boolean inSpace;
    public Boolean blueprint;
    public AuxMapLocation mloc;
    //private Integer x;
    //private Integer y;
    public Boolean canMove;
    public Boolean canAttack; //for workers it counts harvest as action, and healers == heal
    public Boolean canUseAbility;
    public UnitType type;

    public Boolean isBuilt;
    //Team team; no cal crec

    public Integer health;
    public Integer maxHealth;

    public ArrayList<Integer> garrisonUnits;

    public AuxUnit(Unit _unit){
        unit = _unit;
        id = null;
        loc = null;
        garrison = null;
        inSpace = null;
        blueprint = null;
        mloc = null;
        canMove = null;
        canAttack = null;
        canUseAbility = null;
        type = null;
        isBuilt = null;
        garrisonUnits = null;
        health = null;
        maxHealth = null;
    }

    public int getID(){
        if (id == null) id = unit.id();
        return id;
    }

    public Location getLocation(){
        if (loc == null) loc = unit.location();
        return loc;
    }

    public boolean isInGarrison(){
        if (garrison == null) garrison = getLocation().isInGarrison();
        return garrison;
    }

    public boolean isInSpace(){
        if (inSpace == null) inSpace = getLocation().isInSpace();
        return inSpace;
    }

    public boolean isBlueprint(){
        if (blueprint == null) blueprint = (unit.structureIsBuilt() == 0);
        return blueprint;
    }

    public AuxMapLocation getMaplocation(){
        if (isInGarrison()) return null;
        mloc = new AuxMapLocation(getLocation().mapLocation());
        //System.out.println("map location " + mloc);
        return mloc;
    }

    public int getX(){
        return getMaplocation().x;
    }

    public int getY(){
        return getMaplocation().y;
    }

    public boolean canMove(){
        if (canMove == null){
            if (getType() != UnitType.Factory && getType() != UnitType.Rocket) canMove = Data.gc.isMoveReady(getID());
            else canMove = false;
        }
        return canMove;
    }

    public boolean canAttack(){
        if (canAttack == null){
            if (!(getType() == UnitType.Factory || getType() == UnitType.Rocket || getType() == UnitType.Worker || getType() == UnitType.Healer)) canAttack = Data.gc.isAttackReady(getID());
            else if (getType() == UnitType.Worker) canAttack = (unit.workerHasActed() == 0);
            else if (getType() == UnitType.Healer) canAttack = Data.gc.isHealReady(getID());
            else if (getType() == UnitType.Factory) canAttack = (unit.isFactoryProducing() == 0);
            else canAttack = false;
        }
        return canAttack;
    }

    public boolean canUseAbility(){
        if (canUseAbility == null) canUseAbility = unit.abilityHeat() < 10;
        return canUseAbility;
    }

    public UnitType getType(){
        if (type == null) type = unit.unitType();
        return type;
    }

    public boolean getIsBuilt(){
        if (isBuilt == null) isBuilt = (unit.structureIsBuilt() > 0);
        return isBuilt;
    }

    public ArrayList<Integer> getGarrisonUnits(){
        if (garrisonUnits == null){
            garrisonUnits = new ArrayList<>();
            VecUnitID v = unit.structureGarrison();
            for (int i = 0; i < v.size(); ++i) garrisonUnits.add(v.get(i));
        }
        return garrisonUnits;
    }

    public Integer getHealth(){
        if (health == null) health = (int)unit.health();
        return health;
    }

    public Integer getMaxHealth(){
        if (maxHealth == null) maxHealth = (int)unit.maxHealth();
        return maxHealth;
    }

    public boolean isMaxHealth(){
        return Objects.equals(getHealth(), getMaxHealth()); //no se si funciona amb .equals()?
    }



}
