package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import routing.cgr.Contact;
import routing.cgr.Edge;
import routing.cgr.Graph;
import routing.cgr.Path;
import routing.cgr.RouteSearch;
import routing.cgr.Vertex;

public class AbstractCGRRouterTest extends AbstractRouterTest {
	
	protected DTNHost h10;
	protected DTNHost h11;
	protected DTNHost h12;
	protected DTNHost h13;
	protected DTNHost h14;
	protected DTNHost h15;
	
	protected Contact c1;
	protected Contact c2;
	protected Contact c3;
	protected Contact c31;
	protected Contact c4;
	protected Contact c41;
	protected Contact c42;
	protected Contact c5;
	protected Contact c51;
	protected Contact c52;
	protected Contact c53;
	protected Contact c54;
	protected Contact c6;
	protected Contact c7;
	
	protected Vertex  v1;
	protected Vertex  v2;
	protected Vertex  v21;
	protected Vertex  v3;
	protected Vertex  v31;
	protected Vertex  v4;
	protected Vertex  v41;
	protected Vertex  v42;
	protected Vertex  v5;
	protected Vertex  v51;
	protected Vertex  v52;
	protected Vertex  v53;
	protected Vertex  v54;
	protected Vertex  v6;
	protected Vertex  v7;
	protected Vertex  start_pivot;
	protected Vertex  end_pivot;
	
	protected Edge    e1;
	protected Edge    e2;
	protected Edge    e21;
	protected Edge    e3;
	protected Edge    e31;
	protected Edge    e32;
	protected Edge    e33;
	protected Edge    e34;
	protected Edge    e4;
	protected Edge    e41;
	protected Edge    e42;
	protected Edge    e43;
	protected Edge    e44;
	protected Edge    e5;
	protected Edge    e61;
	protected Edge    e62;
	
	protected Map<String, Vertex> vmap01;
	protected Map<String, Vertex> vmap02;
	protected Map<String, Vertex> vmap03;
	protected Map<String, Vertex> vmap04;
	protected Map<String, Vertex> vmap05;
	
	protected Map<String, List<Edge>> ledges01;
	protected Map<String, List<Edge>> ledges02;
	protected Map<String, List<Edge>> ledges03;
	protected Map<String, List<Edge>> ledges04;
	protected Map<String, List<Edge>> ledges05;
	
	protected Graph   g01;
	protected Graph   g02;
	protected Graph   g03;
	protected Graph   g04;
	protected Graph   g05;
	protected Graph   g06;
	protected Graph   g07;
	protected Graph   g08;
	
	protected Message m01;
	protected Message m02;
	protected Message m03;
	protected Message m04;
	protected Message m05;
	protected Message m06;
	protected Message m07;
	protected Message m08;
	protected Message m09;
	protected Message m10;
	
	protected RouteSearch rs01;
	protected RouteSearch rs02;
	protected RouteSearch rs03;
	protected RouteSearch rs04;
	protected RouteSearch rs05;
	protected RouteSearch rs06;
	protected RouteSearch rs07;
	protected RouteSearch rs08;
	
	protected Path path;
	
