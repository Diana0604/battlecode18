import bc.*;

public class Worker extends MyUnit {

    Worker(){
        
    }

    void play(Unit unit){
        move(unit);
    }

    void move(Unit unit){
        Direction dir = dirTo(unit, Planet.Earth, 15, 15);
        if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), dir))  gc.moveRobot(unit.id(), dir);
    }

    Direction dirTo(Unit unit, Planet pl, int destX, int destY){
        MapLocation myLoc = unit.location().mapLocation();
        PathfinderNode myNode = Pathfinder.getInstance().getNode(pl, myLoc.getX() ,myLoc.getY() , destX, destY);
        return myNode.dir;
    }
}