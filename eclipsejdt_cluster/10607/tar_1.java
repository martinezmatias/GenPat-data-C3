/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.performance;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import junit.framework.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.core.tests.model.AbstractJavaModelTests;
import org.eclipse.jdt.core.tests.model.AbstractJavaModelTests.ProblemRequestor;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.test.performance.Performance;

/**
 */
public class FullSourceWorkspaceModelTests extends FullSourceWorkspaceTests implements IJavaSearchConstants {

	// Tests counters
	static int TESTS_COUNT = 0;
	private final static int WARMUP_COUNT = 50;
	private final static int FOLDERS_COUNT = 200;
	private final static int PACKAGES_COUNT = 200;
	static int TESTS_LENGTH;

	// Log file streams
	private static PrintStream[] LOG_STREAMS = new PrintStream[DIM_NAMES.length];

	// Type path
	static IPath BIG_PROJECT_TYPE_PATH;
	static ICompilationUnit WORKING_COPY;

/**
 * @param name
 */
public FullSourceWorkspaceModelTests(String name) {
	super(name);
}

static {
//	TESTS_NAMES = new String[] {
//		"testPerfNameLookupFindKnownSecondaryType",
//		"testPerfNameLookupFindUnknownType",
//		"testPerfReconcile", 
//		"testPerfSearchAllTypeNamesAndReconcile",
//	};
	
//	TESTS_PREFIX = "testPerfReconcile";
}
public static Test suite() {
	Test suite = buildSuite(testClass());
	TESTS_LENGTH = TESTS_COUNT = suite.countTestCases();
	createPrintStream(testClass(), LOG_STREAMS, TESTS_COUNT, null);
	return suite;
}

private static Class testClass() {
	return FullSourceWorkspaceModelTests.class;
}

protected void setUp() throws Exception {
	super.setUp();
	if (BIG_PROJECT == null) {
		setUpBigProject();
	} else if (BIG_PROJECT_TYPE_PATH == null) {
		setUpBigProjectInfo();
	}
}
private void setUpBigProject() throws CoreException, IOException {
	try {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		String targetWorkspacePath = workspaceRoot.getLocation().toFile().getCanonicalPath();
		long start = System.currentTimeMillis();

		// Print for log in case of project creation troubles...
		File wkspDir = new File(targetWorkspacePath);
		File projectDir = new File(wkspDir, BIG_PROJECT_NAME);
		if (projectDir.exists()) {
			System.out.print("Add existing project "+BIG_PROJECT_NAME+" in "+workspaceRoot.getLocation()+" to workspace...");
			IProject bigProject = workspaceRoot.getProject(BIG_PROJECT_NAME);
			if (bigProject.exists()) {
				ENV.addProject(bigProject);
			} else {
				ENV.addProject(BIG_PROJECT_NAME);
			}
			BIG_PROJECT = (JavaProject) ENV.getJavaProject(BIG_PROJECT_NAME);
			BIG_PROJECT.setRawClasspath(BIG_PROJECT.getRawClasspath(), null);
		} else {
			System.out.println("Create project "+BIG_PROJECT_NAME+" in "+workspaceRoot.getLocation()+":");
	
			// setup projects with several source folders and several packages per source folder
			System.out.println("	- create "+FOLDERS_COUNT+" folders x "+PACKAGES_COUNT+" packages...");
			final String[] sourceFolders = new String[FOLDERS_COUNT];
			for (int i = 0; i < FOLDERS_COUNT; i++) {
				sourceFolders[i] = "src" + i;
			}
			String path = workspaceRoot.getLocation().toString() + "/BigProject/src";
			for (int i = 0; i < FOLDERS_COUNT; i++) {
				if (PRINT && i>0 && i%10==0) System.out.print("		+ folder src"+i+"...");
				long top = System.currentTimeMillis();
				for (int j = 0; j < PACKAGES_COUNT; j++) {
					new java.io.File(path + i + "/org/eclipse/jdt/core/tests" + i + "/performance" + j).mkdirs();
				}
				if (PRINT && i>0 && i%10==0) System.out.println("("+(System.currentTimeMillis()-top)+"ms)");
			}
			System.out.println("		=> global time = "+(System.currentTimeMillis()-start)/1000.0+" seconds)");

			// Add project to workspace
			start = System.currentTimeMillis();
			System.out.print("	- add project to full source workspace...");
			ENV.addProject(BIG_PROJECT_NAME);
			BIG_PROJECT = (JavaProject) createJavaProject(BIG_PROJECT_NAME, sourceFolders, "bin", "1.4");
			BIG_PROJECT.setRawClasspath(BIG_PROJECT.getRawClasspath(), null);
		}
		System.out.println("("+(System.currentTimeMillis()-start)+"ms)");

		// Print for log in case of project creation troubles...
		System.out.println("("+(System.currentTimeMillis()-start)+"ms)");
		start = System.currentTimeMillis();
		System.out.print("	- Create compilation unit with secondary type...");

		// Add CU with secondary type
		BIG_PROJECT_TYPE_PATH = new Path("/BigProject/src" + (FOLDERS_COUNT-1) + "/org/eclipse/jdt/core/tests" + (FOLDERS_COUNT-1) + "/performance" + (PACKAGES_COUNT-1) + "/TestBigProject.java");
		IFile file = workspaceRoot.getFile(BIG_PROJECT_TYPE_PATH);
		if (!file.exists()) {
			String content = "package org.eclipse.jdt.core.tests" + (FOLDERS_COUNT-1) + ".performance" + (PACKAGES_COUNT-1) + ";\n" +
				"public class TestBigProject {\n" +
				"	class Level1 {\n" +
				"		class Level2 {\n" +
				"			class Level3 {\n" +
				"				class Level4 {\n" +
				"					class Level5 {\n" +
				"						class Level6 {\n" +
				"							class Level7 {\n" +
				"								class Level8 {\n" +
				"									class Level9 {\n" +
				"										class Level10 {}\n" +
				"									}\n" +
				"								}\n" +
				"							}\n" +
				"						}\n" +
				"					}\n" +
				"				}\n" +
				"			}\n" +
				"		}\n" +
				"	}\n" +
				"}\n" +
				"class TestSecondary {}\n";
			file.create(new ByteArrayInputStream(content.getBytes()), true, null);
		}
		WORKING_COPY = (ICompilationUnit)JavaCore.create(file);
		System.out.println("("+(System.currentTimeMillis()-start)+"ms)");
	} finally {
		// do not delete project
	}
	
}
private void setUpBigProjectInfo() {
	// Set up type path
	BIG_PROJECT_TYPE_PATH = new Path("/BigProject/src" + (FOLDERS_COUNT-1) + "/org/eclipse/jdt/core/tests" + (FOLDERS_COUNT-1) + "/performance" + (PACKAGES_COUNT-1) + "/TestBigProject.java");

	// Set up working copy
	IWorkspace workspace = ResourcesPlugin.getWorkspace();
	IWorkspaceRoot workspaceRoot = workspace.getRoot();
	IFile file = workspaceRoot.getFile(BIG_PROJECT_TYPE_PATH);
	WORKING_COPY = (ICompilationUnit)JavaCore.create(file);
}
/* (non-Javadoc)
 * @see junit.framework.TestCase#tearDown()
 */
protected void tearDown() throws Exception {

	// End of execution => one test less
	TESTS_COUNT--;

	// Log perf result
	if (LOG_DIR != null) {
		logPerfResult(LOG_STREAMS, TESTS_COUNT);
	}

	// Print statistics
	if (TESTS_COUNT == 0) {
		System.out.println("-------------------------------------");
		System.out.println("Model performance test statistics:");
//		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("-------------------------------------\n");
	}
	super.tearDown();
}
/**
 * Simple search result collector: only count matches.
 */
class JavaSearchResultCollector extends SearchRequestor {
	int count = 0;
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		this.count++;
	}
}

