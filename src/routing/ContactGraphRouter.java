/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import routing.cgr.ContactPlanHandler;

/**
 * Contact Graph message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class ContactGraphRouter extends ActiveRouter {
	
	private DTNHost this_host;
	private boolean setup;
	private boolean create_cplan;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ContactGraphRouter(Settings s) {
		super(s);
		this_host = getHost();
		setup = false;
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ContactGraphRouter(ContactGraphRouter r) {
		super(r);
		this_host = r.this_host;
		setup = false;
	}
	
	/**
	 * - Run the normal ActiveRouter update
	 * - Set create_graph option if no graph is available
	 * - Load graph otherwise
	 */
	
	@Override
	public void update() {
		super.update();
		
		if (!setup) { 	// verify if we need a new contact plan
			if (ContactPlanHandler.get().has_contact_plan()) {
				create_cplan = false;	
				// TODO: iri read the vertices, create edges, construct graph()
			} else {
				create_cplan = true;
			}
			setup = true;
		} else if (create_cplan) {	// run simulation creating contact plan
			if (SimClock.getIntTime() >= SimScenario.getInstance().getEndTime() - 1) {
				ContactPlanHandler.get().finish_contactplan(getHost());
				ContactPlanHandler.get().save_contacts(getHost());
			}
		} else {	// contact plan available, run CGR
			
		}
//		if (isTransferring() || !canStartTransfer()) {
//			return; // transferring, don't try other connections yet
//		}
//
//		// Try first the messages that can be delivered to final recipient
//		if (exchangeDeliverableMessages() != null) {
//			return; // started a transfer, don't try others (yet)
//		}
//
//		// verify if there is a contact plan
//		this.tryAllMessagesToAllConnections();
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
	 * Finding out the interface "i" that do not belong to this host, the peer 
	 * host is the i.getHost();
	 *
	 * @param con Connection
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
			if (!con.isUp()) {
				// TODO: iri what should we do with messages that were not sent on the 
				// contact they were scheduled? -- enqueue to be re-route
				// add to statistics
			}
		}
	}
}
