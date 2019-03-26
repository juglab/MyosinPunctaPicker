package de.csbd.learnathon.command;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import bdv.util.BdvOverlay;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.real.DoubleType;

public class FlowOverlay extends BdvOverlay {
	
	private RandomAccessibleInterval< DoubleType > flowData;
	private ArrayList< FlowVector > sparseFlow;

	private PunctaPickerView view;
	
	private boolean visible = false;

	public FlowOverlay( PunctaPickerView view ) {
		this.view = view;
	}

	public void setVisible( final boolean visible ) {
		this.visible = visible;
	}

	public void paintDenseFlow( RandomAccessibleInterval< DoubleType > flowData ) {
		this.flowData = flowData;
	}

	public void paintSparseFlow(ArrayList< FlowVector > sparseFlow) {
		this.sparseFlow = sparseFlow;
	}

	@Override
	protected void draw( Graphics2D g ) {
		int t = info.getTimePointIndex();

		if ( visible ) {
			prepareSparseVectors( g, t );
			prepareDenseVectors( g, t );
		}

	}

	private void prepareSparseVectors( Graphics2D g, int t ) {
		ArrayList< FlowVector > sparseFlowVectors = new ArrayList<>();
		for ( FlowVector flowVector : sparseFlow ) {
			if ( flowVector.getT() == t )
				sparseFlowVectors.add( flowVector );
		}
		for ( FlowVector f : sparseFlowVectors ) {
			drawVector( g, ( int ) f.getX(), ( int ) f.getY(), f.getU(), f.getV(), Color.BLUE );
		}
	}

	private void prepareDenseVectors( Graphics2D g, int t ) {
		final long sizeX = flowData.dimension( 0 );
		final long sizeY = flowData.dimension( 1 );

		if ( t < flowData.dimension( 2 ) / 2 ) {
			int spacing = 10; // spacing between pixels for flow display
			spacing = Math.max( spacing, ( int ) Math.max( sizeX, sizeY ) / 25 ); // but if large image only 25 vecs along longer side

			int startx = ( int ) ( sizeX % spacing ) / 2;
			startx = ( startx == 0 ) ? spacing / 2 : startx;
			int starty = ( int ) ( sizeY % spacing ) / 2;
			starty = ( starty == 0 ) ? spacing / 2 : starty;

			for ( int x = startx; x < sizeX; x += spacing ) {
				for ( int y = starty; y < sizeY; y += spacing ) {

					FlowVector flowVec = getFlowVector( flowData, x, y, t );
					drawVector( g, x, y, flowVec.getU(), flowVec.getV(), Color.RED );
				}
			}
		}
	}

	private FlowVector getFlowVector( RandomAccessibleInterval< DoubleType > f, int x, int y, int t ) {
		RandomAccess< DoubleType > ra = f.randomAccess();
		ra.setPosition( x, 0 );
		ra.setPosition( y, 1 );
		ra.setPosition( 2 * t, 2 );
		Double u = ra.get().getRealDouble();
		ra.setPosition( x, 0 );
		ra.setPosition( y, 1 );
		ra.setPosition( 2 * t + 1, 2 );
		Double v = ra.get().getRealDouble();
		FlowVector flowVector = new FlowVector( x, y, t, u, v );
		return flowVector;
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
