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
	}

	// it would be better to update at every transmission / message reception
	@Override
	public void update() {
		List<DTNHost> hosts = getVertex().get_hosts();
		double h1FreeBSize = hosts.get(0).getRouter().getFreeBufferSize();
		double h2FreeBSize = hosts.get(1).getRouter().getFreeBufferSize();
		double virtualCapacity = util.round(getValue()*(1-WEIGHT) + Math.min(h1FreeBSize, h2FreeBSize)*WEIGHT, 2);
		setValue(virtualCapacity);
		setTimestamp();
	}

	@Override
	protected void setName() {
		super.setName("BufferFreeCapacityPrediction");
	}

	@Override
	public void connUp() {
	}

	@Override
	public void connDown() {
		update();
	}
}
