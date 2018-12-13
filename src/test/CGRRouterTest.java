package test;

import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import interfaces.SimpleBroadcastInterface;
import routing.ContactGraphRouter;
import routing.ocgr.Contact;
import routing.ocgr.Edge;
import routing.ocgr.Graph;
import routing.ocgr.Path;
import routing.ocgr.RouteSearch;
import routing.ocgr.Vertex;

public class CGRRouterTest extends AbstractCGRRouterTest {

	private static int TTL = 300;
	protected static final String BSIZE_S = "bufferSize";


	@Override
	public void setUp() throws Exception {
		ts.setNameSpace(null);
		ts.putSetting(ContactGraphRouter.CGR_NS + "." + ContactGraphRouter.CGR_DISTANCE_ALGO , 
				ContactGraphRouter.CGR_DEFAULT_DISTANCE_ALGO+"");
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

	// Test contact transmission speed math

	public void test_get_transmission_speed() {

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
		assertEquals(c4.get_current_transmission_capacity(), (c4.end() - c4.adjusted_begin()) * c4.get_transmission_speed());
		assertEquals(c4.get_current_transmission_capacity(), 100.0);
	}

	public void test_get_other_host() {
		Contact c3 = new Contact(h10, h11, 0.0, 10.0);
		assertEquals(c3.get_other_host(h10), h11);

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
		List<Object> pivot_structure = create_and_init_pivot(rs01, create_sorted_set_with_node(v4), src, true);
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
		pivot_structure = create_and_init_pivot(rs01, create_sorted_set_with_node(v4), hend, false);
		Vertex pivot_end = (Vertex)pivot_structure.get(0); 

		assertNotNull(pivot_end);

		/*
		 * g2 vertex graph
		 * src = h11
		 * v3 (h10, h11, 0.0, 10.0) --> v4 (h11, h12, 20.0, 30.0)
		 */
		src = h10;
		pivot_structure = create_and_init_pivot(rs02, create_sorted_set_with_node(v3), src, true);
		pivot_begin = (Vertex)pivot_structure.get(0);
		initialize_route_search(rs02, pivot_begin, 0.0);
		DTNHost common_host = v4.get_common_host(v3);
		hend = v4.get_other_host(common_host);
		boolean END_PIVOT=false;
		pivot_structure = create_and_init_pivot(rs02, create_sorted_set_with_node(v4), hend, END_PIVOT);
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
		end_pivot = rs01.search(v4.get_hosts().get(0), 0.0, m01, TTL);
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
		end_pivot = rs02.search(v3.get_hosts().get(0), 0.0, m02, TTL);
		path = rs02.get_path(end_pivot);
		assertEquals(path.get_path_as_list().get(0), v3);
		assertEquals(path.get_path_as_list().get(1), v4);
		assertEquals(path.get_path_as_list().size(), 2);
		// 3 vertex graph
		end_pivot = rs03.search(v3.get_hosts().get(0), 0.0, m03, TTL);
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
		end_pivot = rs09.search(hx10, 105.0, m, TTL);
		assertEquals(distances.get(end_pivot), 202.0); // p2_x9 has distance 281.0, pivot has p2_x9 +1

		m = new Message(hx10, hx8,  "TestMessage", 10);
		end_pivot = rs09.search(hx10, 150.0, m, TTL);
		assertEquals(distances.get(end_pivot), 252.0); 

		end_pivot = rs09.search(hx10, 200.0, m, TTL);
		assertEquals(distances.get(end_pivot), 332.0); 

		m = new Message(hx10, hx9,  "TestMessage", 10);
		end_pivot = rs09.search(hx10, 205.0, m, TTL);
		assertEquals(distances.get(end_pivot), 392.0); 
		
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
		List<Object> pivot_structure = create_and_init_pivot(rs04, create_sorted_set_with_node(v5), src, true);
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
		List<Object> pivot_structure = create_and_init_pivot(rs05, create_sorted_set_with_node(v3), src, true);

		Vertex begin_pivot = (Vertex)pivot_structure.get(0);
		end_pivot = rs05.search(v3.get_hosts().get(0), 0.0, m03, TTL);
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
		end_pivot = rs06.search(v3.get_hosts().get(0), 0.0, m06, TTL);
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

		end_pivot = rs06.search(v3.get_hosts().get(0), 0.0, m07, TTL);
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

	
	/*
	 * Sending a message from one host to another in vertex 41 and breaking the contact in two 
     *
	 * c42, c421:  start = 120.0, end = 130.0
	 * 
	 *  	 
     * v41
     * o----             o v8
     * v421  \   v42    /
     * o -------- o ----
     * v21   /          \
     * o-----            o v9
     * 
     * v61               v71
     * o -------------- o 
     * 
     */
	
	public void test_consume_path_00() throws Exception, SecurityException, IllegalArgumentException, IllegalAccessException {
		Map<String, Vertex> vertices = (Map<String, Vertex>) get_private("vertices", rs10);
		Map<Vertex, Vertex> predecessors = (Map<Vertex, Vertex>) get_private("predecessors", rs10);
		Map<String, List<Edge>> edges = (Map<String, List<Edge>>) get_private("edges", rs10);
		
		Message m = new Message(h10, h12,  "TestMessage", 40);
	
		Vertex result;
		
		result = rs10.search(h10, 120.0, m, TTL);
		assertTrue(result.is_pivot());
		assertEquals(vertices.size(), 8); // v421, v61, v42, v8, v9, v71, pivot_begin, pivot_end
		assertEquals(edges.size(), 8); 
		assertEquals(edges.get(v421.get_id()).size(), 1);
		assertEquals(vertices.get(v42.get_id()).adjusted_begin(), 120.0);
		assertEquals(vertices.get(v42.get_id()).end(), 130.0);
		Path p = new Path();
		p.construct(result,  predecessors);
		g10.consume_path(p, m, 10);
		assertEquals(vertices.get(v421.get_id()).adjusted_begin(), 124.0);
		assertEquals(vertices.get(v42.get_id()).adjusted_begin(), 120.0);
		assertEquals(vertices.get(v42.get_id()).end(), 124.0);
		assertEquals(vertices.size(), 9); // + vertex_42_128.0_130.0
		assertEquals(edges.size(), 9); 
		assertEquals(edges.get(v421.get_id()).size(), 2);

		result = rs10.search(h10, 120.0, m, TTL);
		assertFalse(result.is_pivot());
		assertEquals(p.construct(result, predecessors).size(), 0);	// empty path, list with size 0
		
		// for a 1 sec message there should still be enough resource
		m = new Message(h10, h12,  "TestMessage", 10);
		result = rs10.search(h10, 120.0, m, TTL);
		assertTrue(result.is_pivot());
		assertEquals(vertices.size(), 9); 
		assertEquals(edges.size(), 9); 
		assertEquals(edges.get(v421.get_id()).size(), 2);
		assertEquals(p.construct(result, predecessors).size(), 2);	// 2 vertex path: v421 --> v42
		
		//TODO: add the corner cases we fixed. 
	}

	
	
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
	 * Send messages down to a path and verify that the contact_begin moved
	 * Keeps sending message until another path must be chosen
	 * Go on until no path is available
	 * 
	 */
	
	public void test_consume_path_01() throws Exception, SecurityException, IllegalArgumentException, IllegalAccessException {
		Map<Vertex, Double> distances = (Map<Vertex, Double>) get_private("distances", rs09);
		Map<Vertex, Vertex> predecessors = (Map<Vertex, Vertex>) get_private("predecessors", rs09);
		Map<String, Vertex> vertices = (Map<String, Vertex>) get_private("vertices", rs09);
		Message m = new Message(hx10, hx9,  "TestMessage", 50);
		end_pivot = rs09.search(hx10, 104.0, m, TTL);
		assertEquals(distances.get(end_pivot), 210.0); 
		
		Path p = new Path();
		p.construct(end_pivot,  predecessors);
		g09.consume_path(p, m, 10);
	}
	
	
	/*
	 * BLACK BOX TESTS
	 * 
	 * until now most tests concentrate on dijkstra, the kernel of the path search.
	 * Below we perform black box tests on the router.
	 */
	

	/*
	 * call search on an empty graph
	 */
	public void test_empty_graph() {
		Message m = new Message(hx10, hx9,  "TestMessage", 50);
		assertNull(rs00.search(hx10, 104.0, m, TTL));
	}

	/*
	 * Send a message from a host to a destination in a graph with one node
	 * tests:
	 *  - [1] src, dst exist and start + resource < capacity
	 *  - [2] src does not exist
	 *  - [3] dst does not exist
	 *  - [4] src and dst do not exist 
	 *  - [5] not enough capacity
	 *  - [6] message expires before destination receives
	 */
	public void test_graph_one_node_01() {
		
		/* [1] src, dst exist and start + resource < capacity */ 
		
		//vertice v4: h11 <--> h12 [20.0, 30.0]
		Message m = new Message(h11, h12,  "TestMessage", 50);
		Path p = new Path();
		// enough resource, pivot returned is connected to end host (h12)
		assertTrue(rs01.search(h11, 10.0, m, TTL).get_hosts().contains(h12));
		assertTrue(rs01.search(h11, 20.0, m, TTL).get_hosts().contains(h12));
		assertTrue(rs01.search(h11, 24.9, m, TTL).get_hosts().contains(h12));
		// not enough resource, pivot returned contains start host (h11)
		assertTrue(rs01.search(h11, 25.0, m, TTL).get_hosts().contains(h11));
		assertTrue(rs01.search(h11, 26.0, m, TTL).get_hosts().contains(h11));
		assertNull(rs01.search(h11, 206.0, m, TTL)); // vertex is pruned, null returned

		// vertices were pruned, reconstructing graph g01
		Map<String, Vertex> vmap01 = initialize_vmap(Arrays.asList(v4));
		Map<String, List<Edge>> ledges01 = initialize_edges(vmap01);
		g01 = new Graph(vmap01 , ledges01);
		rs01 = new RouteSearch(g01);
		/* [2] src  does not exist */

		m = new Message(h10, h12,  "TestMessage", 50);
		assertNull(rs01.search(h10, 10.0, m, TTL)); 

		/* [3] dst does not exist */
		m = new Message(h11, h13,  "TestMessage", 50);
		assertNull(rs01.search(h11, 10.0, m, TTL)); 
		
		/* [4] src and dst do not exist */
		m = new Message(h10, h13,  "TestMessage", 50);
		assertNull(rs01.search(h10, 10.0, m, TTL)); 
		
		/* [5] not enough capacity, no pivot will be found, return null */
		m = new Message(h11, h12,  "TestMessage", 110);
		assertNull(rs01.search(h11, 10.0, m, TTL)); 
		
		/* [6] message expires before destination receives 
		 * g70)
		 * 
		 *  cold
		 *  x -------- e1
		 *             \
		 *  o --------- o
		 *  cnew	e2	clast == v42 (120.0, 130.0)
		 * 
		 * 
		 * Sending a message IN vertice v42 (h11-h12) with size 60 takes 6s,
		 * since default speed = 10data units/s.
		 * at time 65 and ttl 1 (60s), should cause the message to expire
		 * at 125s, while the message should arrive at 126.
		 * Dijkstra will return the path without verifying the TTL; this will
		 * is verified by search, that returns null as end_pivot.
		 */

		m = new Message(h11, h12,  "TestMessage", 60);
		// found path
		m.setTtl(1);
		assertNull(rs07.search(h11, 65.0, m, 1));
	}
	
	/*
	 * Send a message from a host to a destination in a graph with two nodes, with normal contacts
	 *  [1] src, dst exist and start + resource < capacity --> ok
	 *  
	 * Same, but contacts overlap
	 *  [2] src, dst exist and start + resource < capacity --> ok
	 *  [3] not enough capacity for the second transfer
	 */
	public void test_graph_one_node_02() {
		/* [1] c3(h10, h11, 0.0, 10.0), c4(h11, h12, 20.0, 30.0) */
		Message m = new Message(h10, h12,  "TestMessage", 60);
		end_pivot = rs02.search(h10, 0.0, m, TTL);
		assertTrue(end_pivot.get_hosts().contains(h12));
	
		/* [2] src, dst exist and start + resource < capacity --> ok, overlapping contacts  
		c42 (h11, h12, 120.0, 130.0),  c421 (h10, h11, 120.0, 130.0)	*/
		m = new Message(h10, h12,  "TestMessage", 30);
		Map<String, Vertex> vmap = initialize_vmap(Arrays.asList(v421, v42));
		Map<String, List<Edge>> ledges = initialize_edges(vmap);
		ledges.get(v421.get_id()).add(e64);  
		Graph g = new Graph(vmap, ledges);
		RouteSearch rs = new RouteSearch(g);
		end_pivot = rs.search(h10, 122.0, m, TTL);
		assertTrue(end_pivot.get_hosts().contains(h12));
		
		/* [3]not enough capacity for the second transfer */  
		m = new Message(h10, h12,  "TestMessage", 40);
		end_pivot = rs.search(h10, 122.0, m, TTL);
		assertTrue(end_pivot.get_hosts().contains(h10));
	}
	
	
	/*
	 * Assure that Dijkstra take in account the host destination buffer
	 * size when calculating the shortest path
	 */
	public void test_not_enough_buffer_found_by_pivot() throws Exception {
		/* c1 (h11, h10, 0.0, 10.0), c2 (h11, h10, 100.0, 110.0), c3 (h10, h11, 0.0, 10.0)
		 * 
		 **/

		
		ts.putSetting(ContactGraphRouter.CREATE_CPLAN, ""+false);
		ocgr = new ContactGraphRouter(ts);
		this.utils.setMessageRouterProto(ocgr);
		core.NetworkInterface.reset();
		core.DTNHost.reset();

		DTNHost hx1 = utils.createHost(c0, "hx1"); // create hosts with cgr router
		DTNHost hx2 = utils.createHost(c0, "hx2");
		DTNHost hx3 = utils.createHost(c0, "hx3");

		Contact cx1  = new Contact(hx2, hx1, 0.0, 10.0);
		Contact cx2  = new Contact(hx1, hx2, 50.0, 60.0);
		Contact cx3  = new Contact(hx2, hx3, 100.0, 110.0);
		
		Vertex vx1  = new Vertex("vertex_x1",  cx1,  false);
		Vertex vx2  = new Vertex("vertex_x2",  cx2,  false);
		Vertex vx3  = new Vertex("vertex_x3",  cx3,  false);
		
		Edge ex1 = new Edge(vx1, vx3);
		Edge ex2 = new Edge(vx2, vx3);

		Map<String, Vertex> vmap = initialize_vmap(Arrays.asList(vx1, vx2, vx3));
		Map<String, List<Edge>> ledges = initialize_edges(vmap);
		ledges.get(vx1.get_id()).add(ex1);  
		ledges.get(vx2.get_id()).add(ex2);
		
		Graph g = new Graph(vmap, ledges);
		
		ContactGraphRouter cgr_tmp;

		List<RouteSearch> lrs = new ArrayList<>();// rs1, rs2, rs3;
		for (DTNHost h : Arrays.asList(hx1, hx2, hx3)) {
			cgr_tmp = (ContactGraphRouter) h.getRouter();
			lrs.add(new RouteSearch(g));
			cgr_tmp.set_route_search(lrs.get(lrs.size()-1));			
		}
		
		assertEquals(mc.TYPE_NONE, mc.getLastType());
		
		Message mx1 = new Message(vx1.get_hosts().get(0), vx3.get_hosts().get(1), "TestMessage1", 70);
		Message mx2 = new Message(vx2.get_hosts().get(0), vx3.get_hosts().get(1), "TestMessage2", 70);
		

		hx1.createNewMessage(mx1);
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertEquals(mc.getLastFrom(), hx1);
		assertEquals(mc.getLastTo(), hx3);

		hx1.createNewMessage(mx2);
		// In pivot creation we verify if we have the capacity, so mc.next() would return false.
		assertFalse(mc.next());
		
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertEquals(mc.getLastFrom(), hx1);
		assertEquals(mc.getLastTo(), hx3);		
	}
	
	public void test_not_enough_buffer_found_on_relax_phase() throws Exception {
		/* c1 (hx1, hx2, 0.0, 50.0), 	capacity: 50*10 == 500
		 * c2 (hx2, hx3, 100.0, 150.0), 
		 * c3 (hx3, hx4, 200.0, 250.0)
		 * c4 (hx3, hx5, 300.0, 350.0)
		 * 
		 * With connectivity speed == 10, c2 can transfer up to (5 * speed) units of data
		 * from c1 --> c2 and 5 units from c2 --> c3. Lets say the speed == 10, so 50 bits
		 * What if h12 buffer supports just 20?
		 * hx1.bufferSize == 100
		 * hx2.bufferSize == 100
		 * hx3.bufferSize == 100
		 * hx4.bufferSize == 100
		 * 
		 **/

		ts.putSetting(ContactGraphRouter.CREATE_CPLAN, ""+false);
		ts.putSetting(BSIZE_S, ""+BUFFER_SIZE);
		ocgr = new ContactGraphRouter(ts);
		this.utils.setMessageRouterProto(ocgr);
		core.NetworkInterface.reset();
		core.DTNHost.reset();

		DTNHost hx1 = utils.createHost(c0, "hx1"); // create hosts with cgr router
		DTNHost hx2 = utils.createHost(c0, "hx2");
		DTNHost hx3 = utils.createHost(c0, "hx3");
		DTNHost hx4 = utils.createHost(c0, "hx4");
		DTNHost hx5 = utils.createHost(c0, "hx5");
		
		List <DTNHost> all_hosts = Arrays.asList(hx1, hx2, hx3, hx4, hx5);

		Contact cx1  = new Contact(hx1, hx2, 0.0, 50.0);
		Contact cx2  = new Contact(hx2, hx3, 100.0, 150.0);
		Contact cx3  = new Contact(hx3, hx4, 200.0, 250.0);
		Contact cx4  = new Contact(hx3, hx5, 300.0, 350.0);
		
		Vertex vx1  = new Vertex("vertex_x1",  cx1,  false);
		Vertex vx2  = new Vertex("vertex_x2",  cx2,  false);
		Vertex vx3  = new Vertex("vertex_x3",  cx3,  false);
		Vertex vx4  = new Vertex("vertex_x4",  cx4,  false);
		
		Edge ex1 = new Edge(vx1, vx2);
		Edge ex2 = new Edge(vx2, vx3);
		Edge ex3 = new Edge(vx2, vx4);

		Map<String, Vertex> vmap = initialize_vmap(Arrays.asList(vx1, vx2, vx3, vx4));
		Map<String, List<Edge>> ledges = initialize_edges(vmap);
		ledges.get(vx1.get_id()).add(ex1);  
		ledges.get(vx2.get_id()).add(ex2);  
		ledges.get(vx2.get_id()).add(ex3);
		
		Graph g = new Graph(vmap, ledges);
		
		ContactGraphRouter cgr_tmp;

		List<RouteSearch> lrs = new ArrayList<>();// rs1, rs2, rs3;
		for (DTNHost h : Arrays.asList(hx1, hx2, hx3, hx4)) {
			cgr_tmp = (ContactGraphRouter) h.getRouter();
			lrs.add(new RouteSearch(g));
			cgr_tmp.set_route_search(lrs.get(lrs.size()-1));			
		}
		
		assertEquals(mc.TYPE_NONE, mc.getLastType());
		
		Message mx1 = new Message(hx2, hx4, "TestMessage1", 70);
		Message mx2 = new Message(hx1, hx5, "TestMessage2", 70);
		

		hx2.createNewMessage(mx1);
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertEquals(mc.getLastFrom(), hx2);
		assertEquals(mc.getLastTo(), hx4);
		
		hx1.createNewMessage(mx2);
		// Host hx3 is out of buffer capacity, there should be no way to send the message
		assertFalse(mc.next());
		
	}
	
}