/*
protected void search(String patternString, int searchFor, int limitTo) throws CoreException {
	int matchMode = patternString.indexOf('*') != -1 || patternString.indexOf('?') != -1
		? SearchPattern.R_PATTERN_MATCH
		: SearchPattern.R_EXACT_MATCH;
	SearchPattern pattern = SearchPattern.createPattern(
		patternString, 
		searchFor,
		limitTo, 
		matchMode | SearchPattern.R_CASE_SENSITIVE);
	this.resultCollector = new JavaSearchResultCollector();
	new SearchEngine().search(
		pattern,
		new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
		this.scope,
		this.resultCollector,
		null);
}
*/

protected void searchAllTypeNames(IJavaSearchScope scope) throws CoreException {
	class TypeNameCounter extends TypeNameRequestor {
		int count = 0;
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
			count++;
		}
	}
	TypeNameCounter requestor = new TypeNameCounter();
	new SearchEngine().searchAllTypeNames(
		null,
		SearchPattern.R_EXACT_MATCH,
		null,
		SearchPattern.R_PREFIX_MATCH, // not case sensitive
		IJavaSearchConstants.TYPE,
		scope,
		requestor,
		WAIT_UNTIL_READY_TO_SEARCH,
		null);
	assertTrue("We should have found at least one type!", requestor.count>0);
}

