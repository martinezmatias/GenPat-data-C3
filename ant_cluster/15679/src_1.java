/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.aut.converter.Converter;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.AbstractMyrmidonTest;
import org.apache.myrmidon.components.configurer.DefaultConfigurer;
import org.apache.myrmidon.components.converter.DefaultConverterRegistry;
import org.apache.myrmidon.components.converter.DefaultMasterConverter;
import org.apache.myrmidon.components.deployer.ClassLoaderManager;
import org.apache.myrmidon.components.deployer.DefaultClassLoaderManager;
import org.apache.myrmidon.components.deployer.DefaultDeployer;
import org.apache.myrmidon.components.extensions.DefaultExtensionManager;
import org.apache.myrmidon.components.role.DefaultRoleManager;
import org.apache.myrmidon.components.service.DefaultAntServiceManager;
import org.apache.myrmidon.components.type.DefaultTypeManager;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.service.AntServiceManager;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * A base class for tests for the default components.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public abstract class AbstractComponentTest
    extends AbstractMyrmidonTest
{
    private DefaultServiceManager m_serviceManager;
    private Logger m_logger;

    public AbstractComponentTest( final String name )
    {
        super( name );
    }

    /**
     * Returns the component manager containing the components to test.
     */
    protected ServiceManager getServiceManager()
    {
        return m_serviceManager;
    }

    /**
     * Returns the type manager.
     */
    protected TypeManager getTypeManager()
        throws ServiceException
    {
        return (TypeManager)getServiceManager().lookup( TypeManager.ROLE );
    }

    /**
     * Setup the test case - prepares the set of components.
     */
    protected void setUp()
        throws Exception
    {
        m_logger = createLogger();

        // Create the components
        m_serviceManager = new DefaultServiceManager();
        List components = new ArrayList();

        Object component = new DefaultMasterConverter();
        m_serviceManager.put( MasterConverter.ROLE, component );
        components.add( component );

        component = new DefaultConverterRegistry();
        m_serviceManager.put( ConverterRegistry.ROLE, component );
        components.add( component );

        component = new DefaultTypeManager();
        m_serviceManager.put( TypeManager.ROLE, component );
        components.add( component );

        component = new DefaultConfigurer();
        m_serviceManager.put( Configurer.ROLE, component );
        components.add( component );

        component = new DefaultDeployer();
        m_serviceManager.put( Deployer.ROLE, component );
        components.add( component );

        final DefaultClassLoaderManager classLoaderMgr = new DefaultClassLoaderManager();
        classLoaderMgr.setBaseClassLoader( getClass().getClassLoader() );
        m_serviceManager.put( ClassLoaderManager.ROLE, classLoaderMgr );
        components.add( classLoaderMgr );

        component = new DefaultExtensionManager();
        m_serviceManager.put( ExtensionManager.ROLE, component );
        components.add( component );

        component = new DefaultRoleManager();
        m_serviceManager.put( RoleManager.ROLE, component );
        components.add( component );

        component = new DefaultAntServiceManager();
        m_serviceManager.put( AntServiceManager.ROLE, component );
        components.add( component );

        // Log enable the components
        for( Iterator iterator = components.iterator(); iterator.hasNext(); )
        {
            Object obj = iterator.next();
            if( obj instanceof LogEnabled )
            {
                final LogEnabled logEnabled = (LogEnabled)obj;
                logEnabled.enableLogging( m_logger );
            }
        }

        // Compose the components
        for( Iterator iterator = components.iterator(); iterator.hasNext(); )
        {
            Object obj = iterator.next();
            if( obj instanceof Serviceable )
            {
                final Serviceable serviceable = (Serviceable)obj;
                serviceable.service( m_serviceManager );
            }
        }
    }


    /**
     * Utility method to register a Converter.
     */
    protected void registerConverter( final Class converterClass,
                                      final Class sourceClass,
                                      final Class destClass )
        throws ServiceException, TypeException
    {
        ConverterRegistry converterRegistry = (ConverterRegistry)getServiceManager().lookup( ConverterRegistry.ROLE );
        converterRegistry.registerConverter( converterClass.getName(), sourceClass.getName(), destClass.getName() );
        DefaultTypeFactory factory = new DefaultTypeFactory( getClass().getClassLoader() );
        factory.addNameClassMapping( converterClass.getName(), converterClass.getName() );
        getTypeManager().registerType( Converter.class, converterClass.getName(), factory );
    }
}
