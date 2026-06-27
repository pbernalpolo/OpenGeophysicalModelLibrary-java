package geophysicalModelLibrary.magneticModel;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import numericalLibrary.functions.SphericalHarmonicsEvaluator;
import numericalLibrary.types.Vector3;



/**
 * World Magnetic Model (WMM), the standard spherical harmonic model of Earth's main (core) magnetic field,
 * produced by NOAA/NCEI and the British Geological Survey.
 * <p>
 * The geomagnetic scalar potential is:
 * <br>
 *   V( r , theta , lambda ) = a sum_{n=1}^{N} ( a / r )^{n+1} sum_{m=0}^{n} [ g_n^m cos( m lambda ) + h_n^m sin( m lambda ) ] P_n^m( cos( theta ) )
 * <br>
 * where  a  is the geomagnetic reference radius ({@link #GEOMAGNETIC_REFERENCE_RADIUS}),  r , theta , lambda  are the
 * radius, colatitude, and longitude of the evaluation point,  P_n^m  are the Schmidt semi-normalized associated Legendre
 * functions, and  g_n^m , h_n^m  are the Gauss coefficients [nT].
 * The magnetic field is its negative gradient, {@code B = -grad V},
 * returned in the same Earth-fixed Cartesian frame as the position.
 * <p>
 * The model is time-dependent: each coefficient comes with a secular-variation rate,
 * so the field is evaluated at the decimal year set with {@link #setDecimalYear(double)}
 * (default: the model epoch) as  g_n^m(t) = g_n^m(t0) + (t - t0) gDot_n^m .
 * The model is meant to be used within its five-year validity window (epoch to epoch + 5).
 * <p>
 * Coefficients are loaded from a {@code .COF} file as distributed at
 * <a href="https://www.ncei.noaa.gov/products/world-magnetic-model">https://www.ncei.noaa.gov/products/world-magnetic-model</a>.
 * <p>
 * The internal evaluation reuses the orthonormal {@link SphericalHarmonicsEvaluator};
 * the Schmidt convention used by the Gauss coefficients is recovered by scaling each orthonormal value by
 * (-1)^m sqrt( 4 pi ( 2 - delta_{m0} ) / ( 2 n + 1 ) ) .
 */
public class WorldMagneticModel
{
	////////////////////////////////////////////////////////////////
	/// CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Geomagnetic reference radius  a  used in the WMM and IGRF potential expansions:
	 * the conventional mean Earth radius of 6371.2 km.
	 * It is a defining constant of the model
	 * (fixed by the WMM2025 Technical Report / IAGA IGRF, not stored in the {@code .COF} file),
	 * and is distinct from the WGS84 equatorial radius (6378137 m) used for coordinate conversions. [m]
	 */
	public static final double GEOMAGNETIC_REFERENCE_RADIUS = 6371200.0;



	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Decimal year of the model epoch (the reference time of the base coefficients), read from the {@code .COF} header.
	 */
	private final double epoch;

	/**
	 * Model name read from the {@code .COF} header (for example {@code "WMM-2025"}).
	 */
	private final String modelName;

	/**
	 * Release date read from the {@code .COF} header.
	 */
	private final String releaseDate;

	/**
	 * Maximum degree  n  loaded (12 for the WMM).
	 */
	private final int maximumDegree;

	/**
	 * Base Gauss coefficients at the model epoch, as triangular arrays indexed by degree then order. [nT]
	 */
	private final double[][] baseG;
	private final double[][] baseH;

	/**
	 * Secular variation (annual rate of change) of the Gauss coefficients, as triangular arrays. [nT/year]
	 */
	private final double[][] rateG;
	private final double[][] rateH;

	/**
	 * Gauss coefficients propagated to {@link #decimalYear}; their contents are refreshed by {@link #setDecimalYear(double)}. [nT]
	 */
	private final double[][] g;
	private final double[][] h;

	/**
	 * Order-dependent part of the Schmidt normalization, {@code (-1)^m sqrt( 4 pi ( 2 - delta_{m0} ) )}, indexed by order.
	 */
	private final double[] schmidtFactorOrder;

	/**
	 * Degree-dependent part of the Schmidt normalization, {@code 1 / sqrt( 2 n + 1 )}, indexed by degree.
	 */
	private final double[] schmidtFactorDegree;

	/**
	 * Evaluator of the orthonormal spherical harmonics and their colatitude derivatives.
	 */
	private final SphericalHarmonicsEvaluator harmonics;

	/**
	 * Decimal year at which the field is currently evaluated.
	 */
	private double decimalYear;

	/**
	 * Magnetic field at the last {@link #setPosition(Vector3)} call.
	 * The same instance is reused on every call. [nT]
	 */
	private final Vector3 magneticField;



