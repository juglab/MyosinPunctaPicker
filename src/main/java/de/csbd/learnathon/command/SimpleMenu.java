package de.csbd.learnathon.command;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

public class SimpleMenu implements ActionListener, ItemListener {

	private final PunctaPickerModel model;

	private static ButtonGroup buttonGroup;

	public static String FLOW_NONE = "none";
	public static String FLOW_TPS = "TPS";
	public static String FLOW_NN = "NN";
	public static String FLOW_kNN = "kNN";
	private static String flowMethod = FLOW_NONE;

	public SimpleMenu( final PunctaPickerModel m ) {
		this.model = m;
	}

	public static String getFlowMethod() {
		return flowMethod;
	}

	public JMenuBar createMenuBar() {


		//create a menubar
		JMenuBar menuBar;
		JMenu filemenu, flowmenu, blobmenu, helpmenu;
		JMenu helpsubmenu;
		JMenuItem menuItem;

		//Create the menu bar.
		menuBar = new JMenuBar();

		//Build the file menu.
		filemenu = new JMenu( "File" );
		filemenu.getAccessibleContext().setAccessibleDescription( "This is the menu to load/save tracklets" );
		filemenu.setLayout( new FlowLayout() );
		menuBar.add( filemenu );

		menuItem = new JMenuItem( "Load tracklets from CSV" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		menuItem.addActionListener( this );
		filemenu.add( menuItem );
		menuItem = new JMenuItem( "Save tracklets to CSV" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		menuItem.addActionListener( this );
		filemenu.add( menuItem );
		menuItem = new JMenuItem( "Load tracklets from old format" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		menuItem.addActionListener( this );
		filemenu.add( menuItem );

		JSlider slider = new JSlider( JSlider.HORIZONTAL );
		slider.setVisible( true );
		ChangeListener cl = e -> {
			JSlider x = ( JSlider ) e.getSource();
			System.out.println( "value is: " + x.getValue() );
		};
		slider.addChangeListener( cl );
		filemenu.add( slider );

		//Build the flow menu.
		flowmenu = new JMenu( "Compute flows" );
		flowmenu.getAccessibleContext().setAccessibleDescription( "This is the menu to compute different flows" );
		menuBar.add( flowmenu );

		menuItem = new JMenuItem( "Nearest Neighbor flow" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		menuItem.addActionListener( this );
		flowmenu.add( menuItem );
		menuItem = new JMenuItem( "k-Nearest Neighbor flow" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		menuItem.addActionListener( this );
		flowmenu.add( menuItem );
		menuItem = new JMenuItem( "Thin Plate Splines Flow" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		menuItem.addActionListener( this );
		flowmenu.add( menuItem );

		//Build the blob detection on/off menu.
		blobmenu = new JMenu( "Automatic Blob Detection" );
		JRadioButtonMenuItem autoSize = new JRadioButtonMenuItem( "Automatically select blob size" );
		JRadioButtonMenuItem autoSizeAndPosition = new JRadioButtonMenuItem( "Automatically select blob size and position" );
		JRadioButtonMenuItem manual = new JRadioButtonMenuItem( "Manually add blob" );
		buttonGroup = new ButtonGroup();
		buttonGroup.add( autoSize );
		buttonGroup.add( autoSizeAndPosition );
		buttonGroup.add( manual );
		manual.setSelected( true );
		blobmenu.add( autoSize );
		blobmenu.add( autoSizeAndPosition );
		blobmenu.add( manual );
		menuBar.add( blobmenu );

		//Build the help menu.
		helpmenu = new JMenu( "Help" );
		flowmenu.getAccessibleContext().setAccessibleDescription( "This is the menu for help about different key bindings" );
		menuBar.add( helpmenu );

		helpsubmenu = new JMenu( "Key bindings" );
		helpsubmenu.getAccessibleContext().setAccessibleDescription( "" );
		helpmenu.add( helpsubmenu );

		menuItem = new JMenuItem( "Add puncta -> A" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Preview puncta -> P" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Select puncta/tracklet -> C" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Increase radius of selected puncta -> E" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Decrease radius of selected puncta -> Q" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Move puncta -> SPACE" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Link punctas -> L" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Delte selected puncta/edge -> D" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		menuItem = new JMenuItem( "Delete selected tracklet -> X" );
		menuItem.getAccessibleContext().setAccessibleDescription( "" );
		helpsubmenu.add( menuItem );
		return menuBar;
	
	}

	@Override
	public void itemStateChanged( final ItemEvent e ) {}

	@Override
	public void actionPerformed( final ActionEvent e ) {
		final JMenuItem jmi = ( JMenuItem ) e.getSource();
		if ( jmi.getText() == "Load tracklets from CSV" ) {
			final JFileChooser jfc = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
			jfc.setDialogTitle( "Load tracklets csv file: " );
			jfc.setAcceptAllFileFilterUsed( false );
			final FileNameExtensionFilter filter = new FileNameExtensionFilter( "*.csv", "csv" );
			jfc.setFileFilter( filter );
			jfc.addChoosableFileFilter( filter );
			final int returnValue = jfc.showOpenDialog( null );
			if ( returnValue == JFileChooser.APPROVE_OPTION ) {
				final File selectedFile = jfc.getSelectedFile();
				if ( model.getGraph().isEmpty() ) {
					model.setGraph( CSVReader.loadCSV( selectedFile.getAbsolutePath() ) );
				} else {
					final Graph addGraph = CSVReader.loadCSV( selectedFile.getAbsolutePath() );
					for ( final Edge ed : addGraph.getEdges() ) {
						model.getGraph().addEdge( ed );
					}
					for ( final Puncta pu : addGraph.getPunctas() ) {
						model.getGraph().addPuncta( pu );
					}
				}
			}
		}
		if ( jmi.getText() == "Save tracklets to CSV" ) {
			final List< Puncta > allPuncta = model.getGraph().getPunctas();
			writeToCSV( allPuncta );
		}
		if ( jmi.getText() == "Load tracklets from old format" ) {
			final JFileChooser jfc = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
			jfc.setDialogTitle( "Load old tracks from csv file: " );
			jfc.setMultiSelectionEnabled( true );
			jfc.setAcceptAllFileFilterUsed( false );
			final FileNameExtensionFilter filter = new FileNameExtensionFilter( "*.csv", "csv" );
			jfc.setFileFilter( filter );
			jfc.addChoosableFileFilter( filter );
			int returnValue = jfc.showOpenDialog( null );
			if ( returnValue == JFileChooser.APPROVE_OPTION ) {
				final File[] selectedFiles = jfc.getSelectedFiles();
				System.out.println( selectedFiles[ 0 ].getAbsolutePath() );
				System.out.println( selectedFiles[ 1 ].getAbsolutePath() );
				model.setGraph( CSVReader.loadOldCSVs( selectedFiles[ 0 ].getAbsolutePath(), selectedFiles[ 1 ].getAbsolutePath() ) ); //How do Fiji coordinates transfer to bdv coordinates?
				int size = model.getGraph().getPunctas().size();
				model.getGraph().setLeadSelectedPuncta( model.getGraph().getPunctas().get( size - 1 ) );
				model.getView().getBdv().getViewerPanel().requestRepaint();
			}

		}
		if ( jmi.getText() == "Nearest Neighbor flow" ) {
			flowMethod = FLOW_NN;
			model.processFlow( flowMethod );
		}
		if ( jmi.getText() == "k-Nearest Neighbor flow" ) {
			flowMethod = FLOW_kNN;
			model.processFlow( flowMethod );
		}
		if ( jmi.getText() == "Thin Plate Splines Flow" ) {
			flowMethod = FLOW_TPS;
			model.processFlow( flowMethod );
		}
		if ( jmi.getText() == "Key bindings" ) {}
	}

	private void writeToCSV( final List< Puncta > allPuncta ) {
		final JFileChooser chooser = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
		chooser.setDialogTitle( "Choose a directory to save your file: " );
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		chooser.showSaveDialog( null );

		File file = chooser.getSelectedFile();
		if ( !file.getName().endsWith( ".csv" ) ) {
			file = new File( file.getAbsolutePath() + ".csv" );
		}
		CSVWriter.writeCsvFile( file, allPuncta, model.getGraph().getEdges() );
	}
	
	public static String getBlobDetectionModuleStatus() {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return button.getText();
            }
        }
		return null;
	}
}

