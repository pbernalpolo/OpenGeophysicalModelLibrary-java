package geophysicalModelLibrary.referenceSystems;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import numericalLibrary.types.Vector3;


/**
 * Implements tests for {@link GeodeticCoordinatesConverter}.
 */
class GeodeticCoordinatesConverterTest
{
    ////////////////////////////////////////////////////////////////
    /// TEST METHODS
    ////////////////////////////////////////////////////////////////

	/**
	 * Tests that zero latitude and zero longitude produces a {@link Vector3} along the x axis.
	 */
	@Test
    void zeroLatitudeZeroLongitudeProducesPositionAlongPositiveX()
	{
		GeodeticCoordinatesConverter converter = new GeodeticCoordinatesConverter( new Wgs84() );
		GeodeticCoordinates coordinates = new GeodeticCoordinates();
		coordinates.setLatitude( 0.0 );
		coordinates.setLongitude( 0.0 );
		coordinates.setHeight( 1.0 );
		Vector3 cartesian = converter.toCartesian( coordinates );
		double theta = cartesian.angleFrom( Vector3.i() );
		assertEquals( 0.0 , theta );
	}


	/**
	 * Tests that transforming from some {@link GeodeticCoordinates} to a {@link Vector3} and then back to {@link GeodeticCoordinates}
	 * results in the initial {@link GeodeticCoordinates}.
	 */
	@Test
    void toCartesianAndBackToGeodeticProducesInitialCoordinates()
	{
		GeodeticCoordinatesConverter converter = new GeodeticCoordinatesConverter( new Wgs84() );
		for( GeodeticCoordinates coordinatesInitial : GeographicCoordinatesTest.getGeographicCoordinatesList() ) {
			Vector3 cartesian = converter.toCartesian( coordinatesInitial );
			GeodeticCoordinates coordinatesFinal = converter.fromCartesian( cartesian );
			assertEquals( coordinatesInitial.getLongitude() , coordinatesFinal.getLongitude() , 1.0e-15 );
			assertEquals( coordinatesInitial.getLatitude() , coordinatesFinal.getLatitude() , 1.0e-15 );
			assertEquals( coordinatesInitial.getHeight() , coordinatesFinal.getHeight() , 1.0e-8 );
		}
	}


	/**
	 * Tests that transforming from some {@link Vector3} to {@link GeodeticCoordinates} and then back to {@link Vector3}
	 * results in the initial {@link Vector3}.
	 */
	@Test
    void toGeodeticAndBackToCartesianProducesInitialCoordinates()
	{
		GeodeticCoordinatesConverter converter = new GeodeticCoordinatesConverter( new Wgs84() );
		for( Vector3 coordinatesInitial : GeodeticCoordinatesConverterTest.getPositionList() ) {
			GeodeticCoordinates cartesian = converter.fromCartesian( coordinatesInitial );
			Vector3 coordinatesFinal = converter.toCartesian( cartesian );
			assertEquals( coordinatesInitial.x() , coordinatesFinal.x() , 1.0e-8 );
			assertEquals( coordinatesInitial.y() , coordinatesFinal.y() , 1.0e-8 );
			assertEquals( coordinatesInitial.z() , coordinatesFinal.z() , 1.0e0 );
		}
	}



    ////////////////////////////////////////////////////////////////
    /// STATIC METHODS
    ////////////////////////////////////////////////////////////////

	/**
	 * @return	list of random {@link Vector3} used for testing purposes.
	 */
	static List<Vector3> getPositionList()
	{
		List<Vector3> output = new ArrayList<>();
		Random randomNumberGenerator = new Random( 42 );
		for( int i=0; i<100; i++ ) {
			output.add(
					Vector3.random( randomNumberGenerator ).scaleInplace( 6378137.0 + 1000 )
					);
		}
		return output;
	}

}
