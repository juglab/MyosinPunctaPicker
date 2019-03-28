package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
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
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;

public class PunctaPickerView {

	@Parameter
	private Context context;

	@Parameter
	private OpService opService;

	public Logger log;

	private Dataset image;

	private PunctaPickerModel model;
	private PunctaPickerController controller;
	private FlowController flowController;

	private CSVReader reader;

	public BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );
	private Overlay overlay;
	private GhostOverlay ghostOverlay;
	private FlowOverlay flowOverlay;

	private JCheckBox activeTrackletCheckBox;
	private int fadeOutValue;

	private ButtonGroup modeButtons;

	private JTextField txtDefaultPunctaRadius;
	private JTextField txtMinScale;
	private JTextField txtMaxScale;
	private JTextField txtStepScale;
	private JTextField txtMaxDist;

	private JCheckBox showPreviousMarkerCheckBox;

	private JCheckBox flowToggleCheckBox;

	private JCheckBox trackletToggleCheckBox;

	private JTextField txtNeighbors;

	public PunctaPickerView( PunctaPickerModel m, Dataset image, OpService os ) {
		this.model = m;
		this.image = image;
		this.ghostOverlay = new GhostOverlay( this );
		this.flowOverlay = new FlowOverlay( this );
		this.controller = new PunctaPickerController( m, this, os );
		model.setController( controller );
		model.setView( this );
		this.opService = os;
		this.overlay = new Overlay( model );
		bdv = initBdv( model.getRawData() );
		this.flowController = new FlowController( m, this );
		model.setFlowController( flowController );
	}

	public String getDetectionMode() {
		return Utils.getSelectedButtonText( modeButtons );
	}

	public float getDefaultPunctaRadius() {
		if ( txtDefaultPunctaRadius.getText().isEmpty() )
			return 0f;
		else
			return Float.valueOf( txtDefaultPunctaRadius.getText().trim() ).floatValue();
	}

	public boolean getActiveTrackletCheckBoxStatus() {
		return activeTrackletCheckBox.isSelected();
	}

	public boolean getPreviousMarkerCheckBoxStatus() {
		return showPreviousMarkerCheckBox.isSelected();
	}

	public boolean getFlowToggleCheckBox() {
		return flowToggleCheckBox.isSelected();
	}

	public boolean getTrackletToggleCheckBox() {
		return trackletToggleCheckBox.isSelected();
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

	public int getKNeighbors() {
		if ( txtNeighbors.getText().isEmpty() )
			return 3;
		else
			return Double.valueOf( txtNeighbors.getText().trim() ).intValue();
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
		BdvFunctions.showOverlay( flowOverlay, "flowoverlay", Bdv.options().addTo( bdv ) );
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

		// OVERLAY PROPS

		JPanel panelOverlayProps = new JPanel( new MigLayout() );
		panelOverlayProps.setBorder( BorderFactory.createTitledBorder( "overlay" ) );

		activeTrackletCheckBox = new JCheckBox( "hide all but selected" );
		activeTrackletCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				bdv.getViewerPanel().requestRepaint();
			}
		} );

		showPreviousMarkerCheckBox = new JCheckBox( "show previous marker" );
		showPreviousMarkerCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				bdv.getViewerPanel().requestRepaint();
			}
		} );

		JLabel lFadeOut = new JLabel( "decay:" );
		JSlider fadeOutSlider = new JSlider( 0, 20, 4 );
		fadeOutSlider.setVisible( true );
		fadeOutSlider.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged( ChangeEvent e ) {
				fadeOutValue = ( ( JSlider ) e.getSource() ).getValue();
				bdv.getViewerPanel().requestRepaint();
			}
		} );

		panelOverlayProps.add( activeTrackletCheckBox, "span 2, align left, growx, wrap" );
		panelOverlayProps.add( showPreviousMarkerCheckBox, "span 2, align left, growx, wrap" );
		panelOverlayProps.add( lFadeOut, "" );
		panelOverlayProps.add( fadeOutSlider, "growx, wrap" );

		// PICKING PROPS

		JPanel panelPickingProps = new JPanel( new MigLayout() );
		panelPickingProps.setBorder( BorderFactory.createTitledBorder( "picking" ) );

		modeButtons = new ButtonGroup();
		JRadioButton bManual = new JRadioButton( "constant radius" );
		bManual.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				txtDefaultPunctaRadius.setEnabled( true );
				txtMinScale.setEnabled( false );
				txtMaxScale.setEnabled( false );
				txtStepScale.setEnabled( false );
				txtMaxDist.setEnabled( false );
			}
		} );
		JRadioButton bAutomaticSize = new JRadioButton( "auto size" );
		bAutomaticSize.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				txtDefaultPunctaRadius.setEnabled( false );
				txtMinScale.setEnabled( true );
				txtMaxScale.setEnabled( true );
				txtStepScale.setEnabled( true );
				txtMaxDist.setEnabled( false );
			}
		} );
		JRadioButton bAutomaticSizeAndPos = new JRadioButton( "auto size&pos" );
		bAutomaticSizeAndPos.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				txtDefaultPunctaRadius.setEnabled( false );
				txtMinScale.setEnabled( true );
				txtMaxScale.setEnabled( true );
				txtStepScale.setEnabled( true );
				txtMaxDist.setEnabled( true );
			}
		} );
		modeButtons.add( bManual );
		modeButtons.add( bAutomaticSize );
		modeButtons.add( bAutomaticSizeAndPos );

		JLabel lDefaultPunctaRadius = new JLabel( "radius:" );
		txtDefaultPunctaRadius = new JTextField( 2 );
		txtDefaultPunctaRadius.setText( Integer.toString( 15 ) );

		JLabel lMinScale = new JLabel( "min \u03C3:" );
		txtMinScale = new JTextField( 5 );
		txtMinScale.setText( "2.0" );

		JLabel lMaxScale = new JLabel( "max \u03C3:" );
		txtMaxScale = new JTextField( 5 );
		txtMaxScale.setText( "20.0" );

		JLabel lStepScale = new JLabel( "\u0394\u03C3:" );
		txtStepScale = new JTextField( 5 );
		txtStepScale.setText( "1.0" );

		JLabel lMaxDist = new JLabel( "max dist:" );
		txtMaxDist = new JTextField( 5 );
		txtMaxDist.setText( "55" );

		panelPickingProps.add( bManual, "span 2, growx, wrap" );
		panelPickingProps.add( bAutomaticSize, "span 2, growx, wrap" );
		panelPickingProps.add( bAutomaticSizeAndPos, "span 2, gapbottom 15, growx, wrap" );

		panelPickingProps.add( lDefaultPunctaRadius, "" );
		panelPickingProps.add( txtDefaultPunctaRadius, "growx, wrap" );
		panelPickingProps.add( lMinScale, "" );
		panelPickingProps.add( txtMinScale, "growx, wrap" );
		panelPickingProps.add( lMaxScale, "" );
		panelPickingProps.add( txtMaxScale, "growx, wrap" );
		panelPickingProps.add( lStepScale, "" );
		panelPickingProps.add( txtStepScale, "growx, wrap" );
		panelPickingProps.add( lMaxDist, "" );
		panelPickingProps.add( txtMaxDist, "growx, wrap" );

		// FLOW PROPS

		JPanel panelFlowProps = new JPanel( new MigLayout() );
		panelFlowProps.setBorder( BorderFactory.createTitledBorder( "flow" ) );
		JButton computeFlowButton = new JButton( "(re)compute kNN flows" );
		computeFlowButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
