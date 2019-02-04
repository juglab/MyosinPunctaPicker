package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Graph {

	private List< Puncta > punctas;
	private List< Edge > edges;

	// Lead selection is used as endpoint for new edges being added.
	private Puncta punctaLeadSelected;
	// Holds the latest puncta that was hovered over.
	private Puncta punctaMouseSelected;

	// Explain what the heck this is
	private Puncta latest;

	public Graph() {
		this.punctas = new ArrayList<>();
		this.edges = new ArrayList<>();

	}

	public Graph( List< Puncta > p, List< Edge > e ) {
		this.punctas = p;
		this.edges = e;
	}

	public List< Puncta > getPunctas() {
		return punctas;
	}

	public List< Edge > getEdges() {
		return edges;
	}

	public void mergeGraph( Graph g ) {
		punctas.addAll( g.getPunctas() );
		edges.addAll( g.getEdges() );

	}

	public void addEdges( List< Edge > edges ) {
		edges.addAll( edges );
	}

	public void addPunctas( List< Puncta > punctas ) {
		punctas.addAll( punctas );
	}

	public void addEdge( Edge edge ) {
		edges.add( edge );
	}

	public void addPuncta( Puncta puncta ) {
		punctas.add( puncta );
	}

	public void removeEdge( Edge e ) {
		edges.remove( e );
	}

	public void removePuncta( Puncta p ) {
		punctas.remove( p );
	}

	public void selectSubgraphContaining( Puncta queryPuncta ) {
		unselectAll();

		Set< Puncta > visited = new HashSet<>();
		visited.add( queryPuncta );
		queryPuncta.setSelected( true );

		LinkedList< Edge > queue = new LinkedList<>();
		queue.addAll( this.getAdjecentEdges( queryPuncta ) );
		while ( !queue.isEmpty() ) {
			Edge cIn = queue.poll();
			cIn.setSelected( true );

			if ( !visited.contains( cIn.getA() ) ) {
				queue.addAll( getAdjecentEdges( cIn.getA() ) );
				visited.add( cIn.getA() );
			}
			if ( !visited.contains( cIn.getB() ) ) {
				queue.addAll( getAdjecentEdges( cIn.getB() ) );
				visited.add( cIn.getB() );
			}
		}
	}

	public void unselectAll() {
		for ( Edge edge : edges ) {
			edge.setSelected( false );
		}
	}

	private List< Edge > getOutEdges( Puncta queryPuncta ) {
		List< Edge > ret = new ArrayList<>();
		for ( Edge edge : edges ) {
			if ( edge.getA().equals( queryPuncta ) ) {
				ret.add( edge );
			}
		}
		return ret;
	}

	private List< Edge > getInEdges( Puncta queryPuncta ) {
		List< Edge > ret = new ArrayList<>();
		for ( Edge edge : edges ) {
			if ( edge.getB().equals( queryPuncta ) ) {
				ret.add( edge );
			}
		}
		return ret;
	}

	public List< Edge > getAdjecentEdges( Puncta queryPuncta ) {
		List< Edge > ret = getOutEdges( queryPuncta );
		ret.addAll( getInEdges( queryPuncta ) );
		return ret;
	}

	public boolean graphContainsPuncta( Puncta p ) {
		return punctas.contains( p );
	}

	public boolean graphContainsEdge( Edge e ) {
		return edges.contains( e );
	}

	public void deleteSelectedElements() {
		Iterator< Edge > iterEdge = edges.iterator();
		while ( iterEdge.hasNext() ) {
			Edge edge = iterEdge.next();
			if ( edge.isSelected() ) {
				iterEdge.remove();
			}
		}

		Iterator< Puncta > iterPuncta = punctas.iterator();
		while ( iterPuncta.hasNext() ) {
			Puncta puncta = iterPuncta.next();
			if ( puncta.isSelected() ) {
				iterPuncta.remove();
			}
		}
	}

	public List< Puncta > getPunctaAtTime( int t ) {
		ArrayList< Puncta > ret = new ArrayList< Puncta >();
		for ( Puncta p : punctas ) {
			if ( p.getT() == t )
				ret.add( p );
		}
		return ret;
	}

	public Puncta getLeadSelectedPuncta() {
		return punctaLeadSelected;
	}

	public Puncta getMouseSelectedPuncta() {
		return punctaMouseSelected;
	}

	public void setLeadSelectedPuncta( Puncta p ) {
		punctaLeadSelected = p;
	}

	public void setMouseSelectedPuncta( Puncta p ) {
		punctaMouseSelected = p;
	}

	public void deleteSelectedPuncta() {
		for ( Edge e : getAdjecentEdges( getLeadSelectedPuncta() ) ) {
			edges.remove( e );
		}
		punctas.remove( getLeadSelectedPuncta() );
		
	}

}
