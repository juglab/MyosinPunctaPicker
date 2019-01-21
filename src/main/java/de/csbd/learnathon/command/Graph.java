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
		LinkedList< Edge > queueOut = new LinkedList<>();
		queueOut.addAll( this.getOutEdges( queryPuncta ) );
		LinkedList< Edge > queueIn = new LinkedList<>();
		queueIn.addAll( this.getInEdges( queryPuncta ) );
		
		LinkedList< Edge > visitedEdges = new LinkedList<>();
		
		while ( !queueIn.isEmpty() || !queueOut.isEmpty()) {
			if( !queueIn.isEmpty()) {
				Edge cIn = queueIn.poll();
				if(!visitedEdges.contains( cIn )) {
					ret.addPuncta( cIn.pA );
					ret.addEdge( cIn );
					queueIn.addAll( getInEdges( cIn.pA ) );
					for ( Edge e : getOutEdges( cIn.pA ) ) {
						if ( !visitedEdges.contains( e ) )
							queueOut.add( e );
					}
					visitedEdges.add( cIn );
				}
			}
			if( !queueOut.isEmpty()) {
				Edge cOut = queueOut.poll();
				if(!visitedEdges.contains( cOut )) {
					ret.addPuncta( cOut.pB );
					ret.addEdge( cOut );
					for ( Edge e : getInEdges( cOut.pB ) ) {
						if ( !visitedEdges.contains( e ) )
							queueIn.add( e );
					}
					queueOut.addAll( getOutEdges( cOut.pB ) );
					visitedEdges.add( cOut );
				}
			}

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
