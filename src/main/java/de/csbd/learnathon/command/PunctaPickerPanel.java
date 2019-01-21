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

		final GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.insets = new Insets( 0, 0, 0, 5 );
		gbc2.gridx = 0;
		gbc2.gridy = 1;
		final JLabel label = new JLabel( "Move to time:" );
		helper.add( label, gbc2 );

		tMoveTime = new JTextField();
		tMoveTime.setColumns( 4 );
		tMoveTime.setMinimumSize( tMoveTime.getPreferredSize() );
		final GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.anchor = GridBagConstraints.WEST;
		gbc3.insets = new Insets( 0, 0, 0, 5 );
		gbc3.gridx = 1;
		gbc3.gridy = 1;
		helper.add( tMoveTime, gbc2 );

		final JButton bMoveTime = initMoveButton();
		final GridBagConstraints gbc4 = new GridBagConstraints();
		gbc4.insets = new Insets( 0, 0, 0, 5 );
		gbc4.anchor = GridBagConstraints.NORTHWEST;
		gbc4.gridx = 2;
		gbc4.gridy = 1;
		helper.add( bMoveTime, gbc4 );

		final GridBagConstraints gbc5 = new GridBagConstraints();
		gbc5.fill = GridBagConstraints.HORIZONTAL;
		gbc5.gridwidth = 2;
		gbc5.anchor = GridBagConstraints.NORTHWEST;
		gbc5.insets = new Insets( 5, 5, 5, 5 );
		gbc5.gridx = 0;
		gbc5.gridy = 2;
		final JButton bStartPickingPuncta = initPunctaPickingButton();
		helper.add( bStartPickingPuncta, gbc5 );

		final GridBagConstraints gbc6 = new GridBagConstraints();
		gbc6.fill = GridBagConstraints.HORIZONTAL;
		gbc6.gridwidth = 2;
		gbc6.anchor = GridBagConstraints.NORTHWEST;
		gbc6.insets = new Insets( 5, 5, 5, 5 );
		gbc6.gridx = 0;
		gbc6.gridy = 6;
		final JButton bPreviousTime = initPreviousTimeButton();
		helper.add( bPreviousTime, gbc6 );

		final GridBagConstraints gbc7 = new GridBagConstraints();
		gbc7.fill = GridBagConstraints.HORIZONTAL;
		gbc7.gridwidth = 2;
		gbc7.anchor = GridBagConstraints.NORTHWEST;
		gbc7.insets = new Insets( 5, 5, 5, 5 );
		gbc7.gridx = 2;
		gbc7.gridy = 6;
		final JButton bNextTime = initNextTimeButton();
		helper.add( bNextTime, gbc7 );

		final GridBagConstraints gbc9 = new GridBagConstraints();
		gbc9.fill = GridBagConstraints.HORIZONTAL;
		gbc9.gridwidth = 2;
		gbc9.anchor = GridBagConstraints.NORTHWEST;
		gbc9.insets = new Insets( 5, 5, 5, 5 );
		gbc9.gridx = 0;
		gbc9.gridy = 8;
		final JButton bModify = initModify();
		helper.add( bModify, gbc9 );

		final GridBagConstraints gbc10 = new GridBagConstraints();
		gbc10.fill = GridBagConstraints.HORIZONTAL;
		gbc10.gridwidth = 2;
		gbc10.anchor = GridBagConstraints.NORTHWEST;
		gbc10.insets = new Insets( 5, 5, 5, 5 );
		gbc10.gridx = 0;
		gbc10.gridy = 9;
		final JButton bDeleteTracklet = initDelete();
		helper.add( bDeleteTracklet, gbc10 );

		final GridBagConstraints gbc8 = new GridBagConstraints();
		gbc8.fill = GridBagConstraints.HORIZONTAL;
		gbc8.gridwidth = 2;
		gbc8.anchor = GridBagConstraints.NORTHWEST;
		gbc8.insets = new Insets( 5, 5, 5, 5 );
		gbc8.gridx = 0;
		gbc8.gridy = 7;
		final JButton bSaveTracklets = initSaveTrackletsButton();
		helper.add( bSaveTracklets, gbc8 );

		final GridBagConstraints gbc11 = new GridBagConstraints();
		gbc11.fill = GridBagConstraints.HORIZONTAL;
		gbc11.gridwidth = 2;
		gbc11.anchor = GridBagConstraints.NORTHWEST;
		gbc11.insets = new Insets( 5, 5, 5, 5 );
		gbc11.gridx = 0;
		gbc11.gridy = 10;
		final JButton bSelectTracklet = initSelectTrackletsButton();
		helper.add( bSelectTracklet, gbc11 );

		return helper;
	}


	private JButton initSelectTrackletsButton() {
		final JButton bSelectTracklet = new JButton( "Select a tracklet" );
		bSelectTracklet.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.setActionIndicator( PunctaPickerModel.ACTION_SELECT );
			}

		} );
		return bSelectTracklet;
	}

	private JButton initModify() {
		final JButton bModifyTracklet = new JButton( "Modify a tracklet" );
		bModifyTracklet.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.setActionIndicator( PunctaPickerModel.ACTION_MODIFY );
			}

		} );
		return bModifyTracklet;

	}

	private JButton initDelete() {
		final JButton bDeleteTracklet = new JButton( "Delete a tracklet" );
		bDeleteTracklet.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.deleteSelectedTracklet();
				punctaClicker.getOverlay().refreshBdv();

			}

		} );
		return bDeleteTracklet;

	}

	private JButton initLoadTrackelts() {
		final JButton bLoadTracklets = new JButton( "Load Tracklets from CSV" );
		bLoadTracklets.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.setPuncta( CSVReader.loadCSV( "test2.csv" ) );
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
				final List< Puncta > allPuncta = model.getPuncta();
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

	protected void writeToCSV( final List< Puncta > allPuncta ) {
		getWriter();
		CSVWriter.writeCsvFile( "test2.csv", allPuncta );

	}

	private JButton initPunctaPickingButton() {
		final JButton bStartPickingPuncta = new JButton( "Start a new tracklet" );
		bStartPickingPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.setActionIndicator( PunctaPickerModel.ACTION_TRACK );

			}



		} );
		return bStartPickingPuncta;
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
		
		punctaClicker.defineBehaviour();


		

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


