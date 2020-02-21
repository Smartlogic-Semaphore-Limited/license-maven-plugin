package org.codehaus.mojo.license.ext;

import org.apache.maven.model.License;

public class LicenseFactory
{

    public static License create( String licenseName, String licenseUrl )
    {
        License license = new License();
        license.setName( licenseName );
        license.setUrl( licenseUrl );
        return license;
    }

}
