package de.csbd.learnathon.command;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

	public static Graph loadCSV( String filename ) {

		String COMMA_DELIMITER = ",";

		ArrayList< Puncta > loadedPunctas = new ArrayList< Puncta >();
		ArrayList< Edge > loadedEdges = new ArrayList< Edge >();

		try (BufferedReader br = new BufferedReader( new FileReader( filename ) )) {
			String line;
			String headerLine = br.readLine();
			boolean indicator = false;
			while ( ( line = br.readLine() ) != null ) {
				
				if ( line.equals( "edgep1,edgep2" ) ) {
					indicator = true;
					line = br.readLine(); 
				}
				if(!indicator) {
					String[] values = line.split( COMMA_DELIMITER );
					System.out.println( values[ 0 ] );
					Puncta p = new Puncta( Float.parseFloat( values[ 1 ] ), Float.parseFloat( values[ 2 ] ), Integer
							.parseInt( values[ 3 ] ) );
					loadedPunctas.add( p );
					}
				else {
					String[] values = line.split( COMMA_DELIMITER );
					Edge e = new Edge( loadedPunctas.get( Integer.parseInt( values[ 0 ] ) ), loadedPunctas.get( Integer.parseInt( values[ 1 ] ) ) );
					loadedEdges.add(e );
				}
				}
				
		} catch ( FileNotFoundException e ) {
			System.out.println( "File not found!" );
			e.printStackTrace();
		} catch ( IOException e ) {
			System.out.println( "File read problem/IO!" );
			e.printStackTrace();
		}
		return new Graph(loadedPunctas, loadedEdges);
	}

}
