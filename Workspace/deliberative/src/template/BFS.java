package template;

/* import table */
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;
import java.util.*;


public class BFS {

	public LinkedHashMap<Node, Double> Q;
 	City[] citiesIndex;
 	Task[] taskList;
	public HashMap<Node, Double> bfs;
	public Node root;
	public Vehicle vehicle;
	public ArrayList<Node> goalNodes;
	public ArrayList<Node> path;
	public HashMap<NodeCompare, Double> compare;

	public BFS(Vehicle vehicle, City[] citiesIndex, Task[] taskList) {
		this.citiesIndex = citiesIndex;
		this.taskList = taskList;
		this.vehicle = vehicle;
		this.goalNodes = new ArrayList<Node>();
		this.Q = new LinkedHashMap<Node, Double>();
		this.compare = new HashMap<NodeCompare, Double>();
		//root
		this.root = new Node(new State(new int[taskList.length], this.vehicle.getCurrentCity().id, this.vehicle.capacity()), 0, null);
		this.root.state.currentCityId = this.vehicle.getCurrentCity().id;
	}

	ArrayList<Node> getSuccessor(Node current) {
		ArrayList<Node> fs = new ArrayList<>();
		return (ArrayList<Node>) getSuccessor(current, fs);
	}

	Collection<Node> getSuccessor(Node current, Collection<Node> fs) {
		//future nodes
		double actionCost = 0;

		//simulate actions and create new nodes
		for (int i = 0; i < taskList.length; i++) {

			if (current.state.stateList[i] == 0 & this.taskList[i].weight < current.state.capacityLeft) {
				//pick up task i
				int[] a = current.state.stateList.clone();
				int a_city;
				double deliveredWeight = 0;
				double pickWeight = 0;

				a[i] = 1;
				actionCost = this.vehicle.costPerKm() * citiesIndex[current.state.currentCityId].distanceTo(this.taskList[i].pickupCity);
				a_city = taskList[i].pickupCity.id;
				Node newNode = new Node(new State(a, a_city, current.state.capacityLeft - this.taskList[i].weight + deliveredWeight - pickWeight), current.cost + actionCost, current);
				NodeCompare newNode1 = new NodeCompare(new State(a, a_city, current.state.capacityLeft - this.taskList[i].weight + deliveredWeight - pickWeight), current.cost + actionCost, current);
				if (this.compare.get(newNode1) == null) {
					fs.add(newNode);
					this.compare.put(newNode1, newNode.cost);
				} else if ((Double) this.compare.get(newNode1) > newNode.cost) {
					this.compare.put(newNode1, newNode.cost);
					fs.add(newNode);
				}
			} else if (current.state.stateList[i] == 1) {
				int[] b = current.state.stateList.clone();
				int b_city;
				int deliveredWeight = 0;
				int pickWeight = 0;

				b[i] = 2;
				actionCost = this.vehicle.costPerKm() * citiesIndex[current.state.currentCityId].distanceTo(this.taskList[i].deliveryCity);
				b_city = taskList[i].deliveryCity.id;
				Node newNode = new Node(new State(b, b_city, current.state.capacityLeft + this.taskList[i].weight + deliveredWeight - pickWeight), current.cost + actionCost, current);
				NodeCompare newNode1 = new NodeCompare(new State(b, b_city, current.state.capacityLeft - this.taskList[i].weight + deliveredWeight - pickWeight), current.cost + actionCost, current);
				if (this.compare.get(newNode1) == null) {
					fs.add(newNode);
					this.compare.put(newNode1, newNode.cost);
				} else if ((Double) this.compare.get(newNode1) > newNode.cost) {
					this.compare.put(newNode1, newNode.cost);
					fs.add(newNode);
				}
			}
		}
		return fs;
	}

	private void createBfs() {

		this.bfs = new HashMap<Node, Double>();
		Q.put(this.root, 0.);
		while (!(Q.isEmpty())) {
			Node current = Q.entrySet().iterator().next().getKey();
			Q.remove(current);
			if (current.state.isFinalState()) {
				this.goalNodes.add(current);
				continue;
			}
			//Add new level
			if (!this.bfs.containsKey(current)) {
				ArrayList<Node> newLevel = getSuccessor(current);
				this.bfs.put(current, current.cost);
				for (Node n : newLevel) {
					Q.put(n, n.cost);
				}
			}
		}
	}

	void executeAlgorithm() {
		createBfs();
	}

	public Plan computePlan() {
		executeAlgorithm();
		Plan plan = new Plan(this.vehicle.getCurrentCity());
		Node bestNode = findBest();
		this.path = new ArrayList<Node>();

		//Get rid of the root node
		while (bestNode != null) {
			path.add(bestNode);
			bestNode = bestNode.parent;
		}
		//DEBUG
		System.out.println(path.size());
		for (Node n : this.path) {
			System.out.println(n.cost);
			System.out.println(n.state.capacityLeft);
			for (int u = 0; u < this.taskList.length; u++) {
				System.out.print(" " + n.state.stateList[u]);
			}
			System.out.println();
		}

		Collections.reverse(path);

		for (int i = 1; i < path.size(); i++) {
			City oldCity = this.citiesIndex[path.get(i - 1).state.currentCityId];
			City newCity = this.citiesIndex[path.get(i).state.currentCityId];

			if (!oldCity.equals(newCity)) {
				for (City city : oldCity.pathTo(newCity)) {
					plan.appendMove(city);
				}
			}
			//pick and deliver
			for (int j = 0; j < this.taskList.length; j++) {
				if (path.get(i).state.stateList[j] == 1 & path.get(i - 1).state.stateList[j] == 0)
					plan.appendPickup(this.taskList[j]);
				if (path.get(i).state.stateList[j] == 2 & path.get(i - 1).state.stateList[j] == 1)
					plan.appendDelivery(this.taskList[j]);
			}
		}
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
