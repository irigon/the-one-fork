package routing.ocgr.metrics;

import core.SimClock;
import routing.ocgr.Vertex;
import routing.ocgr.util;

public class AvgTimeBetweenContactsPrediction extends Prediction {

	double last_contact;
	int counter;
	int MAX;
	final double INI_VAL = Double.POSITIVE_INFINITY;
	public AvgTimeBetweenContactsPrediction(Vertex v) {
		super(v);
		setName();
		last_contact = INI_VAL;
		counter=0;
		setValue(INI_VAL);
		MAX = 10;
	}

	@Override
	public void update() {
		double now = SimClock.getTime();
		double time_between_encounters = now - last_contact;
		last_contact = now;
		// initialization
		if (getValue() == INI_VAL) {
			setValue(util.round(time_between_encounters, 2));
		} else {
			// incremental mean
			double oldValue = getValue();
			setValue(util.round(oldValue + (time_between_encounters - oldValue) / counter, 2));
		}
		setTimestamp();
	}

	@Override
	protected void setName() {
		super.setName("AvgTimeBetweenContactsPred");
	}
	
	@Override
	public void connUp() {
		counter = counter < MAX ? ++counter : MAX;
		if (last_contact == INI_VAL) {
			last_contact = SimClock.getTime();
		} else {
			update();
		}
	}

	@Override
	public void connDown() {}
}
