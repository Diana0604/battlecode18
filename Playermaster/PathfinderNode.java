import bc.*;

class PathfinderNode{

    Direction dir;
    double dist;

    PathfinderNode(Direction dir, int dist){
        this.dir = dir;
        this.dist = dist;
    }
}