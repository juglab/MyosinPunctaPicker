package de.csbd.learnathon.command;

import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class PunctaPickerView {

	@Parameter
	private Context context;

	@Parameter
	private OpService opService;

	public Logger log;

	public BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );

	private JTextField tMoveTime;

	private PunctaPickerModel model;

	private Dataset image;

	private PunctaPickerController controller;

	private CSVWriter writer;

	private CSVReader reader;

	private Overlay overlay;

	public CSVReader getReader() {
		return reader;
	}

	public BdvHandlePanel getBdv() {
		return bdv;
	}


	public PunctaPickerView( PunctaPickerModel m, Dataset image, OpService os ) {
		this.model = m;
		this.image=image;
		this.controller = new PunctaPickerController( m, this, os );
		model.setController( controller );
		model.setView( this );
		this.opService = os;
		this.overlay = new Overlay( model );
		bdv = initBdv( model.getRawData() );
		controller.defineBehaviour();
	}

	private < T extends RealType< T > & NativeType< T > > BdvHandlePanel initBdv( final RandomAccessibleInterval< T > img ) {
		final BdvHandlePanel bdv = getBdv();
		final BdvSource source = BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );
//		final T min = Util.getTypeFromInterval( img ).createVariable();
//		final T max = Util.getTypeFromInterval( img ).createVariable();
//		ImglibUtil.computeMinMax( Views.iterable( img ), min, max );
//		source.setDisplayRangeBounds( 0, max.getRealFloat() );
//		source.setDisplayRange( min.getRealFloat(), max.getRealFloat() );
		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );
		return bdv;
	}


	public JPanel getPanel() {
		return bdv.getViewerPanel();
	}

	public void close() {
		bdv.close();
	}

	public PunctaPickerModel getPunctaPickerModel() {
		return model;
	}

	public void setPunctaPickerModel( final PunctaPickerModel model ) {
		this.model = model;
	}

	public Overlay getOverlay() {
		return overlay;
	}

	public < T extends RealType< T > & NativeType< T > > Img getImage()
	{
		return this.image;}

}


