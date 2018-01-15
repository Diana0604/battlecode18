import bc.*;

/**
 * Created by Pau on 14/01/2018.
 */


public class ConstructionQueue {
    private boolean[] needed = {false, false, false, false, false, false, false};

    public void print(){
        System.out.println("Units requested:");
        for (UnitType type: UnitType.values()){
            System.out.println(type + ": " + needed[type.swigValue()]);
        }
    }

    public boolean needsUnit (UnitType type){
        return needed[type.swigValue()];
    }

    public void requestUnit (UnitType type, boolean b){
        needed[type.swigValue()] = b;
    }

    public void requestUnit (UnitType type){
        requestUnit(type,true);
    }



}
