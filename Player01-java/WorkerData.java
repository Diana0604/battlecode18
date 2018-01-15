import bc.*;

/**
 * Created by Pau on 14/01/2018.
 */

public class WorkerData {
    int id;
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
