package template.DummyAgents;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import template.AuctionAgent;
import template.SLS.SLS;
import template.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dummy agent, bidding exactly his marginal costs computed through SLS.
 */
@SuppressWarnings("unused")
public class SLSDummyAgent extends DummyAgent {
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private Topology.City currentCity;


    public ArrayList<Task> myTasks = new ArrayList<>();
    public ArrayList<Task> myTasksT = new ArrayList<>();
    public SLS currentPlan;
    public SLS potentialPlan;


    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {
        this.agent = agent;
        this.vehicle = agent.vehicles().get(0);
        this.currentCity = vehicle.homeCity();
        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(seed);
        this.topology = topology;
        this.distribution = distribution;

        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        super.setup(topology, distribution, agent);
        this.currentPlan = new SLS(agent.vehicles(), myTasks, bidTimeout);
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        if (winner == agent.id()) {
            currentCity = previous.deliveryCity;
            this.myTasks.add(previous);
            this.currentPlan.bestSolution.vehiclePlan = Utils.cloneArray(this.potentialPlan.bestSolution.vehiclePlan);
        }
        else {
            if (this.myTasksT.size() > 0)
                this.myTasksT.remove(this.myTasksT.size() - 1);
        }
    }

    @Override
    public Long askPrice(Task task) {
        boolean feasible = false;
        for (Vehicle v : this.agent.vehicles()) {
            if (v.capacity() >= task.weight) {
                feasible = true;
                break;
            }
        }

        if (feasible == false) return null;

        this.myTasksT.add(task);
        this.potentialPlan = new SLS(agent.vehicles(), this.myTasksT, this.bidTimeout / 2);

        double marginal = this.potentialPlan.bestSolution.computeCost() - this.currentPlan.bestSolution.computeCost();
        System.out.printf(this.getClass() + ": Marginal costs of the Opponent for the task: %f\n", marginal);
        marginal = AuctionAgent.getRealMarginalCosts(marginal);
        return (long) marginal;
    }
}
