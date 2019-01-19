package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

public class PunctaPickerModel {

	private int id = 0;
	private List< Punctas > puncta = new ArrayList<>();

	void setPuncta( List< Punctas > loadedPuncta ) {

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


	void addPuncta( float x, float y, int t ) {
		Punctas newPuncta = new Punctas( x, y, t, id );
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
