package test;

import core.DTNHost;
import routing.EpidemicRouter;
import routing.MessageRouter;
import routing.cgr.Contact;
import routing.cgr.Edge;
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
	
	
	
	@Override
	public void setUp() throws Exception {
		super.setUp();

		h10 = utils.createHost(c0, "h10");
		h11 = utils.createHost(c0, "h11");
		h12 = utils.createHost(c0, "h12");
		h13 = utils.createHost(c0, "h13");
		
		c1 = new Contact(h10, h11, 0.0, 10.0);
		c2 = new Contact(h11, h12, 20.0, 30.0);
		c3 = new Contact(h12, h13, 40.0, 50.0);
		c4 = new Contact(h11, h10, 0.0, 10.0);
		c5 = new Contact(h11, h10, 100.0, 101.0);

		v1 = new Vertex("vertex_c1", c1, false);
		v2 = new Vertex("pivot_c4", c4, true);
		v3 = new Vertex("vertex_c2", c2, false);
		v4 = new Vertex("vertex_c3", c3, false);

		e1 = new Edge(v1, v2);
		e2 = new Edge(v1, v3);
		e3 = new Edge(v3, v4);





	}
}
