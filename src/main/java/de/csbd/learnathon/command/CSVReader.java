package de.csbd.learnathon.command;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

	public static ArrayList< Punctas > loadCSV( String filename ) {

		String COMMA_DELIMITER = ",";

		ArrayList< Punctas > loadedTracklets = new ArrayList< Punctas >();

		try (BufferedReader br = new BufferedReader( new FileReader( filename ) )) {
			String line;
			String headerLine = br.readLine();
			while ( ( line = br.readLine() ) != null ) {
				System.out.println( line );
				String[] values = line.split( COMMA_DELIMITER );
				System.out.println( values[ 0 ] );
				Punctas puncta = new Punctas( Float.parseFloat( values[ 0 ] ), Float.parseFloat( values[ 1 ] ), Integer
						.parseInt( values[ 2 ] ), Integer.parseInt( values[ 3 ] ) );
				loadedTracklets.add( puncta );
			}
		} catch ( FileNotFoundException e ) {
			System.out.println( "File not found!" );
			e.printStackTrace();
		} catch ( IOException e ) {
			System.out.println( "File read problem/IO!" );
			e.printStackTrace();
		}
		return loadedTracklets;
	}

}
