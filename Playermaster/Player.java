// import the API.
// See xxx for the javadocs.
import bc.*;

public class Player {
    public static void main(String[] args) {
        long initTime = System.nanoTime();
        GameController gc = new GameController();
        ConstructionQueue queue = new ConstructionQueue();
        // Init pathfinder
        UnitManager.initialize(gc,queue);
        UnitManager unitManager = UnitManager.getInstance();
        Pathfinder.getInstance();

        while (true) {
            long roundTime = System.nanoTime();
            //System.out.println("Start of round " + gc.round());
            unitManager.update();
            unitManager.moveUnits();
            long endTime = System.nanoTime();
            long usedTime = endTime - roundTime;
            //if (usedTime > 10000000) System.out.println("Round time: " + usedTime + "    Total time: " + (endTime - initTime));
            gc.nextTurn();
        }
    }
}
