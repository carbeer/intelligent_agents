package template;

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
