public class WorkerData {
    int id;
    AuxMapLocation loc;
    int target_id;
    AuxMapLocation target_loc;
    int target_type;


    WorkerData(int id){
        this.id = id;
        target_id = -1;
        target_type = 0;
        target_loc = null;
    }
}
