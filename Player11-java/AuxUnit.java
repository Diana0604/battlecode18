import bc.*;

import java.util.ArrayList;

/**
 * Created by Ivan on 1/18/2018.
 */
public class AuxUnit {
    Unit unit;
    public Integer id;
    public Location loc;
    public Boolean garrison;
    public AuxMapLocation mloc;
    //private Integer x;
    //private Integer y;
    public Boolean canMove;
    public Boolean canAttack; //for workers it counts as action and healers == heal
    public UnitType type;

    public Boolean[] canMoveTo;

    public Boolean isBuilt;
    //Team team; no cal crec

    public Integer health;

    public ArrayList<Integer> garrisonUnits;

    public AuxUnit(Unit _unit){
        unit = _unit;
        id = null;
        loc = null;
        garrison = null;
        mloc = null;
        canMove = null;
        canAttack = null;
        type = null;
        canMoveTo = new Boolean[9];
        for (int i = 0; i < 9; ++i) canMoveTo[i] = null;
        isBuilt = null;
        garrisonUnits = null;
        health = null;
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

    public AuxMapLocation getMaplocation(){
        if (isInGarrison()) return null;
        mloc = new AuxMapLocation(getLocation().mapLocation());
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
            if (getType() == UnitType.Factory || getType() == UnitType.Rocket) canMove = UnitManager.gc.isMoveReady(getID());
            else canMove = false;
        }
        return canMove;
    }

    public boolean canAttack(){
        if (canAttack == null){
            if (!(getType() == UnitType.Factory || getType() == UnitType.Rocket || getType() == UnitType.Worker || getType() == UnitType.Healer)) canAttack = UnitManager.gc.isAttackReady(getID());
            else if (getType() == UnitType.Worker) canAttack = (unit.workerHasActed() > 0);
            else if (getType() == UnitType.Healer) canAttack = Data.gc.isHealReady(getID());
            else canAttack = false;
        }
        return canAttack;
    }

    public UnitType getType(){
        if (type == null) type = unit.unitType();
        return type;
    }

    public boolean getCanMoveTo(int i){
        if (canMoveTo[i] == null) canMoveTo[i] = UnitManager.gc.canMove(getID(), Data.allDirs[i]);
        return canMoveTo[i];
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



}
