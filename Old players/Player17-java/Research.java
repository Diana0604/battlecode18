

import bc.UnitType;

public class Research {

    static Research instance;

    private UnitType[] fixedTree = new UnitType[]{UnitType.Ranger, UnitType.Healer, UnitType.Rocket, UnitType.Ranger, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Healer, UnitType.Healer};
    private int stage = 0;

    static Research getInstance(){
        if (instance == null) instance = new Research();
        return instance;
    }

    /*public void checkResearch() {
        if(!gc.researchInfo().hasNextInQueue()) {
            if(stage < fixedTree.length) {
                if (gc.queueResearch(fixedTree[stage]) != 0) {
                    ++stage;
                }
                else System.out.println("Per algun motiu ha fallat el research");
            }
        }
    }*/

    public void yolo(){
        try{
            for (int i = 0; i < fixedTree.length; ++i) Data.gc.queueResearch(fixedTree[i]);
        }catch(Exception e) {
            System.out.println(e);
        }
    }

}
