package circledetection.command;

import net.imagej.ops.OpService;
import net.imagej.table.DefaultGenericTable;
import net.imagej.table.FloatColumn;
import net.imagej.table.GenericTable;
import net.imagej.table.IntColumn;
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
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;


/**
 * This is an implementation that calls {@link LaplacianCommand} and {@link LocalMinimaCommand} in order to detect blobs.
 *
 * @param <T> type
 * @author Matthias Arzt, Tim-Oliver Buccholz, Manan Lalit MPI-CBG / CSBD, Dresden
 */

@Plugin(type = Command.class, menuPath = "Plugins > Blob Detection")
public class BlobDetectionCommand<T extends RealType<T> & Type<T>> implements Command {

    @Parameter(type = ItemIO.INPUT)
    Img<T> image;


    @Parameter(type = ItemIO.INPUT)
    double minScale=5;

    @Parameter(type = ItemIO.INPUT)
    double maxScale=5.01;

    @Parameter(type = ItemIO.INPUT)
    double stepScale=1;

    @Parameter(type = ItemIO.INPUT)
    boolean brightBlobs=true; /*1 if bright blobs on dark background, 0 otherwise*/

    @Parameter(type = ItemIO.INPUT)
    int axis=0; /*The axis (0,1,2) which is sampled differently*/

    @Parameter(type = ItemIO.INPUT)
    double samplingFactor=1; /*The sampling factor applied on the axis*/

    @Parameter(type=ItemIO.OUTPUT)
    GenericTable resultsTable;

    @Parameter
    CommandService cs;

    @Parameter
    OpService ops;

    @Override
    public void run() {


        /*Step One: Obtain Laplacian reponse, normalize to make scale-independent and stack in a pyramid*/
        Img<FloatType> normalizedExpandedImage = (Img<FloatType>) multiScaleLaplacian(image, minScale, maxScale, stepScale);


        /*Step Two: Apply LocalMinima Command to obtain the minima on the normalizedExpandedImage*/
        final Future<CommandModule> lp = cs.run(LocalMinimaCommand.class, false, "image", normalizedExpandedImage);
         List<Point> predictedResult = (List<Point>) cs.moduleService().waitFor(lp).getOutput("output");

        /*Step Three: Create a (N+7) dimensioned table based on the results */
        resultsTable=createResultsTable(normalizedExpandedImage, predictedResult);

        predictedResult=null;
        normalizedExpandedImage=null;
    }


    private RandomAccessibleInterval<FloatType> multiScaleLaplacian(final Img<T> image, final double minScale, final double maxScale, final double stepScale) {

        final List<Img<FloatType>> results = new ArrayList<>();
        for (double scale = minScale; scale <= maxScale; scale = scale + stepScale) {
            Img<FloatType> normalizedLaplacianOfGaussian=ops.create().img(image, new FloatType());
            final Future<CommandModule> lp = cs.run(LaplacianCommand.class, false, "image", image, "sigma", scale, "axis", axis, "samplingFactor", samplingFactor);
            final Img<FloatType> laplacianOfGaussian = (Img<FloatType>) cs.moduleService().waitFor(lp).getOutput("output");
            final double s = scale;
            LoopBuilder.setImages((Img<FloatType>) laplacianOfGaussian, normalizedLaplacianOfGaussian).forEachPixel((i, o) -> o.setReal(Math.pow(s, 2) * i.getRealFloat()));
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







