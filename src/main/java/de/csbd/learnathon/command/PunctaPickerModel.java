package de.csbd.learnathon.command;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

public class PunctaPickerModel {

	private RandomAccessibleInterval< DoubleType > rawData;

	private Graph graph = new Graph();
	private FlowVectorsCollection flowVectorsCol = new FlowVectorsCollection();
	private PunctaPickerController controller;
	private FlowController flowController;
	private FlowComputation flowComputation;
	private PunctaPickerView view;

	private RandomAccessibleInterval< DoubleType > denseFlow;

	private ArrayList< FlowVector > handPickedSparseFlow;

	private ArrayList< FlowVector > spacedFlow;

	PunctaPickerModel( RandomAccessibleInterval< DoubleType > image ) {
		this.rawData = image;
		flowComputation = new FlowComputation( this );
	}

	public void setView( PunctaPickerView v ) {
		this.view = v;
	}

	public PunctaPickerView getView() {
		return view;
	}

	public FlowComputation getFlowComputation() {
		return flowComputation;
	}

	public void setController( PunctaPickerController controller ) {
		this.controller = controller;
	}

	public void setFlowController( FlowController flowController ) {
		this.flowController = flowController;
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

	public FlowVectorsCollection getFlowVectorsCollection() {
		return flowVectorsCol;
	}

//	public void processFlow( String flowMethod ) {
//		FlowComputation flowComputation = new FlowComputation( this );
//		flowComputation.computeTMFlow( getRawData() );
//		RandomAccessibleInterval< DoubleType > denseFlow = flowComputation.getDenseFlow();
//		ArrayList< FlowVector > sparseFlow = flowComputation.getSparseHandPickedFlow();
//		ArrayList< LocalMaximaQuartet > localMaxima = flowComputation.getLocalMaxima();
//		ArrayList< LocalMaximaQuartet > thresholdedLocalMaxima = flowComputation.getThresholdedLocalMaxima();
//		FlowOverlay flowDrawer = new FlowOverlay( view );
//		flowDrawer.setDenseFlow( denseFlow );
////		flowDrawer.paintSparseFlow( sparseFlow );
//	}

	public ArrayList< FlowVector > extractAndInitializeControlVectorsFromHandPickedTracklets() {
		ArrayList< FlowVector > controlVecs = flowComputation.initializeControlVectorsForFlow();
		return controlVecs;
	}

	public void processFlow() {
		flowComputation.computeGenericFlow( getRawData() );
	}

	public float getDefaultRadius() {
		return view.getDefaultPunctaRadius();
	}
}









