package de.csbd.learnathon.command;

import java.util.List;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import net.imglib2.RealPoint;

public class FlowController {

	private PunctaPickerView view;
	private PunctaPickerModel<?> model;

	public FlowController( PunctaPickerModel<?> model, PunctaPickerView punctaPickerView ) {
		this.view = punctaPickerView;
		this.model = model;
		installBehaviour();
	}

	private void installBehaviour() {
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( view.getBdv().getBdvHandle().getTriggerbindings(), "my-new-behaviours-flow" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionSelectFlowVector( x, y );
		}, "SelectFlow", " W" );
		behaviours.behaviour( new MoveFlow(), "MoveFlow", "SPACE" );

	}

	private void actionSelectFlowVector( int x, int y ) {
		
		RealPoint pos = new RealPoint( 3 );
		int time = view.getBdv().getViewerPanel().getState().getCurrentTimepoint();
		List< FlowVector > availableFlowVectors = model.getFlowVectorsCollection().getSpacedFlowVectors();
		if ( !availableFlowVectors.isEmpty() ) {
			view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );

			FlowVector minDistVector =
					PPGraphUtils.getClosestFlowVectorAtTimePoint( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), availableFlowVectors, time );
			if ( minDistVector != null ) {
				for ( FlowVector flowVector : availableFlowVectors ) {
					flowVector.setSelected( false ); //Select only one flow vector at a time
				}
				view.getPunctaPickerModel().getGraph().setLeadSelectedPuncta( null ); //Disable Lead Selected puncta if flow vector is selected
				minDistVector.setSelected( true );
			}
			view.getFlowOverlay().requestRepaint();
		}

	}

	private class MoveFlow implements DragBehaviour {

		@Override
		public void init( int x, int y ) {}

		@Override
		public void drag( int x, int y ) {
			moveFlowVector();
		}

		@Override
		public void end( int x, int y ) {
			moveFlowVector();
			FlowVector sel = model.getFlowVectorsCollection().getOnlySelectedFlowVector();
			Puncta pA = new Puncta( sel.getX(), sel.getY(), sel.getT(), 1 );
			Puncta pB = new Puncta( ( float ) ( sel.getX() + sel.getU() ), ( float ) ( sel.getY() + sel.getV() ), sel.getT() + 1, 1 );
			model.getGraph().addPuncta( pA );
			model.getGraph().addPuncta( pB );
			model.getGraph().addEdge( new Edge( pA, pB ) );

		}

		private void moveFlowVector() {
			FlowVector sel = model.getFlowVectorsCollection().getOnlySelectedFlowVector();
			final RealPoint posn = new RealPoint( 3 );
			view.getBdv().getViewerPanel().getGlobalMouseCoordinates( posn );
			sel.setU( posn.getFloatPosition( 0 ) );
			sel.setV( posn.getFloatPosition( 1 ) );
		}



	}

}
