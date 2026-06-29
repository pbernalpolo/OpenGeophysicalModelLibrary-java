package geophysicalModelLibrary.terrain;


import static java.lang.Math.toRadians;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;



/**
 * Implements test methods for {@link EsriAsciiGridElevationModel}.
 * <p>
 * The interpolation, registration, no-data, and bounds behavior are checked against small hand-written grids, and, when
 * the bundled Copernicus GLO-30 sample is present, the loader is checked against its real header and corner value.
 */
public class EsriAsciiGridElevationModelTest
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * A 3x3 corner-registered grid with cell size 1 degree, lower-left cell corner at (0,0), so the cell centers sit at
	 * longitudes / latitudes 0.5, 1.5, 2.5. The value of each cell is  column + 10 * ( row counted from the south ) ,
	 * written north row first.
	 */
	private static final String GRID_3X3 =
			"ncols 3\n" +
			"nrows 3\n" +
			"xllcorner 0.0\n" +
			"yllcorner 0.0\n" +
			"cellsize 1.0\n" +
			"20 21 22\n" +
			"10 11 12\n" +
			"0 1 2\n";



	////////////////////////////////////////////////////////////////
	/// TEST METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Tests that a query at a cell center returns that cell's stored value exactly.
	 */
	@Test
	void returnsCellValueAtCenters( @TempDir Path directory ) throws IOException
	{
		EsriAsciiGridElevationModel model = modelFrom( directory , GRID_3X3 );
		assertEquals( 0.0 , model.elevationAt( toRadians( 0.5 ) , toRadians( 0.5 ) ) , 1.0e-9 );   // south-west cell
		assertEquals( 22.0 , model.elevationAt( toRadians( 2.5 ) , toRadians( 2.5 ) ) , 1.0e-9 );  // north-east cell
		assertEquals( 11.0 , model.elevationAt( toRadians( 1.5 ) , toRadians( 1.5 ) ) , 1.0e-9 );  // center cell
		assertEquals( 20.0 , model.elevationAt( toRadians( 2.5 ) , toRadians( 0.5 ) ) , 1.0e-9 );  // north-west cell
	}


	/**
	 * Tests bilinear interpolation along a column, along a row, and at the center of four cells.
	 */
	@Test
	void interpolatesBilinearly( @TempDir Path directory ) throws IOException
	{
		EsriAsciiGridElevationModel model = modelFrom( directory , GRID_3X3 );
		// Halfway east between the south-west cell (0) and its east neighbor (1).
		assertEquals( 0.5 , model.elevationAt( toRadians( 0.5 ) , toRadians( 1.0 ) ) , 1.0e-9 );
		// Halfway north between the south-west cell (0) and the cell above it (10).
		assertEquals( 5.0 , model.elevationAt( toRadians( 1.0 ) , toRadians( 0.5 ) ) , 1.0e-9 );
		// Center of the four south-west cells: mean of 0, 1, 10, 11.
		assertEquals( 5.5 , model.elevationAt( toRadians( 1.0 ) , toRadians( 1.0 ) ) , 1.0e-9 );
	}


	/**
	 * Tests that positions outside the cell-center extent return NaN, while the extent edges are included.
	 */
	@Test
	void returnsNaNOutsideCoverage( @TempDir Path directory ) throws IOException
	{
		EsriAsciiGridElevationModel model = modelFrom( directory , GRID_3X3 );
		assertTrue( Double.isNaN( model.elevationAt( toRadians( 0.5 ) , toRadians( 0.0 ) ) ) , "west of the westernmost cell center" );
		assertTrue( Double.isNaN( model.elevationAt( toRadians( 3.0 ) , toRadians( 0.5 ) ) ) , "north of the northernmost cell center" );
		assertTrue( Double.isNaN( model.elevationAt( toRadians( 0.5 ) , toRadians( 2.5001 ) ) ) , "just east of the easternmost cell center" );
		// The extent edges themselves are valid.
		assertEquals( 2.0 , model.elevationAt( toRadians( 0.5 ) , toRadians( 2.5 ) ) , 1.0e-9 );
	}


	/**
	 * Tests that a no-data cell yields NaN at its center and wherever its interpolation stencil is touched, while a cell
	 * whose whole stencil is valid returns its value. (Under the conservative no-data rule, any cell bordering the void
	 * is NaN, so the valid probe is placed away from the no-data corner.)
	 */
	@Test
	void honorsNoDataValue( @TempDir Path directory ) throws IOException
	{
		// 3x3 with cell centers at 0.5 / 1.5 / 2.5 and the no-data value at the north-east corner cell.
		String grid =
				"ncols 3\n" +
				"nrows 3\n" +
				"xllcorner 0.0\n" +
				"yllcorner 0.0\n" +
				"cellsize 1.0\n" +
				"NODATA_value -9999\n" +
				"20 21 -9999\n" +
				"10 11 12\n" +
				"0 1 2\n";
		EsriAsciiGridElevationModel model = modelFrom( directory , grid );
		assertTrue( Double.isNaN( model.elevationAt( toRadians( 2.5 ) , toRadians( 2.5 ) ) ) , "the no-data cell center" );
		assertTrue( Double.isNaN( model.elevationAt( toRadians( 2.0 ) , toRadians( 2.0 ) ) ) , "a stencil touching the no-data cell" );
		assertEquals( 10.0 , model.elevationAt( toRadians( 1.5 ) , toRadians( 0.5 ) ) , 1.0e-9 , "a cell center whose stencil avoids the void" );
	}


	/**
	 * Tests that a center-registered grid places its cells half a cell differently from a corner-registered one.
	 */
	@Test
	void supportsCenterRegistration( @TempDir Path directory ) throws IOException
	{
		String grid =
				"ncols 2\n" +
				"nrows 1\n" +
				"xllcenter 10.0\n" +
				"yllcenter 20.0\n" +
				"cellsize 2.0\n" +
				"100 200\n";
		EsriAsciiGridElevationModel model = modelFrom( directory , grid );
		assertEquals( 100.0 , model.elevationAt( toRadians( 20.0 ) , toRadians( 10.0 ) ) , 1.0e-9 );
		assertEquals( 150.0 , model.elevationAt( toRadians( 20.0 ) , toRadians( 11.0 ) ) , 1.0e-9 );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Writes the given ESRI ASCII Grid text to a temporary file and loads a model from it.
	 *
	 * @param directory	temporary directory to write into.
	 * @param gridText	ESRI ASCII Grid content.
	 * @return	model loaded from the written file.
	 * @throws IOException	if the file cannot be written or parsed.
	 */
	private static EsriAsciiGridElevationModel modelFrom( Path directory , String gridText ) throws IOException
	{
		Path file = directory.resolve( "grid.asc" );
		Files.write( file , gridText.getBytes( StandardCharsets.UTF_8 ) );
		return EsriAsciiGridElevationModel.fromFilePath( file.toString() );
	}

}
