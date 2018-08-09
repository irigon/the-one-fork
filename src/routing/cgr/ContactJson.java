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
    private String host_a;
    private String host_b;

    /* The scenario hash is being saved in each contact for compatibility with old version
     * TODO: iri put the hash on the filename concatenated with the node name. We need both just once. */
    private int     scenario_hash;

    private double start;
    private double end;
    
    ContactJson(Contact c, int scenarioHash) {
		host_a = c.get_hosts().get(0).toString();
		host_b = c.get_hosts().get(1).toString();

		scenario_hash = scenarioHash;

		start = c.begin();
		end = c.end();
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
			if (h.toString().equals(host_a)) {
				a = h;
			} else {
				if (h.toString().equals(host_b)) {
					b = h;
				}
			}
		}
		assert(a != null && b != null);
		 
		return new Contact(a, b, start, end);
	}
	
	public int scenario_hash() {
		return this.scenario_hash;
	}
}

