package de.csbd.learnathon.command;


public class FlowVector {

	int x;
	int y;
	int t;
	float u;
	float v;

	public FlowVector( int x, int y, int t, float u, float v ) {
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

	public float getU() {
		return u;
	}

	public float getV() {
		return v;
	}

}
