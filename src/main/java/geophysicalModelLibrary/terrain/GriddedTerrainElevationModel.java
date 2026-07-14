package geophysicalModelLibrary.terrain;


import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.stream.ImageInputStream;



/**
 * A {@link TerrainElevationModel} backed by a single regular grid of elevation samples loaded from a raster file.
 * <p>
 * Two file formats are supported, both describing the same thing — a rectangular grid of terrain elevations at a
 * constant cell spacing — and both reduced to the same internal representation:
 * <ul>
 * <li> <b>ESRI ASCII Grid</b> ({@code .asc}, a.k.a. Arc/Info ASCII Grid), loaded with {@link #fromAscFilePath}: a
 *      plain-text header ({@code ncols}, {@code nrows}, a lower-left {@code xll}/{@code yll} corner or center, and
 *      {@code cellsize}, with an optional {@code NODATA_value}) followed by the values in row-major order, north row
 *      first.
 * <li> <b>GeoTIFF</b> ({@code .tif}), loaded with {@link #fromTiffFilePath}: read through the JDK's ImageIO TIFF
 *      reader; the grid geometry comes from the {@code ModelPixelScale} and {@code ModelTiepoint} tags, whether the
 *      value sits at the pixel center or the pixel node from {@code GTRasterTypeGeoKey}, and the no-data value from the
 *      {@code GDAL_NODATA} tag.
 * </ul>
 * A query is answered by bilinear interpolation among the four surrounding cell centers. Positions outside the
 * cell-center extent, or whose interpolation stencil touches a no-data cell, return {@link Double#NaN}.
 * <p>
 * Coordinates are geographic longitude (x) and latitude (y) on WGS84. Files store the grid geometry in degrees, but
 * this class's public geographic API (the {@link #elevationAt} query and the {@link #latitudeOfRow} /
 * {@link #longitudeOfColumn} / {@link #cellSizeRadians} accessors) is in radians; the geometry is converted to radians
 * on load. Only square cells (equal latitude and longitude spacing) are supported.
 * <p>
 * As distributed by <a href="https://opentopography.org">OpenTopography</a> for the Copernicus GLO-30 (COP30) dataset,
 * the values are a Digital Surface Model (they include vegetation and buildings, not bare earth), referenced to the
 * EGM2008 geoid (orthometric height above mean sea level).
 */
