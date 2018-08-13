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
import util.Tuple;

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
			CPLAN_DIR = new File("").getAbsolutePath() + CONTACT_PLAN_D;
			create_contact_plan_dir();
		}
		return cpl;
	}

    /**
     * Order the hosts by address, create an id and add a contact if not present.
     * @param a The host communicating with the host being updated
     * @param b The updating host
     */
    public void set_contact_start_time(DTNHost a, DTNHost b) {
    	Contact c = new Contact(a, b, round(SimClock.getTime(), 2), -1.0);
		if (!contact_map.keySet().contains(c.pair_id())) {
			contact_map.put(c.pair_id(), c);
		}
    }
    
    /**
     * During simulation set the contact ending time.
     * <p>
     * When collecting the contact, we assume, that a pair of nodes must first close a contact before
     * start a next one, since the simulation is sequential.
     * Therefore, while collecting starting and ending time of a contact, we use as identifier just
     * the pair_id, formed from the alphabetically ordered DTNHost names.
     * When the contact is to be closed, we add the contact to another map with an identifier
     * that includes starting and ending time, since there will be several contacts with the same pairs.
     * 
     * @param a The first node of a contact ordered alphabetically
     * @param b The second node of a contact ordered alphabetically
     */
    public void set_contact_end_time(DTNHost a, DTNHost b) {
		String pair_id = (new Contact(a, b, 0.0, 1.0).pair_id());
		if (!contact_map.containsKey(pair_id)) {
			System.out.println("BUG: Closing a contact to which the key was not found.");
			return;
		}
		if (contact_map.get(pair_id).end() == -1.0) {
			contact_map.get(pair_id).set_end(round(SimClock.getTime(), 2));
		} else {	// second time added, push to the vertex map with the contact id (with begin and end time)
			String c_id = contact_map.get(pair_id).contact_id();
			contacts_ready.put(c_id, contact_map.get(pair_id));
			System.out.println("Completing " + c_id);
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
    		List<DTNHost> hosts = c.get_hosts();
    		set_contact_end_time(hosts.get(0), hosts.get(1));
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
    		String v_id = "vertex_" + c.contact_id();
    		vertices.put(v_id, new Vertex(v_id, c, false));
    	}
    	
    	return vertices;
    }
    
    /**
     * Create a Map of vertices to list of edges
     * @param v_hashmap The vertices hashmap
     * @return
     */
    public Map<String, List<Edge>> edges_from_vertices(Map<String, Vertex> v_hashmap){
    	Map<String, List<Edge>> edges = new HashMap<>();
    	Set<Vertex> v_set = new HashSet<>();
		for (Vertex v : v_hashmap.values()) {
			v_set.add(v);
		}
    	
		for (Vertex v1 : v_set) {
//			if (!edges.containsKey(v1.get_id())) {
				edges.put(v1.get_id(), new LinkedList<>());
//			}
			for (Vertex v2 : v_set) {
				if (v1.get_common_host(v2) != null && v1.begin() < v2.end()) {
					if ( v1.get_hosts().equals(v2.get_hosts()) ) {
						continue; // do not create vertices to itself
					}
					edges.get(v1.get_id()).add(new Edge(v1, v2));
				}
			}
		}
    	
    	return edges;
    }

    /**
     * Create the graph from disk
     * @return	Contact Graph
     */
    public Graph load_graph() {
    	Map<String, Vertex> vertices = load_vertices_from_file();
    	Map<String, List<Edge>> edges = edges_from_vertices(vertices);
    	return new Graph(vertices, edges);
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
        try ( PrintWriter writer = new PrintWriter(absolute_filename(h))) {
            writer.write("[" + String.join(",", json_list) + "]");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    


    // TODO: AUXILIARY METHODS --> COULD BE MOVED TO UTILS
    
	/*
	 * from: https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
	 */
	static double round (double value, int places) {
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