/**
 * @see org.eclipse.jdt.core.tests.model.AbstractJavaModelTests#assertElementEquals(String, String, IJavaElement)
 */
protected void assertElementEquals(String message, String expected, IJavaElement element) {
	String actual = element == null ? "<null>" : ((JavaElement) element).toStringWithAncestors(false/*don't show key*/);
	if (!expected.equals(actual)) {
		System.out.println(getName()+" actual result is:");
		System.out.println(actual + ',');
	}
	assertEquals(message, expected, actual);
}
/**
 * @see org.eclipse.jdt.core.tests.model.AbstractJavaModelTests#assertElementsEqual(String, String, IJavaElement[])
 */
protected void assertElementsEqual(String message, String expected, IJavaElement[] elements) {
	assertElementsEqual(message, expected, elements, false/*don't show key*/);
}
/**
 * @see org.eclipse.jdt.core.tests.model.AbstractJavaModelTests#assertElementsEqual(String, String, IJavaElement[], boolean)
 */
protected void assertElementsEqual(String message, String expected, IJavaElement[] elements, boolean showResolvedInfo) {
	StringBuffer buffer = new StringBuffer();
	if (elements != null) {
		for (int i = 0, length = elements.length; i < length; i++){
			JavaElement element = (JavaElement)elements[i];
			if (element == null) {
				buffer.append("<null>");
			} else {
				buffer.append(element.toStringWithAncestors(showResolvedInfo));
			}
			if (i != length-1) buffer.append("\n");
		}
	} else {
		buffer.append("<null>");
	}
	String actual = buffer.toString();
	if (!expected.equals(actual)) {
		System.out.println(getName()+" actual result is:");
		System.out.println(actual + ',');
	}
	assertEquals(message, expected, actual);
}

/*
 * Creates a simple Java project with no source folder and only rt.jar on its classpath.
 */
private IJavaProject createJavaProject(String name) throws CoreException {
	IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	if (project.exists())
		project.delete(true, null);
	project.create(null);
	project.open(null);
	IProjectDescription description = project.getDescription();
	description.setNatureIds(new String[] {JavaCore.NATURE_ID});
	project.setDescription(description, null);
	IJavaProject javaProject = JavaCore.create(project);
	javaProject.setRawClasspath(new IClasspathEntry[] {JavaCore.newVariableEntry(new Path("JRE_LIB"), null, null)}, null);
	return javaProject;
}

private NameLookup getNameLookup(JavaProject project) throws JavaModelException {
	return project.newNameLookup((WorkingCopyOwner)null);
}

/**
 * Performance tests for model: Find known type in name lookup.
 * 
 * First wait that already started indexing jobs end before perform test.
 * Perform one find before measure performance for warm-up.
 */
