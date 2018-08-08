package routing.cgr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import core.DTNHost;
import core.SimClock;
import core.SimScenario;

public class ContactPlanHandler {

	static ContactPlanHandler cpl;
	Map<String, Contact> contact_map;
	Map<String, Contact> contacts_ready;
	private int scenario_hash;
	
    private static final String CONTACT_PLAN_D = File.separatorChar + 
    		"data" + File.separatorChar + "contact_plans" + File.separatorChar;
    private static String CPLAN_DIR;


	
	static public ContactPlanHandler get() {
		if (cpl == null) {
			cpl = new ContactPlanHandler();
			cpl.contact_map = new HashMap<>();
			cpl.contacts_ready = new HashMap<>();
			create_contact_plan_dir();
			CPLAN_DIR = new File("").getAbsolutePath() + CONTACT_PLAN_D;
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
    public void set_contact_start_time(DTNHost a, DTNHost b) {
		String pair_id = get_pair_id(a, b);
		List<DTNHost> hs = new ArrayList(order(a,b));
		// the second add is ignored
		if (!contact_map.keySet().contains(pair_id)) {
			contact_map.put(pair_id, new Contact(hs.get(0), hs.get(1), round(SimClock.getTime(), 2), -1.0));
		}
    }
    
    public void set_contact_end_time(DTNHost a, DTNHost b) {
		String pair_id = get_pair_id(a, b);
		if (!contact_map.containsKey(pair_id)) {
			return;
		}

		if (contact_map.get(pair_id).end() == -1.0) {
			contact_map.get(pair_id).set_end(round(SimClock.getTime(), 2));
		} else {	// second time added, push to the vertex map
			double start = contact_map.get(pair_id).begin();
			double end = contact_map.get(pair_id).end();
			String v_id = "contact_" + pair_id + "_" + start + "_" + end;
			contacts_ready.put(v_id, contact_map.get(pair_id));
			System.out.println("Completing " + v_id);
			contact_map.remove(pair_id);
		}
    }
    
    /**
     * Close unfinished contacts (just done for the first host)
     * Save contacts of this host to a file
     */
    public void finish_contactplan(DTNHost h) {
    	List<Contact> clist = new ArrayList(contact_map.values());
    	for (Contact c : clist) {
    		set_contact_end_time(c.get_hosts().get(0), c.get_hosts().get(1));
    	}
    }
    
    /**
     * Return the absolute path for the contact file of a host
     * @param h DTNHost 
     * @return	String with the absolute path
     */
    private String absolute_filename(DTNHost h) {
        return CPLAN_DIR + h.toString() + ".json";
    }
    
    /**
     * Load all hosts contact from disk.
     * @param abs_name	Absolute path to the file
     * @return	Set with the contacts that this DTNHost participates
     */
    static public List<ContactJson> load_contacts(String abs_name){
        File file = new File(abs_name);
        Gson gson = new Gson();
        List<ContactJson> contactJsonList = null;
        if (!file.exists()) {
        	String fname = abs_name.substring(abs_name.lastIndexOf("/") + 1);
        	System.out.println("Contact " + fname + " could not be found.");
        	return null;
        }
        
        // read saved contact from disk
        BufferedReader br;
        try {
        	br = new BufferedReader(new FileReader(file));
            contactJsonList = gson.fromJson(br, new TypeToken<List<ContactJson>>(){}.getType());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        
        return contactJsonList;
    }

    /**
     * Verifies if for all hosts in the current simulation there is a host file
     * and if the hash of the simulation is the same as the one used to create the file
     * 
     * @return True if all hosts are found with the right hash. False otherwise.
     */
    public boolean has_contact_plan() {
    	boolean ret = true;
    	for (DTNHost host : SimScenario.getInstance().getHosts()) {
    		List<ContactJson> contacts_json = load_contacts(absolute_filename(host));
    		if (contacts_json == null) { 
    			ret = false;			// some host file was not present 
    			break;
    		} 
    		if (contacts_json.size() > 0) {
    			ContactJson cj = contacts_json.get(0);
    			if (getScenarioHash() != cj.scenario_hash()) {
    				ret = false; 		// host has a different hash
    				break;
    			}
    		}
    	}  	
    	return ret;
    }
    
    /**
     * After verifying that there is a contact plan (has_contact_plan()), 
     * we can load the contacts as vertices 
     * @return
     */
    public Map<String, Vertex> load_vertices_from_file() {
    	Map<String, Vertex> vertices = new HashMap<>();
    	Set<Contact> cs = new HashSet<>();
    	
    	for (DTNHost host : SimScenario.getInstance().getHosts()) {
    		List<ContactJson> contacts_json = load_contacts(absolute_filename(host));
    		for (ContactJson cj : contacts_json) {
    			cs.add(cj.toContact());
    		}
    	}
    	
    	for (Contact c : cs) {
    		String v_id = "vertex_" + c.generate_id();
    		vertices.put(v_id, new Vertex(v_id, c, false));
    	}
    	
    	return vertices;
    }
    
    /**
     * Create a Map of vertices to list of edges
     * @param v_hashmap The vertices hashmap
     * @return
     */
    public Map<String, List<Edge>> edges_from_vertices(Map<String, List<Vertex>> v_hashmap){
    	Map<String, List<Edge>> edges = new HashMap<>();
    	Set<Vertex> v_set = new HashSet<>();
    	for (List<Vertex> v_list : v_hashmap.values()) {
    		for (Vertex v : v_list) {
    			v_set.add(v);
    		}
    	}
    	
		for (Vertex v1 : v_set) {
			for (Vertex v2 : v_set) {
				if (v1.get_common_host(v2) != null && v1.begin() < v2.end()) {
					if (!edges.containsKey(v1.get_id())) {
						edges.put(v1.get_id(), new LinkedList<>());
					}
					edges.get(v1.get_id()).add(new Edge(v1, v2));
				}
			}
		}
    	
    	return edges;
    }

    /**
     * Save all contacts from this hosts that are still not saved
     * @param h Host to be saved
     */
    public void save_contacts(DTNHost h) {
    	List<String> json_list = new LinkedList<>();
    	ContactJson cj;
    	for (Contact c : contacts_ready.values()) {
    		if (c.get_hosts().contains(h)) {
    			cj = new ContactJson(c, getScenarioHash());
    			json_list.add(cj.toJson());
    		}
    	}
    	/* TODO: change toString to a get_name() or get_id(). 
    	 * Leaving currently this way to minimize changing in current src code. */
        try ( PrintWriter writer = new PrintWriter(absolute_filename(h))) {
            writer.write(String.join(",", json_list));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    


    // TODO: AUXILIARY METHODS --> COULD BE MOVED TO UTILS
    
	/*
	 * from: https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
	 */
	private static double round (double value, int places) {
		if (places < 0) throw new IllegalArgumentException();
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places,  RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

    /**
     * Checks if the required directory exists, otherwise create it.
     * @return false if the directory did not exist and could not be created. True otherwise.
     */
    static private boolean create_contact_plan_dir() {
        File dir = new File(CPLAN_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("Failed to create directory: " + dir.toString());
                return false;
        }
        return true;
    }
    
    /**
     * Calculate a hash of a scenario based on its hosts names.
     * @return Scenario's hash code
     */
    private int getScenarioHash() {
        if (scenario_hash == 0) {
            SimScenario scen = SimScenario.getInstance();
            String base = "";
            for (DTNHost host : scen.getHosts()) {
                base += host.toString();
            }
            scenario_hash = base.hashCode();
        }
        return scenario_hash;
    }
    
}
