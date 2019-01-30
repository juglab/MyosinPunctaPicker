package de.csbd.learnathon.command;

public class Edge {

	private Puncta pA;
	private Puncta pB;

	private boolean isSelected;

	public Edge( Puncta pA, Puncta pB ) {
		this.pA = pA;
		this.pB = pB;
		this.isSelected = false;
	}

	Puncta getA() {
		return pA;
	}

	Puncta getB() {
		return pB;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected( boolean selected ) {
		this.isSelected = selected;
		pA.setSelected( selected );
		pB.setSelected( selected );
	}

}