public void testNameLookupFindKnownType() throws CoreException {

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	String fullQualifiedName = BIG_PROJECT_TYPE_PATH.removeFileExtension().removeFirstSegments(2).toString();
	fullQualifiedName = fullQualifiedName.replace('/', '.');
	for (int i=0; i<WARMUP_COUNT; i++) {
		NameLookup nameLookup = BIG_PROJECT.newNameLookup(DefaultWorkingCopyOwner.PRIMARY);
		IType type = nameLookup.findType(fullQualifiedName, false /*full match*/, NameLookup.ACCEPT_ALL);
		if (i==0) assertNotNull("We should find type '"+fullQualifiedName+"' in project "+BIG_PROJECT_NAME, type);
	}

	// Measures
	resetCounters();
	for (int i=0; i<MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int n=0; n<50000; n++) {
			NameLookup nameLookup = BIG_PROJECT.newNameLookup(DefaultWorkingCopyOwner.PRIMARY);
			nameLookup.findType(fullQualifiedName, false /*full match*/, NameLookup.ACCEPT_ALL);
		}
		stopMeasuring();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();
}

/**
 * Performance tests for model: Find known secondary type in name lookup.
 * 
 * First wait that already started indexing jobs end before perform test.
 * Perform one find before measure performance for warm-up.
 */
public void testNameLookupFindKnownSecondaryType() throws CoreException {

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	String fullQualifiedName = BIG_PROJECT_TYPE_PATH.removeFileExtension().removeFirstSegments(2).removeLastSegments(1).toString();
	fullQualifiedName = fullQualifiedName.replace('/', '.')+".TestSecondary";
	for (int i=0; i<WARMUP_COUNT; i++) {
		NameLookup nameLookup = BIG_PROJECT.newNameLookup(DefaultWorkingCopyOwner.PRIMARY);
		IType type = nameLookup.findType(fullQualifiedName, false /*full match*/, NameLookup.ACCEPT_ALL);
		if (i==0 && LOG_VERSION.compareTo("v_623") > 0) {
			assertNotNull("We should find type '"+fullQualifiedName+"' in project "+BIG_PROJECT_NAME, type);
		}
	}

	// Measures
	resetCounters();
	for (int i=0; i<MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int n=0; n<50000; n++) {
			NameLookup nameLookup = BIG_PROJECT.newNameLookup(DefaultWorkingCopyOwner.PRIMARY);
			nameLookup.findType(fullQualifiedName, false /*full match*/, NameLookup.ACCEPT_ALL);
		}
		stopMeasuring();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();
}

/**
 * Performance tests for model: Find Unknown type in name lookup.
 * 
 * First wait that already started indexing jobs end before perform test.
 * Perform one find before measure performance for warm-up.
 */
public void testNameLookupFindUnknownType() throws CoreException {

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	String fullQualifiedName = BIG_PROJECT_TYPE_PATH.removeFileExtension().removeFirstSegments(2).removeLastSegments(1).toString();
	fullQualifiedName = fullQualifiedName.replace('/', '.')+".Unknown";
	for (int i=0; i<WARMUP_COUNT; i++) {
		NameLookup nameLookup = BIG_PROJECT.newNameLookup(DefaultWorkingCopyOwner.PRIMARY);
		IType type = nameLookup.findType(fullQualifiedName, false /*full match*/, NameLookup.ACCEPT_ALL);
		if (i==0) assertNull("We should not find an unknown type in project "+BIG_PROJECT_NAME, type);
	}

	// Measures
	resetCounters();
	for (int i=0; i<MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int n=0; n<50000; n++) {
			NameLookup nameLookup = BIG_PROJECT.newNameLookup(DefaultWorkingCopyOwner.PRIMARY);
			nameLookup.findType(fullQualifiedName, false /*full match*/, NameLookup.ACCEPT_ALL);
		}
		stopMeasuring();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();
}

/**
 * Performance tests for model: Find known type.
 * 
 * First wait that already started indexing jobs end before perform test.
 * Perform one find before measure performance for warm-up.
 */
