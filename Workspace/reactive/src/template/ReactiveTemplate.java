package template;

import java.util.HashMap;
import java.util.Random;

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

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private int numCities;
	private City[] citiesIndex;
	private HashMap<Key, Double> transitionsTable;
	private HashMap<KeyReward, Double> transitionsRewardTable;
	private HashMap<State, Double> V;
	private HashMap<KeyReward, Double> Q;
	private HashMap<State, Integer> best;
	private double[][] distributionsTable;
	private double[][] rewardsTable;
	private int[][] hasNeighbour;
	private double eps;
	

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		
		eps = 0.001;
		numCities = topology.size();
		citiesIndex = new City[numCities];
		int index = 0;
		for (City c : topology) {
			citiesIndex[index] = c;
			index++;
		}
		//create the tables to work with (also the hasNeighbour one)
		
		
		createRewarDistributionTable(td, topology);
		createTransitionsRewardsTable(topology, td);
		
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		
		//Compute optimal policy
		computeOptimalPolicy(discount.shortValue());
		
		System.out.println(V.size());
		System.out.println(best.size());
		System.out.println(Q.size());

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		State currentState;
		City currentCity = vehicle.getCurrentCity();
		
		//Figure out current state
		if (availableTask != null) {
			currentState = new State(currentCity, availableTask.deliveryCity);
		}
		else {
			currentState = new State(currentCity, currentCity);
		}
		
		if (best.get(currentState) == numCities) { 
			action = new Pickup(availableTask);
		
		}
		else { 
			action = new Move(citiesIndex[best.get(currentState)]);				
		}
		
		/*
		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		*/
		return action;
	}
	
	//Method that creates transitions probabilities
	private void createTransitionsRewardsTable (Topology topology, TaskDistribution td){
		
		transitionsTable = new HashMap<Key, Double>();
		transitionsRewardTable = new HashMap<KeyReward, Double>();
		

		for (City fx : topology) {
			for (City fy : topology) {
				//Create the current state (current cities, dest. task city)
				State fState = new State(fx, fy);
			
				//No Task found, go anywhere (neighbour)
				//Taking action from 0 to numCities-1 depending on the dest. city
				if (fx.id == fy.id) {
					
					for (City tx : topology) {
						for (City ty : topology) {
							//Create the next state (next cities, dest. task next city)
							State tState = new State(tx,  ty);
							//the value is the prob. of finding in the des. city (tState.x) a task for the city tState.y
							//the action is the number of the dest. city
							transitionsTable.put(new Key(fState,  tx.id, tState), td.probability(tx, ty) * hasNeighbour[fState.x.id][tState.x.id]);
							
						}	
						if(hasNeighbour[fState.x.id][tx.id] ==1) transitionsRewardTable.put(new  KeyReward(fState, tx.id), -fState.x.distanceTo(fState.y));
					}
					
					
					
				}
				//Task found
				//If a tupla (s,a,s') is not in the hashmap, its transition prob. is 0 
				else {
					for (City tx : topology) {
						for (City ty : topology) {
							State tState = new State(tx,  ty);
							//Action numCities (value of) means pick up and deliver
							transitionsTable.put(new Key(fState, numCities, tState), td.probability(tx, ty));
							
						}
					}
					transitionsRewardTable.put(new  KeyReward(fState, numCities), td.reward(fState.x, fState.y) -fState.x.distanceTo(fState.y));
				
				}
				
			}
			
			
		}

		
	}
	
	//Method that keep tracks of task probabilities and create simple table 
	private void createRewarDistributionTable (TaskDistribution td, Topology topology) {
		
		distributionsTable = new double[numCities][numCities];
		rewardsTable = new double[numCities][numCities];
		hasNeighbour = new int[numCities][numCities];
	
		for (City from : topology) {
			for (City to : topology) {
				
				if (from.id != to.id) {
					
					distributionsTable[from.id][to.id] = td.probability(from, to);
					
					rewardsTable[from.id][to.id] = td.reward(from, to);
					if (from.hasNeighbor(to)) hasNeighbour[from.id][to.id] = 1;

				}
				else {
					//Probability of not having task in from
					distributionsTable[from.id][from.id] = td.probability(from, null);
					rewardsTable[from.id][to.id] = 0;
					hasNeighbour[from.id][to.id] = 0;
				}
			}
		}

	}
	
	private void computeOptimalPolicy(double discount){
		
		V = new HashMap<State, Double>();
		HashMap<State, Double> Vp = new HashMap<State, Double>();
		Q = new HashMap<KeyReward, Double>();
		best = new HashMap<State, Integer>();
		
		//initialize V and Q tables
		for (KeyReward k : transitionsRewardTable.keySet()) {
			V.put(k.fromState, new Double(-10000));
			Vp.put(k.fromState, new Double(-10000));
			Q.put(k, new Double(transitionsRewardTable.get(k)));
		}
		
		//Compute optimal value and policy
		double diff = 100;
		
		while (diff > eps){
			for (State s : V.keySet()) {
				Vp.replace(s, V.get(s));
			}
			for (State s : V.keySet()) {
				for (int i=0; i<=numCities; i++) {
					KeyReward kw = new KeyReward(s,i);
					Q.replace(new KeyReward(s,i), transitionsRewardTable.get(new KeyReward(s,i)) );
				
					for (Key k : transitionsTable.keySet()) {									
						double tmp = 0;
						if (k.fromState.equals(s) && k.action == i) {
						
							if(Q.get(kw) != null) tmp = Q.get(kw);
						
						
							Q.replace(kw, tmp + discount * transitionsTable.get(k) * V.get(s));
						}
					}
				}
				//find max and replace 
				for (KeyReward kw : Q.keySet()) {
					if (kw.fromState.equals(s) && Q.get(kw) > V.get(s)) {
						V.replace(s, Q.get(kw));
						best.put(s, new Integer(kw.action));
					}
				}
			}
			diff=0;
			for (State s : V.keySet()) {
				diff += Math.abs(V.get(s) - Vp.get(s)) ;
			}
			
		}
	}
	
	private class Key {
		//from 0 to n^2 -1 
		public State fromState;
		public State toState;
		//from 0 to n, where action n is pick the task and go to the to city
		public int action;
		
		public Key(State f, int a, State t) {
			fromState = f;
			action = a;
			toState = t;
		}
		@Override
		public boolean equals (Object o) {
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
		//from 0 to n^2 -1 
		public State fromState;
		//from 0 to n, where action n is pick the task and go to the to city
		public int action;
		
		public KeyReward(State f, int a) {
			fromState = f;
			action = a;
		}
		
		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (this == null || getClass() != o.getClass()) return false;
			KeyReward k = (KeyReward) o;
			boolean first = (k.fromState.x.id == this.fromState.x.id && k.fromState.y.id == this.fromState.y.id);
			boolean a = (k.action == this.action);
			return (first  && a);
		}
		
		@Override
		public int hashCode() {
			return (fromState.x.id * numCities + fromState.y.id + action);
		}
	}
	private class State{
		//a state is a couple (x,y) with x != y where x is the current city, y is the
		//destination city of the task found in x if found.
		//The state (x,y) with x == y indicate the State "being in x and not found task"
		public City x;
		public City y;
		public State(City a, City b) {
			x = a;
			y = b;
		}
		
		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (this == null || getClass() != o.getClass()) return false;
			State k = (State) o;
			return (k.x.id == this.x.id && k.y.id == this.y.id);
		}
		@Override
		public int hashCode() {
			return (x.id * numCities + y.id );
		}
	}
}
