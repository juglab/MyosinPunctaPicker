package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class PPGraphUtils {

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
}
