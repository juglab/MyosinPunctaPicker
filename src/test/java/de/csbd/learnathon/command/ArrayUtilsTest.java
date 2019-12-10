package de.csbd.learnathon.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.Test;

public class ArrayUtilsTest {

	@Test
	public void colsToRowsArrayReshapeTest() {
		double[] reshaped;
		double[] colsArray = new double[] { 1.0, 3.0, 5.0, 2.0, 4.0, 6.0, 7.0, 9.0, 11.0, 8.0, 10.0, 12.0 };
		double[] rowsArray = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0 };

		reshaped = ArrayUtils.colsToRowsDoubleArrayReshape( colsArray, 3, 2, 2 );

		assertArrayEquals( rowsArray, reshaped );

	}

	@Test
	public void rowsToColsArrayReshapeTest() {
		double[] reshaped;
		double[] colsArray = new double[] { 1, 3, 5, 2, 4, 6, 7, 9, 11, 8, 10, 12 };
		double[] rowsArray = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

		reshaped = ArrayUtils.rowsToColsDoubleArrayReshape( rowsArray, 3, 2 ,2);
			
		assertArrayEquals( colsArray, reshaped );
	}

	/**
	 * Maps a 3 dimensional array into a single dimension.
	 * Places each row on top of each other.
	 */
	@Test
	public void matrixToRowsArrayTest() {
		double[][][] data = {
							  {
								{ 1, 2 },
								{ 3, 4 },
								{ 5, 6 }
							  },
							  {
								{ 7, 8 },
								{ 9, 10 },
								{ 11, 12 }
							  }
		};
		int rows = 3;
		int cols = 2;
		int depth = 2;
		double[] output = new double[ rows * cols * depth ];
		double[] expOutput = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

		for ( int d = 0; d < depth; d++ ) {
			for ( int r = 0; r < rows; r++ ) {
				for ( int c = 0; c < cols; c++ ) {
					output[ d * rows * cols + ( r * cols + c ) ] = data[ d ][ r ][ c ];
					System.out.println( ( ( r * cols + c ) + d * rows * cols ) + "," + data[ d ][ r ][ c ] );
				}
			}
		}
		for ( int i = 0; i < 12; i++ ) {
			System.out.println( output[ i ] );
		}
		assertArrayEquals( expOutput, output );
	}

	@Test
	public void matrixToColumnsArrayTest() {
		double[][][] data = {
							  {
								{ 1, 2 },
								{ 3, 4 },
								{ 5, 6 }
							  },
							  {
								{ 7, 8 },
								{ 9, 10 },
								{ 11, 12 }
							  }
		};
		int rows = 3;
		int cols = 2;
		int depth = 2;
		double[] output = new double[ rows * cols * depth ];
		double[] expOutput = new double[] { 1, 3, 5, 2, 4, 6, 7, 9, 11, 8, 10, 12 };

		for ( int d = 0; d < depth; d++ ) {
			for ( int c = 0; c < cols; c++ ) {
				for ( int r = 0; r < rows; r++ ) {
					output[ ( c * rows + r ) + d * rows * cols ] = data[ d ][ r ][ c ];
					System.out.println( ( ( c * rows + r ) + d * rows * cols ) + "," + data[ d ][ r ][ c ] );
				}
			}
		}

		for ( int i = 0; i < 12; i++ ) {
			System.out.println( output[ i ] );
		}

		assertArrayEquals( expOutput, output );

	}
}
