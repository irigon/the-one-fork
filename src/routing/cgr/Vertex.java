package routing.cgr;

import java.util.List;

import core.DTNHost;
import util.Tuple;

public class Vertex {
	
	private String vid;
	private Contact contact;
	private boolean is_pivot;
	private DTNHost sender;
	private DTNHost receiver;

	public Vertex(String id, Contact c, boolean pivot) {
		vid = id;
		contact = c;
		is_pivot = pivot;
		sender = null;
		receiver = null;
	}
	
	public Vertex(Vertex v) {
		vid = v.vid;
		contact = v.contact;
		is_pivot = v.is_pivot;
		sender = v.sender;
		receiver = v.receiver;
	}
	
	public Vertex(Vertex v, double start, double end) {
		DTNHost a = (DTNHost) (v.get_hosts().getKey());
		DTNHost b = (DTNHost) (v.get_hosts().getValue());
		contact = new Contact(a, b, start, end);
		vid = v.get_id() +  "_" + start + "_" + end;
		is_pivot = v.is_pivot();
		sender = v.sender;
		receiver = v.receiver;
	}
	
	public String get_id() {
		return vid;
	}
	
	public DTNHost get_sender() {
		return sender;
	}
	
	public DTNHost get_receiver() {
		return receiver;
	}
	
	public void set_sender(DTNHost h) {
		receiver = contact.get_other_host(h);
		sender = h;
	}
	
	public void set_receiver(DTNHost h) {
		sender = contact.get_other_host(h);
		receiver = h;
	}
	
	public int get_transmission_speed() {
		return contact.get_transmission_speed();
	}
	
	public boolean is_pivot() {
		return is_pivot;
	}
	
	public Tuple get_hosts(){
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
		Tuple<DTNHost, DTNHost> xh = x.get_hosts();
		Tuple<DTNHost, DTNHost> ch = contact.get_hosts();
		if (xh.getKey().equals(ch.getKey()) || xh.getKey().equals(ch.getValue())){
			return xh.getKey();
		}
		if (xh.getValue().equals(ch.getKey()) || xh.getValue().equals(ch.getValue())){
			return xh.getValue();
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
		return this.get_id() + " [" + this.get_hosts().getKey() + ", " + this.get_hosts().getValue() + "] ";
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
