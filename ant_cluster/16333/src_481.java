/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.util.ArrayList;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.AbstractFileSet;

/**
 * A file set, that contains those files under a directory that match
 * a set of patterns.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @ant:data-type name="v-fileset"
 */
public class PatternFileSet
    extends AbstractFileSet
    implements FileList, FileSet
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( PatternFileSet.class );

    private FileObject m_dir;

    /**
     * Sets the root directory.
     */
    public void setDir( final FileObject dir )
    {
        m_dir = dir;
    }

    /**
     * Returns the root directory
     */
    public FileObject getDir()
    {
        return m_dir;
    }

    /**
     * Returns the list of files, in depthwise order.
     */
    public FileObject[] listFiles( TaskContext context ) throws TaskException
    {
        final FileSetResult result = getResult( context );
        return result.getFiles();
    }

    /**
     * Returns the contents of the set.
     */
    public FileSetResult getResult( TaskContext context ) throws TaskException
    {
        if( m_dir == null )
        {
            final String message = REZ.getString( "fileset.dir-not-set.error" );
            throw new TaskException( message );
        }

        try
        {
            final DefaultFileSetResult result = new DefaultFileSetResult();
            final ArrayList stack = new ArrayList();
            final ArrayList pathStack = new ArrayList();
            stack.add( m_dir );
            pathStack.add( "." );

            while( stack.size() > 0 )
            {
                // Pop next folder off the stack
                FileObject folder = (FileObject)stack.remove( 0 );
                String path = (String)pathStack.remove( 0 );

                // Queue the children of the folder
                FileObject[] children = folder.getChildren();
                for( int i = 0; i < children.length; i++ )
                {
                    FileObject child = children[ i ];
                    String childPath = path + '/' + child.getName().getBaseName();
                    if( child.getType() == FileType.FILE )
                    {
                        // A regular file - add it straight to the result
                        result.addElement( child, childPath );
                    }
                    else
                    {
                        // A folder - push it on to the stack
                        stack.add( 0, child );
                        pathStack.add( 0, childPath );
                    }
                }
            }

            return result;
        }
        catch( FileSystemException e )
        {
            final String message = REZ.getString( "fileset.list-files.error", m_dir );
            throw new TaskException( message, e );
        }
    }
}
