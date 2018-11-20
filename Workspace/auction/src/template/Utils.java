package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.awt.*;
import java.util.ArrayList;

public class Utils {

	public static ArrayList<Tupla> cloneList(ArrayList<Tupla> l){
		ArrayList<Tupla> newL = new ArrayList<Tupla>();
		for (Tupla t : l) {
			newL.add(t.clone());
		}
		return newL;
	}

	public static ArrayList<Tupla>[] cloneArray(ArrayList<Tupla>[] l) {
		ArrayList<Tupla>[] newS = (ArrayList<Tupla>[]) new ArrayList[l.length];
		for (int i=0; i< l.length; i++) {
			newS[i] = new ArrayList<Tupla>(cloneList(l[i]));
		}
		return newS;
	}
	
}

class Configuration {
	// Potential TODO: Implement and evaluate FROM_LAST_BEST
	enum INIT_SOLUTION {ALL_TO_BIGGEST, EACH_TO_CLOSEST}

	// Config params
	static INIT_SOLUTION initSolution = INIT_SOLUTION.EACH_TO_CLOSEST;
}

abstract class OpponentSimulation {
	public abstract long estimateBid(Task t, long timeout);
	public abstract void auctionFeedback(Task previous, long realBid, boolean won);
}

class DummyVehicle implements Vehicle {

	int capacity;
	Topology.City homeCity;
	int costPerKm;

	public DummyVehicle(int costPerKm, Topology.City c, int capacity) {
		this.capacity = capacity;
		this.homeCity = c;
		this.costPerKm = costPerKm;
	}

	@Override
	public int id() {
		return 0;
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public int capacity() {
		return this.capacity;
	}

	@Override
	public Topology.City homeCity() {
		return this.homeCity;
	}

	@Override
	public double speed() {
		return 0;
	}

	@Override
	public int costPerKm() {
		return this.costPerKm;
	}

	@Override
	public Topology.City getCurrentCity() {
		return this.homeCity;
	}

	@Override
	public TaskSet getCurrentTasks() {
		return null;
	}

	@Override
	public long getReward() {
		return 0;
	}

	@Override
	public long getDistanceUnits() {
		return 0;
	}

	@Override
	public double getDistance() {
		return 0;
	}

	@Override
	public Color color() {
		return null;
	}
}