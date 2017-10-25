package org.codehaus.mojo.license.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.maven.model.License;
import org.junit.Assert;
import org.junit.Test;

public class AboutFilesLicenseResolverTest
{

    private AboutFileLicenseResolver resolver = new AboutFileLicenseResolver();

    @Test
    public void resolve_licensesFromAboutWithUrl()
	throws Exception
    {
	File file = getOsgiResourceFile( "javax.annotation_1.2.0.v201401042248_noCode.jar" );
	License license = resolve( file );
	Assert.assertEquals( "http://glassfish.java.net/public/CDDL+GPL.html", license.getUrl() );
    }

    @Test
    public void resolve_licensesFromAboutWithoutUrl()
	throws Exception
    {
	File file = getOsgiResourceFile( "com.ibm.icu_54.1.1.v201501272100_noCode.jar" );
	License license = resolve( file );
	Assert.assertTrue( license.getUrl().startsWith( "jar:file:" ) );
	Assert.assertTrue( license.getUrl().endsWith( ".jar!/about_files/license.html" ) );
    }

    @Test
    public void resolve_fromAbout()
	throws Exception
    {
	File file = getOsgiResourceFile( "javax.mail_1.4.0.v201005080615_noCode.jar" );
	License license = resolve( file );
	Assert.assertEquals( "http://www.apache.org/licenses/", license.getUrl() );
    }

    @Test
    public void resolve_withoutAbout()
	throws Exception
    {
	File file = getOsgiResourceFile( "jul.to.slf4j_1.7.22_noCode.jar" );
	License license = resolve( file );
	Assert.assertEquals( license, null );
    }

    @Test
    public void resolve_linebreakBeforeHref()
	throws Exception
    {
	File file = getOsgiResourceFile( "org.easymock_2.4.0.v20090202-0900_noCode.jar" );
	License license = resolve( file );
	Assert.assertEquals( "http://www.easymock.org/License.html", license.getUrl() );
    }

    @Test
    public void resolve_targetInsideAtag()
	throws Exception
    {
	File file = getOsgiResourceFile( "org.eclipse.osgi.services_3.5.0.v20150519-2006_noCode.jar" );
	License license = resolve( file );
	Assert.assertEquals( "http://www.apache.org/licenses/LICENSE-2.0.html", license.getUrl() );
    }

    @Test
    public void resolve_untypicalPrefix()
	throws Exception
    {
	File file = getOsgiResourceFile( "javax.inject_1.0.0.v20091030_noCode.jar" );
	License license = resolve( file );
	Assert.assertEquals( "http://www.apache.org/licenses/LICENSE-2.0", license.getUrl() );
    }

    private License resolve( File file )
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
