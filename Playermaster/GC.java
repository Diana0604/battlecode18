import bc.*;

class GC {
    static GameController gc;

    static void initGame(GameController _gc){
        try {
            gc = _gc;
            Vision.initialize();
            Research.initGame();
            Utils.initGame();
            MarsPlanning.initGame();
            Mapa.initGame();
            Rocket.initGame();
            Karbonite.initGame(); //ha d'anar despres de Mapa
            Units.initGame(); //ha d'anar despres de Mapa
            Pathfinder.initGame(); //ha d'anar despres de Mapa i Karbonite
            Explore.initGame(); //ha d'anar despres de Mapa
            WorkerUtil.initGame(); //ha d'anar despres de Mapa i Pathfinder
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void initTurn() {
        try {
            Utils.initTurn();
            Research.initTurn();
            Units.initTurn(); //ha d'anar despres de Utils i Research
            Karbonite.initTurn(); //ha d'anar despres de Units
            Communication.initTurn(); // ha d'anar abans que Explore
            Explore.initTurn(); //ha d'anar despres de Units
            Danger.initTurn(); //ha d'anar despres de Units
            WorkerUtil.initTurn(); //ha d'anar despres de Units i Karbonite
            Rocket.initTurn(); //ha d'anar despres de Units i Danger
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void endTurn() {
        try {
            Communication.endTurn();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void playUnits(){
        UnitManager.moveUnits();
    }
}
