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
}
