package de.csbd.learnathon.command;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.IJ;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import net.miginfocom.swing.MigLayout;

@Plugin( menuPath = "Plugins>Puncta Picker GUI", type = Command.class )
public class FlowGuiCommand implements Command {

	@Parameter
	Dataset image;

	@Parameter
	Context context;
 
	@Parameter
	UIService ui;

	JFrame frame;

	private PunctaPickerView panel;
	private PunctaPickerModel model;

	@Override
	public void run() {
		model = new PunctaPickerModel( toDoubleType( image.getImgPlus().getImg() ) );
		panel = new PunctaPickerView( model, context );
		frame = new JFrame( image.getImgPlus().getSource() );
		frame.setLayout( new MigLayout( "", "[grow]", "[][]" ) );
		frame.add( panel.getPanel(), "h 100%, grow, wrap" );
		frame.addWindowListener( new WindowAdapter() {

			@Override
			public void windowClosed( final WindowEvent windowEvent ) {
				panel.close();
			}
		} );

		frame.setBounds( 100, 100, 1600, 1200 );
		frame.pack();
		frame.setVisible( true );
	}

	private RandomAccessibleInterval< DoubleType > toDoubleType( final RandomAccessibleInterval< ? extends RealType< ? > > image ) {
		if ( Util.getTypeFromInterval( image ) instanceof DoubleType )
			return ( RandomAccessibleInterval< DoubleType > ) image;
		return Converters.convert( image, ( i, o ) -> o.setReal( i.getRealDouble() ), new DoubleType() );
	}

	public static void main( final String... args ) {
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		IJ.openImage( "res/Stack.tif" ).show();

		imageJ.command().run( FlowGuiCommand.class, true );
	}
	
}