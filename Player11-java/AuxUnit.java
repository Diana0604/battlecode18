import bc.*;
/**
 * Created by Ivan on 1/18/2018.
 */
public class AuxUnit {
    Unit unit;
    private Integer id;
    private Location loc;
    private Boolean garrison;
    private MapLocation mloc;
    private Integer x;
    private Integer y;
    private Boolean canMove;
    private Boolean canAttack; //for workers it counts as action
    private UnitType type;
    //Team team; no cal crec

    public AuxUnit(Unit _unit){
        unit = _unit;
        id = null;
        loc = null;
        garrison = null;
        mloc = null;
        x = null;
        y = null;
        canMove = null;
        canAttack = null;
        type = null;
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

    public MapLocation getMaplocation(){
        if (isInGarrison()) return null;
        mloc = getLocation().mapLocation();
        return mloc;
    }

    public int getX(){
        if (x == null) x = getMaplocation().getX();
        return x;
    }

    public int getY(){
        if (y == null) y = getMaplocation().getY();
        return y;
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
            if (getType() == UnitType.Factory || getType() == UnitType.Rocket || getType() == UnitType.Worker) canAttack = UnitManager.gc.isAttackReady(getID());
            else if (getType() == UnitType.Worker) canAttack = (unit.workerHasActed() > 0);
            else canAttack = false;
        }
        return canAttack;
    }

    public UnitType getType(){
        if (type == null) type = unit.unitType();
        return type;
    }

}
