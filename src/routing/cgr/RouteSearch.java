package routing.cgr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import core.DTNHost;
import core.Message;

public class RouteSearch {

	private Graph graph;
	private Map<String, Vertex> vertices;
	private Map<String, List<Edge>> edges;
	private Map<Vertex, Vertex> predecessors;
	private Map<Vertex, Double> distances;
	private Set<Vertex> settled;
	private List<Vertex> unsettled;
	private Message message;
	private final int EARLIER_THAN = 0;
	private final int LATER_THAN = 1;
	private Vertex end_pivot;
	private Distance distance_measure;

	
    public RouteSearch(Graph g) {
        graph                  	= g;
        vertices               	= g.get_vertice_map();
        edges                  	= g.get_edges();
        distances               = new HashMap<>();
        predecessors           	= new HashMap<>();
        unsettled      			= new LinkedList<>();
        settled		        	= new HashSet<>();
        distance_measure        = least_latency;
    }
    
    /**
     * 
     * Interface for distance functions.
     * Most common are:
     * 		least latency to the target
     * 		minimum number of hops to the target
     * 		minimum time span between source and target
     *
     * @param <A>	source distance
     * @param <B>	destination distance
     * @param <R>	new destination distance through this source
     */
    @FunctionalInterface
	interface Distance <A, B, C, R> {
		public R apply (A a, B b, C c);
	}

	Distance<Integer, Vertex, Vertex, Double> least_latency = (size, cur, neighbor) -> {
		double neighbor_transmission_time = (double) size / neighbor.get_transmission_speed();
		return Math.max(distances.get(cur), neighbor.adjusted_begin()) + neighbor_transmission_time;
	};
	
	Distance<Integer, Vertex, Vertex, Double> num_hops = (size, cur, neighbor) -> {
		return distances.get(cur) + 1.0;
	};
    
	/**
	 * Define how to calculate the distance to the next hop
	 * @param name Methods name to calculate distance
	 */
	public void set_distance_algorithm(String name) {
		if (name == "num_hops") {
			distance_measure = num_hops;
		} 
		else if (name == "least_latency") {
			distance_measure = distance_measure;
		} 
	}
    
    /**
     * Create the a pivot and add it to vertices.
     * @param h the source or destination host
     * @param start_time vertex start time
     * @param end_time vertex end time
     * @return vertice representing the contact from the node to itself
     */
    private Vertex create_pivot_and_initialize(DTNHost h, double start_time, double end_time) {
    	Contact c = new Contact(h, h, start_time, end_time);
    	Vertex pivot = new Vertex(c.get_id(), c, true);
    	String name = c.get_id();
    	vertices.put(name, pivot);
    	pivot.set_receiver(h);
    	edges.put(pivot.get_id(), new LinkedList<>());

    	return pivot;
    }

    private void init(Message m, double now, DTNHost cur) {
    	message = m;
    	settled.clear();
    	unsettled.clear();
    	predecessors.clear();
    	prune(now);			// prune old vertices
    	
    	/* Add an src and end pivot + edges to reach it */
    	double end_time = Double.POSITIVE_INFINITY;

    	Vertex rootVertex = create_pivot_and_initialize(cur, 0, end_time);
    	List<Vertex> source_vertexes = new ArrayList<>();
    	
    	end_pivot = create_pivot_and_initialize(m.getTo(), 0, end_time);
    	
    	for (Vertex v : vertices.values()) {
    		distances.put(v, Double.POSITIVE_INFINITY);
    		predecessors.put(v,  null);
    		// connects every vertice that contains the destination host to the pivot
    		if (v.get_hosts().contains(m.getTo()) && !v.is_pivot()) {
    			edges.get(v.get_id()).add(new Edge(v, end_pivot));
    		}
    		// connects every vertex containing src host whose contact did not finished to the pivot
    		if (v.get_hosts().contains(cur) && !v.is_pivot() && v.end() > now) {
    			edges.get(rootVertex.get_id()).add(new Edge(rootVertex, v));
    		}
    	}
    	distances.replace(rootVertex, now);
    	unsettled.add(rootVertex);
    }
    
    public Path get_path(Vertex end_pivot) {
    	Path p = new Path();
    	p.path = p.construct(end_pivot, predecessors);
    	return p;
    }

    /**
     * Simple immutable class that stores the hosts of a vertices
     * as a set to be used in as key in a hashSet (for pruning)
     */
    private class Bigram{
        private Set<DTNHost> hosts_set;
        private String bid;
        
        public Bigram(Set<DTNHost> hset, String bid) {
        	hosts_set = hset;
        	this.bid = bid;
        }

        public String get_bid() {
        	return bid;
        }
        
        @Override
        public int hashCode(){
                final int prime = 31;
                int result = 1;
                result = prime * result + ((bid == null) ? 0 : bid.hashCode());
                return result;
        }

