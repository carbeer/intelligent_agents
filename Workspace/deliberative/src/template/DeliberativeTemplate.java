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
/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	int numCities;
	int numTasks;
	private City[] citiesIndex;
	private Task[] taskList;
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		this.numCities = topology.size();
		this.citiesIndex = new City[this.numCities];
		int k = 0;
		for (City c : topology.cities()) {
			citiesIndex[k] = c;
			k++;
		}
		

		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
	
		numTasks = tasks.size();
		taskList = new Task[tasks.size()];
		int i=0;
		for (Task t : tasks) {
			taskList[i] = t;
			i++;
		}
		
		//DEbug
		Plan p = deliberativePlan(vehicle, tasks);

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			//create BFS passing vehicle, taskslist, citiesindex
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return p;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		for(Task task : tasks) {
			System.out.println(task.id + " " + task.pickupCity.name + " " + task.deliveryCity.name);
		}
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
	
	private Plan deliberativePlan (Vehicle vehicle, TaskSet tasks) {
		Plan plan = new Plan(vehicle.getCurrentCity());
		BFS bfs = new BFS(vehicle, this.citiesIndex, this.taskList, this.numCities );
		System.out.println(bfs.bfs.size());
		plan = bfs.computePlan();
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
	//compute all possible states reachable from current state 's'



}

class State{
	//last value is the current city
	//first values are associated to the tasks
	//0 = not picked up not delivered
	//1 = picked up not delivered
	//2 = delivered
	public int[] stateList;
	public int currentCityId;
	public double capacityLeft;

	public State(int[] s, int currentCityId, double capacityLeft) {
		stateList = s.clone();
		this.currentCityId = currentCityId;
		this.capacityLeft = capacityLeft;
	}
}
class Node{
	public Node parent;
	public State state;
	public double cost;

	public Node (State state, double cost, Node parent) {
		this.cost = cost;
		this.state = state;
		this.parent = parent;
	}
}