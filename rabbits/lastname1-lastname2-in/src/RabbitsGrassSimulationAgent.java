import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	
	private static final int ENERGYEARNT = 5;
	private static final int INITENERGY = 18;
	
	private int energy;
	private int x;
	private int y;
	private int direction;             //0 north, 1 east, 2 south, 3 west
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace grassSpace;
	
	public RabbitsGrassSimulationAgent () {
		energy = INITENERGY;
		x = -1;
		y = -1;
		setDirection();
		IDNumber++;
		ID = IDNumber;
		
	}

	public void draw(SimGraphics G) {
		G.drawFastRoundRect(Color.white);
		
	}
	
	public void setXY (int newX, int newY) {
		x = newX;
		y = newY;
	}
	
	public void setGrassSpace(RabbitsGrassSimulationSpace rsa) {
		grassSpace = rsa;
	}
	
	private void setDirection () {
		direction = 0;
		//we let agents not to move 
		direction = (int)Math.floor(Math.random() * 4);
	}
	
	public void step() {
		setDirection();
		int newX = x;
		int newY = y;

		switch (direction) {
			case 0:
				newY +=1;
				break;
			case 1:
				newX +=1;
				break;
			case 2:
				newY -=1;
				break;
			case 3:
				newX -=1;
				break;
			default:
				break;					
		}
		Object2DGrid grid = grassSpace.getCurrentGrassSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();
		if (tryMove(newX, newY)) {
			energy += grassSpace.takeGrassAt(x, y) * ENERGYEARNT;
		}
		else {
			setDirection();
		}
		energy--;
		
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public String getID() {
		return "A-" + ID;
	}
	
	public int getEnergy() {
		return energy;
	}
	
	public void giveBirth(int e) {
		energy -= e;
	}
	
	public void report() {
		//System.out.println( getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy. ");
	}
	
	private boolean tryMove (int newX, int newY) {
		return grassSpace.moveRabbitsAt(x, y, newX, newY );
	}
}
