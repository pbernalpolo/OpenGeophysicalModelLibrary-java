package geophysicalModelLibrary.referenceSystems;


import numericalLibrary.types.Vector3;



/**
 * Converts between geodetic coordinates (latitude, longitude, ellipsoidal height) and body-centered, body-fixed
 * Cartesian coordinates, for a given reference ellipsoid.
 * <p>
 * The ellipsoid is described by its equatorial radius  a  and the square of its first eccentricity  e^2 .
 * The forward conversion (geodetic to Cartesian) is exact;
 * the inverse (Cartesian to geodetic) uses Bowring's closed-form method.
 */
public class GeodeticCoordinatesConverter
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Equatorial radius (semi-major axis)  a  of the reference ellipsoid. [m]
	 */
	private final double equatorialRadius;

	/**
	 * Square of the first eccentricity  e^2  of the reference ellipsoid. [dimensionless]
	 */
	private final double eccentricitySquared;



	////////////////////////////////////////////////////////////////
	/// PUBLIC CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs a {@link GeodeticCoordinatesConverter} for the ellipsoid of the given geodetic reference system (for
	 * example {@link Wgs84}, the datum of GPS).
	 *
	 * @param referenceSystem	geodetic reference system providing the ellipsoid.
	 */
	public GeodeticCoordinatesConverter( GeodeticReferenceSystem referenceSystem )
	{
		this.equatorialRadius = referenceSystem.equatorialRadius();
		this.eccentricitySquared = referenceSystem.eccentricitySquared();
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Converts a body-centered, body-fixed Cartesian position into geodetic coordinates, using Bowring's method.
	 * <p>
	 * Unlike {@link #toCartesian(GeographicCoordinates)}, this conversion is not exact: Bowring's method is a very
	 * accurate closed-form approximation (sub-millimeter for near-surface points), not an exact inversion.
	 *
	 * @param position	Cartesian position in the body-centered, body-fixed frame. [m]
	 * @return	geodetic latitude and longitude [rad] and ellipsoidal height [m] of the position.
	 */
	public GeographicCoordinates fromCartesian( Vector3 position )
	{
		// We use Bowring's method.
		GeographicCoordinates output = new GeographicCoordinates();
		output.setLongitude(
				Math.atan2( position.y() , position.x() )
				);
		double p = Math.sqrt( position.x() * position.x() + position.y() * position.y() );
		double theta = Math.atan( position.z() / ( p * Math.sqrt( 1.0 - this.eccentricitySquared ) ) );
		double sin_theta = Math.sin( theta );
		double cos_theta = Math.cos( theta );
		double numerator = position.z() +
				this.equatorialRadius * this.eccentricitySquared / Math.sqrt( 1.0 - this.eccentricitySquared ) *
				sin_theta * sin_theta * sin_theta;
		double denominator = p - this.equatorialRadius * this.eccentricitySquared * cos_theta * cos_theta * cos_theta;
		double phi = Math.atan( numerator / denominator );
		output.setLatitude( phi );
		output.setHeight( p / Math.cos( phi ) - this.primeVerticalRadiusOfCurvature( phi ) );
		return output;
	}


	/**
	 * Converts geodetic coordinates into a body-centered, body-fixed Cartesian position.
	 * This conversion is exact.
	 *
	 * @param coordinates	geodetic latitude and longitude [rad] and ellipsoidal height [m].
	 * @return	Cartesian position in the body-centered, body-fixed frame. [m]
	 */
	public Vector3 toCartesian( GeographicCoordinates coordinates )
	{
		double phi = coordinates.getLatitude();
		double lambda = coordinates.getLongitude();
		double h = coordinates.getHeight();
		double N_phi = this.primeVerticalRadiusOfCurvature( phi );
		double r = N_phi + h;
		double cos_phi = Math.cos( phi );
		return Vector3.fromComponents(
				r * cos_phi * Math.cos( lambda ) ,
				r * cos_phi * Math.sin( lambda ) ,
				( ( 1.0 - this.eccentricitySquared ) * N_phi + h ) * Math.sin( phi ) );
				//( r - this.eccentricitySquared * N_phi ) * Math.sin( phi ) );  Same but less readable, and perhaps less numerically stable.
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the radius of curvature of the ellipsoid in the prime vertical at the given geodetic latitude.
	 *
	 * @param phi	geodetic latitude. [rad]
	 * @return	prime-vertical radius of curvature. [m]
	 */
	private double primeVerticalRadiusOfCurvature( double phi )
	{
		double sin_phi = Math.sin( phi );
		return this.equatorialRadius / Math.sqrt( 1.0 - this.eccentricitySquared * ( sin_phi * sin_phi ) );
	}

}
