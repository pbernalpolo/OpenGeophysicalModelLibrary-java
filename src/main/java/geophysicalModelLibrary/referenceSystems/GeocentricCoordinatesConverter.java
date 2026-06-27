package geophysicalModelLibrary.referenceSystems;


import numericalLibrary.types.Vector3;



/**
 * Converts between {@link GeocentricCoordinates} (geocentric latitude, longitude, radius)
 * and body-centered, body-fixed Cartesian coordinates.
 * <p>
 * Both conversions are exact and require no parameters (a sphere has no datum,
 * unlike the ellipsoid of {@link GeodeticCoordinatesConverter}),
 * so the converter is stateless and its methods are {@code static}.
 */
public class GeocentricCoordinatesConverter
{
	////////////////////////////////////////////////////////////////
	/// PUBLIC STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Converts a body-centered, body-fixed Cartesian position
	 * into geocentric spherical coordinates.
	 *
	 * @param position	Cartesian position in the body-centered, body-fixed frame. [m]
	 * @return	geocentric coordinates of the position.
	 */
	public static GeocentricCoordinates fromCartesian( Vector3 position )
	{
		double p = position.x() * position.x() + position.y() * position.y();
		double latitude = Math.atan2( position.z() , Math.sqrt( p ) );
		double longitude = Math.atan2( position.y() , position.x() );
		double radius = Math.sqrt( p + position.z() * position.z() );
		GeocentricCoordinates output = new GeocentricCoordinates();
		output.setLatitude( latitude );
		output.setLongitude( longitude );
		output.setRadius( radius );
		return output;
	}


	/**
	 * Converts geocentric spherical coordinates
	 * into a body-centered, body-fixed Cartesian position.
	 *
	 * @param coordinates	geocentric coordinates.
	 * @return	Cartesian position in the body-centered, body-fixed frame. [m]
	 */
	public static Vector3 toCartesian( GeocentricCoordinates coordinates )
	{
		double r = coordinates.getRadius();
		double phi = coordinates.getLatitude();
		double lambda = coordinates.getLongitude();
		double cos_phi = Math.cos( phi );
		double r_cos_phi = r * cos_phi;
		return Vector3.fromComponents(
				r_cos_phi * Math.cos( lambda ) ,
				r_cos_phi * Math.sin( lambda ) ,
				r * Math.sin( phi ) );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Prevents instantiation: this converter is stateless and used only through its static methods.
	 */
	private GeocentricCoordinatesConverter()
	{
	}

}
