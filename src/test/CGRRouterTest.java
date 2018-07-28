package test;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Coord;
import core.DTNHost;
import core.NetworkInterface;
import interfaces.SimpleBroadcastInterface;
import routing.EpidemicRouter;
import routing.MessageRouter;
import routing.cgr.Contact;
import routing.cgr.Edge;
import routing.cgr.Graph;
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

		assertEquals(c1, c4);
		
		// Verify cid creation
		assertEquals(c1.get_id(), "cid_h10_h11_0_0");
		assertEquals(c5.get_id(), "cid_h10_h11_100_0");
		assertEquals(c1.begin(), 0.0);
		assertEquals(c1.end(), 10.0);
		assertEquals(c5.begin(), 100.0);
		assertEquals(c5.end(), 101.0);
		
		// Assert hash creation
		assertNotEquals(c1.hashCode(), 0);
	}

	// Test contact transmission speed math 
	 
	public void test_get_transmission_speed() {

		h0.setLocation(new Coord(0.0, 0.0));
		h1.setLocation(new Coord(0.1, 0.0));
		
		// default transmission_speed is 10
		assertEquals(c1.get_transmission_speed(), 10);
		
		/* increasing the speed of the interfaces should increase the 
		*  contact speed
		*/
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "15");
		ts.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "3.0");

		DTNHost hx = utils.createHost(c0, "h10");
		DTNHost hy = utils.createHost(c0, "h11");
		h10.setLocation(new Coord(0.0, 0.0));
		h11.setLocation(new Coord(2.1, 0.0));
		
		Contact c2 = new Contact(hx, hy, 0.0, 10.0);
		assertEquals(c2.get_transmission_speed(), 15);
		
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
		
		
		// create a contact c4, but 200.0 is still too far
		Contact c4 = new Contact(hx, hy, 30.0, 40.0);
		assertEquals(c4.get_transmission_speed(), 0);
		
		
		// approximate h10, so the distance stays in the transmit range
		hx.setLocation(new Coord(150.0, 0.0));
		Contact c5 = new Contact(hx, hy, 40.0, 50.0);
		assertEquals(c5.get_transmission_speed(), 2);
	}
	
	public void test_get_other_host() {
		Contact c1 = new Contact(h0, h1, 0.0, 10.0);
		assertEquals(c1.get_other_host(h0), h1);
		
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
		Vertex v1 = new Vertex("vertex_test", cx, false);
		assertEquals(v1.get_transmission_speed(), 15);
		assertFalse(v1.is_pivot());
	}
	
	// test sender/receiver
	public void test_sender_receiver() {
		assertEquals(c1.begin(), 0.0);
		assertEquals(c1.end(), 10.0);
		assertNull(v1.get_receiver());
		assertNull(v1.get_sender());
		v1.set_receiver(h10);
		assertEquals(v1.get_sender(), h11);
		v1.set_receiver(h11);
		assertEquals(v1.get_sender(), h10);
		v1.set_sender(h10);
		assertEquals(v1.get_receiver(), h11);
		assertEquals(v1.get_sender(), h10);
	}
	
	// test is_pivot
	public void test_is_pivot() {
		assertFalse(v1.is_pivot());		
		assertTrue(v2.is_pivot());
	}
	
	// test get_hosts
	public void test_get_hosts() {
		assertEquals(Arrays.asList(h10, h11), v1.get_hosts());
	}
	
	// test get_common_host
	public void test_get_common_host() {
		Vertex v1 = new Vertex("vertex1", c1, false);
		Vertex v2 = new Vertex("vertex2", c2, false);
		assertEquals(h11, v1.get_common_host(v2));
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
		assertEquals(e2.get_src_id(), "vertex_c1");
		assertEquals(e2.get_dest_id(), "vertex_c2");
		assertEquals(e2.toString(), "edge_vertex_c1_vertex_c2");
	}
	
	/*
	 * Graph tests
	 */
	
	public void test_graph_creation() {

		Map<String, Vertex> vertices = new HashMap<>();
		vertices.put("vertex1", v1);
		vertices.put("vertex2", v3);
		vertices.put("vertex3", v4);
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
	public void test_route_search_creation() {
		
	}
}