        @Override 
        public boolean equals(Object obj){
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Bigram other = (Bigram) obj;
            String otherbid = other.get_bid();
            return (bid.equals(otherbid));
        }
    }
    
    private Bigram create_bigram(Vertex v, List<DTNHost> hosts_list) {
		String set_bid = hosts_list.get(0).toString() + hosts_list.get(1).toString();
		Set<DTNHost> hosts_set = new HashSet<>();
		for (DTNHost h : hosts_list) {
			hosts_set.add(h);
		}
		return new Bigram(hosts_set, set_bid);
    }
 
    /**
     * Delete unusable vertices. 
     * Assumption: If two vertex have the same src/dst hosts and both begin in at some 
     * point in time in the past we can safely prune the oldest vertex and its edges.
     * @param visited hosts already visited by this message
     */
    private void prune(double now) {
    	Map<Bigram, Vertex> v_map = new HashMap<Bigram, Vertex>();
    	List<Vertex> to_delete = new LinkedList<Vertex>();

    	for (Vertex v : vertices.values()) {
    		if (v.begin() > now) { // not interested in contacts to come	
    			continue;
    		}
    		List<DTNHost> hosts_list = v.get_hosts();
			Bigram b = create_bigram(v, hosts_list);
    		
    		if (v_map.get(b) == null) { // see vertex for the first time
    			v_map.put(b, v);
    			continue;
    		}	
    		else if (v_map.get(b).begin() < v.begin()) { // found a vertex to prune
    			to_delete.add(v_map.get(b));
    			v_map.put(b, v);
    		} else {
    			to_delete.add(v);
    		}
    	}
    	
    	// prune
    	for (Vertex v : to_delete) {
    		edges.remove(v.get_id());
    		vertices.remove(v.get_id());
    	}   	
    }
    
    /**
     * Searches through unsettled list the vertice with the least distance
     * @param m Message, not sure if we need. -- TODO, if we don't need take this out. 
     * @return the vertice with the least distance from source
     */
    private Vertex get_next_unsettled(Message m) {
		Vertex next = null;
		double shortest_distance = Double.POSITIVE_INFINITY;
		for (Vertex v : unsettled) {
			if (distances.get(v) < shortest_distance) {
				shortest_distance = distances.get(v);
				next = v;
			}
		}
		return next;

    }
   
    /**
     * Calculate the best possible distance until the bundle reception at neighbor.
     * neighbor_min_distance takes in account the fact that distances are started at infinity.
     * In such case the distance to the neighbor becomes neighbor_begin + time to transmit.
     * @param size  Message size
     * @param cur	Current DTNHost
     * @param neighbor	Neighbor DTNHost
     * @return	moment when the message should arrive at neighbor through this path.
     */
    private double calculate_arrival_time(int size, Vertex cur, Vertex neighbor) {
    	double neighbor_transmission_time = (double) size / neighbor.get_transmission_speed();
    	return  Math.max(distances.get(cur), neighbor.adjusted_begin()) + neighbor_transmission_time;
    }
    
    /**
     * Find neighbors of v updating the unsettling list and distances
     * @param v Vertex to expand (find neighbors)
     * @param m The distance depends on the message size and transmission speed
     */
    
    private Vertex relax(Vertex v, int size, int ttl) {
    	Vertex pivot = null;
    	List<Vertex> neighbors = edges.get(v.get_id()).stream()
    			.filter(e -> !settled.contains(e.get_dest_id()))	// filter out settled vertices
    			.filter(e -> e.get_dst_begin() < ttl)				// filter out contacts that start after ttl expiration
    																// TODO filter out contacts with already visited nodes
    			.map(e -> vertices.get(e.get_dest_id()))
    			.filter(e -> e.current_capacity() > size)  			// filter out contacts that does not have enough capacity
    			.collect(Collectors.toList());
    	
		for (Vertex n : neighbors) {
			double at = calculate_arrival_time(size, v, n);
			if (at < distances.get(n) && at < n.end()) { 			// improved distance?
				predecessors.replace(n, v);
				distances.replace(n, at);
				n.set_receiver(v.get_sender());
				if (!unsettled.contains(n)) {
					unsettled.add(n);
				}
				if (n.is_pivot()) {
					pivot = n;
					break;
				}
			}
		}
    	return pivot;
    }
    
    /**
     * Search the best path using Dijkstra algorithm
     * @param this_host the host calculating the best path 
     * @param now simulation time
     * @param m message to be sent
     * @return
     */
    public Vertex search(DTNHost this_host, double now, Message m) {
    	init (m, now, this_host);

    	Vertex cur;
    	Vertex pivot = null;
    	while (!unsettled.isEmpty()) {
    		cur = get_next_unsettled(m);
    		// TODO:verify when cur becomes null
    		if (cur == null || cur.equals(end_pivot)) { 
    			break;
    		}
    		pivot = relax(cur, m.getSize(), m.getTtl());
    		unsettled.remove(cur);
    		settled.add(cur);
    	}
    	return pivot;
    }
}
