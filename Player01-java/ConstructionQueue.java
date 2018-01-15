import bc.*;

public class ConstructionQueue {
    private boolean[] needed = {false, false, false, false, false, false, false};

    public void print(){
        System.out.println("Units requested:");
        for (UnitType type: UnitType.values()){
            System.out.println(type + ": " + needed[type.swigValue()]);
        }
    }

    boolean needsUnit(UnitType type){
        return needed[type.swigValue()];
    }

    void requestUnit(UnitType type, boolean b){
        needed[type.swigValue()] = b;
    }

    void requestUnit(UnitType type){
        requestUnit(type,true);
    }



}
