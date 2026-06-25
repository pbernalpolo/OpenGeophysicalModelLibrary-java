package geophysicalModelLibrary.referenceSystems;


import numericalLibrary.types.Vector3;



public class GeodeticCoordinatesConverter
{
	
	private final double equatorialRadius;
	private final double eccentricitySquared;
	
	
	public static GeodeticCoordinatesConverter fromEquatorialRadiusAndPolarRadius( double equatorialRadius , double polarRadius )
	{
		double eccentricitySquared = 1.0 - ( polarRadius * polarRadius ) / ( equatorialRadius * equatorialRadius );
		GeodeticCoordinatesConverter output = new GeodeticCoordinatesConverter( equatorialRadius , eccentricitySquared );
		return output;
	}
	
	
	public static GeodeticCoordinatesConverter fromEquatorialRadiusAndEccentricity( double equatorialRadius , double eccentricity )
	{
		double eccentricitySquared = eccentricity * eccentricity;
		GeodeticCoordinatesConverter output = new GeodeticCoordinatesConverter( equatorialRadius , eccentricitySquared );
		return output;
	}
	
	
	public static GeodeticCoordinatesConverter fromWGS84()
	{
		// https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84
		return GeodeticCoordinatesConverter.fromEquatorialRadiusAndPolarRadius( 6378137.0 , 6356752.314245 );
	}
	
	
	public GeographicCoordinates fromCartesian( Vector3 position )
	{
		// We use Bowring's method.
		GeographicCoordinates output = new GeographicCoordinates();
		output.setLongitude(
				Math.atan2( position.y() , position.x() )
				);
		double p = Math.sqrt( position.x() * position.x() + position.y() * position.y() );
		double theta = Math.atan( position.z() / ( p * Math.sqrt( 1.0 - this.eccentricitySquared ) ) );
		double sin_theta = Math.sin( theta );
		double cos_theta = Math.cos( theta );
		double numerator = position.z() +
				this.equatorialRadius * this.eccentricitySquared / Math.sqrt( 1.0 - this.eccentricitySquared ) *
				sin_theta * sin_theta * sin_theta;
		double denominator = p - this.equatorialRadius * this.eccentricitySquared * cos_theta * cos_theta * cos_theta;
		double phi = Math.atan( numerator / denominator );
		output.setLatitude( phi );
		output.setHeight( p / Math.cos( phi ) - this.primeVerticalRadiusOfCurvature( phi ) );
		return output;
	}
	
	
	public Vector3 toCartesian( GeographicCoordinates coordinates )
	{
		double phi = coordinates.getLatitude();
		double lambda = coordinates.getLongitude();
		double h = coordinates.getHeight();
		double N_phi = this.primeVerticalRadiusOfCurvature( phi );
		double r = N_phi + h;
		double cos_phi = Math.cos( phi );
		return Vector3.fromComponents(
				r * cos_phi * Math.cos( lambda ) ,
				r * cos_phi * Math.sin( lambda ) ,
				( ( 1.0 - this.eccentricitySquared ) * N_phi + h ) * Math.sin( phi ) );
				//( r - this.eccentricitySquared * N_phi ) * Math.sin( phi ) );  Same but less readable, and perhaps less numerically stable.
	}
	
	
	private GeodeticCoordinatesConverter( double equatorialRadius , double eccentricitySquared )
	{
		this.equatorialRadius = equatorialRadius;
		this.eccentricitySquared = eccentricitySquared;
	}
	
	
	private double primeVerticalRadiusOfCurvature( double phi )
	{
		double sin_phi = Math.sin( phi );
		return this.equatorialRadius / Math.sqrt( 1.0 - this.eccentricitySquared * ( sin_phi * sin_phi ) );
	}
	
}
