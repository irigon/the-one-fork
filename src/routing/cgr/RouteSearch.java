package routing.cgr;

import java.util.ArrayList;
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
	private List<Vertex> settled;
	private List<Vertex> unsettled;
	private Message message;
	private final int EARLIER_THAN = 0;
	private final int LATER_THAN = 1;
	private Vertex end_pivot;

	
    public RouteSearch(Graph g) {
        graph                  	= g;
        vertices               	= g.get_vertice_map();
        edges                  	= g.get_edges();
        distances               = new HashMap<>();
        predecessors           	= new HashMap<>();
        unsettled      			= new LinkedList<>();
        settled		        	= new LinkedList<>();
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
    	/* We can prune the graph to perform the search faster. 
    	 * There are mainly 3 possibilities:
    	 * 	[1] Delete old edges and vertices that ends before "now" 
    	 * 	[2] Delete edges and vertices that starts after m.ttl()
    	 * 	[2] Delete edges and vertices that use vertices already visited
 
    	 *	[1] --> can be performed on the original graph
    	 *	[2] --> must be performed on a copy and it is message dependent 
    	 * */
    	prune(cur.toString(), EARLIER_THAN, now);
    	prune(cur.toString(), LATER_THAN, m.getTtl());
    	prune(m.getHops());
    	
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
     * Prune graph given the starting point and direction
     * @param time	starting point
     * @param when	EARLIER_THAN (prune all branches that end earlier than time)
     * or OLDER_THAN (prune all branches that begin after time)
     */
    private void prune(String curr_id, int when, double time) {
        switch (when) {
        	case EARLIER_THAN: 
        		for (String id : edges.keySet()) {
        			Set<Edge> old_edges_set = new HashSet<>(edges.get(id).stream()
        					// if the current host is the destination of a contact that just finished, it is still
        					// possible to use the edge from this contact
        					.filter(edge -> edge.get_src_end() < time && edge.get_dest_id() != curr_id)
        					.collect(Collectors.toSet()));
        			edges.get(id).removeAll(old_edges_set);
        			if (edges.isEmpty()) {
        				edges.remove(id);
        				vertices.remove(id);
        			}
        		}
        		break;
        	case LATER_THAN: 
	        	//TODO: prune_future_branches
	        	System.out.println("TODO: prune_future_branches");
        		break;
        }
    }
    
    /**
     * Delete already visited edges and vertices
     * @param visited hosts already visited by this message
     */
    private void prune(List<DTNHost> visited) {
    	// TODO: prune visited nodes
    	System.out.println("TODO: prune visited nodes");
    }
    
    /**
     * Searches through unsettled list the vertice with the least distance
     * @param m Message, not sure if we need. -- TODO, if we don't need take this out. 
     * @return the vertice with the least distance from source
     */
    private Vertex get_next_unsettled(Message m) {
		Vertex next = null;
		if (!unsettled.isEmpty()) {
			double shortest_distance = Double.POSITIVE_INFINITY;
			for (Vertex v : unsettled) {
				if (distances.get(v) < shortest_distance) {
					shortest_distance = distances.get(v);
					next = v;
				}
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
    
    public List<Vertex> construct_path(Vertex end){
    	List<Vertex> path = new ArrayList<>();
    	return path;
    }
    
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
    
    /**
     * The following helpers are used for testing purpose
     */
    public Map<Vertex, Double> get_distances(){
    	return distances;
    }
}
