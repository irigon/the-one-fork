package test;

import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import routing.OCGRRouter;
import routing.ocgr.RouteSearch;
import routing.ocgr.Vertex;
import routing.ocgr.metrics.Capacity;
import routing.ocgr.metrics.Prediction;

public class OCGRRouterTest extends AbstractCGRRouterTest {

	private static int TTL = 300;
	protected static final String BSIZE_S = "bufferSize";



	@Override
	public void setUp() throws Exception {
		ts.setNameSpace(null);
		ts.putSetting(BSIZE_S, ""+BUFFER_SIZE);
		setRouterProto(new OCGRRouter(ts));
		super.setUp();
		clock.setTime(0.0);
	}

	/*
	 * Reflection
	 */
	private Object get_private(String fname, RouteSearch rname)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field edges_reflect = RouteSearch.class.getDeclaredField(fname);
		edges_reflect.setAccessible(true);
		return edges_reflect.get(rname);
	}

	private Vertex call_route_search_relax(RouteSearch rs, Vertex v, Message m)
			throws ReflectiveOperationException, RuntimeException {
		Class[] argClasses = { Vertex.class, Message.class, List.class };
		Method method = RouteSearch.class.getDeclaredMethod("relax", argClasses);
		method.setAccessible(true);
		List<DTNHost> blacklist = m.getHops();
		for (DTNHost h : v.get_hosts()) {
			blacklist.remove(h);
		}
		final Object[] parameters = { v, m, blacklist };
		return (Vertex) method.invoke(rs, parameters);
	}

	private void call_prune(RouteSearch rs, double now) throws ReflectiveOperationException, RuntimeException {
		Class[] argClasses = { double.class };
		Method method = RouteSearch.class.getDeclaredMethod("prune", argClasses);
		method.setAccessible(true);
		final Object[] parameters = { now };
		method.invoke(rs, parameters);
	}

	private List<Object> create_and_init_pivot(RouteSearch rs, SortedSet<Vertex> v_to_connect, DTNHost h,
			boolean start) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class[] argClasses = { SortedSet.class, DTNHost.class, boolean.class };
		Method method = RouteSearch.class.getDeclaredMethod("create_pivot_and_initialize", argClasses);
		method.setAccessible(true);
		final Object[] parameters = { v_to_connect, h, start };

		List<Object> pivot_structure = (List<Object>)method.invoke(rs, parameters);
		return pivot_structure;
	}

	// private void init(Vertex pivot_begin, double now) {
	// public void initialize_route_search(RouteSearch rs, Message m, double now,
	// DTNHost host)
	public void initialize_route_search(RouteSearch rs, Vertex pivot_begin, double now)
			throws ReflectiveOperationException, RuntimeException {
		Class[] argClasses = { Vertex.class, double.class };
		Method method = RouteSearch.class.getDeclaredMethod("init", argClasses);
		method.setAccessible(true);
		final Object[] parameters = { pivot_begin, now };
		method.invoke(rs, parameters);
	}

	public SortedSet create_sorted_set_with_node(Vertex v) {
		SortedSet<Vertex> ss = new TreeSet<>(Comparator.comparing(Vertex::adjusted_begin).thenComparing(Vertex::get_id));
		ss.add(v);
		return ss;
	}
	
	/*
	 * Contact Tests
	 */
	public void testContactCreation() {

		// Contacts are ordered by name
		ts.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "10.0");

		assertEquals(c3, c1);

		// Verify cid creation
		assertEquals(c3.get_id(), "0_1_0.0_10.0");
		assertEquals(c2.get_id(), "0_1_100.0_110.0"); // ids are ordered before form the cid
		assertEquals(c3.begin(), 0.0);
		assertEquals(c3.end(), 10.0);
		assertEquals(c2.begin(), 100.0);
		assertEquals(c2.end(), 110.0);

		// Assert hash creation
		assertNotEquals(c3.hashCode(), 0);
	}
	

	/**
	 * Test a simple connection between two nodes
	 */
	public void testConnection() {
		assertEquals(h11.getConnections().size(), 0);
		assertEquals(h12.getConnections().size(), 0);
		h11.forceConnection(h12, null, UP);

		updateAllNodes();
		assertEquals(h11.getConnections().size(), 1);
		assertEquals(h12.getConnections().size(), 1);
		clock.advance(10);
		h11.forceConnection(h12, null, DOWN);
		assertEquals(h11.getConnections().size(), 0);
		assertEquals(h12.getConnections().size(), 0);
	}
	 
	/**
	 * Verify that:
	 * 1) the duration prediction is updated as expected upon a connection between peers
	 * 2) Buffer Capacity is set on the first contact
	 * 3) Buffer free capacity prediction is set from the first contact on
	 * 4) Avg Time Between contacts is set on the second contact and updated further
	 */
	public void testPredictionsAndCapacity() {
		// the first run sets the starting value (10.0)
		h11.forceConnection(h12, null, UP);
		updateAllNodes();
		clock.advance(10);
		h11.forceConnection(h12, null, DOWN);
		Vertex v11 = ((OCGRRouter)h11.getRouter()).getGraph().get_vertice_map().values().iterator().next();
		Vertex v12 = ((OCGRRouter)h12.getRouter()).getGraph().get_vertice_map().values().iterator().next();
		Map<String,Prediction> p11 = v11.get_metrics().getPredictions();
		Map<String,Prediction> p12 = v12.get_metrics().getPredictions();
		Map<String,Capacity> c11 = v11.get_metrics().getCapMap();
		Map<String,Capacity> c12 = v12.get_metrics().getCapMap();

		double predDuration11 = p11.get("DurationPrediction").getValue();
		double predDuration12 = p12.get("DurationPrediction").getValue();
		assertEquals(predDuration11, 10.0); 
		assertEquals(predDuration12, 10.0); 

		double predBufferFreePred11 = p11.get("BufferFreeCapacityPrediction").getValue();
		double predBufferFreePred12 = p12.get("BufferFreeCapacityPrediction").getValue();
		assertEquals(predBufferFreePred11, 100.0); 
		assertEquals(predBufferFreePred11, 100.0); 	

		double predBufferCap11 = c11.get("BufferSizeCapacity").getValue();
		double predBufferCap12 = c12.get("BufferSizeCapacity").getValue();
		assertEquals(predBufferCap11, 100.0); 
		assertEquals(predBufferCap12, 100.0);
		h11.forceConnection(h12, null, UP);

		// first run sets it to: 22
		double predTimeBetweenContacts11 = p11.get("AvgTimeBetweenContactsPred").getValue();
		double predTimeBetweenContacts12 = p12.get("AvgTimeBetweenContactsPred").getValue();
		assertEquals(predTimeBetweenContacts11, 10.0); 
		assertEquals(predTimeBetweenContacts12, 10.0); 	
		
		predBufferFreePred11 = p11.get("BufferFreeCapacityPrediction").getValue();
		predBufferFreePred12 = p12.get("BufferFreeCapacityPrediction").getValue();
		assertEquals(predBufferFreePred11, 100.0); 
		assertEquals(predBufferFreePred11, 100.0); 	


		//updateAllNodes();
		clock.advance(12);
		h11.forceConnection(h12, null, DOWN);
		// 0.8*10.0 + 0.2*12 = 10.4
		predDuration11 = p11.get("DurationPrediction").getValue();
		predDuration12 = p12.get("DurationPrediction").getValue();
		assertEquals(predDuration11, 10.4); // the first time, the value is set to the period
		assertEquals(predDuration12, 10.4); // the first time, the value is set to the period	

		// second run: 10 + (12-10)/3 = 10.67
		h11.forceConnection(h12, null, UP);
		predTimeBetweenContacts11 = p11.get("AvgTimeBetweenContactsPred").getValue();
		predTimeBetweenContacts12 = p12.get("AvgTimeBetweenContactsPred").getValue();
		assertEquals(predTimeBetweenContacts11, 10.67); 
		assertEquals(predTimeBetweenContacts12, 10.67); 	
	}
	
	/* 1) message forwarding without contact returns no path
	 * 2) message forward with incomplete information ignore path with insuficient information
	 * 3) message forward with complete information is calculated
	 */
	public void testMsgForwarding() {
		// 1) message forwarding without contact returns no path
		Message m1 = new Message(h11, h12, "crew", 1);
		h11.createNewMessage(m1);
		assertEquals(h11.getRouter().getMessageCollection().size(), 0);	
	
		// 2) message forward with incomplete information ignore path with insuficient information
		h11.forceConnection(h12, null, UP);
		h11.createNewMessage(m1);
		
		updateAllNodes();
		// the contact is created, but the begin and end == 0, since is the first time they met.
		assertEquals(h11.getRouter().getMessageCollection().size(), 0);	
		clock.advance(10);
		h11.forceConnection(h12, null, DOWN); // contact is in avg 10s large, even if it just happened once
		updateAllNodes();
		
		h11.createNewMessage(m1);
		// virtual frequency is still 0, we need at least two contacts 
		assertEquals(h11.getRouter().getMessageCollection().size(), 0);
		clock.advance(2);
		updateAllNodes();
		
		
		h11.forceConnection(h12, null, UP);
		updateAllNodes();
		clock.advance(8);
		h11.forceConnection(h12, null, DOWN); // contact is in avg 11s
		updateAllNodes();
		
		h11.createNewMessage(m1);
		assertEquals(h11.getRouter().getMessageCollection().size(), 1);

		
		updateAllNodes();
		// The message should be sent on the next opportunity
		h11.forceConnection(h12, null, UP); 
		updateAllNodes();
		clock.advance(8);
		h11.forceConnection(h12, null, DOWN); 
		updateAllNodes();
		assertEquals(h11.getRouter().getMessageCollection().size(), 0);		
	}
	
	/**
	 * 
	 * Verify that the Average time between contactsis updated as predicted upon a connection between peers
	 *
	 */
	
	public void testPredictionsAverageBetweenContacts() {
		for (int i = 0; i<3; i++) {
			h11.forceConnection(h12, null, UP);
			updateAllNodes();
			clock.advance(10);
			h11.forceConnection(h12, null, DOWN);
			updateAllNodes();
			clock.advance(10);
			updateAllNodes();
		}

		Vertex v11 = ((OCGRRouter)h11.getRouter()).getGraph().get_vertice_map().values().iterator().next();
		Vertex v12 = ((OCGRRouter)h12.getRouter()).getGraph().get_vertice_map().values().iterator().next();
		Map<String,Prediction> p11 = v11.get_metrics().getPredictions();
		Map<String,Prediction> p12 = v12.get_metrics().getPredictions();
		double predTimeBetweenContacts11 = p11.get("AvgTimeBetweenContactsPred").getValue();
		double predTimeBetweenContacts12 = p12.get("AvgTimeBetweenContactsPred").getValue();
		assertEquals(predTimeBetweenContacts11, 20.0); 
		assertEquals(predTimeBetweenContacts12, 20.0); 	

	}

	public void testUpdateVertexCapacity() {
		for (int i = 0; i<3; i++) {
			h11.forceConnection(h12, null, UP);
			updateAllNodes();
			clock.advance(10);
			h11.forceConnection(h12, null, DOWN);
			updateAllNodes();
			clock.advance(10);
			updateAllNodes();
		}
		
		Message m1 = new Message(h11, h12, "crew", 1);
		h11.createNewMessage(m1);
		// message was created
		assertEquals(h11.getRouter().getMessageCollection().size(), 1);

	}
}
