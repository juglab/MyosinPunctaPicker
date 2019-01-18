/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.csbd.learnathon.command;

import java.io.File;
import java.io.IOException;

import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;

/**
 * This example illustrates how to create an ImageJ {@link Command} plugin.
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 * <p>
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 */
@Plugin( type = Command.class, menuPath = "Plugins>Puncta Picker" )

public class MyosinPunctaPickingPlugin implements Command {

	@Parameter
	DatasetIOService datasetIOService;

	@Parameter( label = "Image to load" )
	private File inputImage;

	@Parameter( style = "directory" )
	private File saveResultsDir;

	@Parameter
	Context context;

	@Parameter
	StatusService statusService;

	@Parameter
	UIService uiService;


	public static void main( final String[] args ) throws IOException {

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( MyosinPunctaPickingPlugin.class, true );
	}

	@Override
	public void run() {
		try {
			punctaPicking();
		} catch ( final Exception e ) {
			e.printStackTrace();
		}

	}

	private < T > void punctaPicking()
			throws Exception {

		final Dataset imagefile = datasetIOService.open( inputImage.getAbsolutePath() );


		final File saveDir = saveResultsDir;

		final ImgPlus< T > imp = ( ImgPlus< T > ) imagefile.getImgPlus();
		final StatusService statusService = this.statusService;
		uiService.show( imp );
	}
}