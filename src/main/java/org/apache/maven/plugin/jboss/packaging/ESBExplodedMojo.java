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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Builds a JBoss ESB exploded Archive.
 *
 * @author <a href="mailto:kevin.conner@jboss.com">Kevin Conner</a>
 *
 * @goal esb-exploded
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class ESBExplodedMojo extends AbstractESBMojo
{
    /**
     * Execute the mojo in the current project.
     * @throws MojoExecutionException For plugin failures.
     * @throws MojoFailureException For unexpected plugin failures.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        buildExplodedPackaging();
    }
}
