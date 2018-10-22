package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

public class ASTAR extends BFS {
    enum heuristic { FIRST, HIGHEST };
    heuristic heuristicMode;

    // Use default heuristic
    public ASTAR(Vehicle vehicle, Topology.City[] citiesIndex, TaskSet tasks) {
        super(vehicle, citiesIndex, tasks);
        this.heuristicMode = heuristic.FIRST;
    }

    // Set specific heuristic
    public ASTAR(Vehicle vehicle, Topology.City[] citiesIndex, heuristic heuristicMode, TaskSet tasks) {
        super(vehicle, citiesIndex, tasks);
        this.heuristicMode = heuristicMode;
    }

    @Override
    void executeAlgorithm() {
        createASTAR();
    }

    public void createASTAR() {
        PriorityQueue<Node> Q = new PriorityQueue<Node>();
        Map<Node, Double> C = new HashMap<>();
        Q.add(this.root);

        while (!Q.isEmpty()) {
            Node n = Q.remove();
            if (n.state.isFinalState()) {
                goalNodes.add(n);
                break;
            }
            if (!C.containsKey(n)) {
                C.put(n, n.cost);
                PriorityQueue<Node> successorNodes = (PriorityQueue<Node>) getSuccessor(n, new PriorityQueue<Node>());
                Q.addAll(successorNodes);
            }
        }
    }
}
