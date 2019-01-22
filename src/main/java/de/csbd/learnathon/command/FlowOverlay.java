package de.csbd.learnathon.command;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;

public class FlowOverlay{
	
	private Img< FloatType > flowData;
	private BdvHandlePanel bdv;
	
	public FlowOverlay( BdvHandlePanel bdv ) {
		this.bdv = bdv;
	}
	
	public void setData(Img< FloatType > f)
	{
		 this.flowData=f;
	}
	

	public void paint() {

		final BdvOverlay overlay = new BdvOverlay() {
	
			@Override
			protected void draw( final Graphics2D g ) {
				
				final long sizeX = flowData.dimension( 0 );
				final long sizeY = flowData.dimension( 1 );
				
				final int t = 0;
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
		};
		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );
		
	}
		
		
		private void drawVector( AffineTransform2D trans,final Graphics2D g, final int t, int x, int y ) {
			
			
			
			final ValuePair< Float, Float > flowVec = getFlowVector(flowData,x,y,t);
			
			
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

		private ValuePair<Float, Float> getFlowVector(Img<FloatType> f, int x, int y, int t) {
			
    		f.randomAccess().setPosition( x, 0 );
    		f.randomAccess().setPosition( y, 1 );
		f.randomAccess().setPosition( 2 * t, 2 );
    		Float u = f.randomAccess().get().getRealFloat();
    			
    		f.randomAccess().setPosition( x, 0 );
    		f.randomAccess().setPosition( y, 1 );
		f.randomAccess().setPosition( 2 * t, 2 );
    		Float v = f.randomAccess().get().getRealFloat();
    
    		ValuePair< Float, Float > flowVector = new ValuePair< Float, Float >( u, v );
    	
		return flowVector;
		}
		
}
