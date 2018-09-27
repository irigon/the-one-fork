package routing.ocgr.metrics;

import routing.ocgr.Vertex;

public class BufferSizeCapacity extends Capacity {

	public BufferSizeCapacity(Vertex v) {
		super(v);
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
	public void update() {
		setValue(getVertex().buffer_free_capacity());
	}

	@Override
	protected void setName() {
		super.setName("BufferSizeCapacity");
	}
}
