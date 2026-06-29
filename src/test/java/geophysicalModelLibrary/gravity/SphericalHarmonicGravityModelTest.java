package geophysicalModelLibrary.gravity;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import numericalLibrary.types.Vector3;



/**
 * Implements test methods for {@link SphericalHarmonicGravityModel}.
 * <p>
 * The abstract base class is exercised through its concrete subclass {@link Egm2008}, loaded from the {@code .gfc} file
 * shipped under {@code res/}. When that file is not present (it is large and not tracked in the repository),
 * the tests are skipped instead of failing.
 */
public class SphericalHarmonicGravityModelTest
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Path to the {@code res/EGM2008.gfc} file, resolved relative to the location of the compiled test classes so that
	 * the tests work regardless of the working directory they are launched from. It is {@code null} when the file is absent.
	 */
	private static final String GFC_PATH = locateGfcPath();

	/**
	 * Maximum degree loaded for the tests. It is large enough to be representative and small enough to load quickly.
	 */
	private static final int TEST_DEGREE = 360;



	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Model shared by all the test methods.
	 */
	private static SphericalHarmonicGravityModel model;



	////////////////////////////////////////////////////////////////
	/// TEST PREPARATION
	////////////////////////////////////////////////////////////////

	/**
	 * Loads the shared model once before all the tests.
	 */
	@BeforeAll
	static void loadModel() throws IOException
	{
		assumeTrue( GFC_PATH != null , "EGM2008.gfc not found; skipping." );
		model = Egm2008.fromFilePathAndMaximumDegree( GFC_PATH , TEST_DEGREE );
	}



	////////////////////////////////////////////////////////////////
	/// TEST METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Tests that the gravity magnitude on the reference sphere is close to the expected surface value.
	 * <p>
	 * A wrong normalization of the associated Legendre functions would make this value off by a large factor.
	 */
	@Test
	void gravityMagnitudeIsRealisticAtTheSurface()
	{
		double a = model.referenceRadius();
		for( double latitude=-90.0; latitude<=90.0; latitude+=15.0 ) {
			model.setPosition( positionFromGeocentric( latitude , 30.0 , a ) );
			double magnitude = model.getGravityVector().norm();
			assertTrue(  9.5 < magnitude  &&  magnitude < 10.0  ,
					"Unexpected gravity magnitude " + magnitude + " at latitude " + latitude );
		}
	}


	/**
	 * Tests that the gravity vector equals the gradient of the gravitational potential, computed by central finite differences.
	 */
	@Test
	void gravityVectorMatchesNumericalGradientOfPotential()
	{
		double a = model.referenceRadius();
		Vector3 point = positionFromGeocentric( 45.0 , 30.0 , a + 50000.0 );
		double h = 2.0;

		double dVdx = ( potentialAt( point.x()+h , point.y() , point.z() ) - potentialAt( point.x()-h , point.y() , point.z() ) ) / ( 2.0 * h );
		double dVdy = ( potentialAt( point.x() , point.y()+h , point.z() ) - potentialAt( point.x() , point.y()-h , point.z() ) ) / ( 2.0 * h );
		double dVdz = ( potentialAt( point.x() , point.y() , point.z()+h ) - potentialAt( point.x() , point.y() , point.z()-h ) ) / ( 2.0 * h );

		model.setPosition( point );
		Vector3 g = model.getGravityVector();
		double error = Math.sqrt( square( g.x()-dVdx ) + square( g.y()-dVdy ) + square( g.z()-dVdz ) );
		assertEquals( 0.0 , error / g.norm() , 1.0e-7 ,
				"Gravity vector does not match the numerical gradient of the potential." );
	}


	/**
	 * Tests that, far from the Earth, the field approaches the monopole  GM / r^2  pointing towards the origin.
	 */
	@Test
	void farFieldApproachesTheMonopole()
	{
		double r = 10.0 * model.referenceRadius();
		Vector3 point = positionFromGeocentric( 12.0 , 77.0 , r );
		model.setPosition( point );
		Vector3 g = model.getGravityVector();

		double expectedMagnitude = model.gravitationalParameter() / ( r * r );
		assertEquals( 1.0 , g.norm() / expectedMagnitude , 1.0e-4 );

		// The acceleration must point towards the origin: g must be antiparallel to the position.
		double cosAngle = g.dot( point ) / ( g.norm() * point.norm() );
		assertEquals( -1.0 , cosAngle , 1.0e-10 );

		// And the potential must approach  GM / r .
		assertEquals( 1.0 , model.getGravitationalPotential() * r / model.gravitationalParameter() , 1.0e-5 );
	}


	/**
	 * Tests that the same {@link Vector3} instance is returned on every call, with components reflecting the last position.
	 */
	@Test
	void gravityVectorInstanceIsReused()
	{
		double a = model.referenceRadius();
		model.setPosition( positionFromGeocentric( 10.0 , 20.0 , a ) );
		Vector3 first = model.getGravityVector();
		Vector3 second = model.getGravityVector();
		assertSame( first , second );

		model.setPosition( positionFromGeocentric( -40.0 , 100.0 , a ) );
		Vector3 third = model.getGravityVector();
		assertSame( first , third );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the path to {@code res/EGM2008.gfc}, or {@code null} if it cannot be found.
	 * <p>
	 * The {@code res} folder sits at the root of the {@code OpenGeophysicalModelLibrary-java} directory. The tests may be
	 * launched either from that directory (when it is the project) or from a parent repository that embeds it as a
	 * submodule, so the file is searched at both relative locations with respect to the working directory.
	 *
	 * @return	path to {@code res/EGM2008.gfc}, or {@code null} if it cannot be found.
	 */
	private static String locateGfcPath()
	{
		String[] candidatePaths = {
				"res/EGM2008.gfc",                                       // launched from OpenGeophysicalModelLibrary-java
				"lib/OpenGeophysicalModelLibrary-java/res/EGM2008.gfc" , // launched from a parent repository
		};
		for( String candidatePath : candidatePaths ) {
			if( Files.exists( Paths.get( candidatePath ) ) ) {
				return candidatePath;
			}
		}
		return null;
	}


	/**
	 * Returns the gravitational potential at the given Cartesian coordinates.
	 *
	 * @param x		first Cartesian coordinate in [m].
	 * @param y		second Cartesian coordinate in [m].
	 * @param z		third Cartesian coordinate in [m].
	 * @return	gravitational potential in [m^2/s^2].
	 */
	private static double potentialAt( double x , double y , double z )
	{
		model.setPosition( Vector3.fromComponents( x , y , z ) );
		return model.getGravitationalPotential();
	}


	/**
	 * Builds a position from geocentric latitude, longitude, and radius.
	 *
	 * @param latitudeDegrees	geocentric latitude in [deg].
	 * @param longitudeDegrees	longitude in [deg].
	 * @param radius			radius in [m].
	 * @return	position in the Earth-fixed Cartesian frame.
	 */
	private static Vector3 positionFromGeocentric( double latitudeDegrees , double longitudeDegrees , double radius )
	{
		double latitude = Math.toRadians( latitudeDegrees );
		double longitude = Math.toRadians( longitudeDegrees );
		return Vector3.fromComponents(
				radius * Math.cos( latitude ) * Math.cos( longitude ) ,
				radius * Math.cos( latitude ) * Math.sin( longitude ) ,
				radius * Math.sin( latitude ) );
	}


	/**
	 * Returns the square of the argument.
	 *
	 * @param value		value to be squared.
	 * @return	square of the argument.
	 */
	private static double square( double value )
	{
		return value * value;
	}

}
