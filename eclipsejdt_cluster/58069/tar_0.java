/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler;

import java.io.PrintWriter;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

public abstract class AbstractAnnotationProcessorManager {
	public abstract void configure(Main batchCompiler, String[] options);
	
	public abstract void configureFromPlatform(Compiler compiler, Object compilationUnitLocator, Object javaProject);
	
	public abstract void setOut(PrintWriter out);
	
	public abstract void setErr(PrintWriter err);

	public abstract ICompilationUnit[] getNewUnits();
	
	public abstract ICompilationUnit[] getDeletedUnits();
	
	public abstract void reset();
	
	public abstract void processAnnotations(CompilationUnitDeclaration[] units, boolean isLastRound);
	
	public abstract void setProcessors(Object[] processors);

}
