/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;
import org.apache.tools.ant.BuildException;


/**
 * P4Have - lists files currently on client. P4Have simply dumps the current
 * file version info into the Ant log (or stdout).
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Have extends P4Base
{

    public void execute()
        throws BuildException
    {
        execP4Command( "have " + P4CmdOpts + " " + P4View, new SimpleP4OutputHandler( this ) );
    }
}
