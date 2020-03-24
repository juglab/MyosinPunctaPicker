package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class PPGraphUtils {

	///Check for coordinate system consistency 
	public static List< Double > getDistanceSqauredToPuncta( float x, float y, List< Puncta > ps ) {
		ArrayList< Double > ret = new ArrayList<>();
		for ( Puncta p : ps ) {
			ret.add( Math.pow( ( p.getX() - x ), 2 ) + Math.pow( ( p.getY() - y ), 2 ) );
		}
		return ret;
	}

	/**
	 * Computes and returns the closes of the given punktas to the given
	 * coordinates (x,y).
	 * 
	 * @param x
	 *            x-coordinate
	 * @param y
	 *            y-coordinate
	 * @param ps
	 *            List of puncta to find the closes from
	 * @return Returns a Pair, consisting of the closes found puncta and its
	 *         squared distance.
	 */
	public static Pair< Puncta, Double > getClosestPuncta( float x, float y, List< Puncta > ps ) {
		List< Double > ds = getDistanceSqauredToPuncta( x, y, ps );
		int minDsId = ds.indexOf( Collections.min( ds ) );
		if ( minDsId > -1 ) { return new ValuePair<>( ps.get( minDsId ), ds.get( minDsId ) ); }
		return null;
	}

	/**
	 * This method returns true if the collection is null or is empty.
	 * 
	 * @param collection
	 * @return true | false
	 */
	public static boolean isEmpty( Collection< ? > collection ) {
		if ( collection == null || collection.isEmpty() ) { return true; }
		return false;
	}

	/**
	 * This method returns true if the objet is null.
	 * 
	 * @param object
	 * @return true | false
	 */
	public static boolean isEmpty( Object object ) {
		if ( object == null ) { return true; }
		return false;
	}

	/**
	 * This method returns true if the input array is null or its length is
	 * zero.
	 * 
	 * @param array
	 * @return true | false
	 */
	public static boolean isEmpty( Object[] array ) {
		if ( array == null || array.length == 0 ) { return true; }
		return false;
	}

	public static Edge pointOnEdge( float x, float y, List< Edge > edges ) {
		for ( Edge e : edges ) {
			if ( !isInBoundingBox( e, x, y ) ) {
				continue;
			}
			double num = Math.abs(
					( e.getA().getY() - e.getB().getY() ) * x - ( e.getA().getX() - e.getB().getX() ) * y + e.getA().getX() * e.getB().getY() - e.getA().getY() * e.getB().getX() );
			double den = Math.sqrt(
					( e.getA().getY() - e.getB().getY() ) * ( e.getA().getY() - e.getB().getY() ) + ( e.getA().getX() - e.getB().getX() ) * ( e.getA().getX() - e.getB().getX() ) );
			double dist = num / den;
			if ( dist < 2 ) { //If mouse hover is lesser than 2 pixels away from the edge
				return e;
			}
		}
		return null;
	}

	private static boolean isInBoundingBox( Edge e, float x, float y ) {
		float minX = Math.min( e.getA().getX(), e.getB().getX() );
		float maxX = Math.max( e.getA().getX(), e.getB().getX() );
		float minY = Math.min( e.getA().getY(), e.getB().getY() );
		float maxY = Math.max( e.getA().getY(), e.getB().getY() );
		return ( minX <= x && minY <= y ) && ( maxX >= x && maxY >= y );
	}

	public static FlowVector getClosestFlowVectorAtTimePoint(
			float x,
			float y,
			List< FlowVector > availableFlowVectors,
			int time ) {

		for ( FlowVector flowVector : availableFlowVectors ) {
			Edge virtualEdge =
					new Edge( new Puncta( flowVector.getX(), flowVector.getY(), flowVector.getT(), 1 ), new Puncta( ( float ) ( flowVector.getX() + flowVector.getU() ), ( float ) ( flowVector.getY() + flowVector.getV() ), flowVector.getT() + 1, 1 ) );
			if ( !isInBoundingBox( virtualEdge, x, y ) ) {
				continue;
			}
			if ( flowVector.getT() == time ) {
				double num = Math.abs(
						( flowVector.getY() - ( flowVector.getY() + flowVector.getV() ) ) * x - ( flowVector.getX() - ( flowVector.getX() + flowVector.getU() ) ) * y + flowVector.getX() * ( flowVector.getY() + flowVector.getV() ) - flowVector.getY() * ( flowVector.getX() + flowVector.getU() ) );

				double den = Math.sqrt(
						( flowVector.getY() - ( flowVector.getY() + flowVector.getV() ) ) * ( flowVector.getY() - ( flowVector.getY() + flowVector.getV() ) ) + ( flowVector.getX() - ( flowVector.getX() + flowVector.getU() ) ) * ( flowVector.getX() - ( flowVector.getX() + flowVector.getU() ) ) );
				double dist = num / den;
				if ( dist < 4 ) { //If mouse hover is lesser than 4 pixels away from the edge
					return flowVector;
				}
			}
		}

		return null;
	}
}
