package de.csbd.learnathon.command;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

public class SimpleMenu implements ActionListener, ItemListener {

	private PunctaPickerModel model;

	public static String FLOW_NONE = "none";
	public static String FLOW_TPS = "TPS";
	public static String FLOW_NN = "NN";
	public static String FLOW_kNN = "kNN";
	private static String flowMethod = FLOW_NONE;

	public SimpleMenu( PunctaPickerModel m ) {
		this.model = m;
	}

	public static String getFlowMethod() {
		return flowMethod;
	}

	public JMenuBar createMenuBar() {


		//create a menubar
		JMenuBar menuBar;
		JMenu filemenu, flowmenu, helpmenu;
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

		//Build the file menu.
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

		//Build the file menu.
		helpmenu = new JMenu( "Help" );
		flowmenu.getAccessibleContext().setAccessibleDescription( "This is the menu for help about different key bindings" );
		menuBar.add( helpmenu );

		helpsubmenu = new JMenu( "Key bindings" );
		helpsubmenu.getAccessibleContext().setAccessibleDescription( "" );
		helpmenu.add( helpsubmenu );

		menuItem = new JMenuItem( "Add puncta -> A" );
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
	public void itemStateChanged( ItemEvent e ) {}

	@Override
	public void actionPerformed( ActionEvent e ) {
		JMenuItem jmi = ( JMenuItem ) e.getSource();
		if ( jmi.getText() == "Load tracklets from CSV" ) {
			JFileChooser jfc = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
			jfc.setDialogTitle( "Load tracklets csv file: " );
			jfc.setAcceptAllFileFilterUsed( false );
			FileNameExtensionFilter filter = new FileNameExtensionFilter( "*.csv", "csv" );
			jfc.setFileFilter( filter );
			jfc.addChoosableFileFilter( filter );
			int returnValue = jfc.showOpenDialog( null );
			if ( returnValue == JFileChooser.APPROVE_OPTION ) {
				File selectedFile = jfc.getSelectedFile();
				if ( model.getGraph().isEmpty() ) {
					model.setGraph( CSVReader.loadCSV( selectedFile.getAbsolutePath() ) );
				} else {
					Graph addGraph = CSVReader.loadCSV( selectedFile.getAbsolutePath() );
					for ( Edge ed : addGraph.getEdges() ) {
						model.getGraph().addEdge( ed );
					}
					for ( Puncta pu : addGraph.getPunctas() ) {
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
			JFileChooser jfc = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
			jfc.setDialogTitle( "Load old tracks from csv file: " );
			jfc.setMultiSelectionEnabled( true );
			jfc.setAcceptAllFileFilterUsed( false );
			FileNameExtensionFilter filter = new FileNameExtensionFilter( "*.csv", "csv" );
			jfc.setFileFilter( filter );
			jfc.addChoosableFileFilter( filter );
			int returnValue = jfc.showOpenDialog( null );
			if ( returnValue == JFileChooser.APPROVE_OPTION ) {
				File[] selectedFiles = jfc.getSelectedFiles();
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

	private void writeToCSV( List< Puncta > allPuncta ) {
		JFileChooser chooser = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
		chooser.setDialogTitle( "Choose a directory to save your file: " );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		chooser.showSaveDialog( null );

		String path = chooser.getSelectedFile().getAbsolutePath();
		String filename = chooser.getSelectedFile().getName();
		CSVWriter.writeCsvFile( path + filename + ".csv", allPuncta, model.getGraph().getEdges() );
		System.out.println( path + filename + ".csv" );
	}
}

