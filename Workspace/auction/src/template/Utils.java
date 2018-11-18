package template;

import java.util.ArrayList;

public class Utils {
	//Utiliy methods
		public static ArrayList<Tupla>[] cloneSolution (ArrayList<Tupla>[] s){
			ArrayList<Tupla>[] newS = (ArrayList<Tupla>[]) new ArrayList[s.length];
			for (int i=0; i<s.length; i++) {
				newS[i] = new ArrayList<Tupla>(cloneList(s[i]));
			}
			return newS;
		}
		
		public static ArrayList<Tupla> cloneList (ArrayList<Tupla> l){
			ArrayList<Tupla> newL = new ArrayList<Tupla>();
			for (Tupla t : l) {
				newL.add(t.clone());
			}
			return newL;
		}
		
	
}
