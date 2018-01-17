import bc.*;

public class WorkerData {
    int id;
    MapLocation loc;
    int target_id;
    MapLocation target_loc;
    int target_type;
    Direction safest_direction;


    WorkerData(int id){
        this.id = id;
        target_id = -1;
        target_type = 0;
        target_loc = null;
        safest_direction = null;
    }
}
