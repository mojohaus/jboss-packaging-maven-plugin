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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Build a deployable JBoss Service Archive.
 * 
 * @goal sar
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class SarMojo
    extends AbstractPackagingMojo
{

    /**
     * The artifact type.
     */
    private static final String ARTIFACT_TYPE = "jboss-sar";

    /**
     * Get the deployment descriptor filename.
     * 
     * @return The filename of the deployment descriptor (jboss-service.xml).
     */
    public String getDeploymentDescriptorFilename()
    {
        return "jboss-service.xml";
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
    /**
     * Executes the SarMojo on the current project.
     * 
     * @throws MojoExecutionException if an error occured while building the webapp
     */
    public void execute()
        throws MojoExecutionException
    {

        if ( isExploded() )
        {
            buildExplodedPackaging();
        }
        else 
        {
            performPackaging();            
        }
    }

}
