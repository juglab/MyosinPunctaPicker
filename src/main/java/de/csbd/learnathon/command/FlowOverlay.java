package de.csbd.learnathon.command;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import bdv.util.BdvOverlay;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.real.DoubleType;

public class FlowOverlay extends BdvOverlay {
	
	private RandomAccessibleInterval< DoubleType > flowData;

	private ArrayList< FlowVector > handPickedSparseFlowVectors;

	private PunctaPickerView view;
	
	private boolean visible = false;

	private ArrayList< FlowVector > spacedFlowVectors;

	private ArrayList< FlowVector > autoFeatureFlowVectors;

	public FlowOverlay( PunctaPickerView view ) {
		this.view = view;
	}

	public void setVisible( final boolean visible ) {
		this.visible = visible;
	}

	public void setDenseFlow( RandomAccessibleInterval< DoubleType > flowData ) {
		this.flowData = flowData;
	}

	public void setHandPickedSparseFlow(ArrayList< FlowVector > sparseFlow) {
		this.handPickedSparseFlowVectors = sparseFlow;
	}

	public void setAutoFeatureFlow( ArrayList< FlowVector > autoFeatureFlow ) {
		this.autoFeatureFlowVectors = autoFeatureFlow;
	}

	public void setSpacedFlow( ArrayList< FlowVector > spacedFlow ) {
		this.spacedFlowVectors = spacedFlow;
	}

	public void requestRepaint() {
		view.getBdv().getViewerPanel().requestRepaint();
	}

	@Override
	protected void draw( Graphics2D g ) {
		int t = info.getTimePointIndex();

		if ( visible ) {
			if ( !( handPickedSparseFlowVectors == null ) )
				drawSparseHandPickedFlow( g, t );
			if ( view.getShowAutoFlowOnlyCheckBox() && !( autoFeatureFlowVectors == null ) )
				drawAutoFeatureFlow( g, t );
			if ( !( spacedFlowVectors == null ) )
				drawSpacedFlow( g, t );
		}

	}

	private void drawAutoFeatureFlow( Graphics2D g, int t ) {
		ArrayList< FlowVector > flowVectors = new ArrayList<>();
		for ( FlowVector flowVector : autoFeatureFlowVectors ) {
			if ( flowVector.getT() == t )
				flowVectors.add( flowVector );
		}
		for ( FlowVector f : flowVectors ) {
			drawVector( g, ( int ) f.getX(), ( int ) f.getY(), f.getU(), f.getV(), Color.GREEN );
		}

	}

	private void drawSparseHandPickedFlow( Graphics2D g, int t ) { //Can be simplified to only draw but stays here
		ArrayList< FlowVector > sparseFlowVectors = new ArrayList<>();
		for ( FlowVector flowVector : handPickedSparseFlowVectors ) {
			if ( flowVector.getT() == t )
				sparseFlowVectors.add( flowVector );
		}
		for ( FlowVector f : sparseFlowVectors ) {
			drawVector( g, ( int ) f.getX(), ( int ) f.getY(), f.getU(), f.getV(), Color.ORANGE );
		}
	}

	private void drawSpacedFlow( Graphics2D g, int t ) {
		for ( FlowVector flowVec : spacedFlowVectors ) {
			if ( flowVec.getT() == t ) {
				float x = flowVec.getX();
				float y = flowVec.getY();
				Color color = flowVec.isSelected() ? Color.LIGHT_GRAY : Color.RED;
				drawVector( g, ( int ) x, ( int ) y, flowVec.getU(), flowVec.getV(), color );
			}
		}
	}

	private void drawVector( final Graphics2D g, int x, int y, double u, double v, Color c ) {

		if ( x == 0 && y == 0 ) return;
		final AffineTransform2D trans = new AffineTransform2D();
		getCurrentTransform2D( trans );

		g.setColor( c );

		final Graphics2D g2 = g;
		g2.setStroke( new BasicStroke( 1 ) );

		int xto = ( int ) ( x + u );
		int yto = ( int ) ( y + v );

		final double[] from = new double[] { x, y };
		final double[] to = new double[] { xto, yto };
		trans.apply( from, from );
		trans.apply( to, to );

		x = ( int ) from[ 0 ];
		y = ( int ) from[ 1 ];
		xto = ( int ) to[ 0 ];
		yto = ( int ) to[ 1 ];

		g2.drawLine( x, y, xto, yto );

		g2.setColor( Color.PINK );
		g2.drawRect( x - 1, y - 1, 2, 2 );

	}
		
}