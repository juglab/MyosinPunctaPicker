package de.csbd.learnathon.command;

public class Puncta {

	private float x;
	private float y;
	private int t;
	private float r;
	
	private boolean isSelected;

	public Puncta() {}

	public Puncta( float x, float y, int t, float r ) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.r = r;
		this.isSelected = false;
	}

	public float getX() {
		return x;
	}

	public void setX( float x ) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY( float y ) {
		this.y = y;
	}

	public int getT() {
		return t;
	}

	public void setT( int t ) {
		this.t = t;
	}

	public float getR() {
		return r;
	}

	public void setR( float r ) {
		this.r = r;
	}

	public boolean isEmpty() {
		Float value = new Float(this.x);
		return ( value == null );
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected( boolean selected ) {
		this.isSelected = selected;
	}
}
