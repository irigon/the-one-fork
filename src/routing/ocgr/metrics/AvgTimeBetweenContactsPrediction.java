package routing.ocgr.metrics;

import core.SimClock;
import routing.ocgr.Vertex;
import routing.ocgr.util;

public class AvgTimeBetweenContactsPrediction extends Prediction {

	double last_contact;
	int counter;
	int MAX;
	public AvgTimeBetweenContactsPrediction(Vertex v) {
		super(v);
		setName();
		last_contact = -1.0;
		counter=0;
		setValue(-1.0);
		MAX = 10;
	}

	@Override
	public void update() {
		double now = SimClock.getTime();
		double time_between_encounters = now - last_contact;
		last_contact = now;
		// initialization
		if (getValue() == -1.0) {
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
		if (last_contact == -1.0) {
			last_contact = SimClock.getTime();
		} else {
			update();
		}
	}

	@Override
	public void connDown() {}
}