public void testProjectFindKnownType() throws CoreException {
	tagAsSummary("Find known type in project", false); // do NOT put in fingerprint

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	String fullQualifiedName = BIG_PROJECT_TYPE_PATH.removeFileExtension().removeFirstSegments(2).toString();
	fullQualifiedName = fullQualifiedName.replace('/', '.');
	for (int i=0; i<WARMUP_COUNT; i++) {
		IType type = BIG_PROJECT.findType(fullQualifiedName);
		if (i==0) assertNotNull("We should find type '"+fullQualifiedName+"' in project "+BIG_PROJECT_NAME, type);
	}

	// Measures
	resetCounters();
	for (int i=0; i<MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int n=0; n<50000; n++) {
			BIG_PROJECT.findType(fullQualifiedName);
		}
		stopMeasuring();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();
}

/**
 * Performance tests for model: Find known member type.
 * 
 * First wait that already started indexing jobs end before perform test.
 * Perform one find before measure performance for warm-up.
 */
public void testProjectFindKnownMemberType() throws CoreException {
	tagAsSummary("Find known member type in project", false); // do NOT put in fingerprint

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	String fullQualifiedName = BIG_PROJECT_TYPE_PATH.removeFileExtension().removeFirstSegments(2).toString();
	fullQualifiedName = fullQualifiedName.replace('/', '.');
	for (int i=1; i<=10; i++) {
		fullQualifiedName += ".Level" + i;
	}
	for (int i=0; i<WARMUP_COUNT; i++) {
		IType type = BIG_PROJECT.findType(fullQualifiedName);
		if (i==0) assertNotNull("We should find type '"+fullQualifiedName+"' in project "+BIG_PROJECT_NAME, type);
	}

	// Measures
	resetCounters();
	for (int i=0; i<MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int n=0; n<4000; n++) {
			BIG_PROJECT.findType(fullQualifiedName);
		}
		stopMeasuring();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();
}

/**
 * Performance tests for model: Find known secondary type.
 * 
 * First wait that already started indexing jobs end before perform test.
 * Perform one find before measure performance for warm-up.
 */
public void testProjectFindKnownSecondaryType() throws CoreException {
	tagAsSummary("Find known secondary type in project", false); // do NOT put in fingerprint

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	String fullQualifiedName = BIG_PROJECT_TYPE_PATH.removeFileExtension().removeFirstSegments(2).removeLastSegments(1).toString();
	fullQualifiedName = fullQualifiedName.replace('/', '.')+".TestSecondary";
	for (int i=0; i<WARMUP_COUNT; i++) {
		BIG_PROJECT.findType(fullQualifiedName);
	}

	// Measures
	resetCounters();
	for (int i=0; i<MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int n=0; n<1000; n++) {
			BIG_PROJECT.findType(fullQualifiedName);
		}
		stopMeasuring();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();
}

/**
 * Performance tests for model: Find Unknown type.
 * 
 * First wait that already started indexing jobs end before perform test.
 * Perform one find before measure performance for warm-up.
 */
public void testProjectFindUnknownType() throws CoreException {
	tagAsSummary("Find unknown type in project", false); // do NOT put in fingerprint

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	String fullQualifiedName = BIG_PROJECT_TYPE_PATH.removeFileExtension().removeFirstSegments(2).removeLastSegments(1).toString();
	fullQualifiedName = fullQualifiedName.replace('/', '.')+".Unknown";
	for (int i=0; i<WARMUP_COUNT; i++) {
		IType type = BIG_PROJECT.findType(fullQualifiedName);
		assertNull("We should not find an unknown type in project "+BIG_PROJECT_NAME, type);
	}

	// Measures
	resetCounters();
	for (int i=0; i<MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int n=0; n<2000; n++) {
			BIG_PROJECT.findType(fullQualifiedName);
		}
		stopMeasuring();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();
}

/**
 * Ensures that the reconciler does nothing when the source
 * to reconcile with is the same as the current contents.
 */
