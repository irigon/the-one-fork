/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.NetworkInterface;
import core.Settings;
import routing.cgr.ContactPlanHandler;

/**
 * Contact Graph message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class ContactGraphRouter extends ActiveRouter {
	
	private DTNHost this_host;
	private ContactPlanHandler cph;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ContactGraphRouter(Settings s) {
		super(s);
		this_host = getHost();
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ContactGraphRouter(ContactGraphRouter r) {
		super(r);
		this_host = r.this_host;
	}

	
	/**
	 * - Run the normal ActiveRouter update
	 * - Set create_graph option if no graph is available
	 * - Load graph otherwise
	 */
	
	@Override
	public void update() {
		super.update();
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
	
	private DTNHost get_conn_peer(Connection con) {
		NetworkInterface iface = con.getOtherInterface(null);
		if (getHost().getInterfaces().contains(iface)) {
			iface = con.getOtherInterface(iface);
		}
		return iface.getHost();
	}
	
	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) { // a contact start
			cph.get().contactStarted(get_conn_peer(con), getHost());
		} else {
			cph.get().contactEnded(get_conn_peer(con), getHost());
		}
		
	}

}
