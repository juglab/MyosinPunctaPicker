package de.csbd.learnathon.command;

import java.awt.Color;
import java.awt.Graphics2D;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class GhostOverlay extends BdvOverlay {

	private double x, y;
	private boolean visible;
	private PunctaPickerView view;
	private Color defaultColor = new Color( 255, 0, 0 );

	public GhostOverlay( PunctaPickerView view ) {
		this.view = view;
	}

	public void setPosition( final double x, final double y ) {
		this.x = x;
		this.y = y;
	}

	public void setVisible( final boolean visible ) {
		this.visible = visible;
	}

	public void requestRepaint() {
		view.getBdv().getViewerPanel().requestRepaint();
	}

	@Override
	protected void draw( final Graphics2D g ) {
		final AffineTransform3D t = new AffineTransform3D();
		getCurrentTransform3D( t );

		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];

		final int curentTime = info.getTimePointIndex();

		RealPoint planarPoint = new RealPoint( x, y, 0 );
		planarPoint.localize( lPos );
		t.apply( lPos, gPos );
//		System.out.println( gPos[ 0 ] );

		if ( visible ) {
			g.setColor( defaultColor );
			g.drawOval(
					( int ) ( x - 10 ),
					( int ) ( y - 10 ),
					( 20 ),
					( 20 ) );
		}

	}


}
