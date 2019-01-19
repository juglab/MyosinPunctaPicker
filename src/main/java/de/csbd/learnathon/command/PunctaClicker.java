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
	private int minId;

	public PunctaClicker( BdvHandlePanel bdv, PunctaPickerModel model ) {
		PunctaClicker.bdv = bdv;
		this.model = model;
		this.overlay = new Overlay( bdv, model );
	}

	public void mainClick( String actionIndicator ) {

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "my-new-behaviours" );

		if ( actionIndicator.equals( "MODIFY" ) ) {

		}

		else if ( actionIndicator.equals( "DELETE" ) ) {
			if ( !model.getPuncta().isEmpty() ) {

				behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
					float minDist = Float.MAX_VALUE;
					float distance = 0;

					for ( int i = 0; i < model.getPuncta().size(); i++ ) {
						distance = ( x - model.getPuncta().get( i ).getX() ) * ( x - model.getPuncta().get( i ).getX() ) + ( y - model
								.getPuncta()
								.get( i )
								.getY() ) * ( y - model.getPuncta().get( i ).getY() );
						if ( distance <= minDist ) {
							minDist = distance;
							minId = model.getPuncta().get( i ).getId();
						}

					}
					int i = 0;
					while ( i < model.getPuncta().size() ) {
						if ( model.getPuncta().get( i ).getId() == minId ) {
							model.removePunctaAtIndex( i );

						} else {
							i += 1;
						}
					}
					overlay.paint();
					bdv.getViewerPanel().nextTimePoint();
					System.out.println( "global coordinates: " + Util.printCoordinates( pos ) );
				}, "print global pos", "button1" );
			}

		}

		else if ( actionIndicator.equals( "TRACK" ) ) {
				behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

				pos = new RealPoint( 3 );
				bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
				ViewerState state = bdv.getBdvHandle().getViewerPanel().getState();
				int t = state.getCurrentTimepoint();
				Puncta pOld = model.getLatestPuncta();
				Puncta p = model.addPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t );
				if ( pOld.getT() == p.getT() - 1 )
				model.addEdge( pOld, p );
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
