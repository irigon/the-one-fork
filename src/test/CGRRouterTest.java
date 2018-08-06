package test;

import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
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

	private List<Object> create_and_init_pivot(RouteSearch rs, List<Vertex> v_to_connect, DTNHost h,
			boolean start) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class[] argClasses = { List.class, DTNHost.class, boolean.class };
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
		assertEquals(c2.end(), 110.0);

		// Assert hash creation
		assertNotEquals(c3.hashCode(), 0);
	}

	// Test contact transmission speed math

	public void test_get_transmission_speed() {

		h0.setLocation(new Coord(0.0, 0.0));
		h1.setLocation(new Coord(0.1, 0.0));

		// default transmission_speed is 10
		assertEquals(c3.get_transmission_speed(), 10);

		/*
		 * increasing the speed of the interfaces should increase the contact speed
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
		 * Having several interfaces, the contact is made up the first pair of the same
		 * type in which the range is wide enough
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
		assertEquals(c4.get_current_capacity(), (c4.end() - c4.adjusted_begin()) * c4.get_transmission_speed());
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

	public void test_route_search_relax_phase() throws Exception, Exception {

		/* Some assumptions on the following tests */
		assertEquals(v3.get_transmission_speed(), 10);
		assertEquals(v4.get_transmission_speed(), 10);

		Map<Vertex, Double> distances_01 = (Map<Vertex, Double>) get_private("distances", rs01);
		Map<Vertex, Double> distances_02 = (Map<Vertex, Double>) get_private("distances", rs02);

		/*
		 * g1 vertex graph
		 */
		
		DTNHost src = v4.get_hosts().get(0);
		
		List<Object> pivot_structure = create_and_init_pivot(rs01, Arrays.asList(v4), src, true);
		Vertex pivot_begin = (Vertex)pivot_structure.get(0);
		initialize_route_search(rs01, pivot_begin, 0.0);
		Vertex end;

		// access private elements for testing. There is a broad discussion whether this
		// is or not a bad practice
		end = call_route_search_relax(rs01, pivot_begin, m01);
		assertNull(end);

		// after the first relax, the message can be forward after v4_start == 20.0
		// since the message has size 10 and the speed is 10, the time when v4 is ready
		// to transmit further is 21.0
		assertEquals(distances_01.get(v4), 21.0);

		// now the only node in unsettled is v4, we can relax v4 and it should find the
		// pivot_end
		// the pivot start is zero, so the best starting time will be 21.0, it should
		// take 0.0 to transmit,
		// since it is a pivot
		end = call_route_search_relax(rs01, v4, m01);
		double dist = distances_01.get(v4);
		assertEquals(distances_01.get(v4), 21.0);
		//Vertex pivot_end = create_pivot(v4.get_receiver());
		DTNHost hend = v4.get_hosts().get(1);
		pivot_structure = create_and_init_pivot(rs01, Arrays.asList(v4), hend, false);
		Vertex pivot_end = (Vertex)pivot_structure.get(0); 

		assertNotNull(pivot_end);

		/*
		 * g2 vertex graph
		 */

		// private Vertex create_pivot_and_initialize(DTNHost h, double start_time,
		// double end_time) {
		// initialize_route_search(rs02, m02, 0.0, v3.get_hosts().get(0));
		src = v3.get_hosts().get(0);
		pivot_structure = create_and_init_pivot(rs02, Arrays.asList(v3), src, true);
		pivot_begin = (Vertex)pivot_structure.get(0);
		initialize_route_search(rs02, pivot_begin, 0.0);
		DTNHost common_host = v4.get_common_host(v3);
		hend = v4.get_other_host(common_host);
		pivot_structure = create_and_init_pivot(rs02, Arrays.asList(v4), hend, false);
		pivot_end = (Vertex)pivot_structure.get(0);

		// calling relax on pivot will find out vertex v3
		assertEquals(distances_02.get(v3), Double.POSITIVE_INFINITY);
		end = call_route_search_relax(rs02, pivot_begin, m02);
		assertNull(end);
		assertEquals(distances_02.get(v3), 1.0);
		assertEquals(distances_02.get(v4), Double.POSITIVE_INFINITY);

		// the next unsettled to be called is v3 that will find p4 but not the pivot
		end = call_route_search_relax(rs02, v3, m02);
		assertNull(end);
		assertEquals(distances_02.get(v3), 1.0);
		assertEquals(distances_02.get(v4), 21.0);

	}

	public void test_search() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		/*
		 * Search with one vertex
		 */

		// 1 vertex graph
		end_pivot = rs01.search(v4.get_hosts().get(0), 0.0, m01);
		assertNotNull(end_pivot);
		path = rs01.get_path(end_pivot);
		assertEquals(path.get_path_as_list().get(0), v4);
		assertEquals(path.get_path_as_list().size(), 1);
		/*
		 *  2 vertex graph
		 *  
		 *   v3   v4
		 *   o----o
		 */
		end_pivot = rs02.search(v3.get_hosts().get(0), 0.0, m02);
		path = rs02.get_path(end_pivot);
		assertEquals(path.get_path_as_list().get(0), v3);
		assertEquals(path.get_path_as_list().get(1), v4);
		assertEquals(path.get_path_as_list().size(), 2);
		// 3 vertex graph
		end_pivot = rs03.search(v3.get_hosts().get(0), 0.0, m03);
		path = rs03.get_path(end_pivot);
		assertEquals(path.get_path_as_list().get(0), v3);
		assertEquals(path.get_path_as_list().get(1), v4);
		assertEquals(path.get_path_as_list().get(2), v5);
		assertEquals(path.get_path_as_list().size(), 3);
		
		/*
		 * 
		 * p1x8 o           o p2x9
		 *      |           |
		 * x6p1 o           o x7p2
		 *      |  x6p3     |
		 * p4x6 o---o---o---o p5x7
		 *       \    p3x7 /
		 *        \       /
		 *   x10p4 o     o x10p5
		 *          \   /	
		 *           \ / 
		 *            o
		 *      	  pi
		 * 
		 * */

		Map<Vertex, Double> distances = (Map<Vertex, Double>) get_private("distances", rs09);
		
		/* x10 -> p4  time = 105.0 , 
		 * path: x10_p4, p4_x6, x6_p3, p3_x7, p5_x7, x7_p2, p2_x9 time@last_contact 280 
		 */
		Message m = new Message(hx10, hx9,  "TestMessage", 10);
		end_pivot = rs09.search(hx10, 105.0, m);
		assertEquals(distances.get(end_pivot), 202.0); // p2_x9 has distance 281.0, pivot has p2_x9 +1

		end_pivot = rs09.search(hx10, 205.0, m);
		assertEquals(distances.get(end_pivot), 392.0); 
		
		m = new Message(hx10, hx8,  "TestMessage", 10);
		end_pivot = rs09.search(hx10, 150.0, m);
		assertEquals(distances.get(end_pivot), 332.0); 

	}

	public void test_setting_pivot()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, Exception, InvocationTargetException {
		/*
		 * 4 vertex graph, starting at 46.0. Pivot_start should connect only to source
		 * 	
		 *  v31    v41
		 *   x ---- x
		 *           \
		 *            o ---- o
		 *			 / v5    v6
		 *   x ---- x
		 *  v3      v4
		 */

		// search starts after the end of contact v41 (45.0)
		Map<String, List<Edge>> edges = (Map<String, List<Edge>>) get_private("edges", rs04);

		DTNHost src = v5.get_hosts().get(0);
		List<Object> pivot_structure = create_and_init_pivot(rs04, Arrays.asList(v5), src, true);
		Vertex pivot_begin = (Vertex) pivot_structure.get(0);

		assertEquals(edges.get(pivot_begin.get_id()).size(), 1);
	}

	public void test_simple_decision()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, ReflectiveOperationException, InvocationTargetException {
		/*
		 * g50) one start, two ends 
		 *       v5
 		 * 		 o 	
		 * v3	/
		 * o---o v4
		 * 		\
		 * 		 o 
		 *		 v51
		 *
		 *		c3  = (h10, h11, 0.0, 10.0)
		 *      c4  = (h11, h12, 20.0, 30.0)
		 *      c5  = (h12, h13, 40.0, 50.0)
		 *      c51 = (h12, h14, 45.0, 55.0)
		 *      
		 *      m03: h10 --> h13
		 *      
		 */

		Map<String, List<Edge>> edges = (Map<String, List<Edge>>) get_private("edges", rs05);

		DTNHost src = v3.get_hosts().get(0);
		List<Object> pivot_structure = create_and_init_pivot(rs05, Arrays.asList(v3), src, true);

		Vertex begin_pivot = (Vertex)pivot_structure.get(0);
		end_pivot = rs05.search(v3.get_hosts().get(0), 0.0, m03);
		assertNotNull(end_pivot);
		path = rs05.get_path(end_pivot);
		assertEquals(path.get_path_as_list().get(0), v3);
		assertEquals(path.get_path_as_list().get(1), v4);
		assertEquals(path.get_path_as_list().get(2), v5);
		assertEquals(edges.get(begin_pivot.get_id()).get(0).get_dest_id(), v3.get_id());
		assertEquals(edges.get(begin_pivot.get_id()).size(), 1);
		assertEquals(edges.get(v51.get_id()).size(), 0);
	}

	/*
	 * 
	 * g51) one end, two paths
	 * 		 c5
	 *       o	
	 * 	  c4/ \
	 * o---o   o
	 * c3	\ /c6
	 * 		 o
	 *      c51
	 * 
	 * c3 = (h10, h11, 0.0, 10.0) 
	 * c4 = (h11, h12, 20.0, 30.0) 
	 * c5 = (h12, h13, 40.0, 50.0) 
	 * c52 = (h12, h13, 55.0, 58.0) 
	 * c6 = (h13, h14, 60.0, 70.0) 
	 * m06 = v3.get(0) --> v6.get(1)
	 * 
	 */

	public void test_convergence() {
		end_pivot = rs06.search(v3.get_hosts().get(0), 0.0, m06);
		assertNotNull(end_pivot);
		path = rs06.get_path(end_pivot);

		assertEquals(path.get_path_as_list().get(0), v3);
		assertEquals(path.get_path_as_list().get(1), v4);
		assertEquals(path.get_path_as_list().get(2), v5);
		assertEquals(path.get_path_as_list().get(3), v6);

		/*
		 * g52) one end, two paths
		 *        v5	
		 *   	  o	
		 * v3  v4/ \
		 *  o---o   o----o
		 *  	 \ /v6   v7
		 *  	  o
		 *       v52
		 * 
		 * m07: (h10 --> h15)
		 * 
		 */

		end_pivot = rs06.search(v3.get_hosts().get(0), 0.0, m07);
		assertNotNull(end_pivot);
		path = rs06.get_path(end_pivot);
		assertEquals(path.get_path_as_list().get(0), v3);
		assertEquals(path.get_path_as_list().get(1), v4);
		assertEquals(path.get_path_as_list().get(2), v5);
		assertEquals(path.get_path_as_list().get(3), v6);
		assertEquals(path.get_path_as_list().get(4), v7);
	}

	/*
	 * We currently prune contacts from repeated pair of nodes in the past. Say, if
	 * vertex v1 and v2 are two vertex between the same pair of hosts h1 and h2 and
	 * beginning at t1 and t2 respectively: if t1 < t2 < now, v1_t1 can be deleted.
	 */
	public void test_prune() throws ReflectiveOperationException, RuntimeException {

		/*
		 * A simpler example
		 * 
		 * cold
		 * x -------- e1
		 *            \
		 * o --------- o
		 * cnew	  e2	clast
		 * 
		 *
		 * cold v1 (h11, h10, 0.0, 10.0); 
		 * cnew v21 (h11, h10, 100.0, 110.0) 
		 * clast v42 (h11, h12, 120.0, 130.0)
		 * 
		 */

		Map<String, Vertex> vertices = (Map<String, Vertex>) get_private("vertices", rs07);

		int num_vertices = vertices.values().size();
		call_prune(rs07, 105.0);
		int new_num_vertices = vertices.values().size();
		assertEquals(num_vertices, new_num_vertices + 1); // +2 pivots -1 pruned

		/*
		 * A somewhat more complicated example
		 * 
		 * vb_t2, vb_t4 should be pruned
         *
		 * va_t1   vb_t2      
		 * o ------ x
		 *  \
		 *   ------------
		 *     \   vc_t3  \ vb_t4
		 *      --- o ---- x
		 *          |\          vb_t5
		 *          | --------- o 
		 *          |           |
		 *          |          now(t6)
		 *          |            
		 *           \
		 *            ----------- o vb_t7
		 * 
		 * va_t1	--> c4  (h11, h12, 20.0, 30.0)
		 * vb_t2    --> c5  (h12, h13, 40.0, 50.0) 
		 * vc_t3	--> c51	(h12, h14, 45.0, 55.0)
		 * vb_t4	--> c52 (h12, h13, 55.0, 58.0)
		 * vb_t5	--> c53 (h12, h13, 75.0, 90.0)
		 * t6 -- current time 100.0
		 * vb_t7	--> c54 (h12, h13, 110.0, 115.0)
         * 
		 */

		List<Double> times = Arrays.asList(10.0, 19.0, 21.0, 29.0, 47.0, 54.0, 80.0, 100.0, 120.0);
		List<Integer> graphsize = Arrays.asList(6, 6, 6, 6, 5, 4, 2, 1, 0);

		ListIterator<Double> it = times.listIterator();
		while (it.hasNext()) {
			rs08 = new RouteSearch(g08);
			vertices = (Map<String, Vertex>) get_private("vertices", rs08);
			call_prune(rs08, times.get(it.nextIndex()));
			new_num_vertices = vertices.values().size();

			assertEquals(new_num_vertices, (int) graphsize.get(it.nextIndex()));
			it.next();
		}

	}
	
	
	

}
