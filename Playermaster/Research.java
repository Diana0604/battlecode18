

import bc.ResearchInfo;
import bc.UnitType;

public class Research {
    static ResearchInfo researchInfo;
    private static UnitType[] fixedTree = new UnitType[]{UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Rocket, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};

    //es fa l'ultim initgame
    public static void initGame(){
        try{
            for (UnitType tech : fixedTree) GC.gc.queueResearch(tech);
            researchInfo = GC.gc.researchInfo();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void pickResearch(){
        /*
- Si isolated, R H M
- si initDist molt petit (<25), H M R
- si initDist gran (> 60) i kp350 no petit (>300) R H M
- is size molt gran (> 0.95), H R M
*/
        UnitType[] RHM = new UnitType[]{UnitType.Rocket, UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};
        UnitType[] HRM = new UnitType[]{UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Rocket, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};
        UnitType[] HMR = new UnitType[]{UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Rocket, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};
        UnitType[] tree;



        if (Units.isolated) tree = RHM;
        else if (Units.initDistToEnemy > 60 && MarsPlanning.karbo350 > 500) tree = RHM;
        else if (Units.initDistToEnemy < 25) tree = HMR;
        else if
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
