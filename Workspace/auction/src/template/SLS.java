package template;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import java.util.List;
import java.util.Random;


import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class SLS {
	Solution bestSolution;
	Solution tempSolution;

	private ArrayList<Task> taskList;
	private Vehicle[] vehiclesList;
	int numVehicles;

	private int stuck = 0;
	private double currentProb;
	private int jumpWhen;

	final double END_STAGE1;
	final double END_STAGE2;
	final double END_STAGE3;


	public SLS (List<Vehicle> vehicles, ArrayList<Task> taskList, double timeout) {
		this.taskList = taskList;
		this.numVehicles = vehicles.size();
		this.vehiclesList = new Vehicle[this.numVehicles];
		this.bestSolution = new Solution(this.numVehicles);

		// TODO: Maybe it would be worthwhile to make some kind of thread that monitors time and ensures returning before the timeout --> optimizes time usag
		//0.9 to be sure to respect timeout (good for reasonable timeouts)
		END_STAGE1 = System.currentTimeMillis() + timeout * 0.3;
		END_STAGE2 = System.currentTimeMillis() + timeout * 0.6;
		END_STAGE3 = System.currentTimeMillis() + timeout * 0.9;


		int k = 0;
		for (Vehicle v : vehicles) {
			this.vehiclesList[k] = v;
			k++;
		}
		if (taskList.isEmpty()) return;
		initialSolution();
		search();
	}

	/** TODO: Assign to closest vehicle?
	 * Naive initial assignment of all tasks to one vehicle (the biggest one)
	 */
	private void initialSolution() {
		int maxCapacity = 0;
		int v = 0;
		for (int i=0; i<this.numVehicles; i++) {
			if (this.vehiclesList[i].capacity() > maxCapacity) {
				maxCapacity = this.vehiclesList[i].capacity();
				v = i;
			}
		}
		for (int i=0; i < this.taskList.size(); i++ ) {
			this.bestSolution.vehiclePlan[v].add(new Tupla(this.taskList.get(i), 1, this.vehiclesList[v].capacity() - this.taskList.get(i).weight, 0));
			this.bestSolution.vehiclePlan[v].add(new Tupla(this.taskList.get(i), 2, this.vehiclesList[v].capacity() + this.taskList.get(i).weight, 0));
		}
		fixCost(this.bestSolution.vehiclePlan[v], v);
	}
	
	private void search() {
		this.tempSolution = this.bestSolution.clone();

		System.out.println("Initial Solution: ");
		this.bestSolution.print();

		this.currentProb = 0.8;
		this.jumpWhen = 20;
		searchEpoch(END_STAGE1);

		this.currentProb = 0.6;
		this.jumpWhen = 50;
		searchEpoch(END_STAGE2);

		this.currentProb = 0.3;
		this.jumpWhen = 75;
		searchEpoch(END_STAGE3);

		System.out.println("Final Solution: ");
		this.bestSolution.print();
	}

	public void searchEpoch(double end) {
		while (System.currentTimeMillis() < end) {
			HashSet<Solution> neighbors = chooseNeighbors(tempSolution);
			localSearch(neighbors, tempSolution);
		}
	}

	/**
	 *
	 * @param s Currently best list of tuples
	 */
	private HashSet<Solution> chooseNeighbors(Solution s) {
		HashSet<Solution> ns = new HashSet<Solution>();
		Random rand = new Random();
		int v1 = rand.nextInt(this.numVehicles);


		if (s.vehiclePlan[v1].size() <= 0) return ns;

		for (int v2 = 0; v2 < this.numVehicles; v2++) {
			// No need to rotate tasks around the same vehicle
			if (v2 == v1) continue;

			Solution newSolution = s.clone();
			// Move task from v1 to v2 and add this as neighbor solution
			changeVehicle(v1, v2, newSolution.vehiclePlan);
			ns.add(newSolution);

			if (newSolution.vehiclePlan[v1].size() <= 0) continue;

			// Try to insert action a2 at index a1
			for (int a2 = 0; a2 < newSolution.vehiclePlan[v1].size(); a2++ ) {
				for (int a1 = 0; a1 < newSolution.vehiclePlan[v1].size(); a1++) {
					// Try to move a2 to a1
					ArrayList<Tupla> newPlan = Utils.cloneList(newSolution.vehiclePlan[v1]);
					newPlan.add(a1, newPlan.remove(a2));

					if (checkMove(newPlan, a1, this.vehiclesList[v1].capacity())) {

						//if it is allowed, I generate new solution changing the plan for v1
						Solution newSolutionChanged = newSolution.clone();

						newSolutionChanged.vehiclePlan[v1] = Utils.cloneList(newPlan);
						//fix the cumulative cost of the new list
						fixCost(newSolutionChanged.vehiclePlan[v1], v1);
						ns.add(newSolutionChanged);

					}
				}
			}

		}
		return ns;
	}

	/**
	 * Removes the random task of vehicle v1 and appends it to the plan of v2.
	 * @param v1 index of vehicle1
	 * @param v2 index of vehicle2
	 * @param s the currently best plan
	 */
	private boolean changeVehicle(int v1, int v2, ArrayList<Tupla>[] s) {
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
		//check if we can load it
		if (!(entry.task.weight < this.vehiclesList[v2].capacity()))
			return false;
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
		return true;
	}

	/**
	 * Determines whether a swap is valid and doesn't violate any constraints
	 * @param p The plan that is to be validated
	 * @param a The action that has been changed
	 * @param vehicleCapacity The maximum capacity of the vehicle that is supposed to execute the plan
	 * @return boolean value, indicating whether the swap is valid or not.
	 */
	private boolean checkMove(ArrayList<Tupla> p, int a, int vehicleCapacity) {
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
	 * Implements a stochastic local search method
	 * @param ns Set of neighbors of temporary solution
	 * @param ts temporary Solution
	 * @return Cost of a solution
	 */
	private void localSearch(Set<Solution> ns, Solution ts) {
		if (ns.size() <= 0) {
			// System.out.println("No neighbors available");
			// If no neighbors were generated because of constraints, go on with new iteration
			return;
		}

		Solution best = new Solution(this.numVehicles);
		Solution random = new Solution(this.numVehicles);
		double bestCost = Double.POSITIVE_INFINITY;

		Random rand = new Random();
		int randomNeighbor = rand.nextInt(ns.size());

		int i = 0;
		// Find the random and best tasks among the neighbors
		// For sure best will be an element of ns given the positive_infinity
		for (Solution s : ns) {
			// Pick random neighbor solution for the next step
			if (i == randomNeighbor) {
				random.vehiclePlan = Utils.cloneArray(s.vehiclePlan);
			}

			// Calculate the cost of every neighbor solution
			double newCost = s.computeCost();
			if (newCost < bestCost) {
				best = s;
				bestCost = newCost;
			}
			i++;
		}

		// If better solution found, use it with prob. 1
		if (bestCost < ts.computeCost()) {
			// Update the temporary solution
			ts.vehiclePlan = Utils.cloneArray(best.vehiclePlan);
			this.stuck = 0;
			// Update also the overall solution if applicable
			if (bestCost < this.bestSolution.computeCost()) {
				this.bestSolution.vehiclePlan = Utils.cloneArray(best.vehiclePlan);
			}
		}

		// Use it anyway with currentProb
		else if (rand.nextDouble() < this.currentProb) {
			ts.vehiclePlan = Utils.cloneArray(best.vehiclePlan);
			this.stuck++;
		}

		// Jump to a random neighbor to escape local minima if there hasn't been any improvement
		else if (this.stuck > this.jumpWhen){
			ts.vehiclePlan = Utils.cloneArray(random.vehiclePlan);
			this.stuck = 0;
		}
	}

	//================================================================================
	// COMPUTE FINAL PLAN
	//================================================================================

	public List<Plan> computePlans() {
		List<Plan> plans = new ArrayList<Plan>();
		int i = 0;
		for (ArrayList<Tupla> tupleList : bestSolution.vehiclePlan) {
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

	public Solution getSolution () {
		return this.bestSolution.clone();
	}
}
