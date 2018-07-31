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
		//List<Vertex> reversed = path.subList(0, path.size()-1);
		Collections.reverse(path);

		return path.subList(1, path.size());
	}
	
	public List<Vertex> get_path_as_list(){
		return path;
	}
}
