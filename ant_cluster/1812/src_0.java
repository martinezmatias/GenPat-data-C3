/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util.regexp;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Regular expression factory, which will create Regexp objects. The actual
 * implementation class depends on the System or Ant Property: <code>ant.regexp.regexpimpl</code>
 * .
 *
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">
 *      mattinger@mindless.com</a>
 * @version $Revision$
 */
public class RegexpFactory extends RegexpMatcherFactory
{
    public RegexpFactory() { }

    /**
     * Create a new regular expression matcher instance.
     *
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     */
    public Regexp newRegexp()
        throws BuildException
    {
        return ( Regexp )newRegexp( null );
    }

    /**
     * Create a new regular expression matcher instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     */
    public Regexp newRegexp( Project p )
        throws BuildException
    {
        String systemDefault = null;
        if( p == null )
        {
            systemDefault = System.getProperty( "ant.regexp.regexpimpl" );
        }
        else
        {
            systemDefault = ( String )p.getProperties().get( "ant.regexp.regexpimpl" );
        }

        if( systemDefault != null )
        {
            return createRegexpInstance( systemDefault );
            // XXX     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        try
        {
            return createRegexpInstance( "org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp" );
        }
        catch( BuildException be )
        {}

        try
        {
            return createRegexpInstance( "org.apache.tools.ant.util.regexp.JakartaOroRegexp" );
        }
        catch( BuildException be )
        {}

        try
        {
            return createRegexpInstance( "org.apache.tools.ant.util.regexp.JakartaRegexpRegexp" );
        }
        catch( BuildException be )
        {}

        throw new BuildException( "No supported regular expression matcher found" );
    }

    /**
     * Wrapper over {@seee RegexpMatcherFactory#createInstance createInstance}
     * that ensures that we are dealing with a Regexp implementation.
     *
     * @param classname Description of Parameter
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     * @since 1.3
     */
    protected Regexp createRegexpInstance( String classname )
        throws BuildException
    {

        RegexpMatcher m = createInstance( classname );
        if( m instanceof Regexp )
        {
            return ( Regexp )m;
        }
        else
        {
            throw new BuildException( classname + " doesn't implement the Regexp interface" );
        }
    }

}
