package geophysicalModelLibrary.referenceSystems;


import numericalLibrary.types.MatrixReal;
import numericalLibrary.types.Quaternion;
import numericalLibrary.types.UnitQuaternion;
import numericalLibrary.types.Vector3;



/**
 * Coordinates of a point defined by a latitude and a longitude,
 * together with a vertical coordinate provided by the concrete implementation:
 * a height above the reference ellipsoid in {@link GeodeticCoordinates},
 * or a radius from the center in {@link GeocentricCoordinates}.
 * <p>
 * The methods declared here depend only on the latitude and the longitude,
 * so they are shared by both kinds of coordinates through default methods.
 * The local frames (ENU, NED) are defined according to the coordinate's vertical:
 * for geodetic coordinates that vertical is the ellipsoid normal (the conventional local frame),
 * while for geocentric coordinates it is the radial direction.
 * The two differ by the deflection of the vertical (up to ~0.2 deg).
 * Use {@link GeodeticCoordinates} when the standard geodetic local frame is intended.
 */
public interface GeographicCoordinates
{
	////////////////////////////////////////////////////////////////
	/// PUBLIC ABSTRACT METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the latitude:
	 * the angle that defines the north-south position of the point.
	 * Usually denoted phi.
	 *
	 * @return	latitude. [rad]
	 */
	double getLatitude();


	/**
	 * Returns the longitude:
	 * the angle that defines the east-west position of the point.
	 * Usually denoted lambda.
	 *
	 * @return	longitude. [rad]
	 */
	double getLongitude();


	/**
	 * Sets the latitude:
	 * the angle that defines the north-south position of the point.
	 * Usually denoted phi.
	 *
	 * @param latitude	latitude. [rad]
	 */
	void setLatitude( double latitude );


	/**
	 * Sets the longitude:
	 * the angle that defines the east-west position of the point.
	 * Usually denoted lambda.
	 *
	 * @param longitude	longitude. [rad]
	 */
	void setLongitude( double longitude );



	////////////////////////////////////////////////////////////////
	/// DEFAULT METHODS
	////////////////////////////////////////////////////////////////
	
	/**
	 * Returns the orientation from  Body-Centered; Body-Fixed frame  to  East, North, Up frame.
	 * <p>
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
	default UnitQuaternion getOrientationEnuFromBcbf()
	{
		/* Quaternion that produces the axis permutation rotation matrix:
		 *  ( 0  1  0 )
		 *  ( 0  0  1 )
		 *  ( 1  0  0 )
		 */
		UnitQuaternion q0 = UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 0.5 , -0.5 , -0.5 , -0.5 ) );
		return q0.multiplyInplace( this.getOrientationUenFromBcbf() );
	}


	/**
	 * Returns the orientation from  Body-Centered; Body-Fixed frame  to  North, East, Down frame.
	 * <p>
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
	default UnitQuaternion getOrientationNedFromBcbf()
	{
		/* Quaternion that produces the axis permutation rotation matrix:
		 *  (  0  0  1 )
		 *  (  0  1  0 )
		 *  ( -1  0  0 )
		 */
		UnitQuaternion q0 = UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 1.0 , 0.0 , 1.0 , 0.0 ).normalizeInplace() );
		return q0.multiplyInplace( this.getOrientationUenFromBcbf() );
	}


	/**
	 * Returns the orientation from  North, East, Down frame  to  East, North, Up frame.
	 * <p>
	 * The output is a {@link UnitQuaternion} that rotates vectors expressed in NED to the same vectors expressed in ENU.
	 * This is accomplished by a rotation of 180 deg around the (1,1,0) axis.
	 * Such rotation leads to the axis permutation  y_NED -> x_ENU,  x_NED -> y_ENU,  -z_NED -> z_ENU
	 * 
	 * Note that the transformation from NED to ENU happens to be the same as the one from ENU to NED due to the symmetry of the transformation.
	 * 
	 * @return	orientation from  North, East, Down frame  to  East, North, Up frame.
	 */
	default UnitQuaternion getOrientationEnuFromNed()
	{
		return UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 0.0 , 1.0 , 1.0 , 0.0 ).normalizeInplace() );
	}


	/**
	 * Returns the orientation from  East, North, Up frame  to  North, East, Down frame.
	 * <p>
	 * The output is a {@link UnitQuaternion} that rotates vectors expressed in ENU to the same vectors expressed in NED.
	 * This is accomplished by a rotation of 180 deg around the (1,1,0) axis.
	 * Such rotation leads to the axis permutation  y_ENU -> x_NED,  x_ENU -> y_NED,  -z_ENU -> z_NED
	 * 
	 * Note that the transformation from ENU to NED happens to be the same as the one from NED to ENU due to the symmetry of the transformation.
	 * 
	 * @return	orientation from  East, North, Up frame  to  North, East, Down frame.
	 */
	default UnitQuaternion getOrientationNedFromEnu()
	{
		return UnitQuaternion.fromNormalizedQuaternion(
				Quaternion.fromComponents( 0.0 , 1.0 , 1.0 , 0.0 ).normalizeInplace() );
	}


	/**
	 * Returns the 3x3 position covariance matrix expressed in BCBF by rotating
	 * the covariance matrix expressed in the ENU frame at this geographic location.
	 *
	 * @param covarianceEnu		3x3 covariance matrix expressed in ENU frame.
	 * @return	3x3 position covariance matrix in BCBF.
	 */
	default MatrixReal positionCovarianceEcefFromEnu( MatrixReal covarianceEnu )
	{
		UnitQuaternion q_ENU_BCBF = this.getOrientationEnuFromBcbf();
		MatrixReal R_BCBF_ENU = q_ENU_BCBF.inverseMultiplicativeInplace().toRotationMatrix();
		MatrixReal L = R_BCBF_ENU.multiply( covarianceEnu.choleskyDecomposition() );
		return L.multiply( L.transpose() );
	}


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
