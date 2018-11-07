package routing.ocgr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Path {
	List<Vertex> path;
	
	public Path () {
	}

	private void init() {
		if (path == null) {
			path = new ArrayList<>();
		}
		path.clear();
	}
	public List<Vertex> construct(Vertex last_vertice, Map<Vertex, Vertex> predecessors){
		Vertex v = last_vertice;
		this.init();
		if (last_vertice == null) { 
			return path;
		}
		path.add(last_vertice);
		while ((v = predecessors.get(v)) != null && !v.is_pivot()) {
			path.add(v);
		}
		Collections.reverse(path);
		return path;
	}
	
	public List<Vertex> get_path_as_list(){
		return path;
	}
}
