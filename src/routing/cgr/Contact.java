package routing.cgr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import core.DTNHost;
import core.NetworkInterface;
import util.Tuple;

/*
 * A contact between two hosts: host_a and host_b
 */
public class Contact {

	
	private DTNHost host_a;
	private DTNHost host_b;
	private double begin;
	private double end;
	// Adjusted contact begin due to resource allocation
	private double adjusted_begin;
	private int transmission_speed;
	private String interface_type;
	private int hash;
	private String cid;

	public Contact (DTNHost a, DTNHost b, double contactStart, double contactEnd) {
		List<DTNHost> ordered = order(a,b);
		host_a = ordered.get(0);
		host_b = ordered.get(1);
		begin = contactStart;
		adjusted_begin = begin;
		end = contactEnd;
		transmission_speed = calculate_transmission_speed(a, b);
		hash = 0;
		cid = contact_id();
	}
	
	public Contact(Contact contact) {
		host_a = contact.host_a;
		host_b = contact.host_b;
		begin  = contact.begin;
		end    = contact.end;
		adjusted_begin = contact.adjusted_begin;
		cid = contact.cid;
		transmission_speed = contact.transmission_speed;
		hash = contact.hash;
	}

	/**
	 * Order a pair of contacts alphabetically by name
	 * @param a DTNHost a
	 * @param b DTNHost b
	 * @return a list {a,b} ordered by address
	 */
	private List<DTNHost> order(DTNHost a, DTNHost b){
		List<DTNHost> hs = new ArrayList<DTNHost>(Arrays.asList(a, b));
		Collections.sort(hs);
		return hs;
	}

	/**
	 * Find out the transmission speed between host x and y in this contact
	 * Goes through every interface and gets the first pair of interfaces of the 
	 * same type (compatible) that have a range wide enough to start a communication
	 * 
	 * WARNING: this function will just return the right value if this function is called
	 * when both nodes in contact at the time when the method is called (on the fly)
	 * 
	 * @param x DTNHost partner
	 * @param y DTNHost partner
	 * @return the transmission speed of this contact, 0 if no appropriate interface was found
	 */
	public int calculate_transmission_speed(DTNHost x, DTNHost y) {
		List<NetworkInterface> lna = x.getInterfaces();
		List<NetworkInterface> lnb = y.getInterfaces();
		
		int transmission_speed = 0;
		double distance = x.getLocation().distance(y.getLocation());
		for (NetworkInterface nai : lna) {
			double x_radio_range = nai.getTransmitRange();
			for (NetworkInterface nbi : lnb) {
				if (!nbi.getInterfaceType().equals(nai.getInterfaceType())) {
					continue;
				}
				double y_radio_range = nbi.getTransmitRange();
				if (Math.min(x_radio_range, y_radio_range) > distance) {
					transmission_speed = nai.getTransmitSpeed(nbi);
				}
			}
		}
		return transmission_speed;
	}
	
	public int get_transmission_speed() {
		return transmission_speed;
	}
	
	public void set_transmission_speed(int ts) {
		transmission_speed = ts;
	}
	
	public double get_current_capacity() {
		return (end - adjusted_begin) * transmission_speed;
	}
	
	public String get_id() {
		return cid;
	}
	
	/**
	 * Generates an id for this contact
	 * <p>
	 * We use adjusted begin instead of begin, so that if the contact is updated
	 * and a new contact is generated from it (for example on overlapping contacts)
	 * the new generated will have a different id from the original one.
	 * 
	 * @return Generated id
	 */
	public String contact_id() {
		return pair_id() + "_" + adjusted_begin + "_" + end;
	}
	
	/*TODO: do not use getString, but get_address() instead*/
	public String pair_id() {
		return host_a.getAddress() + "_" + host_b.getAddress();
	}
	
	public DTNHost get_other_host(DTNHost x) {
		if (x.equals(host_a)) {
			return host_b;
		}
		return host_a;
	}
	
	public List<DTNHost> get_hosts(){
		return Arrays.asList(host_a, host_b);
	}
		
	public double begin() {
		return begin;
	}
	
	public double adjusted_begin () {
		return adjusted_begin;
	}

	public void set_adjusted_begin(double new_begin) {
		this.adjusted_begin = new_begin;
	}

	public void set_end(double new_end) {
		this.end = new_end;
	}
	
	public double end() {
		return end;
	}
	
    @Override
    public int hashCode() {
        if (hash == 0)
            hash = cid.hashCode();

        return hash;
    }

    /**
     * warning this equals implementation does not consider the residual capacity
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

        Contact c = (Contact) obj;
        return hashCode() == c.hashCode();
    }
}
