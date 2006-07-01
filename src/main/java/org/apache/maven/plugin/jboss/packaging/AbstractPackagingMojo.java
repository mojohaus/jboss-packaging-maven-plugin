package org.apache.maven.plugin.jboss.packaging;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;

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
    private String outputDirectory;    

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
	 * The location of the jboss packaging-type configuration file (e.g.,
	 * jboss-service.xml, jboss-spring.xml, etc). If it is present in the
	 * META-INF directory in src/main/resources with that name then it will
	 * automatically be included. Otherwise this parameter must be set.
	 * 
	 * @parameter
	 */
	protected File packagingFile;

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
	 * Artifacts excluded from packaging within the generated archive file. Use
	 * artifactId:groupId in nested exclude tags.
	 * 
	 * @parameter
	 */
	Set excludes;

	/**
	   * The Jar archiver.
	   * 
	   * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
	   * @required
	   */
	JarArchiver jarArchiver;

	/**
	   * The maven archive configuration to use.
	   * 
	   * @parameter
	   */
	MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * 
     */
    public abstract void execute()
        throws MojoExecutionException;

    /**
     * 
     * @return
     */
    public MavenProject getProject()
    {

        return project;
    }

    /**
     * 
     * @return
     */
    public File getPackagingDirectory()
    {
        return packagingDirectory;
    }
    
    public abstract String getPackagingFilename();
		
	public String getOutputDirectory() {
		return outputDirectory;
	}

	/**
     * 
     * @throws MojoExecutionException
     */
    public void buildExplodedPackaging()
        throws MojoExecutionException
    {
        buildExplodedPackaging( Collections.EMPTY_SET );
    }
    
    
    /**
     * 
     * @throws MojoExecutionException
     */
    public void buildExplodedPackaging( Set excludes )
        throws MojoExecutionException
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
            
            File packagingFileTarget = new File( packagingDirectory, "META-INF" );
            packagingFileTarget = new File( packagingFileTarget, getPackagingFilename() );
            if ( ! packagingFileTarget.exists() )
            {
                if ( ! packagingFileTarget.getParentFile().exists() )
                {
                    packagingFileTarget.getParentFile().mkdirs();
                }
                
                if ( packagingFile == null || ! packagingFile.exists() )
                {
                    throw new MojoExecutionException( "Could not find the " + getPackagingFilename() + " file." );
                }
                else 
                {
                    FileUtils.copyFile( packagingFile, packagingFileTarget );
                }
            }
            
            Set artifacts = project.getArtifacts();
            List rejects = new ArrayList();
            getLog().info( "");
            getLog().info( "    Including artifacts: ");
            getLog().info( "    -------------------");
            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
                if ( !artifact.isOptional() && filter.include( artifact ) )
                {
                    String type = artifact.getType();
                    String descriptor = artifact.getGroupId() + ":" + artifact.getArtifactId();

                    if ( "jar".equals( type ) && ! excludes.contains( descriptor ) )
                    {
                        getLog().info( "        o " + descriptor );
                        FileUtils.copyFileToDirectory( artifact.getFile(), libDirectory );
                    }
                    else
                    {
                        rejects.add( artifact );
                    }
                }
            }
            
            if ( ! excludes.isEmpty() )
            {
                getLog().info( "" );
                getLog().info( "    Excluded artifacts: ");
                getLog().info( "    ------------------");
                for ( int ii = 0; ii < rejects.size(); ii++ )
                {
                    getLog().info( "        o " + rejects.get( ii ) );
                }
            }
            else
            {
                getLog().info( "No artifacts have been excluded.");
            }
            getLog().info( "" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not explode JBoss packaging...", e );
        }
    }

	public String getArchiveName() {
		return archiveName;
	}

	/**
	   * Generates the packaged archive.
	   * 
	   * @param archiveFile the target packaging archive file
	   * @throws IOException
	   * @throws ArchiverException
	   * @throws ManifestException
	   * @throws DependencyResolutionRequiredException
	   */
	protected void performPackaging(File archiveFile) throws IOException, ArchiverException, ManifestException, DependencyResolutionRequiredException, MojoExecutionException {
	
	    buildExplodedPackaging( excludes );
	
	    // generate archive file
	    getLog().info("Generating JBoss packaging " + archiveFile.getAbsolutePath());
	    MavenArchiver archiver = new MavenArchiver();
	    archiver.setArchiver(jarArchiver);
	    archiver.setOutputFile(archiveFile);
	    jarArchiver.addDirectory(getPackagingDirectory());
	
	    // create archive
	    archiver.createArchive(getProject(), archive);
	    getProject().getArtifact().setFile(archiveFile);
	  }
	
}
