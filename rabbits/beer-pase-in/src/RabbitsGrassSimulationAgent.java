import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	
	private static final int ENERGYEARNT = 3;
	private static final int INITENERGY = 10;
	
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

	// Random generator for rabbit movements (0, 1, 2 or 3)
	private void setDirection () {
		direction = (int)Math.floor(Math.random() * 4);
	}
	
	public void step() {
		setDirection();
		int newX = x;
		int newY = y;

		// Map direction value to coordinates
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

		// torus world
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		// Move the rabbit - if successful, the rabbit tries to eat grass afterwards
		if (tryMove(newX, newY)) {
			tryToEat();
		}
		
		// Decrement energy level of the rabbit, no matter whether it moved or not
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

	// Decrease remaining energy level if a rabbit gives birth
	public void giveBirth(int e) {
		energy -= e;
	}

	// Logger
	public void report() {
		//System.out.println( getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy. ");
	}

	// Try to move the rabbit. Passes over a boolean as return value that indicates, whether the movement was successful or not.
	private boolean tryMove (int newX, int newY) {
		return grassSpace.moveRabbitsAt(x, y, newX, newY );
	}

	// Rabbit tries to eat grass. Increase energy level if applicable
	public void tryToEat() {
		energy += grassSpace.takeGrassAt(x, y) * ENERGYEARNT;
	}
}
