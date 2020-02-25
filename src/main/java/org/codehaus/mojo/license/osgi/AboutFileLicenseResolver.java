package org.codehaus.mojo.license.osgi;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2020 Smartlogic
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.License;
import org.codehaus.mojo.license.ext.LicenseFactory;
import org.codehaus.plexus.component.annotations.Component;

/**
 * About file license resolver
 */
@Component( role = AboutFileLicenseResolver.class, hint = "default" )
public class AboutFileLicenseResolver
{

    private static final String ABOUT_HTML = "about.html";
    private static final String FILENAME_PATTERN_STR = "<a\\s+href=\"about_files/(\\S+)\"(.*?)>";
    private static final Pattern FILENAME_PATTERN = Pattern.compile( FILENAME_PATTERN_STR, Pattern.DOTALL );

    public List<License> resolve( String artifactId, File file )
        throws IOException
    {
        List<License> licenses = new ArrayList<License>();
        JarFile jarFile = new JarFile( file );
        try
        {
            ZipEntry entry = jarFile.getEntry( ABOUT_HTML );
            if ( entry != null )
            {
                licenses.add( createJarEmbeddedLicense( artifactId, file, ABOUT_HTML ) );
                String content = readContent( jarFile, entry );
                licenses.addAll( findThrirdPartyLicenses( artifactId, file, content ) );
            }
            return licenses;
        }
        finally
        {
            jarFile.close();
        }
    }

    private List<License> findThrirdPartyLicenses( String artifactId, File file, String content )
        throws MalformedURLException
    {
        List<License> licenses = new ArrayList<License>();
        for ( String licenseFilename : findLicenseFilenames( content ) )
        {
            License license = createJarEmbeddedLicense( artifactId, file, "about_files/" + licenseFilename );
            licenses.add( license );
        }
        return licenses;
    }

    private License createJarEmbeddedLicense( String artifactId, File file, String localPath )
        throws MalformedURLException
    {
        URL url = toJarUrl( file, localPath );
        return LicenseFactory.create( artifactId, url.toString() );
    }

    private URL toJarUrl( File file, String localPath )
        throws MalformedURLException
    {
        URL fileUrl = file.toURI().toURL();
        return new URL( "jar", "", fileUrl.toString() + "!/" + localPath );
    }

    private List<String> findLicenseFilenames( String content )
    {
        return findGroupOccurrences( content, FILENAME_PATTERN, 1 );
    }

    private List<String> findGroupOccurrences( String content, Pattern pattern, int groupNumber )
    {
        List<String> matches = new ArrayList<String>();
        Matcher matcher = pattern.matcher( content );
        while ( matcher.find() )
        {
            if ( groupNumber <= matcher.groupCount() )
            {
                matches.add( matcher.group( groupNumber ) );
            }
        }
        return matches;
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
