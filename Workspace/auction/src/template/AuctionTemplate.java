package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
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
	public ArrayList<Task> myTasks;
	public ArrayList<Task> myTasksT;
	public double planTimeout;
	public double setupTimeout;
	public long bidTimeout;
	public SLS currentPlan;
	public SLS potentialPlan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;

		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		this.myTasks = new ArrayList<Task>();
		this.myTasksT = new ArrayList<Task>();
		this.currentPlan = new SLS(agent.vehicles(), new ArrayList<>(), 0);

		for (City c : topology.cities()) {
		    for (City d: topology.cities()) {
                System.out.printf("Probability that there is a task between %s and %s: %f\n ", c.name, d.name, distribution.probability(c, d));
		    }
        }
        for (City c : topology.cities()) {
            System.out.printf("Probability that there is no task in city %s: %f\n", c.name, distribution.probability(c, null));
        }
		
		LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        this.setupTimeout = ls.get(LogistSettings.TimeoutKey.SETUP);
        this.bidTimeout = ls.get(LogistSettings.TimeoutKey.BID);
		this.planTimeout = ls.get(LogistSettings.TimeoutKey.PLAN);

        System.out.printf("Got the following settings during setup:\n setupTimeout: %f sec\n planTimeout: " +
				"%f sec\n bidTimeout: %d sec\n", this.setupTimeout, this.planTimeout, bidTimeout);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
			this.myTasks.add(previous);
			System.out.printf("WONNN Task %s !!!!\n", previous.id);
			this.currentPlan = this.potentialPlan;
		}
		else {
			if (this.myTasksT.size() > 0)
				this.myTasksT.remove(this.myTasksT.size() - 1);
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		// Check whether at least one vehicle can take this task
		boolean feasible = false;
		for (Vehicle v : this.agent.vehicles()) {
			if (v.capacity() >= task.weight) {
				feasible = true;
				break;
			}
		}
		if (feasible == false) return null;

		this.myTasksT.add(task);
		this.potentialPlan =  new SLS(agent.vehicles(),this.myTasksT, this.bidTimeout/2);

		double marginal = this.potentialPlan.bestSolution.computeCost() - this.currentPlan.bestSolution.computeCost();
		System.out.printf("Marginal costs for the task: %f\n", marginal);

		return (long) marginal;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		long time_start = System.currentTimeMillis();
	
		//Our solution : Recompute again solution using more time !!!!
        SLS sls = new SLS(this.agent.vehicles(), this.myTasks, this.planTimeout );
        List<Plan> plans = new ArrayList<Plan>();
        plans = sls.computePlans();
        long duration =  System.currentTimeMillis() - time_start;
        
        System.out.printf("The plan was generated in %d milliseconds.\n", duration);
		return plans;
	}
}
