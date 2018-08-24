package routing.ocgr;

import routing.cgr.Vertex;

public abstract class Capacity {

	private double value;
	private String name;
	
	public Capacity() {
	}
	
	@Override
	public String toString() {
		return name + ": " + value;
	}
	
	public abstract void update(Vertex vertice);
	
	public double getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	public void setValue(double v) {
		this.value = v;
	}

	protected abstract void setName();

	protected void setName(String name) {
		this.name = name;
	}
	
}
