package routing.cgr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import core.DTNHost;
import core.Message;
import util.Tuple;

public class RouteSearch {

	private Map<String, Vertex> vertices;
	private Map<String, List<Edge>> edges;
	private Map<Vertex, Vertex> predecessors;
	private Map<Vertex, Double> distances;
	private Map<Vertex, Integer> hops;
	private Set<Vertex> settled;
	private SortedSet<Vertex> unsettled;
	private Distance<Integer, Vertex, Vertex, Double> distance_measure;
	private double expire_time;

	public RouteSearch(Graph g) {
		vertices = g.get_vertice_map();
		edges = g.get_edges();
		init_distances();
		init_predecessors();
		init_unsettled();
		init_settled();
		init_hops();
		distance_measure = least_latency;
		expire_time = Double.POSITIVE_INFINITY;
	}
	
	private void init_distances() {
		this.distances = new HashMap<>();
	}
	
	private void init_predecessors() {
		this.predecessors = new HashMap<>();
	}
	
	private void init_unsettled() {
		this.unsettled = new TreeSet<>(Comparator.comparing(Vertex::adjusted_begin)
				.thenComparing(Vertex::get_id));
	}
	
	private void init_settled() {
		settled = new HashSet<>();
	}
	
	private void init_hops() {
		hops = new HashMap<>();
	}
	
	/**
	 * 
	 * Interface for distance functions. Most common are: least latency to the
	 * target minimum number of hops to the target minimum time span between source
	 * and target
	 *
	 * @param <A>
	 *            source distance
	 * @param <B>
	 *            destination distance
	 * @param <R>
	 *            new destination distance through this source
	 */
	@FunctionalInterface
	interface Distance<A, B, C, R> {
		public R apply(A a, B b, C c);
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
	 * 
	 * @param name
	 *            Methods name to calculate distance
	 */
	public void set_distance_algorithm(String name) {
		if (name == "num_hops") {
			distance_measure = num_hops;
		} else if (name == "least_latency") {
			this.distance_measure = least_latency;
		}
	}
	
	/**
	 * Create the a pivot and add it to vertices.
	 * 
	 * @param v_to_connect
	 *            Vertices to which this pivot should connect to
	 * @param h
	 *            the source or destination host
	 * @param start_time
	 *            vertex start time
	 * @param end_time
	 *            vertice end time
	 * @param start
	 *            true if this is the pivot_begin false if it is end (needed for the
	 *            direction of the edge)
	 * @return The pivot vertice
	 */
	private List<Object> create_pivot_and_initialize(SortedSet<Vertex> v_to_connect, DTNHost h, boolean start) {
		List<Object> pivot_obj_list = new LinkedList<>();

		Contact c = new Contact(h, h, 0.0, Double.POSITIVE_INFINITY);
		Vertex pivot = new Vertex(c.get_id(), c, true);
		String name = c.get_id();
		vertices.put(name, pivot);
		edges.put(pivot.get_id(), new LinkedList<>());
		pivot_obj_list.add(pivot);

		for (Vertex v : v_to_connect) {
			if (start) {
				edges.get(pivot.get_id()).add(new Edge(pivot, v));
			} else {
				Edge to_pivot = new Edge(v, pivot);
				edges.get(v.get_id()).add(to_pivot);
				pivot_obj_list.add(to_pivot);
			}
		}
		return pivot_obj_list;
	}
	
	

	/**
	 * Initialize dijkstra
	 * 
	 * @param pivot_begin
	 *            Start pivot
	 * @param now
	 *            Current simulation time
	 */
	private void init(Vertex pivot_begin, double now) {
		settled.clear();
		unsettled.clear();
		predecessors.clear();
		distances.clear();

		for (Vertex v : vertices.values()) {
			distances.put(v, Double.POSITIVE_INFINITY);
			hops.put(v, Integer.MAX_VALUE);
			predecessors.put(v, null);
		}
		distances.replace(pivot_begin, now);
		hops.replace(pivot_begin, 0);
		unsettled.add(pivot_begin);
	}
	
	/**
	 * return the distances ordered to be displayed as routing information
	 * @return
	 */
	public Map<Vertex, Double> get_distances() {
		return this.distances;
	}

	/**
	 * Reconstruct the path given the end pivot
	 * 
	 * @param end_pivot
	 *            The end pivot
	 * @return The shortest path found
	 */
	public Path get_path(Vertex end_pivot) {
		Path p = new Path();
		p.path = p.construct(end_pivot, predecessors);
		return p;
	}

