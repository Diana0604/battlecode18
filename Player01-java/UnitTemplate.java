import bc.*;

public class UnitTemplate {

    static UnitTemplate instance = null;
    static GameController gc;

    static UnitTemplate getInstance(){
        if (instance == null){
            instance = new UnitTemplate();
            gc = MovementManager.gc;
        }
        return instance;
    }

    void play(Unit unit){
    }
}