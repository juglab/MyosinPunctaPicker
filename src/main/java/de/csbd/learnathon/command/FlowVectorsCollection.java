package de.csbd.learnathon.command;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

public class FlowVectorsCollection {

	private FlowVector onlySelectedFlowVector; //The only flow vector which is set as selected at a given time, can be null as well
	private List< FlowVector > sparseHandPickedFlow;
	private List< FlowVector > autoFeatureFlow;
	private RandomAccessibleInterval denseFlow;
	private List< FlowVector > spacedFlow;
	private RandomAccessibleInterval< DoubleType > originalOpticalFlow;

	public FlowVectorsCollection() {}

	public void setOnlySelectedFlowVector( FlowVector f ) {
		onlySelectedFlowVector = f;
	}

	public FlowVector getOnlySelectedFlowVector() {
		if ( spacedFlow != null ) {
			for ( FlowVector flowVector : spacedFlow ) {
				if ( flowVector.isSelected() )
					setOnlySelectedFlowVector( flowVector );
			}
		}
		return onlySelectedFlowVector;
	}

	public void setSparseHandPickedFlow( List< FlowVector > sparseHandPickedFlow ) {
		this.sparseHandPickedFlow = sparseHandPickedFlow;
	}

	public void setAutoFeatureFlow( List< FlowVector > autoFeatureFlow ) {
		this.autoFeatureFlow = autoFeatureFlow;
	}

	public < T > void setDenseFlow( RandomAccessibleInterval< T > denseFlow ) {
		this.denseFlow = denseFlow;
	}

	public void setSpacedFlow( List< FlowVector > spacedFlow ) {
		this.spacedFlow = spacedFlow;
	}

	public List< FlowVector > getSparsehandPickedFlowVectors() {
		return sparseHandPickedFlow;
	}

	public List< FlowVector > getAutofeatureFlowVectors() {
		return autoFeatureFlow;
	}

	public < T > RandomAccessibleInterval< T > getDenseFlow() {
		return denseFlow;
	}

	public List< FlowVector > getSpacedFlowVectors() {
		return spacedFlow;
	}

	public List< FlowVector > getFlowVectorsAtTime( int t, List< FlowVector > flowtype ) {
		List< FlowVector > queriedFlowVectors = new ArrayList<>();
		for ( int i = 0; i < flowtype.size(); i++ ) {
			if ( flowtype.get( i ).getT() == t )
				queriedFlowVectors.add( flowtype.get( i ) );
		}
		return queriedFlowVectors;
	}

	public List< FlowVector > getEditedSpacedFlow() {
		List< FlowVector > editedFlow = new ArrayList<>();
		if ( spacedFlow != null ) {
			for ( FlowVector flowVector : spacedFlow ) {
				if ( flowVector.isEdited() )
					editedFlow.add( flowVector );
			}
		}
		return editedFlow;
	}

	public void setOriginalOpticalFlow( RandomAccessibleInterval< DoubleType > denseFlow ) {
		originalOpticalFlow = denseFlow;
	}

	public RandomAccessibleInterval< DoubleType > getOriginalOpticalFlow() {
		return originalOpticalFlow;
	}

}
