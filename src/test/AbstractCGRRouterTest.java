package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.Coord;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.NetworkInterface;
import junit.framework.TestCase;
import routing.ContactGraphRouter;
import routing.MessageRouter;
import routing.cgr.Contact;
import routing.cgr.Edge;
import routing.cgr.Graph;
import routing.cgr.Path;
import routing.cgr.RouteSearch;
import routing.cgr.Vertex;

public class AbstractCGRRouterTest extends TestCase {
	
	private static final int TTL = 300;
	private static final int TRANSMIT_SPEED = 10;
	protected static final int BUFFER_SIZE = 100;
	protected ContactGraphRouter cgr;
	protected static TestSettings ts = new TestSettings();
	protected TestUtils utils;
	protected Coord c0 = new Coord(0,0);
	protected MessageChecker mc;


	protected DTNHost h10;
	protected DTNHost h11;
	protected DTNHost h12;
	protected DTNHost h13;
	protected DTNHost h14;
	protected DTNHost h15;
	protected DTNHost h16;
	protected DTNHost h17;
	protected DTNHost h18;
	protected DTNHost h19;
	protected DTNHost h20;

	protected DTNHost hx6;
	protected DTNHost hx7;
	protected DTNHost hx8;
	protected DTNHost hx9;
	protected DTNHost hx10;
	protected DTNHost hp1;
	protected DTNHost hp2;
	protected DTNHost hp3;
	protected DTNHost hp4;
	protected DTNHost hp5;
	protected DTNHost hp6;

	protected Contact c1;
	protected Contact c2;
	protected Contact c3;
	protected Contact c31;
	protected Contact c4;
	protected Contact c41;
	protected Contact c42;
	protected Contact c421;
	protected Contact c5;
	protected Contact c51;
	protected Contact c52;
	protected Contact c53;
	protected Contact c54;
	protected Contact c6;
	protected Contact c61;
	protected Contact c7;
	protected Contact c71;
	protected Contact c8;
	protected Contact c9;
	
	protected Contact c_x10_p4_100_110;
	protected Contact c_x10_p5_307_320;
	protected Contact c_x10_p4_149_160;
	protected Contact c_x10_p5_200_210;
	protected Contact c_p4_x6_130_150;
	protected Contact c_p5_x7_325_340;
	protected Contact c_p4_x6_190_200;
	protected Contact c_p5_x7_220_240;
	protected Contact c_x6_p3_140_150;
	protected Contact c_x7_p2_370_380;
	protected Contact c_x6_p1_220_240;
	protected Contact c_p3_x7_250_280;
	protected Contact c_p3_x7_160_170;
	protected Contact c_p2_x9_390_400;
	protected Contact c_p1_x8_250_260;
	protected Contact c_x6_p3_250_280;
	protected Contact c_p5_x7_160_171;
	protected Contact c_p4_x6_250_280;
	protected Contact c_x7_p2_160_171;
	protected Contact c_x6_p1_300_320;
	protected Contact c_p2_x9_200_210;
	protected Contact c_p1_x8_330_340;
	
