package routing.ocgr;

import core.SimClock;
import routing.cgr.ContactPlanHandler;
import routing.cgr.Vertex;

public class AvgTimeBetweenContactsPrediction extends Prediction {

	double last_contact;
	int counter;
	public AvgTimeBetweenContactsPrediction(Vertex v) {
		super(v);
		setName();
		last_contact = -1.0;
		counter=0;
		setValue(-1.0);
	}

	@Override
	public void update() {
		double time_between_encounters = SimClock.getTime() - last_contact;
		last_contact = SimClock.getTime();
		// initialization
		if (getValue() == -1.0) {
			setValue(ContactPlanHandler.round(time_between_encounters, 2));
		} else {
			// incremental mean
			setValue(ContactPlanHandler.round(getValue() + (getValue() - time_between_encounters) / counter, 2));			
		}
	}

	@Override
	protected void setName() {
		super.setName("AvgTimeBetweenContactsPred");
	}
	
	@Override
	public void connUp() {
		counter++;
		if (last_contact == -1.0) {
			last_contact = SimClock.getTime();
		} else {
			update();
		}
	}

	@Override
	public void connDown() {}
}
