package routing.ocgr.metrics;

import core.SimClock;
import routing.ocgr.Vertex;
import routing.ocgr.util;

public class DurationPrediction extends Prediction {
	
	double WEIGHT = 0.2;
	double init;
	double end;
	boolean startup;

	public DurationPrediction(Vertex v) {
		super(v);
		setName();
		init = Double.NaN;
		end = Double.NaN;
		startup = true;
	}

	// it would be better to update at every transmission / message reception
	// setEnd is necessary since the contacts starts with end == 0. We need to change the contact size 
	// so the contact size can be used to verify the capacity for dijkstra.
	@Override
	public void update() {
		double duration;
		if (startup) {
			duration = util.round(end-init, 2);
			startup = false;
		} else {
			duration = util.round(getValue()*(1-WEIGHT) + (end-init)*WEIGHT, 2);
		}
		setValue(duration);
		super.setEnd(duration);
		setTimestamp();
	}

	@Override
	protected void setName() {
		super.setName("DurationPrediction");
	}

	@Override
	public void connUp() {
		init = SimClock.getTime();
		init = now();
		end = init + getValue();
	}
	
	public double now() {
		return SimClock.getTime();
	}

	@Override
	public void connDown() {
		double tmp = end;
		end = now();
		if (tmp == Double.NaN) {
			setValue(end-init);
		} else {
			update();
		}
	}
}