	/**
	 * Delete ended contacts, their edges and the edges that point to them.
	 * 
	 * @param now
	 *            current simulation time
	 * @return the most recent contact from cur_host with begin before now
	 */
	private void prune(double now) {
		Set<Vertex> to_delete = new HashSet<Vertex>();
		Set<Edge> edges_to_delete = new HashSet<>();

		for (Vertex v : vertices.values()) {
			// pivots on vertices map is garbage from old runs.
			if (v.end() < now || v.is_pivot()) { 
				to_delete.add(v);
			}
		}
		
		// prune old vertices and its edges
		for (Vertex v : to_delete) {
			edges.remove(v.get_id());
			vertices.remove(v.get_id());
		}

		// delete the edges that pointed to the above deleted vertices
		for (List<Edge> le : edges.values()) {
			for (Edge e : le) {
				if (to_delete.contains(e.get_dst_vertex())){
					edges_to_delete.add(e);
				}
			}
		}
		
		for (Edge e : edges_to_delete) {
			String src = e.get_src_vertex().get_id();
			edges.get(src).remove(e);
		}
	}

	/**
	 * Find neighbors of v updating the unsettling list and distances
	 * 
	 * @param v
	 *            Vertex to expand (find neighbors)
	 * @param m
	 *            The distance depends on the message size and transmission speed
	 */

	private void relax(Vertex v, Message m, List<DTNHost> blacklist) {
		int size = m.getSize();

		List<Vertex> neighbors = new ArrayList<>();
		Vertex v_dst;
		DTNHost h_dst;
		double h_dst_capacity;
		
		List<Edge> toDelete = new ArrayList<>();
		for (Edge e : edges.get(v.get_id())) {
			if (!(e.get_dst_begin() < this.expire_time)) continue;
			v_dst = vertices.get(e.get_dest_id());
			if (settled.contains(v_dst)) continue;
			if (!Collections.disjoint(v_dst.get_hosts(), blacklist)) continue;
			if (!(v_dst.current_capacity() > size)) continue;
			// verify that the destination host has space for the new message
			h_dst = v_dst.get_other_host(v.get_common_host(v_dst));
			
			// capacity taken in account the messages already planned 
			h_dst_capacity = (v_dst.adjusted_begin() - v_dst.begin())* v_dst.get_transmission_speed();
			
			if (h_dst.getRouter().getFreeBufferSize() - h_dst_capacity < m.getSize()) continue;
			neighbors.add(v_dst);
		}
		
		for (Vertex n : neighbors) {
			double at = (double) distance_measure.apply(size, v, n);
			if (at < n.end()) {
				if (at > distances.get(n)) {
					continue;
				} else if (at < distances.get(n)) { // improved distance
					predecessors.replace(n, v);
					hops.put(n, hops.get(v) + 1);
					distances.replace(n, at);
					unsettled.remove(n); // remove if present, ignore otherwise
					unsettled.add(n);	 // (re)insert the updated value. No effect if distance is unchanged
				} else { // same distance
					if (hops.get(n) > hops.get(v) + 1) { // we can achieve the same vertice with less hops
						predecessors.replace(n, v);
						hops.put(n, hops.get(v) + 1);
					}
				}
			}
		}
	}

	/**
	 * The Dijkstra algorithm
	 * 
	 * @param this_host
	 *            Host calculating the shortest path
	 * @param now
	 *            Simulation current time
	 * @param m
	 *            Message to be sent
	 * @param pivot_begin
	 *            Start pivot
	 * @param pivot_end
	 *            End pivot
	 * @return On success returns end pivot. Returns null if no path was found
	 */
	public Vertex run_dijkstra(Vertex pivot_begin, Vertex pivot_end, double now, Message m, List<DTNHost> blacklist) {
		Vertex next = null;
		Vertex out = null;

		init(pivot_begin, now);

		while (!unsettled.isEmpty()) {
			next = unsettled.first(); // unsettled is ordered
			if (next.equals(pivot_end)) {
				break;
			}
			relax(next, m, blacklist);
			unsettled.remove(next);
			settled.add(next);
		}
		return next;
	}

	/**
	 * Find possible contacts for beginning and end pivots
	 * 
	 * @param h
	 *            Host in which path decision is current taking place
	 * @param now
	 *            Current simulation time
	 * @param m
	 *            Message to be sent
	 * @return Map with the list of candidates for begin pivot and end pivot
	 */
	private Map<String, SortedSet<Vertex>> find_contacts_of_interest(DTNHost h, double now, Message m) {
		Map<String, SortedSet<Vertex>> candidates = new HashMap<String, SortedSet<Vertex>>();
		candidates.put("coi_src", new TreeSet<>(Comparator.comparing(Vertex::adjusted_begin).thenComparing(Vertex::get_id)));
		candidates.put("coi_dst", new TreeSet<>(Comparator.comparing(Vertex::adjusted_begin).thenComparing(Vertex::get_id)));

		for (Vertex c : vertices.values()) {
			if (c.end() < now && c.adjusted_begin() > m.getTtl()) {
				continue;
			}
			if (c.is_pivot()) {
				continue;
			}
			List<DTNHost> hl = c.get_hosts();
			// contacts including current host (used for pivot_begin) with enough capacity
			if (hl.contains(h)  && c.current_capacity() > m.getSize()) {
				candidates.get("coi_src").add(c);
			}
			if (hl.contains(m.getTo()) && c.current_capacity() > m.getSize()) {
				candidates.get("coi_dst").add(c);
			}
		}

		return candidates;
	}

