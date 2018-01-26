import bc.*;

public class Player {
    public static void main(String[] args) {
        try {
            GameController gc = new GameController();
            GC.initGame(gc);
            while (true) {
                try {
                    GC.initTurn();
                    //if(GC.onEarth() && GC.round >= 745) System.err.println("Before moving");
                    //if(GC.onEarth() && GC.round >= 745) GC.printData();
                    //unitManager.update();
                    GC.playUnits();
                    //if(GC.onEarth() && GC.round >= 745) System.err.println("After moving");
                    //if(GC.onEarth() && GC.round >= 745) GC.printData();
                    GC.endTurn();

                    if (Utils.round % 100 == 1) {
                        //System.out.println("Before gc");
                        System.gc();
                        //System.out.println("After  gc");
                    }
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
