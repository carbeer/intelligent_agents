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
	
	Solution solutions;
	int numTasks;
	int numVechicles;
	int numCities;
	private Task[] taskList;
	private Vehicle[] vehiclesList;
	private double timeout;
	private double currentProb;
	private int stuck;
	private int jumpWhen = 20;
	
	public SLS (Topology topology, List<Vehicle> vehicles, Task[] taskList, double timeout) {
		this.taskList = taskList;
		this.numVechicles = vehicles.size();
		this.vehiclesList = new Vehicle[this.numVechicles];
		
		this.numTasks = this.taskList.length;
		this.numCities = topology.size();
		this.solutions = new Solution(this.numVechicles);
		this.stuck = 0;
		this.timeout = timeout;
		this.currentProb = 0.8;
	
		int k=0;
		for (Vehicle v : vehicles) {
			this.vehiclesList[k] = v;
			k++;
		}
		initialSolution();
		search();
	}
	
	private void search() {
		Set<Solution> neighbors = new HashSet<>();

		Solution tempSolution = new Solution(cloneSolution(this.solutions.array));
		
		System.out.println("Initial Solution :" );
		this.solutions.print(computeCost(this.solutions.array));
		
		long time_start = System.currentTimeMillis();
		long time =0;
		boolean third = false;
		boolean second = false;
		//0.9 to be sure to respect timeout (good for reasonable timeouts)
		while (time < this.timeout * 0.9) {
			
			chooseNeighbors(tempSolution, neighbors);
			localSearch(neighbors, tempSolution);
			
			//remove all these neighbors
			neighbors.clear();

			if (!second && time > (this.timeout / 3)  ) {
				this.currentProb = 0.6;
				this.jumpWhen = 50;
				second = true; 
			}
			if (!third && time > (this.timeout / 3) * 2 ){
				this.currentProb = 0.3;
				this.jumpWhen = 75;
				third = true;
			}
			time  =  System.currentTimeMillis() - time_start ;
			
		}
		
		System.out.println("Final Solution :" );
		this.solutions.print(computeCost(this.solutions.array));
		
		
	}

	public List<Plan> computePlans() {
		List<Plan> plans = new ArrayList<Plan>();
		int i = 0;
		for (ArrayList<Tupla> tupleList : solutions.array) {
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
	 * Naive initial assignment of all tasks to one vehicle (the biggest one)
	 */
	private void initialSolution() {
		
		int maxCapacity =0;
		int v=0;
		for (int i=0; i<this.numVechicles; i++) {
			if (this.vehiclesList[i].capacity() > maxCapacity) {
				maxCapacity = this.vehiclesList[i].capacity();
				v = i;
			}
		}
		for (int i=0; i< this.numTasks; i++ ) {
			this.solutions.array[v].add(new Tupla(this.taskList[i], 1, this.vehiclesList[v].capacity() - this.taskList[i].weight, 0));
			this.solutions.array[v].add(new Tupla(this.taskList[i], 2, this.vehiclesList[v].capacity() + this.taskList[i].weight, 0));				
		}
		
		fixCost(this.solutions.array[v], v);
		
	}

	/**
	 *
	 * @param s Currently best list of tuples
	 * @param ns Set of possible neighbor solutions
	 */
	private void chooseNeighbors(Solution s, Set<Solution> ns) {
		Random rand = new Random();
		//indexes for the actions 
		int a1;
		int a2;
	
		// indexes for the vehicles that exchange tasks
		int v1;
		int v2;

		//it is always allowed, two different vehicle tried (heuristic choice)
		for (int y=0; y<2; y++) {
		
			v1 = rand.nextInt(this.numVechicles);
			if (s.array[v1].size() >0) {
				for (int i=0; i < this.numVechicles; i++) {
					v2 = i;
					//You can always add at the end with the new capacity
					Solution newSolution = new Solution (cloneSolution(s.array));
					if (s.array[v1].size() > 0 ) {

						//it is always allowed as adding a sequential action does not affect previous capacity
						changeVehicle(v1, v2, newSolution.array);
						ns.add(newSolution);
					}
					int rv = v1;
					if (newSolution.array[rv].size() != 0) {
						for (int t=0; t < newSolution.array[rv].size(); t++ ) {						
							
							a2 = t;
							for (int j=0; j < newSolution.array[rv].size(); j++) {
							
								a1= j;
								// Try to move a2 to a1
								ArrayList<Tupla> newList = cloneList(newSolution.array[rv]);
								newList.add(a1, newList.remove(a2));
								
								if (checkMove(newList,a1, (double)this.vehiclesList[rv].capacity()))
								{

									//if it is allowed, I generate new newSolutionution changing the plan for v1
									Solution newSolutionChanged = new Solution(cloneSolution(newSolution.array));

									newSolutionChanged.array[rv] = cloneList(newList);
									//fix the cumulative cost of the new list
									fixCost(newSolutionChanged.array[rv], rv);
									ns.add(newSolutionChanged);
								}
							}
						}
					}
				}
			}
		}	
	}

	/**
	 * Removes the random task of vehicle v1 and appends it to the plan of v2.
	 * @param v1 index of vehicle1
	 * @param v2 index of vehicle2
	 * @param s the currently best plan
	 */
	private void changeVehicle(int v1, int v2, ArrayList<Tupla>[] s) {
		//take randomly one task  
		Random rand = new Random();
		int t = rand.nextInt(s[v1].size() / 2);
		int y=0;
		for (int i=0; i<s[v1].size(); i++) {
			if (s[v1].get(i).action == 1) {
				if (y==t) {
					t = i;
					break;
				}
				else {
					y++;
				}
			}
		}
		//now t is the index in the list
		Tupla entry = s[v1].remove(t);
		int i=t;
		//remove the delivery action and rearrange the capacities
		while (true) {
			//removing this element does not affect capacities subsequent actions as the weight of the task was removed anyway 
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
		s[v2].add(new Tupla(entry.task, 1, entry.capacityLeft, 0.));
		s[v2].add(deliverEntry);
		
		//fix costs
		if (s[v1].size() >0) fixCost(s[v1], v1);
		if (s[v2].size() >0) fixCost(s[v2], v2);

	}


	/**
	 * Determines whether a swap is valid and doesn't violate any constraints
	 * @param p The plan that is to be validated
	 * @param a The action that has been changed
	 * @param vehicleCapacity The maximum capacity of the vehicle that is supposed to execute the plan
	 * @return boolean value, indicating whether the swap is valid or not.
	 */
	private boolean checkMove(ArrayList<Tupla> p, int a, double vehicleCapacity) {
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
				capacityLeft = p.get(j).capacityLeft;
				if (capacityLeft <0) return false;					
			}
			else {
				p.get(j).capacityLeft = capacityLeft + p.get(j).task.weight;
				capacityLeft = p.get(j).capacityLeft;
			}
			
		}
		return true;
	}

	/**
	 * Fix the cumulative costs of the actions
	 * @param list
	 * @param v
	 */
	private void fixCost (ArrayList<Tupla> list, int v) {
		City currentCity = this.vehiclesList[v].getCurrentCity();
		list.get(0).cost = this.vehiclesList[v].costPerKm() * currentCity.distanceTo(list.get(0).task.pickupCity);
		currentCity = list.get(0).task.pickupCity;
		
		for (int i=1; i<list.size(); i++) {
			if (list.get(i).action == 1) {
				list.get(i).cost = list.get(i-1).cost + this.vehiclesList[v].costPerKm() * currentCity.distanceTo(list.get(i).task.pickupCity);
				currentCity = list.get(i).task.pickupCity;
			}
			else {
				list.get(i).cost = list.get(i-1).cost + this.vehiclesList[v].costPerKm() * currentCity.distanceTo(list.get(i).task.deliveryCity);
				currentCity = list.get(i).task.deliveryCity;
			}
		}
		return;
	}

	/**
	 * Compute total cost of one solution
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
	
	
	/**
	 * Implement a stochastic local search method
	 * @param ns Set of neighbors of temporary solution
	 * @param ts temporary Solution
	 * @return Cost of a solution
	 */
	private void localSearch(Set<Solution> ns, Solution ts) {
		
		//just to initialize 
		Solution best = new Solution(this.numVechicles);
		Solution random = new Solution(this.numVechicles);
		double bestCost = Double.POSITIVE_INFINITY;
		double newCost;
		Random rand = new Random();
		int randomNeighbor = 0;
		if (ns.size()>0 ) {
		         randomNeighbor = rand.nextInt(ns.size());	
		}
		else {
			//if no neighbors were generated because of constraints, go on with new itereation
			return;
		}
		int r = 0;
		//find the random and best tasks among the neighbors 
		//for sure best will be an element of ns (if not empty) given the positive_infinity
		
		for (Solution s : ns) {
			newCost = computeCost(s.array);

			if (ns.size() > 0 && r == randomNeighbor) {
				random.array = cloneSolution(s.array);
			}
			if (newCost < bestCost) {
				best = s;
				bestCost = newCost;
			}
			r++;
		}
		
		double p = rand.nextDouble();
		
		//If better solution found, use that w.p. 1
		if (bestCost < computeCost(ts.array)) {
			ts.array = cloneSolution(best.array);
			this.stuck =0;
			if (bestCost < computeCost(this.solutions.array)) {
				this.solutions.array = cloneSolution(best.array);
			}
		}
		//Use that anyway w.p. currentProb
		else if (p < this.currentProb) {
			
			this.stuck++;
			ts.array = cloneSolution(best.array);
		}
		//Jump to a random solution if not improve for a long
		else if (stuck > jumpWhen){
			ts.array = cloneSolution(random.array);
			this.stuck = 0;
		}
	
	}
	
	//Utiliy methods
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
	
	public Solution getSolution () {
		return new Solution(cloneSolution(this.solutions.array));
	}

}
