package org.codehaus.mojo.license.model;

@SuppressWarnings( "serial" )
public class LicenseExt
    extends
    org.apache.maven.model.License
{

    private String filename;
    private boolean isLocalJarUrl;

    public void setFilename( String filename )
    {
	this.filename = filename;
    }

    public String getFilename()
    {
	return filename;
    }

    public boolean isLocalJarUrl()
    {
	return isLocalJarUrl;
    }

    public void setLocalJarUrl( boolean isLocalJarUrl )
    {
	this.isLocalJarUrl = isLocalJarUrl;
    }

}
