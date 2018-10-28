package template;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.Random;
import java.util.ArrayList;

public class SLS {
	
	ArrayList<Tupla>[] solutions;
	int numTasks;
	int numVechicles;
	int numCities;
	private Task[] taskList;
	private Vehicle[] vehiclesList;
	private double timeout;
	private double fixedProb;
	
	
	public SLS (Topology topology, List<Vehicle> vehicles, Task[] taskList, double timeout) {
		this.taskList = taskList;
		this.numVechicles = vehicles.size();
		this.vehiclesList = new Vehicle[this.numVechicles];
		
		this.numTasks = this.taskList.length;
		this.numCities = topology.size();
		this.solutions = (ArrayList<Tupla>[]) new ArrayList[this.numVechicles];
		for (int i=0; i<this.solutions.length; i++) {
			this.solutions[i] = new ArrayList<Tupla>();
		}
		this.timeout = timeout;
		//randomly chosen 

		this.fixedProb = 0.5;
		

		int k=0;
		for (Vehicle v : vehicles) {
			this.vehiclesList[k] = v;
			k++;
		}
		search();
	}
	
	private void search() {
		Set<ArrayList<Tupla>[]> neighbors = new HashSet<>();
		initialSolution();
		ArrayList<Tupla>[] tempSolution = cloneSolution(this.solutions);
		int o =0;
		while (o < 1000000) {
			chooseNeighbors(tempSolution, neighbors);
			localSearch(neighbors, tempSolution);
			//remove all these neighbors
			neighbors.clear();
			o++;
			if (o % 10000 == 0) {
				System.out.println(computeCost(this.solutions));
				for (int y=0; y < this.solutions.length; y++) {
					for (Tupla t : this.solutions[y]) {
						System.out.print(t.task.id + "   ");
					}
					System.out.println();
				}
			}
		}
		
		
	}

	public List<Plan> computePlans() {
		List<Plan> plans = new ArrayList<Plan>();
		int i = 0;
		for (ArrayList<Tupla> tupleList : solutions) {
			City currentCity = vehiclesList[i].getCurrentCity();
			Plan plan = new Plan(currentCity);
			for (Tupla tuple : tupleList) {
				switch (tuple.action) {
					// Pickup task
					case 1:
						for (City c : currentCity.pathTo(tuple.task.pickupCity)) {
							plan.appendMove(c);
						}
						plan.appendPickup(tuple.task);
						currentCity = tuple.task.pickupCity;
						break;
					// Deliver task
					case 2:
						for (City c : currentCity.pathTo(tuple.task.deliveryCity)) {
							plan.appendMove(c);
						}
						plan.appendDelivery(tuple.task);
						currentCity = tuple.task.deliveryCity;
						break;
				}
			}
			i++;
			plans.add(plan);
		}
		return plans;
	}

	/**
	 * Naive initial assignment of all tasks to one vehicle
	 * TODO: Optimize
	 */
	private void initialSolution() {
		//at least there is one vehicle, give it sequentially all tasks
		int capacity = this.vehiclesList[0].capacity();
		City currentCity = this.vehiclesList[0].getCurrentCity();
		double costkm = this.vehiclesList[0].costPerKm();
		
		for (int i=0; i<this.numTasks; i++) {
			capacity -= this.taskList[i].weight;
			this.solutions[0].add(new Tupla(this.taskList[i], 1, capacity, 0));
			capacity += this.taskList[i].weight;
			currentCity = this.taskList[i].pickupCity;
			this.solutions[0].add(new Tupla(this.taskList[i], 2, capacity, 0));
			currentCity = this.taskList[i].deliveryCity;
		}
		fixCost(this.solutions[0], 0);
	}

