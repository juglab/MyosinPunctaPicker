package de.csbd.learnathon.command;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class GhostOverlay extends BdvOverlay {

	private double x, y, r;
	private boolean visible;
	private PunctaPickerView view;
	private Color defaultColor = new Color( 255, 0, 0 );

	public GhostOverlay( PunctaPickerView view ) {
		this.view = view;
		view.getBdv().getViewerPanel().getDisplay().addHandler( new MouseOver() );
	}

	public void setPosition( final double x, final double y ) {

		this.x = x;
		this.y = y;
	}

	public void setRadius( final float r ) {
		this.r = r;

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
		double scale = extractScale( t, 0 );

		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		RealPoint planarPoint = new RealPoint( x, y, 0 );
		planarPoint.localize( lPos );
		t.apply( lPos, gPos );
		
		if ( visible ) {
			g.setColor( defaultColor );
			g.drawOval(
					( int ) ( gPos[ 0 ] - ( r * scale ) ),
					( int ) ( gPos[ 1 ] - ( r * scale ) ),
					( int ) ( r * scale * 2 ),
					( int ) ( r * scale * 2 ) );
		}

	}

	public static double extractScale( final AffineTransform3D t, final int axis ) { //TODO Move this method to Utils
		double sqSum = 0;
		final int c = axis;
		for ( int r = 0; r < 4; ++r ) {
			final double x = t.get( r, c );
			sqSum += x * x;
		}
		return Math.sqrt( sqSum );
	}

	private class MouseOver implements MouseMotionListener {

		@Override
		public void mouseDragged( MouseEvent e ) {}

		@Override
		public void mouseMoved( MouseEvent e ) {
			if ( visible ) {
				overlayBlobDetectionResult();
				requestRepaint();
			}
		}

	}

	public void overlayBlobDetectionResult() {
		int time = view.getBdv().getBdvHandle().getViewerPanel().getState().getCurrentTimepoint();
		final RealPoint posn = new RealPoint( 3 );
		view.getBdv().getViewerPanel().getGlobalMouseCoordinates( posn );
		System.out.println( view.getDetectionMode() );

		if ( view.getDetectionMode() == "constant radius" ) {
			setPosition( posn.getDoublePosition( 0 ), posn.getDoublePosition( 1 ) );
			setRadius( view.getPunctaPickerModel().getDefaultRadius() );
		} else if ( view.getDetectionMode() == "auto size&pos" ) {
			Puncta ghostPuncta =
					view.getPunctaPickerController().blobDetectedPuncta( time, posn.getDoublePosition( 0 ), posn.getDoublePosition( 1 ) );
			double posx = ( posn.getDoublePosition( 0 ) - view.getPunctaPickerController().autoOrManualPatchSize / 2 ) + ghostPuncta.getX();
			double posy = ( posn.getDoublePosition( 1 ) - view.getPunctaPickerController().autoOrManualPatchSize / 2 ) + ghostPuncta.getY();
			setPosition( posx, posy );
			setRadius( ghostPuncta.getR() );
		} else {
			Puncta ghostPuncta =
					view.getPunctaPickerController().blobDetectedPuncta( time, posn.getDoublePosition( 0 ), posn.getDoublePosition( 1 ) );
			double posx = posn.getDoublePosition( 0 );
			double posy = posn.getDoublePosition( 1 );
			setPosition( posx, posy );
			setRadius( ghostPuncta.getR() );
		}
	}

}
