package de.csbd.learnathon.command;

public class ArrayUtils {

	public static double[] colsToRowsDoubleArrayReshape( double[] colsArray, int rows, int cols, int depth ) {

		double[] reshaped = new double[ cols * rows * depth ];

		for ( int d = 0; d < depth; d++ ) {
			for ( int c = 0; c < cols; c++ ) {
				for ( int r = 0; r < rows; r++ ) {
					reshaped[ ( r * cols + c ) + d * rows * cols ] = colsArray[ ( c * rows + r ) + d * rows * cols ];
				}
			}
		}
		return reshaped;
	}

	public static double[] rowsToColsDoubleArrayReshape( double[] colsArray, int rows, int cols, int depth ) {
		double[] reshaped = new double[ cols * rows * depth ];

		for ( int d = 0; d < depth; d++ ) {
			for ( int c = 0; c < cols; c++ ) {
				for ( int r = 0; r < rows; r++ ) {

					reshaped[ ( c * rows + r ) + d * rows * cols ] = colsArray[ ( r * cols + c ) + d * rows * cols ];
				}
			}
		}
		return reshaped;
	}
	
	public static byte[] colsToRowsByteArrayReshape( byte[] colsArray, int rows, int cols, int depth ) {

		byte[] reshaped = new byte[ cols * rows * depth ];

		for ( int d = 0; d < depth; d++ ) {
			for ( int c = 0; c < cols; c++ ) {
				for ( int r = 0; r < rows; r++ ) {
					reshaped[ ( r * cols + c ) + d * rows * cols ] = colsArray[ ( c * rows + r ) + d * rows * cols ];
				}
			}
		}
		return reshaped;
	}

	public static byte[] rowsToColsByteArrayReshape( byte[] colsArray, int rows, int cols, int depth ) {
		byte[] reshaped = new byte[ cols * rows * depth ];

		for ( int d = 0; d < depth; d++ ) {
			for ( int c = 0; c < cols; c++ ) {
				for ( int r = 0; r < rows; r++ ) {

					reshaped[ ( c * rows + r ) + d * rows * cols ] = colsArray[ ( r * cols + c ) + d * rows * cols ];
				}
			}
		}
		return reshaped;
	}

}
