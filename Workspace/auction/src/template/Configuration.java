package template;

public class Configuration {
    // Potential TODO: Implement and evaluate FROM_LAST_BEST
    public enum INIT_SOLUTION {ALL_TO_BIGGEST, EACH_TO_CLOSEST}
    public enum OPPONENT_STRATEGY {LINEAR_REG, MIXED, MOVING_AVG, MOVING_MED, SLS}

    //---------------------------------------------
    // CONFIGURATION
    //---------------------------------------------

    // OpponentWrapper
    public static final OPPONENT_STRATEGY ESTIMATION = OPPONENT_STRATEGY.MIXED;

    // MixedStrategyAgent
    public static final double INIT_LEARNING_RATE = 0.6;
    public static final double LAZINESS = 0.9;

    // MovingAverageOpponent
    public static final int AVG_WINDOW = 5;

    // MovingMedianOpponent
    public static final int MEDIAN_WINDOW = 5;

    // SLSOpponent
    public static final double BID_COST_SHARE_OPP = 1.2;
    public static final int MIN_VEHICLES = 2;
    public static final int MAX_VEHICLES = 5;
    public static int INSTANCES = 10;

    // SLS
    public static final INIT_SOLUTION INIT_STRATEGY = INIT_SOLUTION.EACH_TO_CLOSEST;

    // AuctionAgent
    public static final double BID_COST_SHARE_AGENT = 1.1;
    public static final long MIN_BID = 100;
    public static final double OPPONENT_WEIGHT = 0.5;
}
