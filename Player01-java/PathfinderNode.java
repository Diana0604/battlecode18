import bc*;

class PathfinderNode{

    Direction dir;
    int dist;

    PathFinderNode(Direction dir, int dist){
        this.dir = dir;
        this.dist = dist;
    }
}