	/**
	 *
	 * @param s Currently best list of tuples
	 * @param ns Set of possible neighbor solutions
	 */
	private void chooseNeighbors(ArrayList<Tupla>[] s, Set<ArrayList<Tupla>[]> ns) {
		Random rand = new Random();
		int randomVehicle = rand.nextInt(this.numVechicles);
		int a1;
		int a2;
		// Change order in the plan of randomVehicle
		// s.length is always even (one pickup one delivery for every task)
		for (int i=0; i < s[randomVehicle].size() / 2; i++ ) {
			a1 = rand.nextInt(s[randomVehicle].size());
			a2 = rand.nextInt(s[randomVehicle].size());
			// Try to swap the two actions
			ArrayList<Tupla> newList = swap(randomVehicle, a1, a2, s[randomVehicle]);
			if (newList != null) {

				//if it is allowed, I generate new solution changing the plan for randomVehicle
				ArrayList<Tupla>[] newSolution = cloneSolution(s);

				newSolution[randomVehicle] = newList;
				//fix the cumulative cost of that list
				fixCost(newSolution[randomVehicle], randomVehicle);
				// TODO: Do we need to implement something to make contains work properly?
				if (!ns.contains(newSolution)) ns.add(newSolution);
			}
	
		}

		// Change tasks among vehicles (to choose how many)
		int howMany = 100;
		int v1;
		int v2;

		//it is always allowed 

		for (int i=0; i < howMany; i++) {
			v1 = rand.nextInt(this.numVechicles);
			v2 = rand.nextInt(this.numVechicles);
			//You can always add at the end with the new capacity
			ArrayList<Tupla>[] newSolution = s.clone();
			if (s[v1].size() > 0 && vehiclesList[v2].capacity() >= s[v1].get(0).task.weight) {

				//it is always allowed as adding a sequential action does not affect previous capacity
				newSolution = changeVehicle(v1, v2, newSolution);
				if (!ns.contains(newSolution))
					ns.add(newSolution);
			}
		}
	}

	/**
	 * Removes the first task of vehicle v1 and appends it to the plan of v2.
	 * @param v1 index of vehicle1
	 * @param v2 index of vehicle2
	 * @param s the currently best plan
	 */
	private ArrayList<Tupla>[] changeVehicle(int v1, int v2, ArrayList<Tupla>[] s) {
		//take the first(for sure the first action is a pickup  
		//Remove and shift (can be random then)
		Tupla entry = s[v1].remove(0);
		int i=0;
		//remove the delivery action and rearrange the capacities
		while (true) {
			//remove this element does not affect subsequent actions as the weight of the task was removed anyway
			if (s[v1].get(i).task.equals(entry.task)) { 
				s[v1].remove(i);
				break;
			}
			//if we've not found the delivery yet, just add the weight to every capacity
			else {
				s[v1].get(i).capacityLeft += entry.task.weight;
			}
			i++;
		}
		//added at the end, no need to iterate for capacity (at the end full v2 capacity)
		entry.capacityLeft = this.vehiclesList[v2].capacity() - entry.task.weight;
		Tupla deliverEntry = new Tupla(entry.task, 2, this.vehiclesList[v2].capacity(), 0.);
		s[v2].add(entry);
		s[v2].add(deliverEntry);

		if (s[v1].size() >0) fixCost(s[v1], v1);
		if (s[v2].size() >0) fixCost(s[v2], v2);
		
		return s;

	}

	/**
	 * Try to swap two actions in the plan of one vehicle.
	 * Returns null if it's not possible to swap the actions.
	 *
	 * @param v Current vehicle
	 * @param a1 Action 1
	 * @param a2 Action 2
	 * @param vPlan Currently best plan of vehicle v
	 * @return
	 */
	private ArrayList<Tupla> swap(int v, int a1, int a2, ArrayList<Tupla> vPlan ) {
		ArrayList<Tupla> neighbor = (ArrayList<Tupla>) cloneList(vPlan);
		//swap elements
		Tupla temp = neighbor.get(a1);
		neighbor.set(a1, neighbor.get(a2));
		neighbor.set(a2, temp);
		//Short circuit evaluation
		if (checkSwap(neighbor, a1, this.vehiclesList[v].capacity()) && checkSwap(neighbor, a2, this.vehiclesList[v].capacity())) {
			return neighbor;
		}
		return null;
	}

