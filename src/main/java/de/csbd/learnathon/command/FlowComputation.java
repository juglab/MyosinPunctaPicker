package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.scijava.table.Column;
import org.scijava.table.GenericTable;

import circledetection.command.BlobDetectionCommand;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.KDTree;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealPointSampleList;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.neighborsearch.InverseDistanceWeightingInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.neighborsearch.KNearestNeighborSearch;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class FlowComputation {

	private PunctaPickerModel model;
	private FlowVectorsCollection flowVecCollection;
	private OpService os;
	private ArrayList< FlowVector > autoFeatures;

	public FlowComputation( PunctaPickerModel model ) {
		this.model = model;
		this.flowVecCollection = model.getFlowVectorsCollection();
	}

	public ArrayList< FlowVector > initializeControlVectorsForFlow() {
		ArrayList< FlowVector > sparseHandPickedFlow = extractFlowVectorsFromClicks();
		flowVecCollection.setSparseHandPickedFlow( sparseHandPickedFlow );
		return sparseHandPickedFlow;
	}

	public < T extends RealType< T > & NativeType< T > > void computeSemiAutoInterpolatedFlow( RandomAccessibleInterval< T > im ) { //Experimental, may be deleted later

		float sigma = 6;
		RandomAccessibleInterval< T > smoothed_img = gaussian_smoothing2D( im, sigma );
		ArrayList< FlowVector > handPicked = flowVecCollection.getSparsehandPickedFlowVectors();
		Img< T > img = model.getView().getImage();
		ArrayList< Puncta > allFlowBlobs = new ArrayList<>();
		autoFeatures = computeBlobBasedAutoFlowVecs( img, allFlowBlobs );
		ArrayList< FlowVector > concatenatedList = new ArrayList<>();
		if ( !( handPicked == null ) && !( handPicked.isEmpty() ) )
			concatenatedList.addAll( handPicked );
		concatenatedList.addAll( autoFeatures );
		RandomAccessibleInterval< T > denseFlow =
				interpolateFlowkNN( concatenatedList, smoothed_img, model.getView().getKNeighbors() );
		flowVecCollection.setAutoFeatureFlow( autoFeatures );
		flowVecCollection.setDenseFlow( denseFlow );
	}

	public < T extends RealType< T > & NativeType< T > > void computeManuallyInterpolatedFlow( RandomAccessibleInterval< T > im ) { //Experimental, may be deleted later

		float sigma = 6;
		RandomAccessibleInterval< T > smoothed_img = gaussian_smoothing2D( im, sigma );
		ArrayList< FlowVector > handPicked = flowVecCollection.getSparsehandPickedFlowVectors();
		Img< T > img = model.getView().getImage();
		if ( !( handPicked == null ) && !( handPicked.isEmpty() ) ) {
			RandomAccessibleInterval< T > denseFlow =
					interpolateFlowkNN( handPicked, smoothed_img, model.getView().getKNeighbors() );
			flowVecCollection.setDenseFlow( denseFlow );
		}

	}

	private < T extends RealType< T > & NativeType< T > > ArrayList< FlowVector > computeBlobBasedAutoFlowVecs(
			Img< T > img,
			ArrayList< Puncta > allFlowBlobs ) {
		for ( int time = 0; time < img.dimension( 2 ); time++ ) {
			IntervalView< T > image = Views.hyperSlice( img, 2, time );
			Views.extendMirrorSingle( image );
			FinalInterval cropped = Intervals.createMinMax(
					( 0 ),
					( 0 ),
					0,
					( img.dimension( 0 ) - 5 ),
					( img.dimension( 1 ) - 5 ),
					0 );
			RandomAccessibleInterval< T > croppedImage = Views.interval( image, cropped );
			ImagePlus imgPlus = ImageJFunctions.wrap( croppedImage, "cropped" );
			Img< T > newImage = ImageJFunctions.wrap( imgPlus );
			List< Puncta > pun_per_frame = computeAllBlobs( newImage, time );
			allFlowBlobs.addAll( pun_per_frame );
		}
		ArrayList< FlowVector > features;
		String matchingMode = model.getView().getMatchingMode();
		if ( matchingMode == "greedy" ) {
			features = greedyMatching( img, allFlowBlobs );
		} else {
			features = hungarianMatching( img, allFlowBlobs );
		}
		return features;
	}

	private List< Puncta > getFlowBlobsAtTime( int t, ArrayList< Puncta > flowBlobs ) {
		ArrayList< Puncta > ret = new ArrayList< Puncta >();
		for ( Puncta p : flowBlobs ) {
			if ( p.getT() == t )
				ret.add( p );
		}
		return ret;
	}

	private < T extends RealType< T > & NativeType< T > > ArrayList< FlowVector > hungarianMatching( Img<T> img, ArrayList< Puncta > allFlowBlobs ) {  //Experimental, may be deleted later, very simplistic only considers distance and can be one-to many matches
		ArrayList< FlowVector > flowVectorList = new ArrayList<>();

		for ( int pos = 0; pos < img.dimension( 2 ) - 1; pos++ ) {
			List< Puncta > blobs_current = getFlowBlobsAtTime( pos, allFlowBlobs );
			List< Puncta > blobs_next = getFlowBlobsAtTime( pos + 1, allFlowBlobs );
			double[][] initialCostMatrix = new double[ blobs_current.size() ][ blobs_next.size() ];
			double[][] costMatrix = defineCostMatrix( initialCostMatrix, blobs_current, blobs_next );
			HungarianAlgorithm hungarian = new HungarianAlgorithm( costMatrix );
			int[] assgn = hungarian.execute();
			for ( int i = 0; i < assgn.length; i++ ) {
				if ( !( assgn[ i ] == -1 ) ) {
					if ( compute_dist_squared(
							blobs_current.get( i ),
							blobs_next.get( assgn[ i ] ) ) < ( model.getView().getAutoFlowMatchingWindowSize() * model
									.getView()
									.getAutoFlowMatchingWindowSize() ) ) { //A far away assignment doesn't make sense; it can happen in cases where a blob is detected in one time frame but is not detected in the next frame; then it is better to discard it than to assign it to some far away blob
						FlowVector fv =
								new FlowVector( blobs_current.get( i ).getX(), blobs_current.get( i ).getY(), blobs_current
										.get( i )
										.getT(), blobs_next
												.get( assgn[ i ] )
												.getX() - blobs_current
														.get( i )
														.getX(), blobs_next.get( assgn[ i ] ).getY() - blobs_current.get( i ).getY() );
						flowVectorList.add( fv );
					}

				}		
			}
		}
		return flowVectorList;
	}

	private double[][] defineCostMatrix( double[][] costMatrix, List< Puncta > blobs_current, List< Puncta > blobs_next ) {
		double rel_weight = model.getView().getRelWeight() / 10d; // relative weighting to dist and radius difference when matching two blobs
		for ( int i = 0; i < blobs_current.size(); i++ ) {
			for ( int j = 0; j < blobs_next.size(); j++ ) {
				double dist = compute_dist_squared( blobs_current.get( i ), blobs_next.get( j ) );
				double rad_diff = Math.abs( blobs_current.get( i ).getR() - blobs_next.get( j ).getR() );
				costMatrix[ i ][ j ] = rel_weight * dist + ( 1 - rel_weight ) * rad_diff;
			}
		}
		return costMatrix;
	}

	private < T extends RealType< T > & NativeType< T > > ArrayList< FlowVector > greedyMatching(
			Img< T > img,
			ArrayList< Puncta > flowBlobs ) {  //Experimental, may be deleted later, very simplistic only considers distance and can be one-to many matches
		ArrayList< FlowVector > flowVectorList = new ArrayList<>();
		double window = ( model.getView().getAutoFlowMatchingWindowSize() * model
				.getView()
				.getAutoFlowMatchingWindowSize() );

		for ( int pos = 0; pos < img.dimension( 2 ) - 1; pos++ ) {
			List< Puncta > blobs_current = getFlowBlobsAtTime( pos, flowBlobs );
			List< Puncta > blobs_next = getFlowBlobsAtTime( pos + 1, flowBlobs );
			for ( Puncta blob : blobs_current ) {
				double min_dist = Double.MAX_VALUE;
				Puncta closest = null;
				for ( Puncta p : blobs_next ) {
					double computed_distance = compute_dist_squared( blob, p );
					if ( ( computed_distance < window ) && ( computed_distance < min_dist ) ) {
						min_dist = compute_dist_squared( blob, p );
						closest = p;
					}
				}
				if ( !( closest == null ) )
					flowVectorList
							.add(
									new FlowVector( blob.getX(), blob.getY(), blob
											.getT(), closest.getX() - blob.getX(), closest.getY() - blob.getY() ) );
			}
		}
		return flowVectorList;
	}

	private double compute_dist_squared( Puncta blob, Puncta p ) {
		return ( ( blob.getX() - p.getX() ) * ( blob.getX() - p.getX() ) + ( blob.getY() - p.getY() ) * ( blob.getY() - p.getY() ) );
	}

	private ArrayList< FlowVector > extractFlowVectorsFromClicks() {
		ArrayList< FlowVector > featureFlowVectorList = new ArrayList<>();
		if ( model.getGraph() != null ) {

			List< Edge > edges = model.getGraph().getEdges();
			for ( Edge edge : edges ) {
				featureFlowVectorList.add(
						new FlowVector( edge.getA().getX(), edge.getA().getY(), edge
								.getA()
								.getT(), edge.getB().getX() - edge.getA().getX(), edge.getB().getY() - edge.getA().getY() ) );
			}

		}
		return featureFlowVectorList;
	}

	private static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > gaussian_smoothing2D(
			RandomAccessibleInterval< T > img,
			float sigma ) {

		float[] s = new float[ img.numDimensions() - 1 ];

		for ( int d = 0; d < s.length; ++d )
			s[ d ] = sigma;

		for ( long pos = 0; pos < img.dimension( 2 ); pos++ ) {
			RandomAccessibleInterval< T > slice = Views.hyperSlice( img, 2, pos );
			Gauss3.gauss( sigma, Views.extendMirrorSingle( slice ), slice );
		}

		return img;
	}

	private static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > interpolateFlowkNN(
			ArrayList< FlowVector > sparseFlow,
			RandomAccessibleInterval< T > img,
			int kNeighbors ) {

		ArrayList< RandomAccessibleInterval< T > > slices = new ArrayList< RandomAccessibleInterval< T > >();

		for ( long pos = 0; pos < img.dimension( 2 ) - 1; ++pos ) {
			RandomAccessibleInterval< T > slice = Views.hyperSlice( img, 2, pos );

			RealPointSampleList< T > realIntervalU = new RealPointSampleList<>( 2 );
			RealPointSampleList< T > realIntervalV = new RealPointSampleList<>( 2 );
			for ( FlowVector f : sparseFlow ) {
				if ( f.getT() == pos ) {
					RealPoint point = new RealPoint( 2 );
					point.setPosition( f.getX(), 0 );
					point.setPosition( f.getY(), 1 );

					realIntervalU.add( point, ( T ) new DoubleType( f.getU() ) );
					realIntervalV.add( point, ( T ) new DoubleType( f.getV() ) );
				}
			}

			KNearestNeighborSearch< T > searchU =
					new KNearestNeighborSearchOnKDTree<>( new KDTree<>( realIntervalU ), Math.min( kNeighbors, ( int ) realIntervalU.size() ) );
			KNearestNeighborSearch< T > searchV =
					new KNearestNeighborSearchOnKDTree<>( new KDTree<>( realIntervalV ), Math.min( kNeighbors, ( int ) realIntervalV.size() ) );

			RealRandomAccessible< T > realRandomAccessibleU =
					Views.interpolate( searchU, new InverseDistanceWeightingInterpolatorFactory< T >() );
			RealRandomAccessible< T > realRandomAccessibleV =
					Views.interpolate( searchV, new InverseDistanceWeightingInterpolatorFactory< T >() );

			RandomAccessible< T > randomAccessibleU = Views.raster( realRandomAccessibleU );
			RandomAccessible< T > randomAccessibleV = Views.raster( realRandomAccessibleV );

			RandomAccessibleInterval< T > sliceU = Views.interval( randomAccessibleU, slice );
			RandomAccessibleInterval< T > sliceV = Views.interval( randomAccessibleV, slice );

			slices.add( sliceU );
			slices.add( sliceV );

		}

		RandomAccessibleInterval< T > stack = Views.stack( slices );

		return stack;
	}

	public < T extends RealType< T > & NativeType< T > > void modifyOpticalFlowWIthInterpolation( String opticalFlowMode ) {
		ArrayList< FlowVector > handPickedVectors = flowVecCollection.getSparsehandPickedFlowVectors();
		RandomAccessibleInterval< T > denseOpticalFlowOriginal = ( RandomAccessibleInterval< T > ) flowVecCollection.getDenseFlow();
		RandomAccessibleInterval< T > denseFlowCopy =
				Util.getSuitableImgFactory( denseOpticalFlowOriginal, Util.getTypeFromInterval( denseOpticalFlowOriginal ) ).create(
						denseOpticalFlowOriginal );
		LoopBuilder.setImages( denseFlowCopy, denseOpticalFlowOriginal ).forEachPixel( ( a, b ) -> a.setReal( b.getRealFloat() ) );
		double windowSize = model.getView().getOpticalFlowModificationWindowSize();
		BlendingFunctions.Blender blender = null;
		if ( opticalFlowMode == "prefer GT" ) {
			blender = new BlendingFunctions.GaussianBlendedFlow( ( float ) windowSize );
		} else if ( opticalFlowMode == "linear blend" ) {
			blender = new BlendingFunctions.LinearlyBlendedFlow( ( float ) windowSize );
		} else if ( opticalFlowMode == "gaussian blend" ) {
			blender = new BlendingFunctions.GaussianBlendedFlow( ( float ) windowSize );
		} else if ( opticalFlowMode == "gaussian smoothed gt" ) {
			blender = new BlendingFunctions.GaussianSmoothedFlow( ( float ) windowSize );
		}

		for ( FlowVector gtFlowVector : handPickedVectors ) {

			int x = Math.round( gtFlowVector.getX() );
			int y = Math.round( gtFlowVector.getY() );
			int t = gtFlowVector.getT();
			double[] uv = new double[] { gtFlowVector.getU(), gtFlowVector.getV() };

			final FinalInterval sweepingWindow = Intervals.createMinMax(
					( long ) ( x - windowSize / 2 ),
					( long ) ( y - windowSize / 2 ),
					0,
					( long ) ( x + windowSize / 2 ),
					( long ) ( y + windowSize / 2 ),
					0 );

			for ( int i = 0; i < 2; i++ ) {
				IntervalView< T > slice = Views.hyperSlice( denseFlowCopy, 2, 2 * t + i );
				final IterableInterval< T > denseFlowCopyWindow = Views.iterable( Views.interval( slice, sweepingWindow ) );
				final Cursor< T > c = denseFlowCopyWindow.localizingCursor();
				while ( c.hasNext() ) {
					c.fwd();
					float r = ( float ) Math.sqrt(
							( c.getFloatPosition( 0 ) - x ) * ( c.getFloatPosition( 0 ) - x ) + ( c.getFloatPosition( 1 ) - y ) * ( c
									.getFloatPosition( 1 ) - y ) );
					float alpha = blender.getAlpha( r );
					float beta = blender.getBeta( r );
					double flow = alpha * uv[ i ] + beta * c.get().getRealDouble();

					c.get().setReal( flow );
				}
			}

		}
		flowVecCollection.setDenseFlow( denseFlowCopy );

	}

	public < T extends RealType< T > & NativeType< T > > List< Puncta > computeAllBlobs( Img< T > img, int t ) { //Just for experimental purpose, maybe deleted later
		double minScale = 1;
		double stepScale = 1;
		double maxScale = 15;
        boolean brightBlobs = true;
        int axis = 0;
        double samplingFactor = 1;

		FinalInterval outputInterval = Intervals.createMinMax(
				( 0 ),
				( 0 ),
				0,
				( img.dimension( 0 ) ),
				( img.dimension( 1 ) ),
				0 );
		BlobDetectionCommand< T > blobDetection =
				new BlobDetectionCommand<>( img, minScale, maxScale, stepScale, brightBlobs, axis, samplingFactor, model
						.getView()
						.getOs(), outputInterval );

		final GenericTable resultsTable = blobDetection.getResultsTable();

        /*Step Two: Find Otsu Threshold Value on the new List, so obtained*/
        SampleList<FloatType> localMinimaResponse = createIterableList(resultsTable.get("Value"));
		Histogram1d< FloatType > hist = model.getView().getOs().image().histogram( localMinimaResponse );
		String thresholdingMode = model.getView().getThresholdingMode();
		float threshold;
		if ( thresholdingMode == "custom threshold" ) {
			threshold = -1f * ( model.getView().getThreshold() );
		} else {
			threshold = model.getView().getOs().threshold().otsu( hist ).getRealFloat();
		}
		System.out.println( "Threshold used for time " + t + " :" + threshold );
		Pair< List< Puncta >, List< Float > > thresholdedPairList = getThresholdedLocalMinima( threshold, resultsTable, t );
		return thresholdedPairList.getA();
	}

	private Pair< List< Puncta >, List< Float > > getThresholdedLocalMinima( float threshold, GenericTable resultsTable, int t ) { //Just for experimental purpose, maybe deleted later
		Column< ? > valueOld = resultsTable.get( "Value" );
		Column< ? > XOld = resultsTable.get( "X" );
		Column< ? > YOld = resultsTable.get( "Y" );
		Column< ? > radiusOld = resultsTable.get( "Radius" );
		List< Puncta > listPuncta = new LinkedList<>();
		List< Float > listVal = new LinkedList<>();

		for ( int i = 0; i < resultsTable.getRowCount(); i++ ) {
			if ( ( float ) valueOld.get( i ) <= threshold ) {
				listPuncta.add( new Puncta( ( int ) XOld.get( i ), ( int ) YOld.get( i ), t, ( float ) radiusOld.get( i ) ) );
				listVal.add( ( float ) valueOld.get( i ) );
			}
		}
		return new ValuePair<>( listPuncta, listVal );
	}

	private SampleList< FloatType > createIterableList( final Column column ) { //Just for experimental purpose, maybe deleted later
		final Iterator< Float > iterator = column.iterator();
		final List< FloatType > imageResponse = new ArrayList<>();
		while ( iterator.hasNext() ) {
			imageResponse.add( new FloatType( iterator.next().floatValue() ) );
		}

		return new SampleList<>( imageResponse );
	}

	public < T extends RealType< T > & NativeType< T > > void modifyOpticalFlow() {
		ArrayList< FlowVector > handPickedVectors = flowVecCollection.getSparsehandPickedFlowVectors();
		RandomAccessibleInterval< T > denseOpticalFlowOriginal = ( RandomAccessibleInterval< T > ) flowVecCollection.getDenseFlow();
		RandomAccessibleInterval< T > denseFlowCopy =
				Util.getSuitableImgFactory( denseOpticalFlowOriginal, Util.getTypeFromInterval( denseOpticalFlowOriginal ) ).create( denseOpticalFlowOriginal );
		LoopBuilder.setImages( denseFlowCopy, denseOpticalFlowOriginal ).forEachPixel( Type::set );
		double sigma = model.getView().getOpticalFlowModificationWindowSize();
		for ( FlowVector flowVector : handPickedVectors ) {
			float x = flowVector.getX();
			float y = flowVector.getY();
			int t = flowVector.getT();
			double u = flowVector.getU();
			double v = flowVector.getV();
			for ( int i = 0; i < 2; i++ ) {
				IntervalView< T > image = Views.hyperSlice( denseFlowCopy, 2, t + i );
				RandomAccessible< T > infiniteImg = Views.extendMirrorSingle( image );
				
				FinalInterval cropped = Intervals.createMinMax(
						( long ) ( x - sigma / 2 ),
						( long ) ( y - sigma / 2 ),
						0,
						( long ) ( x + sigma / 2 ),
						( long ) ( y + sigma / 2 ),
						0 );

				LoopBuilder.setImages( Views.interval( infiniteImg, cropped ) ).forEachPixel( ( c ) -> c.setZero() );
				RandomAccess< T > ra = infiniteImg.randomAccess();
				ra.setPosition( ( int ) x, 0 );
				ra.setPosition( ( int ) y, 1 );
				double scaling_factor = 0;
				if ( i == 0 ) {
					ra.get().setReal( u );
					scaling_factor = u;
				} else {
					ra.get().setReal( v );
					scaling_factor = v;
				}
				RandomAccessibleInterval< T > region = Views.interval( image, cropped );
				Gauss3.gauss( sigma, infiniteImg, region );
				double actualEffect = ra.get().getRealDouble();
				double scaledEffect = scaling_factor / actualEffect;
				LoopBuilder.setImages( Views.interval( infiniteImg, cropped ) ).forEachPixel( ( c ) -> c.mul( scaledEffect ) );
			}

		}
		flowVecCollection.setDenseFlow( denseFlowCopy );

	}

	public void resetOpticalFlow() {
		if ( !( flowVecCollection.getOriginalOpticalFlow() == null ) ) {
			flowVecCollection.setDenseFlow( flowVecCollection.getOriginalOpticalFlow() );
		}

	}
}
