

package de.csbd.learnathon.command;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform2D;

public class Overlay extends BdvOverlay {
	
	private PunctaPickerModel model;

	public static float FADE_OUT_ALPHA = 1f;
	public int radius = 12;
	public int lineThickness = 2;
	public Color defaultColor = new Color( 1, 0, 1 );
	public Color selectedColor = new Color( 1, 1, 0 );

	public Overlay( PunctaPickerModel model ) {
		super();
		this.model = model;
	}

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

		float transparency;
		final int curentTime = info.getTimePointIndex();;

		for ( Puncta p : model.getGraph().getPunctas() ) {
			if ( p.getT() <= info.getTimePointIndex() ) {
				g.setStroke( new BasicStroke( lineThickness ) );
			} else {
				g.setStroke( new BasicStroke( lineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0 ) );
			}
			if ( p.equals( model.getGraph().getLeadSelectedPuncta() ) ) {
				g.setStroke( new BasicStroke( 2 * lineThickness ) );
			}
			RealPoint planarPoint = new RealPoint( p.getX(), p.getY() );
			planarPoint.localize( lPos );
			t.apply( lPos, gPos );

			transparency = Math.abs( curentTime - ( float ) p.getT() );
			transparency = ( float ) Math.exp( -transparency * FADE_OUT_ALPHA );

			if ( p.isSelected() ) {
				g.setColor( new Color( selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue(), transparency ) );
			} else {
				g.setColor( new Color( defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), transparency ) );
			}

			g.drawOval(
					( int ) ( gPos[ 0 ] - p.getR() ),
					( int ) ( gPos[ 1 ] - p.getR() ),
					( int ) p.getR() * 2,
					( int ) p.getR() * 2 );  // TODO needs to move from pixel coords to BDV coords

			if ( p.getT() == curentTime ) {
				g.setStroke( new BasicStroke( lineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0 ) );
					g.drawOval(
						( int ) ( gPos[ 0 ] - p.getR() - 4 ),
						( int ) ( gPos[ 1 ] - p.getR() - 4 ),
						( int ) p.getR() * 2 + 8,
						( int ) p.getR() * 2 + 8 );  // TODO needs to move from pixel coords to BDV coords
				}

		}

		for ( Edge edge : model.getGraph().getEdges() ) {

			RealPoint point1 = new RealPoint( edge.getA().getX(), edge.getA().getY() );
			point1.localize( lPos1 );
			t.apply( lPos1, gPos1 );

			RealPoint point2 = new RealPoint( edge.getB().getX(), edge.getB().getY() );
			point2.localize( lPos2 );
			t.apply( lPos2, gPos2 );
			if ( edge.getA().getT() <= info.getTimePointIndex() && edge.getB().getT() <= info.getTimePointIndex() ) {
				g.setStroke( new BasicStroke( lineThickness ) );
			} else {
				g.setStroke( new BasicStroke( lineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0 ) );
			}

			float d1 = Math.abs( ( float ) curentTime - edge.getA().getT() );
			float d2 = Math.abs( ( float ) curentTime - edge.getB().getT() );
			transparency = Math.min( d1, d2 );
			transparency = ( float ) Math.exp( -transparency * FADE_OUT_ALPHA );

			if ( edge.isSelected() ) {
				g.setColor( new Color( selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue(), transparency ) );
			} else {
				g.setColor( new Color( defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), transparency ) );
			}

			drawPeripheralLine(
					g,
					( float ) gPos1[ 0 ],
					( float ) gPos1[ 1 ],
					( float ) gPos2[ 0 ],
					( float ) gPos2[ 1 ],
					edge.getA().getR(),
					edge.getB().getR() ); // TODO needs to move from pixel coords to BDV coords
		}
	}

	protected void drawPeripheralLine( Graphics2D g, float x1, float y1, float x2, float y2, float r1, float r2 ) { // TODO needs to take r2 into account!!!!

		float common_factor2 = r1 / ( float ) Math.sqrt( 1 + ( ( y2 - y1 ) * ( y2 - y1 ) ) / ( ( x2 - x1 ) * ( x2 - x1 ) ) );

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

}