package geophysicalModelLibrary.referenceSystems;



/**
 * Reference system of an astronomical body: a rotating "level ellipsoid" that defines both the reference shape of the
 * body and its normal (reference) gravity field. Although the concrete systems provided here ({@link Grs80},
 * {@link Wgs84}) are Earth systems, the abstraction applies equally to other planets, the Moon, or the Sun.
 * <p>
 * A geodetic reference system is fixed by a small set of <i>defining</i> constants:
 * <ul>
 * <li> equatorial radius  a  (the semi-major axis of the oblate ellipsoid),
 * <li> gravitational parameter  GM ,
 * <li> second-degree dynamic form factor  J2  (the unnormalized degree-2 zonal of the normal field),
 * <li> angular velocity  omega ,
 * <li> flattening  f  (geometrically equivalent to  J2  for an equipotential ellipsoid;
 * 		the official definitions list both, so both are exposed as primary constants rather than derived from each other).
 * </ul>
 * Everything else is <i>derived</i> from those constants and provided here as default methods:
 * <ul>
 * <li> the geometry of the ellipsoid (eccentricity, semi-minor axis), used to convert between geodetic and Cartesian
 *      coordinates (see {@link GeodeticCoordinatesConverter});
 * <li> the fully normalized even zonal harmonics  C_{2n,0}  of the normal field, which a measured gravity model
 *      subtracts to isolate the disturbing potential, and hence the geoid undulation / height anomaly;
 * <li> the normal gravity  gamma  on the surface of the ellipsoid (Somigliana's formula).
 * </ul>
 * <p>
 * Concrete systems implement this interface as independent siblings, not by inheriting from one another:
 * one such system is not a behavioral specialization of another (Liskov substitution principle).
 * Their only shared code is the derivation captured by the default methods.
 */
public interface GeodeticReferenceSystem
{
	////////////////////////////////////////////////////////////////
	/// DEFINING CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the equatorial radius  a  of the ellipsoid:
	 * the distance from the center to the surface, orthogonal to the rotation axis.
	 * For an oblate body this is the semi-major axis.
	 *
	 * @return	equatorial radius  a . [m]
	 */
	double equatorialRadius();


	/**
	 * Returns the inverse flattening  1/f  of the ellipsoid.
	 *
	 * @return	inverse flattening  1/f . [dimensionless]
	 */
	double inverseFlattening();


	/**
	 * Returns the gravitational parameter  GM  of the normal field.
	 *
	 * @return	gravitational parameter  GM . [m^3/s^2]
	 */
	double gravitationalParameter();


	/**
	 * Returns the angular velocity  omega  of the rotating reference body.
	 *
	 * @return	angular velocity  omega . [rad/s]
	 */
	double angularVelocity();


	/**
	 * Returns the second-degree dynamic form factor  J2  (unnormalized) of the normal field.
	 *
	 * @return	dynamic form factor  J2 . [dimensionless]
	 */
	double dynamicFormFactor();


	/**
	 * Returns the normal gravity at the equator  gamma_e .
	 *
	 * @return	normal gravity at the equator  gamma_e . [m/s^2]
	 */
	double normalGravityAtEquator();


	/**
	 * Returns the normal gravity at the poles  gamma_p .
	 *
	 * @return	normal gravity at the poles  gamma_p . [m/s^2]
	 */
	double normalGravityAtPole();



	////////////////////////////////////////////////////////////////
	/// DERIVED GEOMETRY
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the flattening  f = ( a - b ) / a  of the ellipsoid.
	 *
	 * @return	flattening  f . [dimensionless]
	 */
	default double flattening()
	{
		return 1.0 / this.inverseFlattening();
	}


	/**
	 * Returns the polar radius  b  of the ellipsoid:
	 * the distance from the center to the surface, along the rotation axis.
	 * For an oblate body this is the semi-minor axis.
	 *
	 * @return	polar radius  b . [m]
	 */
	default double polarRadius()
	{
		return this.equatorialRadius() * ( 1.0 - this.flattening() );
	}


	/**
	 * Returns the square of the first eccentricity  e^2 = f ( 2 - f )  of the ellipsoid.
	 *
	 * @return	first eccentricity squared  e^2 . [dimensionless]
	 */
	default double eccentricitySquared()
	{
		double f = this.flattening();
		return f * ( 2.0 - f );
	}



	////////////////////////////////////////////////////////////////
	/// DERIVED NORMAL FIELD
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the normal gravity on the surface of the ellipsoid at the given geodetic latitude,
	 * using Somigliana's closed-form formula.
	 *
	 * @param geodeticLatitude	geodetic latitude. [rad]
	 * @return	normal gravity  gamma  on the ellipsoid surface. [m/s^2]
	 */
	default double normalGravity( double geodeticLatitude )
	{
		double cos2 = Math.cos( geodeticLatitude );
		cos2 *= cos2;
		double sin2 = 1.0 - cos2;
		double a = this.equatorialRadius();
		double b = this.polarRadius();
		double numerator = a * this.normalGravityAtEquator() * cos2 + b * this.normalGravityAtPole() * sin2;
		double denominator = Math.sqrt( a * a * cos2 + b * b * sin2 );
		return numerator / denominator;
	}


	/**
	 * Returns the fully normalized even zonal harmonic coefficient  C_{l,0}  of the normal field at the given degree;
	 * it returns 0 at odd degrees and at degree 0 (the normal field is zonal and symmetric about the equator).
	 * <p>
	 * The values follow from the closed-form expression of the even zonal harmonics  J_{2n}  of an equipotential
	 * ellipsoid (Heiskanen and Moritz, "Physical Geodesy", eq. 2-92), converted to the geodetic fully normalized
	 * convention with  C_{l,0} = -J_l / sqrt( 2 l + 1 ) .
	 * A measured gravity model subtracts them to remove the normal field when computing the disturbing potential.
	 * <p>
	 * The even zonal series is in fact infinite, but successive terms shrink by a factor of about  e^2  (roughly 400x
	 * every two degrees):  J_2 ~ 1e-3 ,  J_4 ~ 1e-6 ,  J_6 ~ 1e-8 ,  J_8 ~ 1e-11 ,  J_10 ~ 1e-14 , ...
	 * This implementation returns the terms up to degree 8 and truncates the rest to 0;
	 * degree 10 and beyond contribute well under a micrometer to the geoid undulation.
	 * The formula is valid at any even degree, so the cutoff could be raised if a future use ever required it.
	 *
	 * @param degree	harmonic degree  l .
	 * @return	fully normalized even zonal coefficient  C_{l,0}  of the normal field. [dimensionless]
	 */
	default double normalizedEvenZonalCoefficient( int degree )
	{
		// Even degrees only; the (infinite) series is truncated at degree 8, as higher terms are negligible.
		if(  degree < 2  ||  degree > 8  ||  ( degree & 1 ) == 1  ) {
			return 0.0;
		}
		int n = degree / 2;
		double e2 = this.eccentricitySquared();
		double sign = ( ( n & 1 ) == 0 ) ? -1.0 : 1.0;   // (-1)^{n+1}
		double j2n = sign * ( 3.0 * Math.pow( e2 , n ) ) / ( ( 2.0 * n + 1.0 ) * ( 2.0 * n + 3.0 ) )
				* ( 1.0 - n + 5.0 * n * this.dynamicFormFactor() / e2 );
		return -j2n / Math.sqrt( 2.0 * degree + 1.0 );
	}

}
