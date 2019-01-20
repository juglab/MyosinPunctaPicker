package de.csbd.learnathon.command;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform2D;

public class Overlay {

	private static BdvHandlePanel bdv;
	private PunctaPickerModel model;
	private int thickness = 3;

	public Overlay( BdvHandlePanel bdv, PunctaPickerModel model ) {
		Overlay.bdv = bdv;
		this.model = model;
	}

	public void paint() {
		List< Puncta > allPuncta = model.getPuncta();

		final BdvOverlay overlay = new BdvOverlay() {

			@Override
			protected void draw( final Graphics2D g ) {

				final AffineTransform2D t = new AffineTransform2D();
				getCurrentTransform2D( t );

				final double[] lPos = new double[ 2 ];
				final double[] gPos = new double[ 2 ];
				final double[] lPos1 = new double[ 2 ];
				final double[] lPos2 = new double[ 2 ];
				final double[] gPos1 = new double[ 2 ];
				final double[] gPos2 = new double[ 2 ];

				final int start = 0;
				final int end = allPuncta.size();
				final int radius = 20;

				for ( int i = start; i < end; i++ ) {
					if ( allPuncta.get( i ).getT() <= info.getTimePointIndex() ) {
						g.setStroke( new BasicStroke( thickness ) );
					} else {
						g.setStroke( new BasicStroke( thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0 ) );
					}
					if ( allPuncta.get( i ).equals( model.getSelectedPuncta() ) ) {
						g.setStroke( new BasicStroke( 2 * thickness ) );
					}
					RealPoint planarPoint = new RealPoint( allPuncta.get( i ).getX(), allPuncta.get( i ).getY() );
					planarPoint.localize( lPos );
					t.apply( lPos, gPos );
					if ( model.getSelectedSubgraph().graphContainsPuncta( allPuncta.get( i ) ) )
						g.setColor( Color.RED );
					else
						g.setColor( Color.BLUE );
					g.drawOval(
							( int ) ( gPos[ 0 ] - radius ),
							( int ) ( gPos[ 1 ] - radius ),
							radius * 2,
							radius * 2 );

				}

				for ( int i = 0; i < model.getEdges().size(); i++ ) {

					Edge edge = model.getEdges().get( i );
					RealPoint point1 = new RealPoint( edge.getA().getX(), edge.getA().getY() );
					point1.localize( lPos1 );
					t.apply( lPos1, gPos1 );

					RealPoint point2 = new RealPoint( edge.getB().getX(), edge.getB().getY() );
					point2.localize( lPos2 );
					t.apply( lPos2, gPos2 );
					if ( model.getSelectedSubgraph().graphContainsEdge( model.getEdges().get( i ) ) )
						g.setColor( Color.RED );
					else
						g.setColor( Color.BLUE );
					if ( edge.getA().getT() <= info.getTimePointIndex() && edge.getB().getT() <= info.getTimePointIndex() )
						g.setStroke( new BasicStroke( thickness ) );
					else
						g.setStroke( new BasicStroke( thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0 ) );
					drawPeripheralLine(
							g,
							( float ) gPos1[ 0 ],
							( float ) gPos1[ 1 ],
							( float ) gPos2[ 0 ],
							( float ) gPos2[ 1 ],
							radius );

				}
			}
		};
		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );

	}

	protected void drawPeripheralLine( Graphics2D g, float x1, float y1, float x2, float y2, float radius ) {

		float common_factor2 = radius / ( float ) Math.sqrt( 1 + ( ( y2 - y1 ) * ( y2 - y1 ) ) / ( ( x2 - x1 ) * ( x2 - x1 ) ) );

		float x1_prime1 = x1 + common_factor2;
		float x1_prime2 = x1 - common_factor2;
		float y1_prime1 = ((y2 -y1)/(x2-x1))*(x1_prime1 - x1) + y1;
		float y1_prime2 = ((y2 -y1)/(x2-x1))*(x1_prime2 - x1) + y1;


		float x2_prime1 = x2 + common_factor2;
		float x2_prime2 = x2 - common_factor2;
		float y2_prime1 = ( ( y2 - y1 ) / ( x2 - x1 ) ) * ( x2_prime1 - x1 ) + y1;
		float y2_prime2 = ( ( y2 - y1 ) / ( x2 - x1 ) ) * ( x2_prime2 - x1 ) + y1;

		float x1_prime, y1_prime, x2_prime, y2_prime;

		float d1 = ( x1_prime1 - x2_prime1 ) * ( x1_prime1 - x2_prime1 ) + ( y1_prime1 - y2_prime1 ) * ( y1_prime1 - y2_prime1 );
		float d2 = ( x1_prime2 - x2_prime1 ) * ( x1_prime2 - x2_prime1 ) + ( y1_prime2 - y2_prime1 ) * ( y1_prime2 - y2_prime1 );

		if ( d1 <= d2 ) {
			x1_prime = x1_prime1;
			y1_prime = y1_prime1;
		} else {
			x1_prime = x1_prime2;
			y1_prime = y1_prime2;
		}
		
		d1 = ( x2_prime1 - x1_prime1 ) * ( x2_prime1 - x1_prime1 ) + ( y2_prime1 - y1_prime1 ) * ( y2_prime1 - y1_prime1 );
		d2 = ( x2_prime2 - x1_prime1 ) * ( x2_prime2 - x1_prime1 ) + ( y2_prime2 - y1_prime1 ) * ( y2_prime2 - y1_prime1 );
		
		if ( d1 <= d2 ) {
			x2_prime = x2_prime1;
			y2_prime = y2_prime1;
		} else {
			x2_prime = x2_prime2;
			y2_prime = y2_prime2;
		}
		
		g.drawLine( ( int ) x1_prime, ( int ) y1_prime, ( int ) x2_prime, ( int ) y2_prime );
	}

	public void refreshBdv() {
		this.paint();
	}

}