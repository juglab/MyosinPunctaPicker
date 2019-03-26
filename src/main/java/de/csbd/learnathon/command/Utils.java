package de.csbd.learnathon.command;

import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;

import net.imglib2.realtransform.AffineTransform3D;

public class Utils {

	public static String getSelectedButtonText( ButtonGroup buttonGroup ) {
		for ( Enumeration< AbstractButton > buttons = buttonGroup.getElements(); buttons.hasMoreElements(); ) {
			AbstractButton button = buttons.nextElement();

			if ( button.isSelected() ) { return button.getText(); }
		}

		return null;
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

}
