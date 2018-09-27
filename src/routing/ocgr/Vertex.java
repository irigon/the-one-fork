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
	
//	void init_caps() {
//		caps = new HashMap<String, Capacity>();
//		addCapacity(new BufferSizeCapacity(this));
//		addCapacity(new TransmissionSpeed(this));
//	}
//
//	void init_preds() {
//		preds = new HashMap<String, Prediction>();
//		addPrediction(new BufferFreeCapacityPrediction(this));
//		addPrediction(new AvgTimeBetweenContactsPrediction(this));
//		addPrediction(new DurationPrediction(this));
//	}
//	
//	void addPrediction(Prediction p) {
//		preds.put(p.getName(), p);	
//	}
//	
//	void addCapacity(Capacity c) {
//		caps.put(c.getName(), c);	
//	}
//	
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
	 * 
	 * @param pv peer Vertice
	 */
	public void updatePreds(Vertex pv) {
		double remote_timestamp = pv.get_metrics().get_timestamp();
		if (get_metrics().get_timestamp() < remote_timestamp) {
			metrics.transitiveUpdate(pv);
			get_metrics().set_timestamp(remote_timestamp);
		}
	}
//	
//	public void update_preds (DTNHost otherHost) {
//		OCGRRouter otherRouter = (OCGRRouter)otherHost.getRouter();
//		extendVerticesAndEdgesToGraph(otherRouter);
//		Metrics otherMetrics = otherRouter.getMetrics();
//
//		for (Prediction p : getPredictionsFor(v).values()) {
//			p.connUp();
//		}
//
//	}
	
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
//	
//	public double predicted_free_capacity() {
//		return pred_utilization;
//	}
//
	public double virtual_frequency() {
		return this.metrics.getPredictions().get("AvgTimeBetweenContactsPred").getValue();
	}
//
//	public Map<String, Prediction> get_preds(){
//		return preds;
//	}
	
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
