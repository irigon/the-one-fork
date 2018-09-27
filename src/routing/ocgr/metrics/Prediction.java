package routing.ocgr.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;

import core.SimClock;
import routing.ocgr.Vertex;

public abstract class Prediction {

	private Vertex vertice; // change to private
	private double value;
	private String name;
	
	public Prediction(Vertex v) {
		vertice = v;
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
	
	public abstract void update();
	public abstract void connUp();
	public abstract void connDown();
	
	@Override
	public String toString() {
		return name + ": " + value;
	}
}
