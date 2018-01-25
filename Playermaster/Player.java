import bc.*;

public class Player {
    public static void main(String[] args) {
        try {
            GameController gc = new GameController();
            GC.initGame(gc);
            while (true) {
                try {
                    if (Utils.round % 10 == 1) System.gc();
                    GC.initTurn();
                    //if(GC.onEarth() && GC.round >= 745) System.err.println("Before moving");
                    //if(GC.onEarth() && GC.round >= 745) GC.printData();
                    //unitManager.update();
                    GC.playUnits();
                    //if(GC.onEarth() && GC.round >= 745) System.err.println("After moving");
                    //if(GC.onEarth() && GC.round >= 745) GC.printData();
                    GC.endTurn();
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
