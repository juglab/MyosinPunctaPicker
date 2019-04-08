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
import net.imglib2.Interval;
import net.imglib2.KDTree;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealPointSampleList;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.neighborsearch.InverseDistanceWeightingInterpolatorFactory;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.neighborsearch.KNearestNeighborSearch;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
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

	private ArrayList< LocalMaximaQuartet > localMaxima;
	private ArrayList< LocalMaximaQuartet > thresholdedLocalMaxima;
	private PunctaPickerModel model;
	private FlowVectorsCollection flowVecCol;
	private OpService os;

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

	public void computeGenericFlow( RandomAccessibleInterval< DoubleType > img ) {
		float sigma = 2;
		RandomAccessibleInterval< DoubleType > smoothed_img = gaussian_smoothing2D( img, sigma );
		RandomAccessibleInterval< DoubleType > denseFlow =
				interpolateFlowkNN( flowVecCol.getSparsehandPickedFlowVectors(), smoothed_img, model.getView().getKNeighbors() );
		ArrayList< FlowVector > spacedFlow = prepareSpacedVectors( denseFlow );
		flowVecCol.setDenseFlow( denseFlow );
		flowVecCol.setSpacedFlow( spacedFlow );
	}

	public void computeHybridFlow( RandomAccessibleInterval< DoubleType > img ) {
		float sigma = 6;
		RandomAccessibleInterval< DoubleType > smoothed_img = gaussian_smoothing2D( img, sigma );
		ArrayList< FlowVector > handPicked = flowVecCol.getSparsehandPickedFlowVectors();
		ArrayList< FlowVector > autoFeatures = computeTMFeatures( img, sigma );
		ArrayList< FlowVector > concatenatedList = new ArrayList<>();
		concatenatedList.addAll( handPicked );
		concatenatedList.addAll( autoFeatures );
		RandomAccessibleInterval< DoubleType > denseFlow =
				interpolateFlowkNN( concatenatedList, smoothed_img, model.getView().getKNeighbors() );
		ArrayList< FlowVector > spacedFlow = prepareSpacedVectors( denseFlow );
		flowVecCol.setAutoFeatureFlow( autoFeatures );
		flowVecCol.setDenseFlow( denseFlow );
		flowVecCol.setSpacedFlow( spacedFlow );
	}

	public < T extends RealType< T > & NativeType< T > > void computeBlobBasedFlow( RandomAccessibleInterval< T > im ) { //Experimental, may be deleted later

		float sigma = 6;
		RandomAccessibleInterval< T > smoothed_img = gaussian_smoothing2D( im, sigma );
		ArrayList< FlowVector > handPicked = flowVecCol.getSparsehandPickedFlowVectors();
		Img< T > img = model.getView().getImage();
		ArrayList< Puncta > allFlowBlobs = new ArrayList<>();
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
			List< Puncta > pun_per_frame = computeBlobBasedTMFeatures( newImage, time );
			System.out.println( pun_per_frame.size() );
			allFlowBlobs.addAll( pun_per_frame );
		}
		ArrayList< FlowVector > autoFeatures = blobTemplateMatching( im, allFlowBlobs );
		System.out.println( autoFeatures.size() );
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

	private List< Puncta > getFlowBlobsAtTime( int t, ArrayList< Puncta > flowBlobs ) {
		ArrayList< Puncta > ret = new ArrayList< Puncta >();
		for ( Puncta p : flowBlobs ) {
			if ( p.getT() == t )
				ret.add( p );
		}
		return ret;
	}

	private < T extends RealType< T > & NativeType< T > > ArrayList< FlowVector > blobTemplateMatching(
			RandomAccessibleInterval< T > img,
			ArrayList< Puncta > flowBlobs ) {  //Experimental, may be deleted later, very simplistic only considers distance and can be one-to many matches
		ArrayList< FlowVector > flowVectorList = new ArrayList<>();
		int ii = 0;
		int jj = 0;
		double window = 25;
		
		for ( int pos = 0; pos < img.dimension( 2 ) - 1; pos++ ) {
			List< Puncta > blobs_current = getFlowBlobsAtTime( pos, flowBlobs );
			List< Puncta > blobs_next = getFlowBlobsAtTime( pos + 1, flowBlobs );
			for ( Puncta blob : blobs_current ) {
				double min_dist = Double.MAX_VALUE;
				Puncta closest = null;
				for(Puncta p: blobs_next) {
					double computed_distance = compute_dist( blob, p );
					if ( ( computed_distance < window ) && ( computed_distance < min_dist ) ) {
						min_dist = compute_dist( blob, p );
						closest = p;
					}
				}
				if ( !( closest == null ) )
					flowVectorList
						.add( new FlowVector( blob.getX(), blob.getY(), blob.getT(), closest.getX() - blob.getX(), closest.getY() - blob.getY() ) );
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

	private static RandomAccessibleInterval< DoubleType > interpolateFlowNN( ArrayList< FlowVector > sparseFlow, RandomAccessibleInterval< DoubleType > img ) {
		
		ArrayList< RandomAccessibleInterval< DoubleType > > slices = new ArrayList< RandomAccessibleInterval< DoubleType > >();
		
		for ( long pos = 0; pos < img.dimension( 2 ) - 1; pos++ ) {
			RandomAccessibleInterval< DoubleType > slice = Views.hyperSlice( img, 2, pos );
			
			RealPointSampleList< DoubleType > realIntervalU = new RealPointSampleList<>( 2 );
			RealPointSampleList< DoubleType > realIntervalV = new RealPointSampleList<>( 2 );
			 for ( FlowVector f : sparseFlow ) { 
				 if (f.getT()==pos){ 
					 RealPoint point = new RealPoint( 2 );
					 point.setPosition(f.getX(),0);
					 point.setPosition(f.getY(),1);
					 
					realIntervalU.add( point, new DoubleType( f.getU() ) );
					realIntervalV.add( point, new DoubleType( f.getV() ) );
				 }
			 }
			 
			 
			 
			NearestNeighborSearch< DoubleType > searchU = new NearestNeighborSearchOnKDTree<>( new KDTree<>( realIntervalU ) );
			NearestNeighborSearch< DoubleType > searchV = new NearestNeighborSearchOnKDTree<>( new KDTree<>( realIntervalV ) );
			 
			RealRandomAccessible< DoubleType > realRandomAccessibleU =
					Views.interpolate( searchU, new NearestNeighborSearchInterpolatorFactory< DoubleType >() );
			RealRandomAccessible< DoubleType > realRandomAccessibleV =
					Views.interpolate( searchV, new NearestNeighborSearchInterpolatorFactory< DoubleType >() );
			 
			RandomAccessible< DoubleType > randomAccessibleU = Views.raster( realRandomAccessibleU );
			RandomAccessible< DoubleType > randomAccessibleV = Views.raster( realRandomAccessibleV );
			 
			RandomAccessibleInterval< DoubleType > sliceU = Views.interval( randomAccessibleU, slice );
			RandomAccessibleInterval< DoubleType > sliceV = Views.interval( randomAccessibleV, slice );
			 
			 slices.add(sliceU);
			 slices.add(sliceV);
			 
			
		}
		
		RandomAccessibleInterval< DoubleType > stack = Views.stack( slices );
	
		
		return stack;
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

	/////////////////////////////////////////////////////////////........Not the most important bits below this...................////////////////////////////////////////////////////////////////////////////////////

//	public void computeTMFlow( RandomAccessibleInterval< DoubleType > img ) {
//		float sigma = 2;
//		RandomAccessibleInterval< DoubleType > smoothed_img = gaussian_smoothing2D( img, sigma );
//		localMaxima = findLocalMax( img, 20 );
//		thresholdedLocalMaxima = thresholdedMaxima( localMaxima, 150 );
//		sparseHandPickedFlow = templateMatching( smoothed_img, thresholdedLocalMaxima );
//		if ( SimpleMenu.getFlowMethod() == "NN" ) {
//			System.out.println( "NN!" );
//			denseFlow = interpolateFlowNN( sparseHandPickedFlow, smoothed_img );
//		}
//		if ( SimpleMenu.getFlowMethod() == "kNN" ) {
//			denseFlow = interpolateFlowkNN( sparseHandPickedFlow, smoothed_img );
//		}
//		if ( SimpleMenu.getFlowMethod() == "TPS" ) {
//			denseFlow = interpolateFlowTPS( sparseHandPickedFlow, smoothed_img );
//		}
//	}

	public ArrayList< FlowVector > computeTMFeatures( RandomAccessibleInterval< DoubleType > img, float sigma ) {
		RandomAccessibleInterval< DoubleType > smoothed_img = gaussian_smoothing2D( img, sigma );
		localMaxima = findLocalMax( img, 20 );
		thresholdedLocalMaxima = thresholdedMaxima( localMaxima, 50 );
		ArrayList< FlowVector > pts = templateMatching( smoothed_img, thresholdedLocalMaxima );
		return pts;
	}

	public < T extends RealType< T > & NativeType< T > > List< Puncta > computeBlobBasedTMFeatures( Img< T > img, int t ) { //Just for experimental purpose, maybe deleted later
		double minScale = 1;
		double stepScale = 1;
		double maxScale = 5;
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

	public ArrayList< LocalMaximaQuartet > getLocalMaxima() {
		return localMaxima;
	}

	public ArrayList< LocalMaximaQuartet > getThresholdedLocalMaxima() {
		return thresholdedLocalMaxima;
	}

	private static ArrayList< LocalMaximaQuartet > findLocalMax( RandomAccessibleInterval< DoubleType > img, int r ) {

		ArrayList< LocalMaximaQuartet > localMaxList = new ArrayList<>();
		for ( long pos = 0; pos < img.dimension( 2 ); pos++ ) {
			RandomAccessibleInterval< DoubleType > slice = Views.hyperSlice( img, 2, pos );
			Interval interval = Intervals.expand(slice, -r-2);
			slice = Views.interval(slice, interval);
			Cursor< DoubleType > cursor = Views.iterable( slice ).cursor();

			while ( cursor.hasNext() ) {
				cursor.fwd();
				HyperSphere< DoubleType > smallSphere = new HyperSphere<>( slice, cursor, r );
				DoubleType centerValue = cursor.get();
				boolean isMaximum = true;

				for ( final DoubleType value : smallSphere ) {
					if ( centerValue.compareTo( value ) < 0 ) {
						isMaximum = false;
						break;
					}
				}
				if ( isMaximum ) {

					localMaxList
							.add( new LocalMaximaQuartet( cursor.getIntPosition( 0 ), cursor.getIntPosition( 1 ), ( int ) pos, centerValue.get() ) );
				}
			}

		}
		return localMaxList;
	}

	private static ArrayList< LocalMaximaQuartet > thresholdedMaxima( ArrayList< LocalMaximaQuartet > a, float threshold ) {
		ArrayList< LocalMaximaQuartet > thresholdedMaxima = new ArrayList<>();
		int i = 0;
		while ( i < a.size() ) {
			if ( a.get( i ).getV() >= threshold )
				thresholdedMaxima.add( a.get( i ) );
			i++;
		}

		return thresholdedMaxima;
	}

	private static ArrayList< FlowVector > templateMatching( RandomAccessibleInterval< DoubleType > img, ArrayList< LocalMaximaQuartet > quartet ) {
		RectangleShape rectangle = new RectangleShape( 5, false );
		ArrayList<FlowVector> flowVectorList = new ArrayList<>(); 
		int ii = 0;
		int jj = 0;

		for ( long pos = 0; pos < img.dimension( 2 ) - 1; pos++ ) {
			RandomAccessibleInterval< DoubleType > slice_small = Views.hyperSlice( img, 2, pos );
			NeighborhoodsAccessible< DoubleType > neighborhoods1 = rectangle.neighborhoodsRandomAccessible( slice_small );
			RandomAccess< Neighborhood< DoubleType > > ran1 = neighborhoods1.randomAccess();
			RandomAccessibleInterval< DoubleType > slice_big = Views.hyperSlice( img, 2, pos + 1 );
			NeighborhoodsAccessible< DoubleType > neighborhoods2 = rectangle.neighborhoodsRandomAccessible( slice_big );
			RandomAccess< Neighborhood< DoubleType > > ran2 = neighborhoods2.randomAccess();
			for ( LocalMaximaQuartet q : quartet ) {
				if(q.getT() == pos) {
					
				
    				ran1.setPosition( new int [] {q.getX(), q.getY()} );
					Neighborhood< DoubleType > n1 = ran1.get();

    				double maxVal = -Double.MAX_VALUE;
    				for (int i = -5; i< 5; i++ ) {
    					for (int j = -5; j < 5; j++) {
    						ran2.setPosition( new int [] {q.getX()+i, q.getY()+j} );
							Neighborhood< DoubleType > n2 = ran2.get();
    						
							Cursor< DoubleType > cursorA = n1.cursor();
							Cursor< DoubleType > cursorB = n2.cursor();
    				        double sum = 0;
    				        while (cursorA.hasNext()) {
    				            double valueA = cursorA.next().getRealDouble();
    				            double valueB = cursorB.next().getRealDouble();
    				            sum+=(valueA*valueB);
    //				            sum += Math.pow( valueA-valueB,2 );
    			
    				        }
    				        if(sum > maxVal) {
    				        	maxVal = sum;
    				        	ii = i;
    				        	jj = j;
    				        }
    						
    					}
					}
    				
    				flowVectorList.add( new FlowVector( q.getX(), q.getY(), q.getT(), ii, jj ) );
			}
			}
				
				
		}	
			return flowVectorList;
	}

	public static RandomAccessibleInterval< DoubleType > getRandomFlow( RandomAccessibleInterval< DoubleType > image ) {

		final ImgFactory< DoubleType > imgFactory = new CellImgFactory<>( new DoubleType(), 5 );
		final Img< DoubleType > img1 = imgFactory.create( image.dimension( 0 ), image.dimension( 1 ), image.dimension( 2 ) * 2 - 2 );

		Cursor< DoubleType > cursorInput = img1.cursor();

		while ( cursorInput.hasNext() ) {
			cursorInput.fwd();
			cursorInput.get().set( 50 * Math.random() - 25 );
		}
		return img1;
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

	public void plotAutoFeaturesOnly( RandomAccessibleInterval< DoubleType > img ) { //TODO remove it later, for preliminary experimentation only
		float sigma = 6;
		ArrayList< FlowVector > autoFeatures = computeTMFeatures( img, sigma );
		flowVecCol.setAutoFeatureFlow( autoFeatures );
	}

}
