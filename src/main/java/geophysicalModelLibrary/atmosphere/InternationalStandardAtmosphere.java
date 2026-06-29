package geophysicalModelLibrary.atmosphere;



/**
 * The International Standard Atmosphere (ISA).
 * The piecewise model of Earth's atmosphere standardized by ISO/ICAO.
 * The layering implemented here is that of the U.S. Standard Atmosphere 1976,
 * valid up to 86 km geometric (84852 m geopotential), across seven layers of constant lapse rate.
 * <p>
 * Each layer is itself an {@link AtmosphericModel} (see {@link AtmosphericLayer});
 * this class composes the seven of them and dispatches each query to the one that covers the set altitude.
 * When every query is known to fall within a single layer, that layer can be obtained from the factory methods
 * ({@link #troposphere()}, {@link #tropopause()}, ...) and used directly, skipping the dispatch.
 * <p>
 * The atmosphere is set with a geometric altitude above mean sea level.
 * Outside the modeled band the nearest layer's law is extrapolated.
 * The temperatures and lapse rates of the seven layers are the tabulated constants of the standard;
 * each layer's base pressure is chained from the layer below (the pressure at their shared boundary),
 * so the pressure is exactly continuous across the layers.
 */
public class InternationalStandardAtmosphere
	implements AtmosphericModel
{
	////////////////////////////////////////////////////////////////
	/// PUBLIC CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Standard gravitational acceleration. [m/s^2]
	 */
	public static final double STANDARD_GRAVITY = 9.80665;

	/**
	 * Specific gas constant of dry air ( universal gas constant 8.31432 / molar mass 0.0289644 ). [J/(kg K)]
	 */
	public static final double SPECIFIC_GAS_CONSTANT = 287.0528;

	/**
	 * Ratio of specific heats of dry air, used for the speed of sound. [dimensionless]
	 */
	public static final double HEAT_CAPACITY_RATIO = 1.4;

	/**
	 * Nominal Earth radius relating geometric and geopotential altitude in the U.S. Standard Atmosphere 1976. [m]
	 */
	public static final double EARTH_RADIUS = 6356766.0;



	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Physical constants shared by all the standard layers.
	 */
	private static final AtmosphericConstants CONSTANTS =
			new AtmosphericConstants( STANDARD_GRAVITY , SPECIFIC_GAS_CONSTANT , HEAT_CAPACITY_RATIO , EARTH_RADIUS );



	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Layers in ascending order of altitude.
	 */
	private final AtmosphericModel[] layers;

	/**
	 * Geometric altitude of the top of each layer.
	 * Precomputed so the dispatch selects a layer directly from the set altitude,
	 * without a per-call geometric-to-geopotential conversion. [m]
	 */
	private final double[] geometricLayerTops;

	/**
	 * Layer covering the set altitude, to which the getters delegate.
	 */
	private AtmosphericModel selectedLayer;



	////////////////////////////////////////////////////////////////
	/// PUBLIC CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs the International Standard Atmosphere with its seven standard layers.
	 */
	public InternationalStandardAtmosphere()
	{
		this.layers = new AtmosphericModel[]{
				troposphere() ,
				tropopause() ,
				lowerStratosphere() ,
				upperStratosphere() ,
				stratopause() ,
				lowerMesosphere() ,
				upperMesosphere() ,
		};
		this.geometricLayerTops = new double[ this.layers.length ];
		for( int i=0; i<this.layers.length; i++ ) {
			// Every factory returns an AtmosphericLayer, so the cast is safe;
			// it gives access to the geopotential top.
			double geopotentialTop = ( (AtmosphericLayer) this.layers[i] ).topGeopotentialAltitude();
			this.geometricLayerTops[i] = InternationalStandardAtmosphere.geometricAltitude( geopotentialTop );
		}
		this.selectedLayer = this.layers[0];
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the troposphere layer (0 to 11 km, lapse rate -6.5 K/km) as a standalone model.
	 *
	 * @return	troposphere layer.
	 */
	public static AtmosphericModel troposphere()
	{
		return new GradientLayer(
			0.0 ,
			11000.0 ,
			288.15 ,
			101325.0 ,
			-0.0065 ,
			CONSTANTS );
	}


	/**
	 * Returns the tropopause layer (11 to 20 km, isothermal) as a standalone model.
	 *
	 * @return	tropopause layer.
	 */
	public static AtmosphericModel tropopause()
	{
		return new IsothermalLayer(
			11000.0 ,
			20000.0 ,
			216.65 ,
			basePressureFromLayerBelow( troposphere() , 11000.0 ) ,
			CONSTANTS );
	}


	/**
	 * Returns the lower stratosphere layer (20 to 32 km, lapse rate +1.0 K/km) as a standalone model.
	 *
	 * @return	lower stratosphere layer.
	 */
	public static AtmosphericModel lowerStratosphere()
	{
		return new GradientLayer(
			20000.0 ,
			32000.0 ,
			216.65 ,
			basePressureFromLayerBelow( tropopause() , 20000.0 ) ,
			0.001 ,
			CONSTANTS );
	}


	/**
	 * Returns the upper stratosphere layer (32 to 47 km, lapse rate +2.8 K/km) as a standalone model.
	 *
	 * @return	upper stratosphere layer.
	 */
	public static AtmosphericModel upperStratosphere()
	{
		return new GradientLayer(
			32000.0 ,
			47000.0 ,
			228.65 ,
			basePressureFromLayerBelow( lowerStratosphere() , 32000.0 ) ,
			0.0028 ,
			CONSTANTS );
	}


	/**
	 * Returns the stratopause layer (47 to 51 km, isothermal) as a standalone model.
	 *
	 * @return	stratopause layer.
	 */
	public static AtmosphericModel stratopause()
	{
		return new IsothermalLayer(
			47000.0 ,
			51000.0 ,
			270.65 ,
			basePressureFromLayerBelow( upperStratosphere() , 47000.0 ) ,
			CONSTANTS );
	}


	/**
	 * Returns the lower mesosphere layer (51 to 71 km, lapse rate -2.8 K/km) as a standalone model.
	 *
	 * @return	lower mesosphere layer.
	 */
	public static AtmosphericModel lowerMesosphere()
	{
		return new GradientLayer(
			51000.0 ,
			71000.0 ,
			270.65 ,
			basePressureFromLayerBelow( stratopause() , 51000.0 ) ,
			-0.0028 ,
			CONSTANTS );
	}


	/**
	 * Returns the upper mesosphere layer (71 to 84.852 km, lapse rate -2.0 K/km) as a standalone model.
	 *
	 * @return	upper mesosphere layer.
	 */
	public static AtmosphericModel upperMesosphere()
	{
		return new GradientLayer(
			71000.0 ,
			84852.0 ,
			214.65 ,
			basePressureFromLayerBelow( lowerMesosphere() , 71000.0 ) ,
			-0.002 ,
			CONSTANTS );
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	public void setAltitude( double altitude )
	{
		this.selectedLayer = this.layerForAltitude( altitude );
		this.selectedLayer.setAltitude( altitude );
	}


	/**
	 * {@inheritDoc}
	 */
	public double temperature()
	{
		return this.selectedLayer.temperature();
	}


	/**
	 * {@inheritDoc}
	 */
	public double pressure()
	{
		return this.selectedLayer.pressure();
	}


	/**
	 * {@inheritDoc}
	 */
	public double density()
	{
		return this.selectedLayer.density();
	}


	/**
	 * {@inheritDoc}
	 */
	public double speedOfSound()
	{
		return this.selectedLayer.speedOfSound();
	}


	/**
	 * {@inheritDoc}
	 */
	public double temperatureDerivative()
	{
		return this.selectedLayer.temperatureDerivative();
	}


	/**
	 * {@inheritDoc}
	 */
	public double pressureDerivative()
	{
		return this.selectedLayer.pressureDerivative();
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the pressure of the layer below at the geopotential altitude of the boundary it shares with the layer above,
	 * so the layer above can start from that pressure and the seam is exactly continuous.
	 *
	 * @param layerBelow				layer below the boundary.
	 * @param boundaryGeopotentialAltitude	geopotential altitude of the shared boundary. [m]
	 * @return	pressure at the boundary. [Pa]
	 */
	private static double basePressureFromLayerBelow( AtmosphericModel layerBelow , double boundaryGeopotentialAltitude )
	{
		layerBelow.setAltitude( geometricAltitude( boundaryGeopotentialAltitude ) );
		return layerBelow.pressure();
	}


	/**
	 * Returns the geometric altitude corresponding to a geopotential altitude.
	 *
	 * @param geopotentialAltitude	geopotential altitude. [m]
	 * @return	geometric altitude above mean sea level. [m]
	 */
	private static double geometricAltitude( double geopotentialAltitude )
	{
		return EARTH_RADIUS * geopotentialAltitude / ( EARTH_RADIUS - geopotentialAltitude );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the layer covering the given geometric altitude. Below the lowest layer the lowest is returned, and at or
	 * above the highest the highest is returned, so the nearest layer's law is extrapolated outside the modeled band.
	 *
	 * @param altitude	geometric altitude above mean sea level. [m]
	 * @return	layer to evaluate.
	 */
	private AtmosphericModel layerForAltitude( double altitude )
	{
		for( int i=0; i<this.layers.length; i++ ) {
			if( altitude < this.geometricLayerTops[i] ) {
				return this.layers[i];
			}
		}
		return this.layers[ this.layers.length - 1 ];
	}

}
