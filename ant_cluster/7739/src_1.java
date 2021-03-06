/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import java.util.Iterator;
import org.apache.ant.AntException;
import org.apache.ant.configuration.Configurer;
import org.apache.ant.convert.Converter;
import org.apache.ant.tasklet.DataType;
import org.apache.ant.tasklet.engine.DataTypeEngine;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.context.Resolvable;

/**
 * This is the property "task" to declare a binding of a datatype to a name.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Property 
    extends AbstractTask
    implements Configurable, Composable
{
    protected String              m_name;
    protected Object              m_value;
    protected boolean             m_localScope     = true;
    protected DataTypeEngine      m_engine;
    protected Converter           m_converter;
    protected Configurer          m_configurer;
    
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_configurer = (Configurer)componentManager.
            lookup( "org.apache.ant.configuration.Configurer" );

        m_engine = (DataTypeEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.DataTypeEngine" );

        m_converter = (Converter)componentManager.lookup( "org.apache.ant.convert.Converter" );
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        final String[] attributes = configuration.getAttributeNames();

        for( int i = 0; i < attributes.length; i++ )
        {
            final String name = attributes[ i ];
            final String value = configuration.getAttribute( name );

            final Object object = getContext().resolveValue( value );

            if( null == object )
            {
                throw new AntException( "Value for attribute " + name + "resolved to null" );
            }

            if( name.equals( "name" ) )
            {
                try 
                {
                    final String convertedValue = 
                        (String)m_converter.convert( String.class, object, getContext() );
                    setName( convertedValue );
                }
                catch( final Exception e )
                {
                    throw new ConfigurationException( "Error converting value", e );
                }
            }
            else if( name.equals( "value" ) )
            {
                setValue( object );
            }
            else if( name.equals( "local-scope" ) ) 
            {
                try 
                {
                    final Boolean localScope = 
                        (Boolean)m_converter.convert( Boolean.class, object, getContext() );
                    setLocalScope( Boolean.TRUE == localScope ); 
                }
                catch( final Exception e )
                {
                    throw new ConfigurationException( "Error converting value", e );
                }
            }
            else
            {
                throw new ConfigurationException( "Unknown attribute " + name );
            }
        }

        final Configuration[] children = configuration.getChildren();

        for( int i = 0; i < children.length; i++ )
        {
            final Configuration child = children[ i ];

            try
            {
                final DataType value = m_engine.createDataType( child.getName() );
                setValue( value );
                m_configurer.configure( value, child, getContext() );
            }
            catch( final Exception e )
            {
                throw new ConfigurationException( "Unable to set datatype", e );
            }
        }
    }

    public void setName( final String name )
    {
        m_name = name;
    }
    
    public void setValue( final Object value )
        throws AntException
    {
        if( null != m_value )
        {
            throw new AntException( "Value can not be set multiple times" );
        }

        m_value = value;
    }
    
    public void setLocalScope( final boolean localScope )
    {
        m_localScope = localScope;
    }

    public void execute()
        throws AntException
    {
        if( null == m_name )
        {
            throw new AntException( "Name must be specified" );
        }

        if( null == m_value )
        {
            throw new AntException( "Value must be specified" );
        }

        final TaskContext context = getContext();

        Object value = m_value;

        if( value instanceof String )
        {
            value = context.resolveValue( (String)value );
        }

        while( null != value && value instanceof Resolvable )
        {
            value = ((Resolvable)value).resolve( context );
        }

        if( m_localScope )
        {
            context.setProperty( m_name, value );
        }
        else
        {
            context.setProperty( m_name, value, TaskContext.PARENT );
        }
    }
}
