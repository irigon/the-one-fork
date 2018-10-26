package routing.ocgr;

import java.util.List;

import core.DTNHost;
import routing.MessageRouter;
import routing.OCGRRouter;
import routing.ocgr.metrics.Capacity;
import routing.ocgr.metrics.Metrics;
import routing.ocgr.metrics.Prediction;

public class Vertex {
	
	private String vid;
	private Contact contact;
	private boolean is_pivot;
	/** TODO start variables for OCGR **/
	// predicted capacity given the resource that were already allocated for this vertex
	private Metrics metrics;
	/** Ends variables for ocgr **/

	public Vertex(String id, Contact c, boolean pivot) {
		vid = id;
		contact = c;
		is_pivot = pivot;
	}

	public Vertex(String id, Contact c, Metrics m, boolean pivot) {
		vid = id;
		contact = c;
		metrics = m;
		is_pivot = pivot;
		m.init_vertice(this);
	}
	
	public Vertex(Vertex v) {
		vid = v.vid;
		contact = v.contact;
		is_pivot = v.is_pivot;
		metrics = v.metrics;
	}
	
	public Vertex(Vertex v, double start, double end) {
		List<DTNHost> hl = v.get_hosts();
		contact = new Contact(hl.get(0), hl.get(1), start, end);
		vid = "vertex_" +  contact.get_id();
		is_pivot = v.is_pivot();
	}
	
	public Metrics get_metrics() {
		return metrics;
	}
	
	/**
	 * This is not a deep_copy clone
	 * Hosts are still passed by address
	 * 
	 * @return a vertice with the same fields
	 */
	public Vertex hybrid_clone () {
		Metrics m = Metrics.create_metrics();
		return new Vertex(get_id(), new Contact(contact), m, false);
	}
	
	public void update_caps() {
		for (Capacity cap : metrics.getCapMap().values()) {
			cap.update();
		}
	}
	
	/**
	 * [1] Update predictions for the current contact
	 */
	public void connUp() {
		/* [1]  Update predictions for the current contact */
		for (Prediction p : metrics.getPredictions().values()) {
			p.connUp();
		}
	}

	public void connDown() {
		/* [1]  Update predictions for the current contact */
		for (Prediction p : metrics.getPredictions().values()) {
			p.connDown();
		}
	}

	
	/**
	 * Compares the timestamp 
	 * if ov has a newer version, copy predictions
	 * @param pv peer Vertice
	 */
	public void updatePreds(Vertex pv) {
		metrics.transitiveUpdate(pv);
	}
	
	public String get_id() {
		return vid;
	}
	
	public int get_transmission_speed() {
		return contact.get_transmission_speed();
	}
	
	public boolean is_pivot() {
		return is_pivot;
	}
	
	public List<DTNHost> get_hosts(){
		return contact.get_hosts();
	}
	
	public double begin() {
		return contact.begin();
	}
	
	public double end() {
		return contact.end();
	}
	
	public Vertex replicate() {
		return new Vertex(this);
	}
	
	public double current_capacity() {
		return contact.get_current_capacity();
	}
	
	/* Buffer free capacity is the minimal residual capacity among the buffers in this contact */
	public double buffer_free_capacity() {
		double bufferA = get_hosts().get(0).getRouter().getFreeBufferSize();
		double bufferB = get_hosts().get(1).getRouter().getFreeBufferSize();
		return Math.min(bufferA, bufferB);
	}

	public double adjusted_begin() {
		return contact.adjusted_begin();
	}
	
	public DTNHost get_common_host(Vertex x) {
		for (DTNHost h : get_hosts()) {
			if (x.get_hosts().contains(h)) {
				return h;
			}
		}
		return null;
	}
	
	public DTNHost get_other_host(DTNHost x) {
		return contact.get_other_host(x);
	}
	
	public void set_begin(double new_begin) {
		this.contact.set_begin(new_begin);
	}
	
	public void set_adjusted_begin(double new_begin) {
		this.contact.set_adjusted_begin(new_begin);
	}
	
	public void set_end(double new_end) {
		this.contact.set_end(new_end);
	}
	
	public double predicted_free_capacity() {
		double pred_trans_cap = metrics.getPredictions().get("DurationPrediction").getValue() *
				get_transmission_speed();
		return Math.min(buffer_free_capacity(), pred_trans_cap);
	}
	public double pred_time_between_contacts() {
		return this.metrics.getPredictions().get("AvgTimeBetweenContactsPred").getValue(); 
	}
	
	/**
	 * ECC - Estimated Contact Capactiy
	 * "Consume" part of the path.
	 * We want to avoid more data to be planned to a path than the vertices in this path can handle.
	 * A vertice can handle a certain amount of data depending on the transmission speed, contact and buffer size.
	 * Knowing the average transmission speed, contact and buffer size we calculate the amount of data to be processed in average
	 * between two encounters. With the average frequency of encounters we decide not to overlap a specific amount of data in a time span 
	 * equal to the average encounter frequency.
	 * 
	 * So, everytime a path is searched, we add to the vertices the timestamp and amount of data planned.
	 * The sum of these amounts should not overlapp the average within the timespan of the average time between two encounters.
	 *  
     * On the initailization of the search function, old data is removed, letting the sum equal to the available space in each contact opportunity
	 * @param time
	 * @param size
	 */
	public void add_data(double time, int size) {
		if (! is_pivot) {
			metrics.ecc().add_data(time, size);
		}
	}
	
	/**
	 * Before a path is searched using dijkstra, we exclude the size of messages sent long ago.
	 * Long enough : greater than the average time between contact predictions
	 * 
	 */
	public void update_ecc() {
		if (! is_pivot) {
		 metrics.ecc().cleanup();
		}
	}
	
	@Override
	public String toString() {
		List<DTNHost>hl = get_hosts();
		return this.get_id() + " [" + hl.get(0) + ", " + hl.get(1) + "] ";
	}
	
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((vid == null) ? 0 : vid.hashCode());
        return result;
    }
    
    /**
     * warning! this equals method does not consider the residual capacity
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vertex other = (Vertex) obj;
        if (vid == null) {
            if (other.vid != null)
                return false;
        } else if (!vid.equals(vid))
            return false;

        return contact.equals(other.contact);
    }
}
