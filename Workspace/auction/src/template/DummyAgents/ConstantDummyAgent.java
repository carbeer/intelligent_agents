package template.DummyAgents;

import logist.task.Task;

/**
 * Dummy agent, bidding exactly the same in every round.
 */
@SuppressWarnings("unused")
public class ConstantDummyAgent extends DummyAgent{

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
    }

    @Override
    public Long askPrice(Task task) {
        return (long) 1500;
    }

}
