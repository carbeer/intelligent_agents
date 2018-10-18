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
    Map<int[], ArrayList<Integer>> knownStates;
    public ASTAR(Vehicle vehicle, Topology.City[] citiesIndex, Task[] taskList) {
        this.vehicle = vehicle;
        this.citiesIndex = citiesIndex;
        this.taskList = taskList;
        this.knownStates = new HashMap<int[], ArrayList<Integer>>();
    }

    public ArrayList<State> getSuccessorStates(State state) {
        System.out.printf("Got the following initial state: %s, %d, %f\n", Arrays.toString(state.stateList), state.currentCityId, state.capacityLeft);
        ArrayList<State> successors = new ArrayList<>();
        for (Topology.City c : citiesIndex[state.currentCityId].neighbors()) {
            boolean delivered = false;
            System.out.printf("In neighbor city %d now\n", c.id);

            for (int i = 0; i < taskList.length; i++) {
                // TODO: Pickup and delivery in one node!
                // Try to deliver tasks
                if (state.stateList[i] == 1 && taskList[i].deliveryCity == c) {
                    System.out.printf("Can deliver task %d in City %d\n", i, c.id);
                    int newStateList[] = state.stateList.clone();
                    newStateList[i] = 2;
                    State successor = new State(newStateList, c.id, state.capacityLeft+taskList[i].weight);
                    successors.add(successor);
                    delivered = true;
                }
                // Pickup a task
                if (state.stateList[i] == 0 && taskList[i].pickupCity == c) {
                    if (state.capacityLeft >= taskList[i].weight) {
                        int newStateList[] = state.stateList.clone();
                        newStateList[i] = 1;
                        successors.add(new State(newStateList, c.id, state.capacityLeft-taskList[i].weight));
                    } else {
                        System.out.println("Couldn't pick up the task because there is not enough capacity left.");
                    }
                }
            }
            if (!delivered)  {
                System.out.printf("Can't deliver any task in City %d\n", c.id);
                if (knownStates.containsKey(state.stateList)) {
                    if (!knownStates.get(state.stateList).contains(state.currentCityId)) {
                        System.out.println("Know the state but not the city.");
                        int newStateList[] = state.stateList.clone();
                        successors.add(new State(newStateList, c.id, state.capacityLeft));
                        knownStates.get(state.stateList).add(state.currentCityId);
                    } else {
                        System.out.println("Know the state and the city.");
                    }
                } else {
                    System.out.println("Know neither the state nor the city.");
                    int newStateList[] = state.stateList.clone();
                    successors.add(new State(newStateList, c.id, state.capacityLeft));
                    knownStates.put(state.stateList, new ArrayList<Integer>());
                    knownStates.get(state.stateList).add(state.currentCityId);
                }
            }
        }
        for (State s : successors) {
            System.out.printf(" " + citiesIndex[s.currentCityId].name);
        }
        return successors;
    }


    public Double getHeuristicCosts(State a, State b) {
        return citiesIndex[a.currentCityId].distanceTo(citiesIndex[b.currentCityId]);
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
            }
            if (C.isEmpty() || !C.containsKey(n) || C.get(n) > n.cost ) {
                C.put(n.state, n.cost);
                ArrayList<State> successorStates = getSuccessorStates(n.state);
                for (State s : successorStates) {
                    Node successor;
                    if (n.parent != null) {
                        successor = new Node(s, getHeuristicCosts(n.state, s) + getHeuristicCosts(n.parent.state, n.state), n);
                    }
                    else {
                        successor = new Node(s, getHeuristicCosts(n.state, s) + 0, n);
                    }
                    Q.add(successor);

                }
                Collections.sort(Q, new Comparator<Node>() {
                    @Override
                    public int compare(Node s1, Node s2) {
                        return new Double(s1.cost).compareTo(s2.cost);
                    }
                });

            }
        }
    }
}

class StateCosts {
    State state;
    double costs;

    public StateCosts(State state, double costs) {
        this.state = state;
        this.costs = costs;
    }
}
