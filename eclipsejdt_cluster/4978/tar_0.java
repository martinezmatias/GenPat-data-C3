/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.builder;

import junit.framework.*;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.tests.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BuildpathTests extends Tests {

	public BuildpathTests(String name) {
		super(name);
	}

	public static Test suite() {
		if (false) {
			TestSuite suite = new TestSuite(BuildpathTests.class.getName());
			suite.addTest(new BuildpathTests("testExternalJarChange")); //$NON-NLS-1$
			return suite;
		}
		return new TestSuite(BuildpathTests.class);
	}
	public void testClasspathFileChange() throws JavaModelException {
		// create project with src folder, and alternate unused src2 folder
		IPath projectPath = env.addProject("Project"); //$NON-NLS-1$
		env.removePackageFragmentRoot(projectPath, ""); //$NON-NLS-1$
		IPath root = env.addPackageFragmentRoot(projectPath, "src"); //$NON-NLS-1$
		env.addExternalJars(projectPath, Util.getJavaClassLibs());
		env.setOutputFolder(projectPath, "bin"); //$NON-NLS-1$
		IPath classTest1 = env.addClass(root, "p1", "Test1", //$NON-NLS-1$ //$NON-NLS-2$
			"package p1;\n"+ //$NON-NLS-1$
			"public class Test1 extends Zork1 {}" //$NON-NLS-1$
		);
		// not yet on the classpath
		IPath src2Path = env.addFolder(projectPath, "src2"); //$NON-NLS-1$
		IPath src2p1Path = env.addFolder(src2Path, "p1"); //$NON-NLS-1$
		env.addFile(src2p1Path, "Zork1.java", //$NON-NLS-1$ //$NON-NLS-2$
			"package p1;\n"+ //$NON-NLS-1$
			"public class Zork1 {}" //$NON-NLS-1$
		);

		fullBuild();
		expectingSpecificProblemFor(classTest1, new Problem("src", "Zork1 cannot be resolved to a type", classTest1,39, 44, CategorizedProblem.CAT_TYPE)); //$NON-NLS-1$ //$NON-NLS-2$

		//----------------------------
		//           Step 2
		//----------------------------	
		StringBuffer buffer = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
		buffer.append("<classpath>\n"); //$NON-NLS-1$
		buffer.append("    <classpathentry kind=\"src\" path=\"src\"/>\n"); //$NON-NLS-1$
		buffer.append("    <classpathentry kind=\"src\" path=\"src2\"/>\n"); // add src2 on classpath through resource change //$NON-NLS-1$
		String[] classlibs = Util.getJavaClassLibs();
		for (int i = 0; i < classlibs.length; i++) {
			buffer.append("    <classpathentry kind=\"lib\" path=\"").append(classlibs[i]).append("\"/>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		buffer.append("    <classpathentry kind=\"output\" path=\"bin\"/>\n"); //$NON-NLS-1$
		buffer.append("</classpath>"); //$NON-NLS-1$
		boolean wasAutoBuilding = env.isAutoBuilding();
		try {
			// turn autobuild on
			env.setAutoBuilding(true);
			// write new .classpath, will trigger autobuild
			env.addFile(projectPath, ".classpath", buffer.toString()); //$NON-NLS-1$
			// ensures the builder did see the classpath change
			env.waitForAutoBuild();
			expectingNoProblems();
		} finally {
			env.setAutoBuilding(wasAutoBuilding);
		}
	}	

	public void testClosedProject() throws JavaModelException {
		IPath project1Path = env.addProject("CP1"); //$NON-NLS-1$
		env.addExternalJars(project1Path, Util.getJavaClassLibs());
		IPath jarPath = env.addInternalJar(project1Path, "temp.jar", new byte[] {0}); //$NON-NLS-1$

		IPath project2Path = env.addProject("CP2"); //$NON-NLS-1$
		env.addExternalJars(project2Path, Util.getJavaClassLibs());
		env.addRequiredProject(project2Path, project1Path);

		IPath project3Path = env.addProject("CP3"); //$NON-NLS-1$
		env.addExternalJars(project3Path, Util.getJavaClassLibs());
		env.addExternalJar(project3Path, jarPath.toString());

		fullBuild();
		expectingNoProblems();

		//----------------------------
		//           Step 2
		//----------------------------
		env.closeProject(project1Path);

		incrementalBuild();
		expectingOnlyProblemsFor(new IPath[] {project2Path, project3Path});
		expectingOnlySpecificProblemsFor(project2Path,
			new Problem[] {
				new Problem("", "The project cannot be built until build path errors are resolved", project2Path, -1, -1, -1), //$NON-NLS-1$ //$NON-NLS-2$
				new Problem("Build path", "Project CP2 is missing required Java project: 'CP1'", project2Path, -1, -1, -1) //$NON-NLS-1$ //$NON-NLS-2$
			}
		);
		expectingOnlySpecificProblemsFor(project3Path,
			new Problem[] {
				new Problem("", "The project cannot be built until build path errors are resolved", project3Path, -1, -1, -1), //$NON-NLS-1$ //$NON-NLS-2$
				new Problem("Build path", "Project CP3 is missing required library: '/CP1/temp.jar'", project3Path, -1, -1, -1) //$NON-NLS-1$ //$NON-NLS-2$
			}
		);

		env.openProject(project1Path);
		incrementalBuild();
		expectingNoProblems();

		//----------------------------
		//           Step 3
		//----------------------------
		Hashtable options = JavaCore.getOptions();
		options.put(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, JavaCore.IGNORE);
		JavaCore.setOptions(options);
		env.closeProject(project1Path);

		incrementalBuild();
		expectingOnlyProblemsFor(new IPath[] {project2Path, project3Path});
		expectingOnlySpecificProblemFor(project2Path,
			new Problem("Build path", "Project CP2 is missing required Java project: 'CP1'", project2Path, -1, -1, -1) //$NON-NLS-1$ //$NON-NLS-2$
		);
		expectingOnlySpecificProblemFor(project3Path,
			new Problem("Build path", "Project CP3 is missing required library: '/CP1/temp.jar'", project3Path, -1, -1, -1) //$NON-NLS-1$ //$NON-NLS-2$
		);

		env.openProject(project1Path);
		incrementalBuild();
		expectingNoProblems();

		options.put(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, JavaCore.ABORT);
		JavaCore.setOptions(options);
	}
	
	/*
	 * Ensures that changing an external jar and refreshing the projects triggers a rebuild
	 * (regression test for bug 50207 Compile errors fixed by 'refresh' do not reset problem list or package explorer error states)
	 */
	public void testExternalJarChange() throws JavaModelException, IOException {
		// setup
		IPath projectPath = env.addProject("Project"); //$NON-NLS-1$
		env.addExternalJars(projectPath, Util.getJavaClassLibs());
		IPath root = env.getPackageFragmentRootPath(projectPath, ""); //$NON-NLS-1$
		IPath classTest = env.addClass(root, "p", "X", //$NON-NLS-1$ //$NON-NLS-2$
			"package p;\n"+ //$NON-NLS-1$
			"public class X {\n" + //$NON-NLS-1$
			"  void foo() {\n" + //$NON-NLS-1$
			"    new q.Y().bar();\n" + //$NON-NLS-1$
			"  }\n" + //$NON-NLS-1$
			"}" //$NON-NLS-1$
		);
		String externalJar = Util.getOutputDirectory() + File.separator + "test.jar"; //$NON-NLS-1$
		Util.createJar(
			new String[] {
				"q/Y.java", //$NON-NLS-1$
				"package q;\n" + //$NON-NLS-1$
				"public class Y {\n" + //$NON-NLS-1$
				"}" //$NON-NLS-1$
			},
			new HashMap(),
			externalJar
		);
		long lastModified = new java.io.File(externalJar).lastModified();
		env.addExternalJar(projectPath, externalJar);
		
		// build -> expecting problems
		fullBuild();
		expectingProblemsFor(classTest);
		
		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
		}
		// fix jar
		Util.createJar(
			new String[] {
				"q/Y.java", //$NON-NLS-1$
				"package q;\n" + //$NON-NLS-1$
				"public class Y {\n" + //$NON-NLS-1$
				"  public void bar() {\n" + //$NON-NLS-1$
				"  }\n" + //$NON-NLS-1$
				"}" //$NON-NLS-1$
			},
			new HashMap(),
			externalJar
		);
		
		new java.io.File(externalJar).setLastModified(lastModified + 1000); // to be sure its different
		// refresh project and rebuild -> expecting no problems
		IJavaProject project = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject("Project")); //$NON-NLS-1$
		project.getJavaModel().refreshExternalArchives(new IJavaElement[] {project}, null);
		incrementalBuild();
		expectingNoProblems();
		
	}

	public void testMissingLibrary1() throws JavaModelException {
		IPath projectPath = env.addProject("Project"); //$NON-NLS-1$
		env.removePackageFragmentRoot(projectPath, ""); //$NON-NLS-1$
		IPath root = env.addPackageFragmentRoot(projectPath, "src"); //$NON-NLS-1$
		IPath bin = env.setOutputFolder(projectPath, "bin"); //$NON-NLS-1$
		IPath classTest1 = env.addClass(root, "p1", "Test1", //$NON-NLS-1$ //$NON-NLS-2$
			"package p1;\n"+ //$NON-NLS-1$
			"public class Test1 {}" //$NON-NLS-1$
		);

		fullBuild();
		expectingOnlyProblemsFor(new IPath[] {projectPath, classTest1});
		expectingOnlySpecificProblemsFor(projectPath,
			new Problem[] {
				new Problem("", "The project was not built since its build path is incomplete. Cannot find the class file for java.lang.Object. Fix the build path then try building this project", projectPath, -1, -1, -1), //$NON-NLS-1$ //$NON-NLS-2$
				new Problem("p1", "The type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files", classTest1, 0, 1, CategorizedProblem.CAT_BUILDPATH) //$NON-NLS-1$ //$NON-NLS-2$
			}
		);

		//----------------------------
		//           Step 2
		//----------------------------	
		env.addExternalJars(projectPath, Util.getJavaClassLibs());

		incrementalBuild();
		expectingNoProblems();
		expectingPresenceOf(new IPath[]{
			bin.append("p1").append("Test1.class"), //$NON-NLS-1$ //$NON-NLS-2$
		});
	}
	
	public void testMissingLibrary2() throws JavaModelException {
		IPath projectPath = env.addProject("Project"); //$NON-NLS-1$
		env.removePackageFragmentRoot(projectPath, ""); //$NON-NLS-1$
		IPath root = env.addPackageFragmentRoot(projectPath, "src"); //$NON-NLS-1$
		IPath bin = env.setOutputFolder(projectPath, "bin"); //$NON-NLS-1$
		IPath classTest1 = env.addClass(root, "p1", "Test1", //$NON-NLS-1$ //$NON-NLS-2$
			"package p1;\n"+ //$NON-NLS-1$
			"public class Test1 {}" //$NON-NLS-1$
		);
		IPath classTest2 = env.addClass(root, "p2", "Test2", //$NON-NLS-1$ //$NON-NLS-2$
			"package p2;\n"+ //$NON-NLS-1$
			"public class Test2 {}" //$NON-NLS-1$
		);
		IPath classTest3 = env.addClass(root, "p2", "Test3", //$NON-NLS-1$ //$NON-NLS-2$
			"package p2;\n"+ //$NON-NLS-1$
			"public class Test3 {}" //$NON-NLS-1$
		);

		fullBuild();
		expectingSpecificProblemFor(
			projectPath,
			new Problem("", "The project was not built since its build path is incomplete. Cannot find the class file for java.lang.Object. Fix the build path then try building this project", projectPath, -1, -1, -1)); //$NON-NLS-1$ //$NON-NLS-2$
		
		Problem[] prob1 = env.getProblemsFor(classTest1);
		Problem[] prob2 = env.getProblemsFor(classTest2);
		Problem[] prob3 = env.getProblemsFor(classTest3);
		assertEquals("too many problems", prob1.length + prob2.length + prob3.length,1); //$NON-NLS-1$
		if(prob1.length == 1) {
			expectingSpecificProblemFor(classTest1, new Problem("p1", "The type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files", classTest1, -1, -1, -1)); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (prob2.length == 1) {
			expectingSpecificProblemFor(classTest2, new Problem("p2", "The type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files", classTest2, -1, -1, -1)); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			expectingSpecificProblemFor(classTest3, new Problem("p2", "The type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files", classTest3, 0, 1, CategorizedProblem.CAT_BUILDPATH)); //$NON-NLS-1$ //$NON-NLS-2$
		}

		//----------------------------
		//           Step 2
		//----------------------------	
		env.addExternalJars(projectPath, Util.getJavaClassLibs());

		incrementalBuild();
		expectingNoProblems();
		expectingPresenceOf(new IPath[]{
			bin.append("p1").append("Test1.class"), //$NON-NLS-1$ //$NON-NLS-2$
			bin.append("p2").append("Test2.class"), //$NON-NLS-1$ //$NON-NLS-2$
			bin.append("p2").append("Test3.class") //$NON-NLS-1$ //$NON-NLS-2$
		});
	}

	public void testMissingProject() throws JavaModelException {
		IPath project1Path = env.addProject("MP1"); //$NON-NLS-1$
		env.addExternalJars(project1Path, Util.getJavaClassLibs());

		IPath project2Path = env.addProject("MP2"); //$NON-NLS-1$
		env.addExternalJars(project2Path, Util.getJavaClassLibs());
		env.addRequiredProject(project2Path, project1Path);

		fullBuild();
		expectingNoProblems();

		//----------------------------
		//           Step 2
		//----------------------------
		env.removeProject(project1Path);

		incrementalBuild();
		expectingOnlyProblemsFor(project2Path);
		expectingOnlySpecificProblemsFor(project2Path,
			new Problem[] {
				new Problem("", "The project cannot be built until build path errors are resolved", project2Path, -1, -1, -1), //$NON-NLS-1$ //$NON-NLS-2$
				new Problem("Build path", "Project MP2 is missing required Java project: 'MP1'", project2Path, -1, -1, -1) //$NON-NLS-1$ //$NON-NLS-2$
			}
		);

		project1Path = env.addProject("MP1"); //$NON-NLS-1$
		env.addExternalJars(project1Path, Util.getJavaClassLibs());

		incrementalBuild();
		expectingNoProblems();

		//----------------------------
		//           Step 3
		//----------------------------
		Hashtable options = JavaCore.getOptions();
		options.put(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, JavaCore.IGNORE);
		JavaCore.setOptions(options);
		env.removeProject(project1Path);

		incrementalBuild();
		expectingOnlyProblemsFor(project2Path);
		expectingOnlySpecificProblemFor(project2Path,
			new Problem("Build path", "Project MP2 is missing required Java project: 'MP1'", project2Path, -1, -1, -1) //$NON-NLS-1$ //$NON-NLS-2$
		);

		project1Path = env.addProject("MP1"); //$NON-NLS-1$
		env.addExternalJars(project1Path, Util.getJavaClassLibs());

		incrementalBuild();
		expectingNoProblems();

		options.put(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, JavaCore.ABORT);
		JavaCore.setOptions(options);
	}
}