	/**
	 * Search least latency
	 * 
	 * @param pivot_candidates:
	 *            a list of candidates for pivot start and pivot end
	 * @param now
	 *            current simulation time
	 * @param size
	 *            message size
	 * @return
	 */
	private Vertex search_ll(Map<String, SortedSet<Vertex>> pivot_candidates, double now, Message m, DTNHost this_host) {
		SortedSet<Vertex> coi_src = pivot_candidates.get("coi_src");
		SortedSet<Vertex> coi_dst = pivot_candidates.get("coi_dst");

		/* Creating pivots and edges for/from them 
		 * p_begin / p_end is a list of Objects
		 * the first object is the pivot vertex and the rest of the list 
		 * are edges added to this pivot.
		 * We save these edges for cleaning after search, avoiding go through the 
		 * whole edge list.
		 * */
		List<Object> p_begin = create_pivot_and_initialize(coi_src, this_host, true);
		List<Object> p_end = create_pivot_and_initialize(coi_dst, m.getTo(), false);			
		
		List<DTNHost> blacklist = m.getHops();
		blacklist.remove(this_host);
		Vertex pivot_begin = (Vertex)p_begin.get(0);
		Vertex pivot_end = (Vertex)p_end.get(0);
		
		pivot_end = run_dijkstra(pivot_begin, pivot_end, now, m, blacklist);
		
		//cleanup edges from vertices to end_pivots
		for (Edge e: (List<Edge>)(Object)p_end.subList(1, p_end.size())) {
			edges.get(e.get_src_id()).remove(e);
		}
		
		return pivot_end;
	}
		
		private double final_distance(Vertex pivot) {
			double ret = 0.0;
			if (pivot != null) {
				if (predecessors.get(pivot) != null) {
					Vertex pred = predecessors.get(pivot);
					ret = distances.get(pred);
				}
			}
			return ret;
		}

	/**
	 * Search the best path using Dijkstra algorithm
	 * 
	 * @param this_host
	 *            the host calculating the best path
	 * @param now
	 *            simulation time
	 * @param m
	 *            message to be sent
	 * @param expire 
	 * 			  adjusted ttl taking in account the default given in the config file   
	 *         
	 * @return pivot_end on success or null if no path was found
	 */
	public Vertex search(DTNHost this_host, double now, Message m, int configTtl) {
		Map<String, SortedSet<Vertex>> pivot_candidates = new HashMap<String, SortedSet<Vertex>>();
		Vertex pivot_begin = null;
		Vertex pivot_end = null;
		/* transform the ttl (minutes) to the expiration in time (time when the message was
		*	created + original ttl
		*/
		if (m.getTtl() == Integer.MAX_VALUE) { // ttl not defined in msg. Get the value from config
			this.expire_time = Integer.MAX_VALUE;
		} else { 	// ttl is configured in message
			this.expire_time = m.getTtl() * 60 + now;
		}

		/*
		 * Choose search type: If we aim for least latency, we are interested in the
		 * first pb_candidate and the first possible pe_candidate
		 * 
		 * If we aim for the least amount of time the message is underway, or the min
		 * amount of hops we might need to try all possibilities from the possible
		 * pb_candidates and pe_candidates and verify the alternative that provide
		 * provides you that
		 * 
		 * TODO: implement min amount of hops and end2end_min_lantency as first version
		 * we implement just the least_latency.
		 */

		/*
		 * Delete unusable vertexes fpc -- first pivot candidate, the last contact from
		 * that started before now.
		 */
		prune(now);

		pivot_candidates = find_contacts_of_interest(this_host, now, m);

		int start_candidates_size = pivot_candidates.get("coi_src").size();
		int end_candidates_size = pivot_candidates.get("coi_dst").size();

		if (start_candidates_size > 0 && end_candidates_size > 0) {
			// least latency:
			pivot_end = search_ll(pivot_candidates, now, m, this_host);
		} else {
			System.out.println("Pivot could not be found. There is no route.");
		}

		// assert that distance to arrive at destination < expiration time
		if (final_distance(pivot_end) > expire_time) { 
			pivot_end = null;
		}
		
		return pivot_end;
	}

}
