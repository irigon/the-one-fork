package test;

import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import interfaces.SimpleBroadcastInterface;
import routing.EpidemicRouter;
import routing.cgr.Contact;
import routing.cgr.Edge;
import routing.cgr.Graph;
import routing.cgr.RouteSearch;
import routing.cgr.Vertex;

public class CGRRouterTest extends AbstractCGRRouterTest {

	private static int TTL = 300;

	@Override
	public void setUp() throws Exception {
		setRouterProto(new EpidemicRouter(ts));
		super.setUp();
	}
	
	/*
	 * Contact Tests
	 */
	public void testContactCreation() {
		
		// Contacts are ordered by name
		ts.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "10.0");
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "15");

		assertEquals(c3, c1);
		
		// Verify cid creation
		assertEquals(c3.get_id(), "cid_h10_h11_0_0");
		assertEquals(c2.get_id(), "cid_h10_h11_100_0");
		assertEquals(c3.begin(), 0.0);
		assertEquals(c3.end(), 10.0);
		assertEquals(c2.begin(), 100.0);
		assertEquals(c2.end(), 101.0);
		
		// Assert hash creation
		assertNotEquals(c3.hashCode(), 0);
	}

	// Test contact transmission speed math 
	 
	public void test_get_transmission_speed() {

		h0.setLocation(new Coord(0.0, 0.0));
		h1.setLocation(new Coord(0.1, 0.0));
		
		// default transmission_speed is 10
		assertEquals(c3.get_transmission_speed(), 10);
		
		/* increasing the speed of the interfaces should increase the 
		*  contact speed
		*/
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "15");
		ts.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "3.0");

		DTNHost hx = utils.createHost(c0, "h10");
		DTNHost hy = utils.createHost(c0, "h11");
		h10.setLocation(new Coord(0.0, 0.0));
		h11.setLocation(new Coord(2.1, 0.0));
		
		Contact c4 = new Contact(hx, hy, 0.0, 10.0);
		assertEquals(c4.get_transmission_speed(), 15);
		
		/* 
		 * Moving one interface away, get speed 0
		 */
		hy.setLocation(new Coord(200.0, 0.0));
		Contact c3 = new Contact(hx, hy, 20.0, 30.0);
		assertEquals(c3.get_transmission_speed(), 0);
		
		/*
		 * Having several interfaces, the contact is made up the first
		 * pair of the same type in which the range is wide enough
		 */
		
		// adding another interface on nodes h10 and h11
		ts.setNameSpace("Interface2");
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "2");
		ts.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "100.0");
		
		NetworkInterface ni1 = new SimpleBroadcastInterface(ts);
		NetworkInterface ni2 = new SimpleBroadcastInterface(ts);
		ts.restoreNameSpace();
		 
		hx.getInterfaces().add(ni1);
		hy.getInterfaces().add(ni2);
		
		
		// create a contact c1, but 200.0 is still too far
		Contact c1 = new Contact(hx, hy, 30.0, 40.0);
		assertEquals(c1.get_transmission_speed(), 0);
		
		
		// approximate h10, so the distance stays in the transmit range
		hx.setLocation(new Coord(150.0, 0.0));
		Contact c2 = new Contact(hx, hy, 40.0, 50.0);
		assertEquals(c2.get_transmission_speed(), 2);
	}
	
	public void test_current_capacity() {
		assertEquals(c4.get_current_capacity(), (c4.end()-c4.adjusted_begin()) * c4.get_transmission_speed());
		assertEquals(c4.get_current_capacity(), 100.0);
	}
	
	public void test_get_other_host() {
		Contact c3 = new Contact(h0, h1, 0.0, 10.0);
		assertEquals(c3.get_other_host(h0), h1);
		
	}

	/*
	 * Vertex Tests
	 *
	 */
	
	// test vertex creation	
	public void test_vertex_creation() {
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "15");
		DTNHost hx = utils.createHost(c0, "h10");
		DTNHost hy = utils.createHost(c0, "h11");
		Contact cx = new Contact(hx, hy, 0.0, 10.0);
		Vertex v3 = new Vertex("vertex_test", cx, false);
		assertEquals(v3.get_transmission_speed(), 15);
		assertFalse(v3.is_pivot());
	}
	
	// test sender/receiver
	public void test_sender_receiver() {
		assertEquals(c3.begin(), 0.0);
		assertEquals(c3.end(), 10.0);
		assertNull(v3.get_receiver());
		assertNull(v3.get_sender());
		v3.set_receiver(h10);
		assertEquals(v3.get_sender(), h11);
		v3.set_receiver(h11);
		assertEquals(v3.get_sender(), h10);
		v3.set_sender(h10);
		assertEquals(v3.get_receiver(), h11);
		assertEquals(v3.get_sender(), h10);
	}
	
	// test is_pivot
	public void test_is_pivot() {
		assertFalse(v3.is_pivot());		
		assertTrue(v2.is_pivot());
	}
	
	// test get_hosts
	public void test_get_hosts() {
		assertEquals(Arrays.asList(h10, h11), v3.get_hosts());
	}
	
	// test get_common_host
	public void test_get_common_host() {
		Vertex v3 = new Vertex("vertex1", c3, false);
		Vertex v2 = new Vertex("vertex2", c4, false);
		assertEquals(h11, v3.get_common_host(v2));
	}

	/*
	 * Edge tests
	 */
	// test Edge creation
	public void test_edge_creation() {
		assertEquals(e2.get_src_begin(), 0.0);
		assertEquals(e2.get_src_end(), 10.0);
		assertEquals(e2.get_dst_begin(), 20.0);
		assertEquals(e2.get_dst_end(), 30.0);
		assertEquals(e2.get_src_id(), "vertex_3");
		assertEquals(e2.get_dest_id(), "vertex_4");
		assertEquals(e2.toString(), "edge_vertex_3_vertex_4");
	}
	
	/*
	 * Graph tests
	 */
	
	public void test_graph_creation() {

		Map<String, Vertex> vertices = new HashMap<>();
		vertices.put("vertex1", v3);
		vertices.put("vertex2", v4);
		vertices.put("vertex3", v5);
		Map<String, List<Edge>> edges = new HashMap<>();
		edges.put("vertex1", Arrays.asList(e1));
		edges.put("vertex2", Arrays.asList(e1, e3));
		edges.put("vertex3", Arrays.asList(e3));

		Graph g1 = new Graph(vertices, edges);

		// test clone
		Graph g2 = new Graph(g1);
		assertNotEquals(System.identityHashCode(g1), System.identityHashCode(g2));
		assertEquals(g2.get_edges().size(), g1.get_edges().size());
		assertEquals(g2.get_vertice_map().size(), g1.get_vertice_map().size());
		
	}

	/*
	 * Route search
	 */
	
	
	// allow checking private methods through reflection, from https://goo.gl/zGyG8e
	/**
	 * Set accessibility to public for testing purposes
	 * @param rs	RouteSearch Object
	 * @param m		Message to send
	 * @param now	Timestamp
	 * @param host	The host calculating the best path, the source host.
	 * @throws ReflectiveOperationException
	 * @throws RuntimeException
	 */
	public void initialize_route_search(RouteSearch rs, Message m, double now, DTNHost host)
			throws ReflectiveOperationException, RuntimeException {
		Class[] argClasses = { Message.class, double.class, DTNHost.class };
		Method method = RouteSearch.class.getDeclaredMethod("init", argClasses);
		method.setAccessible(true);
		final Object[] parameters = { m, now, host };
		method.invoke(rs, parameters);
	}
	
	public Vertex call_route_search_relax(RouteSearch rs, Vertex v, int size, int ttl)
			throws ReflectiveOperationException, RuntimeException {
		Class[] argClasses = {Vertex.class, int.class, int.class};
		Method method = RouteSearch.class.getDeclaredMethod("relax", argClasses);
		method.setAccessible(true);
		final Object[] parameters = {v, size, ttl};
		
		return (Vertex) method.invoke(rs, parameters);
	}


	public Vertex create_pivot(DTNHost receiver) {
		Contact c = new Contact(receiver, receiver, 0.0, Double.POSITIVE_INFINITY);
		Vertex pivot = new Vertex(c.get_id(), c, true);
		pivot.set_receiver(receiver);
		return pivot;
	}

	
	public void test_route_search_relax_phase() throws Exception, Exception {
		
		/* Some assumptions on the following tests*/
    	assertEquals(v3.get_transmission_speed(), 10);
    	assertEquals(v4.get_transmission_speed(), 10);

    	/*
		 * 1 vertex graph
		 */
		
		RouteSearch rs01 = new RouteSearch(g01);
		
		initialize_route_search(rs01, m01, 0.0, v4.get_hosts().get(0));
		//Vertex pivot_begin = create_pivot(v4, v4.get_sender());
		Vertex pivot_begin = create_pivot(v4.get_hosts().get(0));
		Vertex end;
		
    	// access private elements for testing. There is a broad discussion whether this is or not a bad practice
    	end = call_route_search_relax(rs01, pivot_begin, 10, 300);
    	assertNull(end);
    	
    	// after the first relax, the message can be forward after v4_start == 20.0
    	// since the message has size 10 and the speed is 10, the time when v4 is ready to transmit further is 21.0
    	assertEquals(rs01.get_distances().get(v4), 21.0);
    	
    	// now the only node in unsettled is v4, we can relax v4 and it should find the pivot_end
    	// the pivot start is zero, so the best starting time will be 21.0, it should take 0.0 to transmit,  
    	// since it is a pivot
    	end = call_route_search_relax(rs01, v4, 10, 300);
    	double dist = rs01.get_distances().get(v4);
    	assertEquals(rs01.get_distances().get(v4), 21.0);
    	Vertex pivot_end = create_pivot(v4.get_receiver());
    	assertNotNull(end);
    	
		/*
		 * 2 vertex graph
		 */
		
		RouteSearch rs02 = new RouteSearch(g02);
	    //private Vertex create_pivot_and_initialize(DTNHost h, double start_time, double end_time) {
		initialize_route_search(rs02, m02, 0.0, v3.get_hosts().get(0));
		pivot_begin = create_pivot(v3.get_hosts().get(0));
		DTNHost common_host = v4.get_common_host(v3);
		pivot_end = create_pivot(v4.get_other_host(common_host));

		// calling relax on pivot will find out vertex v3
    	assertEquals(rs02.get_distances().get(v3), Double.POSITIVE_INFINITY);
    	end = call_route_search_relax(rs02, pivot_begin, 10, 300);
    	assertNull(end);
    	assertEquals(rs02.get_distances().get(v3), 1.0);
    	assertEquals(rs02.get_distances().get(v4), Double.POSITIVE_INFINITY);
    	assertEquals(rs02.get_distances().get(pivot_end), Double.POSITIVE_INFINITY);

    	// the next unsettled to be called is v3 that will find p4 but not the pivot
    	end = call_route_search_relax(rs02, v3, 10, 300);
    	assertNull(end);
    	assertEquals(rs02.get_distances().get(v3), 1.0);
    	assertEquals(rs02.get_distances().get(v4), 21.0);
    	assertEquals(rs02.get_distances().get(pivot_end), Double.POSITIVE_INFINITY);

    	// the next unsettled to be called is v4 returning the pivot
    	end = call_route_search_relax(rs02, v4, 10, 300);
    	assertNotNull(end);
    	assertEquals(rs02.get_distances().get(v3), 1.0);
    	assertEquals(rs02.get_distances().get(v4), 21.0);
    	assertEquals(rs02.get_distances().get(pivot_end), 22.0);
	}
}
