

package de.csbd.learnathon.command;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;

import org.scijava.command.CommandService;
import org.scijava.thread.ThreadService;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;

public class Overlay extends BdvOverlay {
	
	private PunctaPickerModel model;
	private CommandService cs;
	private ThreadService ts;

	public int lineThickness = 2;
	private final Color defaultColor = new Color( 0, 1, 1 );
	private final Color selectedColor = new Color( 1, 0, 0 );
	private final Color selectedPunctaColor = new Color( 1, ( float ) 0.6, 0 );
	private float fadeOutAlpha;
	private boolean visible = true;

	public Overlay( PunctaPickerModel model ) {
		super();
		this.model = model;
		model.getView().getBdv().getViewerPanel().getDisplay().addHandler( new MouseOver() );
	}

	@Override
	protected void draw( final Graphics2D g ) {

		if ( visible ) {
			final AffineTransform3D t = new AffineTransform3D();
			getCurrentTransform3D( t );
			double scale = extractScale( t, 0 );

			final double[] lPos = new double[ 3 ];
			final double[] gPos = new double[ 3 ];
			final double[] lPos1 = new double[ 3 ];
			final double[] lPos2 = new double[ 3 ];
			final double[] gPos1 = new double[ 3 ];
			final double[] gPos2 = new double[ 3 ];

			fadeOutAlpha = ( float ) ( model.getView().getFadeOutValue() ) / 15;
			final int curentTime = info.getTimePointIndex();

			if ( model.getView().getActiveTrackletCheckBoxStatus() ) {
				Pair< LinkedList< Puncta >, LinkedList< Edge > > selectedTracklet = model.getGraph().getSelectedTracklet();
				for ( Puncta p : selectedTracklet.getA() ) {
					punctaOverlay( g, t, scale, lPos, gPos, curentTime, p );
				}
				for ( Edge edge : selectedTracklet.getB() ) {
					edgeOverlay( g, t, lPos1, lPos2, gPos1, gPos2, curentTime, edge );
				}
			}

			else {

				for ( Puncta p : model.getGraph().getPunctas() ) {
					punctaOverlay( g, t, scale, lPos, gPos, curentTime, p );
				}

				for ( Edge edge : model.getGraph().getEdges() ) {
					edgeOverlay( g, t, lPos1, lPos2, gPos1, gPos2, curentTime, edge );
				}
			}
		}

	}

