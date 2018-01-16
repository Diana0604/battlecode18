import bc.*;

public class EnemyData {

    static EnemyData instance = null;

    static EnemyData getInstance(){
        if (instance == null) instance = new EnemyData();
        return instance;
    }

    double[][] DPS;
    int[][] minDist;

    public EnemyData(){
        DPS = new double[UnitManager.W][UnitManager.H];
        minDist = new int[UnitManager.W][UnitManager.H];
    }

    public void updateMatrices(){
        for (int i = 0; i < UnitManager.getInstance().enemyUnits.size(); ++i){
            Unit enemy = UnitManager.getInstance().enemyUnits.get(i);
            //fillDist(enemy);
            //if (isAggro(enemy.unitType())) fillDPS(enemy);
        }
    }

}
