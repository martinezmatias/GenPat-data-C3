/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.jsp.compilers;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


/**
 * Creates the necessary compiler adapter, given basic criteria.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public class CompilerAdapterFactory
{

    /**
     * This is a singlton -- can't create instances!!
     */
    private CompilerAdapterFactory() { }

    /**
     * Based on the parameter passed in, this method creates the necessary
     * factory desired. The current mapping for compiler names are as follows:
     *
     * <ul>
     *   <li> jasper = jasper compiler (the default)
     *   <li> <i>a fully quallified classname</i> = the name of a jsp compiler
     *   adapter
     * </ul>
     *
     *
     * @param compilerType either the name of the desired compiler, or the full
     *      classname of the compiler's adapter.
     * @param task a task to log through.
     * @return The Compiler value
     * @throws BuildException if the compiler type could not be resolved into a
     *      compiler adapter.
     */
    public static CompilerAdapter getCompiler( String compilerType, Task task )
        throws BuildException
    {
        /*
         * If I've done things right, this should be the extent of the
         * conditional statements required.
         */
        if( compilerType.equalsIgnoreCase( "jasper" ) )
        {
            return new JasperC();
        }
        return resolveClassName( compilerType );
    }

    /**
     * Tries to resolve the given classname into a compiler adapter. Throws a
     * fit if it can't.
     *
     * @param className The fully qualified classname to be created.
     * @return Description of the Returned Value
     * @throws BuildException This is the fit that is thrown if className isn't
     *      an instance of CompilerAdapter.
     */
    private static CompilerAdapter resolveClassName( String className )
        throws BuildException
    {
        try
        {
            Class c = Class.forName( className );
            Object o = c.newInstance();
            return ( CompilerAdapter )o;
        }
        catch( ClassNotFoundException cnfe )
        {
            throw new BuildException( className + " can\'t be found.", cnfe );
        }
        catch( ClassCastException cce )
        {
            throw new BuildException( className + " isn\'t the classname of "
                 + "a compiler adapter.", cce );
        }
        catch( Throwable t )
        {
            // for all other possibilities
            throw new BuildException( className + " caused an interesting "
                 + "exception.", t );
        }
    }

}
