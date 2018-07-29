package routing.cgr;

import java.util.Arrays;
import java.util.List;

import core.DTNHost;
import core.NetworkInterface;

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
	private String cid;
	private int transmission_speed;
	private int hash;
	
	public Contact (DTNHost a, DTNHost b, double contactStart, double contactEnd) {
		/* Order the hosts alphabetically */
		if (a.compareTo(b) < 0){
			host_a = a;
			host_b = b;
		} else {
			host_a = b;
			host_b = a;
		}
		
		cid = "cid_" + host_a + "_" + host_b + "_" + (int)contactStart + "_" + 0;
		
		begin = contactStart;
		adjusted_begin = begin;
		end = contactEnd;
		transmission_speed = calculate_transmission_speed(a, b);
		hash = 0;
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
	 * Find out the transmission speed between host x and y in this contact
	 * Goes through every interface and gets the first pair of interfaces of the 
	 * same type (compatible) that have a range wide enough to start a communication
	 * 
	 * @param x DTNHost partner
	 * @param y DTNHost partner
	 * @return the transmission speed of this contact, 0 if no appropriate interface was found
	 */
	private int calculate_transmission_speed(DTNHost x, DTNHost y) {
		List<NetworkInterface> lna = x.getInterfaces();
		List<NetworkInterface> lnb = y.getInterfaces();
		
		int transmission_speed = 0;
		double distance = x.getLocation().distance(y.getLocation());
		for (NetworkInterface nai : lna) {
			double x_radio_range = nai.getTransmitRange();
			for (NetworkInterface nbi : lnb) {
				if (nbi.getInterfaceType() != nai.getInterfaceType()) {
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
	
	public double get_current_capacity() {
		return (end - adjusted_begin) * transmission_speed;
	}
	
	public String get_id() {
		return cid;
	}
	
	public DTNHost get_other_host(DTNHost x) {
		if (x == host_a) {
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
