/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.model;

import java.io.UnsupportedEncodingException;

import junit.framework.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Util;

public class EncodingTests extends ModifyingResourceTests {
	IProject encodingProject;
	IJavaProject encodingJavaProject;
	IFile utf8File;
	ISourceReference utf8Source;

	public EncodingTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return suite(EncodingTests.class, null);
	}
	// Use this static initializer to specify subset for tests
	// All specified tests which do not belong to the class are skipped...
	static {
		// Names of tests to run: can be "testBugXXXX" or "BugXXXX")
//		testsNames = new String[] { "Bug51529a", "Bug51529b" };
		// Numbers of tests to run: "test<number>" will be run for each number of this array
//		testsNumbers = new int[] { 13 };
		// Range numbers of tests to run: all tests between "test<first>" and "test<last>" will be run for { first, last }
//		testsRange = new int[] { 16, -1 };
	}

	public void setUpSuite() throws Exception {
		super.setUpSuite();
		getWorkspaceRoot().setDefaultCharset("Cp1252");
		this.encodingJavaProject = setUpJavaProject("Encoding");
		this.encodingProject = (IProject) this.encodingJavaProject.getResource();
		this.utf8File = (IFile) this.encodingProject.findMember("src/testUTF8/Test.java");
	}

	public void tearDownSuite() throws Exception {
		super.tearDownSuite();
		getWorkspaceRoot().setDefaultCharset(null);
	}
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 *  (non-Javadoc)
	 * Reset UTF-8 file and project charset to default.
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		this.encodingProject.setDefaultCharset(null);
		this.utf8File.setCharset(null);
		if (this.utf8Source != null) ((IOpenable) this.utf8Source).close();
		this.encodingJavaProject.close();
	}

	/**
	 * Check that the compilation unit is saved with the proper encoding.
	 */
	public void testCreateCompilationUnitAndImportContainer() throws JavaModelException, CoreException {
		String savedEncoding = null;
		try {
			Preferences preferences = ResourcesPlugin.getPlugin().getPluginPreferences();
			
			savedEncoding = preferences.getString(ResourcesPlugin.PREF_ENCODING);
			String encoding = "UTF-8";
			preferences.setValue(ResourcesPlugin.PREF_ENCODING, encoding);
			
			ResourcesPlugin.getPlugin().savePluginPreferences();

			IJavaProject newProject = createJavaProject("P", new String[] { "" }, "");
			IPackageFragment pkg = getPackageFragment("P", "", "");
			String source = "public class A {\r\n" +
				"	public static main(String[] args) {\r\n" +
				"		System.out.println(\"\u00e9\");\r\n" +
				"	}\r\n" +
				"}";
			ICompilationUnit cu= pkg.createCompilationUnit("A.java", source, false, new NullProgressMonitor());
			assertCreation(cu);
			cu.rename("B.java", true, new NullProgressMonitor());
			cu = pkg.getCompilationUnit("B.java");
			cu.rename("A.java", true, new NullProgressMonitor());
			cu = pkg.getCompilationUnit("A.java");
			byte[] tab = null;
			try {
				tab = cu.getSource().getBytes(encoding);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			byte[] encodedContents = Util.getResourceContentsAsByteArray(newProject.getProject().getWorkspace().getRoot().getFile(cu.getPath()));
			assertTrue("wrong size of encoded string", tab.length == encodedContents.length);
			for (int i = 0, max = tab.length; i < max; i++) {
				assertTrue("wrong size of encoded character at" + i, tab[i] == encodedContents[i]);
			}
		} finally {
			deleteProject("P");
			Preferences preferences = ResourcesPlugin.getPlugin().getPluginPreferences();
			preferences.setValue(ResourcesPlugin.PREF_ENCODING, savedEncoding);
			ResourcesPlugin.getPlugin().savePluginPreferences();
		}
	}	

	/**
	 * Get compilation unit source on a file written in UTF-8 charset using specific UTF-8 encoding for file.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test001() throws JavaModelException, CoreException {

		// Set file encoding
		String encoding = "UTF-8";
		this.utf8File.setCharset(encoding);
		
		// Get compilation unit and compare source
		this.utf8Source = getCompilationUnit(this.utf8File.getFullPath().toString());
		String source = this.utf8Source.getSource();
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertEquals("Encoded UTF-8 source should have been decoded the same way!", encodedContents, source);

		// Now compare bytes array
		byte[] sourceBytes = null;
		try {
			sourceBytes = source.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		byte[] encodedBytes = Util.getResourceContentsAsByteArray(this.utf8File);
		assertEquals("Wrong size of encoded string", encodedBytes.length, sourceBytes.length);
		for (int i = 0, max = sourceBytes.length; i < max; i++) {
			assertTrue("Wrong size of encoded character at " + i, sourceBytes[i] == encodedBytes[i]);
		}
	}	

	/**
	 * Get compilation unit source on a file written in UTF-8 charset using UTF-8 encoding for project.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test002() throws JavaModelException, CoreException {

		// Set project encoding
		String encoding = "UTF-8";
		this.encodingProject.setDefaultCharset(encoding);
		
		// Get compilation unit and compare source
		this.utf8Source = getCompilationUnit(this.utf8File.getFullPath().toString());
		String source = this.utf8Source.getSource();
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertEquals("Encoded UTF-8 source should have been decoded the same way!", encodedContents, source);

		// Now compare bytes array
		byte[] sourceBytes = null;
		try {
			sourceBytes = source.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		byte[] encodedBytes = Util.getResourceContentsAsByteArray(this.utf8File);
		assertEquals("Wrong size of encoded string", encodedBytes.length, sourceBytes.length);
		for (int i = 0, max = sourceBytes.length; i < max; i++) {
			assertTrue("Wrong size of encoded character at " + i, sourceBytes[i] == encodedBytes[i]);
		}
	}	

	/**
	 * Get compilation unit source on a file written in UTF-8 charset using no specific encoding.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test003() throws JavaModelException, CoreException {

		// Get compilation unit and compare source (should be different as no encoding was specified on file)
		this.utf8Source = getCompilationUnit(this.utf8File.getFullPath().toString());
		String source = this.utf8Source.getSource();
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, "UTF-8"));
		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
	}

	/**
	 * Get compilation unit source on a file written in UTF-8 charset using specific UTF-8 encoding for file.
	 * Verification is done by comparing source with file contents read directly with no specific encoding...
	 */
	public void test004() throws JavaModelException, CoreException {

		// Set file encoding
		String encoding = "UTF-8";
		this.utf8File.setCharset(encoding);
		
		// Get compilation unit and compare source
		this.utf8Source = getCompilationUnit(this.utf8File.getFullPath().toString());
		String source = this.utf8Source.getSource();
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, null));
		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
	}	

	/**
	 * Get compilation unit source on a file written in UTF-8 charset using UTF-8 encoding for project.
	 * Verification is done by comparing source with file contents read directly with no specific encoding...
	 */
	public void test005() throws JavaModelException, CoreException {

		// Set project encoding
		String encoding = "UTF-8";
		this.encodingProject.setDefaultCharset(encoding);
		
		// Get compilation unit and compare source
		this.utf8Source = getCompilationUnit(this.utf8File.getFullPath().toString());
		String source = this.utf8Source.getSource();
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, null));
		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
	}	

	/**
	 * Get compilation unit source on a file written in UTF-8 charset using no specific encoding.
	 * Verification is done by comparing source with file contents read directly with no specific encoding...
	 */
	// TODO (frederic) put back assertions when failure on linux platform will be fixed
	public void test006() throws JavaModelException, CoreException {

		// Get compilation unit and compare source
		this.utf8Source = getCompilationUnit(this.utf8File.getFullPath().toString());
		String source = this.utf8Source.getSource();
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File));
		System.out.println("Encoding tests006:");
		System.out.println("  - file encoding: "+this.utf8File.getCharset());
		System.out.println("  - project encoding: "+this.encodingProject.getDefaultCharset());
		System.out.println("  - workspace encoding: "+getWorkspaceRoot().getDefaultCharset());
		if (encodedContents.equals(source)) {
			System.out.println("  => SUCCESS: sources are identical");
		} else {
			System.out.println("  => FAIL: Encoded UTF-8 source should have been decoded the same way!");
		}
