package geophysicalModelLibrary;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import numericalLibrary.functions.SphericalHarmonicsEvaluator;
import numericalLibrary.types.Vector3;



/**
 * Base class for global gravity field models defined by a spherical harmonic expansion.
 * <p>
 * The gravitational potential is represented as:
 * <p>
 *   V( r , theta , lambda ) = ( GM / r ) sum_{l=0}^{lMax} ( a / r )^l sum_{m=0}^{l} P_l^m( cos( theta ) ) [ C_l^m cos( m lambda ) + S_l^m sin( m lambda ) ]
 * <p>
 * where:
 * <ul>
 * <li> GM  is the gravitational parameter (see {@link #gravitationalParameter()}),
 * <li> a  is the reference radius (see {@link #referenceRadius()}),
 * <li> r , theta , lambda  are the radius, colatitude, and longitude of the evaluation point,
 * <li> P_l^m  are the fully normalized associated Legendre functions,
 * <li> C_l^m , S_l^m  are the fully normalized Stokes coefficients (see {@link #normalizedC(int, int)} and {@link #normalizedS(int, int)}).
 * </ul>
 * <p>
 * This class loads the coefficients from a file in the ICGEM {@code .gfc} format
 * (see <a href="https://icgem.gfz.de/tom_longtime">https://icgem.gfz.de/tom_longtime</a>),
 * which is shared by every gravity field model distributed by the ICGEM.
 * Subclasses only need to provide the input stream and the maximum degree to load
 * (see for example {@link Egm2008}).
 * <p>
 * The potential and its gradient (the gravity vector) are evaluated at the position set with {@link #setPosition(Vector3)}.
 * The position and the returned gravity vector are expressed in the Earth-fixed Cartesian frame in which the coefficients are defined.
 */
