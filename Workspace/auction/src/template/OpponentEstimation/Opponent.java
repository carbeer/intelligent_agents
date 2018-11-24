package template.OpponentEstimation;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import org.apache.commons.math.stat.descriptive.rank.Median;
import template.Configuration;
import template.SLS.SLS;

import java.util.*;
import java.util.stream.LongStream;

import static template.Configuration.*;

/**
 * This class contains a collection of Opponents that implement different estimation strategies
 */

public abstract class Opponent {
    ArrayList<Long> errors = new ArrayList<>();

    public abstract long estimateBid(Task t, float timeout);
	public abstract void auctionFeedback(Task previous, long realBid, boolean won);

	public double getCurrentAvgError() {
		return errors.stream().mapToDouble(a -> a).average().orElse(0.0);
	}
	public double getLastError() { return errors.get(errors.size()-1); }
}

/**
 * Uses different Opponents and combines them to compute a bid
 */
class MixedStrategyOpponent extends Opponent {
    ArrayList<Opponent> opponents = new ArrayList<>();
    HashMap<Opponent, Double> weight = new HashMap<Opponent, Double>();
    HashMap<Opponent, Long> lastEstimatorBid = new HashMap<Opponent, Long>();
    long lastBid = 0;
    double eta = INIT_LEARNING_RATE;

    public MixedStrategyOpponent(Topology topology, List<Vehicle> vehicles) {
        this.opponents.add(new SLSOpponent(topology, vehicles));
        this.opponents.add(new MovingAverageOpponent());
        this.opponents.add(new LinearRegressionOpponent());
        this.opponents.add(new MovingMedianOpponent());

        for (Opponent opp : opponents) {
            weight.put(opp, 1.0 / opponents.size());
        }
    }

    @Override
    public long estimateBid(Task t, float timeout) {
        this.lastBid = 0;
        for (Opponent opp : opponents) {
            long oppBid = opp.estimateBid(t, (float) (timeout * 4 / 5));
            this.lastBid += weight.get(opp) * oppBid;
            this.lastEstimatorBid.put(opp, oppBid);
            System.out.println(opp.getClass() + ": Suggested a bid of " + oppBid);
        }
        System.out.println("Final estimation: " + this.lastBid);
        return this.lastBid;
    }

    @Override
    public void auctionFeedback(Task previous, long realBid, boolean won) {
        double totalError = 0;

        for (Opponent opp : opponents) {
            opp.auctionFeedback(previous, realBid, won);
            totalError += opp.getLastError();
        }
        if (totalError > 0) {
            int numOpponents = opponents.size();

            // Update weights according to corresponding error
            for (Opponent opp : opponents) {
                double newWeight = weight.get(opp) - eta * (1.0 / numOpponents) * (opp.getLastError() / totalError - 1.0 / numOpponents);
                if (newWeight <= 0) {
                    // Workaround to avoid ConcurrentModificationException by the Logist platform
                    ArrayList<Opponent> tmp = new ArrayList<>();
                    for (Opponent opp2 : opponents) {
                        if (opp2 != opp) {
                            tmp.add(opp2);
                        }
                    }
                    this.opponents = tmp;
                    System.out.println("Dropping model: " + opp.getClass());
                }
                else {
                    System.out.printf("Updating weight of %s from %f to %f\n", opp.getClass(), weight.get(opp), newWeight);
                    weight.put(opp, newWeight);
                }
            }
        }
        eta = eta * LAZINESS;
        this.errors.add(Math.abs(lastBid - realBid));
    }
}

/**
 * Utilizes linear regression to calculate bids
 */
class LinearRegressionOpponent extends Opponent {
    double beta0 = 0;
    double beta1 = 0;
    ArrayList<Long> realBids = new ArrayList<>();

    public void fit() {
        int round = this.realBids.size();

        long[] y = Arrays.stream(realBids.toArray(new Long[round])).filter(Objects::nonNull)
                .mapToLong(Long::longValue).toArray();
        long[] x = LongStream.range(0, round).toArray();

        double sumx = LongStream.of(x).sum();
        double sumy = LongStream.of(y).sum();

        double xbar = sumx / round;
        double ybar = sumy / round;

        double xxbar = 0.0, xybar = 0.0;

        for (int i = 0; i < round; i++) {
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }

        this.beta1 = xybar / xxbar;
        this.beta0 = ybar - beta1 * xbar;
    }

    @Override
    public long estimateBid(Task t, float timeout) {
        return (long) (beta0 + beta1 * (realBids.size()+1));
    }

    @Override
    public void auctionFeedback(Task previous, long realBid, boolean won) {
        this.errors.add((long) Math.abs(estimateBid(null, 0) - realBid));
        this.realBids.add(realBid);
        this.fit();
    }
}

/**
 * Calculates the moving average over a timespan of AVG_WINDOW
 */
class MovingAverageOpponent extends Opponent {
    long lastRealBid = 0;
    long lastAverage = 0;



    // Used to build the exponential moving average
    static final double expFactor = 2.0 / (1 + AVG_WINDOW);

    @Override
    public long estimateBid(Task t, float timeout) {
        // For the second round
        if (this.lastAverage == 0 && this.lastRealBid != 0) {
            this.lastAverage = this.lastRealBid;
        }
        this.lastAverage = (long) (expFactor * lastRealBid  + (1-expFactor) * lastAverage);
        return this.lastAverage;
    }

