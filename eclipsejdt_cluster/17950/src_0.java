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
package org.eclipse.jdt.core.tests.compiler.regression;

import java.util.ArrayList;

import org.eclipse.jdt.core.tests.util.AbstractCompilerTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Run all compiler regression tests
 */
public class TestAll extends TestCase {

public TestAll(String testName) {
	super(testName);
}
public static Test suite() {
	ArrayList standardTests = new ArrayList();
//	standardTests.addAll(JavadocTest.allTestClasses);
	standardTests.add(ArrayTest.class);
	standardTests.add(AssignmentTest.class);
	standardTests.add(BatchCompilerTest.class);
	standardTests.add(BooleanTest.class);
	standardTests.add(CastTest.class);
	standardTests.add(ClassFileComparatorTest.class);
	standardTests.add(ClassFileReaderTest.class);
	standardTests.add(CollisionCase.class);
	standardTests.add(ConstantTest.class);
	standardTests.add(DeprecatedTest.class);
	standardTests.add(LocalVariableTest.class);
	standardTests.add(LookupTest.class);
	standardTests.add(NumericTest.class);
	standardTests.add(ProblemConstructorTest.class);
	standardTests.add(ScannerTest.class);
	standardTests.add(SwitchTest.class);
	standardTests.add(TryStatementTest.class);
	standardTests.add(UtilTest.class);
	standardTests.add(XLargeTest.class);
	standardTests.add(InternalScannerTest.class);
	// add all javadoc tests
	for (int i=0, l=JavadocTest.allTestClasses.size(); i<l; i++) {
		standardTests.add(JavadocTest.allTestClasses.get(i));
	}

	TestSuite all = new TestSuite(TestAll.class.getName());
	int possibleComplianceLevels = AbstractCompilerTest.getPossibleComplianceLevels();
	if ((possibleComplianceLevels & AbstractCompilerTest.F_1_3) != 0) {
	    ArrayList tests_1_3 = (ArrayList)standardTests.clone();
		tests_1_3.add(Compliance_1_3.class);
		// Reset forgotten subsets tests
		AbstractRegressionTest.testsNames = null;
		AbstractRegressionTest.testsNumbers= null;
		AbstractRegressionTest.testsRange = null;
		all.addTest(AbstractCompilerTest.suiteForComplianceLevel(AbstractCompilerTest.COMPLIANCE_1_3, RegressionTestSetup.class, tests_1_3));
	}
	if ((possibleComplianceLevels & AbstractCompilerTest.F_1_4) != 0) {
	    ArrayList tests_1_4 = (ArrayList)standardTests.clone();
		tests_1_4.add(AssertionTest.class);
		tests_1_4.add(Compliance_1_4.class);
		tests_1_4.add(JavadocTest_1_4.class);
		// Reset forgotten subsets tests
		AbstractRegressionTest.testsNames = null;
		AbstractRegressionTest.testsNumbers= null;
		AbstractRegressionTest.testsRange = null;
		all.addTest(AbstractCompilerTest.suiteForComplianceLevel(AbstractCompilerTest.COMPLIANCE_1_4, RegressionTestSetup.class, tests_1_4));
	}
	if ((possibleComplianceLevels & AbstractCompilerTest.F_1_5) != 0) {
	    ArrayList tests_1_5 = (ArrayList)standardTests.clone();
		tests_1_5.add(AssertionTest.class);
		tests_1_5.add(Compliance_1_5.class);	    
		tests_1_5.add(JavadocTest_1_5.class);
	    tests_1_5.add(GenericTypeTest.class);
	    tests_1_5.add(ForeachStatementTest.class);
	    tests_1_5.add(GenericTypeSignatureTest.class);
	    tests_1_5.add(InternalHexFloatTest.class);
	    tests_1_5.add(StaticImportTest.class);
	    tests_1_5.add(VarargTest.class);
	    tests_1_5.add(EnumTest.class);
		// Reset forgotten subsets tests
		AbstractRegressionTest.testsNames = null;
		AbstractRegressionTest.testsNumbers= null;
		AbstractRegressionTest.testsRange = null;
		all.addTest(AbstractCompilerTest.suiteForComplianceLevel(AbstractCompilerTest.COMPLIANCE_1_5, RegressionTestSetup.class, tests_1_5));
	}
//	// Add Javadoc test suites
//	all.addTest(JavadocTest.suite());
	return all;
}
}
