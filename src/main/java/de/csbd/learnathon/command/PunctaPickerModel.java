package de.csbd.learnathon.command;

import java.util.List;

import net.imglib2.RandomAccessibleInterval;

public class PunctaPickerModel<T> {

	private RandomAccessibleInterval<T> rawData;

	private Graph graph = new Graph();
	private FlowVectorsCollection flowVectorsCol = new FlowVectorsCollection();
	private PunctaPickerController controller;
	private FlowController flowController;
	private FlowComputation flowComputation;
	private PunctaPickerView view;

	private RandomAccessibleInterval< T > denseFlow;

	private List< FlowVector > handPickedSparseFlow;

	private List< FlowVector > spacedFlow;

	PunctaPickerModel( RandomAccessibleInterval< T > img ) {
		this.rawData = img;
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

	public <T> RandomAccessibleInterval<T> getRawData() {
		return ( RandomAccessibleInterval< T > ) rawData;
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

	public List< FlowVector > extractAndInitializeControlVectorsFromHandPickedTracklets() {
		return flowComputation.initializeControlVectorsForFlow();
	}

	public void processSemiAutomatedFlow() {
		flowComputation.computeSemiAutoInterpolatedFlow( getRawData() );
	}

	public void processManuallyInterpolatedFlow() {
		flowComputation.computeManuallyInterpolatedFlow( getRawData() );
	}

	public float getDefaultRadius() {
		return view.getDefaultPunctaRadius();
	}

	public void modifyOpticalFlow( String opticalFlowMode ) {
		flowComputation.modifyOpticalFlowWithInterpolation( opticalFlowMode );
	}

	public void resetOpticalFlow() {
		flowComputation.resetOpticalFlow();

	}

	public List< RandomAccessibleInterval< T > > processOpticalFlowFernback( int numLevels, double pyrScale, boolean fastPyramids, int winSize, int numIters, int polyN, double polySigma, int flags ) {

		return flowComputation.computeOpticalFlowFernback( getRawData(), numLevels, pyrScale, fastPyramids, winSize, numIters, polyN, polySigma, flags );
	}

}