public void testPerfReconcile() throws CoreException {
	tagAsGlobalSummary("Reconcile editor change", true); // put in global fingerprint

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	ICompilationUnit workingCopy = null;
	try {
		final ProblemRequestor requestor = new ProblemRequestor();
		WorkingCopyOwner owner = new WorkingCopyOwner() {
			public IProblemRequestor getProblemRequestor(ICompilationUnit cu) {
				return requestor;
            }
		};
		workingCopy = PARSER_WORKING_COPY.getWorkingCopy(owner, null);
		int warmup = WARMUP_COUNT / 5;
		for (int i=0; i<warmup; i++) {
			CompilationUnit unit = workingCopy.reconcile(AST.JLS3, true, null, null);
			assertNotNull("Compilation Unit should not be null!", unit);
			assertNotNull("Bindings were not resolved!", unit.getPackage().resolveBinding());
		}

		// Measures
		resetCounters();
		int iterations = 2;
		for (int i=0; i<MEASURES_COUNT; i++) {
			runGc();
			startMeasuring();
			for (int n=0; n<iterations; n++) {
				workingCopy.reconcile(AST.JLS3, true, null, null);
			}
			stopMeasuring();
		}
	}
	finally {
		workingCopy.discardWorkingCopy();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();

}

/*
 * Ensures that the performance of reconcile on a big CU when there are syntax errors is acceptable.
 * (regression test for bug 135083 RangeUtil#isInInterval(...) takes significant amount of time while editing)
 */
public void testPerfReconcileBigFileWithSyntaxError() throws JavaModelException {
	tagAsSummary("Reconcile editor change on big file with syntax error", true); // put in fingerprint
	
	// build big file contents
	String method =
		"() {\n" +
		"  bar(\n" +
		"    \"public class X <E extends Exception> {\\n\" + \r\n" + 
		"	 \"    void foo(E e) throws E {\\n\" + \r\n" + 
		"	 \"        throw e;\\n\" + \r\n" + 
		"	 \"    }\\n\" + \r\n" + 
		"	 \"    void bar(E e) {\\n\" + \r\n" + 
		"	 \"        try {\\n\" + \r\n" + 
		"	 \"            foo(e);\\n\" + \r\n" + 
		"	 \"        } catch(Exception ex) {\\n\" + \r\n" + 
		"	 \"	        System.out.println(\\\"SUCCESS\\\");\\n\" + \r\n" + 
		"	 \"        }\\n\" + \r\n" + 
		"	 \"    }\\n\" + \r\n" + 
		"	 \"    public static void main(String[] args) {\\n\" + \r\n" + 
		"	 \"        new X<Exception>().bar(new Exception());\\n\" + \r\n" + 
		"	 \"    }\\n\" + \r\n" + 
		"	 \"}\\n\"" +
		"  );\n" +
		"}\n";
	StringBuffer bigContents = new StringBuffer();
	bigContents.append("public class BigCU {\n");
	int fooIndex = 0;
	while (fooIndex < 2000) { // add 2000 methods (so that source is close to 1MB)
		bigContents.append("public void foo");
		bigContents.append(fooIndex++);
		bigContents.append(method);
	}
	// don't add closing } for class def so as to have a syntax error
	
	ICompilationUnit workingCopy = null;
	try {
		// Setup
		workingCopy = (ICompilationUnit) JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/BigProject/src/org/eclipse/jdt/core/tests/BigCu.java")));
		workingCopy.becomeWorkingCopy(null);
		
		// Warm up
		int warmup = WARMUP_COUNT / 10;
		for (int i=0; i<warmup; i++) {
			workingCopy.getBuffer().setContents(bigContents.append("a").toString());
			workingCopy.reconcile(AST.JLS3, false/*no pb detection*/, null/*no owner*/, null/*no progress*/);
		}
	
		// Measures
		resetCounters();
		for (int i=0; i<MEASURES_COUNT; i++) {
			workingCopy.getBuffer().setContents(bigContents.append("a").toString());
			runGc();
			startMeasuring();
			workingCopy.reconcile(AST.JLS3, false/*no pb detection*/, null/*no owner*/, null/*no progress*/);
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();		
		
	} finally {
		if (workingCopy != null)
			workingCopy.discardWorkingCopy();
	}
}

/**
 * Ensures that the reconciler does nothing when the source
 * to reconcile with is the same as the current contents.
 */
public void testPerfSearchAllTypeNamesAndReconcile() throws CoreException {
	tagAsSummary("Reconcile editor change and complete", true); // put in fingerprint

	// Wait for indexing end
	AbstractJavaModelTests.waitUntilIndexesReady();

	// Warm up
	ICompilationUnit workingCopy = null;
	try {
		final ProblemRequestor requestor = new ProblemRequestor();
		WorkingCopyOwner owner = new WorkingCopyOwner() {
			public IProblemRequestor getProblemRequestor(ICompilationUnit cu) {
				return requestor;
            }
		};
		workingCopy = PARSER_WORKING_COPY.getWorkingCopy(owner, null);
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { JDT_CORE_PROJECT });
		int warmup = WARMUP_COUNT / 5;
		for (int i=0; i<warmup; i++) {
			searchAllTypeNames(scope);
			CompilationUnit unit = workingCopy.reconcile(AST.JLS3, true, null, null);
			if (i == 0) {
				assertNotNull("Compilation Unit should not be null!", unit);
				assertNotNull("Bindings were not resolved!", unit.getPackage().resolveBinding());
			}
		}

		// Measures
		int iterations = 2;
		resetCounters();
		for (int i=0; i<MEASURES_COUNT; i++) {
			runGc();
			startMeasuring();
			for (int n=0; n<iterations; n++) {
				searchAllTypeNames(scope);
				workingCopy.reconcile(AST.JLS3, true, null, null);
			}
			stopMeasuring();
		}
	}
	finally {
		workingCopy.discardWorkingCopy();
	}
	
	// Commit
	commitMeasurements();
	assertPerformance();

}

/*
 * Performance test for looking up package fragments
 * (see bug 72683 Slow code assist in Display view)
 */
public void testSeekPackageFragments() throws CoreException {
	assertNotNull("We should have the 'BigProject' in workspace!", BIG_PROJECT);
	setComment(Performance.EXPLAINS_DEGRADATION_COMMENT, "Test has been rewritten and is not stabilized yet...");
	class PackageRequestor implements IJavaElementRequestor {
		ArrayList pkgs = new ArrayList();
		public void acceptField(IField field) {}
		public void acceptInitializer(IInitializer initializer) {}
		public void acceptMemberType(IType type) {}
		public void acceptMethod(IMethod method) {}
		public void acceptPackageFragment(IPackageFragment packageFragment) {
			if (pkgs != null)
				pkgs.add(packageFragment);
		}
		public void acceptType(IType type) {}
		public boolean isCanceled() {
			return false;
		}
	}
	
	// first pass: ensure all class are loaded, and ensure that the test works as expected
	PackageRequestor requestor = new PackageRequestor();
	for (int i=0; i<WARMUP_COUNT; i++) {
		getNameLookup(BIG_PROJECT).seekPackageFragments("org.eclipse.jdt.core.tests78.performance5", false/*not partial match*/, requestor);
		if (i == 0) {
			int size = requestor.pkgs.size();
			IJavaElement[] result = new IJavaElement[size];
			requestor.pkgs.toArray(result);
			assertElementsEqual(
				"Unexpected packages",
				"org.eclipse.jdt.core.tests78.performance5 [in src78 [in "+BIG_PROJECT_NAME+"]]",
				result
			);
		}
	}
	
	// measure performance
	requestor.pkgs = null;
	resetCounters();
	for (int i = 0; i < MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int j = 0; j < 50000; j++) {
			getNameLookup(BIG_PROJECT).seekPackageFragments("org.eclipse.jdt.core.tests" + i + "0.performance" + i, false/*not partial match*/, requestor);
		}
		stopMeasuring();
	}
	commitMeasurements();
	assertPerformance();
}

