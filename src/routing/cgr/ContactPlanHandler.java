package routing.cgr;

import static org.junit.jupiter.api.Assumptions.assumingThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import core.DTNHost;
import core.SimClock;

public class ContactPlanHandler {

	static ContactPlanHandler cpl;
	Map<String, Contact> contact_map;
	Map<String, Vertex> cp_vertices;
	
	static public ContactPlanHandler get() {
		if (cpl == null) {
			cpl = new ContactPlanHandler();
			cpl.contact_map = new HashMap<>();
			cpl.cp_vertices = new HashMap<>();
		}
		return cpl;
	}

	private List<DTNHost> order(DTNHost a, DTNHost b){
		List<DTNHost> hs = new ArrayList<DTNHost>(Arrays.asList(a, b));
		Collections.sort(hs, Comparator.comparing(DTNHost::toString));
		return hs;
	}
	
	private String get_pair_id(DTNHost a, DTNHost b) {
		List hs = order(a,b);
		return hs.get(0) + "_" + hs.get(1);		
	}
	
    /**
     * Order the hosts by name (toString()), create an id and add a contact if not added by the other host.
     * @param a The host communicating with the host being updated
     * @param b The updating host
     */
    public void contactStarted(DTNHost a, DTNHost b) {
		String pair_id = get_pair_id(a, b);
		List<DTNHost> hs = new ArrayList(order(a,b));
		// the second add is ignored
		if (!contact_map.keySet().contains(pair_id)) {
			contact_map.put(pair_id, new Contact(hs.get(0), hs.get(1), SimClock.getTime(), -1.0));
		}
    }
    
    public void contactEnded(DTNHost a, DTNHost b) {
		String pair_id = get_pair_id(a, b);
		assert(contact_map.keySet().contains(pair_id));
		if (contact_map.get(pair_id).end() == -1.0) {
			contact_map.get(pair_id).set_end(SimClock.getTime());
		} else {	// second time added, push to the vertex map
			String start = String.format("%.2f", contact_map.get(pair_id).begin());
			String end = String.format("%.2f", contact_map.get(pair_id).end());
			String v_id = "vertex_" + pair_id + "_" + start + "_" + end;
			cp_vertices.put(v_id, new Vertex(v_id, contact_map.get(pair_id), false));
			System.out.println("Completing " + v_id);
			contact_map.remove(pair_id);
		}
    }

}
