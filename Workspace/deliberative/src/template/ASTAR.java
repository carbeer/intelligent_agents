package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.*;

public class ASTAR {

    Vehicle vehicle;
    Topology.City[] citiesIndex;
    Task[] taskList;
    Map<String, Double> knownStates;
    public ASTAR(Vehicle vehicle, Topology.City[] citiesIndex, Task[] taskList) {
        this.vehicle = vehicle;
        this.citiesIndex = citiesIndex;
        this.taskList = taskList;
        this.knownStates = new HashMap<String, Double>();
    }


    public ArrayList<Node> getSuccessorNodes(Node node) {
        Node nextNode;

        System.out.printf("Got the following initial node: %d, %s, %d, %f\n", node.state.currentCityId, Arrays.toString(node.state.stateList), node.state.currentCityId, node.state.capacityLeft);
        ArrayList<Node> successors = new ArrayList<>();
        for (Topology.City c : citiesIndex[node.state.currentCityId].neighbors()) {
            System.out.printf("In neighbor city %d now\n", c.id);

            for (int i = 0; i < taskList.length; i++) {
                // Try to deliver tasks
                if (node.state.stateList[i] == 1 && taskList[i].deliveryCity == c) {
                    System.out.printf("Can deliver task %d in City %d\n", i, c.id);
                    nextNode = deliver(node, i);
                    if (!knownStates.containsKey(nextNode.getId()) || knownStates.get(nextNode.getId()) > nextNode.cost) {
                        successors.add(nextNode);
                        knownStates.put(nextNode.getId(), nextNode.cost);
                    }
                }
                // Pickup a task
                if (node.state.stateList[i] == 0 && taskList[i].pickupCity == c) {
                    if (node.state.capacityLeft >= taskList[i].weight) {
                        nextNode = pickUp(node, i);
                        if (!knownStates.containsKey(nextNode.getId()) || knownStates.get(nextNode.getId()) > nextNode.cost) {
                            successors.add(nextNode);
                            knownStates.put(nextNode.getId(), nextNode.cost);
                        }
                    } else {
                        System.out.println("Couldn't pick up the task because there is not enough capacity left.");
                    }
                }
            }

            System.out.printf("Can't deliver any task in City %d\n", c.id);
            nextNode = moveTo(node, c);
            if (knownStates.containsKey(nextNode.getId())) {
                if (knownStates.get(nextNode.getId()) > nextNode.cost) {
                    successors.add(nextNode);
                    System.out.println("Found cost improvement");
                    knownStates.put(nextNode.getId(), nextNode.cost);
                } else {
                    System.out.println("Known state, no improvement");
                }
            } else {
                System.out.println("New state and city pair");
                successors.add(nextNode);
                knownStates.put(nextNode.getId(), nextNode.cost);

            }
        }
        for (Node n : successors) {
            System.out.printf(" " + n.state.currentCityId);
        }
        System.out.println("known states are:");
        for (String key: knownStates.keySet()){
            String value = knownStates.get(key).toString();
            System.out.println(key + " " + value);


        }
        return successors;
    }


    public Double getHeuristicCosts(State a) {
        double costs = 0;
        for (Task task : taskList) {
            if (a.stateList[task.id] == 2) {
                costs -= task.reward;
            }
            else if (a.stateList[task.id] == 1) {
                costs -= task.reward + citiesIndex[a.currentCityId].distanceTo(taskList[task.id].deliveryCity);
            }
        }
        return costs;
    }

    public Node moveTo(Node node, Topology.City c) {
        State successorState = new State(node.state.stateList.clone(), c.id, node.state.capacityLeft);
        return new Node(successorState,getHeuristicCosts(node.state, successorState), node);
    }

    public Node pickUp(Node node, int taskId) {
        int[] newState = node.state.stateList.clone();
        newState[taskId] = 1;
        State successorState = new State(newState, node.state.currentCityId, node.state.capacityLeft - taskList[taskId].weight);
        return new Node(successorState, getHeuristicCosts(node.state, successorState), node);
    }

