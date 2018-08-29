package routing.ocgr;

import routing.cgr.Vertex;

public class BufferFreeStdDev extends Prediction {
	
	int count;
	double oldDelta;
	double newDelta;
	double mean;
	double m2;
	

	public BufferFreeStdDev(Vertex v) {
		super(v);
		setName();
		count = 0;
		mean = 0.0;
		oldDelta = 0.0;
		m2 = Double.NaN; 
	}

	@Override
	public void update() {
		count = count + 1;
		double buffer1 = super.getVertex().get_hosts().get(0).getRouter().getFreeBufferSize();
		double buffer2 = super.getVertex().get_hosts().get(1).getRouter().getFreeBufferSize();
		double newValue = Math.min(buffer1, buffer2);
		// TODO: iri verify that this is correct
		if (count < 2) {
			oldDelta = 0.0;
			mean = newValue;
			m2 = 0.0;
			return;
		}
		oldDelta = newValue - mean;
		mean = mean + oldDelta / count;
		newDelta = newValue - mean;
		m2 = m2 + oldDelta * newDelta;
		setValue(Math.sqrt(m2 / count));
	}

	@Override
	protected void setName() {
		super.setName("BufferFreeStdDev");
	}

	@Override
	public void connUp() {}

	// updating capacity when the contact goes down.
	// TODO: iri Modify this to update on each transmission
	@Override
	public void connDown() {
		update();
	}
}
