package template;
import java.util.ArrayList;

public class Solution implements Cloneable{
	
	public ArrayList<Tupla>[] vehiclePlan;
	
	public Solution(ArrayList<Tupla>[] array) {
		this.vehiclePlan = array;
	}
	
	public Solution(int size) {
		this.vehiclePlan = (ArrayList<Tupla>[]) new ArrayList[size];
		for (int i = 0; i<this.vehiclePlan.length; i++) {
			this.vehiclePlan[i] = new ArrayList<Tupla>();
		}
	}

	public void print() {
		System.out.println("Total Cost of the solution: " + computeCost());
		for (int y = 0; y < this.vehiclePlan.length; y++) {
			for (Tupla t : this.vehiclePlan[y]) {
				System.out.print(t.task.id + "   ");
			}
			System.out.println();
		}
		System.out.println();
	}

	/** TODO: Possible increase performance by caching costs
	 * Compute total cost of one solution
	 * @return Cost of a solution
	 */
	public double computeCost() {
		double cost = 0;
		for (int i = 0; i < vehiclePlan.length; i++) {
			if(vehiclePlan[i].size() > 0) cost += vehiclePlan[i].get(vehiclePlan[i].size()-1).cost;
		}
		return cost;
	}

	@Override
	public Solution clone() {
		ArrayList<Tupla>[] newS = Utils.cloneArray(this.vehiclePlan);
		return new Solution(newS);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		Solution o = (Solution) obj;
		for (int i = 0; i<this.vehiclePlan.length; i++) {
			if (this.vehiclePlan[i].size() != o.vehiclePlan[i].size()) return false;
			for (int j = 0; j<this.vehiclePlan[i].size(); j++) {
				if (!this.vehiclePlan[i].get(j).equals(o.vehiclePlan[i].get(j))) return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i<this.vehiclePlan.length; i++) {
		    for (Tupla t : this.vehiclePlan[i])
		        hashCode = 31*hashCode + (t==null ? 0 : t.hashCode());
		}
		return hashCode;
	}
}

