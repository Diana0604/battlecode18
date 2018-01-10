// import the API.
// See xxx for the javadocs.
import bc.*;

public class Player {
    public static void main(String[] args) {
        // Connect to the manager, starting the game
        GameController gc = new GameController();
        // Init pathfinder
        MyUnit.initialize(gc);



        while (true) {
            // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
            VecUnit units = gc.myUnits();

            MyUnit.update();

            for (int i = 0; i < units.size(); i++) {
                Unit unit = units.get(i);
                if (unit.unitType() == UnitType.Worker) {
                    Worker thisWorker = new Worker();
                    thisWorker.play(unit);
                }
            }
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
}