package de.csbd.learnathon.command;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.util.BdvHandlePanel;
import bdv.viewer.state.ViewerState;
import net.imglib2.RealPoint;

public class PunctaClicker {

	private static BdvHandlePanel bdv;
	private RealPoint pos;
	private PunctaPickerModel model;
	private Overlay overlay;

	public PunctaClicker( BdvHandlePanel bdv, PunctaPickerModel model ) {
		PunctaClicker.bdv = bdv;
		this.model = model;
		this.overlay = new Overlay( bdv, model );
	}

	public Overlay getOverlay() {
		return overlay;
	}

	public void defineClickBehaviour() {
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "my-new-behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			clickAction( x, y );

		}, "print global pos", "button1" );

	}

	private void clickAction( int x, int y ) {
		if ( model.getActionIndicator().equals( PunctaPickerModel.ACTION_MODIFY ) ) {

		}

		else if ( model.getActionIndicator().equals( PunctaPickerModel.ACTION_SELECT ) ) {

			actionSelect( x, y );
		}

		else if ( model.getActionIndicator().equals( PunctaPickerModel.ACTION_TRACK ) ) {
			actionTrack( x, y );
				}
	}

	private void actionSelect( int x, int y ) {
		if ( !model.getPuncta().isEmpty() ) {
			bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
			Puncta minDistPuncta = model.getClosestPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), model.getPuncta() );
			model.setSelectedPuncta( minDistPuncta );
			Graph selectedTracklet = model.getGraph();
			selectedTracklet = selectedTracklet.selectSubgraphContaining( minDistPuncta );
			model.selectSubgraph( selectedTracklet );
			overlay.paint();
		}
	}

	private void actionTrack( int x, int y ) {
		pos = new RealPoint( 3 );
		bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
		ViewerState state = bdv.getBdvHandle().getViewerPanel().getState();
		int t = state.getCurrentTimepoint();
		Puncta pOld = model.getLatestPuncta();
		Puncta p;
		if ( !model.getPunctaAtTime( t ).isEmpty() ) {
			if ( model.getClosestPunctaDist( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), model.getPunctaAtTime( t ) ) < model.radius ) {
				p = model.getClosestPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), model.getPunctaAtTime( t ) );
				mergeTrack( t, pOld, p );
			
			}
			else
				p = continueOriginalTrack( t, pOld );
		} else {
			p = continueOriginalTrack( t, pOld );
		}
			


		Graph selectedTracklet = model.getGraph();
		selectedTracklet = selectedTracklet.selectSubgraphContaining( p );
		model.selectSubgraph( selectedTracklet );
		model.setSelectedPuncta( p );
		overlay.paint();
		bdv.getViewerPanel().nextTimePoint();
	}

	private void mergeTrack( int t, Puncta pOld, Puncta nearestP ) {
		model.addEdge( pOld, nearestP );

	}

	private Puncta continueOriginalTrack( int t, Puncta pOld ) {
		Puncta p = new Puncta();
		int numTimepoints = bdv.getViewerPanel().getState().getNumTimepoints();
		if ( pOld != null ) {
			if ( !( t == numTimepoints - 1 && pOld.getT() == numTimepoints - 1 ) ) {
				p = model.addPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t );
				if ( pOld.getT() == p.getT() - 1 ) {
					model.addEdge( pOld, p );
				}
			}
		} else {
			p = model.addPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t );
				}
		return p;
	}


	public void updateOverlay() {
		overlay.paint();
	}


}
