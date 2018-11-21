package template.OpponentEstimation;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.awt.*;

/**
 * Dummy implementation to allow to instantiate objects of the interface logist.simulation.Vehicle
 */

class DummyVehicle implements Vehicle {

    int capacity;
    Topology.City homeCity;
    int costPerKm;

    public DummyVehicle(int costPerKm, Topology.City c, int capacity) {
        this.capacity = capacity;
        this.homeCity = c;
        this.costPerKm = costPerKm;
    }

    @Override
    public int id() {
        return 0;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public Topology.City homeCity() {
        return this.homeCity;
    }

    @Override
    public double speed() {
        return 0;
    }

    @Override
    public int costPerKm() {
        return this.costPerKm;
    }

    @Override
    public Topology.City getCurrentCity() {
        return this.homeCity;
    }

    @Override
    public TaskSet getCurrentTasks() {
        return null;
    }

    @Override
    public long getReward() {
        return 0;
    }

    @Override
    public long getDistanceUnits() {
        return 0;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public Color color() {
        return null;
    }
}
