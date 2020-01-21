package de.csbd.learnathon.command;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imreadmulti;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_video.DenseOpticalFlow;
import org.bytedeco.opencv.opencv_video.FarnebackOpticalFlow;
import org.junit.Test;

import io.scif.SCIFIO;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.opencv.ImgToMatConverter;
import net.imagej.opencv.MatToImgConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class RealWorldImageTest {
	
	private final static int numLevels = 1;
	private final static double pyrScale = 0.5;
	private final boolean fastPyramids = false;
	private final static int winSize = 5;
	private final static int numIters = 2;
	private final static int polyN = 5;
	private final static double polySigma = 1.1;
	private final static int flags = 0;

	//Update path for Mangal's environment.
	private final static String input = "/Users/turek/Desktop/MAX_2018001_LP823_Control-03-02_Myosin_denoised.tif";
	private static SCIFIO scifio;

	@Test
	public void testMatAndImgArraysAreSameShape() throws IOException {

		MatVector mats = new MatVector();
		imreadmulti( input, mats );
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );
		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		for ( int i = 0; i < 5; i++ ) {
			Mat matSlice = mats.get( i );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/matSlice" + i + ".tif", matSlice );
			RandomAccessibleInterval< ByteType > ijSlice = Views.hyperSlice( image, 2, i );
			saveImg( ijSlice, "/Users/turek/Desktop/test/ijSlice" + i + ".tif" );
			checkData( ijSlice, matSlice );
		}

	}

	@SuppressWarnings( "unchecked" )
	@Test
	public void testFullCircleConversionFromIJ() throws IOException {

		MatVector mats = new MatVector();
		imreadmulti( input, mats );
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );
		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		for ( int i = 0; i < 5; i++ ) {
			RandomAccessibleInterval< ByteType > ijSlice = Views.hyperSlice( image, 2, i );
			saveImg( ijSlice, "/Users/turek/Desktop/test/ijSlice" + i + ".tif" );
			Mat mat = ImgToMatConverter.getByteMat( ijSlice );
			RandomAccessibleInterval< ByteType > newijSlice = ( RandomAccessibleInterval< ByteType > ) MatToImgConverter.convert( mat );
			saveImg( newijSlice, "/Users/turek/Desktop/test/newijSlice" + i + ".tif" );
			checkData( newijSlice, ijSlice );

		}

	}

	@Test
	public void testFullCircleConversionFromMat() throws IOException {

		MatVector mats = new MatVector();
		imreadmulti( input, mats );
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );
		for ( int i = 0; i < 5; i++ ) {
			Mat matSlice = mats.get( i );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/matSlice" + i + ".tif", matSlice );
			RandomAccessibleInterval< ByteType > img = MatToImgConverter.toByteImg( matSlice );
			Mat newMat = new ImgToMatConverter().convert( img, Mat.class );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/newMat" + i + ".tif", newMat );
			checkData( newMat, matSlice );
		}

	}

	@SuppressWarnings( "unchecked" )
	@Test
	public void testMatToImgComversion() throws IOException {
		MatVector mats = new MatVector();
		imreadmulti( input, mats );
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );
		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		for ( int i = 0; i < 5; i++ ) {
			Mat matSlice = mats.get( i );
			RandomAccessibleInterval< ByteType > cvMatSlice = ( RandomAccessibleInterval< ByteType > ) MatToImgConverter.convert( matSlice );
			saveImg( cvMatSlice, "/Users/turek/Desktop/test/cvMatSlice" + i + ".tif" );
			RandomAccessibleInterval< ByteType > ijSlice = Views.hyperSlice( image, 2, i );
			checkData( cvMatSlice, ijSlice );
		}

	}

	@Test
	public void testImgToMatConversion() throws IOException {
		MatVector mats = new MatVector();
		imreadmulti( input, mats );
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );
		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		for ( int i = 0; i < 5; i++ ) {
			RandomAccessibleInterval< ByteType > ijSlice = Views.hyperSlice( image, 2, i );
			Mat cvImgSlice = new ImgToMatConverter().convert( ijSlice, Mat.class );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/cvImgSlice" + i + ".tif", cvImgSlice );
			checkData( cvImgSlice, mats.get( i ) );
		}

	}

	@SuppressWarnings( "unchecked" )
	@Test
	public void computeFernbackOpticalFlowTest() throws IOException {
	
		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		int[] dims = Intervals.dimensionsAsIntArray( image );	
		List< Mat > dataSlices = new ArrayList< Mat >( dims[ 2 ] );
		for ( int i = 0; i < dims[ 2 ]; i++ ) {
			RandomAccessibleInterval< ByteType > ijSlice = Views.hyperSlice( image, 2, i );
			Mat slice = ImgToMatConverter.getByteMat( ijSlice );
			dataSlices.add( slice );
		}
	
		final DenseOpticalFlow opticalFlow = FarnebackOpticalFlow.create( numLevels, pyrScale, fastPyramids, winSize, numIters, polyN, polySigma, flags );
		List< Mat > flows = new ArrayList< Mat >( dims[ 2 ] - 1 );
		Mat flow = null;
		for ( int i = 1; i < dims[ 2 ]; i++ ) {
			flow = new Mat();
			//Note!!!! calc only takes 8 bit images, and returns 2 channel floats!
			opticalFlow.calc( dataSlices.get( i - 1 ), dataSlices.get( i ), flow );
			flows.add( flow );
		}
		assertEquals( 59, flows.size() );
	
		List< RandomAccessibleInterval< FloatType > > flowsx = new ArrayList< RandomAccessibleInterval< FloatType > >();
		List< RandomAccessibleInterval< FloatType > > flowsy = new ArrayList< RandomAccessibleInterval< FloatType > >();
		for ( int i = 0; i < flows.size(); i++ ) {
			MatVector splitflows = new MatVector();
			opencv_core.split( flows.get( i ), splitflows );
			//System.out.println( "X Type " + CvType.typeToString( splitflows.get( 0 ).type()) );
			flowsx.add( ( RandomAccessibleInterval< FloatType > ) MatToImgConverter.convert( splitflows.get( 0 ) ) );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/flowx" + i + ".tif", splitflows.get( 0 ) );
			flowsy.add( ( RandomAccessibleInterval< FloatType > ) MatToImgConverter.convert( splitflows.get( 1 ) ) );
			opencv_imgcodecs.imwrite( "/Users/turek/Desktop/test/flowy" + i + ".tif", splitflows.get( 1 ) );
		}
		assertEquals( 59, flowsx.size() );
		assertEquals( 59, flowsy.size() );
	
		RandomAccessibleInterval< FloatType > xijFlowFinal = Views.stack( flowsx );
		saveImg( xijFlowFinal , "/Users/turek/Desktop/test/JavaOutputX2.tif" );
		RandomAccessibleInterval< FloatType > yijFlowFinal = Views.stack( flowsy );
		saveImg( yijFlowFinal , "/Users/turek/Desktop/test/JavaOutputY2.tif" );
	
	}

	private void checkData( RandomAccessibleInterval< ByteType > img, Mat mat ) {
		final byte[] matData = MatToImgConverter.toByteArray( mat );
		final byte[] imgData = ImgToMatConverter.toByteArray( img );

		assertEquals( matData.length, imgData.length );
		org.junit.Assert.assertArrayEquals( matData, imgData );
	}

	private void checkData( RandomAccessibleInterval< ByteType > img1, RandomAccessibleInterval< ByteType > img2 ) {
		final byte[] imgData1 = ImgToMatConverter.toByteArray( img1 );
		final byte[] imgData2 = ImgToMatConverter.toByteArray( img2 );

		assertEquals( imgData1.length, imgData2.length );
		org.junit.Assert.assertArrayEquals( imgData1, imgData2 );
	}

	private void checkData( Mat mat1, Mat mat2 ) {
		final byte[] matData1 = MatToImgConverter.toByteArray( mat1 );
		final byte[] matData2 = MatToImgConverter.toByteArray( mat2 );

		assertEquals( matData1.length, matData2.length );
		org.junit.Assert.assertArrayEquals( matData1, matData2 );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static void saveImg( RandomAccessibleInterval image, String output ) {

		Dataset ds = getScifio().getContext().getService( DatasetService.class ).create( image );
		try {
			getScifio().datasetIO().save( ds, output );
		} catch ( IOException e ) {
			fail( e.getMessage() );
		}
	}

	private static SCIFIO getScifio() {
		if ( scifio == null )
			scifio = new SCIFIO();
		return scifio;
	}


}
