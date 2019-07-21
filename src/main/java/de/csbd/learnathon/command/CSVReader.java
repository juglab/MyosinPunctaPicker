package de.csbd.learnathon.command;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

	public static float defaultRadius = 3f; // TODO this need thinking

	public static Graph loadCSV( String filename ) {

		String COMMA_DELIMITER = ",";

		ArrayList< Puncta > loadedPunctas = new ArrayList< Puncta >();
		ArrayList< Edge > loadedEdges = new ArrayList< Edge >();

		try (BufferedReader br = new BufferedReader( new FileReader( filename ) )) {
			String line;
			String headerLine = br.readLine();
			boolean indicator = false;
			while ( ( line = br.readLine() ) != null ) {
				line = line.trim().replaceAll( "^\"|\"$", "" );
				if ( line.equals( "edgep1,edgep2" ) ) {
					indicator = true;
					line = br.readLine(); 
				}
				if(!indicator) {
					String[] values = line.split( COMMA_DELIMITER );

					Puncta p = new Puncta( Float.parseFloat( values[ 1 ] ), Float.parseFloat( values[ 2 ] ), Integer
							.parseInt( values[ 3 ] ), defaultRadius );
					loadedPunctas.add( p );
					}
				else {
					String[] values = line.split( COMMA_DELIMITER );
					Edge e = new Edge( loadedPunctas.get( Integer.parseInt( values[ 0 ].trim().replaceAll( "^\"|\"$", "" ) ) ), loadedPunctas
							.get( Integer.parseInt( values[ 1 ].trim().replaceAll( "^\"|\"$", "" ) ) ) );
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

	public static Graph loadOldCSVs( String filename1, String filename2 ) {
		

		String COMMA_DELIMITER = ",";

		ArrayList< Puncta > loadedPunctas = new ArrayList< Puncta >();
		ArrayList< Edge > loadedEdges = new ArrayList< Edge >();
		int lineCounter = 0;
		try (BufferedReader br1 = new BufferedReader( new FileReader( filename1 ) )) {
			String line1;
			String headerLine1 = br1.readLine();

			while ( ( line1 = br1.readLine() ) != null ) {
				lineCounter += 1;
				String[] values1 = line1.split( COMMA_DELIMITER );
				Puncta p = new Puncta( Float.parseFloat( values1[ 3 ].trim() ), Float.parseFloat( values1[ 4 ].trim() ), Integer
						.parseInt( values1[ 7 ].trim() ), defaultRadius );
				loadedPunctas.add( p );
			}
		} catch ( FileNotFoundException e1 ) {
			System.out.println( "File not found!" );
			e1.printStackTrace();
		} catch ( IOException e1 ) {
			System.out.println( "File read problem/IO!" );
			e1.printStackTrace();
		}

		try (BufferedReader br2 = new BufferedReader( new FileReader( filename2 ) )) {
			String line2;
			String headerLine2 = br2.readLine();
			while ( ( line2 = br2.readLine() ) != null ) {

				String[] values2 = line2.split( COMMA_DELIMITER );
				Puncta p = new Puncta( Float.parseFloat( values2[ 1 ].trim() ), Float.parseFloat( values2[ 2 ].trim() ), Integer
						.parseInt( values2[ 4 ].trim() ), defaultRadius );
				loadedPunctas.add( p );
			}
			for ( int count = 0; count < lineCounter; count++ ) {
				Edge e = new Edge( loadedPunctas.get( count ), loadedPunctas.get( count + lineCounter ) );
				loadedEdges.add( e );
			}
			

		} catch ( FileNotFoundException e ) {
			System.out.println( "File not found!" );
			e.printStackTrace();
		} catch ( IOException e ) {
			System.out.println( "File read problem/IO!" );
			e.printStackTrace();
		}
		return new Graph( loadedPunctas, loadedEdges );
	}

}
