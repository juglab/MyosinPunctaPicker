package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

public class Graph {

	List< Puncta > punctaList;
	List< Edge > edgeList;

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

	public Graph selectSubgraph( Puncta queryPuncta, Graph changingGraph ) {
		Graph selectedGraph = new Graph( new ArrayList< Puncta >(), new ArrayList< Edge >() );
		Graph temporaryGraph;

		int i = -1;
		while ( i < changingGraph.getEdgeList().size() - 1 ) {
			i++;

			if ( ( queryPuncta.equals( changingGraph.getEdgeList().get( i ).getA() ) ) || ( queryPuncta
					.equals( changingGraph.getEdgeList().get( i ).getB() ) ) ) {
				Puncta associatedPuncta;
				if ( queryPuncta.equals( changingGraph.getEdgeList().get( i ).getA() ) )
					associatedPuncta = changingGraph.getEdgeList().get( i ).getB();
				else
					associatedPuncta = changingGraph.getEdgeList().get( i ).getA();
				selectedGraph.addEdge( changingGraph.getEdgeList().get( i ) );
				changingGraph.removeEdge( changingGraph.getEdgeList().get( i ) );
				temporaryGraph = selectSubgraph( associatedPuncta, changingGraph );
				selectedGraph.mergeGraph( temporaryGraph );
				i = -1;
			}
		}
		selectedGraph.addPuncta( queryPuncta );

		return selectedGraph;
	}
}
