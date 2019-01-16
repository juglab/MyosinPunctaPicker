package de.csbd.learnathon.command;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
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

	public void mainOverlay() {
		final ArrayList< RealPoint > points = new ArrayList<>();
		List< Punctas > allPuncta = model.getPuncta();
		for ( int i = 0; i < allPuncta.size(); i++ ) {
			Punctas currentPuncta = allPuncta.get( i );
			points.add( new RealPoint( currentPuncta.getX(), currentPuncta.getY() ) );
		}

		final BdvOverlay overlay = new BdvOverlay() {

			@Override
			protected void draw( final Graphics2D g ) {

				final AffineTransform2D t = new AffineTransform2D();
				getCurrentTransform2D( t );
				g.setColor( Color.RED );

				final double[] lPos = new double[ 2 ];
				final double[] gPos = new double[ 2 ];
				final int start = 0;
				final int end = points.size();
				for ( int i = start; i < end; i++ ) {
					int timePt = info.getTimePointIndex();
					if ( allPuncta.get( i ).getT() == timePt ) {
						points.get( i ).localize( lPos );
						t.apply( lPos, gPos );
						g.drawOval( ( int ) gPos[ 0 ], ( int ) gPos[ 1 ], 3, 3 );

					}

				}

			}
		};
		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );

	}


}