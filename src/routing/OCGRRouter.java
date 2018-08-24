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
import routing.cgr.Edge;
import routing.cgr.Graph;
import routing.cgr.Vertex;
import routing.ocgr.Metrics;
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

	/** Every node has its own view of the world **/
	private Graph cg;

	/** Vertex predictability **/
	private Metrics metrics;

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
	 * Copyc onstructor.
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

		if (con.isUp()) {
			metrics.connUp(getHost(), otherHost);
		} else {
			metrics.connDown(getHost(), otherHost);			
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

		tryOtherMessages();
	}
	
	public Metrics getMetrics() {
		return metrics;
	}

	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages =
			new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			OCGRRouter othRouter = (OCGRRouter)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			// TODO: iri schedule the messages that were not scheduled yet, maybe because they
			// did not know about the destination when they were scheduled in first place
//			for (Message m : msgCollection) {
//				if (othRouter.hasMessage(m.getId())) {
//					continue; // skip messages that the other one has
//				}
//				if((othRouter.getPredFor(m.getTo()) >= getPredFor(m.getTo())))
//				{
//					messages.add(new Tuple<Message, Connection>(m,con));
//				}
//			}
		}

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
//		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}



	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(metrics.size() +
				" metrics(s)");

		for (String m : metrics.getMetrics()) {
			ri.addMoreInfo(new RoutingInfo(String.format("%s", m)));
		}

		top.addMoreInfo(ri);
		return top;
	}
	


	@Override
	public MessageRouter replicate() {
		OCGRRouter r = new OCGRRouter(this);
		return r;
	}
}
