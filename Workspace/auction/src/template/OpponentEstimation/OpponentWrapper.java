package template.OpponentEstimation;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import template.Configuration;

import java.util.List;

/**
 * Wrapper, that allows to easily interchange opponent strategies
 */

public class OpponentWrapper extends Opponent {
    Opponent opp;
    public OpponentWrapper(Topology topology, List<Vehicle> vehicles) {
        switch (Configuration.ESTIMATION) {
            case MIXED:
                opp = new MixedStrategyOpponent(topology, vehicles);
                break;
            case LINEAR_REG:
                opp = new LinearRegressionOpponent();
                break;
            case SLS:
                opp = new SLSOpponent(topology, vehicles);
                break;
            case MOVING_AVG:
                opp = new MovingAverageOpponent();
                break;
            case MOVING_MED:
                opp = new MovingMedianOpponent();
                break;
            default:
                System.out.println("WARN: No strategy for opponent estimation selected");
        }
    }

    @Override
    public long estimateBid(Task t, long timeout) {
        return opp.estimateBid(t, timeout);
    }

    @Override
    public void auctionFeedback(Task previous, long realBid, boolean won) {
        opp.auctionFeedback(previous, realBid, won);
    }

    @Override
    public double getCurrentAvgError() {
        return opp.getCurrentAvgError();
    }

    @Override
    public double getLastError() { return opp.getLastError(); }

}
