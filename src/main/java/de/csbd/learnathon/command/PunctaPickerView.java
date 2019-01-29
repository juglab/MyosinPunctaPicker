package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

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
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;

public class PunctaPickerView {

	@Parameter
	private Context context;

	@Parameter
	private CommandService commandService;

	public Logger log;

	public BdvHandlePanel bdv;

	private JTextField tMoveTime;

	private PunctaPickerModel model;

	private PunctaPickerController controller;

	private CSVWriter writer;

	private CSVReader reader;

	private Overlay overlay;

	public CSVReader getReader() {
		return reader;
	}


	public PunctaPickerView( PunctaPickerModel m, final Context context ) {
//		context.inject( this );  // WTF am I good for?
		this.model = m;
		this.controller = new PunctaPickerController( m, this );
		model.setController( controller );
		model.setView( this );
		bdv = initBdv( model.getRawData() );
		this.overlay = new Overlay( bdv, m );
		controller.defineBehaviour();


	}

	private < T extends RealType< T > & NativeType< T > > BdvHandlePanel initBdv( final RandomAccessibleInterval< T > img ) {
		final BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );
		final BdvSource source = BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );


		final T min = Util.getTypeFromInterval( img ).createVariable();
		final T max = Util.getTypeFromInterval( img ).createVariable();
		ImglibUtil.computeMinMax( Views.iterable( img ), min, max );

		source.setDisplayRangeBounds( 0, max.getRealFloat() );
		source.setDisplayRange( min.getRealFloat(), max.getRealFloat() );
		return bdv;
	}


	public JPanel getPanel() {
		final JPanel controls = initControlsPanel();
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

//		final GridBagConstraints gbc2 = new GridBagConstraints();
//		gbc2.insets = new Insets( 0, 0, 0, 5 );
//		gbc2.gridx = 0;
//		gbc2.gridy = 1;
//		final JLabel label = new JLabel( "Move to time:" );
//		helper.add( label, gbc2 );
//
//		tMoveTime = new JTextField();
//		tMoveTime.setColumns( 4 );
//		tMoveTime.setMinimumSize( tMoveTime.getPreferredSize() );
//		final GridBagConstraints gbc3 = new GridBagConstraints();
//		gbc3.anchor = GridBagConstraints.WEST;
//		gbc3.insets = new Insets( 0, 0, 0, 5 );
//		gbc3.gridx = 1;
//		gbc3.gridy = 1;
//		helper.add( tMoveTime, gbc2 );
//
//		final JButton bMoveTime = initMoveButton();
//		final GridBagConstraints gbc4 = new GridBagConstraints();
//		gbc4.insets = new Insets( 0, 0, 0, 5 );
//		gbc4.anchor = GridBagConstraints.NORTHWEST;
//		gbc4.gridx = 2;
//		gbc4.gridy = 1;
//		helper.add( bMoveTime, gbc4 );

		final GridBagConstraints gbc5 = new GridBagConstraints();
		gbc5.fill = GridBagConstraints.HORIZONTAL;
		gbc5.gridwidth = 2;
		gbc5.anchor = GridBagConstraints.NORTHWEST;
		gbc5.insets = new Insets( 5, 5, 5, 5 );
		gbc5.gridx = 0;
		gbc5.gridy = 2;
		final JButton bStartPickingPuncta = initPunctaPickingButton();
		helper.add( bStartPickingPuncta, gbc5 );

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

		final GridBagConstraints gbc12 = new GridBagConstraints();
		gbc12.fill = GridBagConstraints.HORIZONTAL;
		gbc12.gridwidth = 2;
		gbc12.anchor = GridBagConstraints.NORTHWEST;
		gbc12.insets = new Insets( 5, 5, 5, 5 );
		gbc12.gridx = 0;
		gbc12.gridy = 11;
		final JButton bDeleteSelectedPuncta = initDeleteSelectedPunctaButton();
		helper.add( bDeleteSelectedPuncta, gbc12 );

		final GridBagConstraints gbc13 = new GridBagConstraints();
		gbc13.fill = GridBagConstraints.HORIZONTAL;
		gbc13.gridwidth = 2;
		gbc13.anchor = GridBagConstraints.NORTHWEST;
		gbc13.insets = new Insets( 5, 5, 5, 5 );
		gbc13.gridx = 0;
		gbc13.gridy = 12;
		final JButton bShowFlow = initShowFlowButton();
		helper.add( bShowFlow, gbc13 );

		final GridBagConstraints gbc14 = new GridBagConstraints();
		gbc14.fill = GridBagConstraints.HORIZONTAL;
		gbc14.gridwidth = 2;
		gbc14.anchor = GridBagConstraints.NORTHWEST;
		gbc14.insets = new Insets( 5, 5, 5, 5 );
		gbc14.gridx = 0;
		gbc14.gridy = 13;
		final JButton bLoadOld = initLoadOldFormatButton();
		helper.add( bLoadOld, gbc14 );

		return helper;
	}


	private JButton initShowFlowButton() {
		final JButton bShowFlow = new JButton( "Show computed flow" );
		bShowFlow.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				model.processFlow();
			}

		} );
		return bShowFlow;
	}

	private JButton initSelectTrackletsButton() {

		KeyStroke keySelectTracklet = KeyStroke.getKeyStroke( KeyEvent.VK_V, Event.CTRL_MASK );
		Action performSelectTracklet = new AbstractAction( "Select a tracklet" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				model.setActionIndicator( PunctaPickerModel.ACTION_SELECT );
				getOverlay().refreshBdv();
			}
		};
		JButton bSelectTracklet = new JButton( performSelectTracklet );
		bSelectTracklet.getActionMap().put( "performSelectTracklet", performSelectTracklet );
		bSelectTracklet.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keySelectTracklet, "performSelectTracklet" );
		return bSelectTracklet;
	}

	private JButton initDeleteSelectedPunctaButton() {

		KeyStroke keyDeletePuncta = KeyStroke.getKeyStroke( KeyEvent.VK_X, Event.CTRL_MASK );
		Action performDeletePuncta = new AbstractAction( "Delete selected puncta" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				model.deleteSelectedPunctaAndEdges();
				getOverlay().refreshBdv();
			}
		};
		JButton bDeletePuncta = new JButton( performDeletePuncta );
		bDeletePuncta.getActionMap().put( "performDeletePuncta", performDeletePuncta );
		bDeletePuncta.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyDeletePuncta, "performDeletePuncta" );
		return bDeletePuncta;
	}

	private JButton initDelete() {

		KeyStroke keyDeleteTracklet = KeyStroke.getKeyStroke( KeyEvent.VK_D, Event.CTRL_MASK );
		Action performDeleteTracklet = new AbstractAction( "Delete selected tracklet" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				model.deleteSelectedTracklet();
				getOverlay().refreshBdv();
			}
		};
		JButton bDeleteTracklet = new JButton( performDeleteTracklet );
		bDeleteTracklet.getActionMap().put( "performDeleteTracklet", performDeleteTracklet );
		bDeleteTracklet.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyDeleteTracklet, "performDeleteTracklet" );
		return bDeleteTracklet;

	}

	private JButton initLoadTrackelts() {

		KeyStroke keyLoadTracklets = KeyStroke.getKeyStroke( KeyEvent.VK_L, Event.CTRL_MASK );
		Action performLoadTracklets = new AbstractAction( "Load Tracklets from CSV" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				JFileChooser jfc = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
				jfc.setDialogTitle( "Load tracklets csv file: " );
				jfc.setAcceptAllFileFilterUsed( false );
				FileNameExtensionFilter filter = new FileNameExtensionFilter( "*.csv", "csv" );
				jfc.setFileFilter( filter );
				jfc.addChoosableFileFilter( filter );
				int returnValue = jfc.showOpenDialog( null );
				if ( returnValue == JFileChooser.APPROVE_OPTION ) {
					File selectedFile = jfc.getSelectedFile();

					model.setGraph( CSVReader.loadCSV( selectedFile.getAbsolutePath() ) );
					getOverlay().refreshBdv();
				}
			}
		};
		JButton bLoadTracklets = new JButton( performLoadTracklets );
		bLoadTracklets.getActionMap().put( "performLoadTracklets", performLoadTracklets );
		bLoadTracklets.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyLoadTracklets, "performLoadTracklets" );
		return bLoadTracklets;
	}

	private JButton initSaveTrackletsButton() {

		KeyStroke keySaveTracklets = KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.CTRL_MASK );
		Action performSaveTracklets = new AbstractAction( "Save tracklets to CSV" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				final List< Puncta > allPuncta = model.getPuncta();
				writeToCSV( allPuncta );
			}
		};
		JButton bSaveTracklets = new JButton( performSaveTracklets );
		bSaveTracklets.getActionMap().put( "performSaveTracklets", performSaveTracklets );
		bSaveTracklets.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keySaveTracklets, "performSaveTracklets" );
		return bSaveTracklets;
	}

	protected void writeToCSV( final List< Puncta > allPuncta ) {

		JFileChooser chooser = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
		chooser.setDialogTitle( "Choose a directory to save your file: " );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		chooser.showSaveDialog( null );

		String path = chooser.getSelectedFile().getAbsolutePath();
		String filename = chooser.getSelectedFile().getName();
		CSVWriter.writeCsvFile( path + filename + ".csv", allPuncta, model.getEdges() );
		System.out.println( path + filename + ".csv" );
	}

	private JButton initPunctaPickingButton() {
		KeyStroke keyStart = KeyStroke.getKeyStroke( KeyEvent.VK_A, Event.CTRL_MASK );
		Action performStart = new AbstractAction( "Start a new tracklet" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				model.setActionIndicator( PunctaPickerModel.ACTION_TRACK );
			}
		};
		JButton bStartPickingPuncta = new JButton( performStart );
		bStartPickingPuncta.getActionMap().put( "performStart", performStart );
		bStartPickingPuncta.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyStart, "performStart" );
		return bStartPickingPuncta;
	}

	private JButton initMoveButton() {
		KeyStroke keyMove = KeyStroke.getKeyStroke( KeyEvent.VK_Q, Event.CTRL_MASK );
		Action performMove = new AbstractAction( "Move" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				bdv.getViewerPanel().setTimepoint( Integer.parseInt( tMoveTime.getText() ) );
			}
		};
		JButton bMoveTime = new JButton( performMove );
		bMoveTime.getActionMap().put( "performMove", performMove );
		bMoveTime.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyMove, "performMove" );
		return bMoveTime;
	}

	private JButton initLoadOldFormatButton() {
		KeyStroke keyLoadOld = KeyStroke.getKeyStroke( KeyEvent.VK_O, Event.CTRL_MASK );
		Action performLoadOld = new AbstractAction( "Load Old tracks from CSV" ) {

			@Override
			public void actionPerformed( ActionEvent e ) {
				JFileChooser jfc = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
				jfc.setDialogTitle( "Load old tracks from csv file: " );
				jfc.setMultiSelectionEnabled( true );
				jfc.setAcceptAllFileFilterUsed( false );
				FileNameExtensionFilter filter = new FileNameExtensionFilter( "*.csv", "csv" );
				jfc.setFileFilter( filter );
				jfc.addChoosableFileFilter( filter );
				int returnValue = jfc.showOpenDialog( null );
//				File[] files = jfc.getSelectedFiles();
//				if ( returnValue == JFileChooser.APPROVE_OPTION ) {
				File[] selectedFiles = jfc.getSelectedFiles();
				System.out.println( selectedFiles[ 0 ].getAbsolutePath() );
				System.out.println( selectedFiles[ 1 ].getAbsolutePath() );
				model.setGraph( CSVReader.loadOldCSVs( selectedFiles[ 0 ].getAbsolutePath(), selectedFiles[ 1 ].getAbsolutePath() ) );
				getOverlay().refreshBdv();
//				}
			}
		};
		JButton bLoadOldTracks = new JButton( performLoadOld );
		bLoadOldTracks.getActionMap().put( "performLoadOldTracks", performLoadOld );
		bLoadOldTracks.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyLoadOld, "performLoadOldTracks" );
		return bLoadOldTracks;
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

	public Overlay getOverlay() {
		return overlay;
	}
}


