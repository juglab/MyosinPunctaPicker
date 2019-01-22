package de.csbd.learnathon.command;

import java.awt.event.MouseListener;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
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


	private void rightClickAction( int x, int y ) {
		actionSelect( x, y );

	}

	public void defineBehaviour() {
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "my-new-behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {clickAction( x, y );}, "left click", "button1" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {rigthClickAction( x, y );}, "rigth click", "P" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {actionMoveSelectedPuncta( x, y );}, "space click", "SPACE" );
	
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
	
	private void rigthClickAction( int x, int y ) {
			actionSelect( x, y );
	}
	

	

	/**
	 * @param x
	 * @param y
	 */
	private void actionSelect( int x, int y ) {
		if ( !model.getPuncta().isEmpty() ) {
			bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
			Puncta minDistPuncta = model.getClosestPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), model.getPuncta() );
			if ( model.radius > ( model.getClosestPunctaDist( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), model.getPuncta() ) ) ) {
				model.setSelectedPuncta( minDistPuncta );
				Graph selectedTracklet = model.getGraph();
				selectedTracklet = selectedTracklet.selectSubgraphContaining( minDistPuncta );
				model.selectSubgraph( selectedTracklet );
			} else {
				model.setSelectedPuncta( new Puncta() );
				model.selectSubgraph( new Graph() );
			}
			overlay.paint();
			getOverlay().refreshBdv();
			if ( !model.getSelectedPuncta().isEmpty() )
				bdv.getViewerPanel().setTimepoint( model.getSelectedPuncta().getT() );
			
			
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
		model.setLatest( nearestP );

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
	
	
	
	
	
	public void actionMoveSelectedPuncta( int x, int y )
	{
		bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
		model.getSelectedPuncta().setX(pos.getFloatPosition(0));
		model.getSelectedPuncta().setY(pos.getFloatPosition(1));
		overlay.refreshBdv();
	}
	
	

}
