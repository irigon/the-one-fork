package routing.cgr;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import core.DTNHost;
import core.Message;
import jdk.internal.util.xml.impl.Pair;

public class RouteSearch {

	private Map<String, Vertex> vertices;
	private Map<String, List<Edge>> edges;
	private Map<Vertex, Vertex> predecessors;
	private Map<Vertex, Double> distances;
	private Map<Vertex, Integer> hops;
	private Set<Vertex> settled;
	private SortedSet<Vertex> unsettled;
	private Distance<Integer, Vertex, Vertex, Double> distance_measure;

	public RouteSearch(Graph g) {
		vertices = g.get_vertice_map();
		edges = g.get_edges();
		distances = new HashMap<>();
		predecessors = new HashMap<>();
		unsettled = new TreeSet<>(Comparator.comparing(Vertex::adjusted_begin)
				.thenComparing(Vertex::get_id));
		settled = new HashSet<>();
		hops = new HashMap<>();
		distance_measure = least_latency;
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
	private List<Object> create_pivot_and_initialize(List<Vertex> v_to_connect, DTNHost h, boolean start) {
		List<Object> pivot_obj_list = new LinkedList<>();

		Contact c = new Contact(h, h, 0.0, Double.POSITIVE_INFINITY);
		Vertex pivot = new Vertex(c.get_id(), c, true);
		String name = c.get_id();
		vertices.put(name, pivot);
		pivot.set_receiver(h);
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
	 * Delete unusable vertices. Assumption: If a vertex (and therefore a contact)
	 * already ended, it can be discarded with its edges.
	 * 
	 * @param now
	 *            current simulation time
	 * @return the most recent contact from cur_host with begin before now
	 */
	private void prune(double now) {
		List<Vertex> to_delete = new LinkedList<Vertex>();

		for (Vertex v : vertices.values()) {
			// pivots on vertices map is garbage from old runs.
			if (v.end() < now || v.is_pivot()) { 
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
	 * Find neighbors of v updating the unsettling list and distances
	 * 
	 * @param v
	 *            Vertex to expand (find neighbors)
	 * @param m
	 *            The distance depends on the message size and transmission speed
	 */

	private void relax(Vertex v, Message m, List<DTNHost> blacklist) {
		int size = m.getSize();
		int ttl = m.getTtl();
		List<Vertex> neighbors = edges.get(v.get_id()).stream()
				.filter(e -> e.get_dst_begin() < ttl)      // filter out far in the future vertices
				.map(e -> vertices.get(e.get_dest_id()))
				.filter(e -> !settled.contains(e))         // filter out already settled vertices
				.filter(e -> Collections.disjoint(e.get_hosts(), blacklist)) // filter out already visited nodes
				.filter(e -> e.current_capacity() > size)  // filter out contacts without enough capacity
				.collect(Collectors.toList());

		for (Vertex n : neighbors) {
			double at = (double) distance_measure.apply(size, v, n);
			if (at < n.end()) {
				if (at > distances.get(n)) {
					continue;
				} else if (at < distances.get(n)) { // improved distance
					predecessors.replace(n, v);
					hops.put(n, hops.get(v) + 1);
					n.set_receiver(v.get_sender());
					distances.replace(n, at);
					unsettled.remove(n); // remove if present, ignore otherwise
					unsettled.add(n);	 // (re)insert the updated value. No effect if distance is unchanged
				} else { // same distance
					if (hops.get(n) > hops.get(v) + 1) { // we can achieve the same vertice with less hops
						predecessors.replace(n, v);
						hops.put(n, hops.get(v) + 1);
						n.set_receiver(v.get_sender());
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
	 * Add an element ordered in the linked list (from: goo.gl/XrHdhB)
	 * 
	 * @param v
	 *            Vertex to be inserted
	 * @param ll
	 *            Linked List
	 */
	private void orderedAdd(Vertex v, List<Vertex> ll) {
		ListIterator<Vertex> itr = ll.listIterator();
		while (true) {
			if (itr.hasNext() == false) {
				itr.add(v);
				return;
			}

			Vertex elementInList = itr.next();
			if (elementInList.adjusted_begin() > v.adjusted_begin()) {
				itr.previous();
				itr.add(v);
				return;
			}
		}
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
	private Map<String, List<Vertex>> find_contacts_of_interest(DTNHost h, double now, Message m) {
		Map<String, List<Vertex>> candidates = new HashMap<String, List<Vertex>>();
		candidates.put("coi_src", new LinkedList<Vertex>());
		candidates.put("coi_dst", new LinkedList<Vertex>());

		for (Vertex c : vertices.values()) {
			if (c.end() < now && c.adjusted_begin() > m.getTtl()) {
				continue;
			}
			if (c.is_pivot()) {
				continue;
			}
			// contacts including current host (used for pivot_begin) with enough capacity
			if (c.get_hosts().contains(h) && c.current_capacity() > m.getSize()) {
				orderedAdd(c, candidates.get("coi_src"));
				// contacts including destination host (used for pivot end)
			}
			if (c.get_hosts().contains(m.getTo()) && c.current_capacity() > m.getSize()) {
				orderedAdd(c, candidates.get("coi_dst"));
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
	 * @param ttl
	 *            message ttl
	 * @return
	 */
	private Vertex search_ll(Map<String, List<Vertex>> pivot_candidates, double now, Message m, DTNHost this_host) {
		List<Vertex> coi_src = pivot_candidates.get("coi_src");
		List<Vertex> coi_dst = pivot_candidates.get("coi_dst");

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

	/**
	 * Search the best path using Dijkstra algorithm
	 * 
	 * @param this_host
	 *            the host calculating the best path
	 * @param now
	 *            simulation time
	 * @param m
	 *            message to be sent
	 * @return pivot_end on success or null if no path was found
	 */
	public Vertex search(DTNHost this_host, double now, Message m) {
		Map<String, List<Vertex>> pivot_candidates = new HashMap<String, List<Vertex>>();
		Vertex pivot_begin = null;
		Vertex pivot_end = null;

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
		return pivot_end;
	}
}
