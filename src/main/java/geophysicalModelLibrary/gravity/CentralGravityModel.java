package geophysicalModelLibrary.gravity;


import numericalLibrary.types.Vector3;



/**
 * Central gravity field defined by Newton's law of universal gravitation:
 * 		g = - G M r / |r|^3
 * 		  = - mu r / |r|^3
 * where
 * - M  is the mass creating the gravity field, which is located at the origin,
 * - G  is the gravitational constant (6.67430(15)·10^-11 m^3 kg^{-1} s^{-2}),
 * - mu  is the standard gravitational parameter; mu = G M
 * - r  is the position vector at which the gravity field is evaluated, t is time, 
 */
public class CentralGravityModel
    implements GravityModel
{
    ////////////////////////////////////////////////////////////////
    /// PRIVATE VARIABLES
    ////////////////////////////////////////////////////////////////
    
	/**
	 * Position at which the {@link CentralGravityModel} will be evaluated.
	 */
	private Vector3 rOOP;
    
    /**
     * Standard gravitational parameter:
     * product of the gravitational constant and the mass of a given astronomical body.
     * [m^3 s^{-2}]
     */
    private double mu;
    
    
    
    ////////////////////////////////////////////////////////////////
    /// PUBLIC CONSTRUCTORS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Constructs a {@link CentralGravityModel}.
     * 
     * @param standardGravitationalParameter	standard gravitational parameter; mu = G M
     * 
     * @see https://en.wikipedia.org/wiki/Standard_gravitational_parameter
     */
    public CentralGravityModel( double standardGravitationalParameter )
    {
        this.mu = standardGravitationalParameter;
    }
    
    
    
    ////////////////////////////////////////////////////////////////
    /// PUBLIC STATIC METHODS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Returns a new {@link CentralGravityModel} that describes earth gravity.
     * Parameterized with results of using Barycentric Coordinate Time.
     * 
     * See: https://iau-a3.gitlab.io/NSFA/NSFA_cbe.html#GME2009
     * 
     * @return	new {@link CentralGravityModel} parameterized accordingly to the Barycentric Coordinate Time.
     */
    public static CentralGravityModel earthTcbCompatible2009()
    {
    	return new CentralGravityModel( 3.986004418e14 );
    }
    
    
    /**
     * Returns a new {@link CentralGravityModel} that describes earth gravity.
     * Parameterized with results of using Terrestrial Time.
     * 
     * See: https://iau-a3.gitlab.io/NSFA/NSFA_cbe.html#GME2009
     * 
     * @return	new {@link CentralGravityModel} parameterized accordingly to the Terrestrial Time.
     */
    public static CentralGravityModel earthTtCompatible2009()
    {
    	return new CentralGravityModel( 3.986004415e14 );
    }
    
    
    /**
     * Returns a new {@link CentralGravityModel} that describes earth gravity.
     * Parameterized with results of using Barycentric Dynamical Time.
     * 
     * See: https://iau-a3.gitlab.io/NSFA/NSFA_cbe.html#GME2009
     * 
     * @return	new {@link CentralGravityModel} parameterized accordingly to the Barycentric Dynamical Time.
     */
    public static CentralGravityModel earthTdbCompatible2009()
    {
    	return new CentralGravityModel( 3.986004356e14 );
    }
    
    
    
    ////////////////////////////////////////////////////////////////
    /// PUBLIC METHODS
    ////////////////////////////////////////////////////////////////
    
    /**
     * {@inheritDoc}
     */
    public void setPosition( Vector3 position )
    {
    	this.rOOP = position;
    }
    
    
    /**
     * {@inheritDoc}
     * 
     * This implementation returns the result of evaluating:
     * 	g = - mu r / |r|^3
     */
    public Vector3 getGravityVector()
    {
    	double rNorm = this.rOOP.norm();
    	double mu_over_rnorm3 = this.mu / ( rNorm * rNorm * rNorm );
    	return this.rOOP.scale( -mu_over_rnorm3 );
    }
    
}
