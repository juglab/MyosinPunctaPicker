package de.csbd.learnathon.command;

import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

public class PunctaPickerPanel {

	@Parameter
	private Context context;

	@Parameter
	private CommandService commandService;

	public Logger log;

	DefaultListModel< String > model = new DefaultListModel<>();

	private BdvHandlePanel bdv;

	private RandomAccessibleInterval< DoubleType > rawData;

	public PunctaPickerPanel( RandomAccessibleInterval< DoubleType > image, Context context ) {
		this.rawData = image;
		context.inject( this );

	}

	public JPanel getPanel() {
		bdv = initBdv( rawData );
		return bdv.getViewerPanel();
	}


	private < T > BdvHandlePanel initBdv( RandomAccessibleInterval< T > img ) {
		final BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );
		BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );
		return bdv;
	}

	public void close() {
		bdv.close();

	}



}
