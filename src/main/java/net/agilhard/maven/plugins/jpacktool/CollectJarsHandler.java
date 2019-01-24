/**
 * Copyright Fr.Meyer's Sohn Logistics 2019. All Rights Reserved

 * $Date:  $
 * $Author:  $
 * $Revision:  $
 * $Source:  $
 * $State: Exp $ - $Locker:  $
 * **********************
 * auto generated header
 *
 * Project : jlink-jpackager-maven-plugin
 * Created by bei, 20.01.2019
 */
package net.agilhard.maven.plugins.jpacktool;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;


/**
 * @author bei
 *
 */

public class CollectJarsHandler extends AbstractDependencyJarsHandler {

    protected File outputDirectoryAutomaticJars;
    
    protected File outputDirectoryClasspathJars;
    
    protected File outputDirectoryModules;

    protected boolean onlyNamedAreAutomatic = true;
    
    public CollectJarsHandler(AbstractToolMojo mojo, DependencyGraphBuilder dependencyGraphBuilder, File outputDirectoryAutomaticJars, File outputDirectoryClasspathJars, File outputDirectoryModules, boolean onlyNamedAreAutomatic ) {
		super(mojo, dependencyGraphBuilder);
		this.outputDirectoryAutomaticJars = outputDirectoryAutomaticJars;
		this.outputDirectoryClasspathJars = outputDirectoryClasspathJars;
		this.outputDirectoryModules = outputDirectoryModules;
		this.onlyNamedAreAutomatic = onlyNamedAreAutomatic;
	}

    /** {@inheritDoc} */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
    	if ( ! outputDirectoryAutomaticJars.exists() ) {
    		if ( ! outputDirectoryAutomaticJars.mkdirs() ) {
    			throw new MojoExecutionException("directory can not be created:"+outputDirectoryAutomaticJars);
    		}
    	}
    	if ( ! outputDirectoryClasspathJars.exists() ) {
    		if ( ! outputDirectoryClasspathJars.mkdirs() ) {
    			throw new MojoExecutionException("directory can not be created:"+outputDirectoryClasspathJars);
    		}
    	}
    	super.execute();
    }
    
	@Override
    protected void handleNonModJar(final DependencyNode dependencyNode, final Artifact artifact, Map.Entry<File, JavaModuleDescriptor> entry) 
    		throws MojoExecutionException, MojoFailureException {
		
		boolean isAutomatic = (entry == null || entry.getValue() == null) ? false : entry.getValue().isAutomatic();
		
		if ( onlyNamedAreAutomatic && isAutomatic ) {
			try ( JarFile jarFile = new JarFile(artifact.getFile() ) ) {
				Manifest manifest = jarFile.getManifest();
				if ( manifest == null ) {
					isAutomatic = false;
				} else {
					Attributes mainAttributes = manifest.getMainAttributes();
					isAutomatic = mainAttributes.getValue("Automatic-Module-Name") != null;
				}
			} catch (IOException e) {

				getLog().error("error reading manifest");
				throw new MojoExecutionException("error reading manifest");
			}
			
		}

		Path path = artifact.getFile().toPath();
		
        if ( Files.isRegularFile( path ) )
        {
            try
            {
                Path target = null;
                if ( isAutomatic ) {
                	if ( outputDirectoryAutomaticJars != null ) {
                		target = outputDirectoryAutomaticJars.toPath().resolve(path.getFileName());
                	}
                } else {
                	if ( outputDirectoryClasspathJars != null ) {
                		target = outputDirectoryClasspathJars.toPath().resolve(path.getFileName());
                	}
                }
                if ( target != null ) {
                	this.getLog().info( "copy jar " + path + " to " + target.toString());
                	Files.copy( path, target, REPLACE_EXISTING );
                }
            }
            catch ( final IOException e )
            {
                this.getLog().error( "IOException", e );
                throw new MojoExecutionException(
                     "Failure during copying of " + path + " occured." );
            }
        }
	}

    protected void handleModJar(final DependencyNode dependencyNode, final Artifact artifact, Map.Entry<File, JavaModuleDescriptor> entry) throws MojoExecutionException, MojoFailureException
    {
		Path path = artifact.getFile().toPath();
		
        if ( Files.isRegularFile( path ) )
        {
            try
            {
                Path target = null;
                	if ( outputDirectoryModules != null ) {
                		target = outputDirectoryModules.toPath().resolve(path.getFileName());
                	}
                if ( target != null ) {
                	this.getLog().info( "copy jar " + path + " to " + target.toString());
                	Files.copy( path, target, REPLACE_EXISTING );
                }
            }
            catch ( final IOException e )
            {
                this.getLog().error( "IOException", e );
                throw new MojoExecutionException(
                     "Failure during copying of " + path + " occured." );
            }
        }

    }

}