package geophysicalModelLibrary.atmosphere;



/**
 * The physical constants that parameterize a layered atmosphere:
 * the gravitational acceleration and the gas properties of the hydrostatic / ideal-gas relations,
 * plus the reference radius relating geometric and geopotential height.
 * <p>
 * Grouping them keeps the layer constructors short and lets every layer of one atmosphere share a single instance.
 * Immutable.
 */
class AtmosphericConstants
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Gravitational acceleration. [m/s^2]
	 */
	private final double gravity;

	/**
	 * Specific gas constant of the air ( universal gas constant / molar mass ). [J/(kg K)]
	 */
	private final double specificGasConstant;

	/**
	 * Ratio of specific heats of the air, used for the speed of sound. [dimensionless]
	 */
	private final double heatCapacityRatio;

	/**
	 * Reference radius relating geometric and geopotential height.
	 * The  r0  in  H = r0 z / ( r0 + z ) . [m]
	 */
	private final double geopotentialReferenceRadius;



	////////////////////////////////////////////////////////////////
	/// PACKAGE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@link AtmosphericConstants}.
	 *
	 * @param gravity						gravitational acceleration. [m/s^2]
	 * @param specificGasConstant			specific gas constant of the air. [J/(kg K)]
	 * @param heatCapacityRatio				ratio of specific heats of the air. [dimensionless]
	 * @param geopotentialReferenceRadius	reference radius relating geometric and geopotential height. [m]
	 */
	AtmosphericConstants( double gravity , double specificGasConstant , double heatCapacityRatio , double geopotentialReferenceRadius )
	{
		this.gravity = gravity;
		this.specificGasConstant = specificGasConstant;
		this.heatCapacityRatio = heatCapacityRatio;
		this.geopotentialReferenceRadius = geopotentialReferenceRadius;
	}



	////////////////////////////////////////////////////////////////
	/// PACKAGE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the gravitational acceleration.
	 *
	 * @return	gravitational acceleration. [m/s^2]
	 */
	double gravity()
	{
		return this.gravity;
	}


	/**
	 * Returns the specific gas constant of the air.
	 *
	 * @return	specific gas constant. [J/(kg K)]
	 */
	double specificGasConstant()
	{
		return this.specificGasConstant;
	}


	/**
	 * Returns the ratio of specific heats of the air.
	 *
	 * @return	ratio of specific heats. [dimensionless]
	 */
	double heatCapacityRatio()
	{
		return this.heatCapacityRatio;
	}


	/**
	 * Returns the reference radius relating geometric and geopotential height.
	 *
	 * @return	geopotential reference radius. [m]
	 */
	double geopotentialReferenceRadius()
	{
		return this.geopotentialReferenceRadius;
	}

}