    @Override
    public void auctionFeedback(Task previous, long realBid, boolean won) {
        this.lastRealBid = realBid;
        errors.add(Math.abs(lastAverage-lastRealBid));
    }
}

/**
 * Calculates the moving median over a timespan of MEDIAN_WINDOW
 */
class MovingMedianOpponent extends Opponent {
    Queue<Long> realBids = new ArrayDeque<>();
    long lastMedian = 0;
    // Used to build the exponential moving median
    static final double medianWindow = MEDIAN_WINDOW;
    Median median = new Median();

    @Override
    public long estimateBid(Task t, float timeout) {
        double[] window = Arrays.stream(realBids.toArray(new Long[0])).mapToDouble(num -> (double) num).toArray();
        this.lastMedian = (long) median.evaluate(window);
        return this.lastMedian;
    }

    @Override
    public void auctionFeedback(Task previous, long realBid, boolean won) {
        this.realBids.add(realBid);
        if (realBids.size() > medianWindow) {
            this.realBids.remove();
        }
        errors.add(Math.abs(lastMedian-realBid));
    }
}

/**
 * Opponent utilizing SLS strategy. We spin up several instances (to represent different configurations) and narrow
 * them down after each iteration until only a single SLS instance remains.
 */
class SLSOpponent extends Opponent {
    private List<SLSOpponentInstance> estimators = new ArrayList<>();
    static Random rand = new Random();
    SLSOpponentInstance best;
    long lastBid = 0;
    int instances = Configuration.INSTANCES;

    public SLSOpponent(Topology topology, List<Vehicle> vehicles) {
        int overallCapacity = 0;
        for (Vehicle v : vehicles) {
            overallCapacity += v.capacity();
        }
        for (int i=0; i<instances; i++) {
            estimators.add(new SLSOpponentInstance(topology, vehicles.get(0).costPerKm(), overallCapacity));
        }
    }

    @Override
    public long estimateBid(Task t, float timeout) {
        long estimatedCost = 0;
        long bid = -1;

        for (SLSOpponentInstance estimator : estimators) {
            long tmp = estimator.estimateBid(t, (float) ((double)timeout/instances));;
            estimatedCost += tmp * 1.0/instances;
            if (best != null && best == estimator) {
                bid = tmp;
            }
        }
        if (bid != -1) {
            lastBid = bid;
            return bid;
        }
        lastBid = estimatedCost;
        return estimatedCost;
    }

    @Override
    public void auctionFeedback(Task previous, long realBid, boolean won) {
        SLSOpponentInstance.setRealBid(realBid);
        if (won) {
            for (SLSOpponentInstance opp : estimators) {
                opp.lastEstimator = opp.potentialEstimator;
                opp.potentialEstimator = null;
            }
        } else {
            for (SLSOpponentInstance opp : estimators) {
                for (Task t : opp.taskList) {
                    if (previous.id == t.id) {
                        opp.taskList.remove(t);
                        break;
                    }
                }
            }
        }
        errors.add((long) Math.abs(lastBid - realBid));
        cullOpponentModels();
    }

    public void cullOpponentModels() {
        Collections.sort(this.estimators);
        instances -= instances / 2;
        this.estimators = this.estimators.subList(0, instances);
        this.best = this.estimators.get(0);
    }
}

/**
 * Represents a single SLSOpponent instance that is used within the SLSOpponent
 */
class SLSOpponentInstance implements Comparable<SLSOpponentInstance> {
    SLS lastEstimator;
    SLS potentialEstimator;
    ArrayList<Vehicle> dummyVehicles;
    long lastBid;
    Random rand;
    static long realBid;
    ArrayList<Task> taskList = new ArrayList<>();

    public SLSOpponentInstance(Topology topology, int costPerKm, int overallCapacity) {
        dummyVehicles = new ArrayList<>();
        this.rand = SLSOpponent.rand;
        double len = Configuration.MIN_VEHICLES + rand.nextInt(Configuration.MAX_VEHICLES- Configuration.MIN_VEHICLES);

        for (int j=0; j<len; j++) {
            dummyVehicles.add(new DummyVehicle(costPerKm, topology.randomCity(rand), (int) (overallCapacity / len)));
        }
        this.lastEstimator = new SLS(dummyVehicles, taskList, 0);
    }

    public long estimateBid(Task t, float timeout) {
        long now = System.currentTimeMillis();
        this.taskList.add(t);
        lastEstimator.setTimeouts(timeout);
        potentialEstimator = new SLS(dummyVehicles, taskList, timeout);
        double tmp = potentialEstimator.bestSolution.computeCost() - lastEstimator.bestSolution.computeCost();
        System.out.println("Cost: " + tmp);
        this.lastBid = (long) (tmp * Configuration.BID_COST_SHARE_OPP);
        return (long) (this.lastBid);
    }

    public Long getAbsError() {
        return Math.abs(this.lastBid - realBid);
    }

    @Override
    // Ascending order
    public int compareTo(SLSOpponentInstance o) {
        return this.getAbsError().compareTo(o.getAbsError());
    }

    public static void setRealBid(long realBid) {
        SLSOpponentInstance.realBid = realBid;
    }
}