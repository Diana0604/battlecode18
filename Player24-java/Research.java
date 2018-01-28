

import bc.ResearchInfo;
import bc.UnitType;

public class Research {
    static ResearchInfo researchInfo;

    //es fa l'ultim initgame
    public static void initGame(){
        try{
            pickResearch();
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
        if (Mapa.onMars()) return;
        UnitType[] R1 = new UnitType[]{UnitType.Rocket, UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};
        UnitType[] R2 = new UnitType[]{UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Rocket, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};
        UnitType[] R3 = new UnitType[]{UnitType.Healer, UnitType.Healer, UnitType.Healer, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Mage, UnitType.Rocket, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger};
        UnitType[] tree;
        if (Build.isolated) {
            System.out.println("Isolated, triem R1");
            tree = R1;
        }else if (Build.initDistToEnemy < 25) {
            System.out.println("A tocar l'enemic, triem R3");
            tree = R3;
        }else if (MarsPlanning.biggestAreaPercent < 0.25 || MarsPlanning.biggestArea < 30) {
            System.out.println("Mars es shit, triem R3");
            tree = R3;
        }else {
            System.out.println("Default, triem R2");
            tree = R2;
        }

        for (UnitType tech : tree) GC.gc.queueResearch(tech);

    }


    public static void initTurn(){
        try {
            researchInfo = GC.gc.researchInfo();
            if (!Build.canBuildRockets && researchInfo.getLevel(UnitType.Rocket) == 1) Build.troopsSinceRocketResearch = 0;
        }catch(Exception e) {
            e.printStackTrace();
        }
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
