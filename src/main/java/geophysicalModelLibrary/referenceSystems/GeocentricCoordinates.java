package geophysicalModelLibrary.referenceSystems;



/**
 * Geocentric spherical coordinates:
 * geocentric latitude, longitude, and radius (distance from the center).
 * <p>
 * Unlike {@link GeodeticCoordinates}, the reference surface is a sphere;
 * not an ellipsoid, so the latitude is the geocentric latitude
 * (the angle of the position vector above the equatorial plane)
 * and the third coordinate is the radius itself, not a height above any surface.
 * <p>
 * Convert to and from body-centered, body-fixed Cartesian coordinates with {@link GeocentricCoordinatesConverter}.
 */
public class GeocentricCoordinates implements GeographicCoordinates
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////
	
	/**
	 * Geocentric latitude (angle of the position vector with the equatorial plane).
	 * Usually denoted phi. [rad]
	 */
	private double latitude;

	/**
	 * Longitude.ç
	 * Usually denoted lambda. [rad]
	 */
	private double longitude;

	/**
	 * Radius: distance from the center to the point. [m]
	 */
	private double radius;



	////////////////////////////////////////////////////////////////
	/// PUBLIC CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs a {@link GeocentricCoordinates} with zero latitude, longitude, and radius.
	 * Set the values with the setters.
	 */
	public GeocentricCoordinates()
	{
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	public double getLatitude()
	{
		return this.latitude;
	}


	/**
	 * {@inheritDoc}
	 */
	public void setLatitude( double latitude )
	{
		this.latitude = latitude;
	}


	/**
	 * {@inheritDoc}
	 */
	public double getLongitude()
	{
		return this.longitude;
	}


	/**
	 * {@inheritDoc}
	 */
	public void setLongitude( double longitude )
	{
		this.longitude = longitude;
	}


	/**
	 * Returns the radius:
	 * the distance from the center to the point.
	 *
	 * @return	radius. [m]
	 */
	public double getRadius()
	{
		return this.radius;
	}


	/**
	 * Sets the radius:
	 * the distance from the center to the point.
	 *
	 * @param radius	radius. [m]
	 */
	public void setRadius( double radius )
	{
		this.radius = radius;
	}

}
