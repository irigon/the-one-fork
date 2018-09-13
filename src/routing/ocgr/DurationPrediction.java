package routing.ocgr;

import java.util.ArrayList;
import java.util.List;

import core.DTNHost;
import core.SimClock;
import routing.cgr.Vertex;

public class DurationPrediction extends Prediction {
	
	double WEIGHT = 0.2;
	double init;
	double end;

	public DurationPrediction(Vertex v) {
		super(v);
		setName();
		init = Double.NaN;
		end = Double.NaN;
	}

	// it would be better to update at every transmission / message reception
	@Override
	public void update() {
		double cap = util.round(getValue()*(1-WEIGHT) + (end-init)*WEIGHT, 2);
		setValue(cap);
	}

	@Override
	protected void setName() {
		super.setName("DurationPrediction");
	}

	@Override
	public void connUp() {
		init = SimClock.getTime();
		getVertex().set_begin(init);
		getVertex().set_adjusted_begin(init);
		getVertex().set_end(init + getValue());
	}

	@Override
	public void connDown() {
		double tmp = end;
		end = SimClock.getTime();
		if (tmp == Double.NaN) {
			setValue(end-init);
		} else {
			update();
		}
	}
}
