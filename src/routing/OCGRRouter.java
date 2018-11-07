/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 * The Original PRoPHET code updated to PRoPHETv2 router
 * by Samo Grasic(samo@grasic.net) - Jun 2011
 */
package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.ocgr.Contact;
import routing.ocgr.Edge;
import routing.ocgr.Graph;
import routing.ocgr.Path;
import routing.ocgr.RouteSearch;
import routing.ocgr.Vertex;
import routing.ocgr.metrics.Metrics;
import routing.util.RoutingInfo;

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
	private boolean oldTransferState = false;


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
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected OCGRRouter(OCGRRouter r) {
		super(r);
		cg = new Graph(cg);
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
		Vertex v_tmp = new Vertex(vid, c, Metrics.create_metrics(), false);
		if (!cg.has_vertice(v_tmp.get_id())) {
			add_vertice(v_tmp);
		}
		
		Vertex v = cg.get_vertice_map().get(v_tmp.get_id());

		assert(v != null);
		
		/**
		 * When a connection is up:
		 * 		for each vertice V on the neighbors graph:
		 * 			if V is not known locally:
		 * 				clone vertice and add locally
		 *			if vertice is != v:
		 *				local_v = local_graph.get_vertice(V)
		 *				local_v.updatePreds(V) // update newer predictions by transitivity
		 * **/
		if (con.isUp()) {
			v.connUp();
			/* Update graph based on vertices discovered through the peer */ 
			OCGRRouter oR = (OCGRRouter)otherHost.getRouter();
			Graph oG = oR.getGraph();
			for (Vertex ov : oG.get_vertice_map().values()) {
				/* create a vertex locally if a new vertice is found */
				if (!cg.has_vertice(ov.get_id())) {
					Vertex new_v = ov.hybrid_clone();
					/* I am supposing the capacity can be calculated from the cloned vertice */
					add_vertice(new_v);
				}
				// update transitively other vertices predictions if needed
				if (!v.get_id().equals(ov.get_id()) && !ov.is_pivot()) {
					Vertex local_vertex = cg.get_vertice_map().get(ov.get_id());
					local_vertex.updatePreds(ov);
				}
			}
		} else {
			v.connDown();
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

	/**
	 * Verify if the vertex v is present on the graph cg
	 * If not, add vertice and edges and set/update capacity
	 * 
	 * @param v Vertice to be verified
	 */
	public void add_vertice(Vertex v) {
		cg.addVerticeAndEdgesToGraph(v);
		v.update_caps(); // capacity is updated once
	}

	public void clone_vertice_if_not_present(Vertex v) {
		if (!cg.has_vertice(v.get_id())) {
			cg.addVerticeAndEdgesToGraph(new Vertex(v));
			v.update_caps(); 	    // capacity is updated once
		}
	}

	
	@Override
	public void update() {
		super.update();
		// if started or stop transfered, update connection prediction
		if (changedTransferState()) {
			// find out the vertex and update prediction
			oldTransferState = !oldTransferState;
		}
		
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		this.tryAllMessagesToAllConnections();
	}
	
	private boolean changedTransferState() {
		if ((isTransferring() && oldTransferState == false) || (!isTransferring() && oldTransferState == true)) {
			return true;
		}
		return false;
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
		RoutingInfo ri = new RoutingInfo(cg.get_vertice_map().size() +
				" vertices");

		for (Vertex v : cg.get_vertice_map().values()) {
			for (String m : v.get_metrics().getMetrics()) {
				ri.addMoreInfo(new RoutingInfo(String.format("%s ", m)));
			}
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
		route_search = new RouteSearch(cg);
		route_search.set_distance_algorithm("least_latency");			
//		route_search.set_distance_algorithm("min_hops");			

		Vertex last_hop = route_search.search(getHost(), now, m, msgTtl);
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
