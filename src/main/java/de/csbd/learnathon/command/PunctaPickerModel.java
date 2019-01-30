package de.csbd.learnathon.command;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

public class PunctaPickerModel {

	private RandomAccessibleInterval< DoubleType > rawData;

	private Graph graph = new Graph();
	private PunctaPickerController controller;
	private PunctaPickerView view;

	PunctaPickerModel( RandomAccessibleInterval< DoubleType > image ) {
		this.rawData = image;
	}

	public void setView( PunctaPickerView v ) {
		this.view = v;
	}

	public void setController( PunctaPickerController controller ) {
		this.controller = controller;
	}

	public RandomAccessibleInterval< DoubleType > getRawData() {
		return rawData;
	}

	public Graph getGraph() {
		return graph;
	}

	public void setGraph( Graph g ) {
		this.graph = g;
	}

	public void processFlow() {
		FlowComputation flowComputation = new FlowComputation();
		flowComputation.computeTMFlow( getRawData() );
		RandomAccessibleInterval< DoubleType > denseFlow = flowComputation.getDenseFlow();
		ArrayList< FlowVector > sparseFlow = flowComputation.getSparseFlow();
		ArrayList< LocalMaximaQuartet > localMaxima = flowComputation.getLocalMaxima();
		ArrayList< LocalMaximaQuartet > thresholdedLocalMaxima = flowComputation.getThresholdedLocalMaxima();
		
//		RandomAccessibleInterval< DoubleType > flow=FlowComputation.getRandomFlow(getRawData() );
		
		FlowOverlay flowDrawer= new FlowOverlay(view.bdv);
		flowDrawer.paintDenseFlow( denseFlow );
		flowDrawer.paintSparseFlow( sparseFlow );
		
		//BdvFunctions.show(flow,"flow",Bdv.options().addTo(view.bdv));	
	}

	public float getDefaultRadius() {
		return 15f;
	}
}