//				System.out.println( model.getGraph().getEdges().size() );
//				if ( !( model.getFlowVectorsCollection().getEditedSpacedFlow() == null ) ) {
//
//					for ( FlowVector fv : model.getFlowVectorsCollection().getEditedSpacedFlow() ) {
//						Puncta pA = new Puncta( fv.getX(), fv.getY(), fv.getT(), 1 );
//						Puncta pB = new Puncta( ( float ) ( fv.getX() + fv.getU() ), ( float ) ( fv.getY() + fv.getV() ), fv.getT() + 1, 1 );
//						model.getGraph().addPuncta( pA );
//						model.getGraph().addPuncta( pB );
//						model.getGraph().addEdge( new Edge( pB, pA ) ); //Check this again, do
//					}
//					
//				}
				model.extractAndInitializeControlVectorsFromHandPickedTracklets();
//				System.out.println( "Handpicked edges number is:" + model.extractAndInitializeControlVectorsFromHandPickedTracklets().size() );
				if ( ( model.getFlowVectorsCollection().getSparsehandPickedFlowVectors().isEmpty() ) ) {
					JOptionPane optionPane =
							new JOptionPane( "Please select at least one point in each time frame for computing kNN based flows", JOptionPane.ERROR_MESSAGE );
					JDialog dialog =
							optionPane.createDialog( "Error!" );
					dialog.setAlwaysOnTop( true );
					dialog.setVisible( true );
					return;
				} else {
					for ( int t = 0; t < image.dimension( 2 ) - 1; t++ ) {
						ArrayList< FlowVector > vecs = model
								.getFlowVectorsCollection()
								.getFlowVectorsAtTime( t, model.getFlowVectorsCollection().getSparsehandPickedFlowVectors() );
						if ( vecs.isEmpty() ) {
							JOptionPane optionPane =
									new JOptionPane( "Please select at least one point in each time frame for computing kNN based flows", JOptionPane.ERROR_MESSAGE );
							JDialog dialog =
									optionPane.createDialog( "Error!" );
							dialog.setAlwaysOnTop( true );
							dialog.setVisible( true );
							break;
						}

					}
					flowToggleCheckBox.setSelected( true );
					trackletToggleCheckBox.setSelected( true );
					model.processFlow();
					ArrayList< FlowVector > handPickedSparseFlow = model.getFlowVectorsCollection().getSparsehandPickedFlowVectors();
					RandomAccessibleInterval< DoubleType > flowData = model.getFlowVectorsCollection().getDenseFlow();
					ArrayList< FlowVector > spacedFlow = model.getFlowVectorsCollection().getSpacedFlowVectors();
					flowOverlay.setHandPickedSparseFlow( handPickedSparseFlow );
					flowOverlay.setSpacedFlow( spacedFlow );
					flowOverlay.setDenseFlow( flowData );
					flowOverlay.setVisible( true );
					flowOverlay.requestRepaint();
				}
			}
		} );

		JLabel lNeighbors = new JLabel( "k neighbors:" );
		txtNeighbors = new JTextField( 5 );
		txtNeighbors.setText( "3" );

		flowToggleCheckBox = new JCheckBox( "show flows" );
		flowToggleCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( getFlowToggleCheckBox() )
					flowOverlay.setVisible( true );
				else flowOverlay.setVisible( false);
				bdv.getViewerPanel().requestRepaint();
			}
		} );

		trackletToggleCheckBox = new JCheckBox( "show tracklets with flow" );
		trackletToggleCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( getTrackletToggleCheckBox() )
					overlay.setVisible( true );
				else
					overlay.setVisible( false );
				bdv.getViewerPanel().requestRepaint();
			}
		} );

		panelFlowProps.add( computeFlowButton, "span 2, align left, growx, wrap" );
		panelFlowProps.add( lNeighbors, "" );
		panelFlowProps.add( txtNeighbors, "wrap" );
		panelFlowProps.add( flowToggleCheckBox, "span 2, align left, growx, wrap" );
		panelFlowProps.add( trackletToggleCheckBox, "span 2, align left, growx, wrap" );

		helper.add( panelOverlayProps, "growx, wrap" );
		helper.add( panelPickingProps, "growx, wrap" );
		helper.add( panelFlowProps, "growx, wrap" );

		// make default selection such that action is thrown
		bAutomaticSize.doClick();
		trackletToggleCheckBox.setSelected( false );
		flowToggleCheckBox.setSelected( false );

		return helper;
	}

	private JSplitPane initSplitPane( final JPanel left, final JPanel right ) {
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, left, right );
		splitPane.setOneTouchExpandable( true );
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

	public PunctaPickerController getPunctaPickerController() {
		return controller;
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

	public void setActiveTrackletCheckBoxStatus( boolean status ) {
		if ( status == true )
			activeTrackletCheckBox.setSelected( true );
		else
			activeTrackletCheckBox.setSelected( false );
		bdv.getViewerPanel().requestRepaint();

	}

	public void setShowPreviousMarkerCheckBoxStatus( boolean status ) {
		if ( status == true )
			showPreviousMarkerCheckBox.setSelected( true );
		else
			showPreviousMarkerCheckBox.setSelected( false );
		bdv.getViewerPanel().requestRepaint();

	}

	public float getWindowSize() {
		if ( txtMaxDist.getText().isEmpty() )
			return 55f;
		else
			return Float.valueOf( txtMaxDist.getText().trim() ).floatValue();
	}

	public FlowOverlay getFlowOverlay() {
		return flowOverlay;
	}

}


