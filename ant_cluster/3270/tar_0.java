/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.api;

import java.io.File;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.excalibur.property.PropertyException;
import org.apache.avalon.excalibur.property.PropertyUtil;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.DefaultContext;

/**
 * Default implementation of TaskContext.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTaskContext
    extends DefaultContext
    implements TaskContext
{
    private File     m_baseDirectory;

    /**
     * Constructor for Context with no parent contexts.
     */
    public DefaultTaskContext()
    {
        this( null );
    }
    /**
     * Constructor.
     */
    public DefaultTaskContext( final TaskContext parent )
    {
        super( parent );

        if( null != parent )
        {
            m_baseDirectory = (File)parent.getBaseDirectory();
        }
    }

    /**
     * Retrieve JavaVersion running under.
     *
     * @return the version of JVM
     */
    public JavaVersion getJavaVersion()
    {
        try
        {
            return (JavaVersion)get( JAVA_VERSION );
        }
        catch( final ContextException ce )
        {
            throw new IllegalStateException( "No JavaVersion in Context" );
        }
    }


    /**
     * Retrieve Name of tasklet.
     *
     * @return the name
     */
    public String getName()
    {
        try
        {
            return (String)get( NAME );
        }
        catch( final ContextException ce )
        {
            throw new IllegalStateException( "No Name in Context" );
        }
    }

    /**
     * Retrieve base directory.
     *
     * @return the base directory
     */
    public File getBaseDirectory()
    {
        return m_baseDirectory;
    }

    /**
     * Resolve filename.
     * This involves resolving it against baseDirectory and
     * removing ../ and ./ references. It also means formatting
     * it appropriately for the particular OS (ie different OS have
     * different volumes, file conventions etc)
     *
     * @param filename the filename to resolve
     * @return the resolved filename
     */
    public File resolveFile( final String filename )
    {
        final File result = FileUtil.resolveFile( m_baseDirectory, filename );
        if( null != result ) return result;
        else return null;
    }

    /**
     * Resolve property.
     * This evaluates all property substitutions based on current context.
     *
     * @param property the property to resolve
     * @return the resolved property
     */
    public Object resolveValue( final String property )
        throws TaskException
    {
        try { return PropertyUtil.resolveProperty( property, this, false ); }
        catch( final PropertyException pe )
        {
            throw new TaskException( "Error resolving " + property + " due to " + pe.getMessage(),
                                    pe );
        }
    }

    /**
     * Retrieve property for name.
     *
     * @param name the name of property
     * @return the value of the property
     */
    public Object getProperty( final String name )
    {
        try { return get( name ); }
        catch( final ContextException ce )
        {
            return null;
        }
    }

    /**
     * Set property value in current context.
     *
     * @param name the name of property
     * @param value the value of property
     */
    public void setProperty( final String name, final Object value )
        throws TaskException
    {
        setProperty( name, value, CURRENT );
    }

    /**
     * Set property value.
     *
     * @param property the property
     */
    public void setProperty(  final String name, final Object value, final ScopeEnum scope )
        throws TaskException
    {
        checkPropertyValid( name, value );

        if( CURRENT == scope ) put( name, value );
        else if( PARENT == scope )
        {
            if( null == m_parent )
            {
                throw new TaskException( "Can't set a property with parent scope when context " +
                                        " has no parent" );
            }
            else
            {
                ((DefaultTaskContext)m_parent).put( name, value );
            }
        }
        else if( TOP_LEVEL == scope )
        {
            DefaultTaskContext context = this;

            while( null != context.m_parent )
            {
                context = (DefaultTaskContext)context.m_parent;
            }

            context.putValue( name, value );
        }
        else
        {
            throw new TaskException( "Can't set a property with an unknown " +
                                    "property context! (" + scope + ")" );
        }
    }

    /**
     * put a value in context.
     * This put method is overidden so new baseDirectory can be saved
     * in member variable.
     *
     * @param key the key
     * @param value the value
     */
    public void putValue( final Object key, final Object value  )
        throws TaskException
    {
        if( key.equals( BASE_DIRECTORY ) )
        {
            try { m_baseDirectory = (File)value; }
            catch( final ClassCastException cce )
            {
                throw new TaskException( "Can not set baseDirectory to a non-file value.",
                                        cce );
            }
        }

        put( key, value );
    }

    /**
     * Make sure property is valid if it is one of the "magic" properties.
     *
     * @param name the name of property
     * @param value the value of proeprty
     * @exception TaskException if an error occurs
     */
    protected void checkPropertyValid( final String name, final Object value )
        throws TaskException
    {
        if( BASE_DIRECTORY.equals( name ) && !( value instanceof File ) )
        {
            throw new TaskException( "Property " + BASE_DIRECTORY +
                                     " must have a value of type " +
                                     File.class.getName() );
        }
        else if( NAME.equals( name ) && !( value instanceof String ) )
        {
            throw new TaskException( "Property " + NAME +
                                     " must have a value of type " +
                                     String.class.getName() );
        }
        else if( JAVA_VERSION.equals( name ) && !( value instanceof JavaVersion ) )
        {
            throw new TaskException( "property " + JAVA_VERSION +
                                     " must have a value of type " +
                                     JavaVersion.class.getName() );
        }
    }
}
