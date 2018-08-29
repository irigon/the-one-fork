package routing.ocgr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.modelmbean.InvalidTargetObjectTypeException;

import core.DTNHost;
import routing.OCGRRouter;
import routing.cgr.Edge;
import routing.cgr.Graph;
import routing.cgr.Vertex;

public class Metrics {
	
	Graph graph;
	Map<Vertex, Map<String, Prediction>> predMap;
	Map<Vertex, Map<String, Capacity>>   capMap;

	public Metrics(Graph g) {
		graph = g;
		predMap = new HashMap<Vertex, Map<String, Prediction>>();
		capMap = new HashMap<Vertex, Map<String, Capacity>>();
	}

	/**
	 * An new contact was found; add to the graph, create edges and metrics.
	 * 
	 * TODO: iri when are the vertexes pruned?
	 * 
	 * @param v The vertice created from the new contact
	 */
	private void addVerticeAndEdgesToGraph(Vertex v) {
		graph.get_vertice_map().put(v.get_id(), v);
		for (Vertex peer_v : graph.get_vertice_map().values()) {
			if (v.get_common_host(peer_v) != null && !v.equals(peer_v)) {			
				graph.add_edge(v, peer_v);
				graph.add_edge(peer_v, v);
			}
		}
		addMetrics(v);
	}
	
	private void addMetrics(Vertex v) {
		addCapacity(v, new BufferSizeCapacity(v));
		addPrediction(v, new BufferFreeStdDev(v));
		addPrediction(v, new BufferCapacityAvg(v, 20));
		addPrediction(v, new AvgTimeBetweenContactsPrediction(v));
	}
	
	/**
	 * Find all vertices that this Vertex knows about and add it to the map
	 * Find all new connections I don't know about and add to map
	 * @param v
	 */
	private void extendVerticesAndEdgesToGraph(OCGRRouter r) {
		Metrics m = r.getMetrics();
		for (Vertex v : m.graph.get_vertice_map().values()) {
			if (!graph.get_vertice_map().containsKey(v.get_id())) {
				graph.get_vertice_map().put(v.get_id(), v);
				addCapacity(v, new BufferSizeCapacity(v));
				for (Capacity c : capMap.get(v).values()) {
					c.update();
				}
			}
		}
		List<Edge> toAdd = new ArrayList<>();
		for (List<Edge> elist : m.graph.get_edges().values()) {
			for (Edge e : elist) {
				if (graph.get_edges().get(e.get_src_id()) == null || !graph.get_edges().get(e.get_src_id()).contains(e)) {
					toAdd.add(e);
				}
			}
		}
		for (Edge e : toAdd) {
			graph.add_edge(e.get_src_vertex(), e.get_dst_vertex());
		}
	}
	
	private void addCapacity(Vertex v, Capacity c) {
		if (capMap.get(v) == null) {
			capMap.put(v, new HashMap<String, Capacity>());			
		}
		capMap.get(v).put(c.getName(), c);	
	}

	private void addPrediction(Vertex v, Prediction p) {
		if (predMap.get(v) == null) {
			predMap.put(v, new HashMap<String, Prediction>());			
		}
		predMap.get(v).put(p.getName(), p);	
	}

	/**
	 * Another host was detected to be within range
	 * 
	 * 1) Verify if peer is known; 
	 * 	if not, create a node in graph and init structures.
	 *  otherwise, update predictions
	 * 
	 * @param thisHost The DTNHost being executed
	 * @param otherHost DTNHost peer that become within range
	 * @throws InvalidTargetObjectTypeException 
	 */
	public void connUp(Vertex v, DTNHost otherHost) {
		/** If a new vertex is found, add to graph, create edges and metrics **/
		if (!graph.get_vertice_map().containsKey(v.get_id())) {
			addVerticeAndEdgesToGraph(v);
			for (Capacity cap : capMap.get(v).values()) {
				cap.update();
			}
		}
		/** update metrics **/
		OCGRRouter otherRouter = (OCGRRouter)otherHost.getRouter();
		extendVerticesAndEdgesToGraph(otherRouter);
		Metrics otherMetrics = otherRouter.getMetrics();
		Map<String, Prediction> otherPredMap = otherMetrics.getPredictionsFor(v);
		// TODO: iri assert that nodes do not get otherPredMap null after initialization
		if (otherPredMap == null) { //other node predictions is not initialized, ignore
			System.out.println("Ignoring not initialized map on " + otherRouter.toString());
			return;
		} 
		for (Entry<String, Prediction> pred : otherPredMap.entrySet()) {
			pred.getValue().connUp();
		}
	}



	/**
	 * Another host was detected to become out of range (end of contact)
	 * 
	 * 1) Assert that the peer was known 
	 *   update predictions
	 * 
	 * @param thisHost The DTNHost being executed
	 * @param otherHost DTNHost peer that become within range
	 */
	public void connDown(Vertex v, DTNHost otherHost) {
		/** update metrics **/
		OCGRRouter otherRouter = (OCGRRouter)otherHost.getRouter();
		for (Entry<String, Prediction> pred : otherRouter.getMetrics().getPredictionsFor(v).entrySet()) {
			pred.getValue().connDown();
		}		
	}
	
	public int size() {
		return predMap.size() + capMap.size();
	}

	public Map<String, Prediction> getPredictionsFor(Vertex v) {
		return predMap.get(v);
	}
	
	public List<String> getMetrics(){
		List<String> metricList = new ArrayList<String>();
		for (Vertex v : predMap.keySet()) {
			String thisMetric = "";
			for (Prediction pred : predMap.get(v).values()) {
				thisMetric += pred.toString();
			}
			for (Capacity cap : capMap.get(v).values()) {
				thisMetric += cap.toString();
			}
			metricList.add(v.get_id() + " -- " + thisMetric);
		}
		return metricList;
	}
}
