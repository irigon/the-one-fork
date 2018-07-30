package routing.cgr;

import java.util.List;

import core.DTNHost;

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
	
	public Vertex(Vertex vertex) {
		vid = vertex.vid;
		contact = vertex.contact;
		is_pivot = vertex.is_pivot;
		sender = vertex.sender;
		receiver = vertex.receiver;
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
		List<DTNHost> x_hosts = x.get_hosts();
		for (DTNHost l : contact.get_hosts()) {
			if (x_hosts.contains(l)) {
				return l;
			}
		}
		return null;
	}
	
	public DTNHost get_other_host(DTNHost x) {
		return contact.get_other_host(x);
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
