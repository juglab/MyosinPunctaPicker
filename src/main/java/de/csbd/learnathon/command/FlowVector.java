package de.csbd.learnathon.command;


public class FlowVector {

	private float x;
	private float y;
	private int t;
	private double u;
	private double v;

	private boolean isSelected;

	public FlowVector( float x, float y, int t, double u, double v ) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.u = u;
		this.v = v;
		this.isSelected = false;
	}

	public boolean isSelected() {
		return isSelected;
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

	public void setU( float x1 ) {
		u = x1 - getX();
	}

	public void setV( float y1 ) {
		v = y1 - getY();
	}

	public void setSelected( boolean selected ) {
		this.isSelected = selected;
	}

}
