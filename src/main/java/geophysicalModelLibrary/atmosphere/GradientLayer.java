package geophysicalModelLibrary.atmosphere;



/**
 * An {@link AtmosphericLayer} with a non-zero lapse rate,
 * so the temperature changes linearly with geopotential altitude.
 * <p>
 * Integrating the hydrostatic equation with  T = T_b + L ( H - H_b )  gives the power law:
 * <br>
 *   P( H ) = P_b ( T( H ) / T_b )^( -g / ( R L ) )
 */
class GradientLayer
	extends AtmosphericLayer
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Exponent of the pressure power law,  -g / ( R L ) . [dimensionless]
	 */
	private final double pressureExponent;



	////////////////////////////////////////////////////////////////
	/// PACKAGE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs a {@link GradientLayer}.
	 *
	 * @param baseGeopotentialAltitude	geopotential altitude of the bottom of the layer. [m]
	 * @param topGeopotentialAltitude	geopotential altitude of the top of the layer. [m]
	 * @param baseTemperature			temperature at the base of the layer. [K]
	 * @param basePressure				pressure at the base of the layer. [Pa]
	 * @param lapseRate					rate of change of temperature with geopotential altitude (must be non-zero). [K/m]
	 * @param constants					physical constants of the atmosphere.
	 */
	GradientLayer(
		double baseGeopotentialAltitude ,
		double topGeopotentialAltitude ,
		double baseTemperature ,
		double basePressure ,
		double lapseRate ,
		AtmosphericConstants constants
	) {
		super(
			baseGeopotentialAltitude ,
			topGeopotentialAltitude ,
			baseTemperature ,
			basePressure ,
			lapseRate ,
			constants );
		this.pressureExponent = -constants.gravity() / ( constants.specificGasConstant() * lapseRate );
	}



	////////////////////////////////////////////////////////////////
	/// PROTECTED METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	protected double pressureAtGeopotentialAltitude( double geopotentialAltitude )
	{
		double temperatureRatio = this.temperatureAtGeopotentialAltitude( geopotentialAltitude ) / this.baseTemperature;
		return this.basePressure * Math.pow( temperatureRatio , this.pressureExponent );
	}

}
