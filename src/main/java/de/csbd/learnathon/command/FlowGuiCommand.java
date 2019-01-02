package de.csbd.learnathon.command;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

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

	private PunctaPickerPanel panel;

	@Override
	public void run() {
		panel = new PunctaPickerPanel( toDoubleType( image.getImgPlus().getImg() ), context);
		frame = new JFrame( image.getImgPlus().getSource() );
		frame.setLayout( new MigLayout( "", "[grow]", "[][]" ) );
		frame.add( panel.getPanel(), "h 100%, grow, wrap" );
		frame.addWindowListener( new WindowAdapter() {

			@Override
			public void windowClosed( WindowEvent windowEvent ) {
				panel.close();
			}
		} );

		frame.pack();
		frame.setSize( 500, 500 );
		frame.setVisible( true );
	}

	private RandomAccessibleInterval< DoubleType > toDoubleType( RandomAccessibleInterval< ? extends RealType< ? > > image ) {
		if ( Util.getTypeFromInterval( image ) instanceof DoubleType )
			return ( RandomAccessibleInterval< DoubleType > ) image;
		return Converters.convert( image, ( i, o ) -> o.setReal( i.getRealDouble() ), new DoubleType() );
	}

	public static void main( String... args ) {
		ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		imageJ.command().run( FlowGuiCommand.class, true );
	}
}