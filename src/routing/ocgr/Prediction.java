package routing.ocgr;

import routing.cgr.Vertex;

public abstract class Prediction {

	double value;
	String name;
	
	public Prediction() {
	}
	
	public abstract void update(Vertex v);

	public double getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	protected abstract void setName();
	
	protected void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + ": " + value;
	}
}
