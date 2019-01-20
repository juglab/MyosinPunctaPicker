package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

public class PunctaPickerModel {

	public static String ACTION_NONE = "none";
	public static String ACTION_TRACK = "track";
	public static String ACTION_SELECT = "select";
	public static String ACTION_MODIFY = "modify";
	private String actionIndicator = ACTION_NONE;
	private List< Puncta > puncta = new ArrayList<>();
	private List< Edge > edges = new ArrayList<>();
	private Puncta latest;
	private Graph selectedSubgraph = new Graph( new ArrayList< Puncta >(), new ArrayList< Edge >() );
	private Puncta selectedPuncta;
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

	public void selectSubgraph( Graph g ) {
		selectedSubgraph = g;
	}

	public Graph getSelectedSubgraph()
	{
		return selectedSubgraph;
	}

	public void setActionIndicator( String s ) {
		actionIndicator = s;
	}

	public String getActionIndicator() {
		return actionIndicator;
	}

	public void deleteSelectedTracklet() {
		edges.removeAll( selectedSubgraph.getEdgeList() );
		puncta.removeAll( selectedSubgraph.getPunctaList() );
	}

	public void setSelectedPuncta( Puncta p ) {
		selectedPuncta = p;
	}

	public Puncta getSelectedPuncta() {
		return selectedPuncta;
	}

}
