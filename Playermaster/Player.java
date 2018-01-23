

import bc.*;

public class Player {
    public static void main(String[] args) {
        try {
            long initTime = System.nanoTime();
            GameController gc = new GameController();

            Vision.initialize();

            // Init pathfinder
            UnitManager.initialize(gc);
            UnitManager unitManager = UnitManager.getInstance();
            Pathfinder.getInstance();
            Data.initGameAfterPathfinder();

            while (true) {
                try {
                    if (Data.round % 100 == 99) System.gc();


                    //long roundTime = System.nanoTime();
                    //System.out.println("Start of round " + gc.round());
                    Data.initTurn();

                    //if(Data.onEarth() && Data.round >= 745) System.err.println("Before moving");

                    //if(Data.onEarth() && Data.round >= 745) Data.printData();
                    //unitManager.update();
                    unitManager.moveUnits();

                    //if(Data.onEarth() && Data.round >= 745) System.err.println("After moving");

                    //if(Data.onEarth() && Data.round >= 745) Data.printData();
                    //long endTime = System.nanoTime();
                    //long usedTime = endTime - roundTime;
                    //if (usedTime > 10000000) System.out.println("Round time: " + usedTime + "    Total time: " + (endTime - initTime));
                    gc.nextTurn();
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
