// import the API.
// See xxx for the javadocs.
import bc.*;

public class Player {
    public static void main(String[] args) {
        // Connect to the manager, starting the game
        GameController gc = new GameController();
        // Init pathfinder
        UnitManager.initialize(gc);
        UnitManager unitManager = UnitManager.getInstance();

        while (true) {
            unitManager.update();
            unitManager.moveUnits();
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
}