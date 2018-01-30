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

    public boolean isBlueprint(){
        try {
            if (blueprint == null) blueprint = (unit.structureIsBuilt() == 0);
            return blueprint;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public AuxMapLocation getMaplocation(){
        try {
            if (isInGarrison()) return null;
            if (mloc == null) mloc = new AuxMapLocation(getLocation().mapLocation());
            //System.out.println("map location " + mloc);
            return mloc;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getX(){
        try {
            return getMaplocation().x;
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    public int getY(){
        try {
            return getMaplocation().y;
        }catch(Exception e) {
            e.printStackTrace();
            return Integer.parseInt(null);
        }
    }

    public boolean canMove(){
        try {
            if (canMove == null) {
                if (getType() != UnitType.Factory && getType() != UnitType.Rocket)
                    canMove = Data.gc.isMoveReady(getID());
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
                    canAttack = Data.gc.isAttackReady(getID());
                else if (getType() == UnitType.Worker) canAttack = (unit.workerHasActed() == 0);
                else if (getType() == UnitType.Healer) canAttack = Data.gc.isHealReady(getID());
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

    public boolean getIsBuilt(){
        try {
            if (isBuilt == null) isBuilt = (unit.structureIsBuilt() > 0);
            return isBuilt;
        }catch(Exception e) {
            e.printStackTrace();
            return true;
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
            return Objects.equals(getHealth(), getMaxHealth()); //no se si funciona amb .equals()?
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }



}
