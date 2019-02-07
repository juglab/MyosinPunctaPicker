package de.csbd.learnathon.command;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.viewer.state.ViewerState;
import net.imglib2.RealPoint;
import net.imglib2.util.Pair;

public class PunctaPickerController {

	public static String ACTION_NONE = "none";
	public static String ACTION_TRACK = "track";
	public static String ACTION_SELECT = "select";
	private String actionIndicator = ACTION_NONE;

	private RealPoint pos;
	private PunctaPickerModel model;
	private PunctaPickerView view;

	public PunctaPickerController( PunctaPickerModel model, PunctaPickerView punctaPickerView ) {
		this.model = model;
		this.view = punctaPickerView;
	}

	public void defineBehaviour() {
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( view.getBdv().getBdvHandle().getTriggerbindings(), "my-new-behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			clickAction( x, y );
		}, "left click", "button1" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionSelectClosestSubgraph( x, y );
		}, "rigth click", "button3" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			actionMoveLeadPuncta( x, y );
		}, "space click", "SPACE" );
	
		InputMap im = view.getBdv().getViewerPanel().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
		ActionMap am = view.getBdv().getViewerPanel().getActionMap();
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_L, 0 ), "Link" );
		am.put( "Link", new AbstractAction() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( !( model.getGraph().getLeadSelectedPuncta() == null ) && !( model.getGraph().getMouseSelectedPuncta() == null ) ) {
					model.getGraph().addEdge( new Edge( model.getGraph().getLeadSelectedPuncta(), model.getGraph().getMouseSelectedPuncta() ) );
				}
			}
		} );
	}
	
	private void clickAction( int x, int y ) { // TODO clickAction and actionClick might not be self expainatory... ;)
		if ( actionIndicator.equals( ACTION_SELECT ) ) {
			actionSelectClosestSubgraph( x, y );
		} else if ( actionIndicator.equals( ACTION_TRACK ) ) {
			actionClick( x, y );
		}
	}
	
	/**
	 * Sets one of the ACTION_* strings statically defined in this class.
	 * 
	 * @param s
	 *            any of the ACTION_* strings defined in this class
	 */
	public void setActionIndicator( String s ) {
		actionIndicator = s;
	}

	public String getActionIndicator() {
		return actionIndicator;
	}

	private void actionSelectClosestSubgraph( int x, int y ) {
		Graph g = model.getGraph();

		if ( !g.getPunctas().isEmpty() ) {
			view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );

			Pair<Puncta,Double> minEval = PPGraphUtils.getClosestPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), g.getPunctas() );
			if ( minEval != null ) {
				Puncta minDistPuncta = minEval.getA();
				double minDist = Math.sqrt( minEval.getB() );
				if ( minDistPuncta.getR() >= minDist ) {
					minDistPuncta.setSelected( true );
					g.setLeadSelectedPuncta( minDistPuncta );
					g.selectSubgraphContaining( minDistPuncta );
				}
				view.getBdv().getViewerPanel().setTimepoint( minDistPuncta.getT() );
				view.getBdv().getViewerPanel().requestRepaint();
			}
		}
	}

	private void actionClick( int x, int y ) {
		pos = new RealPoint( 3 );
		view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
		ViewerState state = view.getBdv().getBdvHandle().getViewerPanel().getState();

		int t = state.getCurrentTimepoint();
		Graph g = model.getGraph();
		Puncta pOld = g.getLeadSelectedPuncta();

		if ( !g.getPunctaAtTime( t ).isEmpty() ) {
			Pair<Puncta,Double> min = PPGraphUtils.getClosestPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), g.getPunctaAtTime( t ) );

			if ( min.getB() < min.getA().getR() ) {
				addSelectedEdge( g, pOld, min.getA() );
				model.getGraph().setLeadSelectedPuncta( min.getA() );
			}
//			return;
		}

		Puncta pNew = new Puncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), t, model.getDefaultRadius() );
		model.getGraph().addPuncta( pNew );
		model.getGraph().setLeadSelectedPuncta( pNew );
		pNew.setSelected( true );
		if ( pOld != null && pOld.getT() == t - 1 ) {
			addSelectedEdge( g, pOld, pNew );
		}
		else {
			model.getGraph().unselectAll();
			pNew.setSelected( true );
		}
		view.getBdv().getViewerPanel().nextTimePoint();
	}

	private void addSelectedEdge( Graph g, Puncta p1, Puncta p2 ) {
		Edge newE = new Edge( p1, p2 );
		p1.setSelected( true );
		p2.setSelected( true );
		g.addEdge( newE );
		newE.setSelected( true );
	}
	
	public void actionMoveLeadPuncta( int x, int y )
	{
		view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates( x, y, pos );
		Puncta lsp = model.getGraph().getLeadSelectedPuncta();
		lsp.setX( pos.getFloatPosition( 0 ) );
		lsp.setY( pos.getFloatPosition( 1 ) );
//		view.getOverlay().refreshBdv();
	}
}
