package routing.cgr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		if (graph == null) {
			vertices = new HashMap<>();
			edges = new HashMap<>();
		} else {
			vertices = new HashMap<>(makeMapCopy(graph.vertices));
			edges = new HashMap<>(graph.edges);
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
		Vertex new_v;
		
		List<Vertex> path_as_list = p.get_path_as_list();
		List<Vertex> fragmented_vertices = new LinkedList<>();
		
		for (Vertex v : path_as_list) {
			comm_start = Math.max(comm_start, v.adjusted_begin()); // time when transmission takes place
			comm_ends = comm_start + (double)msize/v.get_transmission_speed(); 
			original_end = v.end();
			if (comm_start > v.adjusted_begin()) { // split contact
				v.set_end(comm_start);	// reduce original
				if((comm_ends + epslon * .001) < original_end) { // needs new vertices at the end
					new_v = new Vertex(v, comm_ends, original_end);
					vertices.put(new_v.get_id(), new_v);
					edges.put(new_v.get_id(), new LinkedList<>());
					for (Edge e: edges.get(v.get_id())) {
						edges.get(new_v.get_id()).add(e);
					}
					// copy the edges that arrived at the original node to the new created one
					List<Edge> to_add = new LinkedList<>();
					for (Map.Entry <String, List<Edge>> entry : edges.entrySet()) {
						String key = null;
						for (Edge e: entry.getValue()) {
							if (e.get_dst_vertex() == v) { 
								key = entry.getKey();
								to_add.add(new Edge(e.get_src_vertex(), new_v));
							}
						}
						if (key != null) {
							edges.get(key).addAll(to_add);
						}
					}
				}
			} else {
				v.set_adjusted_begin(comm_ends);
				comm_start = comm_ends;
				
			}
			
		}
	}
}
