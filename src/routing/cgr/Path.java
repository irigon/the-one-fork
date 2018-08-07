package routing.cgr;

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
	public List<Vertex> construct(Vertex last, Map<Vertex, Vertex> predecessors){
		this.init();
		if (!last.is_pivot()) { // on success the last pivot is returned
			return path;
		}
		Vertex v = last;
		while ((v = predecessors.get(v)) != null) {
			path.add(v);
		}
		Collections.reverse(path);
		if (path.size() > 0) { // if there is a pivot_begin, remove it.
			path.remove(0);
		}
		return path;
	}
	
	public List<Vertex> get_path_as_list(){
		return path;
	}
}
