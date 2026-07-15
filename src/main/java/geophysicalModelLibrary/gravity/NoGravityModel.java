package geophysicalModelLibrary.gravity;


import numericalLibrary.types.Vector3;



/**
 * {@link GravityModel} in which there is no acceleration at any point in space.
 * 
 * Use this model in deep space scenarios.
 */
public class NoGravityModel
    implements GravityModel
{
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
     * This implementation always returns a zero {@link Vector3}.
     */
    public Vector3 getGravityVector()
    {
        return Vector3.zero();
    }
    
}
