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
	public List<Vertex> construct(Vertex pivot_end, Map<Vertex, Vertex> predecessors){
		this.init();
		if (pivot_end == null) {
			return path;
		}
		Vertex v = pivot_end;
		while ((v = predecessors.get(v)) != null) {
			path.add(v);
		}
		Collections.reverse(path);
		path.remove(0);
		return path;
	}
	
	public List<Vertex> get_path_as_list(){
		return path;
	}
}
