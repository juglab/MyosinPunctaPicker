package de.csbd.learnathon.command;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imreadmulti;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Range;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_video.DenseOpticalFlow;
import org.bytedeco.opencv.opencv_video.FarnebackOpticalFlow;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;

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
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
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
			IJ.save( ImgToVirtualStack.wrap( toImgPlus( ijSlice ) ), "/Users/turek/Desktop/test/ijslice"+i+".tif" );
			Mat slice = ImgToMatConverter.matByte( ijSlice );
			opencv_imgcodecs.imwrite("/Users/turek/Desktop/test/slice" + i + ".tif", slice);
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
		List< Mat> flowsx = new ArrayList< Mat>();
		List< Mat> flowsy = new ArrayList< Mat>();
		for ( int i = 0; i < flows.size(); i++ ) {
			MatVector splitflows = new MatVector();
			opencv_core.split( flows.get(i), splitflows );
			flowsx.add( splitflows.get( 0 ) );
			opencv_imgcodecs.imwrite("/Users/turek/Desktop/test/flowx" + i + ".tif",splitflows.get( 0 ) );
			//System.out.println( "X Type " + CvType.typeToString( splitflows.get( 0 ).type()) );
			flowsy.add( splitflows.get( 1 ) );
			opencv_imgcodecs.imwrite("/Users/turek/Desktop/test/flowy" + i + ".tif",splitflows.get( 1 ) );
			//System.out.println( "Y Type " + CvType.typeToString( splitflows.get( 1 ).type()) );
		}
		assertEquals( 59, flowsx.size() );
		assertEquals( 59, flowsy.size() );
		
		List< RandomAccessibleInterval< FloatType > > xijFlows = new ArrayList< RandomAccessibleInterval< FloatType > >( 59 );
		List< RandomAccessibleInterval< FloatType > > yijFlows = new ArrayList< RandomAccessibleInterval< FloatType > >( 59 );
		for ( int i = 0; i < 59; i++ ) {
			RandomAccessibleInterval< FloatType > xijFlow = MatToImgConverter.imgFloat( flowsx.get( i ) );
			xijFlows.add( xijFlow );
			RandomAccessibleInterval< FloatType > yijFlow = MatToImgConverter.imgFloat( flowsy.get( i ) );
			yijFlows.add( yijFlow );
		}
		RandomAccessibleInterval< FloatType > xijFlowFinal = Views.stack( xijFlows );
		IJ.save( ImgToVirtualStack.wrap( toImgPlus( xijFlowFinal ) ), "/Users/turek/Desktop/test/JavaOutputX2.tif" );
		RandomAccessibleInterval< FloatType > yijFlowFinal = Views.stack( yijFlows );
		IJ.save( ImgToVirtualStack.wrap( toImgPlus( yijFlowFinal ) ), "/Users/turek/Desktop/test/JavaOutputY2.tif" );
	
	}

	@Ignore
	public void computeFernbackOpticalFlowTestIJ2() throws IOException {

		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		int[] dims = ImgToMatConverter.getImageShape( image );
		for ( int i = 0; i < dims.length; i++ ) {
			System.out.println( "dim" + i + "=" + dims[ i ] );
		}

		Mat mat = ImgToMatConverter.matByte( image );
		System.out.println( "Array Width " + mat.arrayWidth() );
		System.out.println( "Array Height " + mat.arrayHeight() );
		System.out.println( "Array Depth " + mat.arrayDepth() );
		System.out.println( "Array Step " + mat.arrayStep() );
		System.out.println( "Array Size " + mat.arraySize() );
		System.out.println( "Channels " + mat.channels() );
		System.out.println( "Type " + CvType.typeToString( mat.type()) );

		long[] shape = ImgToMatConverter.getMatShape( mat );
		for ( int i = 0; i < shape.length; i++ ) {
			System.out.println( "dim" + i + "=" + shape[ i ] );
		}

		final DenseOpticalFlow opticalFlow = FarnebackOpticalFlow.create( numLevels, pyrScale, fastPyramids, winSize, numIters, polyN, polySigma, flags );
		List< Mat > flows = new ArrayList< Mat >( dims[ 2 ] / 2 - 1 );
		Mat flow = null;
		for ( int i = 1; i < dims[ 2 ]; i++ ) {
			Range range = new Range( 3 );
			range.position( 0 ).put( Range.all() );
			range.position( 1 ).put( Range.all() );
			range.position( 2 ).put( new Range( i - 1, i ) );
			Mat slice1 = mat.apply( range.position( 0 ) ).clone();
			range.close();
			range = new Range( 3 );
			range.position( 0 ).put( Range.all() );
			range.position( 1 ).put( Range.all() );
			range.position( 2 ).put( new Range( i, i + 1 ) );
			Mat slice2 = mat.apply( range.position( 0 ) ).clone();
			range.close();
			flow = new Mat();
			opticalFlow.calc( slice1, slice2, flow );
			assertEquals( 587, flow.arrayWidth() );
			assertEquals( 871, flow.arrayHeight() );
			assertEquals( 32, flow.arrayDepth() );
			flows.add( flow );
		}
		assertEquals( 59, flows.size() );

		List< RandomAccessibleInterval< DoubleType > > ijFlows = new ArrayList< RandomAccessibleInterval< DoubleType > >( 59 );
		
		for ( int i = 0; i < 59; i++ ) {
			RandomAccessibleInterval< DoubleType > ijFlow = MatToImgConverter.imgDouble( flows.get( i ) );
			ijFlows.add( ijFlow );
		}
		RandomAccessibleInterval< DoubleType > ijFlowFinal = Views.stack( ijFlows );
		IJ.save( ImgToVirtualStack.wrap( toImgPlus( ijFlowFinal ) ), "/Users/turek/Desktop/JavaOutput.tif" );
	}

	@Ignore
	public void computeFernbackOpticalFlowTestIJ() throws IOException {
		/*
		 * 1) Compute reading from file.
		 */
		MatVector mats = new MatVector();
		imreadmulti( input, mats);
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );

		assertEquals( 60, mats.size() );

		for ( int i = 0; i < mats.size(); i++ ) {
			assertEquals( 587, mats.get( i ).arrayWidth() );
			assertEquals( 871, mats.get( i ).arrayHeight() );
			assertEquals( 8, mats.get( i ).arrayDepth() );
		}

		final DenseOpticalFlow opticalFlow = FarnebackOpticalFlow.create( numLevels, pyrScale, fastPyramids, winSize, numIters, polyN, polySigma, flags );
		List< Mat > flows = new ArrayList< Mat >();
		for ( int i = 1; i < mats.size(); i++ ) {
			Mat flow = new Mat();
			opticalFlow.calc( mats.get( i - 1 ), mats.get( i ), flow );
			assertEquals( 587, flow.arrayWidth() );
			assertEquals( 871, flow.arrayHeight() );
			assertEquals( 32, flow.arrayDepth() );
			flows.add( flow );
		}

		assertEquals( 59, flows.size() );
		
		
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
