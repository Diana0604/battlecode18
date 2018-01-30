import bc.Team;

public class Utils {
    static int round;
    static Team myTeam;
    static Team enemyTeam;
    static Integer karbonite;

    public static void initGame(){
        round = 1;
        myTeam = GC.gc.team();
        if (myTeam == Team.Blue) enemyTeam = Team.Red;
        else enemyTeam = Team.Blue;
    }

    public static void initTurn(){
        round++;
        karbonite = (int) GC.gc.karbonite();
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
