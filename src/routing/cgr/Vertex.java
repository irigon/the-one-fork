package routing.cgr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import routing.OCGRRouter;
import routing.ocgr.AvgTimeBetweenContactsPrediction;
import routing.ocgr.BufferFreeCapacityPrediction;
import routing.ocgr.BufferSizeCapacity;
import routing.ocgr.Capacity;
import routing.ocgr.DurationPrediction;
import routing.ocgr.Metrics;
import routing.ocgr.Prediction;
import routing.ocgr.TransmissionSpeed;

public class Vertex {
	
	private String vid;
	private Contact contact;
	private boolean is_pivot;
	/** TODO start variables for OCGR **/
	// predicted capacity given the resource that were already allocated for this vertex
	Map<String, Prediction> preds;
	Map<String, Capacity> caps;
	private double pred_utilization;
	/** Ends variables for ocgr **/

	public Vertex(String id, Contact c, boolean pivot) {
		vid = id;
		contact = c;
		is_pivot = pivot;
		init_caps();
		init_preds();
	}
	
	public Vertex(Vertex v) {
		vid = v.vid;
		contact = v.contact;
		is_pivot = v.is_pivot;
		init_caps();
		init_preds();
	}
	
	public Vertex(Vertex v, double start, double end) {
		List<DTNHost> hl = v.get_hosts();
		contact = new Contact(hl.get(0), hl.get(1), start, end);
		vid = "vertex_" +  contact.get_id();
		is_pivot = v.is_pivot();
		init_caps();
		init_preds();
	}
	
	void init_caps() {
		caps = new HashMap<String, Capacity>();
		addCapacity(new BufferSizeCapacity(this));
		addCapacity(new TransmissionSpeed(this));
	}

	void init_preds() {
		preds = new HashMap<String, Prediction>();
		addPrediction(new BufferFreeCapacityPrediction(this));
		addPrediction(new AvgTimeBetweenContactsPrediction(this));
		addPrediction(new DurationPrediction(this));
	}
	
	void addPrediction(Prediction p) {
		preds.put(p.getName(), p);	
	}
	
	void addCapacity(Capacity c) {
		caps.put(c.getName(), c);	
	}
	
	public void update_caps() {
		for (Capacity cap : caps.values()) {
			cap.update();
		}
	}
	
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
	
	public void set_pred_utilization() {
		double pred_trans_cap = preds.get("DurationPrediction").getValue()*get_transmission_speed();
		pred_utilization =  Math.min(buffer_free_capacity(), pred_trans_cap);
	}
	
	public double predicted_free_capacity() {
		return pred_utilization;
	}

	public double virtual_frequency() {
		return this.preds.get("AvgTimeBetweenContactsPred").getValue();
	}

	public Map<String, Prediction> get_preds(){
		return preds;
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
