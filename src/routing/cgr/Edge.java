package routing.cgr;

public class Edge {

	private Vertex source;
	private Vertex destination;
	private String eid;

	public Edge(Vertex s, Vertex d) {
		source = s;
		destination = d;
		eid = source.get_id() + "_" + destination.get_id();
	}
	
	public String get_dest_id() {
		return destination.get_id();
	}
	
	public String get_src_id() {
		return source.get_id();
	}
	
	public double get_src_begin() {
		return source.begin();
	}
	
	public double get_src_end() {
		return source.end();
	}
	
	public double get_dst_begin() {
		return destination.begin();
	}
	
	public double get_dst_end() {
		return destination.end();
	}
	
	public Vertex get_dst_vertex() {
		return destination;
	}
	
	public Vertex get_src_vertex() {
		return source;
	}

	/**
	 * TODO: currently we are not using this code.
	 * Verify afterwards if we really need it.
     * necessary for the hashSet duplicate detection
     * @return has value for this edge
     */
    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + ((eid == null) ? 0 : eid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Edge other = (Edge) obj;
        return eid.equals(other.eid);
    }

    @Override
    public String toString() {
        return "edge_" + eid;
    }
}
