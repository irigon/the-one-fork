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
	EstimatedContactCapacity ecc;
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

	private void addMetrics(Vertex v) {
		addCapacity(new BufferSizeCapacity(v));
		addCapacity(new TransmissionSpeed(v));
		addPrediction(new BufferFreeCapacityPrediction(v));
		addPrediction(new AvgTimeBetweenContactsPrediction(v));
		addPrediction(new DurationPrediction(v));
		ecc = new EstimatedContactCapacity(predMap.get("avgTimeBetweenContactsPred"), predMap.get("DurationPrediction"), v.get_transmission_speed());
	}
	
	/**
	 * Find all vertices that this Vertex knows about and add it to the map
	 * Find all new connections I don't know about and add to map
	 * @param v
	 */

	
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
			if (remote_timestamp > p.getTimestamp()) {
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
	
	public EstimatedContactCapacity ecc() {
		return ecc;
	}
	
	
}