	private void edgeOverlay(
			final Graphics2D g,
			final AffineTransform3D t,
			final double[] lPos1,
			final double[] lPos2,
			final double[] gPos1,
			final double[] gPos2,
			final int curentTime,
			Edge edge ) {
		float transparency;
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
		if ( edge.equals( model.getGraph().getMouseSelectedEdge() ) ) {
			g.setStroke( new BasicStroke( 2 * lineThickness ) );
		}

		float d1 = Math.abs( ( float ) curentTime - edge.getA().getT() );
		float d2 = Math.abs( ( float ) curentTime - edge.getB().getT() );
		transparency = Math.min( d1, d2 );
		transparency = ( float ) Math.exp( -transparency * fadeOutAlpha );

		if ( edge.isSelected() ) {
			g.setColor( new Color( selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue(), transparency ) );
		} else {
			g.setColor( new Color( defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), transparency ) );
		}
		g.drawLine(
				( int ) gPos1[ 0 ],
				( int ) gPos1[ 1 ],
				( int ) gPos2[ 0 ],
				( int ) gPos2[ 1 ] );
	}

	private void punctaOverlay(
			final Graphics2D g,
			final AffineTransform3D t,
			double scale,
			final double[] lPos,
			final double[] gPos,
			final int curentTime,
			Puncta p ) {
		float transparency;
		if ( p.getT() <= info.getTimePointIndex() ) {
			g.setStroke( new BasicStroke( lineThickness ) );
		} else {
			g.setStroke( new BasicStroke( lineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0 ) );
		}
		if ( p.equals( model.getGraph().getLeadSelectedPuncta() ) ) {
			g.setStroke( new BasicStroke( 2 * lineThickness ) );
		}
		RealPoint planarPoint = new RealPoint( p.getX(), p.getY(), 0 );
		planarPoint.localize( lPos );
		t.apply( lPos, gPos );

		transparency = Math.abs( curentTime - ( float ) p.getT() );
		transparency = ( float ) Math.exp( -transparency * fadeOutAlpha );


		if ( p.isSelected() ) {
			g.setColor( new Color( selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue(), transparency ) );
		} else {
			g.setColor( new Color( defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), transparency ) );
		}

		if ( p.getT() == curentTime ) {
			if ( p.equals( model.getGraph().getLeadSelectedPuncta() ) ) {
				g.setColor( selectedPunctaColor );
			}
			g.setStroke( new BasicStroke( lineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 2 }, 0 ) );
			g.drawOval(
					( int ) ( gPos[ 0 ] - ( p.getR() * scale ) - 4 ),
					( int ) ( gPos[ 1 ] - ( p.getR() * scale ) - 4 ),
					( int ) ( p.getR() * scale * 2 + 8 ),
					( int ) ( p.getR() * scale * 2 + 8 ) );
			g.setStroke( new BasicStroke( lineThickness ) );
			g.drawOval(
					( int ) ( gPos[ 0 ] - ( p.getR() * scale ) ),
					( int ) ( gPos[ 1 ] - ( p.getR() * scale ) ),
					( int ) ( p.getR() * scale * 2 ),
					( int ) ( p.getR() * scale * 2 ) );
			int s = 4;
			g.drawLine( ( int ) gPos[ 0 ] - s, ( int ) gPos[ 1 ], ( int ) gPos[ 0 ] + s, ( int ) gPos[ 1 ] );
			g.drawLine( ( int ) gPos[ 0 ], ( int ) gPos[ 1 ] - s, ( int ) gPos[ 0 ], ( int ) gPos[ 1 ] + s );
		} else {
			if ( p.equals( model.getGraph().getLeadSelectedPuncta() ) ) {
				g.setColor( selectedPunctaColor );
			}
			int s = 4;
			g.drawLine( ( int ) gPos[ 0 ] - s, ( int ) gPos[ 1 ], ( int ) gPos[ 0 ] + s, ( int ) gPos[ 1 ] );
			g.drawLine( ( int ) gPos[ 0 ], ( int ) gPos[ 1 ] - s, ( int ) gPos[ 0 ], ( int ) gPos[ 1 ] + s );
		}
		if ( ( p.getT() == curentTime - 1 ) && model.getGraph().punctaInSelectedTracklet( p ) && model.getView().getPreviousMarkerCheckBoxStatus() ) {
			g.setStroke( new BasicStroke( lineThickness ) );
			g.drawOval(
					( int ) ( gPos[ 0 ] - ( p.getR() * scale ) ),
					( int ) ( gPos[ 1 ] - ( p.getR() * scale ) ),
					( int ) ( p.getR() * scale * 2 ),
					( int ) ( p.getR() * scale * 2 ) );
			int s = 4;
			g.drawLine( ( int ) gPos[ 0 ] - s, ( int ) gPos[ 1 ], ( int ) gPos[ 0 ] + s, ( int ) gPos[ 1 ] );
			g.drawLine( ( int ) gPos[ 0 ], ( int ) gPos[ 1 ] - s, ( int ) gPos[ 0 ], ( int ) gPos[ 1 ] + s );
		}
	}

	public static double extractScale( final AffineTransform3D t, final int axis ) {
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
			mouseInsidePuncta();
			mouseOnEdge();
		}

	}

	private void mouseInsidePuncta() {
		final RealPoint pos = new RealPoint(3);
		model.getView().getBdv().getViewerPanel().getGlobalMouseCoordinates( pos );
		if ( !model.getGraph().getPunctas().isEmpty() ) {
			Pair< Puncta, Double > closest =
					PPGraphUtils.getClosestPuncta( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), model.getGraph().getPunctas() );
			if ( closest.getB() <= closest.getA().getR() ) {
				model.getGraph().setMouseSelectedPuncta( closest.getA() );
			} else {
				if ( model.getGraph().getLeadSelectedPuncta() != null ) {
					if ( closest.getA().getT() > model.getGraph().getLeadSelectedPuncta().getT() ) {
						model.getGraph().setMouseSelectedPuncta( closest.getA() );
					}
				}

			}
		}
		
	}

	private void mouseOnEdge() {
		final RealPoint pos = new RealPoint( 3 );
		model.getView().getBdv().getViewerPanel().getGlobalMouseCoordinates( pos );
		if ( !model.getGraph().getEdges().isEmpty() ) {

			Edge selectedEdge =
					PPGraphUtils.pointOnEdge( pos.getFloatPosition( 0 ), pos.getFloatPosition( 1 ), model.getGraph().getEdges() );
			if ( !(selectedEdge == null) ) {
				model.getGraph().setMouseSelectedEdge( selectedEdge );
			}
			else {
				model.getGraph().setMouseSelectedEdge( null );
			}
		}

	}

	public void setVisible( final boolean visible ) {
		this.visible = visible;
	}

}