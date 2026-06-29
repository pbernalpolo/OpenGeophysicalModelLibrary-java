package geophysicalModelLibrary.atmosphere;



/**
 * One layer of a layered atmosphere, and a complete {@link AtmosphericModel} over its band:
 * within the layer the temperature varies linearly with geopotential altitude at a constant lapse rate,
 * and the pressure follows from the hydrostatic equation.
 * <p>
 * Because a single layer is itself a model, it can be used directly when every query altitude is known
 * to fall within its range, skipping the layer dispatch of {@link InternationalStandardAtmosphere}.
 * As an {@link AtmosphericModel} it is set with a geometric altitude above mean sea level
 * and converts internally to the geopotential altitude the layer formulas use:
 * H = r0 z / ( r0 + z ) ,
 * with
 * <ul>
 * <li> H  the geopotential altitude,
 * <li> r0  the reference radius,
 * <li> z  the geometric altitude
 * </ul>
 * <p>
 * The temperature and the (hydrostatic) pressure derivative are the same for every layer and live here;
 * only the pressure law splits on whether the lapse rate is zero, so it is left abstract
 * and provided by {@link GradientLayer} and {@link IsothermalLayer}.
 */
abstract class AtmosphericLayer
	implements AtmosphericModel
{
	////////////////////////////////////////////////////////////////
	/// PROTECTED VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Geopotential altitude of the bottom of the layer. [m]
	 */
	protected final double baseGeopotentialAltitude;

	/**
	 * Geopotential altitude of the top of the layer. [m]
	 */
	protected final double topGeopotentialAltitude;

	/**
	 * Temperature at the base of the layer. [K]
	 */
	protected final double baseTemperature;

	/**
	 * Pressure at the base of the layer. [Pa]
	 */
	protected final double basePressure;

	/**
	 * Rate of change of temperature with geopotential altitude (0 for an isothermal layer). [K/m]
	 */
	protected final double lapseRate;

	/**
	 * Physical constants of the atmosphere this layer belongs to.
	 */
	protected final AtmosphericConstants constants;



	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Geometric altitude above mean sea level at which the layer is evaluated. [m]
	 */
	private double altitude;

	/**
	 * Temperature at the set altitude. [K]
	 */
	private double temperature;

	/**
	 * Pressure at the set altitude. [Pa]
	 */
	private double pressure;

	/**
	 * Derivative of the geopotential altitude with respect to the geometric altitude, at the set altitude;
	 * the factor that turns a derivative with respect to geopotential altitude into one with respect to geometric altitude. [dimensionless]
	 */
	private double d_H_geopotential_over_d_h_geometric;

	/**
	 * Whether the state below must be recomputed because the altitude changed.
	 */
	private boolean dirty = true;



	////////////////////////////////////////////////////////////////
	/// PROTECTED CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@link AtmosphericLayer}.
	 *
	 * @param baseGeopotentialAltitude	geopotential altitude of the bottom of the layer. [m]
	 * @param topGeopotentialAltitude	geopotential altitude of the top of the layer. [m]
	 * @param baseTemperature			temperature at the base of the layer. [K]
	 * @param basePressure				pressure at the base of the layer. [Pa]
	 * @param lapseRate					rate of change of temperature with geopotential altitude. [K/m]
	 * @param constants					physical constants of the atmosphere.
	 */
	protected AtmosphericLayer(
		double baseGeopotentialAltitude ,
		double topGeopotentialAltitude ,
		double baseTemperature ,
		double basePressure ,
		double lapseRate ,
		AtmosphericConstants constants
	) {
		this.baseGeopotentialAltitude = baseGeopotentialAltitude;
		this.topGeopotentialAltitude = topGeopotentialAltitude;
		this.baseTemperature = baseTemperature;
		this.basePressure = basePressure;
		this.lapseRate = lapseRate;
		this.constants = constants;
	}



	////////////////////////////////////////////////////////////////
	/// PROTECTED ABSTRACT METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the pressure at the given geopotential altitude (the lapse-rate-dependent pressure law).
	 *
	 * @param geopotentialAltitude	geopotential altitude. [m]
	 * @return	pressure. [Pa]
	 */
	protected abstract double pressureAtGeopotentialAltitude( double geopotentialAltitude );



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	public void setAltitude( double altitude )
	{
		this.altitude = altitude;
		this.dirty = true;
	}


	/**
	 * {@inheritDoc}
	 */
	public double temperature()
	{
		this.clean();
		return this.temperature;
	}


	/**
	 * {@inheritDoc}
	 */
	public double pressure()
	{
		this.clean();
		return this.pressure;
	}


	/**
	 * {@inheritDoc}
	 */
	public double density()
	{
		this.clean();
		// rho = P / ( R T )
		return this.pressure / ( this.constants.specificGasConstant() * this.temperature );
	}


	/**
	 * {@inheritDoc}
	 */
	public double speedOfSound()
	{
		this.clean();
		return Math.sqrt( this.constants.heatCapacityRatio() * this.constants.specificGasConstant() * this.temperature );
	}


	/**
	 * {@inheritDoc}
	 */
	public double temperatureDerivative()
	{
		this.clean();
		// dT/dh = ( dT/dH )( dH/dh ) .
		return this.lapseRate * this.d_H_geopotential_over_d_h_geometric;
	}


	/**
	 * {@inheritDoc}
	 */
	public double pressureDerivative()
	{
		this.clean();
		// dP/dh = ( dP/dH )( dH/dh ) , with the hydrostatic  dP/dH = -P g / ( R T ) .
		double pressureDerivativeOverGeopotential = -this.pressure * this.constants.gravity()
				/ ( this.constants.specificGasConstant() * this.temperature );
		return pressureDerivativeOverGeopotential * this.d_H_geopotential_over_d_h_geometric;
	}


	/**
	 * Returns the geopotential altitude of the top of the layer.
	 *
	 * @return	geopotential altitude of the top of the layer. [m]
	 */
	public double topGeopotentialAltitude()
	{
		return this.topGeopotentialAltitude;
	}



	////////////////////////////////////////////////////////////////
	/// PROTECTED METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the temperature at the given geopotential altitude.
	 *
	 * @param geopotentialAltitude	geopotential altitude. [m]
	 * @return	temperature. [K]
	 */
	protected double temperatureAtGeopotentialAltitude( double geopotentialAltitude )
	{
		return this.baseTemperature + this.lapseRate * ( geopotentialAltitude - this.baseGeopotentialAltitude );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////
	
	/**
	 * Recomputes the quantities common to the getters (temperature, pressure, and the geopotential-to-geometric
	 * derivative factor) for the set altitude, unless they are already up to date.
	 * The getters then derive the specific quantities (density, speed of sound, derivatives) from these on demand.
	 */
	private void clean()
	{
		if( !this.dirty ) {
			return;
		}
		double referenceRadius = this.constants.geopotentialReferenceRadius();
		double radiusRatio = referenceRadius / ( referenceRadius + this.altitude );
		double geopotentialAltitude = this.altitude * radiusRatio;
		this.d_H_geopotential_over_d_h_geometric = radiusRatio * radiusRatio;
		this.temperature = this.temperatureAtGeopotentialAltitude( geopotentialAltitude );
		this.pressure = this.pressureAtGeopotentialAltitude( geopotentialAltitude );
		this.dirty = false;
	}

}
