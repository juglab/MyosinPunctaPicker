package de.csbd.learnathon.command;


public class FlowVector {

	float x;
	float y;
	int t;
	double u;
	double v;

	public FlowVector( float x, float y, int t, double u, double v ) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.u = u;
		this.v = v;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public int getT() {
		return t;
	}

	public double getU() {
		return u;
	}

	public double getV() {
		return v;
	}

}
