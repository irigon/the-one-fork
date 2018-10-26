package routing.ocgr.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import core.SimClock;

/**
 * 
 * This metrics estimates the available capacity in a vertice (best case, i.e if there are no hosts concurrently using this contact)
 * Based on the amount of data sent to this contact, we can predict what is left in the best case (if no one else is also using it).
 * 
 * Needs:
 * 		a method to add the resource consumed in this vertice and its timestamp
 * 		a method to prune values older than timestamp
 * 		a method to calculate the current free capacity
 * 
 * @author jose
 *
 */
public class EstimatedContactCapacity {

	double free_amount;
	Prediction avgTimeBetweenContactsPred;
	Prediction durationPred;
	double transSpeed;
	SortedSet<Tuple> bundleSet;
	
	public EstimatedContactCapacity(Prediction avgTimeBetweenContactsPred, Prediction durationPred, double transSpeed) {	
		this.avgTimeBetweenContactsPred = avgTimeBetweenContactsPred;
		this.durationPred = durationPred;
		this.transSpeed = transSpeed;
		this.bundleSet = new TreeSet<>(Comparator.comparing(Tuple::get_timestamp));
	}
	
	public void add_data(double time, int size) {
		bundleSet.add(new Tuple(time, size));
	}
	
	public void cleanup() {
		if (avgTimeBetweenContactsPred == null) { return; }
		double time_between_contact = avgTimeBetweenContactsPred.getValue();
		double oldest_acceptable_tt = now() - avgTimeBetweenContactsPred.getValue();
		assert oldest_acceptable_tt > 0;
		List to_delete = new ArrayList();
		int sum = 0;
		for (Tuple t: bundleSet) {
			if (t.get_timestamp() > oldest_acceptable_tt) {
				sum+=t.get_size();
			} else {
				to_delete.add(t);
			}
		}
		bundleSet.removeAll(to_delete);
		free_amount = durationPred.getValue() * transSpeed - sum;
	}
	
	public double free() {
		return free_amount;
	}
	
	private double now() {
		return SimClock.getTime();
	}
	
	private class Tuple { 
		  private double timestamp; 
		  private double size; 
		  public Tuple(double timestamp, int size) { 
		    this.timestamp = timestamp; 
		    this.size = size; 
		  } 
		  public double get_timestamp() {
			  return this.timestamp;
 		  }
		  public double get_size() {
			  return this.size;
		  } 
	} 
	
	 
}
