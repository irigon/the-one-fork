package routing.ocgr.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;

import core.SimClock;
import routing.ocgr.Vertex;

public abstract class Prediction {

	protected Vertex vertice; // change to private
	private double value;
	private String name;
	double timestamp;

	
	public Prediction(Vertex v) {
		vertice = v;
		timestamp=-1.0;
	}
	
	protected Vertex getVertex() {
		return vertice;
	}
	
	public double getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	protected abstract void setName();

	public void setValue(double val) {
		this.value = val;
	}

	protected void setName(String name) {
		this.name = name;
	}
	
	protected void setEnd(double time) {
		vertice.set_end(vertice.begin() + time);
	}
	
	public void setTimestamp() {
		timestamp = SimClock.getTime();
	}
	
	public void setTimestamp(double ts) {
		timestamp = ts;
	}

	public double getTimestamp() {
		return timestamp;
	}
		
	public abstract void update();
	public abstract void connUp();
	public abstract void connDown();
	
	@Override
	public String toString() {
		return name + ": " + value;
	}
}
