package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private Solution solution;
	private Solution solutionT;
	public ArrayList<Task> myTasks;
	public ArrayList<Task> myTasksT;
	private int numMyTask;
	public double planTimeout;
	public double setupTimeout;
	public double bidTimeout;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		//create first solution
		this.solution = new Solution(this.agent.vehicles().size());
		this.solutionT = new Solution(this.agent.vehicles().size());
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		this.myTasks = new ArrayList<Task>();
		this.myTasksT = new ArrayList<Task>();
		this.numMyTask = 0;
		
		
		LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        this.planTimeout = ls.get(LogistSettings.TimeoutKey.PLAN);
        this.setupTimeout = ls.get(LogistSettings.TimeoutKey.SETUP);
        this.bidTimeout = ls.get(LogistSettings.TimeoutKey.BID);
        
        
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
	
		if (winner == agent.id()) {
			this.numMyTask++;
			currentCity = previous.deliveryCity;
			this.myTasks.add(previous);
			this.solution.array = Utils.cloneSolution(this.solutionT.array);
			System.out.println("WONNN !!!!" + previous.id);
			
			
			
			
		}
		else {
			if (this.myTasksT.size()>0)
				this.myTasksT.remove(this.myTasksT.size() - 1);
		}
		
	
	}
	
	@Override
	public Long askPrice(Task task) {
		
	
		//check whether at least one vehicle can take this task
		boolean feasible = false;
		for (Vehicle v : this.agent.vehicles()) {
			if (v.capacity() >= task.weight) feasible = true;
		}
		if (feasible == false)
			return null;
		
		this.myTasksT.add(task);
		SLS slsT =  new SLS (topology, agent.vehicles(),this.myTasksT, this.planTimeout );
		this.solutionT = slsT.getSolution();
		long marginal = (long) (this.solutionT.computeCost() - this.solution.computeCost());
		
		//Probability of having zero marginal with new new plan
		double p = zeroMarginalProb(this.solutionT);
		
		if (marginal >0 )
			return (long) (marginal*1.1) ;
		else if ( marginal == 0) {
			return (long) 100;
		}
		else
			return (long) 0.8 * marginal;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		long time_start = System.currentTimeMillis();
	
		//Our solution : Recompute again solution using more time !!!!
	      
        SLS sls = new SLS(this.topology, this.agent.vehicles(), this.myTasks, this.planTimeout );
        List<Plan> plans = new ArrayList<Plan>();
        plans = sls.computePlans();
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        
        System.out.println("The plan was generated in "+duration+" milliseconds.");
		return plans;
	}
	
	private double zeroMarginalProb (Solution s) {
		//Update graphPlan
		ArrayList<City> graphPlan = new ArrayList<>();
		//just use the longest plan (we can change then)
		int longestVehicle =0;
		for (int i=1; i<s.array.length; i++) {
			if (s.array[i].size() > longestVehicle) longestVehicle = i;
		}
		//create list of city to be visited (in order)
		for (int i=0; i<s.array[longestVehicle].size(); i++) {
			if (s.array[longestVehicle].get(i).action == 1 )
				graphPlan.add(s.array[longestVehicle].get(i).task.pickupCity);
			else
				graphPlan.add(s.array[longestVehicle].get(i).task.deliveryCity);
		}
		double prob = 0;
		//probability of finding such tasks
		for (int i =0; i<graphPlan.size() -1 ; i++) {
			for (int j=i+1; j<graphPlan.size(); j++) {
				prob += this.distribution.probability(graphPlan.get(i), graphPlan.get(j));
			}
		}
		//normalization factor (uniform distrobution among cities)
		return prob/this.topology.cities().size();
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
}
