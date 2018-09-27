import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {	
	
	// Default Values
	private static final int NUMRABBITS = 50;
	private static final int GRIDXSIZE = 20;
	private static final int GRIDYSIZE = 20;
	private static final int GRASSRATE = 1;
	private static final int BIRTHTHRESHOLD = 15;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace grassSpace;
	private DisplaySurface displaySurf;
	private ArrayList rabbitsList;
	private OpenSequenceGraph amountGrassInSpace;
	private OpenSequenceGraph amountRabbitsInSpace;
	
	private int numRabbits = NUMRABBITS;
	private int gridXSize = GRIDXSIZE;
	private int gridYSize = GRIDYSIZE;
	private int grassRate = GRASSRATE;
	private int birthThreshold = BIRTHTHRESHOLD;

	
	public static void main(String[] args) {
			
		System.out.println("Rabbit skeleton");
		SimInit init = new SimInit();
	    RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
	    init.loadModel(model, "", false);
			
	}
	
	class grassInSpace implements DataSource, Sequence {
		public Object execute() {
			return new Double(getSValue());
		}
		public double getSValue() {
			return (double)grassSpace.getTotalGrass();
		}
	}
	
	class rabbitsInSpace implements DataSource, Sequence {
		public Object execute() {
			return new Double(getSValue());
		}
		public double getSValue() {
			return (double)grassSpace.getTotalRabbits();
		}
	}
	

	public void setup() {
		System.out.println("Running setup");
		grassSpace = null;
		rabbitsList = new ArrayList();
		schedule = new Schedule(1);
		
		//Tear Down displays
		if (displaySurf != null) {
			displaySurf.dispose();
		}
		displaySurf = null;
		
		if (amountGrassInSpace != null){
			amountGrassInSpace.dispose();
		}
		amountGrassInSpace = null;
		
		if (amountRabbitsInSpace != null){
			amountRabbitsInSpace.dispose();
		}
		amountRabbitsInSpace = null;
		
		//create display
		displaySurf = new DisplaySurface(this, "Rabbit Grass Simulation 1");
		amountGrassInSpace = new OpenSequenceGraph("Amount of Grass in Space", this);
		amountRabbitsInSpace = new OpenSequenceGraph("Amounts of Rabbits in Space", this);
		
		//register display
        registerDisplaySurface("Rabbit Grass Simulation 1", displaySurf);
        this.registerMediaProducer("Plot0", amountGrassInSpace);
        this.registerMediaProducer("Plot1", amountRabbitsInSpace);

	}

	public void begin() {
		System.out.println("Running begin");
		buildModel();
	    buildSchedule();
	    buildDisplay();

	    displaySurf.display();
	    amountGrassInSpace.display();
	    amountRabbitsInSpace.display();

		
	}
	
	public void buildModel() {
		System.out.println("Running BuildModel");
	    grassSpace = new RabbitsGrassSimulationSpace (gridXSize, gridYSize);
	    //grassSpace.spreadGrass(grassRate);
	    
	    for (int i = 0; i < numRabbits; i++) {
	    	addNewRabbits();
	    }
	    for (int i = 0; i < rabbitsList.size(); i++) {
	    	RabbitsGrassSimulationAgent ra = (RabbitsGrassSimulationAgent)rabbitsList.get(i);
	    	ra.report();
	    }
	}
	
	public void buildSchedule() {
		System.out.println("Running BuildSchedule");
		
		class SimulationStep extends BasicAction{
			public void execute() {
				grassSpace.spreadGrass(grassRate);
				SimUtilities.shuffle(rabbitsList);
				for (int i=0; i < rabbitsList.size(); i++) {
					RabbitsGrassSimulationAgent rsa = (RabbitsGrassSimulationAgent)rabbitsList.get(i);					
					if (rsa.getEnergy() > BIRTHTHRESHOLD) {						
						addNewRabbits();						
						rsa.giveBirth((int)(BIRTHTHRESHOLD * 2 / 3) );						
					}
					
					rsa.step();
				}
				int deadRAbbits = cleanDeadRabbits();
				displaySurf.updateDisplay();
			}
		}
		
		schedule.scheduleActionBeginning(0, new SimulationStep());
		
		
		class updateGrassInSpace extends BasicAction{
			public void execute(){
				amountGrassInSpace.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new updateGrassInSpace());
		
		class updateRabbitsInSpace extends BasicAction{
			public void execute(){
				amountRabbitsInSpace.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new updateRabbitsInSpace());
	}
	

	
	public void buildDisplay() {
		
		System.out.println("Running BuildDisplay");

	    ColorMap map = new ColorMap();

	    
	    map.mapColor(1, new Color(0, 170, 0));
	    
	    map.mapColor(0, new Color(0, 239, 255));

	    Value2DDisplay displayGrass = new Value2DDisplay(grassSpace.getCurrentGrassSpace(), map);
	    
	    Object2DDisplay displayRabbits = new Object2DDisplay(grassSpace.getCurrentRabbitsSpace());
	    displayRabbits.setObjectList(rabbitsList);
	    
	    
	    displaySurf.addDisplayableProbeable(displayGrass, "Garden");
	    displaySurf.addDisplayableProbeable(displayRabbits, "Rabbits");
	    
	    amountGrassInSpace.addSequence("Grass in Space", new grassInSpace ());
	    amountRabbitsInSpace.addSequence("Rabbits in Space", new rabbitsInSpace());
		
	}
	
	private void addNewRabbits () {
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent ();
		rabbitsList.add(a);
		grassSpace.addAgent(a);
	}
	
	private int cleanDeadRabbits(){
	    int count = 0;
	    for(int i = (rabbitsList.size() - 1); i >= 0 ; i--){
	    	RabbitsGrassSimulationAgent rsa = (RabbitsGrassSimulationAgent)rabbitsList.get(i);
	      if(rsa.getEnergy() < 1){
	        grassSpace.removeRabbitsAt(rsa.getX(), rsa.getY());
	        rabbitsList.remove(i);
	        count++;
	      }
	    }
	    return count;
	}
	
	public String[] getInitParam() {
		String [] initParams = { "numRabbits", "gridXSize", "gridYSize", "grassRate", "birthThreshold" };
		return initParams;
	}

	public String getName() {
		return "Rabbit";
	}

	public Schedule getSchedule() {
		return schedule;
	}
		
	public int getNumRabbits(){
	    return numRabbits;
	}

	public void setNumRabbits(int n){
		numRabbits = n;
	}
	public int getGridXSize(){
		return gridXSize;
	}

	public void setGridXSize(int x){
	    gridXSize = x;
	}

	public int getGridYSize(){
	    return gridYSize;
	}

	public void setGridYSize(int y){
		gridYSize = y;
	}
    
	public int getGrassRate() {
		return grassRate;
	}
	
	public void setGrassRate (int rate) {
		grassRate = rate;
	}
		 
	public int getBirthThreshold() {
		return birthThreshold;
	}
	
	public void setBirthThreshold(int t) {
		birthThreshold = t;
	}
	
	
}
