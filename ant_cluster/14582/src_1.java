/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs;

import java.io.File;
import java.util.ArrayList;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.types.Argument;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.CommandlineJava;
import org.apache.tools.todo.types.EnvironmentVariable;
import org.apache.tools.todo.types.Path;

/**
 * This task acts as a loader for java applications but allows to use the same
 * JVM for the called application thus resulting in much faster operation.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Java
    extends AbstractTask
{
    private CommandlineJava m_cmdl = new CommandlineJava();
    private boolean m_fork;
    private File m_dir;

    /**
     * Set the class name.
     */
    public void setClassname( String s )
    {
        m_cmdl.setClassname( s );
    }

    /**
     * Add a classpath element.
     */
    public void addClasspath( final Path classpath )
        throws TaskException
    {
        m_cmdl.createClasspath().addPath( classpath );
    }

    /**
     * The working directory of the process
     *
     * @param dir The new Dir value
     */
    public void setDir( final File dir )
    {
        m_dir = dir;
    }

    /**
     * Set the forking flag.
     */
    public void setFork( final boolean fork )
    {
        m_fork = fork;
    }

    /**
     * set the jar name...
     */
    public void setJar( final File jar )
    {
        m_cmdl.setJar( jar.getAbsolutePath() );
    }

    /**
     * Set the command used to start the VM (only if fork==false).
     */
    public void setJvm( final String jvm )
    {
        m_cmdl.setVm( jvm );
    }

    /**
     * -mx or -Xmx depending on VM version
     */
    public void setMaxmemory( final String max )
    {
        m_cmdl.setMaxmemory( max );
    }

    /**
     * Add a nested sysproperty element.
     */
    public void addSysproperty( final EnvironmentVariable sysp )
    {
        m_cmdl.addSysproperty( sysp );
    }

    /**
     * Creates a nested arg element.
     */
    public void addArg( final Argument argument )
    {
        m_cmdl.addArgument( argument );
    }

    /**
     * Creates a nested jvmarg element.
     */
    public void addJvmarg( final Argument argument )
    {
        m_cmdl.addVmArgument( argument );
    }

    public void execute()
        throws TaskException
    {
        executeJava();
    }

    /**
     * Do the execution.
     *
     * @exception org.apache.myrmidon.api.TaskException Description of Exception
     */
    public void executeJava()
        throws TaskException
    {
        final String classname = m_cmdl.getClassname();
        final String jar = m_cmdl.getJar();
        if( classname != null && jar != null )
        {
            throw new TaskException( "Only one of Classname and Jar can be set." );
        }
        else if( classname == null && jar == null )
        {
            throw new TaskException( "Classname must not be null." );
        }

        if( !m_fork && jar != null )
        {
            throw new TaskException( "Cannot execute a jar in non-forked mode. Please set fork='true'. " );
        }

        if( m_fork )
        {
            getContext().debug( "Forking " + m_cmdl.toString() );
            run( new Commandline( m_cmdl.getCommandline() ) );
        }
        else
        {
            if( m_cmdl.getVmCommand().size() > 1 )
            {
                getContext().warn( "JVM args ignored when same JVM is used." );
            }
            if( m_dir != null )
            {
                getContext().warn( "Working directory ignored when same JVM is used." );
            }

            getContext().debug( "Running in same VM " + m_cmdl.getJavaCommand().toString() );
            run( m_cmdl );
        }
    }

    /**
     * Executes the given classname with the given arguments as it was a command
     * line application.
     */
    protected void run( final String classname, final ArrayList args )
        throws TaskException
    {
        final CommandlineJava java = new CommandlineJava();
        java.setClassname( classname );

        final int size = args.size();
        for( int i = 0; i < size; i++ )
        {
            final String arg = (String)args.get( i );
            java.addArgument( arg );
        }
        run( java );
    }

    /**
     * Executes the given classname with the given arguments as it was a command
     * line application.
     */
    private void run( final CommandlineJava command )
        throws TaskException
    {
        final ExecuteJava exe = new ExecuteJava();
        exe.setJavaCommand( command.getJavaCommand() );
        exe.setClasspath( command.getClasspath() );
        exe.setSystemProperties( command.getSystemProperties() );
        exe.execute();
    }

    /**
     * Executes the given classname with the given arguments in a separate VM.
     */
    private void run( final Commandline command )
        throws TaskException
    {
        final Execute exe = new Execute();
        exe.setWorkingDirectory( m_dir );
        exe.setCommandline( command );
        exe.execute( getContext() );
    }
}
