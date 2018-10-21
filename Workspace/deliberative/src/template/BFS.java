package template;

/* import table */
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class BFS {

	public Stack<Node> Q;
 	final City[] citiesIndex;
 	Task[] taskList;
	public HashMap<Node, Double> bfs;
	final public Node root;
	public Vehicle vehicle;
	public ArrayList<Node> goalNodes;
	public ArrayList<Node> path;
	public HashMap<State, Double> compare;
	final int numCities;
	final int numTasks;
	final int costPerKm;
	final long startTime;

	public BFS(Vehicle vehicle, City[] citiesIndex, Task[] taskList) {
		startTime = System.nanoTime();
		System.out.println("Timer started");
		this.citiesIndex = citiesIndex;
		this.taskList = taskList;
		this.vehicle = vehicle;
		this.goalNodes = new ArrayList<Node>();
		this.Q = new Stack<>();
		this.compare = new HashMap<State, Double>();
		this.numCities = citiesIndex.length;
		this.numTasks = taskList.length;
		this.root = new Node(new State(new int[numTasks+1], this.vehicle.capacity()), 0, null);
		root.state.stateList[numTasks] = this.vehicle.getCurrentCity().id;
		this.costPerKm = vehicle.costPerKm();
		executeAlgorithm();
	}

	Collection<Node> getSuccessor(Node current, Collection<Node> fs) {
		//future nodes
		double actionCost;

		//simulate actions and create new nodes
		for (int i = 0; i < numTasks; i++) {
			if (current.state.stateList[i] == 0 && this.taskList[i].weight < current.state.capacityLeft) {
				//pick up task i
				int[] a = current.state.stateList.clone();
				a[i] = 1;
				a[numTasks] = taskList[i].pickupCity.id;
				actionCost = this.costPerKm * citiesIndex[current.state.stateList[numTasks]].distanceTo(this.taskList[i].pickupCity);
				State newState = new State(a, current.state.capacityLeft - this.taskList[i].weight);
				Node newNode = new Node(newState, current.cost + actionCost, current);
				if (this.compare.get(newState) == null || this.compare.get(newState) > newNode.cost) {
					this.compare.put(newState, newNode.cost);
					fs.add(newNode);
				}
			} else if (current.state.stateList[i] == 1) {
				int[] b = current.state.stateList.clone();
				b[i] = 2;
				b[numTasks] = taskList[i].deliveryCity.id;
				actionCost = this.costPerKm * citiesIndex[current.state.stateList[numTasks]].distanceTo(this.taskList[i].deliveryCity);
				State newState = new State(b, current.state.capacityLeft + this.taskList[i].weight);
				Node newNode = new Node(newState, current.cost + actionCost, current);
				if (this.compare.get(newState) == null || this.compare.get(newState) > newNode.cost) {
					this.compare.put(newState, newNode.cost);
					fs.add(newNode);
				}
			}
		}
		return fs;
	}

	private void createBfs() {
		this.bfs = new HashMap<Node, Double>();
		Q.push(this.root);
		while (!(Q.isEmpty())) {
			Node current = Q.pop();
			if (current.state.isFinalState()) {
				this.goalNodes.add(current);
				continue;
			}
			//Add new level
			if (!this.bfs.containsKey(current)) {
				ArrayList<Node> newLevel = (ArrayList<Node>) getSuccessor(current, new ArrayList<>());
				this.bfs.put(current, current.cost);
				for (Node n : newLevel) {
					Q.push(n);
				}
			}
		}
	}

	void executeAlgorithm() {
		createBfs();
	}

	public Plan computePlan() {
		Plan plan = new Plan(this.vehicle.getCurrentCity());
		Node bestNode = findBest();
		this.path = new ArrayList<Node>();

		while (bestNode != null) {
			path.add(bestNode);
			bestNode = bestNode.parent;
		}
		//DEBUG
		System.out.println(path.size());
		for (Node n : this.path) {
			System.out.println(n.cost);
			System.out.println(n.state.capacityLeft);
			for (int u = 0; u < this.numTasks; u++) {
				System.out.print(" " + n.state.stateList[u]);
			}
			System.out.println();
		}
		Collections.reverse(path);
		for (int i = 1; i < path.size(); i++) {
			City oldCity = this.citiesIndex[path.get(i - 1).state.stateList[numTasks]];
			City newCity = this.citiesIndex[path.get(i).state.stateList[numTasks]];

			if (!oldCity.equals(newCity)) {
				for (City city : oldCity.pathTo(newCity)) {
					plan.appendMove(city);
				}
			}
			//pick and deliver
			for (int j = 0; j < this.numTasks; j++) {
				if (path.get(i).state.stateList[j] == 1 && path.get(i - 1).state.stateList[j] == 0)
					plan.appendPickup(this.taskList[j]);
				else if (path.get(i).state.stateList[j] == 2 && path.get(i - 1).state.stateList[j] == 1)
					plan.appendDelivery(this.taskList[j]);
			}
		}
		System.out.printf("Execution took %d seconds\n", TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
		return plan;
	}

	private Node findBest() {
		double minCost = Double.POSITIVE_INFINITY;
		Node best = this.goalNodes.get(0);
		for (Node n : this.goalNodes) {
			if (n.cost < minCost) {
				minCost = n.cost;
				best = n;
			}
		}
		return best;
	}
}
