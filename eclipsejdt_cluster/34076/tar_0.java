/*******************************************************************************
 * Copyright (c) 2005 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    mkaufman@bea.com - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.apt.core.internal.env;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.apt.core.AptPlugin;
import org.eclipse.jdt.apt.core.env.Phase;
import org.eclipse.jdt.apt.core.internal.generatedfile.FileGenerationResult;
import org.eclipse.jdt.apt.core.internal.generatedfile.GeneratedFileManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

public class JavaSourceFilePrintWriter extends PrintWriter {

    public JavaSourceFilePrintWriter( String typeName, StringWriter sw, ProcessorEnvImpl env, String charsetName )
    {
        super( sw );
        _sw = sw;
        _typeName = typeName;
        _env = env;
        _charsetName = charsetName;
    }
	
    public void close()
    {
        try
        {
            String contents = _sw.toString();
            super.close();
            GeneratedFileManager gfm = GeneratedFileManager.getGeneratedFileManager(_env.getProject());
            Phase phase = _env.getPhase();
		
            if ( phase == Phase.RECONCILE )
            {
            	ICompilationUnit parentCompilationUnit = _env.getCompilationUnit();
                FileGenerationResult result  = gfm.generateFileDuringReconcile( 
                    parentCompilationUnit, _typeName, contents, parentCompilationUnit.getOwner(), null, null );
				if ( result != null )
					_env.addGeneratedFile(result.getFile(), result.isModified());
            }
            else if ( phase == Phase.BUILD)	
            {
				FileGenerationResult result = gfm.generateFileDuringBuild( _env.getFile(), _env.getJavaProject(), _typeName, contents, null /* progress monitor */, _charsetName );
				_env.addGeneratedFile( result.getFile(), result.isModified());
				
				// don't set to false, we don't want to overwrite a previous iteration setting it to true
				if ( result.getSourcePathChanged() )
					_env.setSourcePathChanged( true );
            }
            else
            {
                assert false : "Unexpected phase value: " + phase ; //$NON-NLS-1$
            }
        }
        catch ( JavaModelException jme )
        {
            // TODO:  handle this exception in a nicer way.
            AptPlugin.log(jme, "Unexpected failure closing the JavaSourceFilePrintWriter"); //$NON-NLS-1$
            throw new RuntimeException( jme );
        }
        catch ( CoreException ce )
        {
            // TODO:  handle this exception
            AptPlugin.log(ce, "Unexpected failure closing the JavaSourceFilePrintWriter"); //$NON-NLS-1$
            throw new RuntimeException( ce );
        }
        catch( UnsupportedEncodingException use )
        {
        	AptPlugin.log(use, "Could not encode"); //$NON-NLS-1$
        	// TODO: handle this exception
        	throw new RuntimeException( use );
        }
    }
			
	
    private StringWriter _sw;
    private String _typeName;
    private ProcessorEnvImpl _env;
    private String _charsetName;
	
}