	/**
	 * Determines whether a swap is valid and doesn't violate any constraints
	 * @param p The plan that is to be validated
	 * @param a The action that has been changed
	 * @param vehicleCapacity The maximum capacity of the vehicle that is supposed to execute the plan
	 * @return boolean value, indicating whether the swap is valid or not.
	 */
	private boolean checkSwap(ArrayList<Tupla> p, int a, double vehicleCapacity) {
		// Check logical order - no delivery before pickup, no pickup after delivery
		if (p.get(a).action == 1) {
			for (int i=0; i < a; i++ )
				if (p.get(i).task.equals(p.get(a).task))
					return false;
		}
		else {
			for (int i=a+1; i < p.size(); i++)
				if (p.get(i).task.equals(p.get(a).task))
					return false;
		}
		// Check for capacity constraints and update capacity
		double capacityLeft = vehicleCapacity;
		for (int j = 0; j < p.size(); j++ ) {
			//update capacity 
			if(p.get(j).action == 1) {
				p.get(j).capacityLeft = capacityLeft - p.get(j).task.weight;
				//if at some points I violate constraints
				if (capacityLeft <0) return false;					
			}
			else {
				p.get(j).capacityLeft = capacityLeft + p.get(j).task.weight;
			}
			capacityLeft = p.get(j).capacityLeft;
		}
		return true;
	}

	/**
	 *
	 * @param list
	 * @param v
	 */
	private ArrayList<Tupla> fixCost (ArrayList<Tupla> list, int v) {
		City currentCity = this.vehiclesList[v].getCurrentCity();
		list.get(0).cost = currentCity.distanceTo(list.get(0).task.pickupCity);
		currentCity = list.get(0).task.pickupCity;
		
		for (int i=1; i<list.size(); i++) {
			if (list.get(i).action == 1) {
				list.get(i).cost = list.get(i-1).cost + currentCity.distanceTo(list.get(i).task.pickupCity);
				currentCity = list.get(i).task.pickupCity;
			}
			else {
				list.get(i).cost = list.get(i-1).cost + currentCity.distanceTo(list.get(i).task.deliveryCity);
				currentCity = list.get(i).task.deliveryCity;
			}
		}
		return list;
	}

	/**
	 *
	 * @param s Solution to be computed
	 * @return Cost of a solution
	 */
	private double computeCost (ArrayList<Tupla>[] s) {
		double cost =0;
		for (int i=0; i < s.length; i++) {
			if(s[i].size() >0) cost += s[i].get(s[i].size() -1).cost;
		}
		return cost;
	}
	
	private void localSearch(Set<ArrayList<Tupla>[]> ns, ArrayList<Tupla>[] ts) {
		//just to initialize 
		ArrayList<Tupla>[] best = cloneSolution(ts);
		double bestCost = Double.POSITIVE_INFINITY;
		double newCost;

		//for sure best will an element of ns given the positive_infinity
		for (ArrayList<Tupla>[] s : ns) {
			newCost = computeCost(s);
			if (newCost < bestCost) {
				best = s;
				bestCost = newCost;
			}
		}
		if (computeCost(best) < computeCost(ts)) {
			ts = best;
			if (computeCost(best) < computeCost(this.solutions)) this.solutions = best;
		}
		else {
			Random rand = new Random();
			double p = rand.nextDouble();
			if (p < this.fixedProb) ts = best;
		}
		
		
		
	
	}
	private ArrayList<Tupla>[] cloneSolution (ArrayList<Tupla>[] s){
		ArrayList<Tupla>[] newS = (ArrayList<Tupla>[]) new ArrayList[this.numVechicles];
		for (int i=0; i<s.length; i++) {
			newS[i] = new ArrayList<Tupla>(cloneList(s[i]));
		}
		return newS;
	}
	
	private ArrayList<Tupla> cloneList (ArrayList<Tupla> l){
		ArrayList<Tupla> newL = new ArrayList<Tupla>();
		for (Tupla t : l) {
			newL.add(t.clone());
		}
		return newL;
	}

	private class Tupla{
		public Task task;
		public int action;
		public double capacityLeft;
		double cost;
		
		public Tupla(Task task, int action, double capacityLeft, double cost) {
			this.task = task;
			this.action = action;
			this.capacityLeft = capacityLeft;
			this.cost = cost;
		}
		
		public Tupla clone () {
			return new Tupla(this.task, this.action, this.capacityLeft, this.cost);
		}
	}
}