public void testCloseProjects() throws JavaModelException {
	tagAsSummary("Close all workspace projects", true); // put in fingerprint

	// Warm-up
	int length=ALL_PROJECTS.length;
	int wmax = WARMUP_COUNT / 10;
	for (int i=0; i<wmax; i++) {
		for (int j=0; j<length; j++) {
			ENV.closeProject(ALL_PROJECTS[j].getPath());
		}
		for (int j=0; j<length; j++) {
			ENV.openProject(ALL_PROJECTS[j].getPath());
		}
	}

	// Measures
	for (int i=0; i<MEASURES_COUNT; i++) {
		AbstractJavaModelTests.waitUntilIndexesReady();
		runGc();
		startMeasuring();
		for (int j=0; j<length; j++) {
			ENV.closeProject(ALL_PROJECTS[j].getPath());
		}
		stopMeasuring();
		for (int j=0; j<length; j++) {
			ENV.openProject(ALL_PROJECTS[j].getPath());
		}
	}

	// Commit
	commitMeasurements();
	assertPerformance();
}

/*
 * Tests the performance of JavaCore#create(IResource).
 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133141)
 */
public void testCreateJavaElement() throws CoreException {
	// setup (force the project cache to be created)
	IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(BIG_PROJECT_TYPE_PATH);
	getNameLookup(BIG_PROJECT);
	
	// warm up
	int warmup = WARMUP_COUNT / 10;
	int iterations = 5000;
	for (int i = 0; i < warmup; i++) {
		for (int j = 0; j < iterations; j++) {
			JavaCore.create(file);
		}
	}
		
	// measure performance
	for (int i = 0; i < MEASURES_COUNT; i++) {
		runGc();
		startMeasuring();
		for (int j = 0; j < iterations; j++) {
			JavaCore.create(file);
		}
		stopMeasuring();
	}

	commitMeasurements();
	assertPerformance();
}

