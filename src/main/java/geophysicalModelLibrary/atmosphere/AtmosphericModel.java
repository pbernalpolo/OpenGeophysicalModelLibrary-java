package geophysicalModelLibrary.atmosphere;



/**
 * A model of a planetary atmosphere as a function of altitude above mean sea level (AMSL).
 * <p>
 * The evaluation point is set once with {@link #setAltitude(double)}.
 * After setting altitude the quantities are read with the no-argument getters.
 * Every quantity is a function of the geometric altitude,
 * and the derivatives are taken with respect to it.
 * <p>
 * Implementations need not be thread-safe.
 */
public interface AtmosphericModel
{
	/**
	 * Sets the geometric altitude above mean sea level at which the atmosphere is evaluated.
	 *
	 * @param altitude	geometric altitude above mean sea level. [m]
	 */
	void setAltitude( double altitude );


	/**
	 * Returns the temperature at the set altitude.
	 *
	 * @return	temperature. [K]
	 */
	double temperature();


	/**
	 * Returns the pressure at the set altitude.
	 *
	 * @return	pressure. [Pa]
	 */
	double pressure();


	/**
	 * Returns the density at the set altitude.
	 *
	 * @return	density. [kg/m^3]
	 */
	double density();


	/**
	 * Returns the speed of sound at the set altitude.
	 *
	 * @return	speed of sound. [m/s]
	 */
	double speedOfSound();


	/**
	 * Returns the derivative of the temperature with respect to the geometric altitude, at the set altitude.
	 *
	 * @return	temperature derivative. [K/m]
	 */
	double temperatureDerivative();


	/**
	 * Returns the derivative of the pressure with respect to the geometric altitude, at the set altitude.
	 *
	 * @return	pressure derivative. [Pa/m]
	 */
	double pressureDerivative();

}
