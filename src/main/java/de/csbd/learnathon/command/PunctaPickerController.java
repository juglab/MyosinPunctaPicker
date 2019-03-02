package de.csbd.learnathon.command;

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
import net.imglib2.view.Views;

public class PunctaPickerController {

    private RealPoint pos;
    private PunctaPickerModel model;
    private PunctaPickerView view;
    private OpService os;
	private int patchSize;

	public PunctaPickerController( PunctaPickerModel model, PunctaPickerView punctaPickerView, OpService os ) {
        this.model = model;
        this.view = punctaPickerView;

        this.os = os;
    }

    public void defineBehaviour() {
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( view.getBdv().getBdvHandle().getTriggerbindings(), "my-new-behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionClick( x, y );
		}, "Add", "A" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionSelectClosestSubgraph( x, y );
		}, "Select", "C" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionMoveLeadPuncta( x, y );
		}, "Move", "SPACE" );

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
				model.getGraph().deleteSelectedElements();
				model.getView().getBdv().getViewerPanel().requestRepaint();

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
                    minDistPuncta.setSelected(true);
                    g.setLeadSelectedPuncta(minDistPuncta);
                    g.selectSubgraphContaining(minDistPuncta);
                    view.getBdv().getViewerPanel().setTimepoint(minDistPuncta.getT());
                }
                view.getBdv().getViewerPanel().requestRepaint();
            }
        }
    }

    private <T extends RealType<T> & NativeType<T>> void actionClick(int x, int y) {
		actionClickInThread( x, y );
    }


    private <T extends RealType<T> & NativeType<T>> void actionClickInThread(int x, int y) {
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

		///Blob detection routine starts here

        Img<T> image = view.getImage();
        Views.extendMirrorSingle(image);
		patchSize = 15;
        FinalInterval cropped = Intervals.createMinMax((long) (pos.getDoublePosition(0) - patchSize / 2), (long) (pos.getDoublePosition(1) - patchSize / 2), 0, (long) (pos.getDoublePosition(0) + patchSize / 2), (long) (pos.getDoublePosition(1) + patchSize / 2), 0);
        RandomAccessibleInterval<T> croppedImage = Views.interval(image, cropped);
        ImagePlus imgPlus = ImageJFunctions.wrap(croppedImage, "cropped");
//		ImageJFunctions.show( croppedImage );
        Img<T> newImage = ImageJFunctions.wrap(imgPlus);

        Puncta pNew = detectFeatures(newImage);

		///Blob detection routine ends 
        pNew.setT(t);
        pNew.setX((float) (pos.getDoublePosition(0) - patchSize / 2) + pNew.getX());
        pNew.setY((float) (pos.getDoublePosition(1) - patchSize / 2) + pNew.getY());

        model.getGraph().addPuncta(pNew);
        model.getGraph().setLeadSelectedPuncta(pNew);
        pNew.setSelected(true);
        if (pOld != null && pOld.getT() == t - 1) {
            addSelectedEdge(g, pOld, pNew);
        } else {
            model.getGraph().unselectAll();
            pNew.setSelected(true);
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

    private <T extends RealType<T> & NativeType<T>> Puncta detectFeatures(Img<T> image) {
        double minScale = 2;
        double stepScale = 1;
        double maxScale = 5;
        boolean brightBlobs = true;
        int axis = 0;
        double samplingFactor = 1;

		BlobDetectionCommand< T > blobDetection =
				new BlobDetectionCommand<>( image, minScale, maxScale, stepScale, brightBlobs, axis, samplingFactor, os );
		final GenericTable resultsTable = blobDetection.getResultsTable();

        /*Step Two: Find Otsu Threshold Value on the new List, so obtained*/
        SampleList<FloatType> localMinimaResponse = createIterableList(resultsTable.get("Value"));
        Histogram1d<FloatType> hist = os.image().histogram(localMinimaResponse);
        float otsuThreshold = os.threshold().otsu(hist).getRealFloat();
		Pair< List< Puncta >, List< Float > > thresholdedPairList = getThresholdedLocalMinima( otsuThreshold, resultsTable );

		/* Pick the blob closest to the click with a good enough LOG response */
		List< Puncta > potentBlobs = thresholdedPairList.getA();
//		List< Float > potentBlobValues = thresholdedPairList.getB();
		List< Double > weights = new ArrayList<>();
		double epsilon = 0.01;
		for ( int i = 0; i < potentBlobs.size(); i++ ) {
			double weight =
					1 * ( 1 / ( computeDistFromClick( potentBlobs.get( i ) ) + epsilon ) );
//			double weight =
//					0.5 * Math.abs( potentBlobValues.get( i ) );
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

	private double computeDistFromClick( Puncta puncta ) {
		return Math.sqrt( Math.pow( ( puncta.getX() - patchSize / 2 ), 2 ) + Math.pow( ( puncta.getY() - patchSize / 2 ), 2 ) );
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
