package de.csbd.learnathon.command;


public class Punctas {

	private float x;
	private float y;
	private int t;
	private int id;
	
	public Punctas( float x, float y, int t, int id ) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId( int id ) {
		this.id = id;
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

}
