package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import routing.cgr.Contact;
import routing.cgr.Edge;
import routing.cgr.Graph;
import routing.cgr.Vertex;

public class AbstractCGRRouterTest extends AbstractRouterTest {
	
	protected DTNHost h10;
	protected DTNHost h11;
	protected DTNHost h12;
	protected DTNHost h13;
	
	protected Contact c1;
	protected Contact c2;
	protected Contact c3;
	protected Contact c4;
	protected Contact c5;
	
	protected Vertex  v1;
	protected Vertex  v2;
	protected Vertex  v3;
	protected Vertex  v4;
	
	protected Edge    e1;
	protected Edge    e2;
	protected Edge    e3;
	
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
	
	protected Message m01;
	
	
	
	
	
	@Override
	public void setUp() throws Exception {
		super.setUp();

		h10 = utils.createHost(c0, "h10");
		h11 = utils.createHost(c0, "h11");
		h12 = utils.createHost(c0, "h12");
		h13 = utils.createHost(c0, "h13");
		
		c1 = new Contact(h11, h10, 0.0, 10.0);
		c2 = new Contact(h11, h10, 100.0, 101.0);
		c3 = new Contact(h10, h11, 0.0, 10.0);
		c4 = new Contact(h11, h12, 20.0, 30.0);
		c5 = new Contact(h12, h13, 40.0, 50.0);

		v1 = new Vertex("vertex_1", c3, false);
		v2 = new Vertex("pivot_c1", c1, true);
		v3 = new Vertex("vertex_3", c4, false);
		v4 = new Vertex("vertex_4", c5, false);

		e1 = new Edge(v1, v2);
		e2 = new Edge(v1, v3);
		e3 = new Edge(v3, v4);
		
		// Message from:h11 to:h12, id:"TestMessage", size: 10
		m01 = new Message(v3.get_hosts().get(0), v3.get_hosts().get(1), "TestMessage", 10);

		
		/*
		 * g1) the simplest graph with 1 vertex
		 * 
		 *  o
		 *  
		 */
		
		// create vertices, edges and graph
		Map<String, Vertex> vmap01 = new HashMap<>();
		Map<String, List<Edge>> ledges01 = new HashMap<>();
		vmap01.put(v3.get_id(), v3);
		for (String vname : vmap01.keySet()) {
			ledges01.put(vname, new ArrayList<>());
		}
		g01 = new Graph(vmap01 , ledges01);
		
		/*
		 * g2) a simple graph with 2 vertexes
		 *
		 *	o----o
		 */
		
		
		/*
		 * g3) extending it to 3 nodes in line
		 *
		 *	o----o----o
		 */
		
		
		/*
		 * g4) 3 nodes, with one vertex ending before now (to be pruned)
		 *
		 *	x----o----o
		 *
		 */
		
		/*
		 * g50) one start, two ends
		 * 		 o	
		 * 		/
		 * o---o
		 * 		\
		 * 		 o
		 * 
		 */

		
		/*
		 * g51) one end, two paths
		 * 		 o	
		 * 		/ \
		 * o---o   o
		 * 		\ /
		 * 		 o
		 * 
		 */
		
		/*
		 * g52) one end, two paths
		 * 		 o	
		 * 		/ \
		 * o---o   o----o
		 * 		\ /
		 * 		 o
		 * 
		 */
		
		/*
		 * g100) multipath
		 * 		 o	
		 * 		/ \
		 * o===o   o----o
		 * 		\ /
		 * 		 o
		 * 
		 */

		
	}
}
