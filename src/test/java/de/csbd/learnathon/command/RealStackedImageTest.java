package de.csbd.learnathon.command;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imreadmulti;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_video.DenseOpticalFlow;
import org.bytedeco.opencv.opencv_video.FarnebackOpticalFlow;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencv.core.CvType;

import io.scif.SCIFIO;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.opencv.ImgToMatConverter;
import net.imagej.opencv.ImgToMatVectorConverter;
import net.imagej.opencv.MatToImgConverter;
import net.imagej.opencv.MatVectorToImgConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class RealStackedImageTest {

	private final static int numLevels = 1;
	private final static double pyrScale = 0.5;
	private final boolean fastPyramids = false;
	private final static int winSize = 5;
	private final static int numIters = 2;
	private final static int polyN = 5;
	private final static double polySigma = 1.1;
	private final static int flags = 0;

	private static boolean DEBUG = false;
	private static SCIFIO scifio;
	private static final String TEST_DIR = System.getProperty( "user.dir" ) + File.separator + "test";
	private static final String mat2img = TEST_DIR + File.separator + "mat2img.tif";
	private static final String fullCircleFromImg = TEST_DIR + File.separator + "fullCircleFromImg.tif";
	private static final String input = "res/Substack.tif";

	@BeforeClass
	public static void setup() {

		File testDir = new File( TEST_DIR );

		try {
			//Cleanup before starting
			if ( testDir.exists() ) {
				FileUtils.forceDelete( new File( TEST_DIR ) );
			}
			Files.createDirectory( testDir.toPath(), new FileAttribute< ? >[ 0 ] );
		} catch ( IOException e ) {
			fail( e.getMessage() );
		}
	}

	@SuppressWarnings( "unchecked" )
	@Test
	public void testFullCircleConversionFromIJ() throws IOException {

		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		
		MatVector cvMatImg = ( MatVector ) new ImgToMatVectorConverter().convert( image, MatVector.class );
		RandomAccessibleInterval< ByteType > cvImg = (RandomAccessibleInterval< ByteType >) new MatVectorToImgConverter().convert(cvMatImg, RandomAccessibleInterval.class);
		saveImg(cvImg, fullCircleFromImg );
		
		for ( int i = 0; i < 3; i++ ) {
			saveImg(Views.hyperSlice( cvImg, 2, i ), TEST_DIR + File.separator + "img2imgSlice" + i + ".tif" );
			checkData(Views.hyperSlice( cvImg, 2, i ), Views.hyperSlice( image, 2, i ));
		}
	}

	@SuppressWarnings( "unchecked" )
	@Test
	public void testFullCircleConversionFromMatVector() throws IOException {

		MatVector mats = new MatVector();
		imreadmulti( input, mats, opencv_imgcodecs.IMREAD_GRAYSCALE );
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );
		
		RandomAccessibleInterval< ByteType > cvMats = (RandomAccessibleInterval< ByteType >) new MatVectorToImgConverter().convert(mats, RandomAccessibleInterval.class);
		MatVector cvImgMat = ( MatVector ) new ImgToMatVectorConverter().convert( cvMats, MatVector.class );
		
		for ( int i = 0; i < 3; i++ ) {
			opencv_imgcodecs.imwrite( TEST_DIR + File.separator + "mv2mvSlice" + i + ".tif", cvImgMat.get(i) );
			checkData( mats.get( i ), cvImgMat.get(i) );
		}

	}

	@SuppressWarnings( "unchecked" )
	@Test
	public void testMatVectorToImgComversion() throws IOException {
		MatVector mats = new MatVector();
		imreadmulti( input, mats, opencv_imgcodecs.IMREAD_GRAYSCALE );
		if ( mats.empty() )
			throw new IOException( "Couldn't load image: " + input );
		
		RandomAccessibleInterval< ByteType > cvMats = (RandomAccessibleInterval< ByteType >) new MatVectorToImgConverter().convert(mats, RandomAccessibleInterval.class);
		saveImg(cvMats, mat2img);
		
		for ( int i = 0; i < 3; i++ ) {
			saveImg(Views.hyperSlice( cvMats, 2, i ), TEST_DIR + File.separator + "mv2imgSlice" + i + ".tif" );
			checkData( (RandomAccessibleInterval< ByteType >)Views.hyperSlice( cvMats, 2, i ), mats.get(i));
		}

	}

	@Test
	public void testImgToMatVectorConversion() throws IOException {
		ImageJ ij = new ImageJ();
		Dataset dataset = ij.get( DatasetIOService.class ).open( input );
		RandomAccessibleInterval< ByteType > image =
				RealTypeConverters.convert( ( RandomAccessibleInterval< ? extends RealType< ? > > ) dataset.getImgPlus().getImg(), new ByteType() );
		
		MatVector cvImg = new ImgToMatVectorConverter().convert( image, MatVector.class );
		
		for ( int i = 0; i < 3; i++ ) {
			opencv_imgcodecs.imwrite( TEST_DIR + File.separator + "img2mvSlice" + i + ".tif", cvImg.get( i ) );
			checkData( Views.hyperSlice( image, 2, i ), cvImg.get( i ) );
		}

	}

	@SuppressWarnings( "unchecked" )
	@Test
	public void computeFernbackOpticalFlowTest() throws IOException {

		Dataset dataset = new ImageJ().get( DatasetIOService.class ).open( input );
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
		assertEquals( 3, flows.size() );

		List< RandomAccessibleInterval< FloatType > > ijflows = new ArrayList< RandomAccessibleInterval< FloatType > >();
		for ( int i = 0; i < flows.size(); i++ ) {
			MatVector splitflows = new MatVector();
			opencv_core.split( flows.get( i ), splitflows );
			opencv_imgcodecs.imwrite( TEST_DIR + File.separator + "flowx" + i + ".tif", splitflows.get( 0 ) );
			opencv_imgcodecs.imwrite( TEST_DIR + File.separator + "flowy" + i + ".tif", splitflows.get( 1 ) );
			ijflows.add( ( RandomAccessibleInterval< FloatType > ) MatToImgConverter.convert( splitflows.get( 0 ) ) );
			ijflows.add( ( RandomAccessibleInterval< FloatType > ) MatToImgConverter.convert( splitflows.get( 1 ) ) );
		}
		assertEquals( 6, ijflows.size() );

		RandomAccessibleInterval< FloatType > ijFlowFinal = Views.stack( ijflows );
		saveImg( ijFlowFinal, TEST_DIR + File.separator + "JavaOutput.tif" );
	}

	private void checkData( RandomAccessibleInterval< ByteType > img, Mat mat ) {
		final byte[] matData = MatToImgConverter.toByteArray( mat );
		final byte[] imgData = ImgToMatConverter.toByteArray( img );

		assertEquals( matData.length, imgData.length );
		Assert.assertArrayEquals( matData, imgData );
	}

	private void checkData( RandomAccessibleInterval< ByteType > img1, RandomAccessibleInterval< ByteType > img2 ) {
		final byte[] imgData1 = ImgToMatConverter.toByteArray( img1 );
		final byte[] imgData2 = ImgToMatConverter.toByteArray( img2 );

		assertEquals( imgData1.length, imgData2.length );
		Assert.assertArrayEquals( imgData1, imgData2 );
	}
	
	private void checkData( Mat mat1, Mat mat2 ) {
		int type = mat1.depth();
		switch ( type ) {
		case CvType.CV_8U:
		case CvType.CV_8S:
			byte[] byteData1 = MatToImgConverter.toByteArray( mat1 );
			byte[] byteData2 = MatToImgConverter.toByteArray( mat2 );

			if ( DEBUG ) {
				for ( int i = 0; i < byteData1.length; i++ ) {
					System.out.println( "i = " + i + " , mat1 = " + byteData1[ i ] + " , mat2 = " + byteData2[ i ] );
				}
			}

			assertEquals( byteData1.length, byteData2.length );
			Assert.assertArrayEquals( byteData1, byteData2 );
			break;
		case CvType.CV_32S:
			int[] intData1 = MatToImgConverter.toIntArray( mat1 );
			int[] intData2 = MatToImgConverter.toIntArray( mat2 );

			assertEquals( intData1.length, intData2.length );
			Assert.assertArrayEquals( intData1, intData2 );
			break;
		case CvType.CV_32F:
			float[] matData1 = MatToImgConverter.toFloatArray( mat1 );
			float[] matData2 = MatToImgConverter.toFloatArray( mat2 );

			assertEquals( matData1.length, matData2.length );
			Assert.assertArrayEquals( matData1, matData2, 0.01f );
			break;
		case CvType.CV_64F:
			double[] dblData1 = MatToImgConverter.toDoubleArray( mat1 );
			double[] dblData2 = MatToImgConverter.toDoubleArray( mat2 );

			assertEquals( dblData1.length, dblData2.length );
			Assert.assertArrayEquals( dblData1, dblData2, 0.01 );
			break;
		default:
			throw new UnsupportedOperationException( "Unsupported CvType value: " + type );
		}
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
