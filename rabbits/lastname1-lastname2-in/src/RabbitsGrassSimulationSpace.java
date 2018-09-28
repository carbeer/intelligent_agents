/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {

	// Grid of Integers. 0 for cells without grass, 1 for cells with grass
	private Object2DGrid garden;
	// TODO
	private Object2DGrid rabbitsSpace;
	
	public RabbitsGrassSimulationSpace (int xSize, int ySize) {
		
		garden = new Object2DGrid(xSize, ySize);
		rabbitsSpace = new Object2DGrid(xSize, ySize);

		// Initialization of a garden without grass
		for(int i = 0; i < xSize; i++){
			for (int j = 0; j < ySize; j++){
		    garden.putObjectAt(i,j,new Integer(0));
		    }
		}
	}

	/**
	 * Randomly spread grass in the garden.
	 * New grass grows only on empty cells (neither rabbits nor grass).
	 * Retries 2 times.
	 * @param grass Number of new grass fields per step
	 */
	public void spreadGrass(int grass) {

	    for(int i = 0; i < grass; i++){

	    	// Choose coordinates
	    	int x = (int)(Math.random()*(garden.getSizeX()));
	    	int y = (int)(Math.random()*(garden.getSizeY()));
	    	
	    	// c is the number of trials before giving up
	    	for (int c = 0; c<3; c++) {

	    		// Get the value of the object at those coordinates
	    		int I = getGrassValueAt(x, y);

	    		// Ensure that the cell is empty, i.e. that there is no rabbit or grass
	    		if (rabbitsSpace.getObjectAt(x, y) == null && I == 0) {

	    			// Grow grass at the cell by setting the value to 1
	    			garden.putObjectAt(x,y,new Integer(1));
	    			break;
	    		}
	    	}
	    }
	}

	/**
	 * @param x coordinate
	 * @param y coordinate
	 * @return 1 if there is grass, 0 if there is no grass on the cell
	 */
	public int getGrassValueAt(int x, int y){
	    int i=0;
	    // TODO: if clause should be obsolete.
	    if(garden.getObjectAt(x,y) != null){
	    	i = ((Integer)garden.getObjectAt(x, y)).intValue();
	    }
	    return i;
	}
	
	public Object2DGrid getCurrentGrassSpace(){
	    return garden;
	}
	
	public Object2DGrid getCurrentRabbitsSpace() {
		return rabbitsSpace;
	}
	
	// Check whether there is already a rabbit on the specified cell
	public boolean isCellOccupiedByRabbit (int x, int y) {
		boolean retVal = false;
		if (rabbitsSpace.getObjectAt(x,y) != null) retVal = true;
		return retVal;
	}

	/**
	 * Add agent to the simulation space
	 * @param rabbit Agent object
	 * @return TODO ?
	 */
	public boolean addAgent(RabbitsGrassSimulationAgent rabbit) {
		boolean retVal = false;
		int count = 0;
		// TODO: Is this the maximum number of retries?
		int countLimit = 10 * garden.getSizeX() * garden.getSizeY();
		
		while ((retVal == false) && (count < countLimit)) {
			int x = (int)(Math.random() * (garden.getSizeX()));
			int y = (int)(Math.random() * (garden.getSizeY()));
			
			if (isCellOccupiedByRabbit(x,y) == false) {
				rabbitsSpace.putObjectAt(x, y, rabbit);

				// Set references for the agent object
				rabbit.setXY(x, y);
				rabbit.setGrassSpace(this);

				// Rabbit tries to eat grass instantly after creation
				rabbit.tryToEat();
				retVal = true;
			}
			count++;
		}
		return retVal;
	}

	// Remove object reference for rabbit at a specific cell
	public void removeRabbitsAt(int x, int y) {
		rabbitsSpace.putObjectAt(x,  y,  null);
	}

	/**
	 * Move rabbit to new cell
	 * @param x coordinate
	 * @param y coordinate
	 * @param newX new coordinate
	 * @param newY new coordinate
	 * @return bool value, indicating whether the move was successful
	 */
	public boolean moveRabbitsAt(int x, int y, int newX, int newY) {
		boolean retVal = false;
		if (!isCellOccupiedByRabbit(newX, newY)) {
			RabbitsGrassSimulationAgent rsa = (RabbitsGrassSimulationAgent)rabbitsSpace.getObjectAt(x, y);
			removeRabbitsAt(x,y);		
			rsa.setXY(newX, newY);
			rabbitsSpace.putObjectAt(newX,  newY, rsa);
			retVal = true;
		}
		return retVal;
	}

	/**
	 * Returns energy from a cell, and if applicable, removes the grass
	 * @param x
	 * @param y
	 * @return energy (0 if no grass, 1 if grass on the cell)
	 */
	public int takeGrassAt(int x, int y) {
		int energy = getGrassValueAt(x, y);
		garden.putObjectAt(x, y, new Integer(0));
		return energy;
	}

	// Returns the amount of grass on the entire map
	public int getTotalGrass() {
		int totalGrass = 0;
		for (int i =0 ; i < rabbitsSpace.getSizeX();i++) {
			for(int j=0; j< rabbitsSpace.getSizeY(); j++) {
				if (getGrassValueAt(i,j) == 1) totalGrass++;
			}
		}
		return totalGrass;
	}

	// Returns the number of rabbits on the entire map
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
