package de.csbd.learnathon.command;

public class LocalMaximaQuartet {
	
	int x;
	int y;
	int t;
	float v;
	
	public LocalMaximaQuartet( int x, int y, int t, float v ) {
		this.x = x;
		this.y = y;
		this.v = v;
		this.t = t;
	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	
	public float getV() {
		return v;
	}

	public int getT() {
		return t;
	}


}
