package geophysicalModelLibrary.gravity;


import numericalLibrary.types.Vector3;



/**
 * Represents a gravity model.
 */
public interface GravityModel
{
    ////////////////////////////////////////////////////////////////
    /// PUBLIC ABSTRACT METHODS
    ////////////////////////////////////////////////////////////////
    
	/**
	 * Sets the position at which the {@link GravityModel} will be evaluated.
	 * 
	 * @param position	position at which the {@link GravityModel} will be evaluated.
	 * 
	 * @see #acceleration()
	 */
	public void setPosition( Vector3 position );
	
	
	/**
	 * Returns the gravitational acceleration resulting from evaluating the {@link GravityModel} at the established position.
	 * 
	 * @return	gravitational acceleration resulting from evaluating the {@link GravityModel} at the established position.
	 * 
	 * @see #setPosition(Vector3)
	 */
    public Vector3 getGravityVector();
    
}
