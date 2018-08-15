package routing.cgr;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import core.Message;

public class Graph {

	private Map<String, Vertex> vertices;
	private Map<String, List<Edge>> edges;
	
	public Graph(Map<String, Vertex> vmap , Map<String, List<Edge>> le) {
		vertices = vmap;
		edges = le;
	}
	
	/**
	 * copy constructor
	 * @param graph original
	 */
	public Graph(Graph graph) {
		init_vertices(graph);
		init_edges(graph);
	}
	
	private void init_vertices(Graph g) {
		if (g == null) {
			this.vertices = new HashMap<>();
		} else {
			this.vertices = new HashMap<>(makeMapCopy(g.vertices));
		}
	}
	
	private void init_edges(Graph g) {
		if (g == null) {
			this.edges = new HashMap<>();
		} else {
			edges = new HashMap<>(g.edges);
		}
	}
	
	/**
	 * Helper function for copy constructor
	 * @param original the original graph
	 * @return copy of the original graph
	 */
	private Map<String, Vertex> makeMapCopy(Map<String, Vertex> original) {
		if (original == null) {
			return null;
		}
		Map<String, Vertex> copy = new HashMap<>(original.size());
		for (String s : original.keySet()) {
			copy.put(s,  original.get(s).replicate());
		}
		return copy;
	}
	
	public Map<String, Vertex> get_vertice_map() {
		return vertices;
	}
	
	public Map<String, List<Edge>> get_edges() {
		return edges;
	}
	
	/**
	 * Split a contact if the communication starts in the middle and finishes before its end
	 * @param n_start
	 * @param o_end
	 * @param n_end
	 * @param o_v
	 * @param n_v
	 */
	private void split_contact(double n_start, double o_end, double n_end, Vertex o_v, double epslon) {
		Vertex n_v;
		o_v.set_end(n_start);	// reduce original
		if((n_start + epslon * .001) < o_end) { // needs new vertices at the end
			n_v = new Vertex(o_v, n_end, o_end);
			vertices.put(n_v.get_id(), n_v);
			edges.put(n_v.get_id(), new LinkedList<>());
			for (Edge e: edges.get(o_v.get_id())) {
				edges.get(n_v.get_id()).add(new Edge(n_v, e.get_dst_vertex()));
			}
			// copy the edges that arrived at the original node to the new created one
			List<Edge> to_add = new LinkedList<>();
			for (Map.Entry <String, List<Edge>> entry : edges.entrySet()) {
				for (Edge e: entry.getValue()) {
					if (e.get_dst_vertex().equals(o_v)) { 
						to_add.add(new Edge(e.get_src_vertex(), n_v));
					}
				}
			}

			for (Edge e: to_add) {
				edges.get(e.get_src_vertex().get_id()).add(e);
			}
		}		
	}
	
	/**
	 * Update the channel capacity through path p for message m.
	 * Contacts smaller than epslon (milliseconds) are discarded
	 * 
	 * When a contact is reduced (consumed) it might create another contact
	 * We need therefore to fix:
	 * 	vertices map:
	 * 		the existent vertice has its end reduced 
	 * 		the new vertice (if existent) must be added to the vertices map
	 * 	edges map:
	 * 		if there is a new contact:
	 * 			copy the edges parting from the original to it
	 * 			find out the source of the edges that arrive on it and add an edge to the new
	 * 
	 * ps.: The capacity should have been taking in account in the shortest path calculation.
	 * therefore, if the path do not support the message size, we have a bug.
	 * @param p	Path returned from RouteSearch
	 * @param m	Message to be sent
	 */
	
	public void consume_path(Path p, Message m, double epslon) {
		int msize = m.getSize();
		double comm_start=0.0;
		double original_end;
		double transmission_time;
		double comm_ends;
		
		List<Vertex> path_as_list = p.get_path_as_list();
		List<Vertex> fragmented_vertices = new LinkedList<>();
		
		for (Vertex v : path_as_list) {
			comm_start = Math.max(comm_start, v.adjusted_begin()); // time when transmission takes place
			// TODO: iri move the round function to another place, together with a contains for tuple
			comm_ends = ContactPlanHandler.round(comm_start + (double)msize/v.get_transmission_speed(), 2); 
			original_end = v.end();
			if (comm_start > v.adjusted_begin()) { // split contact
				split_contact(comm_start, original_end, comm_ends, v, epslon);
			} else {
				v.set_adjusted_begin(comm_ends);
				comm_start = comm_ends;
			}
			
		}
	}
}