public abstract class SphericalHarmonicGravityModel
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE VARIABLES
	////////////////////////////////////////////////////////////////

	/**
	 * Gravitational parameter  GM  in [m^3/s^2] read from the {@code earth_gravity_constant} header entry.
	 */
	private final double gravitationalParameter;

	/**
	 * Reference radius  a  in [m] read from the {@code radius} header entry.
	 */
	private final double referenceRadius;

	/**
	 * Maximum degree  l  actually loaded into {@link #C} and {@link #S}.
	 */
	private final int maximumDegree;

	/**
	 * Model name read from the {@code modelname} header entry.
	 */
	private final String modelName;

	/**
	 * Tide system read from the {@code tide_system} header entry.
	 */
	private final String tideSystem;

	/**
	 * Fully normalized cosine Stokes coefficients  C_l^m  stored as a triangular array.
	 * First index is the degree l; second index is the order m (with m = 0 , 1 , ... , l).
	 */
	private final double[][] C;

	/**
	 * Fully normalized sine Stokes coefficients  S_l^m  stored as a triangular array.
	 * First index is the degree l; second index is the order m (with m = 0 , 1 , ... , l).
	 */
	private final double[][] S;

	/**
	 * Evaluator of the orthonormal spherical harmonics and their colatitude derivatives.
	 * Its real and imaginary parts are combined with the Stokes coefficients to build the potential and its gradient.
	 */
	private final SphericalHarmonicsEvaluator harmonics;

	/**
	 * Factor that converts the orthonormal spherical harmonic value of order  m  produced by {@link #harmonics}
	 * into the geodetic fully normalized one. It equals  (-1)^m sqrt( 4 pi ( 2 - delta_{m0} ) ) .
	 */
	private final double[] geodeticNormalizationFactor;

	/**
	 * Fully normalized even zonal harmonic coefficients of the GRS80 reference ellipsoid, indexed by degree
	 * (only degrees 2, 4, 6, 8 are non-zero). Used to remove the normal field when computing the geoid undulation.
	 */
	private final double[] referenceEvenZonal;

	/**
	 * Radius (distance to the origin) of the last position passed to {@link #setPosition(Vector3)}, in [m].
	 */
	private double radius;

	/**
	 * Gravitational potential at the last {@link #setPosition(Vector3)} call.
	 */
	private double potential;

	/**
	 * Gravity vector at the last {@link #setPosition(Vector3)} call.
	 * The same instance is reused on every call; its components are updated in place.
	 */
	private final Vector3 gravityVector;



	////////////////////////////////////////////////////////////////
	/// PUBLIC METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the gravitational parameter  GM  in [m^3/s^2].
	 *
	 * @return	gravitational parameter  GM  in [m^3/s^2].
	 */
	public double gravitationalParameter()
	{
		return this.gravitationalParameter;
	}


	/**
	 * Returns the reference radius  a  in [m].
	 *
	 * @return	reference radius  a  in [m].
	 */
	public double referenceRadius()
	{
		return this.referenceRadius;
	}


	/**
	 * Returns the maximum degree  l  loaded into this model.
	 *
	 * @return	maximum degree  l  loaded into this model.
	 */
	public int maximumDegree()
	{
		return this.maximumDegree;
	}


	/**
	 * Returns the model name read from the {@code .gfc} header.
	 *
	 * @return	model name read from the {@code .gfc} header.
	 */
	public String modelName()
	{
		return this.modelName;
	}


	/**
	 * Returns the tide system read from the {@code .gfc} header.
	 *
	 * @return	tide system read from the {@code .gfc} header.
	 */
	public String tideSystem()
	{
		return this.tideSystem;
	}


	/**
	 * Returns the fully normalized cosine Stokes coefficient  C_l^m .
	 *
	 * @param l		degree in the range l = 0 , 1 , ... , {@link #maximumDegree()}.
	 * @param m		order in the range m = 0 , 1 , ... , l.
	 * @return	fully normalized cosine Stokes coefficient  C_l^m .
	 */
	public double normalizedC( int l , int m )
	{
		return this.C[l][m];
	}


	/**
	 * Returns the fully normalized sine Stokes coefficient  S_l^m .
	 *
	 * @param l		degree in the range l = 0 , 1 , ... , {@link #maximumDegree()}.
	 * @param m		order in the range m = 0 , 1 , ... , l.
	 * @return	fully normalized sine Stokes coefficient  S_l^m .
	 */
	public double normalizedS( int l , int m )
	{
		return this.S[l][m];
	}


	/**
	 * Sets the position at which the gravitational potential and the gravity vector are evaluated.
	 * <p>
	 * The position is expressed in the Earth-fixed Cartesian frame in which the model coefficients are defined.
	 * After calling this method, the results are available through {@link #getGravitationalPotential()} and {@link #getGravityVector()}.
	 * <p>
	 * The gradient is evaluated in spherical coordinates and is therefore singular exactly at the geographic poles
	 * (where  sin( theta ) = 0 ). At those points the horizontal contributions are dropped and only the radial term is kept.
	 *
	 * @param position		evaluation point in the Earth-fixed Cartesian frame, in [m].
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
		this.radius = r;
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

		// Evaluate the spherical harmonics and their colatitude derivatives at the current direction.
		// The direction cosines are passed directly, avoiding arc-cosine / arc-tangent round trips
		// (wasteful, and the arc-cosine is ill-conditioned near the poles).
		this.harmonics.evaluateWithCosThetaCosPhiAndSinPhi( cosTheta , cosLambda , sinLambda );
		this.harmonics.evaluateDerivatives();

		// Accumulate the potential and the spherical components of its gradient.
		// sumV          contributes to  V = ( GM / r ) sumV .
		// sumRadial     contributes to  dV/dr = -( GM / r^2 ) sumRadial .
		// sumColatitude contributes to  dV/dtheta = ( GM / r ) sumColatitude / sin( theta ) .
		// sumLongitude  contributes to  dV/dlambda = ( GM / r ) sumLongitude .
		double sumV = 0.0;
		double sumRadial = 0.0;
		double sumColatitude = 0.0;
		double sumLongitude = 0.0;
		double aOverR = this.referenceRadius / r;
		double u = 1.0;   // ( a / r )^l with l = 0.
		for( int l=0; l<=this.maximumDegree; l++ ) {
			double[] Cl = this.C[l];
			double[] Sl = this.S[l];
			for( int m=0; m<=l; m++ ) {
				double factor = this.geodeticNormalizationFactor[m];
				// Orthonormal spherical harmonic real/imaginary parts and the parts of  sin( theta ) dY_l^m/dtheta .
				double re = this.harmonics.getSphericalHarmonicsRealPart( l , m );
				double im = this.harmonics.getSphericalHarmonicsImaginaryPart( l , m );
				double dre = this.harmonics.getSphericalHarmonicsDerivativeRealPart( l , m );
				double dim = this.harmonics.getSphericalHarmonicsDerivativeImaginaryPart( l , m );
				// Geodetic combinations:
				//   potentialTerm  = P_l^m ( C cos m lambda + S sin m lambda )
				//   longitudeTerm  = P_l^m ( S cos m lambda - C sin m lambda )
				//   colatitudeTerm = ( sin theta dP_l^m/dtheta )( C cos m lambda + S sin m lambda )
				double potentialTerm = factor * ( Cl[m] * re + Sl[m] * im );
				double longitudeTerm = factor * ( Sl[m] * re - Cl[m] * im );
				double colatitudeTerm = factor * ( Cl[m] * dre + Sl[m] * dim );
				sumV += u * potentialTerm;
				sumRadial += ( l + 1 ) * u * potentialTerm;
				sumLongitude += u * m * longitudeTerm;
				sumColatitude += u * colatitudeTerm;
			}
			u *= aOverR;
		}

		// Gravity vector in spherical components  ( g_r , g_theta , g_lambda ) .
		double prefactor = this.gravitationalParameter / ( r * r );
		double gRadial = -prefactor * sumRadial;
		double gColatitude = ( sinTheta > 0.0 ) ? prefactor * sumColatitude / sinTheta : 0.0;
		double gLongitude = ( sinTheta > 0.0 ) ? prefactor * sumLongitude / sinTheta : 0.0;

		// Rotate the spherical components into the Earth-fixed Cartesian frame and store them in the reused vector.
		this.gravityVector.setX( gRadial * sinTheta * cosLambda + gColatitude * cosTheta * cosLambda - gLongitude * sinLambda );
		this.gravityVector.setY( gRadial * sinTheta * sinLambda + gColatitude * cosTheta * sinLambda + gLongitude * cosLambda );
		this.gravityVector.setZ( gRadial * cosTheta - gColatitude * sinTheta );

		this.potential = this.gravitationalParameter / r * sumV;
	}


	/**
	 * Returns the gravitational potential  V  in [m^2/s^2] at the position set with {@link #setPosition(Vector3)}.
	 *
	 * @return	gravitational potential  V  in [m^2/s^2].
	 */
	public double getGravitationalPotential()
	{
		return this.potential;
	}


	/**
	 * Returns the gravity vector in [m/s^2] at the position set with {@link #setPosition(Vector3)}.
	 * <p>
	 * The vector is the gradient of the gravitational potential (i.e. the Newtonian gravitational attraction)
	 * expressed in the same Earth-fixed Cartesian frame as the position. It does not include the centrifugal term.
	 * <p>
	 * The same {@link Vector3} instance is returned on every call; its components are overwritten by {@link #setPosition(Vector3)}.
	 *
	 * @return	gravity vector in [m/s^2].
	 */
	public Vector3 getGravityVector()
	{
		return this.gravityVector;
	}


	/**
	 * Returns the geoid undulation  N  in [m] at the position set with {@link #setPosition(Vector3)}.
	 * <p>
	 * The undulation is the height of the geoid above the GRS80 reference ellipsoid, computed from Bruns' formula
	 * {@code N = T / gamma}, where  T  is the disturbing potential (the model potential with the degrees 0 and 1 and the
	 * even zonal harmonics of the reference ellipsoid removed) and  gamma  is a representative normal gravity, taken here
	 * as  GM / a^2 . When the position lies above the reference sphere this returns the height anomaly at that point.
	 *
	 * @return	geoid undulation  N  in [m].
	 */
	public double getGeoidUndulation()
	{
		double r = this.radius;
		double aOverR = this.referenceRadius / r;
		double u = aOverR * aOverR;   // ( a / r )^l with l = 2.
		int maxRemovedZonal = Math.min( 8 , this.maximumDegree );
		double sum = 0.0;
		for( int l=2; l<=this.maximumDegree; l++ ) {
			double[] Cl = this.C[l];
			double[] Sl = this.S[l];
			for( int m=0; m<=l; m++ ) {
				double cBar = Cl[m];
				if(  m == 0  &&  l <= maxRemovedZonal  &&  ( l & 1 ) == 0  ) {
					cBar -= this.referenceEvenZonal[l];
				}
				double factor = this.geodeticNormalizationFactor[m];
				double re = this.harmonics.getSphericalHarmonicsRealPart( l , m );
				double im = this.harmonics.getSphericalHarmonicsImaginaryPart( l , m );
				sum += u * factor * ( cBar * re + Sl[m] * im );
			}
			u *= aOverR;
		}
		double disturbingPotential = this.gravitationalParameter / r * sum;
		double normalGravity = this.gravitationalParameter / ( this.referenceRadius * this.referenceRadius );
		return disturbingPotential / normalGravity;
	}
	
	
	
	////////////////////////////////////////////////////////////////
	/// PROTECTED CONSTRUCTORS
	////////////////////////////////////////////////////////////////

	/**
	 * Constructs a {@link SphericalHarmonicGravityModel} by loading the coefficients from a {@code .gfc} stream.
	 * <p>
	 * The {@code .gfc} file is assumed to list its coefficients by ascending degree, as defined by the ICGEM format.
	 * The stream is read fully and {@link InputStream#close() closed} before this constructor returns.
	 *
	 * @param input				stream that provides the {@code .gfc} contents.
	 * @param maximumDegree		maximum degree  l  to load. Coefficients with degree above this value are not loaded.
	 * 							It is clamped to the {@code max_degree} declared in the file header.
	 * @throws IllegalArgumentException	if {@code maximumDegree} is negative.
	 * @throws IOException	if the stream cannot be read, or the {@code .gfc} contents are malformed.
	 */
	protected SphericalHarmonicGravityModel( InputStream input , int maximumDegree ) throws IOException
	{
		if( maximumDegree < 0 ) {
			throw new IllegalArgumentException( "maximumDegree must be non-negative; found " + maximumDegree );
		}

		try( BufferedReader reader = new BufferedReader( new InputStreamReader( input , StandardCharsets.US_ASCII ) ) ) {

			//////// Parse the header.
			double gravitationalParameter = Double.NaN;
			double referenceRadius = Double.NaN;
			int fileMaxDegree = Integer.MAX_VALUE;
			String modelName = "unknown";
			String tideSystem = "unknown";
			String norm = null;
			String line;
			while( ( line = reader.readLine() ) != null ) {
				String trimmed = line.trim();
				if( trimmed.isEmpty() ) {
					continue;
				}
				if( trimmed.startsWith( "end_of_head" ) ) {
					break;
				}
				String[] token = trimmed.split( "\\s+" );
				switch( token[0] ) {
					case "earth_gravity_constant":	gravitationalParameter = parseFortranDouble( token[1] );	break;
					case "radius":					referenceRadius = parseFortranDouble( token[1] );			break;
					case "max_degree":				fileMaxDegree = Integer.parseInt( token[1] );				break;
					case "modelname":				modelName = token[1];										break;
					case "tide_system":				tideSystem = token[1];										break;
					case "norm":					norm = token[1];											break;
					default:	// Remaining header entries (product_type, errors, url, ...) are ignored.
						break;
				}
			}
			if( Double.isNaN( gravitationalParameter ) ) {
				throw new IOException( "Missing 'earth_gravity_constant' entry in the .gfc header." );
			}
			if( Double.isNaN( referenceRadius ) ) {
				throw new IOException( "Missing 'radius' entry in the .gfc header." );
			}
			// This loader assumes fully normalized coefficients; reject anything else explicitly.
			if(  norm != null  &&  !norm.equals( "fully_normalized" )  ) {
				throw new IOException( "Unsupported normalization '" + norm + "'; only 'fully_normalized' is supported." );
			}

			//////// Allocate the triangular coefficient arrays.
			int loadedMaxDegree = Math.min( maximumDegree , fileMaxDegree );
			double[][] C = new double[ loadedMaxDegree + 1 ][];
			double[][] S = new double[ loadedMaxDegree + 1 ][];
			for( int l=0; l<=loadedMaxDegree; l++ ) {
				C[l] = new double[ l + 1 ];
				S[l] = new double[ l + 1 ];
			}

			//////// Parse the coefficients.
			// The ICGEM format lists coefficients by ascending degree, so we stop reading as soon as the degree exceeds the requested one.
			while( ( line = reader.readLine() ) != null ) {
				String trimmed = line.trim();
				if( trimmed.isEmpty() ) {
					continue;
				}
				String[] token = trimmed.split( "\\s+" );
				// Only static coefficient lines are loaded; time-variable lines (trnd, acos, asin, ...) are ignored.
				if(  !token[0].equals( "gfc" )  &&  !token[0].equals( "gfct" )  ) {
					continue;
				}
				int l = Integer.parseInt( token[1] );
				if( l > loadedMaxDegree ) {
					break;
				}
				int m = Integer.parseInt( token[2] );
				C[l][m] = parseFortranDouble( token[3] );
				S[l][m] = parseFortranDouble( token[4] );
			}

			//////// Store the parsed model.
			this.gravitationalParameter = gravitationalParameter;
			this.referenceRadius = referenceRadius;
			this.maximumDegree = loadedMaxDegree;
			this.modelName = modelName;
			this.tideSystem = tideSystem;
			this.C = C;
			this.S = S;
		}

		//////// Prepare the machinery used to evaluate the potential and its gradient.
		// The underlying evaluator requires a maximum degree of at least 1 to allocate its recurrence arrays.
		this.harmonics = new SphericalHarmonicsEvaluator( Math.max( 1 , this.maximumDegree ) );
		this.geodeticNormalizationFactor = new double[ this.maximumDegree + 1 ];
		for( int m=0; m<this.geodeticNormalizationFactor.length; m++ ) {
			double sign = ( m % 2 == 0 ) ? 1.0 : -1.0;
			double normalization = ( m == 0 ) ? 1.0 : 2.0;
			this.geodeticNormalizationFactor[m] = sign * Math.sqrt( 4.0 * Math.PI * normalization );
		}
		this.gravityVector = Vector3.zero();

		// Even zonal harmonics of the reference ellipsoid, used to remove the normal field for the geoid undulation.
		this.referenceEvenZonal = referenceEllipsoidEvenZonalCoefficients();
	}
	
	
	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Parses a floating-point value written in either C or Fortran notation.
	 * <p>
	 * The {@code .gfc} format mixes C-style exponents (e.g. {@code -0.484e-03}) with
	 * Fortran-style exponents (e.g. {@code 1.0d0}), so the exponent marker is normalized before parsing.
	 *
	 * @param value		text representation of the value.
	 * @return	parsed value.
	 */
	private static double parseFortranDouble( String value )
	{
		return Double.parseDouble( value.replace( 'D' , 'e' ).replace( 'd' , 'e' ) );
	}


	/**
	 * Returns the fully normalized even zonal harmonic coefficients of the GRS80 reference ellipsoid, indexed by degree.
	 * <p>
	 * They are obtained from the closed-form expression of the even zonal harmonics  J_{2n}  of an equipotential ellipsoid
	 * (Heiskanen and Moritz, "Physical Geodesy", eq. 2-92), and converted to the geodetic fully normalized convention with
	 * {@code C_{l,0} = -J_l / sqrt( 2 l + 1 )}.
	 *
	 * @return	array indexed by degree; only degrees 2, 4, 6, 8 are non-zero.
	 */
	private static double[] referenceEllipsoidEvenZonalCoefficients()
	{
		double firstEccentricitySquared = 0.00669438002290;   // GRS80
		double dynamicFormFactor = 0.00108263;                 // GRS80  J2
		double[] coefficients = new double[ 9 ];
		for( int n=1; n<=4; n++ ) {
			int degree = 2 * n;
			double sign = ( n % 2 == 0 ) ? -1.0 : 1.0;   // (-1)^{n+1}
			double eccentricityPower = Math.pow( firstEccentricitySquared , n );
			double j = sign * ( 3.0 * eccentricityPower ) / ( ( 2.0 * n + 1.0 ) * ( 2.0 * n + 3.0 ) )
					* ( 1.0 - n + 5.0 * n * dynamicFormFactor / firstEccentricitySquared );
			coefficients[ degree ] = -j / Math.sqrt( 2.0 * degree + 1.0 );
		}
		return coefficients;
	}

}
