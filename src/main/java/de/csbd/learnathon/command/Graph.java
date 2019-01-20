package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Graph {

	List< Puncta > punctaList;
	List< Edge > edgeList;

	public Graph() {
		this.punctaList = new ArrayList<>();
		this.edgeList = new ArrayList<>();
	}

	public Graph( List< Puncta > p, List< Edge > e ) {
		this.punctaList = p;
		this.edgeList = e;
	}

	public List< Puncta > getPunctaList() {
		return punctaList;
	}

	public List< Edge > getEdgeList() {
		return edgeList;
	}

	public void mergeGraph( Graph g ) {
		punctaList.addAll( g.getPunctaList() );
		edgeList.addAll( g.getEdgeList() );

	}

	// To select the subgraph associated with a tracklet
	public void addEdges( List< Edge > edges ) {
		edgeList.addAll( edges );
	}

	public void addPunctas( List< Puncta > punctas ) {
		punctaList.addAll( punctas );
	}

	public void addEdge( Edge edge ) {
		edgeList.add( edge );
	}

	public void addPuncta( Puncta puncta ) {
		punctaList.add( puncta );
	}

	public void removeEdge( Edge e ) {
		edgeList.remove( e );
	}

	public void removePuncta( Puncta p ) {
		punctaList.remove( p );
	}

	public Graph selectSubgraphContaining( Puncta queryPuncta ) {
		Graph ret = new Graph();
		ret.addPuncta( queryPuncta );

		// traversing into the future
		LinkedList< Edge > queue = new LinkedList<>();
		queue.addAll( this.getOutEdges( queryPuncta ) );
		while ( !queue.isEmpty() ) {
			Edge ce = queue.poll();

			// feed our new graph
			ret.addPuncta( ce.pB );
			ret.addEdge( ce );

			queue.addAll( getOutEdges( ce.pB ) );
		}

		// traversing into the past
		queue = new LinkedList<>();
		queue.addAll( this.getInEdges( queryPuncta ) );
		while ( !queue.isEmpty() ) {
			Edge ce = queue.poll();

			// feed our new graph
			ret.addPuncta( ce.pA );
			ret.addEdge( ce );

			queue.addAll( getInEdges( ce.pA ) );
		}
		return ret;
	}

	private List< Edge > getOutEdges( Puncta queryPuncta ) {
		List< Edge > ret = new ArrayList<>();
		for ( Edge edge : edgeList ) {
			if ( edge.pA.equals( queryPuncta ) ) {
				ret.add( edge );
			}
		}
		return ret;
	}

	private List< Edge > getInEdges( Puncta queryPuncta ) {
		List< Edge > ret = new ArrayList<>();
		for ( Edge edge : edgeList ) {
			if ( edge.pB.equals( queryPuncta ) ) {
				ret.add( edge );
			}
		}
		return ret;
	}

	private List< Edge > getAdjecentEdges( Puncta queryPuncta ) {
		List< Edge > ret = getOutEdges( queryPuncta );
		ret.addAll( getInEdges( queryPuncta ) );
		return ret;
	}

	public boolean graphContainsPuncta( Puncta p ) {
		return punctaList.contains( p );
	}

	public boolean graphContainsEdge( Edge e ) {
		return edgeList.contains( e );
	}
}
