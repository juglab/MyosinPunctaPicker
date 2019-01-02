package de.csbd.learnathon.command;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.ImagePlus;
import ij.WindowManager;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;

@Plugin( menuPath = "Plugins>Puncta Picker GUI", type = Command.class )
public class FlowGuiCommand implements Command {

	@Parameter
	Dataset image;

	@Parameter
	Context context;

	@Parameter
	UIService ui;

	JFrame frame;

	private PunctaPickerPanel panel;

	@Override
	public void run() {
		panel = new PunctaPickerPanel( toDoubleType( image.getImgPlus().getImg() ), context);
		frame = new JFrame( "Myosin Puncta Picking Frame" );
		//frame.add( panel.getPanel() );
		frame.setLayout( new GridBagLayout() );
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		final JButton bLoadPickedCoordinates = new JButton( "Load Picked Coordinates from CSV" );
		frame.add( bLoadPickedCoordinates, gbc );
		gbc.gridx = 1;
		gbc.gridy = 0;
		final JButton bSaveCoordinates = new JButton( "Load Saved Track Coordinates from CSV" );
		frame.add( bSaveCoordinates, gbc );
		gbc.gridx = 0;
		gbc.gridy = 1;
		JTextField tMove = new JTextField( "Move to time" );
		tMove.setBounds( 50, 150, 200, 300 );
		frame.add( tMove, gbc );
		gbc.gridx = 1;
		gbc.gridy = 1;
		JButton bMove = new JButton( "Move" );
		frame.add( bMove, gbc );
		gbc.gridx = 2;
		gbc.gridy = 1;
		JButton bStartPick = new JButton( "Start chosing puncta to track" );
		frame.add( bStartPick, gbc );
		gbc.gridx = 3;
		gbc.gridy = 1;
		JButton bOverlayAndSave = new JButton( "Overlay Picked puncta and Save" );
		frame.add( bOverlayAndSave, gbc );
		gbc.gridx = 0;
		gbc.gridy = 2;
		JButton bStartTrack = new JButton( "Start tracking of picked puncta" );
		frame.add( bStartTrack, gbc );
		gbc.gridx = 1;
		gbc.gridy = 2;
		JTextField tRadius = new JTextField( "Uncertainty radius" );
		tRadius.setBounds( 50, 150, 200, 300 );
		frame.add( tRadius, gbc );
		gbc.gridx = 2;
		gbc.gridy = 2;
		JButton bDrawCircle = new JButton( "Draw/Update Circle" );
		frame.add( bDrawCircle, gbc );
		gbc.gridx = 3;
		gbc.gridy = 2;
		JButton bConfirm = new JButton( "Confirm" );
		frame.add( bConfirm, gbc );
		gbc.gridx = 0;
		gbc.gridy = 3;
		JButton bPreviousPuncta = new JButton( "Previous puncta" );
		frame.add( bPreviousPuncta, gbc );
		gbc.gridx = 1;
		gbc.gridy = 3;
		JButton bNextPuncta = new JButton( "Next puncta" );
		frame.add( bNextPuncta, gbc );
		gbc.gridx = 0;
		gbc.gridy = 4;
		JButton bPreviousTime = new JButton( "<- Time" );
		frame.add( bPreviousTime, gbc );
		gbc.gridx = 1;
		gbc.gridy = 4;
		JButton bNextTime = new JButton( "Time ->" );
		frame.add( bNextTime, gbc );
		gbc.gridx = 0;
		gbc.gridy = 5;
		JButton bSave = new JButton( "Save" );
		frame.add( bSave, gbc );
		gbc.gridx = 1;
		gbc.gridy = 5;
		JButton bExit = new JButton( "Exit" );
		frame.add( bExit, gbc );

		bLoadPickedCoordinates.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		frame.addWindowListener( new WindowAdapter() {

			@Override
			public void windowClosed( WindowEvent windowEvent ) {
				panel.close();
			}
		} );

		frame.pack();
		frame.setSize( 500, 500 );
		frame.setVisible( true );
	}

	private JButton newButton( String title ) {
		final JButton button = new JButton( title );
//		button.addActionListener( a -> action.run() );
		return button;
	}

	private RandomAccessibleInterval< DoubleType > toDoubleType( RandomAccessibleInterval< ? extends RealType< ? > > image ) {
		if ( Util.getTypeFromInterval( image ) instanceof DoubleType )
			return ( RandomAccessibleInterval< DoubleType > ) image;
		return Converters.convert( image, ( i, o ) -> o.setReal( i.getRealDouble() ), new DoubleType() );
	}

	public static void main( String... args ) {
		ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		imageJ.command().run( FlowGuiCommand.class, true );
	}
}