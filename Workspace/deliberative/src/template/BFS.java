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


public class BFS {
	
	public int numCities;
	public int numTasks;
	private City[] citiesIndex;
	private Task[] taskList;
	public ArrayList<Node> bfs;
	public Node root;
	public Vehicle vehicle;
	public ArrayList<Node> goalNodes;
	
	public BFS (Vehicle vehicle, City[] citiesIndex, Task[] taskList, int numCities) {
		this.citiesIndex = citiesIndex;
		this.taskList = taskList;
		this.numCities = numCities;
		this.numTasks = taskList.length;
		this.vehicle = vehicle;
		this.goalNodes = new ArrayList<Node>();
		
		//root
		this.root = new Node(new State(new int[this.numTasks + 1], this.vehicle.capacity()), 0, null);
		this.root.state.stateList[this.numTasks] = this.vehicle.getCurrentCity().id;		
		createBfs();
	}
	
	private ArrayList<Node> getSucc (Node current){
		//future nodes
		ArrayList<Node> fs = new ArrayList<Node>();
		int n = this.numTasks;
		double actionCost =0;
		boolean isGoal = true;
		
		//simulate actions and create new nodes
		for (int i = 0; i < n; i++) {
			if (current.state.stateList[i] == 0) {
				//pick up task i
				int[] a = current.state.stateList.clone();
				
				//Can I take it or not 
				if ( this.taskList[i].weight < current.state.capacityLeft ) {
					a[i] = 1;
					actionCost = this.vehicle.costPerKm() * citiesIndex[current.state.stateList[n]].distanceTo(this.citiesIndex[taskList[i].pickupCity.id]);
					Node newNode = new Node(new State(a, current.state.capacityLeft - this.taskList[i].weight), current.cost + actionCost, current);
					//Update current city (last index)			
					a[this.numTasks] = taskList[i].pickupCity.id;
					fs.add(newNode);
				}				
			}
			else if (current.state.stateList[i] == 1) {
				//deliver task i
				int[]b = current.state.stateList.clone();
				b[i] = 2;
				actionCost = this.vehicle.costPerKm() * citiesIndex[current.state.stateList[n]].distanceTo(this.citiesIndex[taskList[i].deliveryCity.id]);
				Node newNode = new Node(new State(b, current.state.capacityLeft + this.taskList[i].weight), current.cost + actionCost, current);
				//Update current city (last index)
				b[this.numTasks] = taskList[i].deliveryCity.id;
				fs.add(newNode);
			}
		}
		return fs;
		
	}
	
	private void createBfs(){
		
		ArrayList<Node> Q = new ArrayList<Node>();
		this.bfs = new ArrayList<Node>();
		bfs.add(this.root);
		Q.add(this.root);
		
		int goal;
		
		while(!(Q.isEmpty())) {
			
			goal =0;
			
			Node current = Q.remove(0);
			for (int j=0; j < this.numTasks; j++ )
				goal += current.state.stateList[j]; 
			if(goal == this.numTasks * 2) {
				this.goalNodes.add(current);
				continue;
			}
			//Add new level 
			ArrayList<Node> newLevel = getSucc(current);
			this.bfs.addAll(newLevel);
			Q.addAll(newLevel);
			
		}
	}
	
	public Plan computePlan () {
		Plan plan = new Plan(this.vehicle.getCurrentCity());
		
		return plan;
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
	}
	

}