	////////////////////////////////////////////////////////////////
	/// PUBLIC STATIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Creates a {@link WorldMagneticModel} from a {@code .COF} file.
	 *
	 * @param filePath	path to the WMM {@code .COF} file.
	 * @return	{@link WorldMagneticModel} loaded from the file, set to its epoch.
	 * @throws IOException	if the file cannot be found or read.
	 */
	public static WorldMagneticModel fromFilePath( String filePath ) throws IOException
	{
		return new WorldMagneticModel( new FileInputStream( filePath ) );
	}


	/**
	 * Creates a {@link WorldMagneticModel} from a {@code .COF} stream.
	 * The stream is read fully and closed.
	 *
	 * @param cofStream		stream that provides the WMM {@code .COF} contents.
	 * @return	{@link WorldMagneticModel} loaded from the stream, set to its epoch.
	 * @throws IOException	if the stream cannot be read.
	 */
	public static WorldMagneticModel fromInputStream( InputStream cofStream ) throws IOException
	{
		return new WorldMagneticModel( cofStream );
	}



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the model epoch (reference decimal year of the base coefficients).
	 *
	 * @return	model epoch. [decimal year]
	 */
	public double epoch()
	{
		return this.epoch;
	}


	/**
	 * Returns the model name read from the {@code .COF} header.
	 *
	 * @return	model name.
	 */
	public String modelName()
	{
		return this.modelName;
	}


	/**
	 * Returns the release date read from the {@code .COF} header.
	 *
	 * @return	release date.
	 */
	public String releaseDate()
	{
		return this.releaseDate;
	}


	/**
	 * Returns the maximum degree  n  loaded.
	 *
	 * @return	maximum degree.
	 */
	public int maximumDegree()
	{
		return this.maximumDegree;
	}


	/**
	 * Returns the decimal year at which the field is currently evaluated.
	 *
	 * @return	current decimal year.
	 */
	public double decimalYear()
	{
		return this.decimalYear;
	}


	/**
	 * Sets the decimal year at which the field is evaluated,
	 * propagating the Gauss coefficients from the model epoch with their secular variation:
	 * g_n^m(t) = g_n^m(t0) + (t - t0) gDot_n^m  (and likewise for  h_n^m ).
	 * <p>
	 * The model is intended for use within its five-year validity window (epoch to epoch + 5);
	 * evaluating outside it is a linear extrapolation and loses accuracy.
	 * This method is {@code final} so it is safe to call from the constructor.
	 *
	 * @param decimalYear	decimal year at which to evaluate the field.
	 */
	public final void setDecimalYear( double decimalYear )
	{
		this.decimalYear = decimalYear;
		double dt = decimalYear - this.epoch;
		for( int n=1; n<=this.maximumDegree; n++ ) {
			for( int m=0; m<=n; m++ ) {
				this.g[n][m] = this.baseG[n][m] + dt * this.rateG[n][m];
				this.h[n][m] = this.baseH[n][m] + dt * this.rateH[n][m];
			}
		}
	}


