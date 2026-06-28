package geophysicalModelLibrary.terrain;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;



/**
 * An {@link ElevationModel} backed by a single ESRI ASCII Grid (<i>Arc/Info ASCII Grid</i>, {@code .asc}) raster.
 * <p>
 * The ESRI ASCII Grid format is a plain-text header followed by
 * the elevation values in row-major order from the north row to the south row.
 * The header gives the grid size, the position of the lower-left cell, and the cell size:
 * <pre>
 *   ncols        587
 *   nrows        406
 *   xllcorner    -1.219027788889
 *   yllcorner    37.879583344444
 *   cellsize     0.000277777778
 *   NODATA_value -9999            (optional)
 *   68.12 67.72 66.80 ...
 *   ...
 * </pre>
 * The lower-left anchor may be given as a corner ({@code xllcorner} / {@code yllcorner},
 * the corner of the lower-left cell) or as a center ({@code xllcenter} / {@code yllcenter},
 * the center of the lower-left cell); both are accepted and reduced internally to cell centers.
 * A query is answered by bilinear interpolation among the four surrounding cell centers.
 * Positions outside the cell-center extent, or whose interpolation stencil
 * touches a no-data cell, return {@link Double#NaN}.
 * <p>
 * Coordinates are interpreted as geographic longitude (x) and latitude (y) on WGS84,
 * as distributed by <a href="https://opentopography.org">OpenTopography</a>
 * for the Copernicus GLO-30 (COP30) dataset.
 * That dataset is a Digital Surface Model (it includes vegetation and buildings, not bare earth)
 * with heights referenced to the EGM2008 geoid (orthometric height above mean sea level).
 */
