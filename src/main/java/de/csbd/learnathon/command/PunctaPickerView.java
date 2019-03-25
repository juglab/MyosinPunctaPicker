package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;

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

	private int fadeOutValue;

	private GhostOverlay ghostOverlay;

	private JCheckBox activeTrackletCheckBox;

	private JTextField txtDefaultPunctaRadius;

	private ButtonGroup modeButtons;

	private JTextField txtMinScale;

	private JTextField txtMaxScale;

	private JTextField txtStepScale;

	public String getTrackingMode() {
		return Utils.getSelectedButtonText( modeButtons );
	}

	public float getDefaultPunctaRadius() {
		if ( txtDefaultPunctaRadius.getText().isEmpty() )
			return 0f;
		else
			return Float.valueOf( txtDefaultPunctaRadius.getText().trim() ).floatValue();
	}

	public boolean getCheckBoxStatus() {
		return activeTrackletCheckBox.isSelected();
	}

	public CSVReader getReader() {
		return reader;
	}

	public BdvHandlePanel getBdv() {
		return bdv;
	}

	public int getFadeOutValue() {
		return fadeOutValue;
	}

	public double getMinScale() {
		if ( txtMinScale.getText().isEmpty() )
			return 0d;
		else
			return Double.valueOf( txtMinScale.getText().trim() ).doubleValue();
	}

	public double getMaxScale() {
		if ( txtMaxScale.getText().isEmpty() )
			return 4d;
		else
			return Double.valueOf( txtMaxScale.getText().trim() ).doubleValue();
	}

	public double getStepScale() {
		if ( txtStepScale.getText().isEmpty() )
			return 1d;
		else
			return Double.valueOf( txtStepScale.getText().trim() ).doubleValue();
	}

	public PunctaPickerView( PunctaPickerModel m, Dataset image, OpService os ) {
		this.model = m;
		this.image=image;
		this.ghostOverlay = new GhostOverlay( this );
		this.controller = new PunctaPickerController( m, this, os );
		model.setController( controller );
		model.setView( this );
		this.opService = os;
		this.overlay = new Overlay( model );
		bdv = initBdv( model.getRawData() );
	}

	private < T extends RealType< T > & NativeType< T > > BdvHandlePanel initBdv( final RandomAccessibleInterval< T > img ) {
		final BdvHandlePanel bdv = getBdv();
		final BdvSource source = BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );
		final T min = Util.getTypeFromInterval( img ).createVariable();
		final T max = Util.getTypeFromInterval( img ).createVariable();
		computeMinMax( Views.iterable( img ), min, max );
		source.setDisplayRangeBounds( 0, max.getRealFloat() );
		source.setDisplayRange( min.getRealFloat(), max.getRealFloat() );
		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );
		BdvFunctions.showOverlay( ghostOverlay, "ghostoverlay", Bdv.options().addTo( bdv ) );
		return bdv;
	}


	private < T extends RealType< T > & NativeType< T > > void computeMinMax(
			final IterableInterval< T > iterableInterval,
			final T min,
			final T max ) {
		if ( iterableInterval == null ) { return; }

		// create a cursor for the image (the order does not matter)
		final Iterator< T > iterator = iterableInterval.iterator();

		// initialize min and max with the first image value
		T type = iterator.next();

		min.set( type );
		max.set( type );

		// loop over the rest of the data and determine min and max value
		while ( iterator.hasNext() ) {
			// we need this type more than once
			type = iterator.next();

			if ( type.compareTo( min ) < 0 ) min.set( type );

			if ( type.compareTo( max ) > 0 ) max.set( type );
		}

	}

	public JPanel getPanel() {
		final JPanel controls = initControlsPanel();
		return wrapToJPanel( initSplitPane( controls, bdv.getViewerPanel() ) );
//		return bdv.getViewerPanel();
	}

	private JPanel initControlsPanel() {
		final MigLayout layout = new MigLayout( "fill", "[grow]", "" );
		final JPanel controls = new JPanel( layout );
		final JPanel helper = initHelperPanel();
		controls.add( helper, "h 100%, grow, wrap" );
		return controls;
	}

	private JPanel initHelperPanel() {
		final JPanel helper = new JPanel( new MigLayout() );

		Font font = new Font( "Times", Font.ROMAN_BASELINE, 15 );

		JPanel panelOverlayProps = new JPanel( new MigLayout() );
		panelOverlayProps.setBorder( BorderFactory.createTitledBorder( "overlay props" ) );
		JLabel lFadeOut = new JLabel( "fadeout:" );
		lFadeOut.setFont( font );
		panelOverlayProps.add( lFadeOut, "growx, wrap" );
		JSlider fadeOutSlider = new JSlider( 0, 20, 0 );
//		fadeOutSlider.setMajorTickSpacing( 1 );
//		fadeOutSlider.setPaintTicks( true );
//		fadeOutSlider.setPaintLabels(true);
		
//		Hashtable position = new Hashtable();
//		for ( int i = 0; i <= 20; i = i + 2 ) {
//			position.put( i, new JLabel( Integer.toString( i ) ) );
//		}
//		fadeOutSlider.setLabelTable( position );
		fadeOutSlider.setVisible( true );
		fadeOutSlider.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged( ChangeEvent e ) {
				fadeOutValue = ( ( JSlider ) e.getSource() ).getValue();

			}
		} );

		activeTrackletCheckBox = new JCheckBox( "show tracklet for active only" );
		activeTrackletCheckBox.setFont( font );
		activeTrackletCheckBox.setHorizontalTextPosition( SwingConstants.LEFT );
		panelOverlayProps.add( fadeOutSlider, "growx, wrap" );
		panelOverlayProps.add( activeTrackletCheckBox, "growx, wrap" );

		JPanel panelTrackingProps = new JPanel( new MigLayout() );
		panelTrackingProps.setBorder( BorderFactory.createTitledBorder( "tracking props" ) );
		txtDefaultPunctaRadius = new JTextField( 2 );
		txtDefaultPunctaRadius.setText( Integer.toString( 15 ) );
		txtDefaultPunctaRadius.setFont( font );
		JLabel lDefaultPunctaRadius = new JLabel( "default radius:" );
		lDefaultPunctaRadius.setFont( font );
		panelTrackingProps.add( lDefaultPunctaRadius, "growx, wrap" );
		panelTrackingProps.add( txtDefaultPunctaRadius, "growx, wrap" );
		JRadioButton bAutomaticSize = new JRadioButton();
		JRadioButton bAutomaticSizeAndPos = new JRadioButton();
		JRadioButton bManual = new JRadioButton();
		modeButtons = new ButtonGroup();
		bAutomaticSize.setText( "automatically select size" );
		bAutomaticSizeAndPos.setText( "automatically select size and position" );
		bManual.setText( "manual" );
		bManual.setSelected( true );
		panelTrackingProps.add( bAutomaticSize, "growx, wrap" );
		panelTrackingProps.add( bAutomaticSizeAndPos, "growx, wrap" );
		panelTrackingProps.add( bManual, "growx, wrap" );
		modeButtons.add( bAutomaticSize );
		modeButtons.add( bAutomaticSizeAndPos );
		modeButtons.add( bManual );

		JPanel panelBlobDetectionProps = new JPanel( new MigLayout() );
		panelBlobDetectionProps.setBorder( BorderFactory.createTitledBorder( "blob detection props" ) );
		txtMinScale = new JTextField( 2 );
		txtMinScale.setText( Integer.toString( 2 ) );
		JLabel lMinScale = new JLabel( "min scale:" );
		lMinScale.setFont( font );
		txtMaxScale = new JTextField( 2 );
		txtMaxScale.setText( Integer.toString( 20 ) );
		JLabel lMaxScale = new JLabel( "max scale:" );
		lMaxScale.setFont( font );
		txtMinScale.setFont( font );
		txtStepScale = new JTextField( 2 );
		txtStepScale.setText( Integer.toString( 1 ) );
		JLabel lStepScale = new JLabel( "step scale:" );
		lStepScale.setFont( font );
		panelBlobDetectionProps.add( lMinScale, "growx" );
		panelBlobDetectionProps.add( txtMinScale, "growx, wrap" );
		panelBlobDetectionProps.add( lMaxScale, "growx" );
		panelBlobDetectionProps.add( txtMaxScale, "growx, wrap" );
		panelBlobDetectionProps.add( lStepScale, "growx" );
		panelBlobDetectionProps.add( txtStepScale, "growx, wrap" );

		helper.add( panelOverlayProps, "growx, wrap" );
		helper.add( panelBlobDetectionProps, "growx, wrap" );
		helper.add( panelTrackingProps, "growx, wrap" );
		return helper;
	}

	private JSplitPane initSplitPane( final JPanel left, final JPanel right ) {
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, left, right );
		splitPane.setOneTouchExpandable( true );
		splitPane.setDividerLocation( 100 );
		return splitPane;
	}

	private JPanel wrapToJPanel( final JSplitPane splitPane ) {
		final JPanel splittedPanel = new JPanel();
		splittedPanel.setLayout( new BorderLayout() );
		splittedPanel.add( splitPane, BorderLayout.CENTER );
		return splittedPanel;
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

	public GhostOverlay getGhostOverlay() {

		return ghostOverlay;
	}

}