	private Map<String, List<Edge>> initialize_edges(Map<String, Vertex> vmap) {
		Map<String, List<Edge>> ledges = new HashMap<>();
		for (String vname : vmap.keySet()) {
			ledges.put(vname, new ArrayList<>());
		}
		return ledges;
	}
	private Map<String, Vertex> initialize_vmap(List<Vertex> vertices) {
		Map<String, Vertex> vmap = new HashMap<>();
		for (Vertex v : vertices) {
			vmap.put(v.get_id(), v);
		}
		return vmap;
	}	
	@Override
	public void setUp() throws Exception {
		super.setUp();

		h10 = utils.createHost(c0, "h10");
		h11 = utils.createHost(c0, "h11");
		h12 = utils.createHost(c0, "h12");
		h13 = utils.createHost(c0, "h13");
		h14 = utils.createHost(c0, "h14");
		h15 = utils.createHost(c0, "h15");
		
		c1  = new Contact(h11, h10, 0.0, 10.0);
		c2  = new Contact(h11, h10, 100.0, 110.0);
		c3  = new Contact(h10, h11, 0.0, 10.0);
		c31  = new Contact(h10, h11, 15.0, 25.0);
		c4  = new Contact(h11, h12, 20.0, 30.0);
		c41 = new Contact(h12, h13, 35.0, 45.0);
		c42  = new Contact(h11, h12, 120.0, 130.0);
		c5  = new Contact(h12, h13, 40.0, 50.0);
		c51  = new Contact(h12, h14, 45.0, 55.0);
		c52  = new Contact(h12, h13, 55.0, 58.0);
		c53  = new Contact(h12, h13, 75.0, 90.0);
		c54  = new Contact(h12, h13, 110.0, 115.0);
		c6  = new Contact(h13, h14, 60.0, 70.0);
		c7  = new Contact(h14, h15, 80.0, 90.0);

		v1  = new Vertex("vertex_1", c1, false);
		v2  = new Vertex("pivot_c1", c1, true);
		v21  = new Vertex("vertex_21", c2, false);
		v3  = new Vertex("vertex_3", c3, false);
		v31 = new Vertex("vertex_31", c31, false);
		v4  = new Vertex("vertex_4", c4, false);
		v41 = new Vertex("vertex_41", c41, false);
		v42 = new Vertex("vertex_42", c42, false);
		v5  = new Vertex("vertex_5", c5, false);
		v51 = new Vertex("vertex_51", c51, false);
		v52 = new Vertex("vertex_52", c52, false);
		v53 = new Vertex("vertex_53", c53, false);
		v54 = new Vertex("vertex_54", c54, false);
		v6  = new Vertex("vertex_6", c6, false);
		v7  = new Vertex("vertex_7", c7, false);
		

		e1  = new Edge(v3, v2);
		e2  = new Edge(v3, v4);
		e21 = new Edge(v31, v41);
		e3  = new Edge(v4, v5);
		e31 = new Edge(v41, v5);
		e32 = new Edge(v4, v51);
		e33 = new Edge(v4, v52);
		e34 = new Edge(v4, v7);
		e4  = new Edge(v5, v6);
		e41 = new Edge(v52, v6);
		e42 = new Edge(v51, v52);
		e43 = new Edge(v51, v53);
		e44 = new Edge(v51, v54);
		e5  = new Edge(v6, v7);
		e61 = new Edge(v1, v42);
		e62 = new Edge(v21, v42);

		
		// Message from:h11 to:h12, id:"TestMessage", size: 10
		m01 = new Message(v4.get_hosts().get(0), v4.get_hosts().get(1),  "TestMessage", 10);
		m02 = new Message(v3.get_hosts().get(0), v4.get_hosts().get(1),  "TestMessage", 10);
		m03 = new Message(v3.get_hosts().get(0), v5.get_hosts().get(1),  "TestMessage", 10);
		m04 = new Message(v4.get_hosts().get(0), v5.get_hosts().get(1),  "TestMessage", 10);
		m05 = new Message(v5.get_hosts().get(0), v6.get_hosts().get(1),  "TestMessage", 10);
		m06 = new Message(v3.get_hosts().get(0), v6.get_hosts().get(1),  "TestMessage", 10);
		m07 = new Message(v3.get_hosts().get(0), v7.get_hosts().get(1), "TestMessage", 10);
		m08 = new Message(v2.get_hosts().get(0), v42.get_hosts().get(1), "TestMessage", 10);
		m09 = new Message(v51.get_hosts().get(0), v53.get_hosts().get(1), "TestMessage", 10);
		m10 = new Message(v51.get_hosts().get(1), v54.get_hosts().get(1), "TestMessage", 10);
		
		/*
		 * g1) the simplest graph with 1 vertex
		 * 
		 *  o
		 *  
		 */
		Map<String, Vertex> vmap01 = initialize_vmap(Arrays.asList(v4));
		Map<String, List<Edge>> ledges01 = initialize_edges(vmap01);
		g01 = new Graph(vmap01 , ledges01);
		

		/*
		 * g2) a simple graph with 2 vertexes
		 *
		 *	o----o
		 */
		Map<String, Vertex> vmap02 = initialize_vmap(Arrays.asList(v3, v4));
		Map<String, List<Edge>> ledges02 = initialize_edges(vmap02);
		ledges02.get(v3.get_id()).add(e2);
		g02 = new Graph(vmap02, ledges02);
		
		
		/*
		 * g3) extending it to 3 nodes in line
		 *
		 *	o----o----o
		 */
		Map<String, Vertex> vmap03 = initialize_vmap(Arrays.asList(v3, v4, v5));
		Map<String, List<Edge>> ledges03 = initialize_edges(vmap03);
		ledges03.get(v3.get_id()).add(e2);
		ledges03.get(v4.get_id()).add(e3);
		g03 = new Graph(vmap03, ledges03);
				
		
		/* Multigraph with old edges 
		 * 	
		 *  v31    v41
		 *   x ---- x
		 *           \
		 *            o ---- o
		 *			 / v5    v6
		 *   x ---- x
		 *  v3      v4
		 */
		Map<String, Vertex> vmap04 = initialize_vmap(Arrays.asList(v3, v4, v31, v41, v5, v6));
		Map<String, List<Edge>> ledges04 = initialize_edges(vmap04);
		ledges04.get(v3.get_id()).add(e2);
		ledges04.get(v4.get_id()).add(e3);
		ledges04.get(v31.get_id()).add(e21);
		ledges04.get(v41.get_id()).add(e31);
		ledges04.get(v5.get_id()).add(e4);
		g04 = new Graph(vmap04, ledges04);

		
		/*
		 * g50) one start, two ends
		 * 		 o	
		 * 		/
		 * o---o
		 * 		\
		 * 		 o
		 * 
		 * 
		 * g70) prune visited nodes
		 *
		 * 		 x	
		 * 		/
		 * o---o
		 * 		\
		 * 		 o
		 * 
		 */
		Map<String, Vertex> vmap05 = initialize_vmap(Arrays.asList(v3, v4, v5, v51));
		Map<String, List<Edge>> ledges05 = initialize_edges(vmap05);
		ledges05.get(v3.get_id()).add(e2);
		ledges05.get(v4.get_id()).add(e3);
		ledges05.get(v4.get_id()).add(e32);
		g05 = new Graph(vmap05, ledges05);


		/*
		 * g51) one end, two paths
		 * 		 c5
		 *       o	
		 * 	  c4/ \
		 * o---o   o
		 * c3	\ /c6
		 * 		 o
		 *      c51
		 *
		 * g52) one end, two paths
		 * 		 o	
		 * 		/ \
		 * o---o   o----o
		 * 		\ /
		 * 		 o
		 * 
		 */
		Map<String, Vertex> vmap06 = initialize_vmap(Arrays.asList(v3, v4, v5, v52, v6, v7));
		Map<String, List<Edge>> ledges06 = initialize_edges(vmap06);
		ledges06.get(v3.get_id()).add(e2);
		ledges06.get(v4.get_id()).add(e3);
		ledges06.get(v4.get_id()).add(e33);
		ledges06.get(v5.get_id()).add(e4);
		ledges06.get(v52.get_id()).add(e41);
		ledges06.get(v6.get_id()).add(e5);
		g06 = new Graph(vmap06, ledges06);

		/*
		 * g70)
		 * 
		 * cold
		 * x -------- e1
		 *            \
		 * o --------- o
		 * cnew	  e2	clast
		 */

		Map<String, Vertex> vmap07 = initialize_vmap(Arrays.asList(v1, v21, v42));
		Map<String, List<Edge>> ledges07 = initialize_edges(vmap07);
		ledges07.get(v1.get_id()).add(e61);
		ledges07.get(v21.get_id()).add(e62);
		g07 = new Graph(vmap07, ledges07);

		/* A somewhat more complicated example
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

		Map<String, Vertex> vmap08 = initialize_vmap(Arrays.asList(v4, v5, v51, v52, v53, v54));
		Map<String, List<Edge>> ledges08 = initialize_edges(vmap08);
		ledges08.get(v4.get_id()).add(e3);   // edge e4  --> c5
		ledges08.get(v4.get_id()).add(e32);  // edge e4  --> 51
		ledges08.get(v4.get_id()).add(e33);  // edge e4  --> 52
		ledges08.get(v51.get_id()).add(e42); // edge e51 --> 52
		ledges08.get(v51.get_id()).add(e43); // edge e51 --> 53
		ledges08.get(v51.get_id()).add(e44); // edge e51 --> 54
		g08 = new Graph(vmap08, ledges08);   

		
		
		rs01 = new RouteSearch(g01);
		rs02 = new RouteSearch(g02);
		rs03 = new RouteSearch(g03);
		rs04 = new RouteSearch(g04);
		rs05 = new RouteSearch(g05);
		rs06 = new RouteSearch(g06);
		rs07 = new RouteSearch(g07);
		rs08 = new RouteSearch(g08);
	}
}
