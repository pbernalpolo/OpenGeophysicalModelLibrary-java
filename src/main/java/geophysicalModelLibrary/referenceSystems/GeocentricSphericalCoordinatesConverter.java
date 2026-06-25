package geophysicalModelLibrary.referenceSystems;


import numericalLibrary.types.Vector3;



public class GeocentricSphericalCoordinatesConverter
{
	
	private final double radius;
	
	
	public static GeocentricSphericalCoordinatesConverter fromRadius( double radius )
	{
		GeocentricSphericalCoordinatesConverter output = new GeocentricSphericalCoordinatesConverter( radius );
		return output;
	}
	
	
	public GeographicCoordinates fromCartesian( Vector3 position )
	{
		GeographicCoordinates output = new GeographicCoordinates();
		output.setLatitude(
				Math.atan2( position.z() , Math.sqrt( position.x() * position.x() + position.y() * position.y() ) )
				);
		output.setLongitude(
				Math.atan2( position.y() , position.x() )
				);
		output.setHeight( position.norm() - this.radius );
		return output;
	}
	
	
	public Vector3 toCartesian( GeographicCoordinates coordinates )
	{
		double r = this.radius + coordinates.getHeight();
		double phi = coordinates.getLatitude();
		double lambda = coordinates.getLongitude();
		double cos_phi = Math.cos( phi );
		return Vector3.fromComponents(
				r * cos_phi * Math.cos( lambda ) ,
				r * cos_phi * Math.sin( lambda ) ,
				r * Math.sin( phi ) );
	}
	
	
	private GeocentricSphericalCoordinatesConverter( double radius )
	{
		this.radius = radius;
	}
	
}
