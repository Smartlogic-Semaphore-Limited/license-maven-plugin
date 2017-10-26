package org.codehaus.mojo.license.osgi;

import org.apache.maven.model.License;
import org.junit.Assert;
import org.junit.Test;

public class JarEmbeddedFilesTest
{

    private JarEmbeddedFiles helper = new JarEmbeddedFiles();

    @Test
    public void convertUrlToLocalFile_untouched()
        throws Exception
    {
        License license = create( "http://example.com" );
        helper.convertUrlToLocalFile( license );
        Assert.assertEquals( "http://example.com", license.getUrl() );
    }

    @Test
    public void convertUrlToLocalFile_fixed()
        throws Exception
    {
        License license = create( "jar:file:/dev/c/local!/about.html" );
        helper.convertUrlToLocalFile( license );
        Assert.assertEquals( "file:about.html", license.getUrl() );
    }

    @Test
    public void convertUrlToLocalFile_fixedNested()
        throws Exception
    {
        License license = create( "jar:file:/dev/c/local!/about_files/license.html" );
        helper.convertUrlToLocalFile( license );
        Assert.assertEquals( "file:about_files/license.html", license.getUrl() );
    }

    @Test
    public void getLocalFilepath_noJar()
        throws Exception
    {
        String path = helper.getLocalFilepath( "test.html" );
        Assert.assertEquals( null, path );
    }

    @Test
    public void getLocalFilepath_jar()
        throws Exception
    {
        String path = helper.getLocalFilepath( "jar:file:/dev/c/local!/about_files/license.html" );
        Assert.assertEquals( "about_files/license.html", path );
    }

    private License create( String url )
    {
        License license = new License();
        license.setUrl( url );
        return license;
    }

}
