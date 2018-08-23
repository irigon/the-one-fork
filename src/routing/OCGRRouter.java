/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 * The Original PRoPHET code updated to PRoPHETv2 router
 * by Samo Grasic(samo@grasic.net) - Jun 2011
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Random;

import routing.util.RoutingInfo;


import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import util.Tuple;

/**
 * Implementation of PRoPHETv2" router as described in
 * http://tools.ietf.org/html/draft-irtf-dtnrg-prophet-09
 */
public class OCGRRouter extends ActiveRouter {
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_ALPHA = 0.8;
	Random randomGenerator = new Random();

	/** Prophet router's setting namespace ({@value})*/
	public static final String OCGR_NS = "OCGRRouter";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";

	/**
	 * Transitivity scaling constant (alpha) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_ALPHA}.
	 */
	public static final String ALPHA_S = "alpha";

	private double alpha;
	/** value of beta setting */

	/** Vertex predictability **/
	private Map<Vertex, Map<String, Prediction>> preds;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public OCGRRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(OCGR_NS);

		if (prophetSettings.contains(ALPHA_S)) {
			alpha = prophetSettings.getDouble(ALPHA_S);
		}
		else {
			alpha = DEFAULT_ALPHA;
		}

		initPreds();
	}

	/**
	 * Copyc onstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected OCGRRouter(OCGRRouter r) {
		super(r);
		this.alpha = r.alpha;
		initPreds();
	}

		/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.buff_preds = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateSizePredFor(otherHost);
			updateTransitiveSizePreds(otherHost);
		}
	}

	
	/**
	 * Updates buffer predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * ALPHA
	 * @param host The host we just met
	 */
	private void updateSizePredFor(DTNHost host) {
		double bOld = getBuffPredFor(host); 
		double local_bsize = getHost().getRouter().getBufferSize();
		double remote_bsize = host.getRouter().getBufferSize();
		
		
		double bNew = Math.min(local_bsize, remote_bsize);
		if (bOld != 0) {
			bNew = bOld * alpha + (1-alpha) * bNew;
		}
		buff_preds.put(host, bNew);
	}
		
	/**
 * Returns the current buffer size prediction (B) value for a host or 0 if entry for
 * the host doesn't exist.
 * @param host The host to look the B for
 * @return the current B value
 */
	public double getBuffPredFor(DTNHost host) {
		if (buff_preds.containsKey(host)) {
			return buff_preds.get(host);
		}
		else {
			return 0;
		}
	}

	/**
	 * Updates transitive (A->B->C) buffer size predictions.
	 * <CODE>B(a,c) = B(a,c)_old * ALPHA + (1 - ALPHA)*min(B(a),B(b,c))
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitiveSizePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		double local_bsize = getHost().getRouter().getBufferSize();

		assert otherRouter instanceof OCGRRouter :
			"PRoPHETv2 only works with other routers of same type";

		double pForHost = getBuffPredFor(host); // B(a,c)
		Map<DTNHost, Double> othersPreds =
			((OCGRRouter)otherRouter).getBufferPreds();

		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}
			double remote_bsize = e.getValue();

			double bNew = Math.min(local_bsize, remote_bsize);
			double bOld = getBuffPredFor(e.getKey()); // B(a,c)_old
			if (bOld != 0) {
				bNew = bOld*alpha + (1-alpha) * bNew;
			}
			buff_preds.put(e.getKey(), bNew);
		}
	}

	/**
	 * Returns a map of this router's buffer size predictions
	 * @return a map of this router's buffer size predictions
	 */
	private Map<DTNHost, Double> getBufferPreds() {
		return this.buff_preds;
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
		RoutingInfo ri = new RoutingInfo(buff_preds.size() +
				" prediction(s)");

		for (Map.Entry<DTNHost, Double> e : buff_preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();

			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f",
					host, value)));
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
