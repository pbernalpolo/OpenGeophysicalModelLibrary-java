package geophysicalModelLibrary.referenceSystems;


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import numericalLibrary.types.MatrixReal;



/**
 * Implements tests for {@link GeographicCoordinates}.
 */
class GeographicCoordinatesTest
{
    ////////////////////////////////////////////////////////////////
    /// TEST METHODS
    ////////////////////////////////////////////////////////////////

	/**
	 * Tests that {@link GeographicCoordinates#getOrientationEnuFromBcbf()} is correct.
	 */
	@Test
    void orientationEnuFromBcbf()
	{
		for( GeodeticCoordinates coordinates : GeographicCoordinatesTest.getGeographicCoordinatesList() ) {
			// Compute expected rotation matrix.
			double cosPhi = Math.cos( coordinates.getLatitude() );
			double sinPhi = Math.sin( coordinates.getLatitude() );
			double cosLambda = Math.cos( coordinates.getLongitude() );
			double sinLambda = Math.sin( coordinates.getLongitude() );
			MatrixReal R_ENU_ECEF_expected = MatrixReal.fromEntries3x3(
					-sinLambda           ,   cosLambda           ,  0.0 ,
					-sinPhi * cosLambda  ,  -sinPhi * sinLambda  ,  cosPhi ,
					cosPhi * cosLambda   ,   cosPhi * sinLambda  ,  sinPhi
					);
			// Get rotation matrix from returned quaternion.
			MatrixReal R_ENU_ECEF = coordinates.getOrientationEnuFromBcbf()
					.toRotationMatrix();
			// Compare them.
			assertTrue( R_ENU_ECEF.equalsApproximately( R_ENU_ECEF_expected , 1.0e-14 , 0.0 ) );
		}
	}


	/**
	 * Tests that {@link GeographicCoordinates#getOrientationNedFromBcbf()} is correct.
	 */
	@Test
    void orientationNedFromBcbf()
	{
		for( GeodeticCoordinates coordinates : GeographicCoordinatesTest.getGeographicCoordinatesList() ) {
			// Compute expected rotation matrix.
			double cosPhi = Math.cos( coordinates.getLatitude() );
			double sinPhi = Math.sin( coordinates.getLatitude() );
			double cosLambda = Math.cos( coordinates.getLongitude() );
			double sinLambda = Math.sin( coordinates.getLongitude() );
			MatrixReal R_NED_ECEF_expected = MatrixReal.fromEntries3x3(
					-sinPhi * cosLambda  ,  -sinPhi * sinLambda  ,   cosPhi ,
					-sinLambda           ,   cosLambda           ,   0.0 ,
					-cosPhi * cosLambda  ,  -cosPhi * sinLambda  ,  -sinPhi
					);
			// Get rotation matrix from returned quaternion.
			MatrixReal R_NED_ECEF = coordinates.getOrientationNedFromBcbf()
					.toRotationMatrix();
			// Compare them.
			assertTrue( R_NED_ECEF.equalsApproximately( R_NED_ECEF_expected , 1.0e-14 , 0.0 ) );
		}
	}



    ////////////////////////////////////////////////////////////////
    /// STATIC METHODS
    ////////////////////////////////////////////////////////////////

	/**
	 * @return	list of {@link GeodeticCoordinates} covering a wide range of values used for testing purposes.
	 */
	static List<GeodeticCoordinates> getGeographicCoordinatesList()
	{
		List<GeodeticCoordinates> output = new ArrayList<>();
		for( double h=0.0; h<100; h+=10.0 ) {
			for( double phi=0.0; phi<Math.PI/2.0; phi+=Math.PI/100.0 ) {
				for( double lambda=0.0; lambda<Math.PI; lambda+=Math.PI/100.0 ) {
					GeodeticCoordinates coordinates = new GeodeticCoordinates();
					coordinates.setLatitude( phi );
					coordinates.setLongitude( lambda );
					coordinates.setHeight( h );
					output.add( coordinates );
				}
			}
		}
		return output;
	}

}
