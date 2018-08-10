/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.ArrayList;
import java.util.List;

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
				route_search = new RouteSearch(cg);
			} else {
				create_cplan = true;
			}
			setup = true;
		} 
		if (create_cplan) { // run simulation creating contact plan
			if (SimClock.getIntTime() >= SimScenario.getInstance().getEndTime() - 1) {
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
			if (!con.isUp()) { //re-run dijkstra on the enqueued messages for this contact that were not sent
				//TODO: iri can we avoid to run this twice? It won't run twice, since the message is going to be 
				// either rescheduled or dropped
				List<Message> toDelete = new ArrayList<>();
				for (Message m : getMessageCollection()) {
					if (!isMessageDeliverable(m)) {
						toDelete.add(m);
					}
				}
				for (Message m : toDelete) {
					deleteMessage(m.getId(), true);
				}
			}
		}
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
			for (Connection c : connections) {
				int peer_addr = c.getOtherNode(getHost()).getAddress();
				if (peer_addr == next_hop_addr) {
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

	void set_message_next_hop(Message m, int address) {
		if (m.getProperty(NEXT_CONTACT) != null)
			m.updateProperty(NEXT_CONTACT, address);
		else
			m.addProperty(NEXT_CONTACT, address);
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
		// after initialization ttl is set to the max value, so we have to work around
		// to set the correct value
		// TODO: iri verify this
		int ttl = m.getTtl() != Integer.MAX_VALUE ? m.getTtl() : msgTtl;
		Vertex last_hop = route_search.search(getHost(), SimClock.getIntTime(), m);
		if (last_hop == null) {
			return false;
		}
		Path path = route_search.get_path(last_hop);
		List<Vertex> path_list = path.get_path_as_list();
		if (path_list.size() > 0) {
			DTNHost next_hop = path_list.get(0).get_other_host(getHost());
			set_message_next_hop(m, next_hop.getAddress());
			cg.consume_path(path, m, 0.01);
			result = true;
		}
		return result;
	}

}
