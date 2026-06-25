package geophysicalModelLibrary.referenceSystems;


import numericalLibrary.types.MatrixReal;
import numericalLibrary.types.Quaternion;
import numericalLibrary.types.UnitQuaternion;
import numericalLibrary.types.Vector3;



/**
 * Coordinates in a Geographic Coordinate System: longitude, latitude, and height.
 */
public class GeographicCoordinates
{
    ////////////////////////////////////////////////////////////////
    /// PRIVATE VARIABLES
    ////////////////////////////////////////////////////////////////
    
	/**
	 * Angle that defines the east-west position of a point on the surface of a celestial body.
	 * Usually denoted as lambda.
	 * [rad]
	 */
	private double longitude;
	
	/**
	 * Angle that defines the north-south position of a point on the surface of a celestial body.
	 * Usually denoted as phi.
	 * [rad]
	 */
	private double latitude;
	
	/**
	 * Height from the surface of the reference ellipsoid to the point.
	 * Usually denoted as h.
	 * [m]
	 */
	private double height;
	
	
	
    ////////////////////////////////////////////////////////////////
    /// PUBLIC METHODS
    ////////////////////////////////////////////////////////////////
	
	/**
	 * Sets the longitude of this {@link GeographicCoordinates}.
	 *
	 * Longitude is usually denoted as lambda.
	 *
	 * @param longitude		angle that defines the east-west position of a point on the surface of a celestial body. [rad]
	 */
	public void setLongitude( double longitude )
	{
		this.longitude = longitude;
	}
	
	
	/**
	 * Returns the longitude of this {@link GeographicCoordinates}.
	 * 
	 * The longitude is the angle that defines the east-west position of a point on the surface of a celestial body.
	 * It is usually denoted as lambda.
	 *
	 * @return	longitude of this {@link GeographicCoordinates}. [rad]
	 */
	public double getLongitude()
	{
		return this.longitude;
	}
	
	
	/**
	 * Sets the latitude of this {@link GeographicCoordinates}.
	 *
	 * Latitude is usually denoted as phi.
	 *
	 * @param latitude	angle that defines the north-south position of a point on the surface of a celestial body. [rad]
	 */
	public void setLatitude( double latitude )
	{
		this.latitude = latitude;
	}
	
	
	/**
	 * Returns the latitude of this {@link GeographicCoordinates}.
	 * 
	 * The latitude is the angle that defines the north-south position of a point on the surface of a celestial body.
	 * It is usually denoted as phi.
	 *
	 * @return	latitude of this {@link GeographicCoordinates}. [rad]
	 */
	public double getLatitude()
	{
		return this.latitude;
	}
	
	
	/**
	 * Sets the height of this {@link GeographicCoordinates}.
	 * 
	 * Height is usually denoted as h.
	 * 
	 * @param height	height from the surface of the reference ellipsoid to the point. [m]
	 */
	public void setHeight( double height )
	{
		this.height = height;
	}
	
	
	/**
	 * Returns the height of this {@link GeographicCoordinates}.
	 * 
	 * The height is the altitude from the surface of the reference ellipsoid to the point.
	 * It is usually denoted as h.
	 * 
	 * @return	height of this {@link GeographicCoordinates}. [m]
	 */
	public double getHeight()
	{
		return this.height;
	}
	
	
	/**
	 * Returns the orientation from  Body-Centered; Body-Fixed frame  to  East, North, Up frame.
	 * 
	 * The output is a {@link UnitQuaternion} that rotates vectors expressed in BCBF to the same vectors expressed in ENU.
	 * This is accomplished by:
	 * - a rotation of -lambda around the z axis,
	 * - a rotation of phi around the y axis,
	 * - the axis permutation  y -> x (E),  z -> y (N),  x -> z (U), or equivalently, a -120 deg rotation around the (1,1,1) axis.
	 * Such rotation composition can be expressed as:
	 * 
	 * R_ENU_BCBF  =  R_0  R_y( phi )  R_z( -lambda ) =
	 * 
	 * 				 ( 0  1  0 )  (  cos(phi)  0  sin(phi) )  (  cos(lambda)  sin(lambda)  0 )
	 * 			   = ( 0  0  1 )  (  0         1  0        )  ( -sin(lambda)  cos(lambda)  0 )
	 * 				 ( 1  0  0 )  ( -sin(phi)  0  cos(phi) )  (  0            0            1 )
	 * 
	 * 				 ( -sin(lambda)            cos(lambda)           0        )
	 * 			   = ( -sin(phi) cos(lambda)  -sin(phi) sin(lambda)  cos(phi) )
	 * 				 (  cos(phi) cos(lambda)   cos(phi) sin(lambda)  sin(phi) )
	 * 
	 * @return	{@link UnitQuaternion} that describes the orientation from Body-Centered; Body-Fixed frame to East North Up frame.
	 */
	public UnitQuaternion getOrientationEnuFromBcbf()
	{
		/* Quaternion that produces the rotation matrix:
		 *  ( 0  1  0 )
		 *  ( 0  0  1 )
		 *  ( 1  0  0 )
		 */
		UnitQuaternion q0 = UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 0.5 , -0.5 , -0.5 , -0.5 ) );
		UnitQuaternion q_ENU_BCBF = q0.multiplyInplace( this.getOrientationUenFromBcbf() );
		return q_ENU_BCBF;
	}
	
	
	/**
	 * Returns the orientation from  Body-Centered; Body-Fixed frame  to  North, East, Down frame.
	 * 
	 * The output is a {@link UnitQuaternion} that rotates vectors expressed in BCBF to the same vectors expressed in NED.
	 * This is accomplished by:
	 * - a rotation of -lambda around the z axis,
	 * - a rotation of phi around the y axis,
	 * - the axis permutation  -z -> x (N),  y -> y (E),  x -> z (D), or equivalently, a 90 deg rotation around the (0,1,0) axis.
	 * Such rotation composition can be expressed as:
	 * 
	 * R_NED_BCBF  =  R_0  R_y( phi )  R_z( -lambda ) =
	 * 
	 * 				 (  0  0  1 )  (  cos(phi)  0  sin(phi) )  (  cos(lambda)  sin(lambda)  0 )
	 * 			   = (  0  1  0 )  (  0         1  0        )  ( -sin(lambda)  cos(lambda)  0 )
	 * 				 ( -1  0  0 )  ( -sin(phi)  0  cos(phi) )  (  0            0            1 )
	 * 
	 * 			   = ( -sin(phi) cos(lambda)  -sin(phi) sin(lambda)   cos(phi) )
	 * 				 ( -sin(lambda)            cos(lambda)            0        )
	 * 				 ( -cos(phi) cos(lambda)  -cos(phi) sin(lambda)  -sin(phi) )
	 * 
	 * @return	{@link UnitQuaternion} that describes the orientation from Body-Centered; Body-Fixed frame to  North, East, Down.
	 */
	public UnitQuaternion getOrientationNedFromBcbf()
	{
		/* Quaternion that produces the rotation matrix:
		 *  (  0  0  1 )
		 *  (  0  1  0 )
		 *  ( -1  0  0 )
		 */
		UnitQuaternion q0 = UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 1.0 , 0.0 , 1.0 , 0.0 ).normalizeInplace() );
		UnitQuaternion q_NED_BCBF = q0.multiplyInplace( this.getOrientationUenFromBcbf() );
		return q_NED_BCBF;
	}
	
	
	/**
	 * Returns the orientation from  North, East, Down frame  to  East, North, Up frame.
	 * 
	 * The output is a {@link UnitQuaternion} that rotates vectors expressed in NED to the same vectors expressed in ENU.
	 * This is accomplished by a rotation of 180 deg around the (1,1,0) axis.
	 * Such rotation leads to the axis permutation  y_NED -> x_ENU,  x_NED -> y_ENU,  -z_NED -> z_ENU
	 * 
	 * Note that the transformation from NED to ENU happens to be the same as the one from ENU to NED due to the symmetry of the transformation.
	 * 
	 * @return	orientation from  North, East, Down frame  to  East, North, Up frame.
	 */
	public UnitQuaternion getOrientationEnuFromNed()
	{
		UnitQuaternion q_ENU_NED = UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 0.0 , 1.0 , 1.0 , 0.0 ).normalizeInplace() );
		return q_ENU_NED;
	}
	
	
	/**
	 * Returns the orientation from  East, North, Up frame  to  North, East, Down frame.
	 * 
	 * The output is a {@link UnitQuaternion} that rotates vectors expressed in ENU to the same vectors expressed in NED.
	 * This is accomplished by a rotation of 180 deg around the (1,1,0) axis.
	 * Such rotation leads to the axis permutation  y_ENU -> x_NED,  x_ENU -> y_NED,  -z_ENU -> z_NED
	 * 
	 * Note that the transformation from ENU to NED happens to be the same as the one from NED to ENU due to the symmetry of the transformation.
	 * 
	 * @return	orientation from  East, North, Up frame  to  North, East, Down frame.
	 */
	public UnitQuaternion getOrientationNedFromEnu()
	{
		UnitQuaternion q_NED_ENU = UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 0.0 , 1.0 , 1.0 , 0.0 ).normalizeInplace() );
		return q_NED_ENU;
	}
	
	
	/**
	 * Returns the 3x3 position covariance matrix expressed in BCBF by rotating
	 * the covariance matrix expressed in ENU frame at the current geographic location.
	 *
	 * @param covarianceEnu		3x3 covariance matrix expressed in ENU frame.
	 * @return	3x3 position covariance matrix in ECEF.
	 */
	public MatrixReal positionCovarianceEcefFromEnu( MatrixReal covarianceEnu )
	{
		UnitQuaternion q_ENU_BCBF = this.getOrientationEnuFromBcbf();
		MatrixReal R_BCBF_ENU = q_ENU_BCBF.inverseMultiplicativeInplace().toRotationMatrix();
		MatrixReal L = R_BCBF_ENU.multiply( covarianceEnu.choleskyDecomposition() );
		return L.multiply( L.transpose() );
	}
	
	
	
    ////////////////////////////////////////////////////////////////
    /// PRIVATE METHODS
    ////////////////////////////////////////////////////////////////
	
	/**
	 * Returns the orientation from Body-Centered; Body-Fixed frame to  Up, East, North.
	 * 
	 * Note that Up, East, North is not a standard reference frame,
	 * but it is an intermediate step in both  BCBF -> ENU  and  BCBF -> NED  transformations.
	 * 
	 * The output is a {@link UnitQuaternion} that rotates vectors expressed in BCBF to the same vectors expressed in UEN.
	 * This is accomplished by:
	 * - a rotation of -lambda around the z axis,
	 * - a rotation of phi around the y axis,
	 * 
	 * Such rotation composition can be expressed as:
	 * 
	 * R_UEN_BCBF  =  R_y( phi )  R_z( -lambda ) =
	 * 
	 * 				 (  cos(phi)  0  sin(phi) )  (  cos(lambda)  sin(lambda)  0 )
	 * 			   = (  0         1  0        )  ( -sin(lambda)  cos(lambda)  0 )
	 * 				 ( -sin(phi)  0  cos(phi) )  (  0            0            1 )
	 * 
	 * 				 (  cos(phi) cos(lambda)   cos(phi) sin(lambda)  sin(phi) )
	 * 			   = ( -sin(lambda)            cos(lambda)           0        )
	 * 			     ( -sin(phi) cos(lambda)  -sin(phi) sin(lambda)  cos(phi) )
	 * 
	 * @return	{@link UnitQuaternion} that describes the orientation from Body-Centered; Body-Fixed frame to  Up, East, North.
	 */
	private UnitQuaternion getOrientationUenFromBcbf()
	{
		// Rotation of phi around the y axis.
		UnitQuaternion qy = UnitQuaternion.fromAngleAndUnitVector(
				this.getLatitude() , Vector3.j() );
		// Rotation of -lambda around the z axis.
		UnitQuaternion qz = UnitQuaternion.fromAngleAndUnitVector(
				-this.getLongitude() , Vector3.k() );
		return qy.multiplyInplace( qz );
	}
	
}
