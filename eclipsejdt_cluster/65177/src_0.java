/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.performance;

import java.io.PrintStream;
import java.text.NumberFormat;

import junit.framework.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.core.tests.model.AbstractJavaModelTests;
import org.eclipse.jdt.internal.core.search.processing.IJob;
import org.eclipse.test.performance.Performance;

/**
 */
public class FullSourceWorkspaceSearchTests extends FullSourceWorkspaceTests implements IJavaSearchConstants {

	// Tests counters
	private static int TESTS_COUNT = 0;
	private final static int ITERATIONS_COUNT = 10;

	// Log file streams
	private static PrintStream[] LOG_STREAMS = new PrintStream[LOG_TYPES.length];

	/**
	 * @param name
	 */
	public FullSourceWorkspaceSearchTests(String name) {
		super(name);
	}

	static {
//		org.eclipse.jdt.internal.core.search.processing.JobManager.VERBOSE = true;
//		TESTS_NAMES = new String[] {
//			"testIndexing",
//			"testIndexingOneProject",
//			"testSearchAllTypeNames",
//			"testNewSearchAllTypeNames",
//			"testSearchAllTypeNameMatches",
//		};
	}
	/*
	 * Specific way to build test suite.
	 * We need to know whether test perf indexing is in list to allow
	 * index manager disabling.
	 * CAUTION: If test perf indexing is not included in test suite,
	 * then time for other tests may include time spent to index files!
	 */
	public static Test suite() {
		Test suite = buildSuite(testClass());
		TESTS_COUNT = suite.countTestCases();
		createPrintStream(testClass(), LOG_STREAMS, TESTS_COUNT, null);
		return suite;
	}

	private static Class testClass() {
		return FullSourceWorkspaceSearchTests.class;
	}

	protected void setUp() throws Exception {
		super.setUp();
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
	static int SEARCH_ALL_TYPE_NAMES_COUNT = -1;
	/**
	 * Simple type name requestor: only count classes and interfaces.
	 * @deprecated
	 */
	class OldSearchTypeNameRequestor implements ITypeNameRequestor {
		int count = 0;
		public void acceptClass(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path){
			this.count++;
		}
		public void acceptInterface(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path){
			this.count++;
		}
	}
	/**
	 * Simple type name requestor: only count classes and interfaces.
	 */
	class SearchTypeNameRequestor extends TypeNameRequestor {
		int count = 0;
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
			this.count++;
		}
	}
	/**
	 * Simple type name requestor: only count classes and interfaces.
	 */
	class SearchTypeNameMatchRequestor extends TypeNameMatchRequestor {
		int count = 0;
		public void acceptTypeNameMatch(TypeNameMatch match) {
			this.count++;
		}
	}
	/**
	 * Job to measure times in same thread than index manager.
	 */
	class	 Measuring implements IJob {
		boolean start;
		Measuring(boolean start) {
			this.start = start;
		}
		public boolean belongsTo(String jobFamily) {
			return true;
		}
		public void cancel() {
			// nothing to cancel
		}
		public void ensureReadyToRun() {
		}
		/**
		 * Execute the current job, answer whether it was successful.
		 */
		public boolean execute(IProgressMonitor progress) {
			if (start) {
				startMeasuring();
			} else {
				stopMeasuring();
			}
			return true;
		}
	}
	
	protected void search(String patternString, int searchFor, int limitTo, IJavaSearchScope scope, JavaSearchResultCollector resultCollector) throws CoreException {
		int matchMode = patternString.indexOf('*') != -1 || patternString.indexOf('?') != -1
			? SearchPattern.R_PATTERN_MATCH
			: SearchPattern.R_EXACT_MATCH;
		SearchPattern pattern = SearchPattern.createPattern(
			patternString, 
			searchFor,
			limitTo, 
			matchMode | SearchPattern.R_CASE_SENSITIVE);
		new SearchEngine().search(
			pattern,
			new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
			scope,
			resultCollector,
			null);
	}

