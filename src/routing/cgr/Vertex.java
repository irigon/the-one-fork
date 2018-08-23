package routing.cgr;

import java.util.Arrays;
import java.util.List;

import core.DTNHost;
import util.Tuple;

public class Vertex {
	
	private String vid;
	private Contact contact;
	private boolean is_pivot;

	public Vertex(String id, Contact c, boolean pivot) {
		vid = id;
		contact = c;
		is_pivot = pivot;
	}
	
	public Vertex(Vertex v) {
		vid = v.vid;
		contact = v.contact;
		is_pivot = v.is_pivot;
	}
	
	public Vertex(Vertex v, double start, double end) {
		List<DTNHost> hl = v.get_hosts();
		contact = new Contact(hl.get(0), hl.get(1), start, end);
		vid = "vertex_" +  contact.get_id();
		is_pivot = v.is_pivot();
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
	
	public void set_adjusted_begin(double new_begin) {
		this.contact.set_adjusted_begin(new_begin);
	}
	
	public void set_end(double new_end) {
		this.contact.set_end(new_end);
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