public class EsriAsciiGridElevationModel
	implements ElevationModel
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Number of columns (west-east) and rows (north-south) of the grid.
	 */
	private final int columnCount;
	private final int rowCount;

	/**
	 * Cell size (grid spacing), the same in both directions. [deg]
	 */
	private final double cellSize;

	/**
	 * Longitude of the center of the westernmost column, and latitude of the center of the northernmost row. [deg]
	 */
	private final double westCenterLongitude;
	private final double northCenterLatitude;

	/**
	 * Elevation values in row-major order, north row first;
	 * no-data cells are stored as {@link Float#NaN}. [m]
	 */
	private final float[] elevations;



	////////////////////////////////////////////////////////////////
	/// PUBLIC STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Creates an {@link EsriAsciiGridElevationModel} from an ESRI ASCII Grid file.
	 *
	 * @param filePath	path to the {@code .asc} file.
	 * @return	{@link EsriAsciiGridElevationModel} loaded from the file.
	 * @throws IOException	if the file cannot be found, read, or parsed.
	 */
	public static EsriAsciiGridElevationModel fromFilePath( String filePath ) throws IOException
	{
		return EsriAsciiGridElevationModel.fromInputStream( new FileInputStream( filePath ) );
	}


	/**
	 * Creates an {@link EsriAsciiGridElevationModel} from an ESRI ASCII Grid stream.
	 * The stream is read fully and closed.
	 *
	 * @param gridStream	stream that provides the ESRI ASCII Grid contents.
	 * @return	{@link EsriAsciiGridElevationModel} loaded from the stream.
	 * @throws IOException	if the stream cannot be read or parsed.
	 */
	public static EsriAsciiGridElevationModel fromInputStream( InputStream gridStream ) throws IOException
	{
		try( BufferedReader reader = new BufferedReader( new InputStreamReader( gridStream , StandardCharsets.UTF_8 ) ) ) {
			return new EsriAsciiGridElevationModel( reader );
		}
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 * <p>
	 * The elevation is bilinearly interpolated among the four cell centers surrounding the position.
	 */
	public double elevationAt( double latitudeDegrees , double longitudeDegrees )
	{
		// Fractional column (west to east) and row (north to south) in cell-center coordinates.
		double fractionalColumn = ( longitudeDegrees - this.westCenterLongitude ) / this.cellSize;
		double fractionalRow = ( this.northCenterLatitude - latitudeDegrees ) / this.cellSize;
		if(  fractionalColumn < 0.0  ||  this.columnCount - 1 < fractionalColumn
			||  fractionalRow < 0.0  ||  this.rowCount - 1 < fractionalRow  ) {
			return Double.NaN;
		}

		int column0 = (int) Math.floor( fractionalColumn );
		int row0 = (int) Math.floor( fractionalRow );
		int column1 = Math.min( column0 + 1 , this.columnCount - 1 );
		int row1 = Math.min( row0 + 1 , this.rowCount - 1 );
		double weightColumn = fractionalColumn - column0;
		double weightRow = fractionalRow - row0;

		int indexRow0 = row0 * this.columnCount;
		int indexRow1 = row1 * this.columnCount;
		float topLeft = this.elevations[ indexRow0 + column0 ];
		float topRight = this.elevations[ indexRow0 + column1 ];
		float bottomLeft = this.elevations[ indexRow1 + column0 ];
		float bottomRight = this.elevations[ indexRow1 + column1 ];
		if(  Float.isNaN( topLeft )  ||  Float.isNaN( topRight )  ||  Float.isNaN( bottomLeft )  ||  Float.isNaN( bottomRight )  ) {
			return Double.NaN;
		}

		double top = topLeft + weightColumn * ( topRight - topLeft );
		double bottom = bottomLeft + weightColumn * ( bottomRight - bottomLeft );
		return top + weightRow * ( bottom - top );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@link EsriAsciiGridElevationModel} by parsing an ESRI ASCII Grid from a reader.
	 *
	 * @param reader	reader positioned at the start of the ESRI ASCII Grid.
	 * @throws IOException	if the content cannot be read or parsed.
	 */
	private EsriAsciiGridElevationModel( BufferedReader reader ) throws IOException
	{
		int columns = -1;
		int rows = -1;
		double cellSizeDegrees = Double.NaN;
		double anchorLongitude = Double.NaN;
		double anchorLatitude = Double.NaN;
		boolean anchorLongitudeIsCorner = false;
		boolean anchorLatitudeIsCorner = false;
		double noDataValue = Double.NaN;
		boolean hasNoDataValue = false;

		// Header: lines starting with a letter are key-value pairs; the first line starting with a number begins the data.
		String line;
		String firstDataLine = null;
		while( ( line = reader.readLine() ) != null ) {
			String trimmed = line.trim();
			if( trimmed.isEmpty() ) {
				continue;
			}
			if( !Character.isLetter( trimmed.charAt( 0 ) ) ) {
				firstDataLine = trimmed;
				break;
			}
			String[] keyValue = trimmed.split( "\\s+" , 2 );
			if( keyValue.length < 2 ) {
				throw new IOException( "Malformed ESRI ASCII Grid header line: " + trimmed );
			}
			String key = keyValue[0].toLowerCase();
			String value = keyValue[1].trim();
			switch( key ) {
				case "ncols":        columns = Integer.parseInt( value );  break;
				case "nrows":        rows = Integer.parseInt( value );  break;
				case "cellsize":     cellSizeDegrees = Double.parseDouble( value );  break;
				case "xllcorner":    anchorLongitude = Double.parseDouble( value );  anchorLongitudeIsCorner = true;  break;
				case "xllcenter":    anchorLongitude = Double.parseDouble( value );  anchorLongitudeIsCorner = false;  break;
				case "yllcorner":    anchorLatitude = Double.parseDouble( value );  anchorLatitudeIsCorner = true;  break;
				case "yllcenter":    anchorLatitude = Double.parseDouble( value );  anchorLatitudeIsCorner = false;  break;
				case "nodata_value": noDataValue = Double.parseDouble( value );  hasNoDataValue = true;  break;
				default:             throw new IOException( "Unknown ESRI ASCII Grid header key: " + keyValue[0] );
			}
		}

		if(  columns <= 0  ||  rows <= 0  ||  Double.isNaN( cellSizeDegrees )
				||  Double.isNaN( anchorLongitude )  ||  Double.isNaN( anchorLatitude )  ) {
			throw new IOException( "Incomplete ESRI ASCII Grid header (need ncols, nrows, cellsize, and a lower-left anchor)." );
		}

		this.columnCount = columns;
		this.rowCount = rows;
		this.cellSize = cellSizeDegrees;
		// Reduce the lower-left anchor to cell centers, then locate the west column and the north row.
		double southWestCenterLongitude = anchorLongitudeIsCorner ? anchorLongitude + 0.5 * cellSizeDegrees : anchorLongitude;
		double southWestCenterLatitude = anchorLatitudeIsCorner ? anchorLatitude + 0.5 * cellSizeDegrees : anchorLatitude;
		this.westCenterLongitude = southWestCenterLongitude;
		this.northCenterLatitude = southWestCenterLatitude + ( rows - 1 ) * cellSizeDegrees;

		this.elevations = readElevations( reader , firstDataLine , columns * rows , noDataValue , hasNoDataValue );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Reads the grid values, parsing the already-consumed first data line and then the rest of the reader, until the
	 * expected number of values has been collected. No-data values are stored as {@link Float#NaN}.
	 *
	 * @param reader			reader positioned just after the first data line.
	 * @param firstDataLine		the first data line, already read while detecting the end of the header.
	 * @param expectedCount		number of values expected ({@code ncols * nrows}).
	 * @param noDataValue		value that marks a missing cell.
	 * @param hasNoDataValue	whether a no-data value was declared in the header.
	 * @return	parsed elevation values, row-major, north row first. [m]
	 * @throws IOException	if the number of values does not match, or a value cannot be parsed.
	 */
	private static float[] readElevations( BufferedReader reader , String firstDataLine , int expectedCount ,
			double noDataValue , boolean hasNoDataValue ) throws IOException
	{
		float[] values = new float[ expectedCount ];
		int count = 0;
		String line = firstDataLine;
		while( line != null ) {
			String trimmed = line.trim();
			if( !trimmed.isEmpty() ) {
				for( String token : trimmed.split( "\\s+" ) ) {
					if( count >= expectedCount ) {
						throw new IOException( "ESRI ASCII Grid has more values than ncols * nrows = " + expectedCount );
					}
					double parsed = Double.parseDouble( token );
					values[ count++ ] = ( hasNoDataValue  &&  parsed == noDataValue ) ? Float.NaN : (float) parsed;
				}
			}
			line = reader.readLine();
		}
		if( count != expectedCount ) {
			throw new IOException( "ESRI ASCII Grid has " + count + " values, expected ncols * nrows = " + expectedCount );
		}
		return values;
	}

}
