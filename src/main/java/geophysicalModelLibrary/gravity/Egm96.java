package geophysicalModelLibrary.gravity;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * EGM96 Earth Gravitational Model.
 * <p>
 * EGM96 is a fully normalized spherical harmonic model of the Earth's gravitational potential, complete to degree and
 * order 360, produced jointly by NASA Goddard Space Flight Center and the National Imagery and Mapping Agency (NIMA):
 * <p>
 * Lemoine, F.G., et al. (1998): "The Development of the Joint NASA GSFC and the National Imagery and Mapping Agency
 * (NIMA) Geopotential Model EGM96", NASA/TP-1998-206861.
 * <p>
 * It predates {@link Egm2008} and is still the geoid many GPS receivers use to report height above mean sea level, so
 * it is useful when reconciling GPS altitudes with a model geoid.
 * <p>
 * The coefficients are loaded from a file in the ICGEM {@code .gfc} format, as distributed at
 * <a href="https://icgem.gfz.de/tom_longtime">https://icgem.gfz.de/tom_longtime</a>. The factory methods accept a
 * maximum degree so that only the required portion of the file is loaded.
 * <p>
 * Instances are created through the static factory methods, for example:
 * <pre>
 *   Egm96 model = Egm96.fromFilePathAndMaximumDegree( "res/gravity/EGM96.gfc" , 360 );
 * </pre>
 *
 * @see SphericalHarmonicGravityModel
 */
public class Egm96
	extends SphericalHarmonicGravityModel
{
	////////////////////////////////////////////////////////////////
	/// PUBLIC CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Maximum degree available in the EGM96 {@code .gfc} file.
	 */
	public static final int MAX_DEGREE = 360;



	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@link Egm96} by loading the coefficients from a {@code .gfc} stream.
	 *
	 * @param gfcStream			stream that provides the EGM96 {@code .gfc} contents. It is read fully and closed.
	 * @param maximumDegree		maximum degree  l  to load.
	 * @throws IOException	if the stream cannot be read.
	 */
	private Egm96( InputStream gfcStream , int maximumDegree ) throws IOException
	{
		super( gfcStream , maximumDegree );
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Creates an {@link Egm96} from a {@code .gfc} file, loading every available coefficient.
	 *
	 * @param filePath		path to the EGM96 {@code .gfc} file.
	 * @return	{@link Egm96} loaded up to {@link #MAX_DEGREE}.
	 * @throws IOException	if the file cannot be found or read.
	 */
	public static Egm96 fromFilePath( String filePath ) throws IOException
	{
		return Egm96.fromFilePathAndMaximumDegree( filePath , MAX_DEGREE );
	}


	/**
	 * Creates an {@link Egm96} from a {@code .gfc} file, loading coefficients up to the given degree.
	 *
	 * @param filePath			path to the EGM96 {@code .gfc} file.
	 * @param maximumDegree		maximum degree  l  to load.
	 * @return	{@link Egm96} loaded up to {@code maximumDegree}.
	 * @throws IOException	if the file cannot be found or read.
	 */
	public static Egm96 fromFilePathAndMaximumDegree( String filePath , int maximumDegree ) throws IOException
	{
		return new Egm96( new FileInputStream( filePath ) , maximumDegree );
	}


	/**
	 * Creates an {@link Egm96} from a {@code .gfc} stream, loading coefficients up to the given degree.
	 * <p>
	 * The stream is read fully and closed. This factory is useful to read from sources other than a plain file,
	 * such as a compressed or in-memory stream.
	 *
	 * @param gfcStream			stream that provides the EGM96 {@code .gfc} contents.
	 * @param maximumDegree		maximum degree  l  to load.
	 * @return	{@link Egm96} loaded up to {@code maximumDegree}.
	 * @throws IOException	if the stream cannot be read.
	 */
	public static Egm96 fromInputStreamAndMaximumDegree( InputStream gfcStream , int maximumDegree ) throws IOException
	{
		return new Egm96( gfcStream , maximumDegree );
	}

}
