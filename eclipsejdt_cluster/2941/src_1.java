/*******************************************************************************
 * Copyright (c) 2005 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    sbandow@bea.com - initial API and implementation
 *    
 *******************************************************************************/

package org.eclipse.jdt.apt.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.apt.tests.annotations.ProcessorTestStatus;
import org.eclipse.jdt.apt.tests.annotations.mirrortest.MirrorUtilTestAnnotationProcessor;
import org.eclipse.jdt.apt.tests.annotations.mirrortest.MirrorUtilTestCodeExample;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.tests.builder.Problem;
import org.eclipse.jdt.core.tests.builder.Tests;
import org.eclipse.jdt.core.tests.util.Util;

public class MirrorUtilTests extends Tests {

	public MirrorUtilTests(final String name)
	{
		super(name);
	}

	public static Test suite()
	{
		return new TestSuite(MirrorUtilTests.class);
	}
	
	public void setUp() throws Exception
	{
		ProcessorTestStatus.reset();
		
		super.setUp();

		env.resetWorkspace();

		// project will be deleted by super-class's tearDown() method
		IPath projectPath = env.addProject( getProjectName(), "1.5" ); //$NON-NLS-1$
		env.addExternalJars( projectPath, Util.getJavaClassLibs() );
		fullBuild( projectPath );

		// remove old package fragment root so that names don't collide
		env.removePackageFragmentRoot( projectPath, "" ); //$NON-NLS-1$
		env.addPackageFragmentRoot( projectPath, "src" ); //$NON-NLS-1$
		env.setOutputFolder( projectPath, "bin" ); //$NON-NLS-1$

		IJavaProject jproj = env.getJavaProject(projectPath);
		TestUtil.createAndAddAnnotationJar( jproj );
		
		IProject project = env.getProject( getProjectName() );
		addEnvOptions(jproj);
		IPath srcRoot = getSourcePath();
		String code = MirrorUtilTestCodeExample.CODE;
		env.addClass(srcRoot, MirrorUtilTestCodeExample.CODE_PACKAGE, MirrorUtilTestCodeExample.CODE_CLASS_NAME, code);
		fullBuild( project.getFullPath() );
		assertNoUnexpectedProblems();
	}
	
	/**
	 * Add options which the AnnotationProcessorEnvironment should see.
	 * The options will be verified within the processor code.
	 */
	private void addEnvOptions(IJavaProject jproj) {
		for (int i = 0; i < MirrorUtilTestAnnotationProcessor.ENV_KEYS.length; ++i) {
			AptConfig.addProcessorOption(jproj, 
					MirrorUtilTestAnnotationProcessor.ENV_KEYS[i], 
					MirrorUtilTestAnnotationProcessor.ENV_VALUES[i]);
		}
	}

	/**
	 * 
	 */
	private void assertNoUnexpectedProblems() {
		Problem[] problems = env.getProblems();
		for (Problem problem : problems) {
			if (problem.getMessage().startsWith("The field DeclarationsTestClass")) { //$NON-NLS-1$
				continue;
			}
			fail("Found unexpected problem: " + problem); //$NON-NLS-1$
		}
	}
	
	public static String getProjectName()
	{
		return MirrorUtilTests.class.getName() + "Project"; //$NON-NLS-1$
	}

	public IPath getSourcePath()
	{
		IProject project = env.getProject( getProjectName() );
		IFolder srcFolder = project.getFolder( "src" ); //$NON-NLS-1$
		IPath srcRoot = srcFolder.getFullPath();
		return srcRoot;
	}
	
	public void testMirrorUtils() throws Exception
	{
		assertEquals(ProcessorTestStatus.NO_ERRORS, ProcessorTestStatus.getErrors());
	}
}
