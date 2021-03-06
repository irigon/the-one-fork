/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import routing.cgr.ContactPlanHandler;
import routing.cgr.Graph;
import routing.cgr.Path;
import routing.cgr.RouteSearch;
import routing.cgr.Vertex;
import routing.util.RoutingInfo;
import util.Tuple;

/**
 * Contact Graph message router with drop-oldest buffer and only single
 * transferring connections at a time.
 */
public class ContactGraphRouter extends ActiveRouter {

	private DTNHost this_host;
	private boolean setup;
	private boolean create_cplan;
	private Graph cg;
	private RouteSearch route_search;
	private static final String NEXT_CONTACT = "contact";
	private static final String STARTING_TIME = "starting_time";
	/** CGR router's setting namespace ({@value})*/
	public static final String CGR_NS = "ContactGraphRouter";
	/** CGR router's setting distance algorithm ({@value})*/
	public static final String CGR_DISTANCE_ALGO = "Distance";
	public static final String CGR_DEFAULT_DISTANCE_ALGO = "least_latency";
	/* avoid create plan on testing */
	public static final String CREATE_CPLAN = "create_cplan";


	/**
	 * Constructor. Creates a new message router based on the settings in the given
	 * Settings object.
	 * 
	 * @param s
	 *            The settings object
	 */
	public ContactGraphRouter(Settings s) {
		super(s);
		this_host = getHost();
		setup = false;
		if (s.contains(CREATE_CPLAN)) {
			create_cplan = s.getBoolean(CREATE_CPLAN);
		} else {
			create_cplan = true;
		}
	}

	/**
	 * Copy constructor.
	 * 
	 * @param r
	 *            The router prototype where setting values are copied from
	 */
	protected ContactGraphRouter(ContactGraphRouter r) {
		super(r);
		this_host = r.this_host;
		setup = false;
		create_cplan = r.create_cplan;
		cg = new Graph(cg);
		route_search = new RouteSearch(cg);
}

	
	/*TODO: find a way to allow to set route_search method for testing without using 
	 * public visibility
	 * */
	public void set_route_search(RouteSearch rs) {
		this.route_search = rs;
	}
	
	/**
	 * - Run the normal ActiveRouter update - Set create_graph option if no graph is
	 * available - Load graph otherwise
	 */

	@Override
	public void update() {
		super.update();

		if (!setup) { // verify if we need a new contact plan
			if (ContactPlanHandler.get().has_contact_plan()) {
				create_cplan = false;
				cg = ContactPlanHandler.get().load_graph();
				set_route_search(new RouteSearch(cg));
			} else {
				create_cplan = true;
			}
			setup = true;
		} 
		if (create_cplan) { // run simulation creating contact plan
			if (SimClock.getIntTime() == SimScenario.getInstance().getEndTime()) {
				ContactPlanHandler.get().finish_contactplan(getHost());
				ContactPlanHandler.get().save_contacts(getHost());
			}
		} else { // contact plan available, run CGR
			if (isTransferring() || !canStartTransfer()) {
				return; // transferring, don't try other connections yet
			}

			// Try first the messages that can be delivered to final recipient
			if (exchangeDeliverableMessages() != null) {
				return; // started a transfer, don't try others (yet)
			}

			// verify if there is a contact plan
			this.tryAllMessagesToAllConnections();
		}
	}

	@Override
	public ContactGraphRouter replicate() {
		return new ContactGraphRouter(this);
	}

	/**
	 * Find the other host connected to this one
	 * 
	 * Calling getOtherInterface with null as input, returns the "to" interface.
	 * Calling getOtherInterface with "to" as input, returns the "from" interface.
	 * Finding out the interface "i" that do not belong to this host, the peer host
	 * is the i.getHost();
	 *
	 * @param con
	 *            Connection
	 * @return The peer host
	 */
	private NetworkInterface get_peer_iface(Connection con) {
		NetworkInterface iface = con.getOtherInterface(null);
		if (getHost().getInterfaces().contains(iface)) {
			iface = con.getOtherInterface(iface);
		}
		return iface;
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		if (create_cplan) {
			/*
			 * Save infos about the interface for serialization later on
			 */
			NetworkInterface peer_iface = get_peer_iface(con);

			if (con.isUp()) { // a contact start
				ContactPlanHandler.get().set_contact_start_time(getHost(), peer_iface.getHost());
			} else {
				ContactPlanHandler.get().set_contact_end_time(getHost(), peer_iface.getHost());
			}
		} else {
			/*
			 * Connection got down: .
			 */
			if (!con.isUp()) { // conn down: re-run dijkstra on contacts that should have been sent 
				List<Message> toDelete = new ArrayList<>();
				for (Message m : getMessageCollection()) {
					DTNHost other = con.getOtherNode(getHost());
					if ((int)m.getProperty(NEXT_CONTACT) == other.getAddress()) {
						if (other.getMessageCollection().contains(m)) {
							// message was successfully transfered, delete it
							toDelete.add(m);
						} else {
							// what todo with messages that where scheduled but could not be sent?
						}
					}
				}
				for (Message m : toDelete) { 
					deleteMessage(m.getId(), true);
				}				
			}
		}
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
		if (!create_cplan && getFreeBufferSize() > m.getSize() && isMessageDeliverable(m)) {
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
		Vertex last_hop = route_search.search(getHost(), now, m, msgTtl);
		if (last_hop == null) {
			return false;
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
    
    
    /*
     * (?) verify if using OrderedSet instead of HashMap for distances would improve 
     * performance; This way, the least distances would be tried first and distances later than
     * TTL would be skipped at once.
     */
	@Override
	public RoutingInfo getRoutingInfo() {
		Map<Vertex, Double> distances = route_search.get_distances();
		Comparator<Tuple<Vertex, Double>> comparator = new Comparator<Tuple<Vertex, Double>>() {
			public int compare(Tuple<Vertex, Double> tupleA, Tuple<Vertex, Double> tupleB) {
				return tupleA.getValue().compareTo(tupleB.getValue());
			}
		};
		Set<Tuple<Vertex, Double>> ordered_set = new TreeSet<>(comparator);
		for (Map.Entry<Vertex, Double> e : distances.entrySet()) {
			ordered_set.add(new Tuple(e.getKey(), e.getValue()));
		}

		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(distances.size() +
				" distance prediction(s)");

		for (Tuple<Vertex, Double> t : ordered_set) {
			Vertex v = t.getKey();
			Double value = t.getValue();

			ri.addMoreInfo(new RoutingInfo(String.format("%s <--> %s   %s: %.6f",
					v.get_hosts().get(0), v.get_hosts().get(1), v.get_id(), value)));
		}

		top.addMoreInfo(ri);
		return top;
	}

}