    public Node deliver(Node node, int taskId) {
        int[] newState = node.state.stateList.clone();
        newState[taskId] = 2;
        State successorState = new State(newState, node.state.currentCityId, node.state.capacityLeft - taskList[taskId].weight);
        return new Node(successorState, getHeuristicCosts(node.state, successorState), node);
    }

    public Double getHeuristicCosts(State a, State b) {
        double costs = 0;
        for (Task task : taskList) {
            if (b.stateList[task.id] == 2) {
                costs -= task.reward;
            }
            else if (b.stateList[task.id] == 1) {
                costs -= 0.5 * task.reward;
            }
        }
        double travelCosts = citiesIndex[a.currentCityId].distanceTo(citiesIndex[b.currentCityId]) * vehicle.costPerKm();
        return costs + travelCosts;
    }

    public Plan getPlan() throws Exception {
        Node optimalDestination = null;
        Node[] finalNodes = doASTAR();
        LinkedList<Node> optimalPath = new LinkedList<>();
        Plan plan;

        for (Node n : finalNodes) {
            if (optimalDestination == null || optimalDestination.cost > n.cost) {
                optimalDestination = n;
            }
        }

        for (Node n = optimalDestination; n.parent != null; n = n.parent) {
            System.out.printf("Added city %s with state %s", citiesIndex[n.state.currentCityId].name, Arrays.toString(n.state.stateList));
            optimalPath.addFirst(n);
        }

        plan = new Plan(citiesIndex[optimalPath.getFirst().state.currentCityId]);
        for (Node n : optimalPath) {
            if (n.parent != null) {
                plan.appendMove(citiesIndex[n.state.currentCityId]);
                if (n.parent.state.stateList != n.state.stateList) {
                    for (int i = 0; i < n.state.stateList.length; i++) {
                        if (n.parent.state.stateList[i] != n.state.stateList[i]) {
                            if (n.parent.state.stateList[i] == 0 && taskList[i].pickupCity == citiesIndex[n.state.currentCityId]) {
                                plan.appendPickup(taskList[i]);
                            }
                            else if (n.parent.state.stateList[i] == 1 && taskList[i].deliveryCity == citiesIndex[n.state.currentCityId]) {
                                plan.appendDelivery(taskList[i]);
                            }
                            else {
                                throw new Exception("This should not happen!");
                            }
                        }
                    }
                }
            }
        }

        return plan;
    }

    public Node[] doASTAR() throws Exception {
        Node[] finalNodes = new Node[citiesIndex.length];
        int found = 0;
        LinkedList<Node> Q = new LinkedList<>();
        Map<State, Double> C = new HashMap<>();
        Q.add(new Node(new State(new int[taskList.length], vehicle.getCurrentCity().id, vehicle.capacity()), 0, null));

        while (true) {
            if (Q.isEmpty()) {
                throw new Exception("Need an initial state.");
            }
            Node n = Q.pop();

            if (n.state.isFinalState()) {
                finalNodes[found] = n;
                found++;
                if (found == citiesIndex.length) {
                    return finalNodes;
                }
                continue;
            }
            if (C.isEmpty() || !C.containsKey(n) || C.get(n) > n.cost ) {
                C.put(n.state, n.cost);
                ArrayList<Node> successorNodes = getSuccessorNodes(n);
                for (Node node : successorNodes) {
                    System.out.printf("Current city %d, next city %s, current state %s, next state %s", n.state.currentCityId, node.state.currentCityId, Arrays.toString(n.state.stateList), Arrays.toString(node.state.stateList));
                }
                Q.addAll(successorNodes);
                Collections.sort(Q, new Comparator<Node>() {
                    @Override
                    public int compare(Node s1, Node s2) {
                        return -new Double(s1.cost).compareTo(s2.cost);
                    }
                });
                System.out.printf("\n%d element in Q as of now\n", Q.size());

            }
        }
    }
}

