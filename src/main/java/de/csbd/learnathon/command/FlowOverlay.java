package de.csbd.learnathon.command;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import bdv.util.BdvOverlay;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

public class FlowOverlay extends BdvOverlay {

	private RandomAccessibleInterval flowData;

	private List< FlowVector > handPickedSparseFlowVectors;

	private PunctaPickerView view;

	private boolean visible = false;

	private List< FlowVector > spacedFlowVectors;

	private List< FlowVector > autoFeatureFlowVectors;

	public FlowOverlay( PunctaPickerView view ) {
		this.view = view;
	}

	public void setVisible( final boolean visible ) {
		this.visible = visible;
	}

	public <T> void setDenseFlow( RandomAccessibleInterval< T > flowData ) {
		this.flowData = flowData;
	}

	public void setHandPickedSparseFlow( List< FlowVector > sparseFlow ) {
		this.handPickedSparseFlowVectors = sparseFlow;
	}

	public void setAutoFeatureFlow( List< FlowVector > autoFeatureFlow ) {
		this.autoFeatureFlowVectors = autoFeatureFlow;
	}

	public void requestRepaint() {
		view.getBdv().getViewerPanel().requestRepaint();
	}

	public < T extends RealType< T > & NativeType< T > > List< FlowVector > prepareSpacedFlow() {
		spacedFlowVectors = new ArrayList<>();
		if ( flowData != null ) {
			final long sizeX = flowData.dimension( 0 );
			final long sizeY = flowData.dimension( 1 );
			int spacing = view.getDensity(); // spacing between pixels for flow display 

			for ( int t = 0; t < flowData.dimension( 2 ) / 2; t++ ) {
				int startx = ( int ) ( sizeX % spacing ) / 2;
				startx = ( startx == 0 ) ? spacing / 2 : startx;
				int starty = ( int ) ( sizeY % spacing ) / 2;
				starty = ( starty == 0 ) ? spacing / 2 : starty;
				for ( int x = startx; x < sizeX; x += spacing ) {
					for ( int y = starty; y < sizeY; y += spacing ) {
						FlowVector flowVec = getFlowVector( flowData, x, y, t );
						spacedFlowVectors.add( flowVec );
					}
				}
			}
		}
		view.getPunctaPickerModel().getFlowVectorsCollection().setSpacedFlow( spacedFlowVectors );
		return spacedFlowVectors;
	}

	private < T extends RealType< T > & NativeType< T > > FlowVector getFlowVector( RandomAccessibleInterval< T > f, int x, int y, int t ) {
		RandomAccess< T > ra = f.randomAccess();
		ra.setPosition( x, 0 );
		ra.setPosition( y, 1 );
		ra.setPosition( 2 * t, 2 );
		Double u = ra.get().getRealDouble();
		ra.setPosition( x, 0 );
		ra.setPosition( y, 1 );
		ra.setPosition( 2 * t + 1, 2 );
		Double v = ra.get().getRealDouble();
		return new FlowVector( x, y, t, u, v );
	}

	@Override
	protected void draw( Graphics2D g ) {
		int t = info.getTimePointIndex();

		if ( visible ) {
			if ( view.getShowAutoFlowOnlyFlag() && autoFeatureFlowVectors != null ) {
				drawAutoFeatureFlow( g, t );
				return;
			}

			if ( !view.getShowAutoFlowOnlyFlag() && handPickedSparseFlowVectors != null )
				drawSparseHandPickedFlow( g, t );
			if ( spacedFlowVectors != null )
				drawSpacedFlow( g, t );
			if ( autoFeatureFlowVectors != null )
				drawAutoFeatureFlow( g, t );
		}

	}

	private void drawAutoFeatureFlow( Graphics2D g, int t ) {
		List< FlowVector > flowVectors = new ArrayList<>();
		for ( FlowVector flowVector : autoFeatureFlowVectors ) {
			if ( flowVector.getT() == t )
				flowVectors.add( flowVector );
		}
		for ( FlowVector f : flowVectors ) {
			drawVector( g, ( int ) f.getX(), ( int ) f.getY(), f.getU(), f.getV(), Color.BLUE.brighter().brighter() );
		}

	}

	private void drawSparseHandPickedFlow( Graphics2D g, int t ) { //Can be simplified to only draw but stays here
		List< FlowVector > sparseFlowVectors = new ArrayList<>();
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
				Color color = flowVec.isSelected() ? Color.LIGHT_GRAY : Color.GREEN;
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

		int xto = ( int ) Math.round( x + u );
		int yto = ( int ) Math.round( y + v );

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
