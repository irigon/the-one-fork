package test;

import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import routing.OCGRRouter;
import routing.ProphetRouter;
import routing.ocgr.RouteSearch;
import routing.ocgr.Vertex;

public class OCGRRouterTest extends AbstractCGRRouterTest {

	private static int TTL = 300;
	protected static final String BSIZE_S = "bufferSize";


	@Override
	public void setUp() throws Exception {
		ts.setNameSpace(null);
		setRouterProto(new OCGRRouter(ts));
		super.setUp();
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
	 * Test Capabilities metrics
	 * 
	 * 1) 1 Vertice should be aware of its own capabilities
	 * 2) 2 Vertices graph come in contact to each other
	 * 		they must learn about their capabilities and 
	 * 
	 */
	public void testCapabilities() {
		Message m1 = new Message(h10, h11, "crew", 1);
		h10.createNewMessage(m1);
		
		OCGRRouter r10 = (OCGRRouter)h10.getRouter();
		OCGRRouter r11 = (OCGRRouter)h11.getRouter();
	}
}
