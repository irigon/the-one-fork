package routing.ocgr.metrics;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import core.SimClock;
import routing.ocgr.Vertex;

/**
 *Predicts the least latency a bundle may arrive at this node
 * 
 * If start is not set, calculate the best case based on the frequency and set start
 * If a path is chosen, forward a pointer according to the message size
 * 
 * If the start is set, verify if the message fits in one chunck
 *   in case positiv, the distance is the end of the last message
 *   otherwise return the begining of the next chunck
 * if the path is chosen, update the start accordingly and forward the pointer
 */
public class LeastLatencyPrediction extends Prediction {
	
	double start;
	double end_last_bundle;
	double tspeed;
	double max_timespan;
	public LeastLatencyPrediction(Vertex v) {
		super(v);
		setName();
		start = -1.0;
		end_last_bundle = -1.0;
		tspeed = getVertex().get_transmission_speed();
	}

	@Override
	public void update() {	}
	
	public void add_bundle(double size) {
		double trans_time = size/tspeed;
		if (end_last_bundle + trans_time > max_timespan) {
			contact_full();
		}
	}

	public void contact_full() {
		set_start(start + getVertex().pred_time_between_contacts());
	}
	
	public void set_start(double start) {
		this.start = start;
		end_last_bundle = start;
	}
	
	public void set_max_timespan(double time_span) {
		this.max_timespan = time_span;
	}
	
	@Override
	protected void setName() {
		super.setName("LocalViewCapacityPrediction");
	}

	@Override
	public void connUp() {
	}

	@Override
	public void connDown() {
	}
	
	
	private class Tuple implements Comparable<Tuple>{
		double timestamp;
		double value;
		public Tuple(double timestamp, double value) {
			this.timestamp = timestamp;
			this.value = value;
		}
		
		double get_timestamp() {
			return this.timestamp;
		}
		
		double get_value() {
			return this.value;
		}

		@Override
		public int compareTo(Tuple t2) {
			return Double.compare(get_timestamp(), t2.get_timestamp());
		}
	}
}
