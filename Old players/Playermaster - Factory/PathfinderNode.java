class PathfinderNode{

    byte dir;
    short dist;

    PathfinderNode(int dir, int dist){
        this.dir = (byte) dir;
        this.dist = (short) dist;
        //System.out.println("Dir: " + this.dir + " " + dir + ", Dist: " + this.dist + " " + dist);
    }
}