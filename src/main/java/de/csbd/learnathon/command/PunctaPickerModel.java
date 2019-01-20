package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

public class PunctaPickerModel {

	private int id = 0;
	private List< Puncta > puncta = new ArrayList<>();
	private List< Edge > edges = new ArrayList<>();
	private Puncta latest;

	void setPuncta( List< Puncta > loadedPuncta ) {

		puncta = loadedPuncta;
	}

	public Puncta addPuncta( float x, float y, int t ) {
		Puncta newPuncta = new Puncta( x, y, t );
		puncta.add( newPuncta );
		this.latest = newPuncta;
		return newPuncta;
	}

	public void addEdge( Puncta p1, Puncta p2 ) {
		edges.add( new Edge( p1, p2 ) );
	}

	List< Puncta > getPuncta() {
		return puncta;
	}

	public Puncta getLatestPuncta() {
		return latest;
	}

	public List< Edge > getEdges() {
		return edges;
	}

	public void removeEdge( Edge edge ) {
		edges.remove( edge );
	}

	public void removePuncta( Puncta p ) {
		puncta.remove( p );
	}

	public Graph getGraph() {
		return new Graph( puncta, edges );
	}
}
