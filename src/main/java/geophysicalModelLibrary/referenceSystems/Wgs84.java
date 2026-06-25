package geophysicalModelLibrary.referenceSystems;



/**
 * World Geodetic System 1984 (WGS84), maintained by the U.S. National Geospatial-Intelligence Agency (NGA).
 * <p>
 * WGS84 is the datum in which GPS positions are expressed,
 * so it is the right choice for coordinate conversions in a navigation system.
 * It is defined by  a ,  GM ,  omega , and the inverse flattening  1/f  (from which  J2  follows).
 * Its ellipsoid shares the semi-major axis  a  and angular velocity  omega  of {@link Grs80} and differs only in the
 * 9th significant digit of the flattening, so the two are interchangeable for navigation (sub-millimeter geometry).
 * <p>
 * Values from NGA TR8350.2, "Department of Defense World Geodetic System 1984".
 * See also
 * <a href="https://en.wikipedia.org/wiki/World_Geodetic_System#WGS_84">https://en.wikipedia.org/wiki/World_Geodetic_System#WGS_84</a>
 * (geometric ellipsoid: EPSG:7030).
 */
public final class Wgs84 implements GeodeticReferenceSystem
{
	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	public double equatorialRadius()
	{
		return 6378137.0;
	}


	/**
	 * {@inheritDoc}
	 */
	public double inverseFlattening()
	{
		// Defining constant.
		return 298.257223563;
	}


	/**
	 * {@inheritDoc}
	 */
	public double gravitationalParameter()
	{
		return 3.986004418e14;
	}


	/**
	 * {@inheritDoc}
	 */
	public double angularVelocity()
	{
		return 7.292115e-5;
	}


	/**
	 * {@inheritDoc}
	 */
	public double dynamicFormFactor()
	{
		// Derived from the defining normalized C_{2,0} = -0.484166774985e-3 ( J2 = -sqrt(5) C_{2,0} ).
		return 1.08262982131e-3;
	}


	/**
	 * {@inheritDoc}
	 */
	public double normalGravityAtEquator()
	{
		return 9.7803253359;
	}


	/**
	 * {@inheritDoc}
	 */
	public double normalGravityAtPole()
	{
		return 9.8321849378;
	}

}
