package geophysicalModelLibrary;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * EGM2008 Earth Gravitational Model.
 * <p>
 * EGM2008 is a fully normalized spherical harmonic model of the Earth's gravitational potential, complete to degree and order 2159
 * (with additional coefficients up to degree 2190), produced by the U.S. National Geospatial-Intelligence Agency (NGA):
 * <p>
 * Pavlis, N.K., S.A. Holmes, S.C. Kenyon, and J.K. Factor (2008):
 * "An Earth Gravitational Model to Degree 2160: EGM2008".
 * <p>
 * The coefficients are loaded from a file in the ICGEM {@code .gfc} format, as distributed at
 * <a href="https://icgem.gfz.de/tom_longtime">https://icgem.gfz.de/tom_longtime</a>.
 * Because the full model is large (degree 2190 amounts to roughly 4.8 million coefficients), the factory methods
 * accept a maximum degree so that only the required portion of the file is loaded.
 * <p>
 * Instances are created through the static factory methods, for example:
 * <pre>
 *   Egm2008 model = Egm2008.fromFilePathAndMaximumDegree( "res/EGM2008.gfc" , 360 );
 * </pre>
 *
 * @see SphericalHarmonicGravityModel
 */
public class Egm2008
	extends SphericalHarmonicGravityModel
{
	////////////////////////////////////////////////////////////////
	/// PUBLIC CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Maximum degree available in the EGM2008 {@code .gfc} file.
	 */
	public static final int MAX_DEGREE = 2190;



	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@link Egm2008} by loading the coefficients from a {@code .gfc} stream.
	 *
	 * @param gfcStream			stream that provides the EGM2008 {@code .gfc} contents. It is read fully and closed.
	 * @param maximumDegree		maximum degree  l  to load.
	 * @throws IOException	if the stream cannot be read.
	 */
	private Egm2008( InputStream gfcStream , int maximumDegree ) throws IOException
	{
		super( gfcStream , maximumDegree );
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Creates an {@link Egm2008} from a {@code .gfc} file, loading every available coefficient.
	 *
	 * @param filePath		path to the EGM2008 {@code .gfc} file.
	 * @return	{@link Egm2008} loaded up to {@link #MAX_DEGREE}.
	 * @throws IOException	if the file cannot be found or read.
	 */
	public static Egm2008 fromFilePath( String filePath ) throws IOException
	{
		return Egm2008.fromFilePathAndMaximumDegree( filePath , MAX_DEGREE );
	}


	/**
	 * Creates an {@link Egm2008} from a {@code .gfc} file, loading coefficients up to the given degree.
	 *
	 * @param filePath			path to the EGM2008 {@code .gfc} file.
	 * @param maximumDegree		maximum degree  l  to load.
	 * @return	{@link Egm2008} loaded up to {@code maximumDegree}.
	 * @throws IOException	if the file cannot be found or read.
	 */
	public static Egm2008 fromFilePathAndMaximumDegree( String filePath , int maximumDegree ) throws IOException
	{
		return new Egm2008( new FileInputStream( filePath ) , maximumDegree );
	}


	/**
	 * Creates an {@link Egm2008} from a {@code .gfc} stream, loading coefficients up to the given degree.
	 * <p>
	 * The stream is read fully and closed. This factory is useful to read from sources other than a plain file,
	 * such as a compressed or in-memory stream.
	 *
	 * @param gfcStream			stream that provides the EGM2008 {@code .gfc} contents.
	 * @param maximumDegree		maximum degree  l  to load.
	 * @return	{@link Egm2008} loaded up to {@code maximumDegree}.
	 * @throws IOException	if the stream cannot be read.
	 */
	public static Egm2008 fromInputStreamAndMaximumDegree( InputStream gfcStream , int maximumDegree ) throws IOException
	{
		return new Egm2008( gfcStream , maximumDegree );
	}

}
