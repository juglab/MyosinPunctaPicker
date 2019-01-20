package de.csbd.learnathon.command;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

	public static ArrayList< Puncta > loadCSV( String filename ) {

		String COMMA_DELIMITER = ",";

		ArrayList< Puncta > loadedTracklets = new ArrayList< Puncta >();

		try (BufferedReader br = new BufferedReader( new FileReader( filename ) )) {
			String line;
			String headerLine = br.readLine();
			while ( ( line = br.readLine() ) != null ) {
				System.out.println( line );
				String[] values = line.split( COMMA_DELIMITER );
				System.out.println( values[ 0 ] );
				Puncta puncta = new Puncta( Float.parseFloat( values[ 0 ] ), Float.parseFloat( values[ 1 ] ), Integer
						.parseInt( values[ 2 ] ) );
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
