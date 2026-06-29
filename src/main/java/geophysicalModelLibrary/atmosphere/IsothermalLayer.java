package geophysicalModelLibrary.atmosphere;



/**
 * An {@link AtmosphericLayer} with zero lapse rate, so the temperature is constant throughout the layer.
 * <p>
 * Integrating the hydrostatic equation with  T = T_b  gives the exponential (barometric) law:
 * <br>
 *   P( H ) = P_b exp( -g ( H - H_b ) / ( R T_b ) )
 */
class IsothermalLayer
	extends AtmosphericLayer
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Coefficient of the exponential pressure law,  -g / ( R T_b ) . [1/m]
	 */
	private final double pressureScaleFactor;



	////////////////////////////////////////////////////////////////
	/// PACKAGE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@link IsothermalLayer}. Its lapse rate is zero by definition.
	 *
	 * @param baseGeopotentialAltitude	geopotential altitude of the bottom of the layer. [m]
	 * @param topGeopotentialAltitude	geopotential altitude of the top of the layer. [m]
	 * @param baseTemperature			(constant) temperature of the layer. [K]
	 * @param basePressure				pressure at the base of the layer. [Pa]
	 * @param constants					physical constants of the atmosphere.
	 */
	IsothermalLayer(
		double baseGeopotentialAltitude ,
		double topGeopotentialAltitude ,
		double baseTemperature ,
		double basePressure ,
		AtmosphericConstants constants
	) {
		super(
			baseGeopotentialAltitude ,
			topGeopotentialAltitude ,
			baseTemperature ,
			basePressure ,
			0.0 ,
			constants );
		this.pressureScaleFactor = -constants.gravity() / ( constants.specificGasConstant() * baseTemperature );
	}



	////////////////////////////////////////////////////////////////
	/// PROTECTED METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	protected double pressureAtGeopotentialAltitude( double geopotentialAltitude )
	{
		return this.basePressure * Math.exp( this.pressureScaleFactor * ( geopotentialAltitude - this.baseGeopotentialAltitude ) );
	}

}
