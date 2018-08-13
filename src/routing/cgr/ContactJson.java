package routing.cgr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import core.DTNHost;
import core.SimScenario;

public class ContactJson {
    private int host_a_addr;
    private int host_b_addr;

    /* The scenario hash is being saved in each contact for compatibility with old version
     * TODO: iri put the hash on the filename concatenated with the node name. We need both just once. */
    private int     scenario_hash;

    private double start;
    private double end;
    
    private int transmission_speed;
    
    ContactJson(Contact c, int scenarioHash) {
		host_a_addr = c.get_hosts().get(0).getAddress();
		host_b_addr = c.get_hosts().get(1).getAddress();

		scenario_hash = scenarioHash;

		start = c.begin();
		end = c.end();
		
		transmission_speed = c.get_transmission_speed();
	}

    public String toJson() {
    	Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }    

	public Contact toContact() {
		DTNHost a = null;
		DTNHost b = null;
		Contact c = null;

		for (DTNHost h : SimScenario.getInstance().getHosts()) {
			if (h.getAddress() == host_a_addr) {
				a = h;
			} else {
				if (h.getAddress() == host_b_addr) {
					b = h;
				}
			}
		}
		assert(a != null && b != null);
		c = new Contact(a, b, start, end);
		c.set_transmission_speed(transmission_speed);
		return c;
	}
	
	public int scenario_hash() {
		return this.scenario_hash;
	}
}

