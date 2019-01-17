package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

public class PunctaPickerModel {

	private final List< Punctas > puncta = new ArrayList<>();

	void addPuncta( float x, float y, int t ) {
		Punctas newPuncta = new Punctas( x, y, t );
		puncta.add( newPuncta );
	}

	List< Punctas > getPuncta() {
		return puncta;
	}

	ArrayList< Punctas > getPunctaAt( int t ) {
		List< Punctas > allPuncta = getPuncta();
		ArrayList< Punctas > queriedTimePunctaList = new ArrayList< Punctas >();
		for ( int i = 0; i < allPuncta.size(); i++ ) {
			Punctas currentPuncta = allPuncta.get( i );
			if ( currentPuncta.getT() == t ) {
				queriedTimePunctaList.add( currentPuncta );
			}
		}
		return queriedTimePunctaList;
	}

	List< Punctas > removePuncta( Punctas p ) {
		puncta.remove( p );
		return puncta;
	}
}