	protected Vertex  v1;
	protected Vertex  v2;
	protected Vertex  v21;
	protected Vertex  v3;
	protected Vertex  v31;
	protected Vertex  v4;
	protected Vertex  v41;
	protected Vertex  v42;
	protected Vertex  v421;
	protected Vertex  v5;
	protected Vertex  v51;
	protected Vertex  v52;
	protected Vertex  v53;
	protected Vertex  v54;
	protected Vertex  v6;
	protected Vertex  v61;
	protected Vertex  v7;
	protected Vertex  v71;
	protected Vertex  v8;
	protected Vertex  v9;
	protected Vertex  start_pivot;
	protected Vertex  end_pivot;
	protected Vertex v_x10_p4_100_110;
	protected Vertex v_x10_p5_307_320;
	protected Vertex v_x10_p4_149_160;
	protected Vertex v_x10_p5_200_210;
	protected Vertex v_p4_x6_130_150;
	protected Vertex v_p5_x7_325_340;
	protected Vertex v_p4_x6_190_200;
	protected Vertex v_p5_x7_220_240;
	protected Vertex v_x6_p3_140_150;
	protected Vertex v_x7_p2_370_380;
	protected Vertex v_x6_p1_220_240;
	protected Vertex v_p3_x7_250_280;
	protected Vertex v_p3_x7_160_170;
	protected Vertex v_p2_x9_390_400;
	protected Vertex v_p1_x8_250_260;
	protected Vertex v_x6_p3_250_280;
	protected Vertex v_p5_x7_160_171;
	protected Vertex v_p4_x6_250_280;
	protected Vertex v_x7_p2_160_171;
	protected Vertex v_x6_p1_300_320;
	protected Vertex v_p2_x9_200_210;
	protected Vertex v_p1_x8_330_340;
	
	protected List<Vertex> vertex_list09;
	
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
	protected Edge    e51;
	protected Edge    e61;
	protected Edge    e62;
	protected Edge    e63;
	protected Edge    e64;
	protected Edge    e71;
	protected Edge    e72;
	protected Edge    e81;
	protected Edge    e82;
	
	protected Map<String, List<Edge>> edge_map01;

	
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
	
	protected Graph   g00;
	protected Graph   g01;
	protected Graph   g02;
	protected Graph   g03;
	protected Graph   g04;
	protected Graph   g05;
	protected Graph   g06;
	protected Graph   g07;
	protected Graph   g08;
	protected Graph   g09;
	protected Graph   g10;
	
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
	protected Message m11;
	protected Message m12;
	
	protected RouteSearch rs00;
	protected RouteSearch rs01;
	protected RouteSearch rs02;
	protected RouteSearch rs03;
	protected RouteSearch rs04;
	protected RouteSearch rs05;
	protected RouteSearch rs06;
	protected RouteSearch rs07;
	protected RouteSearch rs08;
	protected RouteSearch rs09;
	protected RouteSearch rs10;
	
	protected Path path;
	
	protected Map<String, List<Edge>> initialize_edges(Map<String, Vertex> vmap) {
		Map<String, List<Edge>> ledges = new HashMap<>();
		for (String vname : vmap.keySet()) {
			ledges.put(vname, new ArrayList<>());
		}
		return ledges;
	}
	protected Map<String, Vertex> initialize_vmap(List<Vertex> vertices) {
		Map<String, Vertex> vmap = new HashMap<>();
		for (Vertex v : vertices) {
			vmap.put(v.get_id(), v);
		}
		return vmap;
	}	
	
/* if two vertex have one host in common and v1.start() < v2.end() */
	private boolean connects(Vertex v1, Vertex v2) {
		Set<DTNHost> h1set = new HashSet<DTNHost>(v1.get_hosts());
		Set<DTNHost> h2set = new HashSet<DTNHost>(v2.get_hosts());
		h1set.retainAll(h2set);
		if (h1set.size() != 1) {
			return false;
		}
		
		return v1.adjusted_begin() < v2.end();
		
	}
	
	private Map<String, List<Edge>> connect_vertices(Map<String, List<Edge>> edges, List<Vertex> vertices){
		for (Vertex v1: vertices) {
			for (Vertex v2: vertices) {
				if (connects(v1, v2)) {
					String vid = v1.get_id();
					if (edges.get(vid) == null) {
						edges.put(vid, new ArrayList<Edge>());
					}
					edges.get(vid).add(new Edge(v1, v2));
				}
			}
		}
		return edges;
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.mc = new MessageChecker();
		mc.reset();

		List<MessageListener> ml = new ArrayList<MessageListener>();
		ml.add(mc);
		
		ts.putSetting(MessageRouter.MSG_TTL_S, ""+TTL);
		ts.setNameSpace(TestUtils.IFACE_NS);
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, ""+TRANSMIT_SPEED);

