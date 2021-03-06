/*******************************************************************************
 * Copyright (c) 2002 IBM Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.core.tests.model;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Run all java model tests.
 */
public class AllJavaModelTests extends TestCase {
public AllJavaModelTests(String name) {
	super(name);
}
public static Test suite() {
	TestSuite suite = new TestSuite(AllJavaModelTests.class.getName());
	// Enter each test here, grouping the tests that are related

	// Java Naming convention tests
	suite.addTest(JavaConventionTests.suite());

	// Project & Root API unit tests
	suite.addTest(JavaProjectTests.suite());

	// Source attachment tests
	suite.addTest(AttachSourceTests.suite());
	
	//Create type source tests
	suite.addTest(CreateTypeSourceExamplesTests.suite());

	//Create method source tests
	suite.addTest(CreateMethodSourceExamplesTests.suite());
		
	// Java search tests
	suite.addTest(JavaSearchTests.suite());
	suite.addTest(WorkingCopySearchTests.suite());
		
	// Working copy tests
	suite.addTest(WorkingCopyTests.suite());
	suite.addTest(WorkingCopyNotInClasspathTests.suite());
	suite.addTest(HierarchyOnWorkingCopiesTests.suite());
	
	// test IJavaModel
	suite.addTest(JavaModelTests.suite());

	// tests to check the encoding
	suite.addTest(EncodingTests.suite());
	
	// test class name with special names like names containing '$'
	suite.addTest(ClassNameTests.suite());
	
	// IBuffer tests
	suite.addTest(BufferTests.suite());

	// Name lookup tests
	suite.addTest(NameLookupTests2.suite());

	// Classpath and output location tests
	suite.addTest(ClasspathTests.suite());

	// Delta tests
	suite.addTest(JavaElementDeltaTests.suite());
	suite.addTest(ExternalJarDeltaTests.suite());

	// Java element existence tests
	suite.addTest(ExistenceTests.suite());
	
	// Support for "open on" feature tests
	suite.addTest(ResolveTests.suite());
		
	return suite;
}

}
