package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;

import com.indago.util.ImglibUtil;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;

public class PunctaPickerPanel {

	@Parameter
	private Context context;

	@Parameter
	private CommandService commandService;

	public Logger log;

	private BdvHandlePanel bdv;

	private final RandomAccessibleInterval< DoubleType > rawData;

	private JTextField tMoveTime;

	private PunctaPickerModel model = new PunctaPickerModel();

	private PunctaClicker punctaClicker;

	private CSVWriter writer;

	private CSVReader reader;

	public CSVReader getReader() {
		return reader;
	}

	public PunctaClicker getPickBehaviour() {
		return punctaClicker;
	}

	public void setPickBehaviour( final PunctaClicker punctaClicker ) {
		this.punctaClicker = punctaClicker;
	}

	public PunctaPickerPanel( final RandomAccessibleInterval< DoubleType > image, final Context context ) {
		this.rawData = image;
		context.inject( this );
	}


	public JPanel getPanel() {
		final JPanel controls = initControlsPanel();
		bdv = initBdv( rawData );
		return wrapToJPanel( initSplitPane( controls, bdv.getViewerPanel() ) );
	}

	private JPanel wrapToJPanel( final JSplitPane splitPane ) {
		final JPanel splittedPanel = new JPanel();
		splittedPanel.setLayout( new BorderLayout() );
		splittedPanel.add( splitPane, BorderLayout.CENTER );
		return splittedPanel;
	}

