package geophysicalModelLibrary.referenceSystems;



/**
 * Geodetic coordinates:
 * latitude and longitude referred to the normal of a reference ellipsoid,
 * plus the height above that ellipsoid.
 * This is the datum in which GPS positions are expressed.
 * <p>
 * Immutable. Convert to and from body-centered, body-fixed Cartesian coordinates with {@link GeodeticCoordinatesConverter},
 * which supplies the ellipsoid.
 */
public class GeodeticCoordinates
	implements GeographicCoordinates
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Geodetic latitude (angle of the ellipsoid normal with the equatorial plane).
	 * Usually denoted phi. [rad]
	 */
	private double latitude;

	/**
	 * Longitude.
	 * Usually denoted lambda. [rad]
	 */
	private double longitude;

	/**
	 * Height above the reference ellipsoid.
	 * Usually denoted h. [m]
	 */
	private double height;



	////////////////////////////////////////////////////////////////
	/// PUBLIC CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs a {@link GeodeticCoordinates} with zero latitude, longitude, and height.
	 * Set the values with the setters.
	 */
	public GeodeticCoordinates()
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
	 * Returns the height above the reference ellipsoid.
	 *
	 * @return	height above the reference ellipsoid. [m]
	 */
	public double getHeight()
	{
		return this.height;
	}
	
	
	/**
	 * Sets the height above the reference ellipsoid.
	 *
	 * @param	height above the reference ellipsoid. [m]
	 */
	public void setHeight( double height )
	{
		this.height = height;
	}
	
}
