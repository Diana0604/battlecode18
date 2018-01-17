import bc.GameController;
import bc.UnitType;

/**
 * Created by David on 16/01/2018.
 */
public class Research {

    static GameController gc;
    static Research instance;

    private UnitType[] fixedTree = new UnitType[]{UnitType.Worker, UnitType.Rocket, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};
    private int stage = 0;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static Research getInstance(){
        if (instance == null) instance = new Research();
        return instance;
    }

    public void checkResearch() {
        if(!gc.researchInfo().hasNextInQueue()) {
            if(stage < fixedTree.length) {
                if (gc.queueResearch(fixedTree[stage]) != 0) {
                    ++stage;
                }
                else System.out.println("Per algun motiu ha fallat el research");
            }
        }
    }

}
