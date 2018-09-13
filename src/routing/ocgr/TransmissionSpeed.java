package routing.ocgr;

import java.util.List;

import core.DTNHost;
import routing.cgr.Vertex;

public class TransmissionSpeed extends Capacity {

	public TransmissionSpeed(Vertex v) {
		super(v);
		setName();
	}

	/**
	 * How much data can this contact exchange per time unit?
	 */
	@Override
	public void update() {
		setValue(getVertex().get_transmission_speed());
	}

	@Override
	protected void setName() {
		super.setName("TransmissionSpeed");
	}

}
