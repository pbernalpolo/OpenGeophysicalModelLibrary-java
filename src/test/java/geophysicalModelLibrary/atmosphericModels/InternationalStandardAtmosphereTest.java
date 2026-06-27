package geophysicalModelLibrary.atmosphericModels;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;



/**
 * Implements test methods for {@link InternationalStandardAtmosphere}.
 * <p>
 * The model is checked against the tabulated base values of the seven standard layers,
 * the well-known sea-level conditions,
 * the consistency of the analytic derivatives with finite differences,
 * and the behavior outside the modeled band.
 */
public class InternationalStandardAtmosphereTest
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Tabulated base values of the seven standard layers: { geopotential altitude [m], temperature [K], pressure [Pa] }.
	 */
	private static final double[][] LAYER_BASES = {
			{     0.0 , 288.15 , 101325.0   } ,
			{ 11000.0 , 216.65 ,  22632.06  } ,
			{ 20000.0 , 216.65 ,   5474.889 } ,
			{ 32000.0 , 228.65 ,    868.0187 } ,
			{ 47000.0 , 270.65 ,    110.9063 } ,
			{ 51000.0 , 270.65 ,     66.93887 } ,
			{ 71000.0 , 214.65 ,      3.956420 } ,
	};



	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Model under test.
	 */
	private final InternationalStandardAtmosphere atmosphere = new InternationalStandardAtmosphere();



	////////////////////////////////////////////////////////////////
	/// TEST METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Tests that the model reproduces the tabulated temperature and pressure at the base of each standard layer.
	 */
	@Test
	void reproducesStandardLayerBaseValues()
	{
		for( double[] base : LAYER_BASES ) {
			this.atmosphere.setAltitude( geometricAltitudeOfGeopotentialAltitude( base[0] ) );
			assertEquals( base[1] , this.atmosphere.temperature() , 1.0e-20 ,
					"temperature at geopotential " + base[0] );
			assertEquals( base[2] , this.atmosphere.pressure() , Math.abs( base[2] ) * 1.0e-5 ,
					"pressure at geopotential " + base[0] );
		}
	}


	/**
	 * Tests that a single layer, used standalone as an {@link AtmosphericModel},
	 * gives the same results as the composite within that layer's range.
	 */
	@Test
	void standaloneLayerMatchesTheComposite()
	{
		AtmosphericModel troposphere = InternationalStandardAtmosphere.troposphere();
		for( double altitude : new double[]{ 0.0 , 2500.0 , 8000.0 , 10900.0 } ) {
			this.atmosphere.setAltitude( altitude );
			troposphere.setAltitude( altitude );
			assertEquals( this.atmosphere.temperature() , troposphere.temperature() , 1.0e-9 );
			assertEquals( this.atmosphere.pressure() , troposphere.pressure() , 1.0e-9 );
			assertEquals( this.atmosphere.density() , troposphere.density() , 1.0e-12 );
			assertEquals( this.atmosphere.pressureDerivative() , troposphere.pressureDerivative() , 1.0e-9 );
		}
	}


	/**
	 * Tests the well-known sea-level standard conditions.
	 */
	@Test
	void reproducesSeaLevelStandardConditions()
	{
		this.atmosphere.setAltitude( 0.0 );
		assertEquals( 288.15 , this.atmosphere.temperature() , 1.0e-6 );
		assertEquals( 101325.0 , this.atmosphere.pressure() , 1.0e-6 );
		assertEquals( 1.225 , this.atmosphere.density() , 1.0e-3 );
		assertEquals( 340.294 , this.atmosphere.speedOfSound() , 1.0e-2 );
	}


	/**
	 * Tests that the pressure derivative at sea level matches the hydrostatic value  -rho g .
	 */
	@Test
	void pressureDerivativeIsHydrostaticAtSeaLevel()
	{
		this.atmosphere.setAltitude( 0.0 );
		double expected = -this.atmosphere.density() * InternationalStandardAtmosphere.STANDARD_GRAVITY;
		assertEquals( expected , this.atmosphere.pressureDerivative() , 1.0e-6 );
		assertEquals( -0.0065 , this.atmosphere.temperatureDerivative() , 1.0e-9 );
	}


	/**
	 * Tests that the analytic temperature and pressure derivatives match central finite differences,
	 * across gradient and isothermal layers.
	 */
	@Test
	void analyticDerivativesMatchFiniteDifferences()
	{
		double delta = 1.0;
		for( double altitude : new double[]{ 0.0 , 5000.0 , 15000.0 , 25000.0 , 45000.0 , 60000.0 } ) {
			this.atmosphere.setAltitude( altitude + delta );
			double temperatureAbove = this.atmosphere.temperature();
			double pressureAbove = this.atmosphere.pressure();
			this.atmosphere.setAltitude( altitude - delta );
			double temperatureBelow = this.atmosphere.temperature();
			double pressureBelow = this.atmosphere.pressure();

			double numericTemperatureDerivative = ( temperatureAbove - temperatureBelow ) / ( 2.0 * delta );
			double numericPressureDerivative = ( pressureAbove - pressureBelow ) / ( 2.0 * delta );

			this.atmosphere.setAltitude( altitude );
			assertEquals( numericTemperatureDerivative , this.atmosphere.temperatureDerivative() , 1.0e-6 ,
					"dT/dz at " + altitude );
			assertEquals( numericPressureDerivative , this.atmosphere.pressureDerivative() , Math.abs( numericPressureDerivative ) * 1.0e-4 + 1.0e-9 ,
					"dP/dz at " + altitude );
		}
	}


	/**
	 * Tests that the pressure decreases monotonically with altitude, including the extrapolated bands below sea level and
	 * above the modeled top, and that the extrapolated values stay finite and positive.
	 */
	@Test
	void pressureDecreasesMonotonicallyAndExtrapolates()
	{
		double previousPressure = Double.POSITIVE_INFINITY;
		for( double altitude=-2000.0; altitude<=120000.0; altitude+=2000.0 ) {
			this.atmosphere.setAltitude( altitude );
			double pressure = this.atmosphere.pressure();
			assertTrue( Double.isFinite( pressure )  &&  pressure > 0.0 , "non-positive or non-finite pressure at " + altitude );
			assertTrue( pressure < previousPressure , "pressure not decreasing at " + altitude );
			previousPressure = pressure;
		}
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the geometric altitude corresponding to a geopotential altitude, the inverse of the model's internal conversion.
	 *
	 * @param geopotentialAltitude	geopotential altitude. [m]
	 * @return	geometric altitude above mean sea level. [m]
	 */
	private static double geometricAltitudeOfGeopotentialAltitude( double geopotentialAltitude )
	{
		double r = InternationalStandardAtmosphere.EARTH_RADIUS;
		return r * geopotentialAltitude / ( r - geopotentialAltitude );
	}

}
