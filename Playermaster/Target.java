import bc.UnitType;

public class Target {

    static int[][] rangerTargets;

    static void initTurn() {
        initMatrixs();
    }

    static void initMatrixs() {
        rangerTargets = new int[Mapa.W][Mapa.H];
        updateRangeMatrix(rangerTargets, UnitType.Ranger);
    }

    static void updateRangeMatrix(int[][] targets, UnitType unitType) {
        int range = Units.getAttackRange(unitType);
        for (AuxUnit unit: Units.enemies) {
            AuxMapLocation loc = unit.mloc;
            for (int i = 0; i < Vision.Mx[range].length; ++i) {
                AuxMapLocation newLoc = loc.add(new AuxMapLocation(Vision.Mx[range][i], Vision.My[range][i]));
                if (newLoc.isOnMap()) {
                    targets[newLoc.x][newLoc.y] += Ranger.getInstance().typePriority(unit.type);
                }
            }
        }
    }



}
