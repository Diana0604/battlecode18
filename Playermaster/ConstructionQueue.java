

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
        try {
            return needed[typeToInt(type)];
        }catch(Exception e) {
<<<<<<< HEAD
            e.printStackTrace();
=======
            System.out.println(e);
>>>>>>> 5a2a7ab... master
            return false;
        }
    }

    void requestUnit(UnitType type, boolean b){
        try {
            needed[typeToInt(type)] = b;
        }catch(Exception e) {
<<<<<<< HEAD
            e.printStackTrace();
=======
            System.out.println(e);
>>>>>>> 5a2a7ab... master
        }
    }

    void requestUnit(UnitType type){
        try {
            requestUnit(type, true);
        }catch(Exception e) {
<<<<<<< HEAD
            e.printStackTrace();
=======
            System.out.println(e);
>>>>>>> 5a2a7ab... master
        }
    }



}
