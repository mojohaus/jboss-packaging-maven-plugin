package org.codehaus.mojo.jboss.packaging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Abstract super class for all the packaging mojos.  This class contains
 * the logic for actually building the packaging types.
 * 
 */
public abstract class AbstractPackagingMojo
    extends AbstractMojo
{

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory for the generated packaging.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classesDirectory;

    /**
     * The directory where the JBoss packaging is built.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File packagingDirectory;

    /**
     * The location of the jboss deployment descriptor file (e.g.,
     * jboss-service.xml, jboss-spring.xml, etc). If it is present in the
     * META-INF directory in src/main/resources with that name then it will
     * automatically be included. Otherwise this parameter must be set.
     *
     * @parameter
     */
    protected File deploymentDescriptorFile;

    /**
     * The directory where to put the libs.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}/lib"
     * @required
     */
    private File libDirectory;

    /**
     * The name of the generated packaging archive.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String archiveName;

    /**
     * All artifacts are excluded
     *
     * @parameter expression="${excludeAll}" default-value="false"
     */
    private boolean excludeAll;

    /**
     * Artifacts excluded from packaging within the generated archive file. Use
     * artifactId:groupId in nested exclude tags.
     *
     * @parameter
     */
    private Set excludes;

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The maven archive configuration to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * The manifest file for the archive.
     *
     * @parameter
     */
    private File manifest;

    /**
     * Classifier to add to the generated artifact. If given, the artifact will be an attachment instead.
     *
     * @parameter
     */
    private String classifier;

    /**
     * Whether this is the main artifact being constructed.
     *
     * @parameter default-value="true"
     */
    private boolean primaryArtifact;

    /**
     * The project helper.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The artifact handler manager.
     *
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * @return the maven project
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * @return the packaging directory
     */
    public File getPackagingDirectory()
    {
        return packagingDirectory;
    }

    /**
     * Get the name of the deployment descriptor file.
     *
     * Sublcasses must override this method and provide the proper name for
     * their type of archive packaging
     *
     * @return deployment descriptor file name, sans path
     */
    public abstract String getDeploymentDescriptorFilename();

    /**
     * Get the type of the artifact.
     *
     * @return The type of the generated artifact.
     */
    public abstract String getArtifactType();

    /**
     * If no deployment descriptor filesnames are found, check for
     * the existence of alternates before failing.
     *
     * Subclasses are not required to override this method.
     *
     * @return alternate deployment descriptor filenames
     */
    public String[] getAlternateDeploymentDescriptorFilenames()
    {
        return null;
    }

    /**
     * @return The directory to write the archive
     */
    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @throws MojoExecutionException if an error occurred
     * @throws MojoFailureException if an error occurred
     */
    public void buildExplodedPackaging()
        throws MojoExecutionException, MojoFailureException
    {
        buildExplodedPackaging( Collections.EMPTY_SET );
    }


    /**
     * Build the package in an exploded format.
     * 
     * @param excludes File patterns to exclude from the packaging.
     * 
     * @throws MojoExecutionException if an error occurred
     * @throws MojoFailureException if an error occurred
     */
    public void buildExplodedPackaging( Set excludes )
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Exploding JBoss packaging..." );

        if ( excludes == null )
        {
            excludes = Collections.EMPTY_SET;
        }

        packagingDirectory.mkdirs();
        libDirectory.mkdirs();
        try
        {
            getLog().info( "Assembling JBoss packaging " + project.getArtifactId() + " in " + packagingDirectory );

            if ( classesDirectory.exists() && ( !classesDirectory.equals( packagingDirectory ) ) )
            {
                FileUtils.copyDirectoryStructure( classesDirectory, packagingDirectory );
            }

            File packagingFileTargetParent = new File( packagingDirectory, "META-INF" );
            File packagingFileTarget = new File( packagingFileTargetParent, getDeploymentDescriptorFilename() );
            if ( !packagingFileTarget.exists() )
            {
                if ( !packagingFileTargetParent.exists() )
                {
                    packagingFileTargetParent.mkdirs();
                }

                if ( deploymentDescriptorFile == null || !deploymentDescriptorFile.exists() )
                {
                    // Check alternates list
                    StringBuffer buffer = new StringBuffer();
                    buffer.append( getDeploymentDescriptorFilename() );

                    String[] alternateDescriptorFilenames = getAlternateDeploymentDescriptorFilenames();
                    if ( alternateDescriptorFilenames != null )
                    {
                        for ( int i = 0; i < alternateDescriptorFilenames.length; i++ )
                        {
                            buffer.append( ", " );
                            buffer.append( alternateDescriptorFilenames[i] );

                            deploymentDescriptorFile =
                                new File( packagingFileTargetParent, alternateDescriptorFilenames[i] );
                            if ( deploymentDescriptorFile != null && deploymentDescriptorFile.exists() )
                            {
                                break;
                            }
                        }
                    }

                    if ( deploymentDescriptorFile == null || !deploymentDescriptorFile.exists() )
                    {
                        throw new MojoExecutionException( "Could not find descriptor files: " + buffer.toString() );
                    }
                }

                FileUtils.copyFile( deploymentDescriptorFile, packagingFileTarget );
            }

            Set artifacts = project.getArtifacts();
            List rejects = new ArrayList();
            final Set includedArtifacts = new HashSet();
            final ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
            getLog().info( "" );
            getLog().info( "    Including artifacts: " );
            getLog().info( "    -------------------" );
            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( !artifact.isOptional() && filter.include( artifact ) )
                {
                    String descriptor = artifact.getGroupId() + ":" + artifact.getArtifactId();

                    if ( !excludeAll && artifact.getArtifactHandler().isAddedToClasspath() && !excludes.contains(
                        descriptor ) )
                    {
                        getLog().info( "        o " + descriptor );

                        String name = getArtifactName( artifact );
                        if ( !includedArtifacts.add( name ) )
                        {
                            name = artifact.getGroupId() + "-" + name;
                            getLog().debug( "Duplicate artifact discovered, using full name: " + name );
                        }

                        FileUtils.copyFile( artifact.getFile(), new File( libDirectory, name ) );
                    }
                    else
                    {
                        rejects.add( artifact );
                    }
                }
            }

            if ( !excludes.isEmpty() )
            {
                getLog().info( "" );
                getLog().info( "    Excluded artifacts: " );
                getLog().info( "    ------------------" );
                for ( int ii = 0; ii < rejects.size(); ii++ )
                {
                    getLog().info( "        o " + rejects.get( ii ) );
                }
            }
            else
            {
                getLog().info( "No artifacts have been excluded." );
            }
            getLog().info( "" );

            buildSpecificPackaging( excludes );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not explode JBoss packaging...", e );
        }
    }

    /**
     * Perform any packaging specific to this type.
     *
     * @param excludes The exclude list.
     * @throws MojoExecutionException For plugin failures.
     * @throws MojoFailureException   For unexpected plugin failures.
     * @throws IOException            For exceptions during IO operations.
     */
    protected void buildSpecificPackaging( final Set excludes )
        throws MojoExecutionException, MojoFailureException, IOException
    {
    }

    /**
     * 
     * @return The name of the archive
     */
    public String getArchiveName()
    {
        return archiveName;
    }

    /**
     * Generates the packaged archive.
     *
     * @throws IOException if there is a problem
     * @throws ArchiverException if there is a problem
     * @throws ManifestException if there is a problem
     * @throws DependencyResolutionRequiredException if there is a problem
     * @throws MojoExecutionException if there is a problem
     * @throws MojoFailureException if there is a problem
     */
    protected void performPackaging()
        throws IOException, ArchiverException, ManifestException, DependencyResolutionRequiredException,
        MojoExecutionException, MojoFailureException
    {

        final ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( getArtifactType() );
        final String extension = artifactHandler.getExtension();
        final String type = getArtifactType();

        final File archiveFile = calculateFile( outputDirectory, archiveName, classifier, extension );

        buildExplodedPackaging( excludes );

        // generate archive file
        getLog().info( "Generating JBoss packaging " + archiveFile.getAbsolutePath() );
        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver( jarArchiver );
        archiver.setOutputFile( archiveFile );
        jarArchiver.addDirectory( getPackagingDirectory() );
        if ( manifest != null )
        {
            jarArchiver.setManifest( manifest );
        }

        // create archive
        archiver.createArchive( getProject(), archive );
        if ( classifier != null )
        {
            projectHelper.attachArtifact( project, type, classifier, archiveFile );
        }
        else if ( primaryArtifact )
        {
            final Artifact artifact = project.getArtifact();
            artifact.setFile( archiveFile );
            artifact.setArtifactHandler( artifactHandler );
        }
    }

    /**
     * Calculate the name of the archive file.
     *
     * @param outputDirectory The output directory.
     * @param archiveName     The name of the artifact archive.
     * @param classifier      The classifier of the artifact.
     * @param extension       The artifact archive extension.
     * 
     * @return The archive file.
     */
    private static File calculateFile( final File outputDirectory, final String archiveName, final String classifier,
                                       final String extension )
    {
        final String basename;
        if ( StringUtils.isEmpty( classifier ) )
        {
            basename = archiveName;
        }
        else
        {
            basename = archiveName + '-' + classifier;
        }
        return new File( outputDirectory, basename + '.' + extension );
    }

    /**
     * Get the name of the artifact.
     *
     * @param artifact The current artifact.
     * @return The name of the artifact.
     */
    private static String getArtifactName( final Artifact artifact )
    {
        final String artifactName;
        final String classifier = artifact.getClassifier();

        if ( StringUtils.isEmpty( classifier ) )
        {
            artifactName = artifact.getArtifactId() + '-' + artifact.getVersion() + '.'
                + artifact.getArtifactHandler().getExtension();
        }
        else
        {
            artifactName = artifact.getArtifactId() + '-' + artifact.getVersion() + '-' + classifier + '.'
                + artifact.getArtifactHandler().getExtension();
        }
        return artifactName;
    }
}
