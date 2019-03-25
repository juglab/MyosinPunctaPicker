package de.csbd.learnathon.command;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.scijava.table.Column;
import org.scijava.table.GenericTable;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.viewer.state.ViewerState;
import circledetection.command.BlobDetectionCommand;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class PunctaPickerController {

    private RealPoint pos;
    private PunctaPickerModel model;
    private PunctaPickerView view;
    private OpService os;
	final private GhostOverlay ghostOverlay;
	private GhostCircle ghostCircle;

	public PunctaPickerController( PunctaPickerModel model, PunctaPickerView punctaPickerView, OpService os ) {
        this.model = model;
        this.view = punctaPickerView;
		this.ghostOverlay = view.getGhostOverlay();
        this.os = os;
		installBehaviour();
    }

	public void installBehaviour() {
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( view.getBdv().getBdvHandle().getTriggerbindings(), "my-new-behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionClick( x, y );
		}, "Add", "button1" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionSelectClosestSubgraph( x, y );
		}, "Select", "C" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionMoveLeadPuncta( x, y );
		}, "Move", "SPACE" );

		ghostCircle = new GhostCircle();
		behaviours.behaviour( ghostCircle, "Ghost Circle", "A" );

        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "IncreaseRadius", new AbstractAction() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				Puncta pun = model.getGraph().getLeadSelectedPuncta();
                if (!(pun == null)) {
                    pun.setR(pun.getR() * 1.2f);
                    model.getView().getBdv().getViewerPanel().requestRepaint();
                }
			}
        });

        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), "DecreaseRadius", new AbstractAction() {
			
			@Override
			public void actionPerformed( ActionEvent e ) {
				if (!(model.getGraph().getLeadSelectedPuncta() == null)) {
                    model.getGraph().getLeadSelectedPuncta().setR(model.getGraph().getLeadSelectedPuncta().getR() * 0.8f);
                    model.getView().getBdv().getViewerPanel().requestRepaint();
				}
			}
		} );
		registerKeyBinding( KeyStroke.getKeyStroke( KeyEvent.VK_L, 0 ), "Link", new AbstractAction() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( !( model.getGraph().getLeadSelectedPuncta() == null ) && !( model.getGraph().getMouseSelectedPuncta() == null ) ) {
					model.getGraph().addEdge( new Edge( model.getGraph().getLeadSelectedPuncta(), model.getGraph().getMouseSelectedPuncta() ) );
					model.getGraph().selectSubgraphContaining( model.getGraph().getLeadSelectedPuncta() );
					model.getView().getBdv().getViewerPanel().requestRepaint();
				}

			}
		} );
		registerKeyBinding( KeyStroke.getKeyStroke( KeyEvent.VK_X, 0 ), "DeleteTracklet", new AbstractAction() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				int confirmButton = JOptionPane.YES_NO_OPTION;
				int confirmResult =
						JOptionPane.showConfirmDialog( null, "Would you like to delete the selected tracklet?", "Warning", confirmButton );
				if ( confirmResult == JOptionPane.YES_OPTION ) {
					model.getGraph().deleteSelectedElements();
					model.getGraph().setLeadSelectedPuncta( null );
					model.getView().getBdv().getViewerPanel().requestRepaint();
				}

			}
		} );
		registerKeyBinding( KeyStroke.getKeyStroke( KeyEvent.VK_H, 0 ), "HideAllButSelected", new AbstractAction() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( view.getCheckBoxStatus() )
					view.setCheckBoxStatus( false );
				else
					view.setCheckBoxStatus( true );
			}
		} );
		registerKeyBinding( KeyStroke.getKeyStroke( KeyEvent.VK_D, 0 ), "DeletePunctaOrEdge", new AbstractAction() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( !( model.getGraph().getMouseSelectedEdge() == null ) ) {
					model.getGraph().removeEdge( model.getGraph().getMouseSelectedEdge() );
					model.getGraph().setMouseSelectedEdge( null );
					model.getGraph().selectSubgraphContaining( model.getGraph().getLeadSelectedPuncta() ); //Trial Basis
					model.getView().getBdv().getViewerPanel().requestRepaint();
				} else {
					model.getGraph().deleteSelectedPuncta();
					model.getView().getBdv().getViewerPanel().requestRepaint();

				}

			}
		} );
    }


	private class GhostCircle implements ClickBehaviour {

		private boolean isOn = false;

		@Override
		public void click( int x, int y ) {
			if ( !isOn ) {
				isOn = true;
				ghostOverlay.overlayBlobDetectionResult();
				ghostOverlay.setVisible( true );
				view.getBdv().getViewerPanel().setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
				ghostOverlay.requestRepaint();
			} else {
				isOn = false;
				ghostOverlay.setVisible( false );
				view.getBdv().getViewerPanel().setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
				ghostOverlay.requestRepaint();
			}
		}

	}

    public void registerKeyBinding(KeyStroke keyStroke, String name, Action action) {

        InputMap im = view.getBdv().getViewerPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = view.getBdv().getViewerPanel().getActionMap();

        im.put(keyStroke, name);
        am.put(name, action);
    }


    private void actionSelectClosestSubgraph(int x, int y) {
        Graph g = model.getGraph();

        if (!g.getPunctas().isEmpty()) {
            view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates(x, y, pos);

            Pair<Puncta, Double> minEval = PPGraphUtils.getClosestPuncta(pos.getFloatPosition(0), pos.getFloatPosition(1), g.getPunctas());
            if (minEval != null) {
                Puncta minDistPuncta = minEval.getA();
                double minDist = Math.sqrt(minEval.getB());
                if (minDistPuncta.getR() >= minDist) {
					minDistPuncta.setSelected( true );
                    g.setLeadSelectedPuncta(minDistPuncta);
                    g.selectSubgraphContaining(minDistPuncta);
                    view.getBdv().getViewerPanel().setTimepoint(minDistPuncta.getT());
                }
                view.getBdv().getViewerPanel().requestRepaint();
            }
        }
    }

    private <T extends RealType<T> & NativeType<T>> void actionClick(int x, int y) {
    	
        pos = new RealPoint(3);
        view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates(x, y, pos);
        ViewerState state = view.getBdv().getBdvHandle().getViewerPanel().getState();
        int t = state.getCurrentTimepoint();

        Graph g = model.getGraph();
        Puncta pOld = g.getLeadSelectedPuncta();


        if (!g.getPunctaAtTime(t).isEmpty()) {
            Pair<Puncta, Double> min = PPGraphUtils.getClosestPuncta(pos.getFloatPosition(0), pos.getFloatPosition(1), g.getPunctaAtTime(t));

            if (min.getB() < min.getA().getR()) {
                addSelectedEdge(g, pOld, min.getA());
                model.getGraph().setLeadSelectedPuncta(min.getA());
            }
        }

		String blobDetectionStatus = view.getDetectionMode();

		if ( blobDetectionStatus == "manual" ) {
			Puncta pNew = new Puncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t, model.getDefaultRadius() );
			addPunctaToGraph( t, g, pOld, pNew );

		} else if ( blobDetectionStatus == "automatically select size" ) {

			Puncta pNew = blobDetectedPuncta( t, pos.getDoublePosition( 0 ), pos.getDoublePosition( 1 ) );
			pNew.setT( t );
			pNew.setX( ( float ) ( pos.getDoublePosition( 0 ) - view.getWindowSize() / 2 ) + pNew.getX() );
			pNew.setY( ( float ) ( pos.getDoublePosition( 1 ) - view.getWindowSize() / 2 ) + pNew.getY() );
			addPunctaToGraph( t, g, pOld, pNew );
			System.out.println( pNew.getR() );

		}
		else {
			Puncta pNew = blobDetectedPuncta( t, pos.getDoublePosition( 0 ), pos.getDoublePosition( 1 ) );
			pNew.setT( t );
			pNew.setX( ( float ) ( pos.getDoublePosition( 0 ) ) );
			pNew.setY( ( float ) ( pos.getDoublePosition( 1 ) ) );
			addPunctaToGraph( t, g, pOld, pNew );
			System.out.println( pNew.getR() );
		}

    }

	public < T extends RealType< T > & NativeType< T > > Puncta blobDetectedPuncta( int t, double x, double y ) {
		Img< T > fullImage = view.getImage();
		IntervalView< T > image = Views.hyperSlice( fullImage, 2, t );
		Views.extendMirrorSingle( image );
		FinalInterval cropped = Intervals.createMinMax(
				( long ) ( x - view.getWindowSize() / 2 ),
				( long ) ( y - view.getWindowSize() / 2 ),
				0,
				( long ) ( x + view.getWindowSize() / 2 ),
				( long ) ( y + view.getWindowSize() / 2 ),
				0 );
		FinalInterval outputInterval;
		if ( view.getDetectionMode() == "automatically select size" ) {
			outputInterval = Intervals.createMinMax(
					( long ) ( x - 1 / 2 ),
					( long ) ( y - 1 / 2 ),
					0,
					( long ) ( x + 1 / 2 ),
					( long ) ( y + 1 / 2 ),
					0 );
		} else {
			outputInterval = cropped;
		}
		RandomAccessibleInterval< T > croppedImage = Views.interval( image, cropped );
		ImagePlus imgPlus = ImageJFunctions.wrap( croppedImage, "cropped" );
		Img< T > newImage = ImageJFunctions.wrap( imgPlus );
		Puncta pNew = detectFeatures( newImage, outputInterval );
		return pNew;
	}

	private void addPunctaToGraph( int t, Graph g, Puncta pOld, Puncta pNew ) {
		model.getGraph().addPuncta( pNew );
		model.getGraph().setLeadSelectedPuncta( pNew );
		pNew.setSelected( true );
		if ( pOld != null && pOld.getT() == t - 1 ) {
			addSelectedEdge( g, pOld, pNew );
		} else {
			model.getGraph().unselectAll();
			pNew.setSelected( true );
		}
		view.getBdv().getViewerPanel().nextTimePoint();
	}

    private void addSelectedEdge(Graph g, Puncta p1, Puncta p2) {
        Edge newE = new Edge(p1, p2);
        p1.setSelected(true);
        p2.setSelected(true);
        g.addEdge(newE);
        newE.setSelected(true);
    }

    public void actionMoveLeadPuncta(int x, int y) {
        view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates(x, y, pos);
        Puncta lsp = model.getGraph().getLeadSelectedPuncta();
        lsp.setX(pos.getFloatPosition(0));
        lsp.setY(pos.getFloatPosition(1));
    }

	private < T extends RealType< T > & NativeType< T > > Puncta detectFeatures( Img< T > newImage, FinalInterval outputInterval ) {
		double minScale = view.getMinScale();
		double stepScale = view.getStepScale();
		double maxScale = view.getMaxScale();
        boolean brightBlobs = true;
        int axis = 0;
        double samplingFactor = 1;

		BlobDetectionCommand< T > blobDetection =
				new BlobDetectionCommand<>( newImage, minScale, maxScale, stepScale, brightBlobs, axis, samplingFactor, os, outputInterval );

		final GenericTable resultsTable = blobDetection.getResultsTable();

        /*Step Two: Find Otsu Threshold Value on the new List, so obtained*/
        SampleList<FloatType> localMinimaResponse = createIterableList(resultsTable.get("Value"));
        Histogram1d<FloatType> hist = os.image().histogram(localMinimaResponse);
        float otsuThreshold = os.threshold().otsu(hist).getRealFloat();
		Pair< List< Puncta >, List< Float > > thresholdedPairList = getThresholdedLocalMinima( otsuThreshold, resultsTable );

		/* Pick the blob closest to the click with a good enough LOG response */
		List< Puncta > potentBlobs = thresholdedPairList.getA();
		List< Double > weights = new ArrayList<>();
		double epsilon = 0.01;
		for ( int i = 0; i < potentBlobs.size(); i++ ) {
			double weight =
					1 * ( 1 / ( computeDistFromClick( potentBlobs.get( i ), view.getWindowSize() ) + epsilon ) );
			weights.add( weight );
		}
		Double maxWeight = 0d;
		int maxWeightInd = 0;
		for ( int ind = 0; ind < weights.size(); ind++ ) {
			if ( weights.get( ind ) > maxWeight ) {
				maxWeight = weights.get( ind );
				maxWeightInd = ind;
			}
		}
		return potentBlobs.get( maxWeightInd );

    }

	private double computeDistFromClick( Puncta puncta, float smallPatchSize ) {
		return Math.sqrt( Math.pow( ( puncta.getX() - smallPatchSize / 2 ), 2 ) + Math.pow( ( puncta.getY() - smallPatchSize / 2 ), 2 ) );
	}

	private SampleList< FloatType > createIterableList( final Column column ) {

        final Iterator<Float> iterator = column.iterator();
        final List<FloatType> imageResponse = new ArrayList<>();
        while (iterator.hasNext()) {
            imageResponse.add(new FloatType(iterator.next().floatValue()));
        }

        return new SampleList<>(imageResponse);
    }

	private Pair< List< Puncta >, List< Float > > getThresholdedLocalMinima( float threshold, GenericTable resultsTable ) {
		Column< ? > valueOld = resultsTable.get( "Value" );
		Column< ? > XOld = resultsTable.get( "X" );
		Column< ? > YOld = resultsTable.get( "Y" );
		Column< ? > radiusOld = resultsTable.get( "Radius" );
		List< Puncta > listPuncta = new LinkedList<>();
		List< Float > listVal = new LinkedList<>();

		for ( int i = 0; i < resultsTable.getRowCount(); i++ ) {
			if ( ( float ) valueOld.get( i ) <= threshold ) {
				listPuncta.add( new Puncta( ( int ) XOld.get( i ), ( int ) YOld.get( i ), 0, ( float ) radiusOld.get( i ) ) );
				listVal.add( ( float ) valueOld.get( i ) );
			}
		}
		return new ValuePair<>( listPuncta, listVal );
	}


}
