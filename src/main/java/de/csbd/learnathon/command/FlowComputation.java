package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.scijava.table.Column;
import org.scijava.table.GenericTable;

import circledetection.command.BlobDetectionCommand;
import ij.ImagePlus;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
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
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.neighborsearch.InverseDistanceWeightingInterpolatorFactory;
import net.imglib2.neighborsearch.KNearestNeighborSearch;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class FlowComputation {

	private PunctaPickerModel model;
	private FlowVectorsCollection flowVecCol;
	private OpService os;
	private ArrayList< FlowVector > autoFeatures;

	public FlowComputation( PunctaPickerModel model ) {
		this.model = model;
		this.flowVecCol = model.getFlowVectorsCollection();
	}

	public FlowComputation() {}

	public ArrayList< FlowVector > initializeControlVectorsForFlow() {
		ArrayList< FlowVector > sparseHandPickedFlow = extractFlowVectorsFromClicks();
		flowVecCol.setSparseHandPickedFlow( sparseHandPickedFlow );
		return sparseHandPickedFlow;
	}

	public < T extends RealType< T > & NativeType< T > > void computeSemiAutoInterpolatedFlow( RandomAccessibleInterval< T > im ) { //Experimental, may be deleted later

		float sigma = 6;
		RandomAccessibleInterval< T > smoothed_img = gaussian_smoothing2D( im, sigma );
		ArrayList< FlowVector > handPicked = flowVecCol.getSparsehandPickedFlowVectors();
		Img< T > img = model.getView().getImage();
		ArrayList< Puncta > allFlowBlobs = new ArrayList<>();
		autoFeatures = computeBlobBasedAutoFlowVecs( img, allFlowBlobs );
		ArrayList< FlowVector > concatenatedList = new ArrayList<>();
		concatenatedList.addAll( handPicked );
		concatenatedList.addAll( autoFeatures );
		RandomAccessibleInterval< T > denseFlow =
				interpolateFlowkNN( concatenatedList, smoothed_img, model.getView().getKNeighbors() );
		ArrayList< FlowVector > spacedFlow = prepareSpacedVectors( denseFlow );
		flowVecCol.setAutoFeatureFlow( autoFeatures );
		flowVecCol.setDenseFlow( denseFlow );
		flowVecCol.setSpacedFlow( spacedFlow );

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
//		autoFeatures = greedyMatching( img, allFlowBlobs );
		autoFeatures = hungarianMatching( img, allFlowBlobs );
		return autoFeatures;
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
				if ( !( assgn[ i ] == -1 ) ) { //Expose window size as parameter
					if ( compute_dist( blobs_current.get( i ), blobs_next.get( assgn[ i ] ) ) < 25 ) { //A far away assignment doesn't make sense; it can happen in cases where a blob is detected in one time frame but is not detected in the next frame; then it is better to discard it than to assign it to some far away blob
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
		double rel_weight = 1; // relative weighting to dist and radius difference when matching two blobs, can  be a exposed parameter
		for ( int i = 0; i < blobs_current.size(); i++ ) {
			for ( int j = 0; j < blobs_next.size(); j++ ) {
				double dist = compute_dist( blobs_current.get( i ), blobs_next.get( j ) );
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
		double window = 25;

		for ( int pos = 0; pos < img.dimension( 2 ) - 1; pos++ ) {
			List< Puncta > blobs_current = getFlowBlobsAtTime( pos, flowBlobs );
			List< Puncta > blobs_next = getFlowBlobsAtTime( pos + 1, flowBlobs );
			for ( Puncta blob : blobs_current ) {
				double min_dist = Double.MAX_VALUE;
				Puncta closest = null;
				for ( Puncta p : blobs_next ) {
					double computed_distance = compute_dist( blob, p );
					if ( ( computed_distance < window ) && ( computed_distance < min_dist ) ) {
						min_dist = compute_dist( blob, p );
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

	private double compute_dist( Puncta blob, Puncta p ) {
		return ( ( blob.getX() - p.getX() ) * ( blob.getX() - p.getX() ) + ( blob.getY() - p.getY() ) * ( blob.getY() - p.getY() ) );
	}

	private < T extends RealType< T > & NativeType< T > > ArrayList< FlowVector > prepareSpacedVectors( RandomAccessibleInterval< T > flowData ) { //Might move to Flow Computation
		ArrayList< FlowVector > spacedFlowVectors = new ArrayList<>();
		final long sizeX = flowData.dimension( 0 );
		final long sizeY = flowData.dimension( 1 );
		int density = model.getView().getDensity(); // spacing between pixels for flow display 
		int spacing = 4;

		for ( int t = 0; t < flowData.dimension( 2 ) / 2; t++ ) {
			int startx = ( int ) ( sizeX % spacing ) / 2;
			startx = ( startx == 0 ) ? spacing / 2 : startx;
			int starty = ( int ) ( sizeY % spacing ) / 2;
			starty = ( starty == 0 ) ? spacing / 2 : starty;
			for ( int x = startx; x < sizeX; x += spacing ) {
				for ( int y = starty; y < sizeY; y += spacing ) {
					FlowVector flowVec = getFlowVector( flowData, x, y, t );
					spacedFlowVectors.add( flowVec );
				}
			}
		}
		return spacedFlowVectors;
	}

	private < T extends RealType< T > & NativeType< T > > FlowVector getFlowVector( RandomAccessibleInterval< T > f, int x, int y, int t ) {
		RandomAccess< T > ra = f.randomAccess();
		ra.setPosition( x, 0 );
		ra.setPosition( y, 1 );
		ra.setPosition( 2 * t, 2 );
		Double u = ra.get().getRealDouble();
		ra.setPosition( x, 0 );
		ra.setPosition( y, 1 );
		ra.setPosition( 2 * t + 1, 2 );
		Double v = ra.get().getRealDouble();
		FlowVector flowVector = new FlowVector( x, y, t, u, v );
		return flowVector;
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


	public < T extends RealType< T > & NativeType< T > > List< Puncta > computeAllBlobs( Img< T > img, int t ) { //Just for experimental purpose, maybe deleted later
		double minScale = 1;
		double stepScale = 1;
		double maxScale = 20;
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
		float otsuThreshold = model.getView().getOs().threshold().otsu( hist ).getRealFloat();
		Pair< List< Puncta >, List< Float > > thresholdedPairList = getThresholdedLocalMinima( otsuThreshold, resultsTable, t );
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

	private static RandomAccessibleInterval< DoubleType > interpolateFlowTPS(
			ArrayList< FlowVector > sparseFlow,
			RandomAccessibleInterval< DoubleType > image ) {
		ArrayList< RandomAccessibleInterval< DoubleType > > slices = new ArrayList< RandomAccessibleInterval< DoubleType > >();
		for ( long pos = 0; pos < image.dimension( 2 ) - 1; ++pos ) {
			System.out.println( pos );

			RandomAccessibleInterval< DoubleType > slice = Views.hyperSlice( image, 2, pos );
			final ImgFactory< DoubleType > imgFactory = new CellImgFactory<>( new DoubleType(), 5 );
			final Img< DoubleType > sliceU = imgFactory.create( slice.dimension( 0 ), slice.dimension( 1 ) );
			final Img< DoubleType > sliceV = imgFactory.create( slice.dimension( 0 ), slice.dimension( 1 ) );

			int size = 0;
			for ( FlowVector f : sparseFlow ) {
				if ( f.getT() == pos )
					size++;
			}

			double[][] srcPts = new double[ 2 ][ size ];
			double[][] tgtPts = new double[ 2 ][ size ];
			int i = 0;
			System.out.println( srcPts[ 1 ].length );
			for ( int k = 0; k < sparseFlow.size(); k++ ) {
				if ( sparseFlow.get( k ).getT() == pos ) {
					srcPts[ 0 ][ i ] = sparseFlow.get( i ).getX();
					srcPts[ 1 ][ i ] = sparseFlow.get( i ).getY();
					tgtPts[ 0 ][ i ] = sparseFlow.get( i ).getX() + sparseFlow.get( i ).getU();
					tgtPts[ 1 ][ i ] = sparseFlow.get( i ).getY() + sparseFlow.get( i ).getV();
					i++;
				}
			}

			ThinPlateR2LogRSplineKernelTransform tps = new ThinPlateR2LogRSplineKernelTransform( 2, srcPts, tgtPts, true );
//			System.out.println( pos );
			Cursor< DoubleType > cursor1 = Views.iterable( sliceU ).cursor();
			Cursor< DoubleType > cursor2 = Views.iterable( sliceV ).cursor();

			while ( cursor1.hasNext() ) {
				cursor1.fwd();
				cursor2.fwd();
				double[] position = new double[ 2 ];
				position[ 0 ] = cursor1.getDoublePosition( 0 );
				position[ 1 ] = cursor1.getDoublePosition( 1 );
				double[] output = new double[ 2 ];

				tps.apply( position, output );
				cursor1.get().set( output[ 0 ] - position[ 0 ] );
				cursor2.get().set( output[ 1 ] - position[ 1 ] );
			}
			slices.add( sliceU );
			slices.add( sliceV );
		}

		RandomAccessibleInterval< DoubleType > stack = Views.stack( slices );
		return stack;
	}

	public < T extends RealType< T > & NativeType< T > > void plotAutoFeaturesOnly() { //TODO remove it later, for preliminary experimentation only
		ArrayList< Puncta > allFlowBlobs = new ArrayList<>();
		Img< T > img = model.getView().getImage();
		autoFeatures = computeBlobBasedAutoFlowVecs( img, allFlowBlobs );
		flowVecCol.setAutoFeatureFlow( autoFeatures );
	}

}
