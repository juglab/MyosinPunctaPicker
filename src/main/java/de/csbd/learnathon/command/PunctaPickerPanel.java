package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import ij.ImagePlus;
import ij.WindowManager;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.miginfocom.swing.MigLayout;

public class PunctaPickerPanel {

	@Parameter
	private Context context;

	@Parameter
	private CommandService commandService;

	public Logger log;

	DefaultListModel< String > model = new DefaultListModel<>();

	private BdvHandlePanel bdv;

	private RandomAccessibleInterval< DoubleType > rawData;

	public PunctaPickerPanel( RandomAccessibleInterval< DoubleType > image, Context context ) {
		this.rawData = image;
		context.inject( this );

	}

	public JPanel getPanel() {
		final JPanel controls = initControlsPanel();
		bdv = initBdv( rawData );
		return wrapToJPanel( initSplitPane( controls, bdv.getViewerPanel() ) );
	}

	private JPanel wrapToJPanel( JSplitPane splitPane ) {
		final JPanel splittedPanel = new JPanel();
		splittedPanel.setLayout( new BorderLayout() );
		splittedPanel.add( splitPane, BorderLayout.CENTER );
		return splittedPanel;
	}

	private JSplitPane initSplitPane( JPanel left, JPanel right ) {
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, left, right );
		splitPane.setOneTouchExpandable( true );
		splitPane.setDividerLocation( 300 );
		return splitPane;
	}


	private JPanel initControlsPanel() {
		final MigLayout layout = new MigLayout( "fill", "[grow]", "" );
		final JPanel controls = new JPanel( layout );
		JPanel helper = initHelperPanel();
		controls.add( helper, "h 100%, grow, wrap" );

		JButton bStartSegmentation = new JButton( "start matching with selected template" );
		controls.add( bStartSegmentation, "growx, gapy 5 0, wrap" );
		return controls;
	}
	
	private JPanel initHelperPanel() {
		JPanel helper = new JPanel(  );
		GridBagLayout gbl_helper = new GridBagLayout();
		gbl_helper.columnWidths = new int[] { 200, 7, 200, 60, 18, 0 };
		gbl_helper.rowHeights = new int[] { 20, 20 };
		gbl_helper.columnWeights = new double[] { 1.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_helper.rowWeights = new double[] { 0.0, 0 };
		helper.setLayout( gbl_helper );

		GridBagConstraints gbc1 = new GridBagConstraints();
	    gbc1.fill = GridBagConstraints.HORIZONTAL;
	    gbc1.gridwidth = 2;
		gbc1.anchor = GridBagConstraints.NORTHWEST;
	    gbc1.insets = new Insets(5,5,5,5);
	    gbc1.gridx = 0;
	    gbc1.gridy = 0;
		JButton bAddPunctaFromCsv = new JButton( "Add puncta coordinates from CSV" );
		helper.add( bAddPunctaFromCsv, gbc1 );

	    GridBagConstraints gbc2 = new GridBagConstraints();
	    gbc2.fill = GridBagConstraints.HORIZONTAL;
	    gbc2.gridwidth = 2;
	    gbc2.anchor = GridBagConstraints.NORTHWEST;
	    gbc2.insets = new Insets(5,5,5,5);
		gbc2.gridx = 2;
		gbc2.gridy = 0;
		JButton bAddTrackFromCsv = new JButton( "Add track coordinates from CSV" );

		helper.add( bAddTrackFromCsv, gbc2 );

	    GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.insets = new Insets( 0, 0, 0, 5 );
	    gbc3.gridx = 0;
		gbc3.gridy = 1;
		JLabel label = new JLabel( "Move to time:" );
		helper.add( label, gbc3 );

		JTextField tMoveTime = new JTextField();
		tMoveTime.setColumns( 4 );
		tMoveTime.setMinimumSize( tMoveTime.getPreferredSize() );
		GridBagConstraints gbc4 = new GridBagConstraints();
		gbc4.anchor = GridBagConstraints.WEST;
		gbc4.insets = new Insets( 0, 0, 0, 5 );
		gbc4.gridx = 1;
		gbc4.gridy = 1;
		helper.add( tMoveTime, gbc4 );

		JButton bMoveTime = new JButton( "Move" );
		GridBagConstraints gbc5 = new GridBagConstraints();
		gbc5.insets = new Insets( 0, 0, 0, 5 );
		gbc5.anchor = GridBagConstraints.NORTHWEST;
		gbc5.gridx = 2;
		gbc5.gridy = 1;
		helper.add( bMoveTime, gbc5 );

		GridBagConstraints gbc6 = new GridBagConstraints();
		gbc6.fill = GridBagConstraints.HORIZONTAL;
		gbc6.gridwidth = 4;
		gbc6.anchor = GridBagConstraints.NORTHWEST;
		gbc6.insets = new Insets( 5, 5, 5, 5 );
		gbc6.gridx = 0;
		gbc6.gridy = 2;
		JButton bStartPickingPuncta = new JButton( "Start picking puncta to track" );
		helper.add( bStartPickingPuncta, gbc6 );

		GridBagConstraints gbc7 = new GridBagConstraints();
		gbc7.fill = GridBagConstraints.HORIZONTAL;
		gbc7.gridwidth = 4;
		gbc7.anchor = GridBagConstraints.NORTHWEST;
		gbc7.insets = new Insets( 5, 5, 5, 5 );
		gbc7.gridx = 0;
		gbc7.gridy = 3;
		JButton bOverlayAndSave = new JButton( "Overlay picked puncta and save" );

		helper.add( bOverlayAndSave, gbc7 );

		GridBagConstraints gbc8 = new GridBagConstraints();
		gbc8.fill = GridBagConstraints.HORIZONTAL;
		gbc8.gridwidth = 4;
		gbc8.anchor = GridBagConstraints.CENTER;
		gbc8.insets = new Insets( 5, 5, 5, 5 );
		gbc8.gridx = 0;
		gbc8.gridy = 4;
		JButton bStartTracking = new JButton( "Start tracking already selected puncta" );
		helper.add( bStartTracking, gbc8 );

		GridBagConstraints gbc9 = new GridBagConstraints();
		gbc9.insets = new Insets( 0, 0, 0, 5 );
		gbc9.gridx = 0;
		gbc9.gridy = 5;
		JLabel lRadius = new JLabel( "Tracking uncertainty radius:" );
		helper.add( lRadius, gbc9 );

		JTextField tRadius = new JTextField();
		tRadius.setColumns( 4 );
		tRadius.setMinimumSize( tRadius.getPreferredSize() );
		GridBagConstraints gbc10 = new GridBagConstraints();
		gbc10.anchor = GridBagConstraints.WEST;
		gbc10.insets = new Insets( 0, 0, 0, 5 );
		gbc10.gridx = 1;
		gbc10.gridy = 5;
		helper.add( tRadius, gbc10 );

		GridBagConstraints gbc11 = new GridBagConstraints();
		gbc11.fill = GridBagConstraints.HORIZONTAL;
		gbc11.gridwidth = 1;
		gbc11.anchor = GridBagConstraints.NORTHWEST;
		gbc11.insets = new Insets( 5, 5, 5, 5 );
		gbc11.gridx = 2;
		gbc11.gridy = 5;
		JButton bDrawCircle = new JButton( "Draw/Update circle" );
		helper.add( bDrawCircle, gbc11 );

		GridBagConstraints gbc12 = new GridBagConstraints();
		gbc12.fill = GridBagConstraints.HORIZONTAL;
		gbc12.gridwidth = 1;
		gbc12.anchor = GridBagConstraints.WEST;
		gbc12.insets = new Insets( 0, 0, 0, 5 );
		gbc12.gridx = 3;
		gbc12.gridy = 5;
		JButton bConfirmCircle = new JButton( "Confirm circle" );
		helper.add( bConfirmCircle, gbc12 );

		GridBagConstraints gbc13 = new GridBagConstraints();
		gbc13.fill = GridBagConstraints.HORIZONTAL;
		gbc13.gridwidth = 2;
		gbc13.anchor = GridBagConstraints.NORTHWEST;
		gbc13.insets = new Insets( 5, 5, 5, 5 );
		gbc13.gridx = 0;
		gbc13.gridy = 6;
		JButton bPreviousPuncta = new JButton( "<- Previous puncta" );
		helper.add( bPreviousPuncta, gbc13 );

		GridBagConstraints gbc14 = new GridBagConstraints();
		gbc14.fill = GridBagConstraints.HORIZONTAL;
		gbc14.gridwidth = 2;
		gbc14.anchor = GridBagConstraints.NORTHWEST;
		gbc14.insets = new Insets( 5, 5, 5, 5 );
		gbc14.gridx = 2;
		gbc14.gridy = 6;
		JButton bNextPuncta = new JButton( "Next puncta ->" );
		helper.add( bNextPuncta, gbc14 );

		GridBagConstraints gbc15 = new GridBagConstraints();
		gbc15.fill = GridBagConstraints.HORIZONTAL;
		gbc15.gridwidth = 2;
		gbc15.anchor = GridBagConstraints.NORTHWEST;
		gbc15.insets = new Insets( 5, 5, 5, 5 );
		gbc15.gridx = 0;
		gbc15.gridy = 7;
		JButton bPreviousTime = new JButton( "<- Previous time" );
		helper.add( bPreviousTime, gbc15 );

		GridBagConstraints gbc16 = new GridBagConstraints();
		gbc16.fill = GridBagConstraints.HORIZONTAL;
		gbc16.gridwidth = 2;
		gbc16.anchor = GridBagConstraints.NORTHWEST;
		gbc16.insets = new Insets( 5, 5, 5, 5 );
		gbc16.gridx = 2;
		gbc16.gridy = 7;
		JButton bNextTime = new JButton( "Next time ->" );
		helper.add( bNextTime, gbc16 );

		GridBagConstraints gbc17 = new GridBagConstraints();
		gbc17.fill = GridBagConstraints.HORIZONTAL;
		gbc17.gridwidth = 2;
		gbc17.anchor = GridBagConstraints.NORTHWEST;
		gbc17.insets = new Insets( 5, 5, 5, 5 );
		gbc17.gridx = 0;
		gbc17.gridy = 8;
		JButton bSave = new JButton( "Save" );
		helper.add( bSave, gbc17 );

		GridBagConstraints gbc18 = new GridBagConstraints();
		gbc18.fill = GridBagConstraints.HORIZONTAL;
		gbc18.gridwidth = 2;
		gbc18.anchor = GridBagConstraints.NORTHWEST;
		gbc18.insets = new Insets( 5, 5, 5, 5 );
		gbc18.gridx = 2;
		gbc18.gridy = 8;
		JButton bQuit = new JButton( "Quit" );
		helper.add( bQuit, gbc18 );

		return helper;
	}


	private JButton initQuitButton() {
		final JButton bQuit = new JButton( "Quit" );
		bQuit.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bQuit;

	}

	private JButton initSaveButton() {
		final JButton bSave = new JButton( "Save" );
		bSave.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bSave;

	}

	private JButton initNextTimeButton() {
		final JButton bNextTime = new JButton( "Next time ->" );
		bNextTime.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bNextTime;

	}

	private JButton initPreviousTimeButton() {
		final JButton bPreviousTime = new JButton( "<- Previous time" );
		bPreviousTime.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bPreviousTime;

	}

	private JButton initNextPunctaButton() {
		final JButton bNextPuncta = new JButton( "Next puncta ->" );
		bNextPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bNextPuncta;

	}

	private JButton initPreviousPunctaButton() {
		final JButton bPreviousPuncta = new JButton( " <- Previous puncta" );
		bPreviousPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bPreviousPuncta;

	}

	private JButton initConfirmCircleButton() {
		final JButton bConfirmCircle = new JButton( "Confirm circle" );
		bConfirmCircle.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bConfirmCircle;

	}

	private JButton initDrawCircleButton() {
		final JButton bDrawCircle = new JButton( "Draw circle of uncertainity" );
		bDrawCircle.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bDrawCircle;
	}

	private JButton initStartTrackingButton() {
		final JButton bStartTracking = new JButton( "Start tracking picked puncta" );
		bStartTracking.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bStartTracking;

	}

	private JButton initOverlayAndSaveButton() {
		final JButton bOverlayAndSave = new JButton( "Overlay selected puncta and save" );
		bOverlayAndSave.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bOverlayAndSave;

	}

	private JButton initPunctaPickingButton() {
		final JButton bStartPickingPuncta = new JButton( "Start picking puncta to track" );
		bStartPickingPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bStartPickingPuncta;
	}

	private JButton initMoveButton() {
		final JButton bMoveTime = new JButton( "Move" );
		bMoveTime.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bMoveTime;
	}

	private < T > BdvHandlePanel initBdv( RandomAccessibleInterval< T > img ) {
		final BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );
		BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );
		return bdv;
	}

	public void close() {
		bdv.close();

	}



}
