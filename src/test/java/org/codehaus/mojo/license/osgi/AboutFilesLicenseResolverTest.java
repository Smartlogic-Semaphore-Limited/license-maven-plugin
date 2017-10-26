package org.codehaus.mojo.license.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.maven.model.License;
import org.junit.Assert;
import org.junit.Test;

public class AboutFilesLicenseResolverTest
{

    private AboutFileLicenseResolver resolver = new AboutFileLicenseResolver();

    @Test
    public void resolve_fromAbout()
        throws Exception
    {
        File file = getOsgiResourceFile( "javax.mail_1.4.0.v201005080615_noCode.jar" );
        List<License> licenses = resolve( file );
        License aboutLicense = licenses.get( 0 );
        License thirdPartyLicense = licenses.get( 1 );
        Assert.assertEquals( 2, licenses.size() );
        Assert.assertTrue( aboutLicense.getUrl().matches( "jar:file:.*!/about.html" ) );
        Assert.assertTrue( thirdPartyLicense.getUrl().matches( "jar:file:.*!/about_files/LICENSE.txt" ) );
    }

    @Test
    public void resolve_withoutAbout()
        throws Exception
    {
        File file = getOsgiResourceFile( "jul.to.slf4j_1.7.22_noCode.jar" );
        List<License> licenses = resolve( file );
        Assert.assertEquals( 0, licenses.size() );
    }

    @Test
    public void resolve_multipleThirdPartyLicenses()
        throws Exception
    {
        File file = getOsgiResourceFile( "javax.xml_1.3.4.v201005080400_noCode.jar" );
        List<License> licenses = resolve( file );
        Assert.assertEquals( 7, licenses.size() );
        Assert.assertTrue( licenses.get( 0 ).getUrl().matches( "jar:file:.*!/about.html" ) );
        Assert.assertTrue( licenses.get( 1 ).getUrl().matches( "jar:file:.*!/about_files/LICENSE" ) );
        Assert.assertTrue(
                licenses.get( 2 ).getUrl().matches( "jar:file:.*!/about_files/LICENSE.dom-documentation.txt" ) );
    }

    @Test
    public void resolve_noEmbeddedLicense()
        throws Exception
    {
        File file = getOsgiResourceFile( "javax.inject_1.0.0.v20091030_noCode.jar" );
        List<License> licenses = resolve( file );
        License aboutLicense = licenses.get( 0 );
        Assert.assertEquals( 1, licenses.size() );
        Assert.assertTrue( aboutLicense.getUrl().matches( "jar:file:.*!/about.html" ) );
    }

    private List<License> resolve( File file )
        throws URISyntaxException, IOException
    {
        return resolver.resolve( "name", file );
    }

    private File getOsgiResourceFile( String path )
    {
        return getResourceFile( "osgi/" + path );
    }

    private File getResourceFile( String path )
    {
        return new File( "src/test/resources/" + path );
    }

}
