/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.deployer.ConverterDefinition;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.converter.ConverterException;
import org.apache.myrmidon.converter.Converter;
import java.io.File;

/**
 * Test cases for the default deployer.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class DefaultDeployerTest
    extends AbstractComponentTest
{
    private static final String TEST_TYPE1_NAME = "test-type1";
    private static final String DATA_TYPE_ROLE = "data-type";

    private Deployer m_deployer;
    private RoleManager m_roleManager;
    private MasterConverter m_converter;

    public DefaultDeployerTest( final String name )
    {
        super( name );
    }

    /**
     * Setup the test case - prepares the set of components, including the
     * deployer.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        m_deployer = (Deployer)getComponentManager().lookup( Deployer.ROLE );
        m_converter = (MasterConverter)getComponentManager().lookup( MasterConverter.ROLE );

        // Add some core roles
        m_roleManager = (RoleManager)getComponentManager().lookup( RoleManager.ROLE );
        m_roleManager.addNameRoleMapping( DATA_TYPE_ROLE, DataType.ROLE );
        m_roleManager.addNameRoleMapping( "converter", Converter.ROLE );
    }

    /**
     * Tests deployment of a single type from a ClassLoader.
     */
    public void testSingleType() throws Exception
    {
        final String typeName = TEST_TYPE1_NAME;
        final String classname = TestType1.class.getName();

        // Determine the shorthand for the DataType role

        // Create the type definition
        final TypeDefinition typeDef =  new TypeDefinition( typeName, DATA_TYPE_ROLE, classname );

        final ClassLoader classLoader = getClass().getClassLoader();
        final TypeDeployer typeDeployer = m_deployer.createDeployer( classLoader );

        // Make sure the test types have not been deployed
        assertTypesNotRegistered( );

        // Deploy the type
        typeDeployer.deployType( typeDef );

        // Check the type has been registered
        final TypeFactory typeFactory = getTypeManager().getFactory( DataType.class );
        final Object result = typeFactory.create( typeName );
        assertTrue( result instanceof TestType1 );
    }

    /**
     * Tests deployment of a single converter from a ClassLoader.
     */
    public void testSingleConverter() throws Exception
    {
        // Create the type definition
        final String classname = TestConverter1.class.getName();
        final String source = "java.lang.String";
        final String destClass = TestType1.class.getName();

        final ConverterDefinition typeDef =
            new ConverterDefinition( classname, source, destClass );

        final ClassLoader classLoader = getClass().getClassLoader();
        final TypeDeployer typeDeployer = m_deployer.createDeployer( classLoader );

        // Make sure the test types have not been deployed
        assertTypesNotRegistered( );

        // Deploy the type
        typeDeployer.deployType( typeDef );

        // Try to convert from string to test type
        final Object result = m_converter.convert( TestType1.class, "some-string", null );
        assertTrue( result instanceof TestType1 );
    }

    /**
     * Tests deployment of types from a typelib descriptor.
     */
    public void testLibDescriptor() throws Exception
    {
        final File typelib = getTestResource( "test.atl" );
        assertTrue( "File " + typelib + " does not exist", typelib.exists() );

        final TypeDeployer typeDeployer = m_deployer.createDeployer( typelib );

        // Make sure the test types have not been deployed
        assertTypesNotRegistered();

        // Deploy all the types from the descriptor
        typeDeployer.deployAll();

        // Make sure the test types have been deployed
        assertTypesRegistered();
    }

    /**
     * Ensures that the test types have not ben deployed.
     */
    private void assertTypesNotRegistered() throws Exception
    {
        // Check the data-type
        TypeFactory typeFactory = getTypeManager().getFactory( DataType.class );
        try
        {
            typeFactory.create( TEST_TYPE1_NAME );
            fail();
        }
        catch( TypeException e )
        {
            // TODO - check error message
        }

        // Check the custom role implementation
        typeFactory = getTypeManager().getFactory( TestRole1.class );
        try
        {
            typeFactory.create( TEST_TYPE1_NAME );
            fail();
        }
        catch( TypeException e )
        {
            // TODO - check error message
        }

        // Check the converter
        try
        {
            m_converter.convert( TestType1.class, "some string", null );
            fail();
        }
        catch( ConverterException e )
        {
            // TODO - check error message
        }
    }

    /**
     * Ensures the types from the test typelib descriptor have been correctly
     * deployed.
     */
    private void assertTypesRegistered( ) throws Exception
    {
        // Check the data-type
        TypeFactory typeFactory = getTypeManager().getFactory( DataType.class );
        Object object = typeFactory.create( TEST_TYPE1_NAME );
        assertTrue( object instanceof TestType1 );

        // Check the custom role implementation
        typeFactory = getTypeManager().getFactory( TestRole1.class );
        object = typeFactory.create( TEST_TYPE1_NAME );
        assertTrue( object instanceof TestType1 );

        // Check the converter
        object = m_converter.convert( TestType1.class, "some string", null );
        assertTrue( object instanceof TestType1 );
    }
}
