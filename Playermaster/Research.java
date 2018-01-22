

import bc.UnitType;

public class Research {

    static Research instance;

<<<<<<< HEAD
    private UnitType[] fixedTree = new UnitType[]{UnitType.Worker, UnitType.Ranger, UnitType.Healer, UnitType.Healer, UnitType.Rocket, UnitType.Ranger, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage};
=======
    private UnitType[] fixedTree = new UnitType[]{UnitType.Worker, UnitType.Ranger, UnitType.Ranger, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage};
>>>>>>> 5a2a7ab... master
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
<<<<<<< HEAD
            e.printStackTrace();
=======
            System.out.println(e);
>>>>>>> 5a2a7ab... master
        }
    }

}
