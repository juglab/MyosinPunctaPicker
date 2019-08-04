package de.csbd.learnathon.command;

public class BlendingFunctions {

	public static interface Blender {
		/**
		 * Alpha gives weighting to GT flow and is a function of distance r and
		 * returns a factor between 0 and 1.
		 */
		float getAlpha( float r );

		/**
		 * Beta gives weighting to underlying Optical flow and is a function of
		 * distance r and returns a factor between 0 and 1.
		 */
		float getBeta( float r );
	}

	public static class PreferGroundTruthFlow implements Blender {

		@Override
		public float getAlpha( float r ) {
			return 1f;
		}

		@Override
		public float getBeta( float r ) {
			return 0f;
		}
	}

	public static class LinearlyBlendedFlow implements Blender {

		final float windowSize;

		public LinearlyBlendedFlow( float windowSize ) {
			this.windowSize = windowSize;
		}

		@Override
		public float getAlpha( float r ) {
			return ( ( windowSize - r ) / windowSize );
		}

		@Override
		public float getBeta( float r ) {
			return ( r / windowSize );
		}
	}
	
	public static class GaussianBlendedFlow implements Blender {

		final float windowSize;
		private float alpha;
		private float sigma;

		public GaussianBlendedFlow( float windowSize ) {
			this.windowSize = windowSize;
			this.sigma = windowSize / 6; // The effect of gaussian is mostly within 3 sigma and the gaussian spreads half way across window size
		}

		@Override
		public float getAlpha( float r ) {
			alpha = ( float ) Math.exp( -( r * r ) / ( 2 * sigma * sigma ) );
			return alpha;
		}

		@Override
		public float getBeta( float r ) {
			return ( 1 - alpha );
		}
	}

	public static class GaussianSmoothedFlow implements Blender {

		final float windowSize;
		private float alpha;
		private float sigma;

		public GaussianSmoothedFlow( float windowSize ) {
			this.windowSize = windowSize;
			this.sigma = windowSize; // The effect of gaussian is mostly within 3 sigma and the gaussian spreads half way across window size
		}

		@Override
		public float getAlpha( float r ) {
			alpha = ( float ) Math.exp( -( r * r ) / ( 2 * sigma * sigma ) );
			return alpha;
		}

		@Override
		public float getBeta( float r ) {
			return 0f;
		}
	}

}
