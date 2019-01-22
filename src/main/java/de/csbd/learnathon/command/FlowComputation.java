package de.csbd.learnathon.command;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;


public class FlowComputation {

	public static Img< FloatType > getConstantFlow( RandomAccessibleInterval< DoubleType > image ) {

		final ImgFactory< FloatType > imgFactory = new CellImgFactory<>( new FloatType(), 5 );
		final Img< FloatType > img1 = imgFactory.create( image.dimension( 0 ), image.dimension( 1 ), 8 * 2 - 2 );

		Cursor< FloatType > cursorInput = img1.cursor();

		while ( cursorInput.hasNext() ) {
			cursorInput.fwd();
			cursorInput.get().set( 5f );
		}

		return img1;

	}

}
