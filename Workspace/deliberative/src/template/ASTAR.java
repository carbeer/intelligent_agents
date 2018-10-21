package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.*;

public class ASTAR extends BFS {
    enum heuristic { FIRST, HIGHEST };
    heuristic heuristicMode = heuristic.FIRST;

    // Use default heuristic
    public ASTAR(Vehicle vehicle, Topology.City[] citiesIndex, Task[] taskList) {
        super(vehicle, citiesIndex, taskList);
    }

    // Set specific heuristic
    public ASTAR(Vehicle vehicle, Topology.City[] citiesIndex, Task[] taskList, heuristic heuristicMode) {
        super(vehicle, citiesIndex, taskList);
        this.heuristicMode = heuristicMode;
    }

    Comparator<Node> NodeComparator = new Comparator<Node>() {
        @Override
        public int compare(Node n1, Node n2) {
            return (int)(getHeuristicCosts(n1) - getHeuristicCosts(n2));
        }
    };

    PriorityQueue<Node> getSuccessor(Node current, PriorityQueue<Node> fs) {
        return (PriorityQueue) getSuccessor(current, (Collection<Node>) fs);
    }

    private Double getHeuristicCosts (Node n) {
        Double costs = 0d;
        switch (heuristicMode) {
            case FIRST:
                costs = getFirstDistanceAsCost(n);
                break;
            case HIGHEST:
                costs = getHighestDistanceAsCost(n);
        }
        return costs;
    }

//        double costs = 0;
//        for (Task task : taskList) {
//            if (a.stateList[task.id] == 2) {
//                costs -= task.reward;
//            }
//            else if (a.stateList[task.id] == 1) {
//                costs -= task.reward + citiesIndex[a.currentCityId].distanceTo(taskList[task.id].deliveryCity);
//
//            }
//        }
//        return costs;

    Double getFirstDistanceAsCost(Node n) {
        double h = 0;
        for (int y = 0; y < this.taskList.length; y++) {
            if (n.state.stateList[y] == 0) {
                h = this.taskList[y].pickupCity.distanceTo(this.citiesIndex[n.state.currentCityId]);
            }
        }
        return n.cost + h;
    }

    Double getHighestDistanceAsCost(Node n) {
        double h = 0;
        for (int y = 0; y < this.taskList.length; y++) {
            if (n.state.stateList[y] == 0 && this.taskList[y].pickupCity.distanceTo(this.citiesIndex[n.state.stateList[this.taskList.length]]) > h) {
                h = this.taskList[y].pickupCity.distanceTo(this.citiesIndex[n.state.currentCityId]);
            }
        }
        return n.cost + h;
    }

    @Override
    void executeAlgorithm() {
        createASTAR();
    }

    public void createASTAR() {
        PriorityQueue<Node> Q = new PriorityQueue<>(NodeComparator);
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
                PriorityQueue<Node> successorNodes = getSuccessor(n, new PriorityQueue<>(NodeComparator));
                Q.addAll(successorNodes);
            }
        }
    }
}
