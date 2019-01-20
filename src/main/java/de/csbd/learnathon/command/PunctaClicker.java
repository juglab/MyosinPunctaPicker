package de.csbd.learnathon.command;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.util.BdvHandlePanel;
import bdv.viewer.state.ViewerState;
import net.imglib2.RealPoint;
import net.imglib2.util.Util;

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

	public void mainClick() {
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "my-new-behaviours" );

		if ( model.getActionIndicator().equals( PunctaPickerModel.ACTION_MODIFY ) ) {

		}

		else if ( model.getActionIndicator().equals( PunctaPickerModel.ACTION_SELECT ) ) {

			if ( !model.getPuncta().isEmpty() ) {

				behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
					float minDist = Float.MAX_VALUE;
					float distance = 0;
					Puncta minDistPuncta = null;
					bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
					for ( int i = 0; i < model.getPuncta().size(); i++ ) {
						
						distance = ( pos.getFloatPosition( 0 ) - model.getPuncta().get( i ).getX() ) * ( pos
								.getFloatPosition( 0 ) - model.getPuncta().get( i ).getX() ) + ( pos.getFloatPosition( 1 ) - model
								.getPuncta()
								.get( i )
										.getY() ) * ( pos.getFloatPosition( 1 ) - model.getPuncta().get( i ).getY() );
						if ( distance <= minDist ) {
							minDist = distance;
							minDistPuncta = model.getPuncta().get( i );
						}
					}
					model.setSelectedPuncta( minDistPuncta );
					Graph selectedTracklet = model.getGraph();
					selectedTracklet = selectedTracklet.selectSubgraphContaining( minDistPuncta );
					model.selectSubgraph( selectedTracklet );
					overlay.paint();
				}, "print global pos", "button1" );
			}
		}

		else if ( model.getActionIndicator().equals( PunctaPickerModel.ACTION_TRACK ) ) {
				behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

				pos = new RealPoint( 3 );
				bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
				ViewerState state = bdv.getBdvHandle().getViewerPanel().getState();
				int t = state.getCurrentTimepoint();
				int numTimepoints = bdv.getViewerPanel().getState().getNumTimepoints();
				Puncta pOld = model.getLatestPuncta();
				Puncta p = new Puncta();

				if ( pOld != null ) {
					if ( !( t == numTimepoints - 1 && pOld.getT() == numTimepoints - 1 ) ) {
						p = model.addPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t );
						if ( pOld.getT() == p.getT() - 1 ) {
							model.addEdge( pOld, p );
						}
					}
				}
				else {
					p = model.addPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t );
				}
				Graph selectedTracklet = model.getGraph();
				selectedTracklet = selectedTracklet.selectSubgraphContaining( p );
				model.selectSubgraph( selectedTracklet );
				model.setSelectedPuncta( p );
				overlay.paint();
				bdv.getViewerPanel().nextTimePoint();
				System.out.println( "global coordinates: " + Util.printCoordinates( pos ) );
			}, "print global pos", "button1" );
		}


	}

	public void updateOverlay() {
		overlay.paint();
	}


}
