import java.util.*;

public class Maxflow {
    private static int[] parent;
    private static Queue<Integer> queue;
    private static int numberOfVertices;
    private static boolean[] visited;

    private static boolean bfs(int source, int goal, int graph[][]) {
        boolean pathFound = false;
        int destination, element;

        for(int vertex = 1; vertex <= numberOfVertices; vertex++) {
            parent[vertex] = -1;
            visited[vertex] = false;
        }

        queue.add(source);
        parent[source] = -1;
        visited[source] = true;

        while (!queue.isEmpty()) {
            element = queue.remove();
            destination = 1;

            while (destination <= numberOfVertices) {
                if (graph[element][destination] > 0 &&  !visited[destination]) {
                    parent[destination] = element;
                    queue.add(destination);
                    visited[destination] = true;
                }
                destination++;
            }
        }
        if(visited[goal]) pathFound = true;
        return pathFound;
    }

    private static int[][] fordFulkerson(int graph[][], int source, int destination) {
        int u, v;
        int pathFlow;

        int[][] residualGraph = new int[numberOfVertices + 1][numberOfVertices + 1];
        for (int sourceVertex = 1; sourceVertex <= numberOfVertices; sourceVertex++) {
            for (int destinationVertex = 1; destinationVertex <= numberOfVertices; destinationVertex++) {
                residualGraph[sourceVertex][destinationVertex] = graph[sourceVertex][destinationVertex];
            }
        }

        while (bfs(source ,destination, residualGraph)) {
            pathFlow = Integer.MAX_VALUE;
            for (v = destination; v != source; v = parent[v])
            {
                u = parent[v];
                pathFlow = Math.min(pathFlow, residualGraph[u][v]);
            }
            for (v = destination; v != source; v = parent[v])
            {
                u = parent[v];
                residualGraph[u][v] -= pathFlow;
                residualGraph[v][u] += pathFlow;
            }
        }


        int[][] flowGraph = new int[numberOfVertices + 1][numberOfVertices + 1];
        for (int i = 0; i < residualGraph.length; i++){
            for (int j = 0; j < residualGraph[0].length; j++){
                int flow = Math.max(0, graph[i][j] - residualGraph[i][j]);
                flowGraph[i][j] = flow;
            }
        }

        return flowGraph;
    }


    /*
    Cridar aquesta funcio. Units es una array amb tots els rangers (o lo que sigui)
    Les possibles destinacions per cada ranger han d'anar posades a unit.possibleTargets
    Es poden passar rangers que no es puguin moure tambe, simplement els ignora i funciona igualment (crec :D)
     */
    public static void maxflow(AuxUnit[] units){
        ArrayList<AuxMapLocation> possibleTargets = new ArrayList<>();
        for (AuxUnit unit: units){
            for (AuxMapLocation t: unit.possibleTargets){
                if (!possibleTargets.contains(t)) possibleTargets.add(t);
            }
        }

        numberOfVertices = 1 + 1 + units.length + possibleTargets.size();
        queue = new LinkedList<>();
        parent = new int[numberOfVertices + 1];
        visited = new boolean[numberOfVertices + 1];


        //graph[0][] = graph[][0] = merda (no es fa servir)
        //graph[1][2 + index(units)] = del source a les units. Val 1 si la unit es pot moure, 0 si no
        //graph[2 + index(units)][2 + units.length + index(possibleTargets)] = de les units a les destinations
        //graph[2 + units.length + index(possibleTargets)][numberOfNodes] = de les destinations al sink
        int[][] graph = new int[numberOfVertices + 1][numberOfVertices + 1];
        int sourceIndex = 1;
        int sinkIndex = numberOfVertices;
        
        for (int i = 0; i < units.length; i++)
            if (units[i].canMove()) graph[1][2 + i] = 1;

        for (int i = 0; i < units.length; i++){
            AuxUnit unit = units[i];
            for (AuxMapLocation t: unit.possibleTargets){
                int locIndex = possibleTargets.indexOf(t);
                graph[2 + i][2 + units.length + locIndex] = 1;
            }
        }
        for (int i = 0; i < possibleTargets.size(); i++)
            graph[2 + units.length + i][numberOfVertices] = 1;


        int[][] flowGraph;

        flowGraph = fordFulkerson(graph, sourceIndex, sinkIndex);

        for (int i = 0; i < units.length; i++){
            for (int j = 0; j < possibleTargets.size(); j++){
                if(flowGraph[2 + i][2 + units.length + j] == 1){
                    System.out.println("Ranger " + units[i].getID() + " amb posicio " + units[i].getMapLocation() + " es mou a la posicio " + possibleTargets.get(j));
                }
            }
        }
    }
}