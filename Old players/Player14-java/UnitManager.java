import bc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

class UnitManager{
    private static UnitManager instance;
    static GameController gc;

    static void initialize(GameController _gc){
        gc = _gc;
    }

    static UnitManager getInstance(){
        if (instance == null) instance = new UnitManager();
        return instance;
    }

    UnitManager(){
        try{
            Data.initGame(gc);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    void moveUnits() {
        try {
            for (int i = 0; i < Data.myUnits.length; i++) {
                AuxUnit unit = Data.myUnits[i];
                //if (unit.getType() == UnitType.Factory) System.err.println("I'm a factory! :D");
                if (unit.getType() == UnitType.Rocket) {
                    Rocket.getInstance().playFirst(unit);
                }
            }
            for (int i = 0; i < Data.myUnits.length; i++) {
                //System.err.println("playing unit " + Data.myUnits[i].getType());
                AuxUnit unit = Data.myUnits[i];
                if (unit.isInGarrison()) continue;
                if (unit.getType() == UnitType.Worker) {
                    Worker.getInstance().play(unit);
                }
                if (unit.getType() == UnitType.Factory) {
                    Factory.getInstance().play(unit);
                }
                if (unit.getType() == UnitType.Ranger) {
                    Ranger.getInstance().play(unit);
                }
                if (unit.getType() == UnitType.Rocket) {
                    Rocket.getInstance().play(unit);
                }
                if (unit.getType() == UnitType.Healer) {
                    Healer.getInstance().play(unit);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    static void initWorkerMaps(){
        try {
            Data.blueprintsToBuild = new HashMap<>();
            Data.blueprintsToPlace = new HashMap<>();
            Data.structuresToRepair = new HashMap<>();
            HashSet<AuxUnit> workers = new HashSet<>();
            for (AuxUnit unit : Data.myUnits) {
                if (unit.getType() == UnitType.Worker) workers.add(unit);
            }

            for (int index : Data.structures) {
                //Per cada blueprint, crida els 8 workers mes propers a construir-lo
                AuxUnit bp = Data.myUnits[index];
                if (bp.isMaxHealth()) continue;
                if (!bp.isBlueprint()) continue;

                ArrayList<Pair> sorted = new ArrayList<>();
                for (AuxUnit worker : workers) {
                    AuxMapLocation workerloc = worker.getMaplocation();
                    if (workerloc == null) continue;
                    Pair p = new Pair(bp.getMaplocation().distanceSquaredTo(worker.getMaplocation()), worker);
                    sorted.add(p);
                }
                sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
                List<Pair> cut = sorted.subList(0, Math.min(8, sorted.size())); //no se si funciona si hi ha menys de 8 workers total
                for (Pair p : cut) {
                    int key = p.unit.getID();
                    int value = bp.getID();
                    if (!Data.blueprintsToBuild.containsKey(key)) Data.blueprintsToBuild.put(key, value);
                }
            }

            for (int index : Data.structures) {
                //Per cada blueprint, crida els 8 workers mes propers a construir-lo
                AuxUnit s = Data.myUnits[index];
                if (s.isMaxHealth()) continue;
                if (s.isBlueprint()) continue;

                final int WORKERS_TO_CALL = 3;
                ArrayList<Pair> sorted = new ArrayList<>();
                for (AuxUnit worker : workers) {
                    //no fiquem workers si ja son cridats per un blueprint
                    if (Data.blueprintsToBuild.containsKey(worker.getID())) continue;
                    Pair p = new Pair(s.getMaplocation().distanceSquaredTo(worker.getMaplocation()), worker);
                    sorted.add(p);
                }
                sorted.sort((a, b) -> a.dist < b.dist ? -1 : a.dist == b.dist ? 0 : 1);
                List<Pair> cut = sorted.subList(0, Math.min(WORKERS_TO_CALL, sorted.size())); //no se si funciona si hi ha menys de 8 workers total
                for (Pair p : cut) {
                    int key = p.unit.getID();
                    int value = s.getID();
                    if (!Data.structuresToRepair.containsKey(key)) Data.structuresToRepair.put(key, value);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    private static class Pair {
        double dist;
        AuxUnit unit;

        Pair(double dist, AuxUnit unit){
            this.dist = dist;
            this.unit = unit;
        }
    }

}
