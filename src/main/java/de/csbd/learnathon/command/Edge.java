package de.csbd.learnathon.command;

public class Edge {

	Puncta pA, pB;

	public Edge( Puncta pA, Puncta pB ) {
		this.pA = pA;
		this.pB = pB;
	}

	Puncta getA() {
		return pA;
	}

	Puncta getB() {
		return pB;
	}

}
