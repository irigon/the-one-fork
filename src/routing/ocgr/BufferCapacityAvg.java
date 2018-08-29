package routing.ocgr;

import java.util.ArrayList;
import java.util.List;

import core.SimClock;
import routing.cgr.Vertex;

public class BufferCapacityAvg extends Prediction {
	
	List<Double> last_values;
	int count;
	int l_size;
	double mean;
	double curr_capacity;
	

	public BufferCapacityAvg(Vertex v, int size) {
		super(v);
		last_values = new ArrayList<Double>();
		count = 0;
		// TODO: iri -- initiate a list with size N
		while (count < size) {
			count += 1;
			last_values.add(0.0);
		}
		count = 0;
		mean = 0.0;
		setName();
	}

	@Override
	public void update() {
		count = count + 1;
		if (count < last_values.size()) {
			l_size = count;
		} else {
			l_size = last_values.size();
		int i = 0;
		while (i < l_size) {
				setValue(getValue() + (getValue() - curr_capacity)/count);
			}
		}
	}

	@Override
	protected void setName() {
		super.setName("BufferCapacityAvg" + last_values.size());
	}

	@Override
	public void connUp() {
		getVertex().set_adjusted_begin(SimClock.getTime());
	}

	// updating capacity when the contact goes down.
	// TODO: iri Would it be better to update on each transmission?
	// TODO: use a circular list and avoid to create and delete elements every time
	@Override
	public void connDown() {
//		getVertex().set_end(SimClock.getTime());
//		curr_capacity = getVertex().current_capacity();
//		if (count < last_values.size()) {
//			last_values.set(count, curr_capacity);
//		} else {
//			last_values.remove(0);
//			last_values.add(curr_capacity);
//		}
//		update();
		// NEED TO THINK BETTER ABOUT IT
	}
}