	/**
	 * Clean last category table cache
	 * @param type Tells whether previous search was a type search or not
	 * @param scope TODO
	 */
	protected void cleanCategoryTableCache(boolean type, IJavaSearchScope scope, JavaSearchResultCollector resultCollector) throws CoreException {
		long time = System.currentTimeMillis();
		if (type) {
			search("foo", FIELD, DECLARATIONS, scope, resultCollector);
		} else {
			search("Foo", TYPE, DECLARATIONS, scope, resultCollector);
		}
		if (DEBUG) System.out.println("Time to clean category table cache: "+(System.currentTimeMillis()-time));
	}

	/**
	 * Performance tests for search: Indexing entire workspace
	 * 
	 * First wait that already started indexing jobs ends before performing test and measure.
	 * Consider this initial indexing jobs as warm-up for this test.
	 * 
	 * TODO (frederic) After 3.3, activate several iteration for this test to have more accurate results,
	 * 	then rename the test as numbers will be different...
	 */
	public void testIndexing() throws CoreException {
		tagAsSummary("Search indexes building", true); // put in fingerprint

		// Wait for indexing end (we use initial indexing as warm-up)
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Measures
		int measures = false ? MEASURES_COUNT/2 : 1;
		for (int i=0; i<measures; i++) {

			// Remove project previous indexing
			INDEX_MANAGER.removeIndexFamily(new Path(""));
			INDEX_MANAGER.reset();

			// Clean memory
			runGc();

			// Restart brand new indexing
			INDEX_MANAGER.request(new Measuring(true/*start measuring*/));
			for (int j=0, length=ALL_PROJECTS.length; j<length; j++) {
				INDEX_MANAGER.indexAll(ALL_PROJECTS[j].getProject());
			}
			AbstractJavaModelTests.waitUntilIndexesReady();

			// end measure
			INDEX_MANAGER.request(new Measuring(false /*end measuring*/));
			AbstractJavaModelTests.waitUntilIndexesReady();
		}

		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Indexing one project (JDT/Core).
	 */
	public void testIndexingOneProject() throws CoreException {
		tagAsSummary("Search JDT/Core indexes building", true); // put in fingerprint

		// Warm-up
		INDEX_MANAGER.removeIndexFamily(JDT_CORE_PROJECT.getPath());
		INDEX_MANAGER.indexAll(JDT_CORE_PROJECT.getProject());
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Measures
		int max = MEASURES_COUNT * 10;
		for (int i=0; i<max; i++) {
			runGc();
			INDEX_MANAGER.removeIndexFamily(JDT_CORE_PROJECT.getPath());
			INDEX_MANAGER.request(new Measuring(true/*start measuring*/));
			INDEX_MANAGER.indexAll(JDT_CORE_PROJECT.getProject());
			AbstractJavaModelTests.waitUntilIndexesReady();
			INDEX_MANAGER.request(new Measuring(false /*end measuring*/));
			AbstractJavaModelTests.waitUntilIndexesReady();
		}

		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Search All Types Names using 2.1 API.
	 * 
	 * @deprecated As we use deprecated API
	 */
	public void testSearchAllTypeNames() throws CoreException {
		tagAsGlobalSummary("Old Search all type names", false); // do NOT put in global fingerprint
		OldSearchTypeNameRequestor requestor = new OldSearchTypeNameRequestor();

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
		new SearchEngine().searchAllTypeNames(
			null,
			null,
			SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
			IJavaSearchConstants.TYPE,
			scope, 
			requestor,
			WAIT_UNTIL_READY_TO_SEARCH,
			null);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	All type names = "+intFormat.format(requestor.count));
		if (SEARCH_ALL_TYPE_NAMES_COUNT == -1) {
			SEARCH_ALL_TYPE_NAMES_COUNT = requestor.count;
		} else {
			assertEquals("We should find same number of types in the workspace whatever the search method is!", SEARCH_ALL_TYPE_NAMES_COUNT, requestor.count);
		}

		// Measures
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		for (int i=0; i<MEASURES_COUNT; i++) {
			cleanCategoryTableCache(true, scope, resultCollector);
			runGc();
			startMeasuring();
			for (int j=0; j<ITERATIONS_COUNT; j++) {
				new SearchEngine().searchAllTypeNames(
					null,
					null,
					SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
					IJavaSearchConstants.TYPE,
					scope, 
					requestor,
					WAIT_UNTIL_READY_TO_SEARCH,
					null);
			}
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Search All Types Names.
	 */
	public void testNewSearchAllTypeNames() throws CoreException {
		// TODO (frederic) put this test in global summary when be sure of its number.
		//tagAsGlobalSummary("Search all type names", true); // put in global fingerprint
		tagAsSummary("Search all type names", false);
		SearchTypeNameRequestor requestor = new SearchTypeNameRequestor();

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
		new SearchEngine().searchAllTypeNames(
			null,
			SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
			null,
			SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
			IJavaSearchConstants.TYPE,
			scope, 
			requestor,
			WAIT_UNTIL_READY_TO_SEARCH,
			null);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	All type names = "+intFormat.format(requestor.count));
		if (SEARCH_ALL_TYPE_NAMES_COUNT == -1) {
			SEARCH_ALL_TYPE_NAMES_COUNT = requestor.count;
		} else {
			assertEquals("We should find same number of types in the workspace whatever the search method is!", SEARCH_ALL_TYPE_NAMES_COUNT, requestor.count);
		}

		// Measures
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		for (int i=0; i<MEASURES_COUNT; i++) {
			cleanCategoryTableCache(true, scope, resultCollector);
			runGc();
			startMeasuring();
			new SearchEngine().searchAllTypeNames(
				null,
				SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
				null,
				SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
				IJavaSearchConstants.TYPE,
				scope, 
				requestor,
				WAIT_UNTIL_READY_TO_SEARCH,
				null);
			stopMeasuring();
		}

		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Search All Types Names Matches.
	 * 
	 * This test use a collector which accepts IType.
	 */
	public void testSearchAllTypeNameMatches() throws CoreException {
		tagAsSummary("Search all type name matches", false); // do NOT put in fingerprint
		SearchTypeNameMatchRequestor requestor = new SearchTypeNameMatchRequestor();

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
		new SearchEngine().searchAllTypeNames(
			null,
			SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
			null,
			SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
			IJavaSearchConstants.TYPE,
			scope, 
			requestor,
			WAIT_UNTIL_READY_TO_SEARCH,
			null);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	All type names = "+intFormat.format(requestor.count));
		if (SEARCH_ALL_TYPE_NAMES_COUNT == -1) {
			SEARCH_ALL_TYPE_NAMES_COUNT = requestor.count;
		} else {
			assertEquals("We should find same number of types in the workspace whatever the search method is!", SEARCH_ALL_TYPE_NAMES_COUNT, requestor.count);
		}

		// Measures
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		for (int i=0; i<MEASURES_COUNT; i++) {
			cleanCategoryTableCache(true, scope, resultCollector);
			runGc();
			startMeasuring();
			new SearchEngine().searchAllTypeNames(
				null,
				SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
				null,
				SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
				IJavaSearchConstants.TYPE,
				scope, 
				requestor,
				WAIT_UNTIL_READY_TO_SEARCH,
				null);
			stopMeasuring();
		}

		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search:  Types occurrences.
	 * 
	 * Note that following search have been tested:
	 *		- "String":				> 65000 macthes (CAUTION: needs -Xmx512M)
	 *		- "Object":			13497 matches
	 *		- ""IResource":	5886 macthes
	 *		- "JavaCore":		2145 matches
	 */
	public void testSearchType() throws CoreException {
		tagAsSummary("Search type occurences", true); // put in fingerprint

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { JDT_CORE_PROJECT }, IJavaSearchScope.SOURCES);
		String name = "JavaCore";
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		search(name, TYPE, ALL_OCCURRENCES, scope, resultCollector);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	- "+intFormat.format(resultCollector.count)+" occurences for type '"+name+"' in project "+JDT_CORE_PROJECT.getElementName());

		// Measures
		for (int i=0; i<MEASURES_COUNT; i++) {
			cleanCategoryTableCache(true, scope, resultCollector);
			runGc();
			startMeasuring();
			search(name, TYPE, ALL_OCCURRENCES, scope, resultCollector);
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Fields occurrences.
	 */
	public void testSearchField() throws CoreException {
		tagAsSummary("Search field occurences", true); // put in fingerprint

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { JDT_CORE_PROJECT }, IJavaSearchScope.SOURCES);
		String name = "TYPE";
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		search(name, FIELD, ALL_OCCURRENCES, scope, resultCollector);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	- "+intFormat.format(resultCollector.count)+" occurences for field '"+name+"' in project "+JDT_CORE_PROJECT.getElementName());

		// Measures
		for (int i=0; i<MEASURES_COUNT; i++) {
			cleanCategoryTableCache(false, scope, resultCollector);
			runGc();
			startMeasuring();
			search(name, FIELD, ALL_OCCURRENCES, scope, resultCollector);
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Methods occurrences.
	 * This search do NOT use binding resolution.
	 */
	public void testSearchMethod() throws CoreException {
		tagAsSummary("Search method occurences (no resolution)", false); // do NOT put in fingerprint
		setComment(Performance.EXPLAINS_DEGRADATION_COMMENT, "Test is not enough stable and will be replaced by another one...");
	
		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();
	
		// Warm up
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { JDT_CORE_PROJECT }, IJavaSearchScope.SOURCES);
		String name = "equals";
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		search(name, METHOD, ALL_OCCURRENCES, scope, resultCollector);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	- "+intFormat.format(resultCollector.count)+" occurences for method '"+name+"' in project "+JDT_CORE_PROJECT.getElementName());
	
		// Measures
		for (int i=0; i<MEASURES_COUNT; i++) {
			// clean before test
			cleanCategoryTableCache(false, scope, resultCollector);
			runGc();
	
			// test
			startMeasuring();
			search(name, METHOD, ALL_OCCURRENCES, scope, resultCollector);
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Methods occurrences.
	 * This search use binding resolution.
	 */
	public void testSearchBinaryMethod() throws CoreException {
		tagAsSummary("Search method occurences", true); // put in fingerprint

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { JDT_CORE_PROJECT }, IJavaSearchScope.SOURCES);
		String name = "java.lang.Object.hashCode()";
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		search(name, METHOD, ALL_OCCURRENCES, scope, resultCollector);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	- "+intFormat.format(resultCollector.count)+" occurences for method '"+name+"' in project "+JDT_CORE_PROJECT.getElementName());

		// Measures
		for (int i=0; i<MEASURES_COUNT; i++) {
			// clean before test
			cleanCategoryTableCache(false, scope, resultCollector);
			runGc();

			// test
			startMeasuring();
			search(name, METHOD, ALL_OCCURRENCES, scope, resultCollector);
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Constructors occruences.
	 */
	public void testSearchConstructor() throws CoreException {
		tagAsSummary("Search constructor occurences", true); // put in fingerprint

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { JDT_CORE_PROJECT }, IJavaSearchScope.SOURCES);
		String name = "String";
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		search(name, CONSTRUCTOR, ALL_OCCURRENCES, scope, resultCollector);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	- "+intFormat.format(resultCollector.count)+" occurences for constructor '"+name+"' in project "+JDT_CORE_PROJECT.getElementName());

		// Measures
		for (int i=0; i<MEASURES_COUNT; i++) {
			cleanCategoryTableCache(false, scope, resultCollector);
			runGc();
			startMeasuring();
			search(name, CONSTRUCTOR, ALL_OCCURRENCES, scope, resultCollector);
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance tests for search: Package Declarations.
	 */
	public void testSearchPackageDeclarations() throws CoreException {
		tagAsSummary("Search package declarations", true); // put in fingerprint

		// Wait for indexing end
		AbstractJavaModelTests.waitUntilIndexesReady();

		// Warm up
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { JDT_CORE_PROJECT }, IJavaSearchScope.SOURCES);
		String name = "*";
		JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
		search(name, PACKAGE, DECLARATIONS, scope, resultCollector);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("	- "+intFormat.format(resultCollector.count)+" package declarations in project "+JDT_CORE_PROJECT.getElementName());

		// Measures
		for (int i=0; i<MEASURES_COUNT; i++) {
			cleanCategoryTableCache(false, scope, resultCollector);
			runGc();
			startMeasuring();
			search(name, PACKAGE, DECLARATIONS, scope, resultCollector);
			stopMeasuring();
		}
		
		// Commit
		commitMeasurements();
		assertPerformance();
	}
}
