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
            Karbonite.initGame();
            Mapa.initGame();
            Units.initGame(); //ha d'anar despres de Mapa
            WorkerUtil.initGame(); //ha d'anar despres de Mapa
            Pathfinder.initGame(); //ha d'anar despres de Mapa
            Explore.initGame(); //ha d'anar despres de Mapa
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void initTurn() {
        try {
            Utils.initTurn();
            Research.initTurn();
            Rocket.initTurn();
            Units.initTurn(); //ha d'anar despres de Utils i Research
            Karbonite.initTurn(); //ha d'anar despres de Units
            Explore.initTurn(); //ha d'anar despres de Units
            Danger.initTurn(); //ha d'anar despres de Units
            WorkerUtil.initTurn(); //ha d'anar despres de Units i Karbonite
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void playUnits(){
        UnitManager.moveUnits();
    }
}
