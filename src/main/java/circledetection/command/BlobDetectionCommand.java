package circledetection.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.scijava.table.DefaultGenericTable;
import org.scijava.table.FloatColumn;
import org.scijava.table.GenericTable;
import org.scijava.table.IntColumn;

import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;


/**
 * This is an implementation that calls {@link LaplacianCommand} and {@link LocalMinimaCommand} in order to detect blobs.
 *
 * @param <T> type
 * @author Matthias Arzt, Tim-Oliver Buccholz, Manan Lalit MPI-CBG / CSBD, Dresden
 */

public class BlobDetectionCommand< T extends RealType< T > & Type< T > > {

    Img<T> image;
    double minScale=5;
    double maxScale=5.01;
    double stepScale=1;
    boolean brightBlobs=true; /*1 if bright blobs on dark background, 0 otherwise*/
    int axis=0; /*The axis (0,1,2) which is sampled differently*/
    double samplingFactor=1; /*The sampling factor applied on the axis*/
	OpService ops;
	private GenericTable resultsTable;
	private Interval output;

	public BlobDetectionCommand(
			Img< T > image,
			double minScale,
			double maxScale,
			double stepScale,
			boolean brightBlobs,
			int axis,
			double samplingFactor,
			OpService ops,
			Interval output ) {
		this.image = image;
		this.minScale = minScale;
		this.maxScale = maxScale;
		this.stepScale = stepScale;
		this.brightBlobs = brightBlobs;
		this.axis = axis;
		this.samplingFactor = samplingFactor;
		this.ops = ops;
		this.output = output;
		setUpBlobDetectionCommand();
	}

	private void setUpBlobDetectionCommand() {

		/*
		 * Step One: Obtain Laplacian reponse, normalize to make
		 * scale-independent and stack in a pyramid
		 */
		List< Point > predictedResult;
		Img< FloatType > normalizedExpandedImage = ( Img< FloatType > ) multiScaleLaplacian( image, minScale, maxScale, stepScale, image );
		LocalMinimaCommand localMinimaCommand = new LocalMinimaCommand<>( normalizedExpandedImage );
		localMinimaCommand.setLocalMinima();
		predictedResult = localMinimaCommand.getOutput();

		/*
		 * Step Three: Create a (N+7) dimensioned table based on the results
		 */
		resultsTable = createResultsTable( normalizedExpandedImage, predictedResult );

        predictedResult=null;
        normalizedExpandedImage=null;
	}


	public GenericTable getResultsTable() {
		return resultsTable;
	}

	private RandomAccessibleInterval< FloatType > multiScaleLaplacian(
			final Img< T > image,
			final double minScale,
			final double maxScale,
			final double stepScale,
			final Interval outputInterval ) {

        final List<Img<FloatType>> results = new ArrayList<>();
        for (double scale = minScale; scale <= maxScale; scale = scale + stepScale) {
            Img<FloatType> normalizedLaplacianOfGaussian=ops.create().img(image, new FloatType());
			LaplacianCommand laplacianCommand = new LaplacianCommand<>( image, scale, axis, samplingFactor, outputInterval, ops );
			laplacianCommand.runLaplacian();
			final Img< FloatType > laplacianOfGaussian = laplacianCommand.getOutput();
            final double s = scale;
            LoopBuilder.setImages(laplacianOfGaussian, normalizedLaplacianOfGaussian).forEachPixel((i, o) -> o.setReal(Math.pow(s, 2) * i.getRealFloat()));
            results.add(normalizedLaplacianOfGaussian);
        }
        return copy(Views.stack(results));


    }

    private RandomAccessibleInterval<FloatType> copy(final RandomAccessibleInterval<FloatType> input) {
        Img<FloatType> output = ArrayImgs.floats(Intervals.dimensionsAsLongArray(input));
        LoopBuilder.setImages(input, output).forEachPixel((i, o) -> o.set(i));
        return output;
    }


    private GenericTable createResultsTable(final Img<FloatType> input, final List<Point> listOfMinima) {

        IntColumn XColumn = new IntColumn("X");
        IntColumn YColumn = new IntColumn("Y");
        IntColumn ZColumn = new IntColumn("Z");
        IntColumn SliceColumn=new IntColumn("Slice");
        FloatColumn ScaleColumn = new FloatColumn("Scale");
        FloatColumn RadiusColumn = new FloatColumn("Radius");
        FloatColumn ValueColumn = new FloatColumn("Value");
        IntColumn RedColumn=new IntColumn("Red");
        IntColumn GreenColumn=new IntColumn("Green");
        IntColumn BlueColumn=new IntColumn("Blue");

        GenericTable resultsTable = new DefaultGenericTable();
        final Iterator<Point> iterator = listOfMinima.iterator();
        final RandomAccess<FloatType> randomAccess = input.randomAccess();

        while (iterator.hasNext()) {
            Point point=iterator.next();
            if (point.numDimensions()==4){
                /*i.e. 3D image*/
                randomAccess.setPosition(point);
                XColumn.add(point.getIntPosition(0));
                YColumn.add(point.getIntPosition(1));
                ZColumn.add(point.getIntPosition(2));
                SliceColumn.add(point.getIntPosition(3));
                ScaleColumn.add((float)(minScale+stepScale*point.getIntPosition(3)));
                RadiusColumn.add((float)(Math.sqrt(3)*(minScale+stepScale*point.getIntPosition(3))));

        }
            else if (point.numDimensions()==3){
            /*i.e. 2D image*/
                randomAccess.setPosition(point);
                XColumn.add(point.getIntPosition(0));
                YColumn.add(point.getIntPosition(1));
                SliceColumn.add(point.getIntPosition(2));
                ScaleColumn.add((float)(minScale+stepScale*point.getIntPosition(2)));
                RadiusColumn.add((float)(Math.sqrt(2)*(minScale+stepScale*point.getIntPosition(2))));

            }
            ValueColumn.add(randomAccess.get().copy().getRealFloat());
            RedColumn.add((int) (255 * Math.random()));
            GreenColumn.add((int) (255 * Math.random()));
            BlueColumn.add((int) (255 * Math.random()));
        }


        resultsTable.add(XColumn);
        resultsTable.add(YColumn);
        resultsTable.add(ZColumn);
        resultsTable.add(SliceColumn);
        resultsTable.add(ScaleColumn);
        resultsTable.add(RadiusColumn);
        resultsTable.add(ValueColumn);
        resultsTable.add(RedColumn);
        resultsTable.add(GreenColumn);
        resultsTable.add(BlueColumn);

        return resultsTable;

    }



}