		this.utils = new TestUtils(null,ml,ts);
		
		core.NetworkInterface.reset();
		core.DTNHost.reset();




		h10 = utils.createHost(c0, "h10");
		h11 = utils.createHost(c0, "h11");
		h12 = utils.createHost(c0, "h12");
		h13 = utils.createHost(c0, "h13");
		h14 = utils.createHost(c0, "h14");
		h15 = utils.createHost(c0, "h15");
		h16 = utils.createHost(c0, "h16");
		h17 = utils.createHost(c0, "h17");
		h18 = utils.createHost(c0, "h18");
		h19 = utils.createHost(c0, "h19");
		
		hx6  = utils.createHost(c0, "hx6");
		hx7  = utils.createHost(c0, "hx7");
		hx8  = utils.createHost(c0, "hx8");
		hx9  = utils.createHost(c0, "hx9");
		hx10 = utils.createHost(c0, "hx10");
		hp1  = utils.createHost(c0, "hp1");
		hp2  = utils.createHost(c0, "hp2");
		hp3  = utils.createHost(c0, "hp3");
		hp4  = utils.createHost(c0, "hp4");
		hp5  = utils.createHost(c0, "hp5");
		hp6  = utils.createHost(c0, "hp6");

		
		
		c1  = new Contact(h11, h10, 0.0, 10.0);
		c2  = new Contact(h11, h10, 100.0, 110.0);
		c3  = new Contact(h10, h11, 0.0, 10.0);
		c31 = new Contact(h10, h11, 15.0, 25.0);
		c4  = new Contact(h11, h12, 20.0, 30.0);
		c41 = new Contact(h12, h13, 35.0, 45.0);
		c42 = new Contact(h11, h12, 120.0, 130.0);
		c421 = new Contact(h10, h11, 120.0, 130.0);
		c5  = new Contact(h12, h13, 40.0, 50.0);
		c51 = new Contact(h12, h14, 45.0, 55.0);
		c52 = new Contact(h12, h13, 55.0, 58.0);
		c53 = new Contact(h12, h13, 75.0, 90.0);
		c54 = new Contact(h12, h13, 110.0, 115.0);
		c6  = new Contact(h13, h14, 60.0, 70.0);
		c61  = new Contact(h13, h14, 160.0, 170.0);
		c7  = new Contact(h14, h15, 80.0, 90.0);		
		c71  = new Contact(h14, h15, 180.0, 190.0);		
		c8  = new Contact(h11, h15, 150.0, 170.0);		
		c9  = new Contact(h12, h16, 200.0, 210.0);		
		c_x10_p4_100_110 = new Contact(hx10, hp4, 100.0, 110.0);
		c_x10_p5_307_320 = new Contact(hx10, hp5, 307.0, 320.0);
		c_x10_p4_149_160 = new Contact(hx10, hp4, 149.0, 160.0);
		c_x10_p5_200_210 = new Contact(hx10, hp5, 200.0, 210.0);
		c_p4_x6_130_150  = new Contact(hp4,  hx6, 130.0, 150.0);
		c_p5_x7_325_340  = new Contact(hp5,  hx7, 325.0, 340.0);
		c_p4_x6_190_200  = new Contact(hp4,  hx6, 190.0, 200.0);
		c_p5_x7_220_240  = new Contact(hp5,  hx7, 220.0, 240.0);
		c_x6_p3_140_150  = new Contact(hx6,  hp3, 140.0, 150.0);
		c_x7_p2_370_380  = new Contact(hx7,  hp2, 370.0, 380.0);
		c_x6_p1_220_240  = new Contact(hx6,  hp1, 220.0, 240.0);
		c_p3_x7_250_280  = new Contact(hp3,  hx7, 250.0, 280.0);
		c_p3_x7_160_170  = new Contact(hp3,  hx7, 160.0, 170.0);
		c_p2_x9_390_400  = new Contact(hp2,  hx9, 390.0, 400.0);
		c_p1_x8_250_260  = new Contact(hp1,  hx8, 250.0, 260.0);
		c_x6_p3_250_280  = new Contact(hx6,  hp3, 250.0, 280.0);
		c_p5_x7_160_171  = new Contact(hp5,  hx7, 160.0, 171.0);
		c_p4_x6_250_280  = new Contact(hp4,  hx6, 250.0, 280.0);
		c_x7_p2_160_171  = new Contact(hx7,  hp2, 160.0, 171.0);
		c_x6_p1_300_320  = new Contact(hx6,  hp1, 300.0, 320.0);
		c_p2_x9_200_210  = new Contact(hp2,  hx9, 200.0, 210.0);
		c_p1_x8_330_340  = new Contact(hp1,  hx8, 330.0, 340.0);

