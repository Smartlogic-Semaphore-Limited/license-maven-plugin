package org.codehaus.mojo.license.osgi;

import java.util.regex.Pattern;

import org.apache.maven.model.License;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = JarEmbeddedFiles.class, hint = "default" )
public class JarEmbeddedFiles
{

    private Pattern PATTERN = Pattern.compile( "jar:file:(.*)" );

    public void convertUrlToLocalFile( License license )
    {
        if ( isLocalJarPath( license.getUrl() ) )
        {
            String filepath = getLocalFilepath( license.getUrl() );
            if ( filepath != null )
            {
                license.setUrl( "file:" + filepath );
            }
        }
    }

    public boolean isLocalJarPath( String url )
    {
        return PATTERN.matcher( url ).matches();
    }

    public String getLocalFilepath( String url )
    {
        String marker = "!/";
        int index = url.indexOf( marker );
        if ( index > 0 )
        {
            return url.substring( index + marker.length() );
        }
        return null;
    }

}