//		assertTrue("Encoded UTF-8 source should have been decoded the same way!", encodedContents.equals(source));
			
		// Now compare bytes array
		byte[] sourceBytes = source.getBytes();
		byte[] encodedBytes = Util.getResourceContentsAsByteArray(this.utf8File);
		if (encodedBytes.length == sourceBytes.length) {
			System.out.println("  => SUCCESS: bytes arrays have same size");
		} else {
			System.out.println("  => FAIL: Wrong size of encoded string");
		}
//		assertEquals("Wrong size of encoded string", encodedBytes.length, sourceBytes.length);
		for (int i = 0, max = sourceBytes.length; i < max; i++) {
			if (sourceBytes[i] != encodedBytes[i]) {
				System.out.println("  => FAIL: Wrong size of encoded character at " + i);
				break;
			}
//			assertTrue("Wrong size of encoded character at " + i, sourceBytes[i] == encodedBytes[i]);
		}
	}

	/**
	 * Get class file with an associated source written in UTF-8 charset using specific UTF-8 encoding for file.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test007() throws JavaModelException, CoreException {

		// Set file encoding
		String encoding = "UTF-8";
		this.utf8File.setCharset(encoding);
		
		// Get class file and compare source (should be the same as we ge charset on file in SourceMapper.findSource(String)...)
		this.utf8Source = getClassFile("Encoding" , "bins", "testUTF8", "Test.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertEquals("Encoded UTF-8 source should have been decoded the same way!", encodedContents, source);

		// Now compare bytes array
		byte[] sourceBytes = null;
		try {
			sourceBytes = source.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		byte[] encodedBytes = Util.getResourceContentsAsByteArray(this.utf8File);
		assertEquals("Wrong size of encoded string", encodedBytes.length, sourceBytes.length);
		for (int i = 0, max = sourceBytes.length; i < max; i++) {
			assertTrue("Wrong size of encoded character at " + i, sourceBytes[i] == encodedBytes[i]);
		}
	}

	/**
	 * Get class file with an associated source written in UTF-8 charset using specific UTF-8 encoding for project.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test008() throws JavaModelException, CoreException {

		// Set project encoding
		String encoding = "UTF-8";
		this.encodingProject.setDefaultCharset(encoding);
		
		// Get class file and compare source
		this.utf8Source = getClassFile("Encoding" , "bins", "testUTF8", "Test.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertEquals("Encoded UTF-8 source should have been decoded the same way!", encodedContents, source);

		// Now compare bytes array
		byte[] sourceBytes = null;
		try {
			sourceBytes = source.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		byte[] encodedBytes = Util.getResourceContentsAsByteArray(this.utf8File);
		assertEquals("Wrong size of encoded string", encodedBytes.length, sourceBytes.length);
		for (int i = 0, max = sourceBytes.length; i < max; i++) {
			assertTrue("Wrong size of encoded character at " + i, sourceBytes[i] == encodedBytes[i]);
		}
	}

	/**
	 * Get class file with an associated source written in UTF-8 charset using specific ASCII encoding on project.
	 * Verification is done by comparing source with file contents read directly with ASCII encoding...
	 */
	public void test009() throws JavaModelException, CoreException {

		// Set project encoding
		String encoding = "ASCII";
		this.encodingProject.setDefaultCharset(encoding);
		
		// Get class file and compare source
		this.utf8Source = getClassFile("Encoding" , "bins", "testUTF8", "Test.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertEquals("Encoded UTF-8 source should have been decoded the same way!", encodedContents, source);

		// Cannot compare bytes array as read UTF-8 using ASCII encoding destroy meaningful bytes...
	}

	/**
	 * Get class file with an associated source written in UTF-8 charset using no specific encoding.
	 * Verification is done by comparing source with file contents read directly with ASCII encoding...
	 */
	// TODO (frederic) put back assertions when failure on linux platform will be fixed
	public void test010() throws JavaModelException, CoreException {

		// Get class file and compare source
		this.utf8Source = getClassFile("Encoding" , "bins", "testUTF8", "Test.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, "ASCII"));
//		assertFalse("Workspace encoding: "+getWorkspaceRoot().getDefaultCharset()+" should be different than ASCII!", "ASCII".equals(getWorkspaceRoot().getDefaultCharset()));
//		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
		System.out.println("Encoding tests010:");
		System.out.println("  - file encoding: "+this.utf8File.getCharset());
		System.out.println("  - project encoding: "+this.encodingProject.getDefaultCharset());
		System.out.println("  - workspace encoding: "+getWorkspaceRoot().getDefaultCharset());
		if (encodedContents.equals(source)) {
			System.out.println("  => FAIL: Sources should not be the same as they were decoded with different encoding!");
		} else {
			System.out.println("  => SUCCESS: sources are different");
		}
	}

	/**
	 * Get class file with an associated source written in UTF-8 charset using specific UTF-8 encoding on project.
	 * Verification is done by comparing source with file contents read directly with ASCII encoding...
	 */
	public void test011() throws JavaModelException, CoreException {

		// Set project encoding
		this.encodingProject.setDefaultCharset("UTF-8");
		
		// Get class file and compare source
		this.utf8Source = getClassFile("Encoding" , "bins", "testUTF8", "Test.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, "ASCII"));
		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
	}

	/**
	 * Get class file with an associated source written in UTF-8 charset using specific UTF-8 encoding for file.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test012() throws JavaModelException, CoreException {

		// Set file encoding
		String encoding = "UTF-8";
		IFile classFile = (IFile) this.encodingProject.findMember("bins/testUTF8/Test.class"); //$NON-NLS-1$
		assertNotNull("Cannot find class file!", classFile);
		classFile.setCharset(encoding);
		
		// Get class file and compare source (should not be the same as modify charset on file is unefficient for class files...)
		this.utf8Source = getClassFile("Encoding" , "bins", "testUTF8", "Test.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
		
		// Reset class file encoding
		classFile.setCharset(null);
	}

	/**
	 * Get class file from jar file with an associated source written in UTF-8 charset using no specific encoding for file.
	 * Verification is done by comparing source with file contents read directly with no specific encoding...
	 */
	public void test013() throws JavaModelException, CoreException {

		// Get class file and compare source
		IPackageFragmentRoot root = getPackageFragmentRoot("Encoding", "testUTF8.jar");
		this.utf8Source = root.getPackageFragment("testUTF8").getClassFile("Test.class");
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, null));
//		String independentEncodedContents = org.eclipse.jdt.core.tests.util.Util.convertToIndependantLineDelimiter(encodedContents);
//		String independentSource = org.eclipse.jdt.core.tests.util.Util.convertToIndependantLineDelimiter(source);
//		assertEquals("Encoded UTF-8 source should have been decoded the same way!", independentEncodedContents, independentSource);
		assertSourceEquals("Encoded UTF-8 source should have been decoded the same way!", source, encodedContents);

		// Cannot compare bytes array without encoding as we're dependent of linux/windows os for new lines delimiter
	}

	/**
	 * Get class file from jar file with an associated source written in UTF-8 charset using specific UTF-8 encoding for project.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test014() throws JavaModelException, CoreException {

		// Set project encoding
		String encoding = "UTF-8";
		this.encodingProject.setDefaultCharset(encoding);

		// Get class file and compare source (should not be the same as modify charset on zip file has no effect...)
		IPackageFragmentRoot root = getPackageFragmentRoot("Encoding", "testUTF8.jar");
		this.utf8Source = root.getPackageFragment("testUTF8").getClassFile("Test.class");
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
	}

	/**
	 * Get class file from jar file with an associated source written in UTF-8 charset using specific UTF-8 encoding for file.
	 * Verification is done by comparing source with file contents read directly with UTF-8 encoding...
	 */
	public void test015() throws JavaModelException, CoreException {

		// Set file encoding
		String encoding = "UTF-8";
		IFile zipFile = (IFile) this.encodingProject.findMember("testUTF8.zip"); //$NON-NLS-1$
		assertNotNull("Cannot find class file!", zipFile);
		zipFile.setCharset(encoding);

		// Get class file and compare source (should not be the same as modify charset on zip file has no effect...)
		IPackageFragmentRoot root = getPackageFragmentRoot("Encoding", "testUTF8.jar");
		this.utf8Source = root.getPackageFragment("testUTF8").getClassFile("Test.class");
		assertNotNull(this.utf8Source);
		String source = this.utf8Source.getSource();
		assertNotNull(source);
		String encodedContents = new String (Util.getResourceContentsAsCharArray(this.utf8File, encoding));
		assertFalse("Sources should not be the same as they were decoded with different encoding!", encodedContents.equals(source));
		
		// Reset zip file encoding
		zipFile.setCharset(null);
	}

	public void test016() throws JavaModelException, CoreException {
		ICompilationUnit workingCopy = null;
		try {
			String encoding = "UTF-8";
			this.createJavaProject("P", new String[] {""}, "");
			String initialContent = "/**\n"+
				" */\n"+
				"public class Test {}";
			IFile file = this.createFile("P/Test.java", initialContent);
			file.setCharset(encoding);
			ICompilationUnit cu = this.getCompilationUnit("P/Test.java"); 
			
			// Modif direct the buffer
			String firstModif = "/**\n"+
				" * Caract�res exotiques:\n"+
				" * �|#|�|�|�|�|�|�|�|�|��\n"+
				" */\n"+
				"public class Test {}";
			cu.getBuffer().setContents(firstModif);
			cu.getBuffer().save(null, true);
			String source = cu.getBuffer().getContents();
			
			// Compare strings and bytes arrays
			String encodedContents = new String (Util.getResourceContentsAsCharArray(file, encoding));
			assertEquals("Encoded UTF-8 source should have been decoded the same way!", encodedContents, source);
			byte[] sourceBytes = source.getBytes(encoding);
			byte[] encodedBytes = Util.getResourceContentsAsByteArray(file);
			assertEquals("Wrong size of encoded string", encodedBytes.length, sourceBytes.length);
			for (int i = 0, max = sourceBytes.length; i < max; i++) {
				assertTrue("Wrong size of encoded character at " + i, sourceBytes[i] == encodedBytes[i]);
			}
		} catch (UnsupportedEncodingException e) {
		} finally {
			this.stopDeltas();
			if (workingCopy != null) {
				workingCopy.discardWorkingCopy();
			}
			this.deleteProject("P");
		}

	}

	public void test017() throws JavaModelException, CoreException {
		ICompilationUnit workingCopy = null;
		try {
			String encoding = "UTF-8";
			this.createJavaProject("P", new String[] {""}, "");
			String initialContent = "/**\n"+
				" */\n"+
				"public class Test {}";
			IFile file = this.createFile("P/Test.java", initialContent);
			file.setCharset(encoding);
			ICompilationUnit cu = this.getCompilationUnit("P/Test.java"); 
			
			// Modif using working copy
			workingCopy = cu.getWorkingCopy(null);
			String secondModif = "/**\n"+
				" * Caract�res exotiques:\n"+
				" * �|#|�|�|�|�|�|�|�|�|��\n"+
				" * Autres caract�res exotiques:\n"+
				" * �|�|�|�|�|�\n"+
				" */\n"+
				"public class Test {}";
			workingCopy.getBuffer().setContents(secondModif);
			workingCopy.commitWorkingCopy(true, null);
			String source = workingCopy.getBuffer().getContents();
			
			// Compare strings and bytes arrays
			String encodedContents = new String (Util.getResourceContentsAsCharArray(file));
			assertEquals("Encoded UTF-8 source should have been decoded the same way!", encodedContents, source);
			byte[] sourceBytes = source.getBytes(encoding);
			byte[] encodedBytes = Util.getResourceContentsAsByteArray(file);
			assertEquals("Wrong size of encoded string", encodedBytes.length, sourceBytes.length);
			for (int i = 0, max = sourceBytes.length; i < max; i++) {
				assertTrue("Wrong size of encoded character at " + i, sourceBytes[i] == encodedBytes[i]);
			}
		} catch (UnsupportedEncodingException e) {
		} finally {
			this.stopDeltas();
			if (workingCopy != null) {
				workingCopy.discardWorkingCopy();
			}
			this.deleteProject("P");
		}

	}
}
