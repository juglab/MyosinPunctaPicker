package de.csbd.learnathon.command;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

public class FlowVectorsCollection {

	private FlowVector onlySelectedFlowVector; //The only flow vector which is set as selected at a given time, can be null as well
	private ArrayList< FlowVector > sparseHandPickedFlow;
	private RandomAccessibleInterval< DoubleType > denseFlow;
	private ArrayList< FlowVector > spacedFlow;

	public FlowVectorsCollection() {
	}

	public void setOnlySelectedFlowVector( FlowVector f ) {
		onlySelectedFlowVector = f;
	}

	public FlowVector getOnlySelectedFlowVector() {
		for ( FlowVector flowVector : spacedFlow ) {
			if ( flowVector.isSelected() )
				setOnlySelectedFlowVector( flowVector );
		}
		return onlySelectedFlowVector;
	}

	public void setSparseHandPickedFlow( ArrayList< FlowVector > sparseHandPickedFlow ) {
		this.sparseHandPickedFlow = sparseHandPickedFlow;
	}

	public void setDenseFlow( RandomAccessibleInterval< DoubleType > denseFlow ) {
		this.denseFlow = denseFlow;
	}

	public void setSpacedFlow( ArrayList< FlowVector > spacedFlow ) {
		this.spacedFlow = spacedFlow;
	}

	public ArrayList< FlowVector > getSparsehandPickedFlowVectors() {
		return sparseHandPickedFlow;
	}

	public RandomAccessibleInterval< DoubleType > getDenseFlow() {
		return denseFlow;
	}

	public ArrayList< FlowVector > getSpacedFlowVectors() {
		return spacedFlow;
	}
}
