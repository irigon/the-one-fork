package routing.ocgr.metrics;

import java.util.List;

import core.DTNHost;
import routing.ocgr.Vertex;
import routing.ocgr.util;

public class BufferFreeCapacityPrediction extends Prediction {
	
	double WEIGHT = 0.2;

	public BufferFreeCapacityPrediction(Vertex v) {
		super(v);
		setName();
		setValue(-1.0);
	}

	// it would be better to update at every transmission / message reception
	@Override
	public void update() {
		double virtualCapacity= getVertex().buffer_free_capacity();
		if (getValue() != -1.0) {
			virtualCapacity = getValue()*(1-WEIGHT) + virtualCapacity*WEIGHT;
		}
		setValue(util.round(virtualCapacity, 2));
		setTimestamp();
	}

	@Override
	protected void setName() {
		super.setName("BufferFreeCapacityPrediction");
	}

	@Override
	public void connUp() {
		update();
	}

	@Override
	public void connDown() {
	}
}
