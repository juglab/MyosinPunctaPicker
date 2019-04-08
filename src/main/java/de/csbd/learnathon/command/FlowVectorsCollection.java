package de.csbd.learnathon.command;

import java.util.ArrayList;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class FlowVectorsCollection {

	private FlowVector onlySelectedFlowVector; //The only flow vector which is set as selected at a given time, can be null as well
	private ArrayList< FlowVector > sparseHandPickedFlow;
	private ArrayList< FlowVector > autoFeatureFlow;
	private RandomAccessibleInterval denseFlow;
	private ArrayList< FlowVector > spacedFlow;
	private ArrayList< FlowVector > editedFlow = new ArrayList<>();

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

	public void setAutoFeatureFlow( ArrayList< FlowVector > autoFeatureFlow ) {
		this.autoFeatureFlow = autoFeatureFlow;
	}

	public < T extends RealType< T > & NativeType< T > > void setDenseFlow( RandomAccessibleInterval< T > denseFlow ) {
		this.denseFlow = denseFlow;
	}

	public void setSpacedFlow( ArrayList< FlowVector > spacedFlow ) {
		this.spacedFlow = spacedFlow;
	}

	public ArrayList< FlowVector > getSparsehandPickedFlowVectors() {
		return sparseHandPickedFlow;
	}

	public ArrayList< FlowVector > getAutofeatureFlowVectors() {
		return autoFeatureFlow;
	}

	public RandomAccessibleInterval< DoubleType > getDenseFlow() {
		return denseFlow;
	}

	public ArrayList< FlowVector > getSpacedFlowVectors() {
		return spacedFlow;
	}

	public ArrayList< FlowVector > getFlowVectorsAtTime( int t, ArrayList< FlowVector > flowtype ) {
		ArrayList< FlowVector > queriedFlowVectors = new ArrayList<>();
		for ( int i = 0; i < flowtype.size(); i++ ) {
			if ( flowtype.get( i ).getT() == t )
				queriedFlowVectors.add( flowtype.get( i ) );
		}
		return queriedFlowVectors;
	}

	public ArrayList< FlowVector > getEditedSpacedFlow() {
		if ( !( spacedFlow == null ) ) {
			for ( FlowVector flowVector : spacedFlow ) {
				if ( flowVector.isEdited() )
					editedFlow.add( flowVector );
			}
			return editedFlow;
		}
		else
			return null;
	}

	public ArrayList< FlowVector > getDenseFlowVectors() { //Only experimental, needs deletion later
		ArrayList< FlowVector > dfvec = new ArrayList<>();
		RandomAccessibleInterval< DoubleType > df = getDenseFlow();
		int count = 0;
		for ( int slice = 0; slice < df.dimension( 2 ); slice = slice + 2 ) {
			IntervalView< DoubleType > u = Views.hyperSlice( df, 2, slice );
			IntervalView< DoubleType > v = Views.hyperSlice( df, 2, slice+1 );
			RandomAccess< DoubleType > ura = u.randomAccess();
			RandomAccess< DoubleType > vra = v.randomAccess();
			for ( int x = 0; x < df.dimension( 0 ); x++ ) {
				for(int y = 0; y < df.dimension( 1 ); y++) {
					ura.setPosition( x, 0 );
					ura.setPosition( y, 1 );
					vra.setPosition( x, 0 );
					vra.setPosition( y, 1 );
					dfvec.add( new FlowVector( x, y, count, ura.get().getRealDouble(), vra.get().getRealDouble() ) );
				}
			}
			count = count + 1;
		}
		return dfvec;
	}
}
