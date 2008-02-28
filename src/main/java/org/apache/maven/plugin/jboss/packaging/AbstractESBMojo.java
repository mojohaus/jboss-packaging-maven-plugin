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
package org.apache.maven.plugin.jboss.packaging;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Base class for building a JBoss ESB archive.
 *
 * @author <a href="mailto:kevin.conner@jboss.com">Kevin Conner</a>
 */
public abstract class AbstractESBMojo extends AbstractPackagingMojo
{
    /**
     * The name of the meta-inf directory.
     */
    private static final String META_INF = "META-INF";

    /**
     /**
     * The location of the jboss-esb.xml file.
     */
    private static final String JBOSS_ESB_XML = "jboss-esb.xml";

    /**
     * The location of the deployment.xml file.
     */
    private static final String DEPLOYMENT_XML = "deployment.xml";

    /**
     * The artifact type.
     */
    private static final String ARTIFACT_TYPE = "jboss-esb";

    /**
     * Override the deployment xml file
     * @parameter expression="${maven.esb.deployment.xml}"
     */
    private File deploymentXml;

    /**
     * Perform any packaging specific to this type.
     * 
     * @param excludes
     *            The exclude list.
     * 
     * @throws MojoExecutionException
     *             For plugin failures.
     * @throws MojoFailureException
     *             For unexpected plugin failures.
     * @throws IOException
     *             For exceptions during IO operations.
     */
    protected void buildSpecificPackaging( final Set excludes )
        throws MojoExecutionException, MojoFailureException, IOException
    {
        final File metainfDir = new File( getOutputDirectory(), META_INF );
        if ( deploymentXml != null )
        {
            FileUtils.copyFile( deploymentXml, new File( metainfDir, DEPLOYMENT_XML ) );
        }
    }

    /**
     * Get the name of the deployment descriptor file.
     *
     * Sublcasses must override this method and provide the proper name for
     * their type of archive packaging
     *
     * @return deployment descriptor file name, sans path
     */
    public String getDeploymentDescriptorFilename()
    {
        return JBOSS_ESB_XML;
    }

    /**
     * Get the type of the artifact.
     *
     * @return The type of the generated artifact.
     */
    public String getArtifactType()
    {
        return ARTIFACT_TYPE;
    }
}
