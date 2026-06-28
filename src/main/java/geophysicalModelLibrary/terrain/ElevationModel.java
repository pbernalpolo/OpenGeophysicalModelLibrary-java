package geophysicalModelLibrary.terrain;



/**
 * Represents a digital elevation model:
 * a queryable surface of terrain heights over the Earth.
 * <p>
 * Unlike the magnetic or gravity models, which are evaluated at a three-dimensional position,
 * an elevation is a function of horizontal position only,
 * so the model is queried with a geographic latitude and longitude
 * and returns the elevation at that point.
 * Implementations differ in their data source
 * (a high-resolution raster, a coarse global grid, tiled data, ...),
 * but all expose the same query.
 * <p>
 * The vertical datum (the reference the heights are measured from,
 * typically a geoid such as EGM2008, i.e. orthometric height above mean sea level)
 * and the surface they describe
 * (bare earth versus the visible surface including vegetation and buildings)
 * depend on the underlying dataset and are documented by each implementation.
 */
public interface ElevationModel
{
	/**
	 * Returns the terrain elevation at a geographic position, or {@link Double#NaN} where the model has no data
	 * (the position is outside the model's coverage, or the data contains a no-data value).
	 *
	 * @param latitudeDegrees	geodetic latitude. [deg]
	 * @param longitudeDegrees	longitude. [deg]
	 * @return	elevation above the model's vertical datum, or {@code NaN} if unavailable. [m]
	 */
	double elevationAt( double latitudeDegrees , double longitudeDegrees );
}
