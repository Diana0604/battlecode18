

import bc.*;

public class ConstructionQueue {

    private int typeToInt(UnitType type){
        switch(type){
            case Factory:
                return 0;
            case Worker:
                return 1;
            case Ranger:
                return 2;
            case Knight:
                return 3;
            case Mage:
                return 4;
            case Rocket:
                return 5;
            case Healer:
                return 6;
            default:
                return 2;
        }
    }

    private boolean[] needed = {false, false, false, false, false, false, false};

    public void print(){
        System.out.println("Units requested:");
        for (UnitType type: UnitType.values()){
            System.out.println(type + ": " + needed[typeToInt(type)]);
        }
    }

    boolean needsUnit(UnitType type){
        return needed[typeToInt(type)];
    }

    void requestUnit(UnitType type, boolean b){
        needed[typeToInt(type)] = b;
    }

    void requestUnit(UnitType type){
        requestUnit(type,true);
    }



}
