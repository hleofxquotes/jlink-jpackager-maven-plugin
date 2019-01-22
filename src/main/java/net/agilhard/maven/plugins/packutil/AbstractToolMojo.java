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
package net.agilhard.maven.plugins.packutil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @author bei
 *
 */
public abstract class AbstractToolMojo extends AbstractMojo {

    @Component
    protected LocationManager locationManager;
    @Component
    protected ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    /**
     * This will turn on verbose mode.
     * <p>
     * The jlink/jpackager command line equivalent is: <code>--verbose</code>
     * </p>
     */
    @Parameter(defaultValue = "false")
    protected boolean verbose;

    /**
     * <p>
     * Specify the requirements for this jdk toolchain. This overrules the toolchain selected by the
     * maven-toolchain-plugin.
     * </p>
     * <strong>note:</strong> requires at least Maven 3.3.1
     */
    @Parameter
    protected Map<String, String> jdkToolchain;

    /**
     *
     */
    public AbstractToolMojo() {
        super();
    }

    protected String getToolExecutable(final String toolName) throws IOException {
        final Toolchain tc = this.getToolchain();

        String toolExecutable = null;
        if ( tc != null )
        {
            toolExecutable = tc.findTool( toolName );
        }

        // TODO: Check if there exist a more elegant way?
        final String toolCommand = toolName + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File toolExe;

        if ( StringUtils.isNotEmpty( toolExecutable ) )
        {
            toolExe = new File( toolExecutable );

            if ( toolExe.isDirectory() )
            {
               toolExe = new File( toolExe, toolCommand );
            }

            if ( SystemUtils.IS_OS_WINDOWS && toolExe.getName().indexOf( '.' ) < 0 )
            {
                toolExe = new File( toolExe.getPath() + ".exe" );
            }

            if ( !toolExe.isFile() )
            {
                throw new IOException( "The " + toolName + " executable '" + toolExe
                                     + "' doesn't exist or is not a file." );
            }
            return toolExe.getAbsolutePath();
        }

        // ----------------------------------------------------------------------
        // Try to find tool from System.getProperty( "java.home" )
        // By default, System.getProperty( "java.home" ) = JRE_HOME and JRE_HOME
        // should be in the JDK_HOME
        // ----------------------------------------------------------------------
        toolExe = new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "bin", toolCommand );

        // ----------------------------------------------------------------------
        // Try to find javadocExe from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if ( !toolExe.exists() || !toolExe.isFile() )
        {
            final Properties env = CommandLineUtils.getSystemEnvVars();
            final String javaHome = env.getProperty( "JAVA_HOME" );
            if ( StringUtils.isEmpty( javaHome ) )
            {
                throw new IOException( "The environment variable JAVA_HOME is not correctly set." );
            }
            if ( !new File( javaHome ).getCanonicalFile().exists()
                || new File( javaHome ).getCanonicalFile().isFile() )
            {
                throw new IOException( "The environment variable JAVA_HOME=" + javaHome
                    + " doesn't exist or is not a valid directory." );
            }

            toolExe = new File( javaHome + File.separator + "bin", toolCommand );
        }

        if ( !toolExe.getCanonicalFile().exists() || !toolExe.getCanonicalFile().isFile() )
        {
            throw new IOException( "The " + toolName + " executable '" + toolExe
                + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable." );
        }

        return toolExe.getAbsolutePath();
    }

    protected void executeCommand(final Commandline cmd) throws MojoExecutionException {
        if ( this.getLog().isDebugEnabled() )
        {
            // no quoted arguments ???
            this.getLog().debug( CommandLineUtils.toString( cmd.getCommandline() ).replaceAll( "'", "" ) );
        }

        final CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        final CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        try
        {
            final int exitCode = CommandLineUtils.executeCommandLine( cmd, out, err );

            final String output = ( StringUtils.isEmpty( out.getOutput() ) ? null : '\n' + out.getOutput().trim() );

            if ( exitCode != 0 )
            {

                if ( StringUtils.isNotEmpty( output ) )
                {
                    // Reconsider to use WARN / ERROR ?
                   //  getLog().error( output );
                    for ( final String outputLine : output.split( "\n" ) )
                    {
                        this.getLog().error( outputLine );
                    }
                }

                final StringBuilder msg = new StringBuilder( "\nExit code: " );
                msg.append( exitCode );
                if ( StringUtils.isNotEmpty( err.getOutput() ) )
                {
                    msg.append( " - " ).append( err.getOutput() );
                }
                msg.append( '\n' );
                msg.append( "Command line was: " ).append( cmd ).append( '\n' ).append( '\n' );

                throw new MojoExecutionException( msg.toString() );
            }

            if ( StringUtils.isNotEmpty( output ) )
            {
                //getLog().info( output );
                for ( final String outputLine : output.split( "\n" ) )
                {
                    this.getLog().info( outputLine );
                }
            }
        }
        catch ( final CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to execute command: " + e.getMessage(), e );
        }

    }

    protected Toolchain getToolchain() {
        Toolchain tc = null;

        if ( this.jdkToolchain != null )
        {
            // Maven 3.3.1 has plugin execution scoped Toolchain Support
            try
            {
                final Method getToolchainsMethod = this.toolchainManager.getClass().getMethod( "getToolchains", MavenSession.class,
                                                                                    String.class, Map.class );

                @SuppressWarnings( "unchecked" )
                final
                List<Toolchain> tcs =
                    (List<Toolchain>) getToolchainsMethod.invoke( this.toolchainManager, this.session, "jdk", this.jdkToolchain );

                if ( tcs != null && tcs.size() > 0 )
                {
                    tc = tcs.get( 0 );
                }
            }
            catch ( final ReflectiveOperationException e )
            {
                // ignore
            }
            catch ( final SecurityException e )
            {
                // ignore
            }
            catch ( final IllegalArgumentException e )
            {
                // ignore
            }
        }

        if ( tc == null )
        {
            // TODO: Check if we should make the type configurable?
            tc = this.toolchainManager.getToolchainFromBuildContext( "jdk", this.session );
        }

        return tc;
    }

    protected MavenProject getProject() {
        return this.project;
    }

    protected MavenSession getSession() {
        return this.session;
    }

}