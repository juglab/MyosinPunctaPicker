package de.csbd.learnathon.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

import net.imglib2.realtransform.AffineTransform3D;

public class Utils {

	public static String getSelectedButtonText( ButtonGroup buttonGroup ) {
		for ( Enumeration< AbstractButton > buttons = buttonGroup.getElements(); buttons.hasMoreElements(); ) {
			AbstractButton button = buttons.nextElement();

			if ( button.isSelected() ) { return button.getText(); }
		}

		return null;
	}

	public static double extractScale( final AffineTransform3D t, final int axis ) { //TODO Move this method to Utils
		double sqSum = 0;
		final int c = axis;
		for ( int r = 0; r < 4; ++r ) {
			final double x = t.get( r, c );
			sqSum += x * x;
		}
		return Math.sqrt( sqSum );
	}

	public static ArrayList< FlowVector > findCorrespondencesforExperimentalFlowVectors(
			ArrayList< FlowVector > autofeatures,
			ArrayList< FlowVector > manualfeatures ) {
		ArrayList< FlowVector > correspondingVec = new ArrayList<>();
		for ( int i = 0; i < manualfeatures.size(); i++ ) {
			FlowVector closest = getClosestVector( manualfeatures.get( i ), autofeatures );
			correspondingVec.add( closest );
		}
		return correspondingVec;
	}

	public static FlowVector getClosestVector( FlowVector a, ArrayList< FlowVector > vecList ) {
		float minDist = 100000000;
		FlowVector closest = null;
		for ( int i = 0; i < vecList.size(); i++ ) {
			if ( vecList.get( i ).getT() == a.getT() ) {

				float dist = ( a.getX() - vecList.get( i ).getX() ) * ( a.getX() - vecList.get( i ).getX() ) + ( a
						.getY() - vecList.get( i ).getY() ) * ( a.getY() - vecList.get( i ).getY() );
				if ( dist < minDist ) {
					closest = vecList.get( i );
					minDist = dist;
				}
			}
		}
		return closest;
	}

	public static void saveExperimentalFlowVectors( //For experimental purposes only, needs deletion later
			ArrayList< FlowVector > autofeatures,
			ArrayList< FlowVector > manualfeatures ) {
		ArrayList< FlowVector > correspondingAutoFeatures = findCorrespondencesforExperimentalFlowVectors( autofeatures, manualfeatures );
		final JFileChooser chooser = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
		chooser.setDialogTitle( "Choose a directory to save your file: " );
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		chooser.showSaveDialog( null );

		File file = chooser.getSelectedFile();
		if ( !file.getName().endsWith( ".csv" ) ) {
			file = new File( file.getAbsolutePath() + ".csv" );
		}

		//Delimiter used in CSV file
		final String COMMA_DELIMITER = ",";
		final String NEW_LINE_SEPARATOR = "\n";

		//CSV file header
		final String FILE_HEADER = "x,y,u,v,t";
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter( file );

			//Write the CSV file header
			fileWriter.append( FILE_HEADER.toString() );

			//Add a new line separator after the header
			fileWriter.append( NEW_LINE_SEPARATOR );

			//Write a new object list to the CSV file
			int id = -1;
			for ( int i = 0; i < correspondingAutoFeatures.size(); i++ ) {
				id++;
				fileWriter.append( String.valueOf( manualfeatures.get( id ).getX() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( manualfeatures.get( id ).getY() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( manualfeatures.get( id ).getU() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( manualfeatures.get( id ).getV() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( manualfeatures.get( id ).getT() ) );
				fileWriter.append( NEW_LINE_SEPARATOR );
				fileWriter.append( String.valueOf( correspondingAutoFeatures.get( id ).getX() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( correspondingAutoFeatures.get( id ).getY() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( correspondingAutoFeatures.get( id ).getU() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( correspondingAutoFeatures.get( id ).getV() ) );
				fileWriter.append( COMMA_DELIMITER );
				fileWriter.append( String.valueOf( correspondingAutoFeatures.get( id ).getT() ) );
				fileWriter.append( NEW_LINE_SEPARATOR );
			}

			System.out.println( "CSV file was created successfully !!!" );

		} catch ( final Exception e ) {
			System.out.println( "Error in CsvFileWriter !!!" );
			e.printStackTrace();
		}
		finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch ( final IOException e ) {
				System.out.println( "Error while flushing/closing fileWriter !!!" );
				e.printStackTrace();
			}

		}

	}

}