		v1  = new Vertex("vertex_1",  c1,  false);
		v2  = new Vertex("pivot_c1",  c1,  true);
		v21 = new Vertex("vertex_21", c2,  false);
		v3  = new Vertex("vertex_3",  c3,  false);
		v31 = new Vertex("vertex_31", c31, false);
		v4  = new Vertex("vertex_4",  c4,  false);
		v41 = new Vertex("vertex_41", c41, false);
		v42 = new Vertex("vertex_42", c42, false);
		v421= new Vertex("vertex_421", c421, false);
		v5  = new Vertex("vertex_5",  c5,  false);
		v51 = new Vertex("vertex_51", c51, false);
		v52 = new Vertex("vertex_52", c52, false);
		v53 = new Vertex("vertex_53", c53, false);
		v54 = new Vertex("vertex_54", c54, false);
		v6  = new Vertex("vertex_6",  c6,  false);
		v61  = new Vertex("vertex_61",  c61,  false);
		v7  = new Vertex("vertex_7",  c7,  false);
		v71  = new Vertex("vertex_71",  c71,  false);
		v8  = new Vertex("vertex_8",  c8,  false);
		v9  = new Vertex("vertex_9",  c9,  false);
		
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
		e51 = new Edge(v61, v71);
		e61 = new Edge(v1, v42);
		e62 = new Edge(v21, v42);
		e63 = new Edge(v41, v42);
		e64 = new Edge(v421, v42);
		e71 = new Edge(v42, v8);
		e72 = new Edge(v42, v9);
		e81 = new Edge(v1, v21);
		e82 = new Edge(v3, v21);
		
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
		m11 = new Message(v1.get_hosts().get(1), v21.get_hosts().get(1), "TestMessage", 70);
		m12 = new Message(v3.get_hosts().get(0), v21.get_hosts().get(1), "TestMessage", 70);
		
		
		/*
		 * g0) an empty graph
		 */
		Map<String, Vertex> vmap00 = initialize_vmap(Arrays.asList());
		Map<String, List<Edge>> ledges00 = initialize_edges(vmap00);
		g00 = new Graph(vmap00 , ledges00);
		
		
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
		 * 
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

		
		/*
		 * 
		 * p1x8 o           o p2x9
		 *      |           |
		 * x6p1 o           o x7p2
		 *      |  x6p3     |
		 * p4x6 o---o---o---o p5x7
		 *       \    p3x7 /
		 *        \       /
		 *   x10p4 o     o x6p5
		 *          \   /	
		 *           \ / 
		 *            o
		 *      	  pi
		 * 		 * 
		 */
		vertex_list09 = new ArrayList<>();
		vertex_list09.add(new Vertex("vertex_x10_p4_100_110", c_x10_p4_100_110, false));
		vertex_list09.add(new Vertex("vertex_x10_p5_307_320", c_x10_p5_307_320, false));
		vertex_list09.add(new Vertex("vertex_x10_p4_149_160", c_x10_p4_149_160, false));
		vertex_list09.add(new Vertex("vertex_x10_p5_200_210", c_x10_p5_200_210, false));
		vertex_list09.add(new Vertex("vertex_p4_x6_130_150", c_p4_x6_130_150, false));
		vertex_list09.add(new Vertex("vertex_p5_x7_325_340", c_p5_x7_325_340, false));
		vertex_list09.add(new Vertex("vertex_p4_x6_190_200", c_p4_x6_190_200, false));
		vertex_list09.add(new Vertex("vertex_p5_x7_220_240", c_p5_x7_220_240, false));
		vertex_list09.add(new Vertex("vertex_x6_p3_140_150", c_x6_p3_140_150, false));
		vertex_list09.add(new Vertex("vertex_x7_p2_370_380", c_x7_p2_370_380, false));
		vertex_list09.add(new Vertex("vertex_x6_p1_220_240", c_x6_p1_220_240, false));
		vertex_list09.add(new Vertex("vertex_p3_x7_250_280", c_p3_x7_250_280, false));
		vertex_list09.add(new Vertex("vertex_p3_x7_160_170", c_p3_x7_160_170, false));
		vertex_list09.add(new Vertex("vertex_p2_x9_390_400", c_p2_x9_390_400, false));
		vertex_list09.add(new Vertex("vertex_p1_x8_250_260", c_p1_x8_250_260, false));
		vertex_list09.add(new Vertex("vertex_x6_p3_250_280", c_x6_p3_250_280, false));
		vertex_list09.add(new Vertex("vertex_p5_x7_160_171", c_p5_x7_160_171, false));
		vertex_list09.add(new Vertex("vertex_p4_x6_250_280", c_p4_x6_250_280, false));
		vertex_list09.add(new Vertex("vertex_x7_p2_160_171", c_x7_p2_160_171, false));
		vertex_list09.add(new Vertex("vertex_x6_p1_300_320", c_x6_p1_300_320, false));
		vertex_list09.add(new Vertex("vertex_p2_x9_200_210", c_p2_x9_200_210, false));
		vertex_list09.add(new Vertex("vertex_p1_x8_330_340", c_p1_x8_330_340, false)); 
		
		
		Map<String, Vertex> vmap09 = initialize_vmap(vertex_list09);
		Map<String, List<Edge>> edge_map09 = initialize_edges(vmap09);

