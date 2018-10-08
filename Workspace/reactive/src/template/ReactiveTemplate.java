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

	double cost;
	private Agent myAgent;
	private int numCities;
	private City[] citiesIndex;
	private HashMap<Key, Double> transitionsTable;
	private HashMap<KeyReward, Double> transitionsRewardTable;
	private HashMap<State, Double> V;
	private HashMap<KeyReward, Double> Q;
	private HashMap<State, Integer> best;
	private double eps;
	

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		
		eps = 0.00001;
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
		
		cost = agent.vehicles().get(0).costPerKm();
		
		
		//create the tables to work with 
		createTransitionsRewardsTable(topology, td, cost);
		
		
		//Debug printing 
		for (Key k : transitionsTable.keySet())
		{
			System.out.println("From State: " + k.fromState.x.id + " " + k.fromState.y.id + " with action " + k.action + " to State: "  + k.toState.x.id + " " + k.toState.y.id + " " + " with probability " + transitionsTable.get(k));
		}
		
		//Compute optimal policy
		computeOptimalPolicy(discount.doubleValue());
		
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
			//Pick and deliver
			action = new Pickup(availableTask);
		
		}
		else { 
			//Just move to the best neighbor city
			action = new Move(citiesIndex[best.get(currentState)]);				
		}
		return action;
	}
	
	//Method that creates transitions probabilities
	private void createTransitionsRewardsTable (Topology topology, TaskDistribution td, double cost){
		
		transitionsTable = new HashMap<Key, Double>();
		transitionsRewardTable = new HashMap<KeyReward, Double>();
		

		for (City fx : topology) {
			for (City fy : topology) {
				//Create the current state (current cities, task destination city)
				State fState = new State(fx, fy);
			
				//No Task found, go anywhere (neighborhood)
				//Taking action from 0 to numCities-1 depending on the destination city
				if (fx.id == fy.id) {
					
					for (City tx : topology) {
						for (City ty : topology) {
							//Create the next state (next cities, dest. task next city)
							State tState = new State(tx,  ty);
							//the value is the prob. of finding in the destination city (tx) a task for the city ty
							//the action is the number of the destination city city
							if( fState.x.hasNeighbor(tx)) transitionsTable.put(new Key(fState,  tx.id, tState), new Double(td.probability(tx, ty)));
							
						}	
						//Reward table R(s,a)
						if(fState.x.hasNeighbor(tx)) transitionsRewardTable.put(new  KeyReward(fState, tx.id), new Double(-(cost * fState.x.distanceTo(tx))));
					}				
				}
				//Task found
				//If a tupla (s,a,s') is not in the hashmap, its transition probability is 0 
				else {
					for (City tx : topology) {
						for (City ty : topology) {
							State tState = new State(tx,  ty);
							//Action numCities (value of) means pick up and deliver
							if (tx.id == fState.y.id) transitionsTable.put(new Key(fState, numCities, tState), new Double(td.probability(tx, ty)));
							//Action tx.id means not to pick up the task and just move, if the city is a neighbor city 
							if(fState.x.hasNeighbor(tState.x)) transitionsTable.put(new Key(fState, tx.id, tState), new Double(td.probability(tx, ty)));						
						}
						//Reward if the task was delivered 
						if (tx.id == fState.y.id) transitionsRewardTable.put(new  KeyReward(fState, numCities), new Double((td.reward(fState.x, fState.y) -(fState.x.distanceTo(fState.y)*cost)) ));
						//Reward if just moving without picking the task
						if(fState.x.hasNeighbor(tx)) transitionsRewardTable.put(new  KeyReward(fState, tx.id), new Double(-(fState.x.distanceTo(tx)*cost)));
					}			
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
			V.put(k.fromState, new Double(1));
			Vp.put(k.fromState, new Double(1));
			Q.put(k, new Double(0));
		}
		
		//Compute optimal value and policy
		double diff = 100;
		int h=0;
		double tmp = 0;
		//Value Iteration algorithm
		while (diff > eps){
			for (State si : V.keySet()) {
				Vp.replace(si, new Double(V.get(si)));
			}
			for (State s : V.keySet()) {
				
				for (int i=0; i<=numCities; i++) {
					KeyReward kw = new KeyReward(s,i);
					if (transitionsRewardTable.get(kw) != null) {
						tmp = transitionsRewardTable.get(kw).doubleValue();
					}
				
					for (Key k : transitionsTable.keySet()) {															
						if (k.fromState.equals(s) && k.action == i) {						
							if(Q.get(kw) != null) {
								tmp = tmp + ( discount * transitionsTable.get(k) * V.get(k.toState));
							}							
						}
					}
					Q.replace(kw, new Double(tmp));
				}
				double max = -100000;
				for (KeyReward kr : Q.keySet()) {					
					if (kr.fromState.equals(s) && Q.get(kr) > max) {
						max = Q.get(kr);
						best.put(s, new Integer(kr.action));
						V.replace(s, new Double (max));
					}
					
				}
			}	
			h++;
			diff = 0;
			for (State ss : V.keySet()) {
				if(Math.abs(V.get(ss) - Vp.get(ss)) > diff) diff =  Math.abs(V.get(ss) - Vp.get(ss)) ;					
			}	
			System.out.println(diff);
		}
		System.out.println(h);
	}
	
	//Custom classes
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
