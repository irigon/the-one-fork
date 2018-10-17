package routing.ocgr.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.modelmbean.InvalidTargetObjectTypeException;

import core.DTNHost;
import core.SimClock;
import routing.ocgr.TransmissionSpeed;
import routing.ocgr.Vertex;

public class Metrics {
	Map<String, Prediction> predMap;
	Map<String, Capacity>   capMap;
	Vertex vertice;

	public Metrics(Map<String, Prediction> pM, Map<String, Capacity> cM) {
		predMap = pM;
		capMap = cM;
	}

	public static Metrics create_metrics() {
		Map<String, Prediction> pM = new HashMap<String, Prediction>();
		Map<String, Capacity> cM = new HashMap<String, Capacity>();
		return new Metrics(pM, cM);
	}

	public void init_vertice(Vertex v) {
		vertice = v;
		addMetrics(v);
	}
	
	public void update_capacities() {
		for (Capacity cap: capMap.values()) {
			cap.update();
		}
	}
	
	
	/**
	 * An new contact was found; add to the graph, create edges and metrics.
	 * 
	 * TODO: iri when are the vertexes pruned?
	 * 
	 * @param v The vertice created from the new contact
	 */
//	private void addVerticeAndEdgesToGraph(Vertex v) {
//		graph.get_vertice_map().put(v.get_id(), v);
//		for (Vertex peer_v : graph.get_vertice_map().values()) {
//			if (v.get_common_host(peer_v) != null && !v.equals(peer_v)) {			
//				graph.add_edge(v, peer_v);
//				graph.add_edge(peer_v, v);
//			}
//		}
//		addMetrics(v);
//	}
	
	private void addMetrics(Vertex v) {
		addCapacity(new BufferSizeCapacity(v));
		addCapacity(new TransmissionSpeed(v));
		addPrediction(new BufferFreeCapacityPrediction(v));
		addPrediction(new AvgTimeBetweenContactsPrediction(v));
		addPrediction(new DurationPrediction(v));
	}
	
	/**
	 * Find all vertices that this Vertex knows about and add it to the map
	 * Find all new connections I don't know about and add to map
	 * @param v
	 */
//	private void extendVerticesAndEdgesToGraph(OCGRRouter r) {
//		Metrics m = r.getMetrics();
//		for (Vertex v : m.graph.get_vertice_map().values()) {
//			if (!graph.get_vertice_map().containsKey(v.get_id())) {
//				graph.get_vertice_map().put(v.get_id(), v);
//				addCapacity(v, new BufferSizeCapacity(v));
//				for (Capacity c : capMap.get(v).values()) {
//					c.update();
//				}
//			}
//		}
//		List<Edge> toAdd = new ArrayList<>();
//		for (List<Edge> elist : m.graph.get_edges().values()) {
//			for (Edge e : elist) {
//				if (graph.get_edges().get(e.get_src_id()) == null || !graph.get_edges().get(e.get_src_id()).contains(e)) {
//					toAdd.add(e);
//				}
//			}
//		}
//		for (Edge e : toAdd) {
//			graph.add_edge(e.get_src_vertex(), e.get_dst_vertex());
//		}
//	}
	
	private void addCapacity(Capacity c) {
		capMap.put(c.getName(), c);	
	}

	private void addPrediction(Prediction p) {
		predMap.put(p.getName(), p);	
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
//		if (!graph.get_vertice_map().containsKey(v.get_id())) {
//			addVerticeAndEdgesToGraph(v);
//			for (Capacity cap : capMap.get(v).values()) {
//				cap.update();
//			}
//		}
//		
//		/** update metrics **/
//		OCGRRouter otherRouter = (OCGRRouter)otherHost.getRouter();
//		extendVerticesAndEdgesToGraph(otherRouter);
//		Metrics otherMetrics = otherRouter.getMetrics();
//
//		if (getPredictionsFor(v) != null) {
//			for (Prediction p : getPredictionsFor(v).values()) {
//				p.connUp();
//			}
//		}
//		
//		transitivePredUpdates(otherMetrics, v);
//		transitiveCapUpdates(otherMetrics, v);

	}
	
//	public void transitivePredUpdates(Metrics m, Vertex v) {
//		// for every vertice known by the neighbor
//		for (Vertex ov : m.predMap.keySet()) {
//			// update capacity prediction
//			if (ov.equals(v)) { // ignore predictions about this node
//				continue;
//			}
//			Map<String, Prediction> otherPredMap = m.getPredictionsFor(ov);
//			// TODO: iri assert that nodes do not get otherPredMap null after initialization
//			if (otherPredMap == null) { //other node predictions is not initialized, ignore
//				return;
//			} 
//			for (Map.Entry<String, Prediction> pred : otherPredMap.entrySet()) {
//				addPrediction(pred.getValue().getVertex(), pred.getValue());
//			}		
//			
//			/* uses caps and predictions, and therefore not either of them 
//			 * and must be update separately
//			 **/
//			ov.set_pred_utilization();
//		}
//	}
//
//	// considering that capacity do not change in runtime
//	public void transitiveCapUpdates(Metrics m, Vertex v) {
//		for (Vertex ov : m.predMap.keySet()) {
//			if (ov.equals(v)) { // ignore predictions about this node
//				continue;
//			}
//			Map<String, Capacity> otherCapMap = m.getCapacitiesFor(ov);
//			if (otherCapMap == null) { //other node predictions is not initialized, ignore
//				return;
//			} 
//			for (Map.Entry<String, Capacity> cap : otherCapMap.entrySet()) {
//				if (capMap.containsKey(cap.getKey())) {
//					continue;
//				}
//				addCapacity(cap.getValue().getVertex(), cap.getValue());
//			}
//		}
//	}


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
//		OCGRRouter otherRouter = (OCGRRouter)otherHost.getRouter();
//		for (Entry<String, Prediction> pred : otherRouter.getMetrics().getPredictionsFor(v).entrySet()) {
//			pred.getValue().connDown();
//		}		
	}
	
	public int size() {
		return predMap.size() + capMap.size();
	}

	public Map<String, Prediction> getPredictions() {
		return predMap;
	}
	
	public Map<String, Capacity> getCapMap() {
		return capMap;
	}
	
	/*
	 * For every vertex pv knows about
	 * if its version is newer than mine:
	 *     updateMineVersion()
	 * */
	public void transitiveUpdate(Vertex pv) {
		for (Prediction p : predMap.values()) {
			Prediction remote_pred = pv.get_metrics().getPredictions().get(p.getName());
			double remote_timestamp = remote_pred.getTimestamp();
			if (remote_timestamp < p.getTimestamp()) {
				p.setValue(remote_pred.getValue());
				p.setTimestamp(remote_timestamp);
			}
		}
	}
	
	public List<String> getMetrics(){
		String thisMetric = "";
		List<String> metricList = new ArrayList<String>();
		for (Prediction pred : predMap.values()) {
			thisMetric += pred.toString() + " ";
		}
		for (Capacity cap : capMap.values()) {
			thisMetric += cap.toString() + " ";
		}
		metricList.add(vertice.get_id() + " " + thisMetric);
		return metricList;
	}
}