	private JSplitPane initSplitPane( final JPanel left, final JPanel right ) {
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, left, right );
		splitPane.setOneTouchExpandable( true );
		splitPane.setDividerLocation( 300 );
		return splitPane;
	}

	private JPanel initControlsPanel() {
		final MigLayout layout = new MigLayout( "fill", "[grow]", "" );
		final JPanel controls = new JPanel( layout );
		final JPanel helper = initHelperPanel();
		controls.add( helper, "h 100%, grow, wrap" );
		return controls;
	}

	private JPanel initHelperPanel() {
		final JPanel helper = new JPanel(  );
		final GridBagLayout gbl_helper = new GridBagLayout();
		gbl_helper.columnWidths = new int[] { 200, 7, 200, 60, 18, 0 };
		gbl_helper.rowHeights = new int[] { 20, 20 };
		gbl_helper.columnWeights = new double[] { 1.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_helper.rowWeights = new double[] { 0.0, 0 };
		helper.setLayout( gbl_helper );

		final GridBagConstraints gbc1 = new GridBagConstraints();
	    gbc1.fill = GridBagConstraints.HORIZONTAL;
	    gbc1.gridwidth = 2;
		gbc1.anchor = GridBagConstraints.NORTHWEST;
	    gbc1.insets = new Insets(5,5,5,5);
	    gbc1.gridx = 0;
	    gbc1.gridy = 0;
		final JButton bAddPunctaFromCsv = initLoadTrackelts();
		helper.add( bAddPunctaFromCsv, gbc1 );

	    final GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.insets = new Insets( 0, 0, 0, 5 );
	    gbc3.gridx = 0;
		gbc3.gridy = 1;
		final JLabel label = new JLabel( "Move to time:" );
		helper.add( label, gbc3 );

		tMoveTime = new JTextField();
		tMoveTime.setColumns( 4 );
		tMoveTime.setMinimumSize( tMoveTime.getPreferredSize() );
		final GridBagConstraints gbc4 = new GridBagConstraints();
		gbc4.anchor = GridBagConstraints.WEST;
		gbc4.insets = new Insets( 0, 0, 0, 5 );
		gbc4.gridx = 1;
		gbc4.gridy = 1;
		helper.add( tMoveTime, gbc4 );

		final JButton bMoveTime = initMoveButton();
		final GridBagConstraints gbc5 = new GridBagConstraints();
		gbc5.insets = new Insets( 0, 0, 0, 5 );
		gbc5.anchor = GridBagConstraints.NORTHWEST;
		gbc5.gridx = 2;
		gbc5.gridy = 1;
		helper.add( bMoveTime, gbc5 );

		final GridBagConstraints gbc6 = new GridBagConstraints();
		gbc6.fill = GridBagConstraints.HORIZONTAL;
		gbc6.gridwidth = 2;
		gbc6.anchor = GridBagConstraints.NORTHWEST;
		gbc6.insets = new Insets( 5, 5, 5, 5 );
		gbc6.gridx = 0;
		gbc6.gridy = 2;
		final JButton bStartPickingPuncta = initPunctaPickingButton();
		helper.add( bStartPickingPuncta, gbc6 );

		final GridBagConstraints gbc15 = new GridBagConstraints();
		gbc15.fill = GridBagConstraints.HORIZONTAL;
		gbc15.gridwidth = 2;
		gbc15.anchor = GridBagConstraints.NORTHWEST;
		gbc15.insets = new Insets( 5, 5, 5, 5 );
		gbc15.gridx = 0;
		gbc15.gridy = 6;
		final JButton bPreviousTime = initPreviousTimeButton();
		helper.add( bPreviousTime, gbc15 );

		final GridBagConstraints gbc16 = new GridBagConstraints();
		gbc16.fill = GridBagConstraints.HORIZONTAL;
		gbc16.gridwidth = 2;
		gbc16.anchor = GridBagConstraints.NORTHWEST;
		gbc16.insets = new Insets( 5, 5, 5, 5 );
		gbc16.gridx = 2;
		gbc16.gridy = 6;
		final JButton bNextTime = initNextTimeButton();
		helper.add( bNextTime, gbc16 );

		final GridBagConstraints gbc17 = new GridBagConstraints();
		gbc17.fill = GridBagConstraints.HORIZONTAL;
		gbc17.gridwidth = 2;
		gbc17.anchor = GridBagConstraints.NORTHWEST;
		gbc17.insets = new Insets( 5, 5, 5, 5 );
		gbc17.gridx = 0;
		gbc17.gridy = 7;
		final JButton bSaveTracklets = initSaveTrackletsButton();
		helper.add( bSaveTracklets, gbc17 );

		return helper;
	}


	private JButton initLoadTrackelts() {
		final JButton bLoadTracklets = new JButton( "Load Tracklets from CSV" );
		bLoadTracklets.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.setPuncta( CSVReader.loadCSV( "test2.csv" ) );
				model.setMaxId();
				punctaClicker.updateOverlay();
			}
		} );
		return bLoadTracklets;

	}

	private JButton initSaveTrackletsButton() {
		final JButton bSaveTracklets = new JButton( "Save tracklets" );
		bSaveTracklets.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final List< Punctas > allPuncta = model.getPuncta();
				writeToCSV( allPuncta );
			}
		} );
		return bSaveTracklets;

	}

	private JButton initNextTimeButton() {
		final JButton bNextTime = new JButton( "Next time ->" );
		bNextTime.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				bdv.getBdvHandle().getViewerPanel().nextTimePoint();
			}
		} );
		return bNextTime;

	}

	private JButton initPreviousTimeButton() {
		final JButton bPreviousTime = new JButton( "<- Previous time" );
		bPreviousTime.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				bdv.getBdvHandle().getViewerPanel().previousTimePoint();
			}
		} );
		return bPreviousTime;

	}

	protected void writeToCSV( final List< Punctas > allPuncta ) {
		getWriter();
		CSVWriter.writeCsvFile( "test2.csv", allPuncta );

	}

	private JButton initPunctaPickingButton() {
		final JButton bStartPickingPuncta = new JButton( "Start a new tracklet" );
		bStartPickingPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.increaseId();
				defineBehavior();

			}



		} );
		return bStartPickingPuncta;
	}


	private void defineBehavior() {

		punctaClicker.mainClick();
	}




	private JButton initMoveButton() {
		final JButton bMoveTime = new JButton( "Move" );
		bMoveTime.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				bdv.getViewerPanel().setTimepoint( Integer.parseInt( tMoveTime.getText() ) );

			}
		} );
		return bMoveTime;
	}

	private < T extends RealType< T > & NativeType< T > > BdvHandlePanel initBdv( final RandomAccessibleInterval< T > img ) {
		final BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );
		final BdvSource source = BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );
		punctaClicker = new PunctaClicker( bdv, model );

		final T min = Util.getTypeFromInterval( img ).createVariable();
		final T max = Util.getTypeFromInterval( img ).createVariable();
		ImglibUtil.computeMinMax( Views.iterable( img ), min, max );

		source.setDisplayRangeBounds( 0, max.getRealFloat() );
		source.setDisplayRange( min.getRealFloat(), max.getRealFloat() );
		return bdv;
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

	public CSVWriter getWriter() {
		return writer;
	}

	public void setWriter( final CSVWriter writer ) {
		this.writer = writer;
	}
}


