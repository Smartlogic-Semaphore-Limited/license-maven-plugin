package org.codehaus.mojo.license;

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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.mojo.license.download.LicensedArtifact;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.ext.LicenseFactory;
import org.codehaus.mojo.license.osgi.AboutFileLicenseResolver;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Extends DownloadLicensesMojo with support for OSGi bundles.
 *
 * @author Piotr Paczynski
 */
@Mojo( name = "download-osgi-licenses", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.PACKAGE )
public class DownloadOsgiLicensesMojo
    extends
    DownloadLicensesMojo
{

    private static final String BUNDLE_LICENSE = "Bundle-License";

    /**
     * A filter to exclude some artifacts (regular expression).
     */
    @Parameter( property = "license.exludedArtifacts" )
    private String excludedArtifacts;

    /**
     * A filter to exclude some groups (regular expression).
     */
    @Parameter( property = "license.exludedGroups" )
    private String excludedGroups;

    /**
     * Location of the local repository.
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true )
    protected ArtifactRepository localRepository;

    @Component
    private ArtifactResolver artifactResovler;

    @Component
    private ArtifactFactory artifactFactory;

    @Requirement
    private MavenSession mavenSession;

    @Component
    private AboutFileLicenseResolver aboutFileLicenseResolver;

    @Override
    protected ProjectLicenseInfo createDependencyProject( LicensedArtifact licensedArtifact )
        throws MojoFailureException
    {
        ProjectLicenseInfo dependencyProject = super.createDependencyProject( licensedArtifact );
        if ( dependencyProject.getLicenses().isEmpty() )
        {
            readLicensesFromProject( dependencyProject.getLicenses(), licensedArtifact );
        }
        return dependencyProject;
    }

    private void readLicensesFromProject( List<ProjectLicense> licenses, LicensedArtifact licensedArtifact )
    {
        Artifact artifact = resolveArtifact( licensedArtifact );
        readLicensesFromArtifact( licenses, artifact );
    }

    private Artifact resolveArtifact( LicensedArtifact licensedArtifact )
    {
        String key = String.join( ":", licensedArtifact.getGroupId(), licensedArtifact.getArtifactId() );
        return this.project.getArtifactMap().get( key );
    }

    private void readLicensesFromArtifact( List<ProjectLicense> licenses, Artifact artifact )
    {
        File artifactFile = artifact.getFile();
        if ( artifactFile == null )
        {
            getLog().warn( "Artifact file is null: " + artifact );
            return;
        }

        try
        {
            JarFile jarFile = new JarFile( artifactFile );
            try
            {
                readLicensesFromManifest( licenses, artifact, jarFile );
                if ( licenses.isEmpty() )
                {
                    readLicensesFromEmbeddedPoms( licenses, jarFile );
                }
                if ( licenses.isEmpty() )
                {
                    readLicensesFromAboutFile( licenses, artifact, artifactFile );
                }
            }
            finally
            {
                jarFile.close();
            }
        }
        catch ( IOException e )
        {
            getLog().warn( "Cannot open jar file", e );
        }
    }

    private void readLicensesFromManifest( List<ProjectLicense> licenses, Artifact artifact, JarFile jarFile )
        throws IOException
    {
        Manifest manifest = jarFile.getManifest();
        if ( manifest == null )
        {
            getLog().debug( "Artifact does not have manifest: " + artifact );
            return;
        }
        Attributes attribtues = manifest.getMainAttributes();
        if ( attribtues == null )
        {
            getLog().warn( "Artifact does not have main attributes: " + artifact );
            return;
        }
        String licensesUrls = attribtues.getValue( BUNDLE_LICENSE );
        if ( licensesUrls != null )
        {
            getLog().debug( "Found license urls in manifest header: " + licensesUrls );
            for ( String licenseUrl : licensesUrls.split( " *, *" ) )
            {
                addLicense( licenses, artifact, licenseUrl );
            }
        }
    }

    private void readLicensesFromEmbeddedPoms( final List<ProjectLicense> licenses, final JarFile jarFile )
    {
        jarFile.stream().filter( new Predicate<JarEntry>() {
            @Override
            public boolean test( JarEntry entry )
            {
                return entry.getName().matches( "^META-INF/maven/.*/pom\\.xml$" );
            }
        } ).forEach( new Consumer<JarEntry>() {
            @Override
            public void accept( JarEntry pomEntry )
            {
                readLicensesFromPom( licenses, jarFile, pomEntry );
            }
        } );
    }

    private void readLicensesFromPom( List<ProjectLicense> licenses, JarFile jarFile, JarEntry pomEntry )
    {
        getLog().debug( "Found embedded pom: " + pomEntry );
        try
        {
            InputStream is = jarFile.getInputStream( pomEntry );
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read( new InputStreamReader( is ) );
            while ( model.getLicenses().isEmpty() )
            {
                Parent p = model.getParent();
                if ( p == null )
                {
                    break;
                }
                model = readModelFromArtifact( p.getGroupId(), p.getArtifactId(), p.getVersion() );
            }
            for ( Object license : model.getLicenses() )
            {
                licenses.add( (ProjectLicense) license );
            }
        }
        catch ( IOException e )
        {
            getLog().warn( "Artifact could not be resolved", e );
        }
        catch ( XmlPullParserException e )
        {
            getLog().warn( "Artifact could not be resolved", e );
        }
        catch ( ArtifactResolutionException e )
        {
            getLog().warn( "Artifact could not be resolved", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().debug( "Artifact not found", e );
        }
    }

    Model readModelFromArtifact( String groupId, String artifactId, String version )
        throws ArtifactResolutionException, ArtifactNotFoundException, FileNotFoundException, IOException,
        XmlPullParserException
    {
        Artifact parentArtifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );
        artifactResovler.resolve( parentArtifact, remoteRepositories, localRepository );
        return new MavenXpp3Reader().read( new FileReader( parentArtifact.getFile() ) );
    }

    void readLicensesFromAboutFile( List<ProjectLicense> licenses, Artifact artifact, File jarFile )
        throws IOException
    {
        for ( License license : aboutFileLicenseResolver.resolve( artifact.getArtifactId(), jarFile ) )
        {
            licenses.add( new ProjectLicense( license ) );
        }
    }

    private void addLicense( List<ProjectLicense> licenses, Artifact artifact, String licenseUrl )
    {
        licenses.add( new ProjectLicense( LicenseFactory.create( artifact.getArtifactId(), licenseUrl ) ) );
    }

}