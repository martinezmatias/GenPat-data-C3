/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.rmic;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the rmic for Kaffe
 *
 * @author <a href="mailto:tokamoto@rd.nttdata.co.jp">Takashi Okamoto</a>
 */
public class KaffeRmic extends DefaultRmicAdapter
{

    public boolean execute()
        throws BuildException
    {
        getRmic().log( "Using Kaffe rmic", Project.MSG_VERBOSE );
        Commandline cmd = setupRmicCommand();

        try
        {

            Class c = Class.forName( "kaffe.rmi.rmic.RMIC" );
            Constructor cons = c.getConstructor( new Class[]{String[].class} );
            Object rmic = cons.newInstance( new Object[]{cmd.getArguments()} );
            Method doRmic = c.getMethod( "run", null );
            String str[] = cmd.getArguments();
            Boolean ok = ( Boolean )doRmic.invoke( rmic, null );

            return ok.booleanValue();
        }
        catch( ClassNotFoundException ex )
        {
            throw new BuildException( "Cannot use Kaffe rmic, as it is not available" +
                " A common solution is to set the environment variable" +
                " JAVA_HOME or CLASSPATH." );
        }
        catch( Exception ex )
        {
            if( ex instanceof BuildException )
            {
                throw ( BuildException )ex;
            }
            else
            {
                throw new BuildException( "Error starting Kaffe rmic: ", ex );
            }
        }
    }
}
