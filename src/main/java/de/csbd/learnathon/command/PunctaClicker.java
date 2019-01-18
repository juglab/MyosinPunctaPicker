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

	public void mainClick() {

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "my-new-behaviours" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			pos = new RealPoint( 3 );
			bdv.getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
			ViewerState state = bdv.getBdvHandle().getViewerPanel().getState();
			int t = state.getCurrentTimepoint();
			model.addPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t );
//			overlay = new Overlay( bdv, model );
			overlay.paintPoint();
			bdv.getViewerPanel().nextTimePoint();
			overlay.paintAncestors();
			System.out.println( "global coordinates: " + Util.printCoordinates( pos ) );
		}, "print global pos", "button1" );


	}


}
