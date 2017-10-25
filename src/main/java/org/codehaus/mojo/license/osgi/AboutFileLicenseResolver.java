package org.codehaus.mojo.license.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.License;
import org.codehaus.mojo.license.ext.LicenseFactory;

public class AboutFileLicenseResolver
{

    private static final String FILENAME_PATTERN_STR = "Third Party Content.*<a\\s+href=\"about_files/(\\S+)\"(.*?)>";
    private static final Pattern FILENAME_PATTERN = Pattern.compile( FILENAME_PATTERN_STR, Pattern.DOTALL );

    private static final String URL_PATTERN_STR = "Third Party Content.*also.available.at.<a\\s+href=\"(\\S+)\"(.*?)>";
    private static final Pattern URL_PATTERN = Pattern.compile( URL_PATTERN_STR, Pattern.DOTALL );

    public License resolve( String artifactId, File file )
	throws IOException
    {
	JarFile jarFile = new JarFile( file );
	try
	{
	    ZipEntry entry = jarFile.getEntry( "about.html" );
	    if ( entry != null )
	    {
		return resolveFromAbout( artifactId, file, jarFile, entry );
	    }
	    return null;
	}
	finally
	{
	    jarFile.close();
	}
    }

    private License resolveFromAbout( String artifactId, File file, JarFile jarFile, ZipEntry entry )
	throws IOException, MalformedURLException
    {
	String content = readContent( jarFile, entry );
	String licenseUrl = findLicenseUrl( content );
	if ( licenseUrl != null )
	{
	    return LicenseFactory.create( artifactId, licenseUrl );
	}
	String licenseFilename = findLicenseFilename( content );
	if ( licenseFilename != null )
	{
	    URL url = toJarUrl( file, "about_files/" + licenseFilename );
	    return LicenseFactory.create( artifactId, url.toString(), true );
	}

	URL url = toJarUrl( file, entry.getName() );
	return LicenseFactory.create( artifactId, url.toString(), true );
    }

    private URL toJarUrl( File file, String localPath )
	throws MalformedURLException
    {
	URL fileUrl = file.toURI().toURL();
	return new URL( "jar", "", fileUrl.toString() + "!/" + localPath );
    }

    private String findLicenseFilename( String content )
    {
	return findGroup( content, FILENAME_PATTERN, 1 );
    }

    private String findLicenseUrl( String content )
    {
	return findGroup( content, URL_PATTERN, 1 );
    }

    private String findGroup( String content, Pattern pattern, int groupNumber )
    {
	Matcher matcher = pattern.matcher( content );
	if ( matcher.find() )
	{
	    if ( groupNumber <= matcher.groupCount() )
	    {
		return matcher.group( groupNumber );
	    }
	}
	return null;
    }

    private String readContent( JarFile jarFile, ZipEntry entry )
	throws IOException
    {
	InputStream inputStream = jarFile.getInputStream( entry );
	StringWriter writer = new StringWriter();
	IOUtils.copy( inputStream, writer, StandardCharsets.UTF_8.name() );
	return writer.toString();
    }

}
