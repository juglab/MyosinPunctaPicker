package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
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

	private PunctaPickerModel< ? > model;
	private PunctaPickerController controller;
	private FlowController flowController;

	private CSVReader reader;

	public BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );
	private Overlay overlay;
	private GhostOverlay ghostOverlay;
	private FlowOverlay flowOverlay;
	private JCheckBox activeTrackletCheckBox;
	private JSlider windowSizeSliderForFlowComputation;
	private ButtonGroup pickingModeButtons;
	private JTextField txtDefaultPunctaRadius;
	private JTextField txtMinScale;
	private JTextField txtMaxScale;
	private JTextField txtStepScale;
	private JTextField txtMaxDist;
	private JCheckBox showPreviousMarkerCheckBox;
	private JCheckBox showFlowCheckBox;
	private JTextField txtKnnNeighborsForInterpolatedFlowComputation;
	private JCheckBox showTrackletsCheckBox;
	private boolean showAutoFlowOnlyFlag;
	private JSlider relWeightSliderForFlowComputation;
	private JSlider fadeOutSlider;
	private JSlider densitySlider;
	private ButtonGroup matchingModeButtons;
	private ButtonGroup thresholdingModeButtons;
	private JTextField txtThresholdForAutoPunctaDetection;
	private ButtonGroup flowModeButtons;
	private JTextField txtWindowAroundNeighbor;
	private ButtonGroup opticalFlowMixingModeButtons;
	private String opticalFlowMode;

	public <T> PunctaPickerView( PunctaPickerModel<?> m, Dataset image, OpService os ) {
		this.model = m;
		this.image = image;
		this.ghostOverlay = new GhostOverlay( this );
		this.flowOverlay = new FlowOverlay( this );
		this.controller = new PunctaPickerController( m, this, os );
		model.setController( controller );
		model.setView( this );
		this.opService = os;
		this.overlay = new Overlay( model );
		bdv = initBdv(model.getRawData() );
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
		return windowSizeSliderForFlowComputation.getValue();
	}

	public double getOpticalFlowModificationWindowSize() {
		if ( txtWindowAroundNeighbor.getText().isEmpty() )
			return 5d;
		else
			return Double.valueOf( txtWindowAroundNeighbor.getText().trim() ).doubleValue();
	}

	public double getRelWeight() {
		return relWeightSliderForFlowComputation.getValue();
	}

	public float getThreshold() {
		if ( txtThresholdForAutoPunctaDetection.getText().isEmpty() )
			return 20f;
		else
			return Float.valueOf( txtThresholdForAutoPunctaDetection.getText().trim() ).floatValue();
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
		if ( txtKnnNeighborsForInterpolatedFlowComputation.getText().isEmpty() )
			return 3;
		else
			return Double.valueOf( txtKnnNeighborsForInterpolatedFlowComputation.getText().trim() ).intValue();
	}

	private void sliderChanged( JSlider slider ) {
		slider.getValue();
		bdv.getViewerPanel().requestRepaint();

	}

	private < T extends RealType< T >> BdvHandlePanel initBdv( final RandomAccessibleInterval< T > img ) {
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

	private < T extends RealType< T > > void computeMinMax(
			final IterableInterval< T > iterableInterval,
			final T min,
			final T max ) {
		if ( iterableInterval == null ) { return; }
		final Iterator< T > iterator = iterableInterval.iterator();
		T type = iterator.next();

		min.set( type );
		max.set( type );
		while ( iterator.hasNext() ) {
			type = iterator.next();

			if ( type.compareTo( min ) < 0 ) min.set( type );

			if ( type.compareTo( max ) > 0 ) max.set( type );
		}

	}

	public JPanel getPanel() {
		final JPanel controls = initControlsPanel();
		final JScrollPane scrollp = new JScrollPane( controls );
		return wrapToJPanel( initSplitPane( scrollp, bdv.getViewerPanel() ) );
	}

	private JPanel initControlsPanel() {
		final MigLayout layout = new MigLayout( "gap rel 0", "[grow]", "" );
		final JPanel controls = new JPanel( layout );
		final JPanel helper = initHelperPanel();
		controls.add( helper, "h 100%, grow, wrap" );
		return controls;
	}

	private JPanel initHelperPanel() {
		final JPanel helper = new JPanel( new MigLayout( "gap rel 0", "[grow]", "" ) );

		// TRACKLETS PROPS

		JPanel panelTrackletsProps = new JPanel( new MigLayout( "gap rel 0", "[grow]", "" ) );
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

		JPanel panelPickingProps = new JPanel( new MigLayout( "gap rel 0", "[grow]", "" ) );
		panelPickingProps.setBorder( BorderFactory.createTitledBorder( "picking" ) );

		pickingModeButtons = new ButtonGroup();
		JRadioButton bConstantRadius = new JRadioButton( "constant radius" );
		bConstantRadius.addActionListener( new ActionListener() {

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
		pickingModeButtons.add( bConstantRadius );
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

		panelPickingProps.add( bConstantRadius, "span 2, growx, wrap" );
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

		// FLOW VISUALIZATION PROPS

		JPanel panelFlowVisualizationProps = new JPanel( new MigLayout( "gap rel 0", "[grow]", "" ) );
		panelFlowVisualizationProps.setBorder( BorderFactory.createTitledBorder( "flow visualization" ) );

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

		panelFlowVisualizationProps.add( showFlowCheckBox, " w 5%" );
		panelFlowVisualizationProps.add( showAutoFlowOnlyCheckBox, "w 5%, growx, wrap" );
		panelFlowVisualizationProps.add( lDensity, "" );
		panelFlowVisualizationProps.add( densitySlider, "growx, wrap" );

		// FLOW CURATION PROPS
		JPanel panelFlowCurationProps = new JPanel( new MigLayout( "gap rel 0", "[grow]", "" ) );
		panelFlowCurationProps.setBorder( BorderFactory.createTitledBorder( "flow curation" ) );

		opticalFlowMixingModeButtons = new ButtonGroup();
		JRadioButton bOnlyGtBasedCuration = new JRadioButton( "prefer hand picked only" );
		bOnlyGtBasedCuration.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				opticalFlowMode = "prefer hand picked only";
			}
		} );
		JRadioButton bLinearBlendBasedCuration = new JRadioButton( "linear blend" );
		bLinearBlendBasedCuration.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				opticalFlowMode = "linear blend";
			}
		} );
		JRadioButton bGaussianBlendBasedCuration = new JRadioButton( "gaussian blend" );
		bGaussianBlendBasedCuration.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				opticalFlowMode = "gaussian blend";
			}
		} );

		opticalFlowMixingModeButtons.add( bOnlyGtBasedCuration );
		opticalFlowMixingModeButtons.add( bLinearBlendBasedCuration );
		opticalFlowMixingModeButtons.add( bGaussianBlendBasedCuration );

		JLabel lFlowCurationWindowAroundNeighor = new JLabel( "modfication window:" );
		txtWindowAroundNeighbor = new JTextField( 2 );
		txtWindowAroundNeighbor.setText( "3" );

		JButton bModifyFlowBasedOnCuration = new JButton( "Modify" );
		bModifyFlowBasedOnCuration.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				List< FlowVector > handPickedTracklets = model.extractAndInitializeControlVectorsFromHandPickedTracklets();
				if ( handPickedTracklets != null && !handPickedTracklets.isEmpty()) {
					model.modifyOpticalFlow( opticalFlowMode );
//					ArrayList< FlowVector > handPickedSparseFlow = model.getFlowVectorsCollection().getSparsehandPickedFlowVectors();
					RandomAccessibleInterval<?> flowData = model.getFlowVectorsCollection().getDenseFlow();
//					flowOverlay.setHandPickedSparseFlow( handPickedSparseFlow );
					flowOverlay.setDenseFlow( flowData );
					flowOverlay.prepareSpacedFlow();
					flowOverlay.setVisible( true );
					flowOverlay.requestRepaint();
				}
			}
		} );

		JButton bResetCuratedOpticalFlow = new JButton( "Reset" );
		bResetCuratedOpticalFlow.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( !( model.getFlowVectorsCollection().getOriginalOpticalFlow() == null ) ) {
					model.resetOpticalFlow();
					flowOverlay.setDenseFlow( model.getFlowVectorsCollection().getOriginalOpticalFlow() );
					flowOverlay.prepareSpacedFlow();
					flowOverlay.setVisible( true );
					flowOverlay.requestRepaint();
				}
			}
		} );

		panelFlowCurationProps.add( bOnlyGtBasedCuration, "w 5%, growx, wrap" );
		panelFlowCurationProps.add( bLinearBlendBasedCuration, "w 5%, growx, wrap" );
		panelFlowCurationProps.add( bGaussianBlendBasedCuration, "w 5%, growx, wrap" );
		panelFlowCurationProps.add( lFlowCurationWindowAroundNeighor, "w 5%" );
		panelFlowCurationProps.add( txtWindowAroundNeighbor, "w 5%, growx, wrap" );
		panelFlowCurationProps.add( bModifyFlowBasedOnCuration, "w 5%" );
		panelFlowCurationProps.add( bResetCuratedOpticalFlow, "w 5%, growx, wrap" );

		// COMPUTE DENSE FLOW PROPS
		JPanel panelFlowComputationProps = new JPanel( new MigLayout( "gap rel 0", "[grow]", "" ) );
		panelFlowComputationProps.setBorder( BorderFactory.createTitledBorder( "compute dense flow" ) );

		JLabel lBasedOn = new JLabel( "Based on:" );
		JLabel lDetectionFilterForFullyAutoFlowComputation = new JLabel( "detection filter " );
		JLabel lMatchingMethodForFullyAutoFlowComputation = new JLabel( "matching method " );
		
		JPanel panelSubPanelForFullyAutoFlowComputation = new JPanel( new MigLayout( "gap rel 0", "[grow]", "" ) );
		panelSubPanelForFullyAutoFlowComputation.setBorder( BorderFactory.createTitledBorder( "auto augmentation options" )  );

		matchingModeButtons = new ButtonGroup();
		JRadioButton bGreedy = new JRadioButton( "greedy" );
		bGreedy.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				relWeightSliderForFlowComputation.setEnabled( false );
			}
		} );

		JRadioButton bHungarian = new JRadioButton( "hungarian" );
		bHungarian.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				windowSizeSliderForFlowComputation.setEnabled( true );
				relWeightSliderForFlowComputation.setEnabled( true );
			}
		} );

		matchingModeButtons.add( bGreedy );
		matchingModeButtons.add( bHungarian );

		txtThresholdForAutoPunctaDetection = new JTextField( 5 );
		txtThresholdForAutoPunctaDetection.setText( "20.0" );

		thresholdingModeButtons = new ButtonGroup();
		JRadioButton bManualThreshold = new JRadioButton( "manual threshold" );
		bManualThreshold.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				txtThresholdForAutoPunctaDetection.setEnabled( true );
			}
		} );

		JRadioButton bOtsu = new JRadioButton( "otsu" );
		bOtsu.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				txtThresholdForAutoPunctaDetection.setEnabled( false );
			}
		} );

		thresholdingModeButtons.add( bManualThreshold );
		thresholdingModeButtons.add( bOtsu );
		
		JLabel lWindowSizeForFlowComputation = new JLabel( "window:" );
		windowSizeSliderForFlowComputation = new JSlider( 1, 11, 5 );
		windowSizeSliderForFlowComputation.setMajorTickSpacing( 2 );
		windowSizeSliderForFlowComputation.setPaintTicks( true );
		windowSizeSliderForFlowComputation.setPaintLabels( true );
		Hashtable< Integer, JLabel > position = new Hashtable< Integer, JLabel >();
		position.put( 1, new JLabel( "1" ) );
		position.put( 3, new JLabel( "3" ) );
		position.put( 5, new JLabel( "5" ) );
		position.put( 7, new JLabel( "7" ) );
		position.put( 9, new JLabel( "9" ) );
		position.put( 11, new JLabel( "11" ) );

		// Set the label to be drawn
		windowSizeSliderForFlowComputation.setLabelTable( position );
		windowSizeSliderForFlowComputation.addChangeListener( e -> sliderChanged( windowSizeSliderForFlowComputation ) );
		windowSizeSliderForFlowComputation.setVisible( true );

		JLabel lRelativeWeight = new JLabel( "rel. weight:" );
		relWeightSliderForFlowComputation = new JSlider( 0, 10, 8 );
		relWeightSliderForFlowComputation.setMajorTickSpacing( 2 );
		relWeightSliderForFlowComputation.setPaintTicks( true );
		relWeightSliderForFlowComputation.setPaintLabels( true );
		Hashtable< Integer, JLabel > positionWeight = new Hashtable< Integer, JLabel >();
		positionWeight.put( 0, new JLabel( "0" ) );
		positionWeight.put( 2, new JLabel( "0.2" ) );
		positionWeight.put( 4, new JLabel( "0.4" ) );
		positionWeight.put( 6, new JLabel( "0.6" ) );
		positionWeight.put( 8, new JLabel( "0.8" ) );
		positionWeight.put( 10, new JLabel( "1" ) );

		// Set the label to be drawn
		relWeightSliderForFlowComputation.setLabelTable( positionWeight );
		relWeightSliderForFlowComputation.addChangeListener( e -> sliderChanged( relWeightSliderForFlowComputation ) );
		relWeightSliderForFlowComputation.setVisible( true );
		
		panelSubPanelForFullyAutoFlowComputation.add( lDetectionFilterForFullyAutoFlowComputation, "w 5%, growx, wrap" );
		panelSubPanelForFullyAutoFlowComputation.add( bOtsu, "w 5%, growx, wrap" );
		panelSubPanelForFullyAutoFlowComputation.add( bManualThreshold, "w 5%" );
		panelSubPanelForFullyAutoFlowComputation.add( txtThresholdForAutoPunctaDetection, "growx, wrap" );
		panelSubPanelForFullyAutoFlowComputation.add( lMatchingMethodForFullyAutoFlowComputation, "w 5%, growx, wrap" );
		panelSubPanelForFullyAutoFlowComputation.add( bHungarian, "w 5%, growx, wrap" );
		panelSubPanelForFullyAutoFlowComputation.add( bGreedy, "w 5%, growx, wrap" );
		panelSubPanelForFullyAutoFlowComputation.add( lWindowSizeForFlowComputation, "" );
		panelSubPanelForFullyAutoFlowComputation.add( windowSizeSliderForFlowComputation, "growx, wrap" );
		panelSubPanelForFullyAutoFlowComputation.add( lRelativeWeight, "" );
		panelSubPanelForFullyAutoFlowComputation.add( relWeightSliderForFlowComputation, "growx, wrap" );

		JLabel lKnnInterpolateForFlowComputation = new JLabel( "Neighbors kNN:" );
		txtKnnNeighborsForInterpolatedFlowComputation = new JTextField( 2 );
		txtKnnNeighborsForInterpolatedFlowComputation.setText( "3" );

		JButton bActionImportOpticalFlow = new JButton( "Load" );
		bActionImportOpticalFlow.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				RandomAccessibleInterval< DoubleType > denseFlow = TiffLoader.loadFlowFieldFromDirectory();
				model.getFlowVectorsCollection().setDenseFlow( denseFlow );
				model.getFlowVectorsCollection().setOriginalOpticalFlow( denseFlow );
				flowOverlay.setDenseFlow( denseFlow );
				flowOverlay.prepareSpacedFlow();
				flowOverlay.setVisible( true );
				flowOverlay.requestRepaint();
			}
		} );

		flowModeButtons = new ButtonGroup();

		JRadioButton bLoadDenseFlowRadioButton = new JRadioButton( "load denseFlow" );
		bLoadDenseFlowRadioButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				relWeightSliderForFlowComputation.setEnabled( false );
				windowSizeSliderForFlowComputation.setEnabled( false );
				txtThresholdForAutoPunctaDetection.setEnabled( false );
				bManualThreshold.setEnabled( false );
				bOtsu.setEnabled( false );
				bGreedy.setEnabled( false );
				bHungarian.setEnabled( false );
				showAutoFlowOnlyCheckBox.setEnabled( false );
				bActionImportOpticalFlow.setEnabled( true );
				txtKnnNeighborsForInterpolatedFlowComputation.setEnabled( false );
			}
		} );

		JRadioButton bManualInterpFlowModeRadioButton = new JRadioButton( "manually picked tracklets" );
		bManualInterpFlowModeRadioButton.setActionCommand( "manually picked tracklets" );
		bManualInterpFlowModeRadioButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				relWeightSliderForFlowComputation.setEnabled( false );
				windowSizeSliderForFlowComputation.setEnabled( false );
				txtThresholdForAutoPunctaDetection.setEnabled( false );
				bManualThreshold.setEnabled( false );
				bOtsu.setEnabled( false );
				bGreedy.setEnabled( false );
				bHungarian.setEnabled( false );
				bActionImportOpticalFlow.setEnabled( false );
				txtKnnNeighborsForInterpolatedFlowComputation.setEnabled( true );
				showAutoFlowOnlyCheckBox.setEnabled( false );
			}
		} );
		JRadioButton bSemiAutoInterpFlowModeRadioButton = new JRadioButton( "manual + auto augmented" );
		bSemiAutoInterpFlowModeRadioButton.setActionCommand( "manual + auto augmented" );
		bSemiAutoInterpFlowModeRadioButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				relWeightSliderForFlowComputation.setEnabled( true );
				windowSizeSliderForFlowComputation.setEnabled( true );
				txtThresholdForAutoPunctaDetection.setEnabled( true );
				bManualThreshold.setEnabled( true );
				bOtsu.setEnabled( true );
				bGreedy.setEnabled( true );
				bHungarian.setEnabled( true );
				bActionImportOpticalFlow.setEnabled( false );
				txtKnnNeighborsForInterpolatedFlowComputation.setEnabled( true );
				showAutoFlowOnlyCheckBox.setEnabled( true );
			}
		} );

		JRadioButton bOpticalFlowFarnebackRadioButton = new JRadioButton( "Optical Flow Farneback" );
		bOpticalFlowFarnebackRadioButton.setActionCommand( "Optical Flow Farneback" );
		bOpticalFlowFarnebackRadioButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				relWeightSliderForFlowComputation.setEnabled( false );
				windowSizeSliderForFlowComputation.setEnabled( false );
				txtThresholdForAutoPunctaDetection.setEnabled( false );
				bManualThreshold.setEnabled( false );
				bOtsu.setEnabled( false );
				bGreedy.setEnabled( false );
				bHungarian.setEnabled( false );
				bActionImportOpticalFlow.setEnabled( false );
				txtKnnNeighborsForInterpolatedFlowComputation.setEnabled( false );
				showAutoFlowOnlyCheckBox.setEnabled( false );
			}
		} );

		flowModeButtons.add( bOpticalFlowFarnebackRadioButton );
		flowModeButtons.add( bManualInterpFlowModeRadioButton );
		flowModeButtons.add( bLoadDenseFlowRadioButton );
		flowModeButtons.add( bSemiAutoInterpFlowModeRadioButton );

		JButton bComputeFlowBasedOnSelectedMode = new JButton( "(re)compute" );
		bComputeFlowBasedOnSelectedMode.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( flowModeButtons.getSelection().getActionCommand() == "manually picked tracklets" ) {
					flowComputation( flowModeButtons.getSelection().getActionCommand() );
					setFlowForVisulaization();
				} else if ( flowModeButtons.getSelection().getActionCommand() == "manual + auto augmented" ) {
					flowComputation( flowModeButtons.getSelection().getActionCommand() );
					setFlowForVisulaization();
				} else if ( flowModeButtons.getSelection().getActionCommand() == "Optical Flow Farneback" ) {
					flowComputation( flowModeButtons.getSelection().getActionCommand() );
					setFlowForVisulaization();
				}

			}

			private void flowComputation( String string ) {
				model.extractAndInitializeControlVectorsFromHandPickedTracklets();
				showFlowCheckBox.setSelected( true );
				if ( string == "manually picked tracklets" ) {
					model.processManuallyInterpolatedFlow();
				} else if ( string == "manual + auto augmented" ) {
					showAutoFlowOnlyFlag = false;
					model.processSemiAutomatedFlow();
					List< FlowVector > autoFeatureFlow = model.getFlowVectorsCollection().getAutofeatureFlowVectors();
					flowOverlay.setAutoFeatureFlow( autoFeatureFlow );
				} else if ( string == "Optical Flow Farneback" ) {
					model.processOpticalFlowFernback(1, 0.5, showAutoFlowOnlyFlag, 5, 2, 5, 1.1, 0);
				}

			}

			private void setFlowForVisulaization() {
				List< FlowVector > handPickedSparseFlow = model.getFlowVectorsCollection().getSparsehandPickedFlowVectors();
				RandomAccessibleInterval< ? > flowData = model.getFlowVectorsCollection().getDenseFlow();
				flowOverlay.setHandPickedSparseFlow( handPickedSparseFlow );
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
				TiffSaver.chooseFlowFieldSaveDirectory( model.getFlowVectorsCollection().getDenseFlow());
			}
		} );

		panelFlowComputationProps.add( lBasedOn, "w 5%, growx, wrap" );
		panelFlowComputationProps.add( bOpticalFlowFarnebackRadioButton, "w 5%, growx, wrap" );
		panelFlowComputationProps.add( bManualInterpFlowModeRadioButton, "w 5%, growx, wrap" );
		panelFlowComputationProps.add( bLoadDenseFlowRadioButton, "w 5%" );
		panelFlowComputationProps.add( bActionImportOpticalFlow, "w 5%, growx, wrap" );
		panelFlowComputationProps.add( bSemiAutoInterpFlowModeRadioButton, "w 5%, growx, wrap" );
		panelFlowComputationProps.add( panelSubPanelForFullyAutoFlowComputation, "growx, wrap" );

		panelFlowComputationProps.add( lKnnInterpolateForFlowComputation, "w 5%" );
		panelFlowComputationProps.add( txtKnnNeighborsForInterpolatedFlowComputation, "w 5%, growx, wrap" );
		panelFlowComputationProps.add( bComputeFlowBasedOnSelectedMode, "w 5%" );
		panelFlowComputationProps.add( bSaveFlows, "w 5%, growx, wrap" );

		helper.add( panelTrackletsProps, "growx, wrap" );
		helper.add( panelPickingProps, "growx, wrap" );
		helper.add( panelFlowComputationProps, "growx, wrap" );
		helper.add( panelFlowVisualizationProps, "growx, wrap" );
		helper.add( panelFlowCurationProps, "growx, wrap" );

		// make default selection such that action is thrown
		bSemiAutoInterpFlowModeRadioButton.doClick();
		bAutomaticSize.doClick();
		bHungarian.doClick();
		bOtsu.doClick();
		showTrackletsCheckBox.setSelected( true );
		return helper;
	}

	private JSplitPane initSplitPane( final JScrollPane scrollp, final JPanel right ) {
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, scrollp, right );
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

	public PunctaPickerModel<?> getPunctaPickerModel() {
		return model;
	}

	public PunctaPickerController getPunctaPickerController() {
		return controller;
	}

	public void setPunctaPickerModel( final PunctaPickerModel<?> model ) {
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


