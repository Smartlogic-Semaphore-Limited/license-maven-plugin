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

import java.util.regex.Pattern;

import org.apache.maven.model.License;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Util methods for JAR files
 */
@Component( role = JarEmbeddedFiles.class, hint = "default" )
public class JarEmbeddedFiles
{

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
        final Pattern pattern = Pattern.compile( "jar:file:(.*)" );
        return pattern.matcher( url ).matches();
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
