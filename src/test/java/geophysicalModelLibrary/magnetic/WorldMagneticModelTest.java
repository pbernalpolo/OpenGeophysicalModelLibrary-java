package geophysicalModelLibrary.magnetic;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import geophysicalModelLibrary.referenceSystems.GeocentricCoordinates;
import geophysicalModelLibrary.referenceSystems.GeocentricCoordinatesConverter;
import geophysicalModelLibrary.referenceSystems.GeodeticCoordinates;
import geophysicalModelLibrary.referenceSystems.GeodeticCoordinatesConverter;
import geophysicalModelLibrary.referenceSystems.Wgs84;
import numericalLibrary.types.Vector3;



/**
 * Implements test methods for {@link WorldMagneticModel}.
 * <p>
 * The model is loaded from the {@code WMM.COF} file shipped under {@code res/magnetic/WMM2025COF/}
 * and validated against every row of the official NCEI {@code WMM2025_TestValues.txt}.
 * Each geodetic test point is converted to a Cartesian position with the {@link Wgs84} ellipsoid.
 * The field is evaluated and rotated into the local north/east/down frame,
 * and the resulting X, Y, Z are compared to the published values.
 * When the files are absent the tests are skipped instead of failing.
 */
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
public class WorldMagneticModelTest
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Path to the {@code WMM.COF} coefficient file, or {@code null} when it is absent.
	 */
	private static final String COF_PATH = locate( "WMM.COF" );

	/**
	 * Path to the official {@code WMM2025_TestValues.txt} file, or {@code null} when it is absent.
	 */
	private static final String TEST_VALUES_PATH = locate( "WMM2025_TestValues.txt" );

	/**
	 * Tolerance for the comparison with the official test values. [nT]
	 */
	private static final double TOLERANCE_NANOTESLA = 1.0e-3;

	/**
	 * The model now returns the field in SI tesla; the official test values are in nanotesla, so the model output is
	 * scaled by this factor before the comparison.
	 */
	private static final double TESLA_TO_NANOTESLA = 1.0e9;



	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Model shared by all the test methods.
	 */
	private WorldMagneticModel model;

	/**
	 * Converter used to turn the geodetic test points into Cartesian positions.
	 */
	private final GeodeticCoordinatesConverter geodeticCoordinatesConverter = new GeodeticCoordinatesConverter( new Wgs84() );



	////////////////////////////////////////////////////////////////
	/// TEST PREPARATION
	////////////////////////////////////////////////////////////////

	/**
	 * Loads the shared model once before all the tests.
	 */
	@BeforeAll
	void loadModel() throws IOException
	{
		assumeTrue( COF_PATH != null , "WMM.COF not found; skipping." );
		model = WorldMagneticModel.fromFilePath( COF_PATH );
	}



	////////////////////////////////////////////////////////////////
	/// TEST METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Tests that the model reproduces every row of the official NCEI test values
	 * (which also covers the secular variation, since the file spans several dates within the validity window).
	 * Each row is checked in turn, so a failure stops at, and reports, the offending point.
	 */
	@Test
	void reproducesOfficialTestValues() throws IOException
	{
		assumeTrue( TEST_VALUES_PATH != null , "WMM test values not found; skipping." );
		List<double[]> testValues = readTestValues( TEST_VALUES_PATH );
		assertTrue( !testValues.isEmpty() , "No test values were parsed from " + TEST_VALUES_PATH );

		for( double[] testValue : testValues ) {
			checkAgainstTestValue( testValue );
		}
	}


	/**
	 * Tests that the field magnitude on the reference sphere lies in the plausible range of Earth's surface field.
	 * <p>
	 * A wrong normalization of the associated Legendre functions would make this value off by a large factor.
	 */
	@Test
	void fieldMagnitudeIsRealisticAtTheSurface()
	{
		for( double latitude=-90.0; latitude<=90.0; latitude+=15.0 ) {
			model.setPosition( positionOnReferenceSphere( latitude , 45.0 ) );
			double magnitude = model.getMagneticField().norm() * TESLA_TO_NANOTESLA;
			assertTrue(  20000.0 < magnitude  &&  magnitude < 70000.0  ,
					"Unexpected field magnitude " + magnitude + " nT at latitude " + latitude );
		}
	}


	/**
	 * Tests that the radial field points into the Earth in the north and out of it in the south, as the dominant dipole requires.
	 */
	@Test
	void verticalComponentFollowsTheDipole()
	{
		double radialNorth = radialComponent( 80.0 , 0.0 );
		double radialSouth = radialComponent( -80.0 , 0.0 );
		assertTrue( radialNorth < 0.0 , "Radial field should point inward in the north; found " + radialNorth );
		assertTrue( radialSouth > 0.0 , "Radial field should point outward in the south; found " + radialSouth );
	}


	/**
	 * Tests that changing the evaluation year changes the coefficients (secular variation is applied).
	 */
	@Test
	void secularVariationChangesTheCoefficients()
	{
		model.setDecimalYear( model.epoch() );
		double atEpoch = model.gaussG( 1 , 0 );
		model.setDecimalYear( model.epoch() + 1.0 );
		double oneYearLater = model.gaussG( 1 , 0 );
		assertNotEquals( atEpoch , oneYearLater , "Secular variation should change g(1,0) over a year." );
		model.setDecimalYear( model.epoch() );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Evaluates the field at one test point and asserts that its north, east, and down components match the published ones.
	 *
	 * @param testValue		{ decimal year, altitude [km], geodetic latitude [deg], geodetic longitude [deg], X [nT], Y [nT], Z [nT] }.
	 */
	private void checkAgainstTestValue( double[] testValue )
	{
		double year = testValue[0];
		double altitudeMeters = testValue[1] * 1000.0;
		double latitude = Math.toRadians( testValue[2] );
		double longitude = Math.toRadians( testValue[3] );

		GeodeticCoordinates coordinates = new GeodeticCoordinates();
		coordinates.setLatitude( latitude );
		coordinates.setLongitude( longitude );
		coordinates.setHeight( altitudeMeters );

		model.setDecimalYear( year );
		model.setPosition( geodeticCoordinatesConverter.toCartesian( coordinates ) );

		// Express the Cartesian field in the local north/east/down frame at the geodetic location.
		Vector3 fieldNed = coordinates.getOrientationNedFromBcbf().rotate( model.getMagneticField() );

		String where = "year " + year + " , lat " + testValue[2] + " , lon " + testValue[3];
		assertEquals( testValue[4] , fieldNed.x() * TESLA_TO_NANOTESLA , TOLERANCE_NANOTESLA , "X at " + where );
		assertEquals( testValue[5] , fieldNed.y() * TESLA_TO_NANOTESLA , TOLERANCE_NANOTESLA , "Y at " + where );
		assertEquals( testValue[6] , fieldNed.z() * TESLA_TO_NANOTESLA , TOLERANCE_NANOTESLA , "Z at " + where );
	}
	
	
	/**
	 * Returns the radial (outward) component of the field at a point on the geomagnetic reference sphere.
	 *
	 * @param latitudeDegrees	geocentric latitude. [deg]
	 * @param longitudeDegrees	longitude. [deg]
	 * @return	radial component of the field. [nT]
	 */
	private double radialComponent( double latitudeDegrees , double longitudeDegrees )
	{
		Vector3 position = positionOnReferenceSphere( latitudeDegrees , longitudeDegrees );
		model.setPosition( position );
		return model.getMagneticField().dot( position.normalizeInplace() );
	}


	/**
	 * Builds a Cartesian position on the geomagnetic reference sphere from geocentric latitude and longitude.
	 *
	 * @param latitudeDegrees	geocentric latitude. [deg]
	 * @param longitudeDegrees	longitude. [deg]
	 * @return	position in the Earth-fixed Cartesian frame. [m]
	 */
	private Vector3 positionOnReferenceSphere( double latitudeDegrees , double longitudeDegrees )
	{
		GeocentricCoordinates coordinates = new GeocentricCoordinates();
		coordinates.setLatitude( Math.toRadians( latitudeDegrees ) );
		coordinates.setLongitude( Math.toRadians( longitudeDegrees ) );
		coordinates.setRadius( WorldMagneticModel.GEOMAGNETIC_REFERENCE_RADIUS );
		return GeocentricCoordinatesConverter.toCartesian( coordinates );
	}
	
	
	
	////////////////////////////////////////////////////////////////
	/// PRIVATE STATIC METHODS
	////////////////////////////////////////////////////////////////
	
	/**
	 * Reads the official test-value file, keeping the columns needed to drive and check the model.
	 * <p>
	 * The file format is documented in its own header: field 1 is the decimal year, 2 the altitude [km], 3 and 4 the
	 * geodetic latitude and longitude [deg], and 8, 9, 10 the X, Y, Z components [nT].
	 *
	 * @param path	path to the test-value file.
	 * @return	list of { decimal year, altitude [km], latitude [deg], longitude [deg], X [nT], Y [nT], Z [nT] }.
	 * @throws IOException	if the file cannot be read.
	 */
	private static List<double[]> readTestValues( String path ) throws IOException
	{
		List<double[]> testValues = new ArrayList<>();
		for( String line : Files.readAllLines( Paths.get( path ) ) ) {
			String trimmed = line.trim();
			if(  trimmed.isEmpty()  ||  trimmed.startsWith( "#" )  ) {
				continue;
			}
			String[] token = trimmed.split( "\\s+" );
			testValues.add( new double[]{
					Double.parseDouble( token[0] ) ,   // decimal year
					Double.parseDouble( token[1] ) ,   // altitude [km]
					Double.parseDouble( token[2] ) ,   // geodetic latitude [deg]
					Double.parseDouble( token[3] ) ,   // geodetic longitude [deg]
					Double.parseDouble( token[7] ) ,   // X [nT]
					Double.parseDouble( token[8] ) ,   // Y [nT]
					Double.parseDouble( token[9] ) ,   // Z [nT]
			} );
		}
		return testValues;
	}
	
	
	/**
	 * Returns the path to a file under the {@code res/WMM2025COF/} folder, or {@code null} if it cannot be found.
	 * <p>
	 * The folder is searched relative to the working directory, so the data is found whether the tests run in the
	 * standalone library (its own {@code res/}) or in a parent repository that embeds it as a submodule (the parent's
	 * {@code res/}).
	 *
	 * @param fileName	name of the file inside {@code res/magnetic/WMM2025COF/}.
	 * @return	path to the file, or {@code null} if it cannot be found.
	 */
	private static String locate( String fileName )
	{
		Path candidate = Paths.get( "res/magnetic/WMM2025COF/" + fileName );
		return Files.exists( candidate ) ? candidate.toString() : null;
	}
	
}
