

import bc.ResearchInfo;
import bc.UnitType;

public class Research {
    static ResearchInfo researchInfo;
    private static UnitType[] fixedTree = new UnitType[]{UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Rocket, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};

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
        if (!Units.canBuildRockets && researchInfo.getLevel(UnitType.Rocket) == 1) Units.troopsSinceRocketResearch = 0;
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
