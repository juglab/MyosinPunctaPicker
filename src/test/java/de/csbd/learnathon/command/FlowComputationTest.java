package de.csbd.learnathon.command;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_video.DenseOpticalFlow;
import org.bytedeco.opencv.opencv_video.FarnebackOpticalFlow;
import org.junit.jupiter.api.Test;

import ij.IJ;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.Views;

public class FlowComputationTest {

	private final static int numLevels = 1;
	private final static double pyrScale = 0.5;
	private final boolean fastPyramids = false;
	private final static int winSize = 5;
	private final static int numIters = 2;
	private final static int polyN = 5;
	private final static double polySigma = 1.1;
	private final static int flags = 0;
	private final static String input = "/Users/turek/Downloads/MAX_2018001_LP823_Control-03-02_Myosin_denoised.tif";

	@Test
	public void computeFernbackOpticalFlowTestIJ2_2() throws IOException {

		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		int[] dims = ImgToMatConverter.getImageShape( image );

		List< Mat > dataSlices = new ArrayList< Mat >( dims[ 2 ] );
		for ( int i = 0; i < dims[ 2 ]; i++ ) {
			RandomAccessibleInterval< ByteType > ijSlice = Views.hyperSlice( image, 2, i );
			IJ.save( ImgToVirtualStack.wrap( toImgPlus( ijSlice ) ), "/Users/turek/Desktop/test/ijslice" + i + ".tif" );
			Mat slice = ImgToMatConverter.matByte( ijSlice );
			dataSlices.add( slice );
		}

		final DenseOpticalFlow opticalFlow = FarnebackOpticalFlow.create( numLevels, pyrScale, fastPyramids, winSize, numIters, polyN, polySigma, flags );
		List< Mat > flows = new ArrayList< Mat >( dims[ 2 ] - 1 );
		Mat flow = null;
		for ( int i = 1; i < dims[ 2 ]; i++ ) {
			flow = new Mat();
			//Note!!!! calc only takes 8 bit images!
			opticalFlow.calc( dataSlices.get( i - 1 ), dataSlices.get( i ), flow );
			flows.add( flow );
		}
		assertEquals( 59, flows.size() );

		List< RandomAccessibleInterval< ByteType > > flowsx = new ArrayList< RandomAccessibleInterval< ByteType > >();
		List< RandomAccessibleInterval< ByteType > > flowsy = new ArrayList< RandomAccessibleInterval< ByteType > >();
		for ( int i = 0; i < flows.size(); i++ ) {
			MatVector splitflows = new MatVector();
			opencv_core.split( flows.get( i ), splitflows );
			flowsx.add( MatToImgConverter.imgByte( splitflows.get( 0 ) ) );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/flowx" + i + ".tif", splitflows.get( 0 ) );
			//System.out.println( "X Type " + CvType.typeToString( splitflows.get( 0 ).type()) );
			flowsy.add( MatToImgConverter.imgByte( splitflows.get( 1 ) ) );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/flowy" + i + ".tif", splitflows.get( 1 ) );
			//System.out.println( "Y Type " + CvType.typeToString( splitflows.get( 1 ).type()) );
		}
		assertEquals( 59, flowsx.size() );
		assertEquals( 59, flowsy.size() );

		RandomAccessibleInterval< ByteType > xijFlowFinal = Views.stack( flowsx );
		IJ.save( ImgToVirtualStack.wrap( toImgPlus( xijFlowFinal ) ), "/Users/turek/Desktop/test/JavaOutputX2.tif" );
		RandomAccessibleInterval< ByteType > yijFlowFinal = Views.stack( flowsy );
		IJ.save( ImgToVirtualStack.wrap( toImgPlus( yijFlowFinal ) ), "/Users/turek/Desktop/test/JavaOutputY2.tif" );

	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private static ImgPlus< ? > toImgPlus( RandomAccessibleInterval< ? > image ) {
		if ( image instanceof ImgPlus )
			return ( ImgPlus< ? > ) image;
		if ( image instanceof Img )
			return new ImgPlus<>( ( Img< ? > ) image );
		return new ImgPlus<>( ImgView.wrap( ( RandomAccessibleInterval ) image, null ) );
	}
}
