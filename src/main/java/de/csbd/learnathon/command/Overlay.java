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

	public Overlay( BdvHandlePanel bdv, PunctaPickerModel model ) {
		Overlay.bdv = bdv;
		this.model = model;
	}

	public void paintPoint() {
		List< Punctas > allPuncta = model.getPuncta();

		final BdvOverlay overlay = new BdvOverlay() {

			@Override
			protected void draw( final Graphics2D g ) {

				System.out.println( "Method1" );

				final AffineTransform2D t = new AffineTransform2D();
				getCurrentTransform2D( t );
				g.setColor( Color.RED );


				final double[] lPos = new double[ 2 ];
				final double[] gPos = new double[ 2 ];
				final double[] lPos1 = new double[ 2 ];
				final double[] lPos2 = new double[ 2 ];
				final double[] gPos1 = new double[ 2 ];
				final double[] gPos2 = new double[ 2 ];

				final int start = 0;
				final int end = allPuncta.size();

				for ( int i = start; i < end; i++ ) {
					if ( allPuncta.get( i ).getT() <= info.getTimePointIndex() ) {
						int thickness = 5 - ( info.getTimePointIndex() - allPuncta.get( i ).getT() );
						if ( thickness <= 0 ) {
							thickness = 0;
						}
						g.setStroke( new BasicStroke( thickness ) );
						RealPoint planarPoint = new RealPoint( allPuncta.get( i ).getX(), allPuncta.get( i ).getY() );
						planarPoint.localize( lPos );
						t.apply( lPos, gPos );
						g.drawOval( ( int ) gPos[ 0 ], ( int ) gPos[ 1 ], 3, 3 );
						if ( i > 0 ) {
							RealPoint point1 = new RealPoint( allPuncta.get( i - 1 ).getX(), allPuncta.get( i - 1 ).getY() );
							point1.localize( lPos1 );
							t.apply( lPos1, gPos1 );

							RealPoint point2 = new RealPoint( allPuncta.get( i ).getX(), allPuncta.get( i ).getY() );
							point2.localize( lPos2 );
							t.apply( lPos2, gPos2 );
							g.drawLine(
									( int ) gPos1[ 0 ],
									( int ) gPos1[ 1 ],
									( int ) gPos2[ 0 ],
									( int ) gPos2[ 1 ] );
						}
//						g.drawOval( ( int ) gPos[ 0 ], ( int ) gPos[ 1 ], 3, 3 );

					}


				}
			}
		};
		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );

	}




}