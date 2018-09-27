/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
	
	private Object2DGrid garden;
	private Object2DGrid rabbitsSpace;
	
	public RabbitsGrassSimulationSpace (int xSize, int ySize) {
		
		garden = new Object2DGrid(xSize, ySize);
		rabbitsSpace = new Object2DGrid(xSize, ySize);
		
		
		for(int i = 0; i < xSize; i++){
			for (int j = 0; j < ySize; j++){
		    garden.putObjectAt(i,j,new Integer(0));
		    }
		}
	}
	
	public void spreadGrass(int grass) {
		// Randomly place grass in the garden
	    for(int i = 0; i < grass; i++){

	    	// Choose coordinates
	    	int x = (int)(Math.random()*(garden.getSizeX()));
	    	int y = (int)(Math.random()*(garden.getSizeY()));

	    	// Get the value of the object at those coordinates
	    	int I = getValueAt(x, y);
	    	
	    	// Replace the Integer object with another one with the new value
	    	if (i != 1 && i != -1 ) {
	    		garden.putObjectAt(x,y,new Integer(1));
	    	}
	    }
	}
	public int getValueAt(int x, int y){
	    int i;
	    if(garden.getObjectAt(x,y)!= null){
	    	i = ((Integer)garden.getObjectAt(x,y)).intValue();
	    }
	    else if(rabbitsSpace.getObjectAt(x, y) != null) {
	    	i = -1;
	    }
	    
	    else{
	    	i = 0;
	    }
	    return i;
	}
	
	public Object2DGrid getCurrentGrassSpace(){
	    return garden;
	}
	
	public Object2DGrid getCurrentRabbitsSpace() {
		return rabbitsSpace;
	}
	
	//Rabbits can be placed in a cell with grass
	public boolean isCellOccupied (int x, int y) {
		boolean retVal = false;
		if (garden.getObjectAt(x, y) != null && rabbitsSpace.getObjectAt(x,y) != null) retVal = true;
		return retVal;
	}
	
	public boolean addAgent(RabbitsGrassSimulationAgent rabbit) {
		boolean retVal = false;
		int count = 0;
		int countLimit = 10 * garden.getSizeX() * garden.getSizeY();
		
		while ((retVal == false) && (count < countLimit)) {
			int x = (int)(Math.random() * (garden.getSizeX()));
			int y = (int)(Math.random() * (garden.getSizeY()));
			
			if (isCellOccupied(x,y) == false) {
				rabbitsSpace.putObjectAt(x, y, rabbit);
				rabbit.setXY(x, y);
				rabbit.setGrassSpace(this);
				retVal = true;
			}
			count++;
		}
		
		return retVal;
	}
	public void removeRabbitsAt(int x, int y) {
		rabbitsSpace.putObjectAt(x,  y,  null);
	}
	
	public boolean moveRabbitsAt(int x, int y, int newX, int newY) {
		boolean retVal = false;
		if (!isCellOccupied(newX, newY)) {
			RabbitsGrassSimulationAgent rsa = (RabbitsGrassSimulationAgent)rabbitsSpace.getObjectAt(x, y);
			removeRabbitsAt(x,y);
			System.out.println("NewwX and NewY: " + newX + " " + newY );
			System.out.println("NewwX and NewY: " + x + " " + y ) ;
			
			
			rsa.setXY(newX, newY);
			rabbitsSpace.putObjectAt(newX,  newY, rsa);
			retVal = true;
		}
		return retVal;
	}
	
	public int takeGrassAt(int x, int y) {
		int energy = getValueAt(x, y);
		garden.putObjectAt(x, y, new Integer(0));
		return energy;
	}
	
	public int getTotalGrass() {
		int totalGrass = 0;
		for (int i =0 ; i < rabbitsSpace.getSizeX();i++) {
			for(int j=0; j< rabbitsSpace.getSizeY(); j++) {
				if (getValueAt(i,j) == 1) totalGrass++;
			}
		}
		return totalGrass;
	}
	
	public int getTotalRabbits() {
		int totalRabbits = 0;
		for (int i =0 ; i < rabbitsSpace.getSizeX();i++) {
			for(int j=0; j< rabbitsSpace.getSizeY(); j++) {
				if (rabbitsSpace.getObjectAt(i, j) != null) totalRabbits++;
			}
		}
		return totalRabbits;
	}
	
}
