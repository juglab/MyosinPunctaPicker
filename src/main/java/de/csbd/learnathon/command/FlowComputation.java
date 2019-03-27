package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.Cursor;
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
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.interpolation.neighborsearch.InverseDistanceWeightingInterpolatorFactory;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.neighborsearch.KNearestNeighborSearch;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class FlowComputation {


	private ArrayList< LocalMaximaQuartet > localMaxima;
	private ArrayList< LocalMaximaQuartet > thresholdedLocalMaxima;
	private PunctaPickerModel model;
	private FlowVectorsCollection flowVecCol;

	public FlowComputation( PunctaPickerModel model ) {
		this.model = model;
		this.flowVecCol = model.getFlowVectorsCollection();
	}

	public FlowComputation() {}

//	public ArrayList< FlowVector > getSparseHandPickedFlow() {
//		return sparseHandPickedFlow;
//	}
//
//	public RandomAccessibleInterval< DoubleType > getDenseFlow() {
//		return denseFlow;
//	}
//
//	public ArrayList< FlowVector > getSpacedFlow() {
//		return spacedFlow;
//	}

	public void computeGenericFlow( RandomAccessibleInterval< DoubleType > img ) {
		float sigma = 2;
		RandomAccessibleInterval< DoubleType > smoothed_img = gaussian_smoothing2D( img, sigma );
		ArrayList< FlowVector > sparseHandPickedFlow = extractFlowVectorsFromClicks();
		RandomAccessibleInterval< DoubleType > denseFlow = interpolateFlowkNN( sparseHandPickedFlow, smoothed_img );
		ArrayList< FlowVector > spacedFlow = prepareSpacedVectors( denseFlow );
		flowVecCol.setSparseHandPickedFlow( sparseHandPickedFlow );
		flowVecCol.setDenseFlow( denseFlow );
		flowVecCol.setSpacedFlow( spacedFlow );
	}

	private ArrayList< FlowVector > prepareSpacedVectors( RandomAccessibleInterval< DoubleType > flowData ) { //Might move to Flow Computation
		ArrayList< FlowVector > spacedFlowVectors = new ArrayList<>();
		final long sizeX = flowData.dimension( 0 );
		final long sizeY = flowData.dimension( 1 );
		int spacing = 100; // spacing between pixels for flow display //TODO expose this as parameter
		spacing = Math.max( spacing, ( int ) Math.max( sizeX, sizeY ) / 25 ); // but if large image only 25 vecs along longer side

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

	private FlowVector getFlowVector( RandomAccessibleInterval< DoubleType > f, int x, int y, int t ) {
		RandomAccess< DoubleType > ra = f.randomAccess();
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

	private static RandomAccessibleInterval< DoubleType > gaussian_smoothing2D( RandomAccessibleInterval< DoubleType > img, float sigma ) {

		float[] s = new float[ img.numDimensions() - 1 ];

		for ( int d = 0; d < s.length; ++d )
			s[ d ] = sigma;

		for ( long pos = 0; pos < img.dimension( 2 ); pos++ ) {
			RandomAccessibleInterval< DoubleType > slice = Views.hyperSlice( img, 2, pos );
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

	private static RandomAccessibleInterval< DoubleType > interpolateFlowkNN(
			ArrayList< FlowVector > sparseFlow,
			RandomAccessibleInterval< DoubleType > img ) {

		ArrayList< RandomAccessibleInterval< DoubleType > > slices = new ArrayList< RandomAccessibleInterval< DoubleType > >();

		for ( long pos = 0; pos < img.dimension( 2 ) - 1; ++pos ) {
			RandomAccessibleInterval< DoubleType > slice = Views.hyperSlice( img, 2, pos );

			RealPointSampleList< DoubleType > realIntervalU = new RealPointSampleList<>( 2 );
			RealPointSampleList< DoubleType > realIntervalV = new RealPointSampleList<>( 2 );
			for ( FlowVector f : sparseFlow ) {
				if ( f.getT() == pos ) {
					RealPoint point = new RealPoint( 2 );
					point.setPosition( f.getX(), 0 );
					point.setPosition( f.getY(), 1 );

					realIntervalU.add( point, new DoubleType( f.getU() ) );
					realIntervalV.add( point, new DoubleType( f.getV() ) );
				}
			}

			KNearestNeighborSearch< DoubleType > searchU =
					new KNearestNeighborSearchOnKDTree<>( new KDTree<>( realIntervalU ), Math.min( 3, ( int ) realIntervalU.size() ) );
			KNearestNeighborSearch< DoubleType > searchV =
					new KNearestNeighborSearchOnKDTree<>( new KDTree<>( realIntervalV ), Math.min( 3, ( int ) realIntervalV.size() ) );

			RealRandomAccessible< DoubleType > realRandomAccessibleU =
					Views.interpolate( searchU, new InverseDistanceWeightingInterpolatorFactory< DoubleType >() );
			RealRandomAccessible< DoubleType > realRandomAccessibleV =
					Views.interpolate( searchV, new InverseDistanceWeightingInterpolatorFactory< DoubleType >() );

			RandomAccessible< DoubleType > randomAccessibleU = Views.raster( realRandomAccessibleU );
			RandomAccessible< DoubleType > randomAccessibleV = Views.raster( realRandomAccessibleV );

			RandomAccessibleInterval< DoubleType > sliceU = Views.interval( randomAccessibleU, slice );
			RandomAccessibleInterval< DoubleType > sliceV = Views.interval( randomAccessibleV, slice );

			slices.add( sliceU );
			slices.add( sliceV );

		}

		RandomAccessibleInterval< DoubleType > stack = Views.stack( slices );

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

}
