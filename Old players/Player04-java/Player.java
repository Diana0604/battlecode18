
// See xxx for the javadocs.
import bc.*;

public class Player {
    public static void main(String[] args) {
        GameController gc = new GameController();
        ConstructionQueue queue = new ConstructionQueue();
        // Init pathfinder
        UnitManager.initialize(gc,queue);
        UnitManager unitManager = UnitManager.getInstance();
        Pathfinder.getInstance();

        while (true) {
            System.out.println("Start of round " + gc.round());
            unitManager.update();
            unitManager.moveUnits();
            gc.nextTurn();
        }
    }
}
