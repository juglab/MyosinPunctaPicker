package de.csbd.learnathon.command;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;

public class FlowOverlay{
	
	private RandomAccessibleInterval< DoubleType > flowData;
	private BdvHandlePanel bdv;
	
	public FlowOverlay( BdvHandlePanel bdv ) {
		this.bdv = bdv;
	}
	
	public void setData(RandomAccessibleInterval< DoubleType > f)
	{
		 this.flowData=f;
	}
	

	public void paint() {

		final BdvOverlay overlay = new BdvOverlay() {
	
			@Override
			protected void draw( final Graphics2D g ) {
				
				final long sizeX = flowData.dimension( 0 );
				final long sizeY = flowData.dimension( 1 );
				
				int t = info.getTimePointIndex();
				
				if (t<flowData.dimension(2)/2)
				{
					int spacing = 20; // at most all 10 pixels
					
					
					final AffineTransform2D trans = new AffineTransform2D();
					getCurrentTransform2D( trans );
					
					
					
					
					int startx = ( int ) ( sizeX % spacing ) / 2;
					startx = ( startx == 0 ) ? spacing / 2 : startx;
					int starty = ( int ) ( sizeY % spacing ) / 2;
					starty = ( starty == 0 ) ? spacing / 2 : starty;
			
					for ( int x = startx; x < sizeX; x += spacing ) {
						for ( int y = starty; y < sizeY; y += spacing ) {
							
							
							drawVector( trans ,g, t, x, y );
						}
					}
				}
			}
		};
		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );
		
	}
		
		
		private void drawVector( AffineTransform2D trans,final Graphics2D g, final int t, int x, int y ) {
			
			
			
			final ValuePair<Double, Double> flowVec = getFlowVector(flowData,x,y,t);
			
			
			if ( x == 0 && y == 0 ) return;
	
	
			g.setColor( Color.YELLOW );
	
			final Graphics2D g2 = g;
			g2.setStroke( new BasicStroke( 1 ) );
	
			int xto = ( int ) ( x + flowVec.getA() );
			int yto = ( int ) ( y + flowVec.getB() );
	
			final double[] from = new double[]{x,y};
			final double[] to = new double[] { xto, yto };
			trans.apply( from, from );
			trans.apply( to, to );
	
			x = (int)from[0];
			y = (int)from[1];
			xto = (int)to[0];
			yto = (int)to[1];
	
			g.drawLine( x, y, xto, yto );
	
			g.setColor( Color.PINK );
			g.drawRect( x - 1, y - 1, 2, 2 );

	

		}

		private ValuePair<Double, Double> getFlowVector(RandomAccessibleInterval<DoubleType> f, int x, int y, int t) {
			RandomAccess<DoubleType> ra = f.randomAccess();
    		
			ra.setPosition( x, 0 );
    		ra.setPosition( y, 1 );
    		ra.setPosition( 2 * t, 2 );
    		//System.out.println(2 * t);
    		
    		Double u = ra.get().getRealDouble();
    		
    		
    		ra.setPosition( x, 0 );
    		ra.setPosition( y, 1 );
    		ra.setPosition( 2 * t+1, 2 );
    		Double v = ra.get().getRealDouble();

			
    		
		    ValuePair< Double, Double > flowVector = new ValuePair< Double, Double >( u, v );
		    return flowVector;
		}
		
}
