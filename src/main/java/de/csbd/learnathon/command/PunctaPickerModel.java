package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class PunctaPickerModel {

	private int id = 0;
	private List< Puncta > puncta = new ArrayList<>();
	private List< Pair< Puncta, Puncta > > edges = new ArrayList<>();
	private Puncta latest;

	void setPuncta( List< Puncta > loadedPuncta ) {

		puncta = loadedPuncta;
	}

	void increaseId() {
		id = id + 1;

		System.out.println( id );
	}

	void setMaxId() {
		if ( puncta != null && !puncta.isEmpty() ) {
			int maxId = 0;
			for ( int i = 0; i < puncta.size(); i++ ) {
				int id = puncta.get( i ).getId();
				if ( id > maxId ) {
					maxId = id;
				}

			}
			id = maxId;
		}

	}

	public Puncta addPuncta( float x, float y, int t ) {
		Puncta newPuncta = new Puncta( x, y, t, id );
		puncta.add( newPuncta );
		this.latest = newPuncta;
		return newPuncta;
	}

	public void addEdge( Puncta p1, Puncta p2 ) {
		edges.add( new ValuePair<>( p1, p2 ) );
	}

	List< Puncta > getPuncta() {
		return puncta;
	}

	ArrayList< Puncta > getPunctaAt( int t ) {
		List< Puncta > allPuncta = getPuncta();
		ArrayList< Puncta > queriedTimePunctaList = new ArrayList< Puncta >();
		for ( int i = 0; i < allPuncta.size(); i++ ) {
			Puncta currentPuncta = allPuncta.get( i );
			if ( currentPuncta.getT() == t ) {
				queriedTimePunctaList.add( currentPuncta );
			}
		}
		return queriedTimePunctaList;
	}

	List< Puncta > removePunctaAtIndex( int index ) {
		puncta.remove( index );
		return puncta;
	}

	public Puncta getLatestPuncta() {
		return latest;
	}
}
