package geophysicalModelLibrary.referenceSystems;



/**
 * Geodetic Reference System 1980 (GRS80), adopted by the International Union of Geodesy and Geophysics (IUGG).
 * <p>
 * GRS80 is defined by four constants — semi-major axis  a , gravitational parameter  GM , dynamic form factor  J2 ,
 * and angular velocity  omega  — from which the flattening and the normal gravity field follow.
 * It underlies most modern national datums (NAD83, ETRS89, ...) and is the reference field of the EGM geoid models.
 * Its ellipsoid is almost identical to that of {@link Wgs84} (same  a ; the flattenings differ in the 9th significant digit).
 * <p>
 * Values from Moritz, H. (2000), "Geodetic Reference System 1980", Journal of Geodesy 74, 128-133.
 * See also
 * <a href="https://en.wikipedia.org/wiki/Geodetic_Reference_System_1980">https://en.wikipedia.org/wiki/Geodetic_Reference_System_1980</a>
 * (geometric ellipsoid: EPSG:7019).
 */
public final class Grs80 implements GeodeticReferenceSystem
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
		// Derived constant (GRS80 defines J2 rather than f); listed by Moritz (2000).
		return 298.257222101;
	}


	/**
	 * {@inheritDoc}
	 */
	public double gravitationalParameter()
	{
		return 3.986005e14;
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
		// Defining constant: J2 = 108263e-8.
		return 1.0826300e-3;
	}


	/**
	 * {@inheritDoc}
	 */
	public double normalGravityAtEquator()
	{
		return 9.7803267715;
	}


	/**
	 * {@inheritDoc}
	 */
	public double normalGravityAtPole()
	{
		return 9.8321863685;
	}

}
