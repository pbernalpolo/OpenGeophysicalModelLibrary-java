package geophysicalModelLibrary.gravity;


import numericalLibrary.types.Vector3;



/**
 * Uniform gravity defined by a constant {@link Vector3}.
 * 
 * Use this model in scenarios in which the gravity field is not expected to change.
 * For example, in a neighborhood of a point on the Earth surface.
 */
public class ConstantGravityModel
    implements GravityModel
{
    ////////////////////////////////////////////////////////////////
    /// PRIVATE VARIABLES
    ////////////////////////////////////////////////////////////////
    
	/**
	 * {@link Vector3} that defines the constant gravity field.
	 */
	private Vector3 constantGravityVector;
    
    
    
    ////////////////////////////////////////////////////////////////
    /// PUBLIC CONSTRUCTORS
    ////////////////////////////////////////////////////////////////
	
	/**
	 * Constructs a {@link ConstantGravityModel}.
	 * 
	 * @param constantGravityVector		{@link Vector3} that defines the constant gravity field.
	 */
    public ConstantGravityModel( Vector3 constantGravityVector )
    {
        this.constantGravityVector = constantGravityVector;
    }
    
    
    
    ////////////////////////////////////////////////////////////////
    /// PUBLIC METHODS
    ////////////////////////////////////////////////////////////////
    
    /**
     * {@inheritDoc}
     */
    public void setPosition( Vector3 thePosition )
    {
        // It is not necessary to store the position in order to evaluate this model.
    }
    
    
    /**
     * {@inheritDoc}
     * 
     * This implementation always returns a constant vector.
     */
    public Vector3 getGravityVector()
    {
        return this.constantGravityVector;
    }
    
    
    
    ////////////////////////////////////////////////////////////////
    /// PUBLIC STATIC METHODS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Returns a new {@link ConstantGravityModel} with acceleration along the z axis.
     * 
     * @param scale		scaling value that defines the magnitude and direction of the gravity field.
     * 
     * @return	new {@link ConstantGravityModel} with acceleration along the z axis.
     */
    public static ConstantGravityModel inZDirection( double scale )
    {
        return new ConstantGravityModel( Vector3.k().scaleInplace( scale ) );
    }
    
}
