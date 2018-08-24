package routing.ocgr;

import java.util.List;

import core.DTNHost;
import routing.cgr.Vertex;

public class BufferSizeCapacity extends Capacity {

	public BufferSizeCapacity() {
		super();
		setName();
	}

	/**
	 * Update the buffer capacity of a vertex
	 * The buffer capacity of a Vertex is defined as the minimal
	 * host buffer size from the hosts of this contact
	 * 
	 * We consider that the buffer size of a host will most
	 * probably not change during the simulation time.
	 */
	@Override
	public void update(Vertex vertice) {
		List<DTNHost> hosts = vertice.get_hosts();
		double host1BufferSize = hosts.get(0).getRouter().getBufferSize();
		double host2BufferSize = hosts.get(1).getRouter().getBufferSize();
		setValue(Math.min(host1BufferSize, host2BufferSize));
	}

	@Override
	protected void setName() {
		super.setName("BufferSizeCapacity");
	}

}
