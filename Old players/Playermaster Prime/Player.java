import bc.*;

public class Player {
    public static void main(String[] args) {
        try {
            GameController gc = new GameController();
            GC.initGame(gc);


            System.out.println(WorkerUtil.closeFactory);
            while (true) {
                try {

                    //System.out.println("Round " + Utils.round);

                    GC.initTurn();

                    GC.playUnits();

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
