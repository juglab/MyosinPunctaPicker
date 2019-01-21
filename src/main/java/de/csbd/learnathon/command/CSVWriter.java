package de.csbd.learnathon.command;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVWriter {

	//Delimiter used in CSV file
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	//CSV file header
	private static final String FILE_HEADER = "id,x,y,t";

	public static void writeCsvFile( String fileName, List< Puncta > punctaList, List< Edge > edgeList ) {

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter( fileName );

			//Write the CSV file header
			fileWriter.append( FILE_HEADER.toString() );

			//Add a new line separator after the header
			fileWriter.append( NEW_LINE_SEPARATOR );

			//Write a new student object list to the CSV file
			int id=-1;
			for ( Puncta puncta : punctaList ) {
				id++;
				fileWriter.append( String.valueOf( id) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( puncta.getX() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( puncta.getY() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( puncta.getT() ) );
				fileWriter.append( NEW_LINE_SEPARATOR );
			}
			
			fileWriter.append( "edgep1,edgep2" );
			fileWriter.append( NEW_LINE_SEPARATOR );
			
			for ( Edge e :  edgeList ) {
				fileWriter.append( String.valueOf( punctaList.indexOf(e.pA)) );			
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( punctaList.indexOf(e.pB)) );	
				fileWriter.append( NEW_LINE_SEPARATOR );
			}

			System.out.println( "CSV file was created successfully !!!" );

		} catch ( Exception e ) {
			System.out.println( "Error in CsvFileWriter !!!" );
			e.printStackTrace();
		}
		finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch ( IOException e ) {
				System.out.println( "Error while flushing/closing fileWriter !!!" );
				e.printStackTrace();
			}

		}
	}
}