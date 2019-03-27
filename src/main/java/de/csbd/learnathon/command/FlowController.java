package de.csbd.learnathon.command;

import java.util.ArrayList;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import net.imglib2.RealPoint;

public class FlowController {

	private PunctaPickerView view;
	private RealPoint pos;
	private PunctaPickerModel model;

	public FlowController( PunctaPickerModel model, PunctaPickerView punctaPickerView ) {
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
//		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
//			actionMoveFlowVector( x, y );
//		}, "MoveFlow", "SPACE" );

	}

	private void actionSelectFlowVector( int x, int y ) {
		pos = new RealPoint( 3 );
		int time = view.getBdv().getViewerPanel().getState().getCurrentTimepoint();
		ArrayList< FlowVector > availableFlowVectors = model.getFlowVectorsCollection().getSpacedFlowVectors();
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

//	private void actionMoveFlowVector( int x, int y ) {
//
//		FlowVector fv = model.getFlowVectorsCollection().getOnlySelectedFlowVector();
//		if ( !( fv == null ) ) {
//			view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
//			fv.setU( pos.getFloatPosition( 0 ) );
//			fv.setV( pos.getFloatPosition( 1 ) );
//			}
//			view.getFlowOverlay().requestRepaint();
//	}

}
