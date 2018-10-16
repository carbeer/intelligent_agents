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
		numCities = topology.size();
		
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

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
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
		Plan plan;
		
		
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
	private ArrayList<State> succFunction (State s){
		//future states
		ArrayList<State> fs = new ArrayList<State>();
		int n = s.stateList.length;
		
		//simulate actions
		for (int i = 0; i < n-1; i++) {
			switch(s.stateList[i]) {
			case 0:
				//pick up task i
				int[] a = s.stateList.clone();
				a[i] = 1;
				//TODO update cost of this action in e[n-2]
				//...
				//Update current city (last index)
				a[-1] = taskList[i].pickupCity.id;
				fs.add(new State(a));
				break;
			case 1:
				//deliver task i
				int[] b = s.stateList.clone();
				b[i] = 2;
				//TODO update cost of this action in e[n-2]
				//...
				//Update current city (last index)
				b[-1] = taskList[i].deliveryCity.id;
				fs.add(new State(b));
				break;
			case 2:
				break;
			}
		}
		return fs;
		
	}
	
	private class State{
		//last values are the cumulative cost and the current city
		//first values are associated to the tasks
		//0 = not picked up not delivered
		//1 = picked up not delivered
		//2 = delivered
		//current city in {0, ... , numCity-1}
		int[] stateList;
		
		public State(int[] s) {
			stateList = s.clone();
		}
	}

}