	/**
	 * Sets the position at which the magnetic field is evaluated.
	 * Then computes the field from {@code B = -grad V}.
	 * <p>
	 * The position is expressed in the Earth-fixed Cartesian frame in which the coefficients are defined.
	 * The result is available through {@link #getMagneticField()} in the same frame.
	 * <p>
	 * The field is evaluated in spherical coordinates and is singular exactly at the geographic poles
	 * (where {@code sin( theta ) = 0}).
	 * At those points the horizontal contributions are dropped and only the radial term is kept.
	 *
	 * @param position		evaluation point in the Earth-fixed Cartesian frame. [m]
	 * @throws IllegalArgumentException	if the position is the origin.
	 */
	public void setPosition( Vector3 position )
	{
		double x = position.x();
		double y = position.y();
		double z = position.z();
		double r = position.norm();
		if( r == 0.0 ) {
			throw new IllegalArgumentException( "The position must not be the origin." );
		}
		double rho = Math.hypot( x , y );
		
		// Direction cosines of the colatitude and longitude.
		double cosTheta = z / r;
		if( cosTheta > 1.0 ) {
			cosTheta = 1.0;
		} else if( cosTheta < -1.0 ) {
			cosTheta = -1.0;
		}
		double sinTheta = rho / r;
		double cosLambda = ( rho > 0.0 ) ? x / rho : 1.0;
		double sinLambda = ( rho > 0.0 ) ? y / rho : 0.0;
		
		this.harmonics.setCosTheta( cosTheta );
		this.harmonics.setCosPhiAndSinPhi( cosLambda , sinLambda );
		this.harmonics.evaluate();
		this.harmonics.evaluateDerivatives();
		
		// Accumulate the spherical components of  B = -grad V .
		// sumRadial     contributes to  B_r      = sumRadial .
		// sumColatitude contributes to  B_theta  = -sumColatitude / sin( theta ) .
		// sumLongitude  contributes to  B_lambda = -sumLongitude / sin( theta ) .
		double sumRadial = 0.0;
		double sumColatitude = 0.0;
		double sumLongitude = 0.0;
		double aOverR = GEOMAGNETIC_REFERENCE_RADIUS / r;
		double w = aOverR * aOverR * aOverR;   // ( a / r )^{n+2} with n = 1.
		for( int n=1; n<=this.maximumDegree; n++ ) {
			double degreeFactor = this.schmidtFactorDegree[n];
			double[] gn = this.g[n];
			double[] hn = this.h[n];
			for( int m=0; m<=n; m++ ) {
				double factor = this.schmidtFactorOrder[m] * degreeFactor;   // (-1)^m sqrt( 4 pi ( 2 - delta_{m0} ) / ( 2 n + 1 ) )
				double re = this.harmonics.getSphericalHarmonicsRealPart( n , m );
				double im = this.harmonics.getSphericalHarmonicsImaginaryPart( n , m );
				double dre = this.harmonics.getSphericalHarmonicsDerivativeRealPart( n , m );
				double dim = this.harmonics.getSphericalHarmonicsDerivativeImaginaryPart( n , m );
				double gnm = gn[m];
				double hnm = hn[m];
				// Schmidt-normalized combinations:
				//   valueTerm = ( g cos m lambda + h sin m lambda ) P_n^m
				//   thetaTerm = ( g cos m lambda + h sin m lambda ) sin( theta ) dP_n^m/dtheta
				//   longitudeTerm = m ( h cos m lambda - g sin m lambda ) P_n^m
				double valueTerm = factor * ( gnm * re + hnm * im );
				double thetaTerm = factor * ( gnm * dre + hnm * dim );
				double longitudeTerm = m * factor * ( hnm * re - gnm * im );
				sumRadial += ( n + 1 ) * w * valueTerm;
				sumColatitude += w * thetaTerm;
				sumLongitude += w * longitudeTerm;
			}
			w *= aOverR;
		}
		
		double bRadial = sumRadial;
		double bColatitude = ( sinTheta > 0.0 ) ? -sumColatitude / sinTheta : 0.0;
		double bLongitude = ( sinTheta > 0.0 ) ? -sumLongitude / sinTheta : 0.0;
		
		// Rotate the spherical components ( B_r , B_theta , B_lambda ) into the Earth-fixed Cartesian frame.
		this.magneticField.setX( bRadial * sinTheta * cosLambda + bColatitude * cosTheta * cosLambda - bLongitude * sinLambda );
		this.magneticField.setY( bRadial * sinTheta * sinLambda + bColatitude * cosTheta * sinLambda + bLongitude * cosLambda );
		this.magneticField.setZ( bRadial * cosTheta - bColatitude * sinTheta );
	}


	/**
	 * Returns the magnetic field at the position set with {@link #setPosition(Vector3)},
	 * in the same Earth-fixed Cartesian frame as the position.
	 * The same {@link Vector3} instance is returned on every call. [nT]
	 *
	 * @return	magnetic field. [nT]
	 */
	public Vector3 getMagneticField()
	{
		return this.magneticField;
	}


	/**
	 * Returns the cosine Gauss coefficient  g_n^m  propagated to the current {@link #decimalYear()}. [nT]
	 *
	 * @param n		degree in the range n = 1 , 2 , ... , {@link #maximumDegree()}.
	 * @param m		order in the range m = 0 , 1 , ... , n.
	 * @return	Gauss coefficient  g_n^m . [nT]
	 */
	public double gaussG( int n , int m )
	{
		return this.g[n][m];
	}


