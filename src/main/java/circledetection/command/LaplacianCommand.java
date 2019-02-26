package circledetection.command;

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a Laplacian implementation optimized for speed using
 * {@link SeparableKernelConvolution}.
 *
 * @param <T> type
 * @author Matthias Arzt, Tim-Oliver Buccholz, Manan Lalit, MPI-CBG / CSBD, Dresden
 */
@Plugin(type = Command.class, menuPath = "Plugins > Laplacian")
public class LaplacianCommand<T extends RealType<T>> implements Command {

    @Parameter(type = ItemIO.INPUT)
    Img<T> image;

    @Parameter(type = ItemIO.INPUT)
    double sigma;

    /*The axis (0, 1, 2) which is sampled differently than the rest*/
    @Parameter(type = ItemIO.INPUT)
    int axis;

    /*The sampling factor on the previously mentioned axis*/
    @Parameter(type = ItemIO.INPUT)
    double samplingFactor;

    /* The Output "Laplacian_Of_Gaussian-filtered" Image should be Float Type and not "T" to allow for negative responses
	 which would not be captured by the original 8-bit image.*/
    @Parameter(type = ItemIO.OUTPUT)
    Img<FloatType> output;

    @Parameter
    OpService ops;

    @Override
    public void run() {

        final List<RandomAccessibleInterval<FloatType>> secondDerivativeList = new ArrayList<>();

        for (int dim = 0; dim < image.numDimensions(); dim++) {
            secondDerivativeList.add(secondDerivative(image, sigma, dim));
        }

        output = ops.create().img(image, new FloatType());
        LoopBuilder.setImages(Views.collapse(Views.stack(secondDerivativeList)), output).forEachPixel((derivatives, r) -> {

            float sum = 0;
            for (int i = 0; i <= image.numDimensions(); i++) {
                sum += derivatives.get(i).getRealFloat();
            }

            r.setReal(sum);
        });

    }


    private RandomAccessibleInterval<FloatType> secondDerivative(final RandomAccessibleInterval<T> input, final double sigma, final int d) {


        /*Output here is the second derivative of the image w.r.t one axis*/
        final Img<FloatType> output = ArrayImgs.floats(Intervals.dimensionsAsLongArray(input));

        final Kernel1D second_derivative;

        if (d == axis) {
            second_derivative = DerivedNormalDistribution.derivedGaussKernel(sigma / samplingFactor, 2);
        } else {
            second_derivative = DerivedNormalDistribution.derivedGaussKernel(sigma, 2);
        }


        final Kernel1D[] kernels = new Kernel1D[image.numDimensions()];
        for (int i = 0; i < image.numDimensions(); i++) {
            if (i == axis) {
                kernels[i] = DerivedNormalDistribution.derivedGaussKernel(sigma / samplingFactor, 0);
            } else {
                kernels[i] = DerivedNormalDistribution.derivedGaussKernel(sigma, 0);
            }

        }
        kernels[d] = second_derivative;
        SeparableKernelConvolution.convolution(kernels).process(Views.extendBorder(input), output);
        return output;
    }

    public static void main(String[] args) {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
    }

}