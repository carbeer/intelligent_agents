package template;
import java.util.ArrayList;

public class Solution {
	public ArrayList<Tupla>[] array;
	
	public Solution (ArrayList<Tupla>[] array) {
		this.array = array;
	}
	
	public Solution (int size) {
		this.array = (ArrayList<Tupla>[]) new ArrayList[size];
		for (int i=0; i<this.array.length; i++) {
			this.array[i] = new ArrayList<Tupla>();
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		Solution o = (Solution) obj;
		for (int i=0; i<this.array.length; i++) {
			if (this.array[i].size() != o.array[i].size()) return false;
			for (int j=0; j<this.array[i].size(); j++) {
				if (!this.array[i].get(j).equals(o.array[i].get(j))) return false;
			}
		}
		return true;
	}
	public void print (double cost) {
		System.out.println("Total Cost of the solution :" + cost);
		for (int y=0; y < this.array.length; y++) {
			for (Tupla t : this.array[y]) {
				System.out.print(t.task.id + "   ");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	@Override
	public int hashCode() {
		int hashCode = 1;
		for (int i=0; i<this.array.length; i++) {			
		    for (Tupla t : this.array[i])
		        hashCode = 31*hashCode + (t==null ? 0 : t.hashCode());
		}
		return hashCode;
	}
}

