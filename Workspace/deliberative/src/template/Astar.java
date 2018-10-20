package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import java.util.*;


public class Astar {
	
	public PriorityQueue<Node> Q; 
	public int numCities;
	public int numTasks;
	private City[] citiesIndex;
	private Task[] taskList;
	public HashMap<Node, Double> bfs;
	public Node root;
	public Vehicle vehicle;
	public ArrayList<Node> goalNodes;
	public ArrayList<Node> path;
	public HashMap<NodeCompare, Double> compare;
	
	public Astar (Vehicle vehicle, City[] citiesIndex, Task[] taskList, int numCities) {
		this.citiesIndex = citiesIndex;
		this.taskList = taskList;
		this.numCities = numCities;
		this.numTasks = taskList.length;
		this.vehicle = vehicle;
		this.goalNodes = new ArrayList<Node>();
		this.Q = new PriorityQueue<Node>(NodeComparator);
		this.compare = new HashMap<NodeCompare, Double>();
		
		//root
		this.root = new Node(new State(new int[this.numTasks + 1], this.vehicle.capacity()), 0, null);
		this.root.state.stateList[this.numTasks] = this.vehicle.getCurrentCity().id;	
		
		createBfs();
	}
	
	private PriorityQueue<Node> getSuccessor (Node current){
		//future nodes
		PriorityQueue<Node> fs = new PriorityQueue<Node>(NodeComparator);
		int n = this.numTasks;
		double actionCost =0;

		//simulate actions and create new nodes
		for (int i = 0; i < n; i++) {
			
			if (current.state.stateList[i] == 0 & this.taskList[i].weight  < current.state.capacityLeft) {
				//pick up task i
				int[] a = current.state.stateList.clone();
				double deliveredWeight = 0;
				double pickWeight = 0;
				
				a[i] = 1;
				actionCost = this.vehicle.costPerKm() * citiesIndex[current.state.stateList[n]].distanceTo(this.taskList[i].pickupCity);
				a[n] = taskList[i].pickupCity.id;
				Node newNode = new Node(new State(a, current.state.capacityLeft - this.taskList[i].weight + deliveredWeight - pickWeight ), current.cost + actionCost, current);		
				NodeCompare newNode1 = new NodeCompare(new State(a, current.state.capacityLeft - this.taskList[i].weight + deliveredWeight - pickWeight ), current.cost + actionCost, current);
				if (this.compare.get(newNode1) == null) {
					fs.add(newNode);
					this.compare.put(newNode1, newNode.cost);

				}
				else if ((Double) this.compare.get(newNode1) > newNode.cost){
					this.compare.put(newNode1, newNode.cost);
					fs.add(newNode);
				}		
				
			
					
				//}
			}
			else if (current.state.stateList[i] == 1) {
				
				int[]b = current.state.stateList.clone();
				int deliveredWeight = 0;
				int pickWeight = 0;
				
				b[i] = 2;
				actionCost = this.vehicle.costPerKm() * citiesIndex[current.state.stateList[n]].distanceTo(this.taskList[i].deliveryCity);
				b[n] = taskList[i].deliveryCity.id;
				Node newNode = new Node(new State(b, current.state.capacityLeft + this.taskList[i].weight + deliveredWeight- pickWeight), current.cost + actionCost, current);					
				NodeCompare newNode1 = new NodeCompare(new State(b, current.state.capacityLeft - this.taskList[i].weight + deliveredWeight - pickWeight ), current.cost + actionCost, current);
				if (this.compare.get(newNode1) == null) {
					fs.add(newNode);
					this.compare.put(newNode1, newNode.cost);

				}
				else if ((Double) this.compare.get(newNode1) > newNode.cost){
					this.compare.put(newNode1, newNode.cost);
					fs.add(newNode);
				}			
				}
				
			}
			
		return fs;
		
	}
	
