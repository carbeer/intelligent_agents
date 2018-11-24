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
import template.SLS.SLS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class DummyAgent implements AuctionBehavior {
    Topology topology;
    TaskDistribution distribution;
    Agent agent;
    Random random;
    Vehicle vehicle;
    Topology.City currentCity;

    public long planTimeout;
    public long setupTimeout;
    public long bidTimeout;

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

        this.setupTimeout = ls.get(LogistSettings.TimeoutKey.SETUP);
        this.bidTimeout = ls.get(LogistSettings.TimeoutKey.BID);
        this.planTimeout = ls.get(LogistSettings.TimeoutKey.PLAN);
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        int reward = 0;
        ArrayList<Task> taskList = new ArrayList<>();
        for (Task t : tasks) {
            taskList.add(t);
            reward += t.reward;
        }
        SLS sls = new SLS(vehicles, taskList, this.planTimeout);
        List<Plan> plans = sls.computePlans();

        double profit = reward - sls.bestSolution.computeCost();
        System.out.println(this.getClass() + ": Made a profit of " + profit);

        return plans;
    }

}
