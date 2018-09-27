package routing.ocgr;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import core.Message;
import routing.OCGRRouter;

public class Graph {

	private Map<String, Vertex> vertices;
	private Map<String, List<Edge>> edges;
	boolean tainted;
	
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
	
	public boolean is_tainted() {
		return tainted;
	}
	
	public void set_taint(boolean is_tainted) {
		tainted = is_tainted;
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
	
	public boolean has_vertice(String id) {
		return vertices.get(id) != null;
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
			for (Edge e: edges.get(o_v.get_id())) {
				add_edge(n_v, e.get_dst_vertex());
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
	 * Add edge from v1 to v2
	 * 
	 * @param v1 Source vertex
	 * @param v2 Destination vertex
	 */
	public void add_edge(Vertex v1, Vertex v2) {
		if (edges.get(v1.get_id()) == null) {
			edges.put(v1.get_id(), new LinkedList<>());
		}
		edges.get(v1.get_id()).add(new Edge(v1, v2));
	}
	
	/**
	 * Update the channel capacity through path p for message m.
	 * Contacts smaller than epslon (milliseconds) are discarded
	 * 
	 * A contact is consumed by increasing the amount of time to its adjusted_begin
	 * edges are not changed
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
			comm_start = v.adjusted_begin(); // time when transmission takes place
			// TODO: iri move the round function to another place, together with a contains for tuple
			comm_ends = ContactPlanHandler.round(comm_start + (double)msize/v.get_transmission_speed(), 2); 
			v.set_adjusted_begin(comm_ends);
		}
	}
	
	public void addVerticeAndEdgesToGraph(Vertex v) {
		vertices.put(v.get_id(), v);
		for (Vertex peer_v : vertices.values()) {
			if (v.get_common_host(peer_v) != null && !v.equals(peer_v)) {			
				add_edge(v, peer_v);
				add_edge(peer_v, v);
			}
		}
	}
	
	/**
	 * Find all vertices that the other router knows about and add it to the map
	 * Add edges to the new vertices
	 * @param 
	 */
	public void extendVerticesAndEdgesToGraph(OCGRRouter r) {
		Graph otherGraph = r.getGraph();
		for (Vertex v : otherGraph.get_vertice_map().values()) {
			if (!vertices.containsKey(v.get_id())) {
				addVerticeAndEdgesToGraph(v);
			}
		}
	}
}
