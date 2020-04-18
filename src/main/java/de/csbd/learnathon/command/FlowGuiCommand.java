package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
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
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;

/**
 * This is an implementation of manual puncta picking.
 *
 * @param <T>
 *            type
 * @author Mangal Prakash MPI-CBG / CSBD, Dresden
 */

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
		panel = new PunctaPickerView( model, image, context.getService( OpService.class ) );

		frame = new JFrame();
		frame.setLayout( new BorderLayout() );
		
		frame.add( panel.getPanel(), BorderLayout.CENTER );
		frame.addWindowListener( new WindowAdapter() {

			@Override
			public void windowClosed( final WindowEvent windowEvent ) {
				panel.close();
			}
		} );

		SimpleMenu smenu = new SimpleMenu( model );
		frame.setJMenuBar( smenu.createMenuBar() );
		frame.setBounds( getCenteredRectangle( 1200, 900 ) );
		frame.setVisible( true );
	}

	public static Rectangle getCenteredRectangle( int w, int h ) {
		final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		final int maxwidth = gd.getDisplayMode().getWidth();
		final int maxheight = gd.getDisplayMode().getHeight();
		w = Math.min( w, maxwidth );
		h = Math.min( h, maxheight );
		final int x = ( maxwidth - w ) / 2;
		final int y = ( maxheight - h ) / 2;
		return new Rectangle( x, y, w, h );
	}

	private RandomAccessibleInterval< DoubleType > toDoubleType( final RandomAccessibleInterval< ? extends RealType< ? > > image ) {
		if ( Util.getTypeFromInterval( image ) instanceof DoubleType )
			return ( RandomAccessibleInterval< DoubleType > ) image;
		RandomAccessibleInterval< DoubleType > image2 = Converters.convert( image, ( i, o ) -> o.setReal( i.getRealDouble() ), new DoubleType() );
		return image2;
	}
	
	public static void main( final String... args ) {
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		IJ.openImage( "res/Stack.tif" ).show();

		imageJ.command().run( FlowGuiCommand.class, true );
	}
	
}