package Playermaster;

import bc.*;

class WorkerData {
    int id;
    MapLocation loc;
    int target_id;
    MapLocation target_loc;
    int target_type;
    Direction safest_direction;
    int karbonite_index; //index de l'array de la mina


    WorkerData(int id){
        this.id = id;
        target_id = -1;
        target_type = 0;
        target_loc = null;
        safest_direction = null;
        karbonite_index = -1;
    }
}
