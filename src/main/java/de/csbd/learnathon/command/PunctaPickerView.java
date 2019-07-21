package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
	private JSlider windowSizeSlider;

	private ButtonGroup pickingModeButtons;

	private JTextField txtDefaultPunctaRadius;
	private JTextField txtMinScale;
	private JTextField txtMaxScale;
	private JTextField txtStepScale;
	private JTextField txtMaxDist;

	private JCheckBox showPreviousMarkerCheckBox;

	private JCheckBox showFlowCheckBox;

	private JTextField txtNeighbors;

	private JCheckBox showTrackletsCheckBox;

	private boolean showAutoFlowOnlyFlag;

	private JSlider relWeightSlider;

	private JSlider fadeOutSlider;

	private JSlider densitySlider;

	private JSlider thresholdingSlider;

	private ButtonGroup matchingModeButtons;

	private ButtonGroup thresholdingModeButtons;

	private JTextField txtThreshold;

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
		return Utils.getSelectedButtonText( pickingModeButtons );
	}

	public String getMatchingMode() {
		return Utils.getSelectedButtonText( matchingModeButtons );
	}

	public String getThresholdingMode() {
		return Utils.getSelectedButtonText( thresholdingModeButtons );
	}

	public int getAutoFlowMatchingWindowSize() {
		return windowSizeSlider.getValue();
	}

	public double getRelWeight() {
		return relWeightSlider.getValue();
	}

	public float getThreshold() {
		if ( txtThreshold.getText().isEmpty() )
			return 20f;
		else
			return Float.valueOf( txtThreshold.getText().trim() ).floatValue();
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

	public boolean getShowFlowCheckBox() {
		return showFlowCheckBox.isSelected();
	}

	public boolean getShowTrackletsCheckBox() {
		return showTrackletsCheckBox.isSelected();
	}

	public CSVReader getReader() {
		return reader;
	}

	public BdvHandlePanel getBdv() {
		return bdv;
	}

	public int getFadeOutValue() {
		return fadeOutSlider.getValue();
	}

	public int getDensity() {
		return densitySlider.getValue();
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

	private void sliderChanged( JSlider slider ) {
		slider.getValue();
		bdv.getViewerPanel().requestRepaint();

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

		// TRACKLETS PROPS

		JPanel panelTrackletsProps = new JPanel( new MigLayout() );
		panelTrackletsProps.setBorder( BorderFactory.createTitledBorder( "tracklets" ) );

		showTrackletsCheckBox = new JCheckBox( "show" );
		showTrackletsCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( getShowTrackletsCheckBox() )
					overlay.setVisible( true );
				else
					overlay.setVisible( false );
				bdv.getViewerPanel().requestRepaint();

			}
		} );

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
		fadeOutSlider = new JSlider( 0, 20, 4 );
		fadeOutSlider.addChangeListener( e -> sliderChanged( fadeOutSlider ) );
		fadeOutSlider.setVisible( true );

		panelTrackletsProps.add( showTrackletsCheckBox, "w 5%" );
		panelTrackletsProps.add( activeTrackletCheckBox, "w 5%, growx, wrap" );
		panelTrackletsProps.add( showPreviousMarkerCheckBox, "span 2, align left, growx, wrap" );
		panelTrackletsProps.add( lFadeOut, "" );
		panelTrackletsProps.add( fadeOutSlider, "growx, wrap" );

		// PICKING PROPS

		JPanel panelPickingProps = new JPanel( new MigLayout() );
		panelPickingProps.setBorder( BorderFactory.createTitledBorder( "picking" ) );

		pickingModeButtons = new ButtonGroup();
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
		pickingModeButtons.add( bManual );
		pickingModeButtons.add( bAutomaticSize );
		pickingModeButtons.add( bAutomaticSizeAndPos );

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

		showFlowCheckBox = new JCheckBox( "show" );
		showFlowCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( getShowFlowCheckBox() )
					flowOverlay.setVisible( true );
				else
					flowOverlay.setVisible( false );
				bdv.getViewerPanel().requestRepaint();
			}
		} );

		JCheckBox showAutoFlowOnlyCheckBox = new JCheckBox( "show auto only" );
		showAutoFlowOnlyCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( showAutoFlowOnlyFlag ) {
					showAutoFlowOnlyFlag = false;
					showFlowCheckBox.setEnabled( true );
				}

				else {
					showAutoFlowOnlyFlag = true;
					showFlowCheckBox.setEnabled( false );
				}

				flowOverlay.setVisible( true );
				bdv.getViewerPanel().requestRepaint();
			}

		} );

		matchingModeButtons = new ButtonGroup();
		JRadioButton bGreedy = new JRadioButton( "greedy" );
		bGreedy.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				relWeightSlider.setEnabled( false );
			}
		} );

		JRadioButton bHungarian = new JRadioButton( "hungarian" );
		bHungarian.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				windowSizeSlider.setEnabled( true );
				relWeightSlider.setEnabled( true );
			}
		} );

		matchingModeButtons.add( bGreedy );
		matchingModeButtons.add( bHungarian );

		JLabel lThreshold = new JLabel( "threshold:" );
		txtThreshold = new JTextField( 5 );
		txtThreshold.setText( "20.0" );

		thresholdingModeButtons = new ButtonGroup();
		JRadioButton bCustom = new JRadioButton( "custom threshold" );
		bCustom.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				txtThreshold.setEnabled( true );
			}
		} );

		JRadioButton bOtsu = new JRadioButton( "otsu" );
		bOtsu.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				txtThreshold.setEnabled( false );
			}
		} );

		thresholdingModeButtons.add( bCustom );
		thresholdingModeButtons.add( bOtsu );

		JLabel lWindowSize = new JLabel( "window:" );
		windowSizeSlider = new JSlider( 1, 11, 5 );
		windowSizeSlider.setMajorTickSpacing( 2 );
		windowSizeSlider.setPaintTicks( true );
		windowSizeSlider.setPaintLabels( true );
		Hashtable< Integer, JLabel > position = new Hashtable< Integer, JLabel >();
		position.put( 1, new JLabel( "1" ) );
		position.put( 3, new JLabel( "3" ) );
		position.put( 5, new JLabel( "5" ) );
		position.put( 7, new JLabel( "7" ) );
		position.put( 9, new JLabel( "9" ) );
		position.put( 11, new JLabel( "11" ) );

		// Set the label to be drawn
		windowSizeSlider.setLabelTable( position );
		windowSizeSlider.addChangeListener( e -> sliderChanged( windowSizeSlider ) );
		windowSizeSlider.setVisible( true );

		JLabel lRelativeWeight = new JLabel( "rel. weight:" );
		relWeightSlider = new JSlider( 0, 10, 8 );
		relWeightSlider.setMajorTickSpacing( 2 );
		relWeightSlider.setPaintTicks( true );
		relWeightSlider.setPaintLabels( true );
		Hashtable< Integer, JLabel > positionWeight = new Hashtable< Integer, JLabel >();
		positionWeight.put( 0, new JLabel( "0" ) );
		positionWeight.put( 2, new JLabel( "0.2" ) );
		positionWeight.put( 4, new JLabel( "0.4" ) );
		positionWeight.put( 6, new JLabel( "0.6" ) );
		positionWeight.put( 8, new JLabel( "0.8" ) );
		positionWeight.put( 10, new JLabel( "1" ) );

		// Set the label to be drawn
		relWeightSlider.setLabelTable( positionWeight );
		relWeightSlider.addChangeListener( e -> sliderChanged( relWeightSlider ) );
		relWeightSlider.setVisible( true );

		JLabel lDensity = new JLabel( "density:" );
		densitySlider = new JSlider( 2, 50, 10 );
		densitySlider.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged( ChangeEvent e ) {
				densitySlider.getValue();
				flowOverlay.prepareSpacedFlow();
				bdv.getViewerPanel().requestRepaint();

			}
		} );
		densitySlider.setVisible( true );

		JButton bComputeSemiAutoFlow = new JButton( "(re)compute" );
		bComputeSemiAutoFlow.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {

				model.extractAndInitializeControlVectorsFromHandPickedTracklets();
				showFlowCheckBox.setSelected( true );
				showAutoFlowOnlyFlag = false;
				model.processSemiAutomatedFlow();
				ArrayList< FlowVector > handPickedSparseFlow = model.getFlowVectorsCollection().getSparsehandPickedFlowVectors();
				ArrayList< FlowVector > autoFeatureFlow = model.getFlowVectorsCollection().getAutofeatureFlowVectors();
				RandomAccessibleInterval< DoubleType > flowData = model.getFlowVectorsCollection().getDenseFlow();
				flowOverlay.setHandPickedSparseFlow( handPickedSparseFlow );
				flowOverlay.setAutoFeatureFlow( autoFeatureFlow );
				flowOverlay.setDenseFlow( flowData );
				flowOverlay.prepareSpacedFlow();
				flowOverlay.setVisible( true );
				flowOverlay.requestRepaint();

			}
		} );

		JButton bSaveFlows = new JButton( "save" );
		bSaveFlows.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				RandomAccessibleInterval< DoubleType > denseFlow = model.getFlowVectorsCollection().getDenseFlow();
				TiffSaver.chooseFlowFieldSaveDirectory( denseFlow );
			}
		} );

		JLabel lInterpolate = new JLabel( "Neighbors kNN:" );
		txtNeighbors = new JTextField( 2 );
		txtNeighbors.setText( "3" );

		panelFlowProps.add( showFlowCheckBox, " w 5%" );
		panelFlowProps.add( showAutoFlowOnlyCheckBox, "w 5%, growx, wrap" );
		panelFlowProps.add( bGreedy, "w 5%" );
		panelFlowProps.add( bHungarian, "w 5%, growx, wrap" );
		panelFlowProps.add( bCustom, "w 5%" );
		panelFlowProps.add( bOtsu, "w 5%, growx, wrap" );
		panelFlowProps.add( lThreshold, "" );
		panelFlowProps.add( txtThreshold, "growx, wrap" );
		panelFlowProps.add( lWindowSize, "" );
		panelFlowProps.add( windowSizeSlider, "growx, wrap" );
		panelFlowProps.add( lRelativeWeight, "" );
		panelFlowProps.add( relWeightSlider, "growx, wrap" );
		panelFlowProps.add( lDensity, "" );
		panelFlowProps.add( densitySlider, "growx, wrap" );
		panelFlowProps.add( lInterpolate, "w 5%" );
		panelFlowProps.add( txtNeighbors, "w 5%, growx, wrap" );
		panelFlowProps.add( bComputeSemiAutoFlow, "w 5%, " );
		panelFlowProps.add( bSaveFlows, "w 5%, growx, wrap" );

		helper.add( panelTrackletsProps, "growx, wrap" );
		helper.add( panelPickingProps, "growx, wrap" );
		helper.add( panelFlowProps, "growx, wrap" );

		// make default selection such that action is thrown
		bAutomaticSize.doClick();
		bHungarian.doClick();
		bOtsu.doClick();
		showTrackletsCheckBox.setSelected( true );

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

	public float getPickingWindowSize() {
		if ( txtMaxDist.getText().isEmpty() )
			return 55f;
		else
			return Float.valueOf( txtMaxDist.getText().trim() ).floatValue();
	}

	public FlowOverlay getFlowOverlay() {
		return flowOverlay;
	}

	public OpService getOs() {
		// TODO Auto-generated method stub
		return opService;
	}

	public boolean getShowAutoFlowOnlyFlag() {
		// TODO Auto-generated method stub
		return showAutoFlowOnlyFlag;
	}

}


