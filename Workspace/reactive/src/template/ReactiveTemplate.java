package template;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {
	private static final Logger LOGGER = Logger.getLogger( ReactiveTemplate.class.getName() );
	double cost;
	private Agent myAgent;
	private int numCities;
	private City[] citiesIndex;
	private HashMap<Key, Double> transitionsTable;
	private HashMap<KeyReward, Double> transitionsRewardTable;
	private HashMap<State, Double> V;
	private HashMap<KeyReward, Double> Q;
	private HashMap<State, Integer> best;
	// Epsilon for Reinforcement Learning
	private double epsilon;


	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		epsilon = 0.00001;

		// Create array citiesIndex containing all cities
		numCities = topology.size();
		citiesIndex = new City[numCities];
		int index = 0;
		for (City c : topology) {
			citiesIndex[index] = c;
			index++;
		}

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		this.myAgent = agent;

		// Get the costs per km (as we currently only use a single vehicle per agent, only the first value is relevant)
		cost = agent.vehicles().get(0).costPerKm();

		// Create the tables to work with
		createTransitionsRewardsTable(topology, td, cost);


		// Debug printing
		for (Key k : transitionsTable.keySet()) {
			LOGGER.log(Level.FINER, "Stato " + k.fromState.x.id + " " + k.fromState.y.id + " Azione " + k.action + " a Stato " + " Probab " + transitionsTable.get(k));
		}

		// Compute optimal policy
		computeOptimalPolicy(discount.doubleValue());

		// Debug printing
		LOGGER.log(Level.FINER, "V.size():  " + V.size());
		LOGGER.log(Level.FINER, "best.size(): " + best.size());
		LOGGER.log(Level.FINER, "Q.size()" + Q.size());
		LOGGER.log(Level.FINER, "transitionTable.size()" + transitionsTable.size());
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		State currentState;
		City currentCity = vehicle.getCurrentCity();

		//Figure out current state
		if (availableTask != null) {
			currentState = new State(currentCity, availableTask.deliveryCity);
		} else {
			currentState = new State(currentCity, currentCity);
		}

		if (best.get(currentState) == numCities) {
			//Pick and deliver
			action = new Pickup(availableTask);

		} else {
			//Just move to the best neighbor city
			action = new Move(citiesIndex[best.get(currentState)]);
		}
		return action;
	}

	// Method that computes transitions probabilities
	private void createTransitionsRewardsTable(Topology topology, TaskDistribution td, double cost) {

		transitionsTable = new HashMap<Key, Double>();
		transitionsRewardTable = new HashMap<KeyReward, Double>();

		for (City fx : topology) {
			for (City fy : topology) {
				// Create the current state (current cities, task destination city)
				State fState = new State(fx, fy);

				// No Task found, go anywhere (neighborhood)
				// Taking action from 0 to numCities-1 depending on the destination city
				if (fx.id == fy.id) {
					for (City tx : topology) {
						for (City ty : topology) {
							// The value is the prob. of finding in the destination city (tx) a task for the city ty
							// The action is the number of the destination city
							// Create the next state (next cities, dest. task next city) and add it to the transitionTable
							State tState = new State(tx,  ty);
							if( fState.x.hasNeighbor(tx)) transitionsTable.put(new Key(fState,  tx.id, tState), td.probability(tx, ty));
						}
						//Reward table R(s,a)
						if(fState.x.hasNeighbor(tx)) transitionsRewardTable.put(new  KeyReward(fState, tx.id), -(cost * fState.x.distanceTo(tx)));
					}
				}
				// Task found
				// If a tuple (s,a,s') is not in the hashmap, its transition probability is 0
				else {
					for (City tx : topology) {
						for (City ty : topology) {
							State tState = new State(tx, ty);
							// Action numCities corresponds to picking up and delivering the task
							if (tx.id == fState.y.id)
								transitionsTable.put(new Key(fState, numCities, tState), td.probability(tx, ty));
							// Action tx.id means the agent does not pick up the task and just moves, if the city is a neighbor city
							if (fState.x.hasNeighbor(tState.x))
								transitionsTable.put(new Key(fState, tx.id, tState), td.probability(tx, ty));
						}
						// Reward if the task was delivered
						if (tx.id == fState.y.id)
							transitionsRewardTable.put(new KeyReward(fState, numCities), (td.reward(fState.x, fState.y) - (fState.x.distanceTo(fState.y) * cost)));
						// Reward if the agent is just moving without picking the task
						if (fState.x.hasNeighbor(tx))
							transitionsRewardTable.put(new KeyReward(fState, tx.id), -(fState.x.distanceTo(tx) * cost));
					}
				}
			}
		}
	}

	private void computeOptimalPolicy(double discount) {
		// Accumulated value for each state
		V = new HashMap<State, Double>();
		HashMap<State, Double> Vp = new HashMap<State, Double>();
		// Expected reward given state s and action a
		Q = new HashMap<KeyReward, Double>();
		// Best action for each state
		best = new HashMap<State, Integer>();

		// Initialize V and Q tables
		for (KeyReward k : transitionsRewardTable.keySet()) {
			V.put(k.fromState, 1d);
			Vp.put(k.fromState, 1d);
			Q.put(k, (double) 0);
		}

		// Compute optimal value and policy
		double diff = 100;
		int h = 0;
		double tmp = 0;

		// Reinforcement Learning Algorithm
		while (diff > epsilon) {

			// Keeps track of the best strategy of the previous iteration
			for (State si : V.keySet()) {
				Vp.replace(si, V.get(si));
			}

			// For every state
			for (State s : V.keySet()) {
				// For every city
				for (int i = 0; i <= numCities; i++) {
					KeyReward kw = new KeyReward(s, i);
					// Check whether the transitionRewardTable has an entry for kw (if null, this means that the transition is not possible)
					if (transitionsRewardTable.get(kw) != null) {
						tmp = transitionsRewardTable.get(kw).doubleValue();
					}
					for (Key k : transitionsTable.keySet()) {
						// Check if a transitionTable entry exists that for state s where the action a is to pick up and deliver a task to city i
						if (k.fromState.equals(s) && k.action == i) {
							// If the kw is in Q, it means that it is in the keyRewardTable.
							if (Q.get(kw) != null) {
								tmp = tmp + (discount * transitionsTable.get(k) * V.get(k.toState));
							}
						}
					}
					// Update the expected reward given state s and action a
					Q.replace(kw, tmp);
				}

				// Set initial reward to a high negative number to ensure that every possible movement has a higher reward.
				double max = -100000;

				for (KeyReward kr : Q.keySet()) {
					// Loop through all KeyRewards in Q where the initial state is s. Update the expected reward and the best strategy for every action.
					if (kr.fromState.equals(s) && Q.get(kr) > max) {
						max = Q.get(kr);
						// Update best strategy
						best.put(s, kr.action);
						// Update best reward
						V.replace(s, max);
					}
				}
			}
			// Counts number of iterations
			h++;
			diff = 0;

			// Compute the highest increase in the accumulated reward for a state and store it in diff
			for (State ss : V.keySet()) {
				if (Math.abs(V.get(ss) - Vp.get(ss)) > diff) diff = Math.abs(V.get(ss) - Vp.get(ss));
			}
			LOGGER.log(Level.FINER, "Diff: " + diff);
		}
		LOGGER.log(Level.FINER, "h: " + h);
	}

	// ########################
	// #### Custom classes ####
	// ########################

	private class Key {
		// From 0 to n^2 -1
		public State fromState;
		public State toState;
		// From 0 to n, where action n is pick the task and go to the to city
		public int action;

		public Key(State f, int a, State t) {
			fromState = f;
			action = a;
			toState = t;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (this == null || getClass() != o.getClass()) return false;
			Key k = (Key) o;
			boolean first = (k.fromState.x.id == this.fromState.x.id && k.fromState.y.id == this.fromState.y.id);
			boolean second = (k.toState.x.id == this.toState.x.id && k.toState.y.id == this.toState.y.id);
			boolean a = (k.action == this.action);
			return (first && second && a);
		}

		@Override
		public int hashCode() {
			return (fromState.x.id * numCities + fromState.y.id + toState.x.id * numCities + toState.y.id + action);
		}
	}

	private class KeyReward {
		// From 0 to n^2 -1
		public State fromState;
		// From 0 to n, where action n is pick the task and go to the to city
		public int action;

		public KeyReward(State f, int a) {
			fromState = f;
			action = a;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (this == null || getClass() != o.getClass()) return false;
			KeyReward k = (KeyReward) o;
			boolean first = (k.fromState.x.id == this.fromState.x.id && k.fromState.y.id == this.fromState.y.id);
			boolean a = (k.action == this.action);
			return (first && a);
		}

		@Override
		public int hashCode() {
			return (fromState.x.id * numCities + fromState.y.id + action);
		}
	}

	private class State {
		// A state is a tuple (x,y) with x != y where x is the current city, y is the
		// destination city of the task found in x (if existing).
		// The state (x,y) with x == y indicates that no state was found in x.
		public City x;
		public City y;

		public State(City a, City b) {
			x = a;
			y = b;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (this == null || getClass() != o.getClass()) return false;
			State k = (State) o;
			return (k.x.id == this.x.id && k.y.id == this.y.id);
		}

		@Override
		public int hashCode() {
			return (x.id * numCities + y.id);
		}
	}
}
