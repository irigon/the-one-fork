package test;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;

import core.Coord;
import core.DTNHost;
import core.NetworkInterface;
import interfaces.SimpleBroadcastInterface;
import routing.EpidemicRouter;
import routing.MessageRouter;
import routing.cgr.Contact;
import routing.cgr.Vertex;

public class CGRRouterTest extends AbstractRouterTest {

	private static int TTL = 300;

	@Override
	public void setUp() throws Exception {
		ts.putSetting(MessageRouter.MSG_TTL_S, ""+TTL);
		ts.putSetting(MessageRouter.B_SIZE_S, ""+BUFFER_SIZE);
		setRouterProto(new EpidemicRouter(ts));
		super.setUp();
	}
	
	
	/*
	 * Contact Tests
	 */
	public void testContactCreation() {
		
		// Contacts are ordered by name
		Contact c1 = new Contact(h0, h1, 0.0, 10.0);
		Contact c2 = new Contact(h1, h0, 0.0, 10.0);
		Contact c3 = new Contact(h1, h0, 100.0, 101.0);
		
		ts.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "10.0");
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "15");

		h4 = utils.createHost(new Coord(0.0, 10.0), "h4");
		
		assertEquals(c1, c2);
		
		// Verify cid creation
		assertEquals(c1.get_id(), "cid_h0_h1_0_0");
		assertEquals(c3.get_id(), "cid_h0_h1_100_0");
		
		// Assert hash creation
		assertNotEquals(c1.hashCode(), 0);
	}

	// Test contact transmission speed math 
	 
	public void test_get_transmission_speed() {

		h0.setLocation(new Coord(0.0, 0.0));
		h1.setLocation(new Coord(0.1, 0.0));
		
		Contact c1 = new Contact(h0, h1, 0.0, 10.0);
		
		// default transmission_speed is 10
		assertEquals(c1.get_transmission_speed(), 10);
		
		/* increasing the speed of the interfaces should increase the 
		*  contact speed
		*/
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "15");
		ts.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "3.0");

		DTNHost h10 = utils.createHost(c0, "h10");
		DTNHost h11 = utils.createHost(c0, "h11");
		h10.setLocation(new Coord(0.0, 0.0));
		h11.setLocation(new Coord(2.1, 0.0));
		
		Contact c2 = new Contact(h10, h11, 0.0, 10.0);
		assertEquals(c2.get_transmission_speed(), 15);
		
		/* 
		 * Moving one interface away, get speed 0
		 */
		h11.setLocation(new Coord(200.0, 0.0));
		Contact c3 = new Contact(h10, h11, 20.0, 30.0);
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
		 
		h10.getInterfaces().add(ni1);
		h11.getInterfaces().add(ni2);
		
		
		// create a contact c4, but 200.0 is still too far
		Contact c4 = new Contact(h10, h11, 30.0, 40.0);
		assertEquals(c4.get_transmission_speed(), 0);
		
		
		// approximate h10, so the distance stays in the transmit range
		h10.setLocation(new Coord(150.0, 0.0));
		Contact c5 = new Contact(h10, h11, 40.0, 50.0);
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
		DTNHost h10 = utils.createHost(c0, "h10");
		DTNHost h11 = utils.createHost(c0, "h11");
		Contact c1 = new Contact(h10, h11, 0.0, 10.0);
		Vertex v1 = new Vertex("vertex_test", c1, false);
		assertEquals(v1.get_transmission_speed(), 15);
		assertFalse(v1.is_pivot());
	}
	
	// test sender/receiver
	public void test_sender_receiver() {
		DTNHost h10 = utils.createHost(c0, "h10");
		DTNHost h11 = utils.createHost(c0, "h11");
		Contact c1 = new Contact(h10, h11, 0.0, 10.0);
		Vertex v1 = new Vertex("vertex_test", c1, false);
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
		DTNHost h10 = utils.createHost(c0, "h10");
		DTNHost h11 = utils.createHost(c0, "h11");
		Contact c1 = new Contact(h10, h11, 0.0, 10.0);
		Vertex v1 = new Vertex("vertex_test", c1, false);
		Vertex v2 = new Vertex("vertex_test", c1, true);
		assertFalse(v1.is_pivot());		
		assertTrue(v2.is_pivot());
	}
	
	// test get_hosts
	public void test_get_hosts() {
		DTNHost h10 = utils.createHost(c0, "h10");
		DTNHost h11 = utils.createHost(c0, "h11");
		Contact c1 = new Contact(h10, h11, 0.0, 10.0);
		Vertex v1 = new Vertex("vertex_test", c1, false);
		assertEquals(Arrays.asList(h10, h11), v1.get_hosts());
	}
	
	// test get_common_host
	public void test_get_common_host() {
		DTNHost h10 = utils.createHost(c0, "h10");
		DTNHost h11 = utils.createHost(c0, "h11");
		DTNHost h12 = utils.createHost(c0, "h12");
		Contact c1 = new Contact(h10, h11, 0.0, 10.0);
		Contact c2 = new Contact(h11, h12, 0.0, 10.0);
		Vertex v1 = new Vertex("vertex1", c1, false);
		Vertex v2 = new Vertex("vertex2", c2, false);
		assertEquals(h11, v1.get_common_host(v2));
	}

}
