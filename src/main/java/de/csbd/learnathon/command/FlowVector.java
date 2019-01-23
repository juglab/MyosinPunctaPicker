package de.csbd.learnathon.command;


public class FlowVector {

	int x;
	int y;
	int t;
	double u;
	double v;

	public FlowVector( int x, int y, int t, double u, double v ) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.u = u;
		this.v = v;
	}

	public int getX() {
		return x;
	}

	public int getY() {
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
