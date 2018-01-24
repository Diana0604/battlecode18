

import bc.ResearchInfo;
import bc.UnitType;

public class Research {
    static ResearchInfo researchInfo;
    private static UnitType[] fixedTree = new UnitType[]{UnitType.Ranger, UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Rocket, UnitType.Ranger, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage};

    public static void initGame(){
        try{
            for (UnitType tech : fixedTree) GC.gc.queueResearch(tech);
            researchInfo = GC.gc.researchInfo();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void initTurn(){
        researchInfo = GC.gc.researchInfo();
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
}