public void testInitJDTPlugin() throws JavaModelException, CoreException {
	tagAsSummary("JDT/Core plugin initialization", true); // put in fingerprint

	// Warm-up
	int wmax = WARMUP_COUNT / 5;
	for (int i=0; i<wmax; i++) {
		simulateExitRestart();
		JavaCore.initializeAfterLoad(null);
		AbstractJavaModelTests.waitUntilIndexesReady();
	}

	// Measures
	for (int i=0; i<MEASURES_COUNT; i++) {
		// shutdwon
		simulateExit();			
		runGc();
		startMeasuring();
		// restart
		simulateRestart();
		JavaCore.initializeAfterLoad(null);
		AbstractJavaModelTests.waitUntilIndexesReady();
		stopMeasuring();
	}
	// Commit
	commitMeasurements();
	assertPerformance();
}

/*
 * Performance test for the first use of findType(...)
 * (see bug 161175 JarPackageFragmentRoot slow to initialize)
 */
public void testFindType() throws CoreException {
	
	IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
	IJavaProject[] existingProjects = model.getJavaProjects();
	
	try {
		// close existing projects
		for (int i = 0, length = existingProjects.length; i < length; i++) {
			existingProjects[i].getProject().close(null);
		}
	
		// get 20 projects
		int max = 20;
		IJavaProject[] projects = new IJavaProject[max];
		for (int i = 0; i < max; i++) {
			projects[i] = createJavaProject("FindType" + i);
		}
		AbstractJavaModelTests.waitUntilIndexesReady();
		AbstractJavaModelTests.waitForAutoBuild();
		
		try {
			// warm up
			int warmup = WARMUP_COUNT / 10;
			for (int i = 0; i < warmup; i++) {
				model.close();
				for (int j = 0; j < max; j++) {
					projects[j].findType("java.lang.Object");
				}
			}
				
			// measure performance
			for (int i = 0; i < MEASURES_COUNT; i++) {
				model.close();
				runGc();
				startMeasuring();
				for (int j = 0; j < max; j++) {
					projects[j].findType("java.lang.Object");
				}
				stopMeasuring();
			}
		
			commitMeasurements();
			assertPerformance();
		} finally {
			for (int i = 0; i < max; i++) {
				projects[i].getProject().delete(false, null);
			}
		}
	} finally {
		// reopen existing projects
		for (int i = 0, length = existingProjects.length; i < length; i++) {
			existingProjects[i].getProject().open(null);
		}
	}
}

protected void resetCounters() {
	// do nothing
}
}
