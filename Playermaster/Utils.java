import bc.Team;
import java.util.ArrayList;

public class Utils {
    static int round;
    static Team myTeam;
    static Team enemyTeam;
    static Integer karbonite;

    static ArrayList<AuxMapLocation> startingLocations, enemyStartingLocations;

    public static void initGame(){
        try {
            round = 1;
            myTeam = GC.gc.team();
            if (myTeam == Team.Blue) enemyTeam = Team.Red;
            else enemyTeam = Team.Blue;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void initTurn(){
        try {
            round++;
            karbonite = (int) GC.gc.karbonite();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    static Integer encode(int i, int j){
        return i* Const.maxMapSize+j;
    }

    static int decodeX(Integer c){
        return c/ Const.maxMapSize;
    }

    static int decodeY(Integer c){
        return c% Const.maxMapSize;
    }
}
