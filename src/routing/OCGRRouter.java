/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 * The Original PRoPHET code updated to PRoPHETv2 router
 * by Samo Grasic(samo@grasic.net) - Jun 2011
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.cgr.Contact;
import routing.cgr.Edge;
import routing.cgr.Graph;
import routing.cgr.Path;
import routing.cgr.RouteSearch;
import routing.cgr.Vertex;
import routing.ocgr.Capacity;
import routing.ocgr.Metrics;
import routing.ocgr.Prediction;
import routing.util.RoutingInfo;
import util.Tuple;

/**
 * Implementation of PRoPHETv2" router as described in
 * http://tools.ietf.org/html/draft-irtf-dtnrg-prophet-09
 */
public class OCGRRouter extends ActiveRouter {
	Random randomGenerator = new Random();

	/** Prophet router's setting namespace ({@value})*/
	public static final String OCGR_NS = "OCGRRouter";

	private static final String NEXT_CONTACT = "contact";
	private static final String STARTING_TIME = "starting_time";


	/** Every node has its own view of the world **/
	private Graph cg;

	/** Vertex predictability **/
	private Metrics metrics;

	private RouteSearch route_search;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public OCGRRouter(Settings s) {
		super(s);
		Settings ocgrSettings = new Settings(OCGR_NS);
		cg = createGraph();
		metrics = new Metrics(cg);
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected OCGRRouter(OCGRRouter r) {
		super(r);
		cg = new Graph(cg);
		metrics = new Metrics(cg);
	}

	
	private Graph createGraph() {
		Map<String, Vertex> vertices = new HashMap<String, Vertex>();
		Map<String, List<Edge>> edges = new HashMap<String, List<Edge>>();
		return new Graph(vertices, edges);
	}
	
	@Override
	public void changedConnection(Connection con) {
		DTNHost otherHost = con.getOtherNode(getHost());
		assert otherHost.getRouter() instanceof OCGRRouter :
			"OCGRRouter only works with other routers of same type";

		Contact c = new Contact (getHost(), otherHost, 0.0, 0.0);
		String vid = "vertex_" + c.get_id();
		Vertex v = new Vertex(vid, c, false);

		/**
		 * When a connection is up:
		 * 		if the node is not on the graph:
		 * 			- add local vertex
		 * 			- update vertex capabilities
		 * 		search on the other router for unknown vertices
		 * 			- add
		 * 			- update capabilities
		 * **/
		if (con.isUp()) {
			if (!cg.get_vertice_map().containsKey(v.get_id())) {
				cg.addVerticeAndEdgesToGraph(v);
				cg.get_vertice_map().get(v.get_id()).update_caps();
			}
			
			/** update metrics **/
			OCGRRouter otherRouter = (OCGRRouter)otherHost.getRouter();
			cg.extendVerticesAndEdgesToGraph(otherRouter);

//			for (Prediction p : cg.get_vertice_map().get(v.get_id()).get_preds().values()) {
//				p.connUp();
//			}			
			metrics.connUp(v, otherHost);
		} else {
			metrics.connDown(v, otherHost);	
			/** Delete transfered messages **/
			List<Message> toDelete = new ArrayList<>();
			for (Message m : getMessageCollection()) {
				DTNHost other = con.getOtherNode(getHost());
				if ((int)m.getProperty(NEXT_CONTACT) == other.getAddress()) {
					if (other.getMessageCollection().contains(m)) {
						// message was successfully transfered, delete it
						toDelete.add(m);
					} 
				}
			}
			for (Message m : toDelete) { 
				deleteMessage(m.getId(), true);
			}				
		}
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		this.tryAllMessagesToAllConnections();
	}
	
	public Metrics getMetrics() {
		return metrics;
	}
	
	public Graph getGraph() {
		return cg;
	}

	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(metrics.size() +
				" metrics(s)");

		for (String m : metrics.getMetrics()) {
			ri.addMoreInfo(new RoutingInfo(String.format("%s ", m)));
		}

		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		OCGRRouter r = new OCGRRouter(this);
		return r;
	}
	
	
	private Message tryMessageToConnection(Connection con, Message m, int next_hop_addr) {
		int peer_addr = con.getOtherNode(getHost()).getAddress();
		if (peer_addr == next_hop_addr) {
			if (startTransfer(m, con) == RCV_OK) {
				return m;	// accepted a message, don't try others
			}
		}
		return null;
	}
	