	/**
	 * Returns the sine Gauss coefficient  h_n^m  propagated to the current {@link #decimalYear()}. [nT]
	 *
	 * @param n		degree in the range n = 1 , 2 , ... , {@link #maximumDegree()}.
	 * @param m		order in the range m = 0 , 1 , ... , n.
	 * @return	Gauss coefficient  h_n^m . [nT]
	 */
	public double gaussH( int n , int m )
	{
		return this.h[n][m];
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs a {@link WorldMagneticModel} by loading the coefficients from a {@code .COF} stream, set to the model epoch.
	 * <p>
	 * The stream is read fully and {@link InputStream#close() closed} before this constructor returns.
	 *
	 * @param input		stream that provides the {@code .COF} contents.
	 * @throws IOException	if the stream cannot be read, or the {@code .COF} contents are malformed.
	 */
	private WorldMagneticModel( InputStream input ) throws IOException
	{
		double parsedEpoch;
		String parsedModelName;
		String parsedReleaseDate;
		int parsedMaximumDegree;
		double[][] parsedBaseG;
		double[][] parsedBaseH;
		double[][] parsedRateG;
		double[][] parsedRateH;
		
		try( BufferedReader reader = new BufferedReader( new InputStreamReader( input , StandardCharsets.US_ASCII ) ) ) {
			
			// Header: epoch, model name, release date.
			String header = null;
			String line;
			while( ( line = reader.readLine() ) != null ) {
				String trimmed = line.trim();
				if( !trimmed.isEmpty() ) {
					header = trimmed;
					break;
				}
			}
			if( header == null ) {
				throw new IOException( "Empty WMM .COF file." );
			}
			String[] headerToken = header.split( "\\s+" );
			parsedEpoch = Double.parseDouble( headerToken[0] );
			parsedModelName = ( headerToken.length > 1 ) ? headerToken[1] : "unknown";
			parsedReleaseDate = ( headerToken.length > 2 ) ? headerToken[2] : "unknown";
			
			// Coefficient lines:  n  m  g  h  gDot  hDot , until a terminator line of 9s.
			List<double[]> rows = new ArrayList<>();
			int maxDegree = 0;
			while( ( line = reader.readLine() ) != null ) {
				String trimmed = line.trim();
				if( trimmed.isEmpty() ) {
					continue;
				}
				if( trimmed.startsWith( "9999" ) ) {
					break;
				}
				String[] token = trimmed.split( "\\s+" );
				int n = Integer.parseInt( token[0] );
				int m = Integer.parseInt( token[1] );
				double gValue = Double.parseDouble( token[2] );
				double hValue = Double.parseDouble( token[3] );
				double gRate = Double.parseDouble( token[4] );
				double hRate = Double.parseDouble( token[5] );
				rows.add( new double[]{ n , m , gValue , hValue , gRate , hRate } );
				if( n > maxDegree ) {
					maxDegree = n;
				}
			}
			
			parsedMaximumDegree = maxDegree;
			parsedBaseG = newTriangularArray( maxDegree );
			parsedBaseH = newTriangularArray( maxDegree );
			parsedRateG = newTriangularArray( maxDegree );
			parsedRateH = newTriangularArray( maxDegree );
			for( double[] row : rows ) {
				int n = (int) row[0];
				int m = (int) row[1];
				parsedBaseG[n][m] = row[2];
				parsedBaseH[n][m] = row[3];
				parsedRateG[n][m] = row[4];
				parsedRateH[n][m] = row[5];
			}
		}
		
		this.epoch = parsedEpoch;
		this.modelName = parsedModelName;
		this.releaseDate = parsedReleaseDate;
		this.maximumDegree = parsedMaximumDegree;
		this.baseG = parsedBaseG;
		this.baseH = parsedBaseH;
		this.rateG = parsedRateG;
		this.rateH = parsedRateH;
		this.g = newTriangularArray( this.maximumDegree );
		this.h = newTriangularArray( this.maximumDegree );
		
		// Schmidt normalization split into an order part and a degree part:
		//   P_n^m(Schmidt) = (-1)^m sqrt( 4 pi ( 2 - delta_{m0} ) / ( 2 n + 1 ) ) P_n^m(orthonormal) .
		this.schmidtFactorOrder = new double[ this.maximumDegree + 1 ];
		for( int m=0; m<this.schmidtFactorOrder.length; m++ ) {
			double sign = ( ( m & 1 ) == 0 ) ? 1.0 : -1.0;
			double neumann = ( m == 0 ) ? 1.0 : 2.0;   // 2 - delta_{m0}
			this.schmidtFactorOrder[m] = sign * Math.sqrt( 4.0 * Math.PI * neumann );
		}
		this.schmidtFactorDegree = new double[ this.maximumDegree + 1 ];
		for( int n=0; n<this.schmidtFactorDegree.length; n++ ) {
			this.schmidtFactorDegree[n] = 1.0 / Math.sqrt( 2.0 * n + 1.0 );
		}
		
		// The underlying evaluator requires a maximum degree of at least 1 to allocate its recurrence arrays.
		this.harmonics = new SphericalHarmonicsEvaluator( Math.max( 1 , this.maximumDegree ) );
		this.magneticField = Vector3.zero();
		
		// Start at the model epoch (base coefficients).
		this.setDecimalYear( this.epoch );
	}
	
	
	
	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////
	
	/**
	 * Allocates a triangular array indexed by degree then order, with row  n  holding orders  m = 0 , 1 , ... , n .
	 *
	 * @param maximumDegree	maximum degree.
	 * @return	triangular array of zeros.
	 */
	private static double[][] newTriangularArray( int maximumDegree )
	{
		double[][] array = new double[ maximumDegree + 1 ][];
		for( int n=0; n<=maximumDegree; n++ ) {
			array[n] = new double[ n + 1 ];
		}
		return array;
	}
	
}
