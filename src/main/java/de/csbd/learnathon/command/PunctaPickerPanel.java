package de.csbd.learnathon.command;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import ij.ImagePlus;
import ij.WindowManager;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform2D;
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

	private PunctaClicker pickBehaviour;

	private CSVWriter writer;

	public PunctaClicker getPickBehaviour() {
		return pickBehaviour;
	}

	public void setPickBehaviour( final PunctaClicker pickBehaviour ) {
		this.pickBehaviour = pickBehaviour;
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
		final JButton bAddPunctaFromCsv = new JButton( "Add puncta coordinates from CSV" );
		helper.add( bAddPunctaFromCsv, gbc1 );

	    final GridBagConstraints gbc2 = new GridBagConstraints();
	    gbc2.fill = GridBagConstraints.HORIZONTAL;
	    gbc2.gridwidth = 2;
	    gbc2.anchor = GridBagConstraints.NORTHWEST;
	    gbc2.insets = new Insets(5,5,5,5);
		gbc2.gridx = 2;
		gbc2.gridy = 0;
		final JButton bAddTrackFromCsv = new JButton( "Add track coordinates from CSV" );

		helper.add( bAddTrackFromCsv, gbc2 );

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

		final GridBagConstraints gbc7 = new GridBagConstraints();
		gbc7.fill = GridBagConstraints.HORIZONTAL;
		gbc7.gridwidth = 2;
		gbc7.anchor = GridBagConstraints.NORTHWEST;
		gbc7.insets = new Insets( 5, 5, 5, 5 );
		gbc7.gridx = 2;
		gbc7.gridy = 2;
		final JButton bSavePickedPuncta = initSavePickedPuncta();

		helper.add( bSavePickedPuncta, gbc7 );

		final GridBagConstraints gbc8 = new GridBagConstraints();
		gbc8.fill = GridBagConstraints.HORIZONTAL;
		gbc8.gridwidth = 2;
		gbc8.anchor = GridBagConstraints.CENTER;
		gbc8.insets = new Insets( 5, 5, 5, 5 );
		gbc8.gridx = 2;
		gbc8.gridy = 3;
		final JButton bStartTracking = new JButton( "Start tracking already selected puncta" );
		helper.add( bStartTracking, gbc8 );

		final GridBagConstraints gbc9 = new GridBagConstraints();
		gbc9.insets = new Insets( 0, 0, 0, 5 );
		gbc9.gridx = 0;
		gbc9.gridy = 4;
		final JLabel lRadius = new JLabel( "Tracking uncertainty radius:" );
		helper.add( lRadius, gbc9 );

		final JTextField tRadius = new JTextField();
		tRadius.setColumns( 4 );
		tRadius.setMinimumSize( tRadius.getPreferredSize() );
		final GridBagConstraints gbc10 = new GridBagConstraints();
		gbc10.anchor = GridBagConstraints.WEST;
		gbc10.insets = new Insets( 0, 0, 0, 5 );
		gbc10.gridx = 1;
		gbc10.gridy = 4;
		helper.add( tRadius, gbc10 );

		final GridBagConstraints gbc11 = new GridBagConstraints();
		gbc11.fill = GridBagConstraints.HORIZONTAL;
		gbc11.gridwidth = 1;
		gbc11.anchor = GridBagConstraints.NORTHWEST;
		gbc11.insets = new Insets( 5, 5, 5, 5 );
		gbc11.gridx = 2;
		gbc11.gridy = 4;
		final JButton bDrawCircle = new JButton( "Draw/Update circle" );
		helper.add( bDrawCircle, gbc11 );

		final GridBagConstraints gbc12 = new GridBagConstraints();
		gbc12.fill = GridBagConstraints.HORIZONTAL;
		gbc12.gridwidth = 1;
		gbc12.anchor = GridBagConstraints.WEST;
		gbc12.insets = new Insets( 0, 0, 0, 5 );
		gbc12.gridx = 3;
		gbc12.gridy = 4;
		final JButton bConfirmCircle = new JButton( "Confirm circle" );
		helper.add( bConfirmCircle, gbc12 );

		final GridBagConstraints gbc13 = new GridBagConstraints();
		gbc13.fill = GridBagConstraints.HORIZONTAL;
		gbc13.gridwidth = 2;
		gbc13.anchor = GridBagConstraints.NORTHWEST;
		gbc13.insets = new Insets( 5, 5, 5, 5 );
		gbc13.gridx = 0;
		gbc13.gridy = 5;
		final JButton bPreviousPuncta = new JButton( "<- Previous puncta" );
		helper.add( bPreviousPuncta, gbc13 );

		final GridBagConstraints gbc14 = new GridBagConstraints();
		gbc14.fill = GridBagConstraints.HORIZONTAL;
		gbc14.gridwidth = 2;
		gbc14.anchor = GridBagConstraints.NORTHWEST;
		gbc14.insets = new Insets( 5, 5, 5, 5 );
		gbc14.gridx = 2;
		gbc14.gridy = 5;
		final JButton bNextPuncta = new JButton( "Next puncta ->" );
		helper.add( bNextPuncta, gbc14 );

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
		final JButton bSave = new JButton( "Save" );
		helper.add( bSave, gbc17 );

		final GridBagConstraints gbc18 = new GridBagConstraints();
		gbc18.fill = GridBagConstraints.HORIZONTAL;
		gbc18.gridwidth = 2;
		gbc18.anchor = GridBagConstraints.NORTHWEST;
		gbc18.insets = new Insets( 5, 5, 5, 5 );
		gbc18.gridx = 2;
		gbc18.gridy = 7;
		final JButton bQuit = new JButton( "Quit" );
		helper.add( bQuit, gbc18 );

		return helper;
	}


	private JButton initQuitButton() {
		final JButton bQuit = new JButton( "Quit" );
		bQuit.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bQuit;

	}

	private JButton initSaveButton() {
		final JButton bSave = new JButton( "Save" );
		bSave.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bSave;

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

	private JButton initNextPunctaButton() {
		final JButton bNextPuncta = new JButton( "Next puncta ->" );
		bNextPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bNextPuncta;

	}

	private JButton initPreviousPunctaButton() {
		final JButton bPreviousPuncta = new JButton( " <- Previous puncta" );
		bPreviousPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bPreviousPuncta;

	}

	private JButton initConfirmCircleButton() {
		final JButton bConfirmCircle = new JButton( "Confirm circle" );
		bConfirmCircle.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bConfirmCircle;

	}

	private JButton initDrawCircleButton() {
		final JButton bDrawCircle = new JButton( "Draw circle of uncertainity" );
		bDrawCircle.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bDrawCircle;
	}

	private JButton initStartTrackingButton() {
		final JButton bStartTracking = new JButton( "Start tracking picked puncta" );
		bStartTracking.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final ImagePlus currentImage = WindowManager.getCurrentImage();
				System.out.println( currentImage.getCurrentSlice() );
			}
		} );
		return bStartTracking;

	}

	private JButton initSavePickedPuncta() {
		final JButton bSavePickedPuncta = new JButton( "Save picked puncta" );
		bSavePickedPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final List< Punctas > allPuncta = model.getPuncta();
				writeToCSV( allPuncta );
			}
		} );
		return bSavePickedPuncta;

	}


	protected void writeToCSV( final List< Punctas > allPuncta ) {
		getWriter();
		CSVWriter.writeCsvFile( "test.csv", allPuncta );

	}

	private JButton initPunctaPickingButton() {
		final JButton bStartPickingPuncta = new JButton( "Start picking puncta to track" );
		bStartPickingPuncta.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				defineBehavior();

			}



		} );
		return bStartPickingPuncta;
	}


	private void defineBehavior() {
		if ( getPickBehaviour() == null ) {
			pickBehaviour = new PunctaClicker( bdv, model );
			pickBehaviour.mainClick();

		}

	}


	private void overlayPoints( final ArrayList< RealPoint > points ) {
		final BdvOverlay overlay = new BdvOverlay() {

			@Override
			protected void draw( final Graphics2D g ) {

				final AffineTransform2D t = new AffineTransform2D();
				getCurrentTransform2D( t );

				g.setColor( Color.RED );

				final double[] lPos = new double[ 2 ];
				final double[] gPos1 = new double[ 2 ];
				final double[] gPos2 = new double[ 2 ];
				for ( int i = 0; i < points.size(); i += 1 ) {
					points.get( i ).localize( lPos );
					t.apply( lPos, gPos1 );
					points.get( i + 1 ).localize( lPos );
					t.apply( lPos, gPos2 );
					g.drawOval( ( int ) gPos1[ 0 ], ( int ) gPos1[ 1 ], 10, 10 );

				}

			}
		};

		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );
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