	private void createBfs(){
		
		
		this.bfs = new HashMap<Node, Double>();
		//bfs.put(this.root, 0.);
		Q.add(this.root);
		
		int goal;
		int y = 0;
		while(!(Q.isEmpty())) {
			
			goal =0;
			
			Node current = this.Q.remove();
			for (int j=0; j < this.numTasks; j++ )
				goal += current.state.stateList[j]; 
			if(goal == this.numTasks * 2) {
				this.goalNodes.add(current);
				break;
			}
			//Add new level
			if (!this.bfs.containsKey(current) ) {
				PriorityQueue<Node> newLevel = getSuccessor(current);
				this.bfs.put(current, current.cost);
				this.Q.addAll(newLevel);
			}
		}
	}
	
	public Plan computePlan () {
		Plan plan = new Plan(this.vehicle.getCurrentCity());
		Node bestNode = findBest();
		this.path = new ArrayList<Node>();
		
		//Get rid of the root node
		while (bestNode != null){
			path.add(bestNode);
			bestNode = bestNode.parent;
		} 
		//DEBUG 
		System.out.println(path.size());
		for (Node n : this.path) {
			System.out.println(n.cost);
			System.out.println(n.state.capacityLeft);
			for (int u=0; u<=this.numTasks; u++) {
				System.out.print(" " + n.state.stateList[u]);
			}
			System.out.println();
		}
		
		Collections.reverse(path);
		
		for (int i=1; i<path.size(); i++) {
			City oldCity= this.citiesIndex[path.get(i-1).state.stateList[this.numTasks]];
			City newCity = this.citiesIndex[path.get(i).state.stateList[this.numTasks]];
			
			if (!oldCity.equals(newCity)) {
				for (City city : oldCity.pathTo(newCity)) {
					plan.appendMove(city);
				}
			}
			//pick and deliver
			for (int j=0; j<this.numTasks; j++) {				
				if (path.get(i).state.stateList[j] == 1 & path.get(i-1).state.stateList[j] == 0) plan.appendPickup(this.taskList[j]);
				if (path.get(i).state.stateList[j] == 2 & path.get(i-1).state.stateList[j] == 1) plan.appendDelivery(this.taskList[j]);
				
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

	
	private class State{
		//last value is the current city
		//first values are associated to the tasks
		//0 = not picked up not delivered
		//1 = picked up not delivered
		//2 = delivered
		public int[] stateList;
		public double capacityLeft;
		
		public State(int[] s, double capacityLeft) {
			stateList = s.clone();
			this.capacityLeft = capacityLeft;
		}
	}
	private class Node{
		public Node parent;
		public State state;
		public double cost;
		
		public Node (State state, double cost, Node parent) {
			this.cost = cost;
			this.state = state;
			this.parent = parent;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(this.state.stateList);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			Node o = (Node) obj;
			if (!Arrays.equals(this.state.stateList, o.state.stateList))
				return false;
			if (this.cost != o.cost)
				return false;
			return true;
		 }
	}
	
	private class NodeCompare{
		public Node parent;
		public State state;
		public double cost;
		
		public NodeCompare (State state, double cost, Node parent) {
			this.cost = cost;
			this.state = state;
			this.parent = parent;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(this.state.stateList);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			NodeCompare o = (NodeCompare) obj;
			if (!Arrays.equals(this.state.stateList, o.state.stateList))
				return false;
			return true;
		 }
	}
	Comparator<Node> NodeComparator = new Comparator<Node>() {
        @Override
        public int compare(Node n1, Node n2) {
            return heuristic(n1) - heuristic(n2);
        }
    };
    private int heuristic (Node n) {
    	double h = 0;
    	for (int y = 0; y<this.numTasks; y++) {
    		if (n.state.stateList[y] == 0 ) h = this.taskList[y].pickupCity.distanceTo(this.citiesIndex[n.state.stateList[this.numTasks]]);   			
    	}
    	
    	return (int) (n.cost + h);
    }
}
