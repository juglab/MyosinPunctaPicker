package de.csbd.learnathon.command;

import java.io.File;

import javax.swing.JFileChooser;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;

public class TiffSaver {
	
	public static void chooseFlowFieldSaveDirectory( RandomAccessibleInterval< DoubleType > denseFlow, String dir_path ) {
		final JFileChooser chooser = new JFileChooser( new File( dir_path ) );
		chooser.setDialogTitle( "Choose a directory to save flow fields: " );
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		chooser.showSaveDialog( null );

		File file = chooser.getSelectedFile();
		if ( !file.getName().endsWith( ".tif" ) ) {
			file = new File( file.getAbsolutePath() + ".tif" );
		}
		ImagePlus imagePlus = ImageJFunctions.wrap( denseFlow, null );
		System.out.println( file.getAbsolutePath() );
		IJ.save( imagePlus, file.getAbsolutePath() );
	}
}