public class GriddedTerrainElevationModel
	implements TerrainElevationModel
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * GeoTIFF tag numbers and the {@code GTRasterTypeGeoKey} code for a point-registered raster.
	 */
	private static final int TAG_MODEL_PIXEL_SCALE = 33550;
	private static final int TAG_MODEL_TIEPOINT = 33922;
	private static final int TAG_GEO_KEY_DIRECTORY = 34735;
	private static final int TAG_GDAL_NODATA = 42113;
	private static final int GEO_KEY_RASTER_TYPE = 1025;
	private static final int RASTER_PIXEL_IS_POINT = 2;



	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Number of columns (west-east) and rows (north-south) of the grid.
	 */
	private final int columnCount;
	private final int rowCount;

	/**
	 * Cell size (grid spacing), the same in both directions. [rad]
	 */
	private final double cellSize;

	/**
	 * Longitude of the center of the westernmost column, and latitude of the center of the northernmost row. [rad]
	 */
	private final double westCenterLongitude;
	private final double northCenterLatitude;

	/**
	 * Elevation values in row-major order, north row first; no-data cells are stored as {@link Float#NaN}. [m]
	 */
	private final float[] elevations;



	////////////////////////////////////////////////////////////////
	/// PUBLIC STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Creates a model from an ESRI ASCII Grid ({@code .asc}) file.
	 *
	 * @param filePath	path to the {@code .asc} file.
	 * @return	model loaded from the file.
	 * @throws IOException	if the file cannot be found, read, or parsed.
	 */
	public static GriddedTerrainElevationModel fromAscFilePath( String filePath ) throws IOException
	{
		return GriddedTerrainElevationModel.fromAscInputStream( new FileInputStream( filePath ) );
	}


	/**
	 * Creates a model from an ESRI ASCII Grid stream. The stream is read fully and closed.
	 *
	 * @param gridStream	stream that provides the ESRI ASCII Grid contents.
	 * @return	model loaded from the stream.
	 * @throws IOException	if the stream cannot be read or parsed.
	 */
	public static GriddedTerrainElevationModel fromAscInputStream( InputStream gridStream ) throws IOException
	{
		try( BufferedReader reader = new BufferedReader( new InputStreamReader( gridStream , StandardCharsets.UTF_8 ) ) ) {
			return readAsciiGrid( reader );
		}
	}


	/**
	 * Creates a model from a GeoTIFF ({@code .tif}) file.
	 *
	 * @param filePath	path to the {@code .tif} file.
	 * @return	model loaded from the file.
	 * @throws IOException	if the file cannot be found, read, or parsed.
	 */
	public static GriddedTerrainElevationModel fromTiffFilePath( String filePath ) throws IOException
	{
		ImageInputStream input = ImageIO.createImageInputStream( new File( filePath ) );
		if( input == null ) {
			throw new IOException( "Could not open a GeoTIFF image stream for " + filePath );
		}
		return readGeoTiff( input );
	}


	/**
	 * Creates a model from a GeoTIFF stream. The stream is read fully and closed.
	 *
	 * @param tiffStream	stream that provides the GeoTIFF contents.
	 * @return	model loaded from the stream.
	 * @throws IOException	if the stream cannot be read or parsed.
	 */
	public static GriddedTerrainElevationModel fromTiffInputStream( InputStream tiffStream ) throws IOException
	{
		ImageInputStream input = ImageIO.createImageInputStream( tiffStream );
		if( input == null ) {
			throw new IOException( "Could not open a GeoTIFF image stream." );
		}
		return readGeoTiff( input );
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 * <p>
	 * The elevation is bilinearly interpolated among the four cell centers surrounding the position.
	 */
	public double elevationAt( double latitudeRadians , double longitudeRadians )
	{
		// Fractional column (west to east) and row (north to south) in cell-center coordinates.
		double fractionalColumn = ( longitudeRadians - this.westCenterLongitude ) / this.cellSize;
		double fractionalRow = ( this.northCenterLatitude - latitudeRadians ) / this.cellSize;
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


	/**
	 * Returns the number of rows (north-south) of the grid.
	 *
	 * @return	number of rows.
	 */
	public int rowCount()
	{
		return this.rowCount;
	}


	/**
	 * Returns the number of columns (west-east) of the grid.
	 *
	 * @return	number of columns.
	 */
	public int columnCount()
	{
		return this.columnCount;
	}


	/**
	 * Returns the cell size (grid spacing), the same in both directions.
	 *
	 * @return	cell size. [rad]
	 */
	public double cellSizeRadians()
	{
		return this.cellSize;
	}


	/**
	 * Returns the latitude of the cell centers of a grid row; row 0 is the northernmost.
	 *
	 * @param row	row index, in {@code [0, rowCount - 1]}.
	 * @return	latitude of the row's cell centers. [rad]
	 */
	public double latitudeOfRow( int row )
	{
		return this.northCenterLatitude - row * this.cellSize;
	}


	/**
	 * Returns the longitude of the cell centers of a grid column; column 0 is the westernmost.
	 *
	 * @param column	column index, in {@code [0, columnCount - 1]}.
	 * @return	longitude of the column's cell centers. [rad]
	 */
	public double longitudeOfColumn( int column )
	{
		return this.westCenterLongitude + column * this.cellSize;
	}


	/**
	 * Returns the stored elevation of a grid cell, without interpolation, or {@link Double#NaN} for a no-data cell.
	 *
	 * @param row		row index, in {@code [0, rowCount - 1]} (0 is the northernmost).
	 * @param column	column index, in {@code [0, columnCount - 1]} (0 is the westernmost).
	 * @return	cell elevation, or {@code NaN} if the cell has no data. [m]
	 */
	public double elevation( int row , int column )
	{
		return this.elevations[ row * this.columnCount + column ];
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs a model from an already-parsed grid. All angular arguments are in radians.
	 *
	 * @param columnCount				number of columns (west-east).
	 * @param rowCount					number of rows (north-south).
	 * @param cellSizeRadians			cell spacing. [rad]
	 * @param westCenterLongitudeRadians	longitude of the westernmost column's cell centers. [rad]
	 * @param northCenterLatitudeRadians	latitude of the northernmost row's cell centers. [rad]
	 * @param elevations				elevations, row-major, north row first (no-data as {@link Float#NaN}). [m]
	 */
	private GriddedTerrainElevationModel( int columnCount , int rowCount , double cellSizeRadians ,
			double westCenterLongitudeRadians , double northCenterLatitudeRadians , float[] elevations )
	{
		this.columnCount = columnCount;
		this.rowCount = rowCount;
		this.cellSize = cellSizeRadians;
		this.westCenterLongitude = westCenterLongitudeRadians;
		this.northCenterLatitude = northCenterLatitudeRadians;
		this.elevations = elevations;
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE STATIC METHODS - ESRI ASCII GRID
	////////////////////////////////////////////////////////////////

	/**
	 * Parses an ESRI ASCII Grid from a reader into a model.
	 *
	 * @param reader	reader positioned at the start of the ESRI ASCII Grid.
	 * @return	parsed model.
	 * @throws IOException	if the content cannot be read or parsed.
	 */
	private static GriddedTerrainElevationModel readAsciiGrid( BufferedReader reader ) throws IOException
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

		float[] elevations = readAsciiElevations( reader , firstDataLine , columns * rows , noDataValue , hasNoDataValue );

		// Reduce the lower-left anchor to cell centers, then locate the west column and the north row.
		double southWestCenterLongitude = anchorLongitudeIsCorner ? anchorLongitude + 0.5 * cellSizeDegrees : anchorLongitude;
		double southWestCenterLatitude = anchorLatitudeIsCorner ? anchorLatitude + 0.5 * cellSizeDegrees : anchorLatitude;
		double northCenterLatitudeDegrees = southWestCenterLatitude + ( rows - 1 ) * cellSizeDegrees;
		return new GriddedTerrainElevationModel( columns , rows , Math.toRadians( cellSizeDegrees ) ,
				Math.toRadians( southWestCenterLongitude ) , Math.toRadians( northCenterLatitudeDegrees ) , elevations );
	}


	/**
	 * Reads the ASCII grid values, parsing the already-consumed first data line and then the rest of the reader, until
	 * the expected number of values has been collected. No-data values are stored as {@link Float#NaN}.
	 *
	 * @param reader			reader positioned just after the first data line.
	 * @param firstDataLine		the first data line, already read while detecting the end of the header.
	 * @param expectedCount		number of values expected ({@code ncols * nrows}).
	 * @param noDataValue		value that marks a missing cell.
	 * @param hasNoDataValue	whether a no-data value was declared in the header.
	 * @return	parsed elevation values, row-major, north row first. [m]
	 * @throws IOException	if the number of values does not match, or a value cannot be parsed.
	 */
	private static float[] readAsciiElevations( BufferedReader reader , String firstDataLine , int expectedCount ,
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



	////////////////////////////////////////////////////////////////
	/// PRIVATE STATIC METHODS - GEOTIFF
	////////////////////////////////////////////////////////////////

	/**
	 * Parses a GeoTIFF from an image input stream into a model, using the JDK's ImageIO TIFF reader. The stream is
	 * read fully and closed.
	 *
	 * @param input	image input stream over the GeoTIFF.
	 * @return	parsed model.
	 * @throws IOException	if the content cannot be read or parsed.
	 */
	private static GriddedTerrainElevationModel readGeoTiff( ImageInputStream input ) throws IOException
	{
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName( "TIFF" );
		if( !readers.hasNext() ) {
			throw new IOException( "No TIFF ImageReader is available in this JDK." );
		}
		ImageReader reader = readers.next();
		try {
			reader.setInput( input , true );
			int width = reader.getWidth( 0 );
			int height = reader.getHeight( 0 );
			TIFFDirectory directory = TIFFDirectory.createFromMetadata( reader.getImageMetadata( 0 ) );

			double[] pixelScale = requiredDoubles( directory , TAG_MODEL_PIXEL_SCALE , "ModelPixelScale (33550)" );
			double[] tiepoint = requiredDoubles( directory , TAG_MODEL_TIEPOINT , "ModelTiepoint (33922)" );
			double pixelWidthDegrees = pixelScale[0];
			double pixelHeightDegrees = pixelScale[1];
			if( Math.abs( pixelWidthDegrees - pixelHeightDegrees ) > 1.0e-12 ) {
				throw new IOException( "Non-square GeoTIFF pixels are not supported: " + pixelWidthDegrees + " x " + pixelHeightDegrees );
			}

			// The tiepoint maps raster point ( i , j ) to geographic ( x , y ); locate the reference point of raster (0,0).
			double referenceColumn = tiepoint[0];
			double referenceRow = tiepoint[1];
			double referenceLongitude = tiepoint[3];
			double referenceLatitude = tiepoint[4];
			double column0ReferenceLongitude = referenceLongitude - referenceColumn * pixelWidthDegrees;
			double row0ReferenceLatitude = referenceLatitude + referenceRow * pixelHeightDegrees;

			// For a point-registered raster the tiepoint is the cell center (node); otherwise it is the cell corner.
			double nwCenterLongitudeDegrees;
			double nwCenterLatitudeDegrees;
			if( rasterTypeOf( directory ) == RASTER_PIXEL_IS_POINT ) {
				nwCenterLongitudeDegrees = column0ReferenceLongitude;
				nwCenterLatitudeDegrees = row0ReferenceLatitude;
			} else {
				nwCenterLongitudeDegrees = column0ReferenceLongitude + 0.5 * pixelWidthDegrees;
				nwCenterLatitudeDegrees = row0ReferenceLatitude - 0.5 * pixelHeightDegrees;
			}

			double noDataValue = Double.NaN;
			boolean hasNoDataValue = false;
			TIFFField noDataField = directory.getTIFFField( TAG_GDAL_NODATA );
			if( noDataField != null ) {
				try {
					noDataValue = Double.parseDouble( noDataField.getValueAsString( 0 ).trim() );
					hasNoDataValue = true;
				} catch( NumberFormatException ignored ) {
					// Leave hasNoDataValue false if the tag is not a parsable number.
				}
			}

			// The JDK TIFF reader does not support readRaster for these images, but read(0) preserves the float samples.
			Raster raster = reader.read( 0 ).getRaster();
			float[] elevations = new float[ width * height ];
			for( int row = 0; row < height; row++ ) {
				int rowBase = row * width;
				for( int column = 0; column < width; column++ ) {
					double value = raster.getSampleDouble( column , row , 0 );
					boolean missing = Double.isNaN( value )  ||  ( hasNoDataValue  &&  value == noDataValue );
					elevations[ rowBase + column ] = missing ? Float.NaN : (float) value;
				}
			}

			return new GriddedTerrainElevationModel( width , height , Math.toRadians( pixelWidthDegrees ) ,
					Math.toRadians( nwCenterLongitudeDegrees ) , Math.toRadians( nwCenterLatitudeDegrees ) , elevations );
		} finally {
			reader.dispose();
			input.close();
		}
	}


	/**
	 * Returns a required GeoTIFF tag as a {@code double} array.
	 *
	 * @param directory	the TIFF directory.
	 * @param tag		tag number.
	 * @param name		human-readable tag name, for the error message.
	 * @return	the tag values.
	 * @throws IOException	if the tag is absent.
	 */
	private static double[] requiredDoubles( TIFFDirectory directory , int tag , String name ) throws IOException
	{
		TIFFField field = directory.getTIFFField( tag );
		if( field == null ) {
			throw new IOException( "GeoTIFF is missing the " + name + " tag." );
		}
		double[] values = new double[ field.getCount() ];
		for( int i = 0; i < values.length; i++ ) {
			values[i] = field.getAsDouble( i );
		}
		return values;
	}


	/**
	 * Returns the {@code GTRasterTypeGeoKey} value (1 = area, 2 = point) from the GeoKeyDirectory, defaulting to area.
	 *
	 * @param directory	the TIFF directory.
	 * @return	the raster type code (defaults to 1, area, when absent).
	 */
	private static int rasterTypeOf( TIFFDirectory directory )
	{
		TIFFField field = directory.getTIFFField( TAG_GEO_KEY_DIRECTORY );
		if( field == null ) {
			return 1;
		}
		// The directory is a header of 4 shorts, then key entries of 4 shorts: { keyId, tagLocation, count, value }.
		int count = field.getCount();
		for( int i = 4; i + 3 < count; i += 4 ) {
			int keyId = field.getAsInt( i );
			int tagLocation = field.getAsInt( i + 1 );
			int value = field.getAsInt( i + 3 );
			if( keyId == GEO_KEY_RASTER_TYPE  &&  tagLocation == 0 ) {
				return value;
			}
		}
		return 1;
	}

}
