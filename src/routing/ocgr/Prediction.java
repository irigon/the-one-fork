package routing.ocgr;

import java.math.BigDecimal;
import java.math.RoundingMode;

import routing.cgr.Vertex;

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
	
	public abstract void update();

	public double getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	protected abstract void setName();

	protected void setValue(double val) {
		this.value = val;
	}

	protected void setName(String name) {
		this.name = name;
	}
	
	public abstract void connUp();
	public abstract void connDown();
	
	@Override
	public String toString() {
		return name + ": " + value;
	}
}
