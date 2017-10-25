package org.codehaus.mojo.license.ext;

import org.apache.maven.model.License;
import org.codehaus.mojo.license.model.LicenseExt;

public class LicenseFactory
{

    public static License create( String licenseName, String licenseUrl )
    {
	return create( licenseName, licenseUrl, false );
    }

    public static License create( String licenseName, String licenseUrl, boolean isLocalJarUrl )
    {
	LicenseExt license = new LicenseExt();
	license.setName( licenseName );
	license.setLocalJarUrl( isLocalJarUrl );
	license.setUrl( licenseUrl );
	return license;
    }
}
