package circledetection.command;

import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;


/**
 * This is an implementation that calls {@link LaplacianCommand} and then identifies the local minima.
 *
 * @param <T> type
 * @author Matthias Arzt, Tim-Oliver Buccholz, Manan Lalit, MPI-CBG / CSBD, Dresden
 */

@Plugin(type = Command.class, menuPath = "Plugins > LocalMinima")
public class LocalMinimaCommand<T extends RealType<T> & NativeType<T> & Comparable<T>> implements Command {

    @Parameter(type = ItemIO.INPUT)
    Img<T> image;

    /*The output is a list of Points*/
    @Parameter(type = ItemIO.OUTPUT)
    List<Point> output;

    @Parameter
    CommandService cs;

    @Parameter
    OpService ops;

    @Override
    public void run() {


        final T value = image.firstElement().createVariable();

        /*Extract Local Minima*/
        output = extractLocalMinima(image, value);

    }


    private List<Point> extractLocalMinima(final RandomAccessibleInterval<T> image, final T highValue) {
        return extractLocalMinima(Views.extendValue(image, highValue), image);
    }


    private List<Point> extractLocalMinima(final RandomAccessible<T> image, final Interval interval) {
        final Shape shape = new RectangleShape(1, true);
        final RandomAccessible<Neighborhood<T>> neighborhoods = shape.neighborhoodsRandomAccessibleSafe(image);
        final List<Point> points = new ArrayList<>();

        LoopBuilder.setImages(Views.interval(image, interval), Views.interval(neighborhoods, interval)).forEachPixel(
                (centerValue, neighborhood) -> {
                    if (isCenterMinimal(centerValue, neighborhood)) {
                        final Point minimumLocation = new Point(neighborhood);
                        points.add(minimumLocation);
                    }
                }
        );
        return points;
    }


    private boolean isCenterMinimal(final T center, final Neighborhood<T> neighborhood) {
        boolean isMinimum = true;
        for (final T value : neighborhood) {
            if (center.compareTo(value) >= 0) {
                isMinimum = false;
                break;
            }
        }
        return isMinimum;
    }
}

