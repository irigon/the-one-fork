package routing.cgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

}
