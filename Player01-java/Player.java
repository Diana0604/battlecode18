// import the API.
// See xxx for the javadocs.
import bc.*;

public class Player {
    public static void main(String[] args) {
        // Connect to the manager, starting the game
        GameController gc = new GameController();
        ConstructionQueue queue = new ConstructionQueue();
        // Init pathfinder
        UnitManager.initialize(gc,queue);
        UnitManager unitManager = UnitManager.getInstance();

        while (true) {
            System.out.println("Start of round " + gc.round());

            unitManager.update();
            unitManager.moveUnits();
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
}