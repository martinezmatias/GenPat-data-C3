/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.builder;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.log.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.components.model.Condition;
import org.apache.myrmidon.components.model.DefaultProject;
import org.apache.myrmidon.components.model.DefaultTarget;
import org.apache.myrmidon.components.model.Project;
import org.apache.myrmidon.components.model.Target;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Default implementation to construct project from a build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectBuilder
    extends AbstractLoggable
    implements ProjectBuilder
{
    /**
     * build a project from file.
     *
     * @param source the source
     * @return the constructed Project
     * @exception IOException if an error occurs
     * @exception Exception if an error occurs
     */
    public Project build( final File projectFile )
        throws Exception
    {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        final SAXParser saxParser = saxParserFactory.newSAXParser();
        final XMLReader parser = saxParser.getXMLReader();
        parser.setFeature( "http://xml.org/sax/features/namespace-prefixes", false );
        parser.setFeature( "http://xml.org/sax/features/namespaces", false );
        //parser.setFeature( "http://xml.org/sax/features/validation", false );

        final SAXConfigurationHandler handler = new SAXConfigurationHandler();
        parser.setContentHandler( handler );
        parser.setErrorHandler( handler );

        final String location = projectFile.toURL().toString();
        parser.parse( location );

        return build( projectFile, handler.getConfiguration() );
    }
/*
    private final void dump( final Configuration configuration )
        throws Exception
    {
        System.out.println( "Configuration: "+ configuration.getName() + "/" + configuration.getLocation() );
        final Configuration[] children = configuration.getChildren();

        for( int i = 0; i < children.length; i++ )
        {
            dump( children[ i ] );
        }
    }
*/

    /**
     * build project from configuration.
     *
     * @param file the file from which configuration was loaded
     * @param configuration the configuration loaded
     * @return the created Project
     * @exception IOException if an error occurs
     * @exception Exception if an error occurs
     * @exception ConfigurationException if an error occurs
     */
    protected final Project build( final File file, final Configuration configuration )
        throws Exception
    {
        if( !configuration.getName().equals("project") )
        {
            throw new Exception( "Project file must be enclosed in project element" );
        }

        //get project-level attributes
        final String baseDirectoryName = configuration.getAttribute( "basedir" );
        final String defaultTarget = configuration.getAttribute( "default" );
        //final String name = configuration.getAttribute( "name" );

        //determine base directory for project
        final File baseDirectory =
            (new File( file.getParentFile(), baseDirectoryName )).getAbsoluteFile();

        getLogger().debug( "Project " + file + " base directory: " + baseDirectory );

        //create project and ...
        final DefaultProject project = new DefaultProject();
        project.setDefaultTargetName( defaultTarget );
        project.setBaseDirectory( baseDirectory );
        //project.setName( name );

        //build using all top-level attributes
        buildTopLevelProject( project, configuration );

        return project;
    }

    /**
     * Handle all top level elements in configuration.
     *
     * @param project the project
     * @param configuration the Configuration
     * @exception Exception if an error occurs
     */
    protected void buildTopLevelProject( final DefaultProject project,
                                         final Configuration configuration )
        throws Exception
    {
        final Configuration[] children = configuration.getChildren();

        for( int i = 0; i < children.length; i++ )
        {
            final Configuration element = children[ i ];
            final String name = element.getName();

            //handle individual elements
            if( name.equals( "target" ) ) buildTarget( project, element );
            else if( name.equals( "property" ) ) buildImplicitTask( project, element );
            else
            {
                throw new Exception( "Unknown top-level element " + name +
                                        " at " + element.getLocation() );
            }
        }
    }

    /**
     * Build a target from configuration.
     *
     * @param project the project
     * @param task the Configuration
     */
    protected void buildTarget( final DefaultProject project, final Configuration target )
        throws Exception
    {
        final String name = target.getAttribute( "name", null );
        final String depends = target.getAttribute( "depends", null );
        final String ifCondition = target.getAttribute( "if", null );
        final String unlessCondition = target.getAttribute( "unless", null );

        if( null == name )
        {
            throw new Exception( "Discovered un-named target at " +
                                    target.getLocation() );
        }

        getLogger().debug( "Parsing target: " + name );

        if( null != ifCondition && null != unlessCondition )
        {
            throw new Exception( "Discovered invalid target that has both a if and " +
                                    "unless condition at " + target.getLocation() );
        }

        Condition condition = null;

        if( null != ifCondition )
        {
            getLogger().debug( "Target if condition: " + ifCondition );
            condition = new Condition( true, ifCondition );
        }
        else if( null != unlessCondition )
        {
            getLogger().debug( "Target unless condition: " + unlessCondition );
            condition = new Condition( false, unlessCondition );
        }

        final DefaultTarget defaultTarget = new DefaultTarget( condition );

        //apply depends attribute
        if( null != depends )
        {
            final String[] elements = ExceptionUtil.splitString( depends, "," );

            for( int i = 0; i < elements.length; i++ )
            {
                final String dependency = elements[ i ].trim();

                if( 0 == dependency.length() )
                {
                    throw new Exception( "Discovered empty dependency in target " +
                                         target.getName() + " at " + target.getLocation() );
                }

                getLogger().debug( "Target dependency: " + dependency );
                defaultTarget.addDependency( dependency );
            }
        }

        //add all the targets from element
        final Configuration[] tasks = target.getChildren();
        for( int i = 0; i < tasks.length; i++ )
        {
            getLogger().debug( "Parsed task: " + tasks[ i ].getName() );
            defaultTarget.addTask( tasks[ i ] );
        }

        //add target to project
        project.addTarget( name, defaultTarget );
    }

    /**
     * Create an implict task from configuration
     *
     * @param project the project
     * @param task the configuration
     */
    protected void buildImplicitTask( final DefaultProject project, final Configuration task )
    {
        DefaultTarget target = (DefaultTarget)project.getImplicitTarget();

        if( null == target )
        {
            target = new DefaultTarget();
            project.setImplicitTarget( target );
        }

        getLogger().debug( "Parsed implicit task: " + task.getName() );
        target.addTask( task );
    }
}
