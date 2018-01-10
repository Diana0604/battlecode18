import bc.*;

class MyUnit{
    static Pathfinder pathfinder;
    static GameController gc;

    static void initialize(GameController _gc){
        pathfinder = new Pathfinder(_gc);
        gc = _gc;
    }

    static void update(){
        return;
    }
}