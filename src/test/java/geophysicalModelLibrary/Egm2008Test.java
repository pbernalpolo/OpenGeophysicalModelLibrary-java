package geophysicalModelLibrary;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;



/**
 * Implements test methods for {@link Egm2008}.
 * <p>
 * The tests load the EGM2008 {@code .gfc} file shipped under {@code res/}. When that file is not present
 * (it is large and not tracked in the repository), the tests are skipped instead of failing.
 */
public class Egm2008Test
{
	////////////////////////////////////////////////////////////////
	/// PRIVATE CONSTANTS
	////////////////////////////////////////////////////////////////

	/**
	 * Path to the {@code res/EGM2008.gfc} file, resolved relative to the location of the compiled test classes so that
	 * the tests work regardless of the working directory they are launched from. It is {@code null} when the file is absent.
	 */
	private static final String GFC_PATH = locateGfcPath();

	/**
	 * Tolerance used when comparing loaded coefficients with their reference values.
	 */
	private static final double COEFFICIENT_TOLERANCE = 1.0e-20;



	////////////////////////////////////////////////////////////////
	/// TEST METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Tests that the header metadata is parsed from the {@code .gfc} file.
	 */
	@Test
	void headerIsParsed() throws IOException
	{
		assumeTrue( GFC_PATH != null , "EGM2008.gfc not found; skipping." );
		Egm2008 model = Egm2008.fromFilePathAndMaximumDegree( GFC_PATH , 4 );
		assertEquals( "EGM2008" , model.modelName() );
		assertEquals( "tide_free" , model.tideSystem() );
		assertEquals( 3.986004415e14 , model.gravitationalParameter() , 1.0 );
		assertEquals( 6378136.3 , model.referenceRadius() , 1.0e-16 );
		assertEquals( 4 , model.maximumDegree() );
	}


	/**
	 * Tests that known coefficients are loaded with the expected values, including the {@code C_0^0 = 1} term written in Fortran notation.
	 */
	@Test
	void knownCoefficientsAreLoaded() throws IOException
	{
		assumeTrue( GFC_PATH != null , "EGM2008.gfc not found; skipping." );
		Egm2008 model = Egm2008.fromFilePathAndMaximumDegree( GFC_PATH , 5 );
		assertEquals(  1.0                     , model.normalizedC( 0 , 0 ) , COEFFICIENT_TOLERANCE );   // written as 1.0d0
		assertEquals(  0.0                     , model.normalizedS( 0 , 0 ) , COEFFICIENT_TOLERANCE );
		assertEquals( -0.484165143790815e-03   , model.normalizedC( 2 , 0 ) , COEFFICIENT_TOLERANCE );
		assertEquals(  0.243938357328313e-05   , model.normalizedC( 2 , 2 ) , COEFFICIENT_TOLERANCE );
		assertEquals( -0.140027370385934e-05   , model.normalizedS( 2 , 2 ) , COEFFICIENT_TOLERANCE );
		assertEquals(  0.174811795496002e-06   , model.normalizedC( 5 , 5 ) , COEFFICIENT_TOLERANCE );
		assertEquals( -0.669379935180165e-06   , model.normalizedS( 5 , 5 ) , COEFFICIENT_TOLERANCE );
	}


	/**
	 * Tests that the requested maximum degree truncates the loaded model.
	 */
	@Test
	void maximumDegreeTruncatesTheModel() throws IOException
	{
		assumeTrue( GFC_PATH != null , "EGM2008.gfc not found; skipping." );
		Egm2008 model = Egm2008.fromFilePathAndMaximumDegree( GFC_PATH , 17 );
		assertEquals( 17 , model.maximumDegree() );
	}


	/**
	 * Tests that loading from a non-existent file raises an {@link IOException}.
	 */
	@Test
	void missingFileRaisesIOException()
	{
		assertThrows( IOException.class , () -> Egm2008.fromFilePathAndMaximumDegree( "res/does-not-exist.gfc" , 4 ) );
	}


	/**
	 * Tests that a negative maximum degree is rejected.
	 */
	@Test
	void negativeMaximumDegreeIsRejected()
	{
		assumeTrue( GFC_PATH != null , "EGM2008.gfc not found; skipping." );
		assertThrows( IllegalArgumentException.class , () -> Egm2008.fromFilePathAndMaximumDegree( GFC_PATH , -1 ) );
	}



	////////////////////////////////////////////////////////////////
	/// PRIVATE METHODS
	////////////////////////////////////////////////////////////////

	/**
	 * Returns the path to {@code res/EGM2008.gfc}, or {@code null} if it cannot be found.
	 * <p>
	 * The {@code res} folder sits at the root of the {@code OpenGeophysicalModelLibrary-java} directory. The tests may be
	 * launched either from that directory (when it is the project) or from a parent repository that embeds it as a
	 * submodule, so the file is searched at both relative locations with respect to the working directory.
	 *
	 * @return	path to {@code res/EGM2008.gfc}, or {@code null} if it cannot be found.
	 */
	private static String locateGfcPath()
	{
		String[] candidatePaths = {
				"res/EGM2008.gfc",                                       // launched from OpenGeophysicalModelLibrary-java
				"lib/OpenGeophysicalModelLibrary-java/res/EGM2008.gfc" , // launched from a parent repository
		};
		for( String candidatePath : candidatePaths ) {
			if( Files.exists( Paths.get( candidatePath ) ) ) {
				return candidatePath;
			}
		}
		return null;
	}

}
