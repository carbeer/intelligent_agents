package template;

import java.util.ArrayList;
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


import java.util.ArrayList;

public class SLS {
	
	ArrayList<Tupla>[] solutions;
	int numTasks;
	int numVechicles;
	int numCities;
	private City[] citiesIndex;
	private Task[] taskList;
	private Vehicle[] vehiclesList;
	
	
	public SLS (Topology topology, List<Vehicle> vehicles, City[] citiesIndex, Task[] taskList) {
		this.citiesIndex = citiesIndex;
		this.taskList = taskList;
		this.vehiclesList = new Vehicle[this.numVechicles];
		this.numVechicles = vehicles.size();
		this.numTasks = this.taskList.length;
		this.numCities = this.citiesIndex.length;
		this.solutions = (ArrayList<Tupla>[]) new ArrayList[this.numVechicles];
		
		int k=0;
		for (Vehicle v : vehicles) {
			this.vehiclesList[k] = v;
			k++;
		}
		
		search();
		
	}
	
	private void search() {
		initialSolution();
	}
	
	private void initialSolution() {
		//TO be optimized
		//at least there is one vehicle, give it sequentially all tasks
		int capacity = this.vehiclesList[0].capacity();
		City currentCity = this.vehiclesList[0].getCurrentCity();
		double costkm = this.vehiclesList[0].costPerKm();
		
		for (int i=0; i<this.numTasks; i++) {
			capacity -= this.taskList[i].weight;
			solutions[0].add(new Tupla(this.taskList[i], 1, capacity, costkm * currentCity.distanceTo(this.taskList[i].pickupCity)));
			capacity += this.taskList[i].weight;
			currentCity = this.taskList[i].pickupCity;
			solutions[0].add(new Tupla(this.taskList[i], 2, capacity, costkm * currentCity.distanceTo(this.taskList[i].deliveryCity)));
			currentCity = this.taskList[i].deliveryCity;
		}
	}
	
	private void chooseNeighbors() {}
	
	private void changeOrder() {};
	private void changeVehicle() {};
	
	private boolean swap(int v, int a1, int a2 ) {
		boolean admitted = true;
		ArrayList<Tupla> neighbor = (ArrayList<Tupla>) this.solutions[0].clone();
		neighbor.add(a1, new Tupla(this.solutions[v].get(a2).task, this.solutions[v].get(a2).action, this.solutions[v].get(a2).capacityLeft, this.solutions[v].get(a2).cost));
		neighbor.add(a2, new Tupla(this.solutions[v].get(a1).task, this.solutions[v].get(a1).action, this.solutions[v].get(a1).capacityLeft, this.solutions[v].get(a1).cost));
		//Short circuit evaluation
		if (checkSwap(neighbor, a1, this.vehiclesList[v].capacity()) && checkSwap(neighbor, a2, this.vehiclesList[v].capacity())) {
			this.solutions[v] = neighbor;
			return true;
		}
		return false;
		
	}
	 
	private boolean checkSwap(ArrayList<Tupla> p, int a, double vehicleCapacity) {
		//check logical order
		if (p.get(a).action == 1) {
			
			//check if delivered is first than new pick up position (just if it has moved forward)
			for (int i=0; i <a; i++ ) if (p.get(i).task.equals(p.get(a).task))return false;			
			
		}
		else {
			//check if picked up after new delivery
			for (int i=a+1; i < p.size(); i++) if (p.get(i).task.equals(p.get(a).task)) return false;		
		}
		
		//given that's logically correct, check for capacity constraints and update capacity
		double capacityLeft = vehicleCapacity;
		for (int j = 0; j < p.size(); j++ ) {
			//update capacity 
			if(p.get(j).action == 1) {
				p.get(j).capacityLeft = capacityLeft - p.get(j).task.weight;
				if (capacityLeft <0) return false;					
			}
			else {
				p.get(j).capacityLeft = capacityLeft + p.get(j).task.weight;
			}
			capacityLeft= p.get(j).capacityLeft;
			
		}
		return true;
	}
	
	
	
	
	private void localSearch() {};
	
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
	}
}
