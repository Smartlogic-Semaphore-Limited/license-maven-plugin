package org.codehaus.mojo.license;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Extends DownloadLicensesMojo with support for OSGi bundles.
 *
 * @author Piotr Paczynski
 */
@Mojo( name = "download-osgi-licenses", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.PACKAGE )
public class DownloadOsgiLicensesMojo
    extends DownloadLicensesMojo
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

    @Component
    private ArtifactResolver artifactResovler;

    @Component
    private ArtifactFactory artifactFactory;

    @Override
    public String getExcludedArtifacts()
    {
        return excludedArtifacts;
    }

    @Override
    public String getExcludedGroups()
    {
        return excludedGroups;
    }

    @Override
    protected ProjectLicenseInfo createDependencyProject( MavenProject depMavenProject )
    {
        ProjectLicenseInfo dependencyProject = super.createDependencyProject( depMavenProject );
        if ( dependencyProject.getLicenses().isEmpty() )
        {
            readLicensesFromProject( dependencyProject.getLicenses(), depMavenProject );
        }
        return dependencyProject;
    }

    private void readLicensesFromProject( List<License> licenses, MavenProject depMavenProject )
    {
        Artifact artifact = resolveArtifact( depMavenProject );
        readLicensesFromArtifact( licenses, artifact );
    }

    private Artifact resolveArtifact( MavenProject project )
    {
        Artifact artifact = project.getArtifact();
        String key = ArtifactUtils.versionlessKey( artifact );
        artifact = (Artifact) this.project.getArtifactMap().get( key );
        return artifact;
    }

    private void readLicensesFromArtifact( List<License> licenses, Artifact artifact )
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
                    readLicensesFromAboutFile( licenses, artifact, jarFile );
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

    private void readLicensesFromManifest( List<License> licenses, Artifact artifact, JarFile jarFile )
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

    private void readLicensesFromEmbeddedPoms( final List<License> licenses, final JarFile jarFile )
    {
        jarFile.stream().filter( new Predicate<JarEntry>() {
            public boolean test( JarEntry entry )
            {
                return entry.getName().matches( "^META-INF/maven/.*/pom\\.xml$" );
            }
        } ).forEach( new Consumer<JarEntry>() {
            public void accept( JarEntry pomEntry )
            {
                readLicensesFromPom(licenses, jarFile, pomEntry);
            }
        } );
    }

    private void readLicensesFromPom( List<License> licenses, JarFile jarFile, JarEntry pomEntry )
    {
        getLog().debug( "Found embedded pom: " + pomEntry );
        try
        {
            InputStream is = jarFile.getInputStream( pomEntry );
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read( new InputStreamReader( is ) );
            while (model.getLicenses().isEmpty())
            {
                Parent p = model.getParent();
                if ( p == null )
                {
                    break;
                }
                Artifact parentArtifact =
                    artifactFactory.createProjectArtifact( p.getGroupId(), p.getArtifactId(), p.getVersion() );
                artifactResovler.resolve( parentArtifact, remoteRepositories, localRepository );
                model = reader.read( new FileReader( parentArtifact.getFile() ) );
            }
            for ( Object license : model.getLicenses() )
            {
                licenses.add( (License) license );
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

    void readLicensesFromAboutFile( List<License> licenses, Artifact artifact, JarFile jarFile )
        throws MalformedURLException
    {
        ZipEntry entry = jarFile.getEntry( "about.html" );
        if ( entry != null )
        {
            getLog().debug( "Found about file: " + entry );
            URL uri = artifact.getFile().toURI().toURL();
            String licenseUrl = new URL( "jar", "", uri.toString() + "!/" + entry.getName() ).toString();
            addLicense( licenses, artifact, licenseUrl );
        }
    }

    private void addLicense( List<License> licenses, Artifact artifact, String licenseUrl )
    {
        licenses.add( createLicense( artifact.getArtifactId(), licenseUrl ) );
    }

    private License createLicense( String licenseName, String licenseUrl )
    {
        License license = new License();
        license.setName( licenseName );
        license.setUrl( licenseUrl );
        return license;
    }

}
