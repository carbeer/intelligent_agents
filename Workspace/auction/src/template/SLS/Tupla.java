package template.SLS;

import logist.task.Task;


//Class for Tupla describing actions.
public class Tupla{
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
	
	public Tupla clone() {
		return new Tupla(this.task, this.action, this.capacityLeft, this.cost);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		Tupla o = (Tupla) obj;
		if (this.task.equals(o.task) && this.action == o.action && this.capacityLeft == o.capacityLeft && this.cost == o.cost) return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		int hashCode = 1;
		hashCode = 31*hashCode + this.task.hashCode();
		hashCode = 31*hashCode + this.action;
		hashCode = 31*hashCode + (int) this.capacityLeft;
		hashCode = 31*hashCode + (int) this.cost;
		return hashCode;
	}
}