		edge_map09 = connect_vertices(edge_map09, vertex_list09);
		g09 = new Graph(vmap09, edge_map09);   
		
		
		
		/*
		 * 	 
		 * v41
		 * o----             o v8
		 * v421  \   v42    /
		 * o -------- o ----
		 * v21   /          \
		 * o-----            o v9
		 * 
		 * v6               v7
		 * o --------------- o 
		 * 
		 */

		
		Map<String, Vertex> vmap10 = initialize_vmap(Arrays.asList(v421, v21, v41, v42, v8, v9, v61, v71));
		Map<String, List<Edge>> ledges10 = initialize_edges(vmap10);
		ledges10.get(v421.get_id()).add(e64); // edge v421 --> v42
		ledges10.get(v21.get_id()).add(e62);  // edge v21  --> v42
		ledges10.get(v41.get_id()).add(e63);  // edge v41  --> v42
		ledges10.get(v42.get_id()).add(e71);  // edge v42  --> v8
		ledges10.get(v42.get_id()).add(e72);  // edge v42  --> v9
		ledges10.get(v61.get_id()).add(e51);   // edge v61  --> v71
		g10 = new Graph(vmap10, ledges10);   

		
		rs00 = new RouteSearch(g00);
		rs01 = new RouteSearch(g01);
		rs02 = new RouteSearch(g02);
		rs03 = new RouteSearch(g03);
		rs04 = new RouteSearch(g04);
		rs05 = new RouteSearch(g05);
		rs06 = new RouteSearch(g06);
		rs07 = new RouteSearch(g07);
		rs08 = new RouteSearch(g08);
		rs09 = new RouteSearch(g09);
		rs10 = new RouteSearch(g10);
	}
}