	/**
	 * For every message to be sent (respecting the queue order), send the first
	 * queued message to which we are connected to the next hop.
	 * 
	 * @param messages
	 *            List of messages queued to be sent
	 * @param connections
	 *            currently up
	 * 
	 */
	@Override
	protected Connection tryMessagesToConnections(List<Message> messages, List<Connection> connections) {
		for (int i = 0, n = messages.size(); i < n; i++) {
			Message m = messages.get(i);
			int next_hop_addr = (int) m.getProperty(NEXT_CONTACT);
			double starting_time = (double) m.getProperty(STARTING_TIME);
			// the message is scheduled for later on. Sending now could cause a buffered message
			// to be deleted before sent
			if (SimClock.getTime() < starting_time) {
				return null;
			}
			for (Connection c : connections) {
				if (tryMessageToConnection(c, m, next_hop_addr) != null) {
					return c;
				}
			}
		}
		return null;
	}

	
	@Override
	public boolean createNewMessage(Message m) {
		if (getFreeBufferSize() > m.getSize() && isMessageDeliverable(m)) {
			return super.createNewMessage(m);
		}
		return false;
	}

	void set_message_next_hop(Message m, int address, double start_time) {
		if (m.getProperty(NEXT_CONTACT) != null)
			m.updateProperty(NEXT_CONTACT, address);
		else
			m.addProperty(NEXT_CONTACT, address);
		if (m.getProperty(STARTING_TIME) != null)
			m.updateProperty(STARTING_TIME, start_time);
		else
			m.addProperty(STARTING_TIME, start_time);
	}
	
	/**
	 * go to each vertex and reset the adjusted_begin to the predicted value
	 */
	private void reset_capacity() {
		for (Vertex v : cg.get_vertice_map().values()) {
			if (v.is_pivot()) {
				continue;
			}
			Map<String, Prediction> predMap = metrics.getPredictionsFor(v);
			if (predMap == null) { continue; }
			
			Prediction capPred = predMap.get("DurationPrediction");
			if (capPred == null) { continue; }
			
			v.set_adjusted_begin(v.begin() + capPred.getValue());
		}
	}
	
	/**
	 * if the destination host is reachable a cgr exists and contains a route which
	 * starts in the future
	 * 
	 * @param m
	 *            the to be delivered message
	 * @return true if the message is deliverable to its destination host
	 */
	boolean isMessageDeliverable(Message m) {
		boolean result = false;
		double now = SimClock.getTime();
		// just create a new RouteSearch if some vertex changed
		if (cg.is_tainted()) {
			route_search = new RouteSearch(cg);
			route_search.set_distance_algorithm("fair_distribution");			
		}

		Vertex last_hop = route_search.search(getHost(), now, m, msgTtl);
		if (last_hop == null) {
			reset_capacity();
			last_hop = route_search.search(getHost(), now, m, msgTtl);
			if (last_hop == null) {
				return false;				
			}
		}
		Path path = route_search.get_path(last_hop);
		List<Vertex> path_list = path.get_path_as_list();
		if (path_list.size() > 0) {
			DTNHost next_hop = path_list.get(0).get_other_host(getHost());
			double start_time = Math.max(now, path_list.get(0).adjusted_begin());
			set_message_next_hop(m, next_hop.getAddress(), start_time);
			cg.consume_path(path, m, 0.01);
			result = true;
		}
		return result;
	}
	
	/**
	 * A node just completed a message transmission. 
	 * Recalculate path, exclude message if it is not reachable, set next hop otherwise.
	 * @param from	Host from who the router got this message
	 * @param m	Message to be sent
	 * @return return Message
	 */
    @Override
    public Message messageTransferred(String id, DTNHost from) {
    	Message m = super.messageTransferred(id, from);
    	if (m != null) {
    		from.getRouter().removeFromMessages(m.getId());
    		if (!isMessageDeliverable(m)) {
    			removeFromMessages(m.getId());
    		}
    	}
        return m;
    }

}
