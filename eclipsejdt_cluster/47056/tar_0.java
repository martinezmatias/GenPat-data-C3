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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.*;

import junit.framework.Test;

public class CompilationUnitTests extends ModifyingResourceTests {
	ICompilationUnit cu;
	
public CompilationUnitTests(String name) {
	super(name);
}
public void setUpSuite() throws Exception {
	super.setUpSuite();
	
	createJavaProject("P", new String[] {"src"}, new String[] {getExternalJCLPathString()}, "bin", "1.5");
	createFolder("/P/src/p");
	createFile(
		"/P/src/p/X.java",
		"/* some comment */" +
		"package p;\n" +
		"import p2.*;\n" +
		"import p3.Z;\n" +
		"public class X implements Runnable {\n" +
		"  public int f1;\n" +
		"  /** @deprecated\n */" +
		"  protected Object f2;\n" +
		"  private X f3;\n" +
		"  java.lang.String f4;\n" +
		"  int f5, f6, f7;\n" +
		"  public class Inner {\n" +
		"    class InnerInner {\n" +
		"    }\n" +
		"  }\n" +
		"  public void foo(Y y) throws IOException {\n" +
		"  }\n" +
		"  protected static Object bar() {\n" +
		"  }\n" +
		"  /** @deprecated\n */" +
		"  private int fred() {\n" +
		"  }\n" +
		"  void testIsVarArgs(String s, Object ... args) {\n" +
		"  }\n" +
		"}\n" +
		"/** @deprecated\n */" +
		"interface I {\n" +
		"  int run();\n" +
		"}");
	this.cu = getCompilationUnit("/P/src/p/X.java");
}

// Use this static initializer to specify subset for tests
// All specified tests which do not belong to the class are skipped...
static {
	// Names of tests to run: can be "testBugXXXX" or "BugXXXX")
//	testsNames = new String[] { "Bug73884" };
	// Numbers of tests to run: "test<number>" will be run for each number of this array
//	testsNumbers = new int[] { 13 };
	// Range numbers of tests to run: all tests between "test<first>" and "test<last>" will be run for { first, last }
//	testsRange = new int[] { 16, -1 };
}
public static Test suite() {
	return buildTestSuite(CompilationUnitTests.class);
}
public void tearDownSuite() throws Exception {
	this.deleteProject("P");
	super.tearDownSuite();
}
/**
 * Calls methods that do nothing to ensure code coverage
 */
public void testCodeCoverage() throws JavaModelException {
	this.cu.discardWorkingCopy();
	this.cu.restore();
}
/**
 * Ensures <code>commitWorkingCopy(boolean, IProgressMonitor)</code> throws the correct 
 * <code>JavaModelException</code> for a <code>CompilationUnit</code>.
 */
public void testCommitWorkingCopy() {
	try {
		this.cu.commitWorkingCopy(false, null);
	} catch (JavaModelException jme) {
		assertTrue("Incorrect status for committing a CompilationUnit", jme.getStatus().getCode() == IJavaModelStatusConstants.INVALID_ELEMENT_TYPES);
		return;
	}
	assertTrue("A compilation unit should throw an exception is a commit is attempted", false);
}
/*
 * Ensure that the deprecated flags is correctly reported
 * (regression test fo bug 23207 Flags.isDeprecated(IMethod.getFlags()) doesn't work)
 */
public void testDeprecatedFlag() throws JavaModelException {
	IType type = this.cu.getType("X");
	assertTrue("Type X should not be deprecated", !Flags.isDeprecated(type.getFlags()));
	assertTrue("Type I should be deprecated", Flags.isDeprecated(this.cu.getType("I").getFlags()));
	
	assertTrue("Field f1 should not be deprecated", !Flags.isDeprecated(type.getField("f1").getFlags()));
	assertTrue("Field f2 should be deprecated", Flags.isDeprecated(type.getField("f2").getFlags()));
	
	assertTrue("Method bar should not be deprecated", !Flags.isDeprecated(type.getMethod("bar", new String[]{}).getFlags()));
	assertTrue("Method fred should be deprecated", Flags.isDeprecated(type.getMethod("fred", new String[]{}).getFlags()));
}
/**
 * Ensures <code>getContents()</code> returns the correct value
 * for a <code>CompilationUnit</code> that is not present
 */
public void testGetContentsForNotPresent() {
	CompilationUnit compilationUnit = (CompilationUnit)getCompilationUnit("/P/src/p/Absent.java");
	
	assertSourceEquals("Unexpected contents for non present cu", "", new String(compilationUnit.getContents()));
}
/**
 * Tests Java element retrieval via source position 
 */
public void testGetElementAt() throws JavaModelException {
	IType type = this.cu.getType("X");
	ISourceRange sourceRange= type.getSourceRange();
	//ensure that we are into the body of the type
	IJavaElement element= 
		this.cu.getElementAt(sourceRange.getOffset() + type.getElementName().length() + 1);
	assertTrue("Should have found a type", element instanceof IType);
	assertEquals(
		"Should have found X",
		"X",
		element.getElementName());
	//ensure that null is returned if there is no element other than the compilation
 	//unit itself at the given position
 	element= this.cu.getElementAt(this.cu.getSourceRange().getOffset() + 1);
 	assertEquals("Should have not found any element", null, element);
}
/**
 * Tests import declararion retrieval via source position.
 * (regression test for bug 14331 ICompilationUnit.getElementAt dos not find import decl)
 */
public void testGetElementAt2() throws JavaModelException {
	IImportContainer container = this.cu.getImportContainer();
	ISourceRange sourceRange= container.getSourceRange();
	//ensure that we are inside the import container
	IJavaElement element= this.cu.getElementAt(sourceRange.getOffset() + 1);
	assertTrue("Should have found an import", element instanceof IImportDeclaration);
	assertEquals(
		"Import not found",
		"p2.*",
		 element.getElementName());
}
/*
 * Ensures that the right field is returnd in a muti-declaration field.
 */
public void testGetElementAt3() throws JavaModelException {
	int fieldPos = this.cu.getSource().indexOf("f5");
	IJavaElement element= this.cu.getElementAt(fieldPos);
	assertEquals(
		"Unexpected field found",
		this.cu.getType("X").getField("f5"),
		 element);
}
/*
 * Ensures that the right field is returnd in a muti-declaration field.
 */
public void testGetElementAt4() throws JavaModelException {
	int fieldPos = this.cu.getSource().indexOf("f6");
	IJavaElement element= this.cu.getElementAt(fieldPos);
	assertEquals(
		"Unexpected field found",
		this.cu.getType("X").getField("f6"),
		 element);
}
/*
 * Ensures that the right field is returnd in a muti-declaration field.
 */
public void testGetElementAt5() throws JavaModelException {
	int fieldPos = this.cu.getSource().indexOf("f7");
	IJavaElement element= this.cu.getElementAt(fieldPos);
	assertEquals(
		"Unexpected field found",
		this.cu.getType("X").getField("f7"),
		 element);
}
/*
 * Ensures that the right field is returnd in a muti-declaration field.
 */
public void testGetElementAt6() throws JavaModelException {
	int fieldPos = this.cu.getSource().indexOf("int f5");
	IJavaElement element= this.cu.getElementAt(fieldPos);
	assertEquals(
		"Unexpected field found",
		this.cu.getType("X").getField("f5"),
		 element);
}
/**
 * Ensures that correct number of fields with the correct names, modifiers, signatures
 * and declaring types exist in a type.
 */
public void testGetFields() throws JavaModelException {
	IType type = this.cu.getType("X");
	IField[] fields= type.getFields();
	String[] fieldNames = new String[] {"f1", "f2", "f3", "f4", "f5", "f6", "f7"};
	String[] flags = new String[] {"public", "protected", "private", "", "", "", ""};
	String[] signatures = new String[] {"I", "QObject;", "QX;", "Qjava.lang.String;", "I", "I", "I"};
	assertEquals("Wrong number of fields returned",  fieldNames.length, fields.length);
	for (int i = 0; i < fields.length; i++) {
		assertEquals("Incorrect name for the " + i + " field", fieldNames[i], fields[i].getElementName());
		String mod= Flags.toString(fields[i].getFlags());
		assertEquals("Unexpected modifier for " + fields[i].getElementName(), flags[i], mod);
		assertEquals("Unexpected type signature for " + fields[i].getElementName(), signatures[i], fields[i].getTypeSignature());
		assertEquals("Unexpected declaring type for " + fields[i].getElementName(), type, fields[i].getDeclaringType());
		assertTrue("Field should exist " + fields[i], fields[i].exists());
	}
}
/**
 * Ensure that import declaration handles are returned from the
 * compilation unit.
 * Checks non-existant handle, on demand and not.
 */
public void testGetImport() {
	IImportDeclaration imprt = this.cu.getImport("java.lang");
	assertTrue("Import should not exist " + imprt, !imprt.exists());
	
	imprt = this.cu.getImport("p2.*");
	assertTrue("Import should exist " + imprt, imprt.exists());
	
	imprt = this.cu.getImport("p3.Z");
	assertTrue("Import should exist " + imprt, imprt.exists());
}
/**
 * Ensures that correct number of imports with the correct names
 * exist in "GraphicsTest" compilation unit.
 */
public void testGetImports() throws JavaModelException {
	IImportDeclaration[] imprts = this.cu.getImports();
	IImportContainer container= this.cu.getImportContainer();
	String[] importNames = new String[] {"p2.*", "p3.Z"};

	assertEquals("Wrong number of imports returned", importNames.length, imprts.length);
	for (int i = 0; i < imprts.length; i++) {
		assertTrue("Incorrect name for the type in this position: " + imprts[i].getElementName(), imprts[i].getElementName().equals(importNames[i]));
		assertTrue("Import does not exist " + imprts[i], imprts[i].exists());
		if (i == 0) {
			assertTrue("Import is not on demand " + imprts[i], imprts[i].isOnDemand());
			assertTrue("Import should be non-static " + imprts[i], imprts[i].getFlags() == Flags.AccDefault);
		} else {
			assertTrue("Import is on demand " + imprts[i], !imprts[i].isOnDemand());
			assertTrue("Import should be non-static " + imprts[i], imprts[i].getFlags() == Flags.AccDefault);
		}
		assertTrue("Container import does not equal import", container.getImport(imprts[i].getElementName()).equals(imprts[i]));
	}

	assertTrue("Import container must exist and have children", container.exists() && container.hasChildren());
	ISourceRange containerRange= container.getSourceRange();
	assertEquals(
		"Offset container range not correct", 
		imprts[0].getSourceRange().getOffset(),
		containerRange.getOffset());
	assertEquals(
		"Length container range not correct",
		imprts[imprts.length-1].getSourceRange().getOffset() + imprts[imprts.length-1].getSourceRange().getLength(),
		containerRange.getOffset() + containerRange.getLength());
	assertSourceEquals("Source not correct", 
		"import p2.*;\n" +
		"import p3.Z;",
		container.getSource());
	
}
/**
 * Ensure that type handles are returned from the
 * compilation unit for an inner type.
 */
public void testGetInnerTypes() throws JavaModelException {
	IType type1 = cu.getType("X");
	assertTrue("X type should have children", type1.hasChildren());
	assertTrue("X type superclass name should be null", type1.getSuperclassName() == null);
	String[] superinterfaceNames= type1.getSuperInterfaceNames();
	assertEquals("X type should have one superinterface", 1, superinterfaceNames.length);
	assertEquals("Unexpected super interface name", "Runnable", superinterfaceNames[0]);
	assertEquals("Fully qualified name of the type is incorrect", "p.X", type1.getFullyQualifiedName());
	IType type2 = type1.getType("Inner");
	superinterfaceNames = type2.getSuperInterfaceNames();
	assertEquals("X$Inner type should not have a superinterface", 0, superinterfaceNames.length);
	assertEquals("Fully qualified name of the inner type is incorrect", "p.X$Inner", type2.getFullyQualifiedName());
	assertEquals("Declaring type of the inner type is incorrect", type1, type2.getDeclaringType());
	IType type3 = type2.getType("InnerInner");
	assertTrue("InnerInner type should not have children", !type3.hasChildren());
}
/**
 * Ensures that a method has the correct return type, parameters and exceptions.
 */
public void testGetMethod1() throws JavaModelException {
	IType type = this.cu.getType("X");
	IMethod foo = type.getMethod("foo", new String[]{"QY;"});
	String[] exceptionTypes= foo.getExceptionTypes();
	assertEquals("Wrong number of exception types", 1, exceptionTypes.length);
	assertEquals("Unxepected exception type", "QIOException;", exceptionTypes[0]);
	assertEquals("Wrong return type", "V", foo.getReturnType());
	String[] parameterNames = foo.getParameterNames();
	assertEquals("Wrong number of parameter names", 1, parameterNames.length);
	assertEquals("Unexpected parameter name", "y", parameterNames[0]);
}
/**
 * Ensures that a method has the correct AccVarargs flag set.
 */
public void testGetMethod2() throws JavaModelException {
	IType type = this.cu.getType("X");
	IMethod method = type.getMethod("testIsVarArgs", new String[]{"QString;", "[QObject;"});
	assertTrue("Should have the AccVarargs flag set", Flags.isVarargs(method.getFlags()));
}

/**
 * Ensures that correct number of methods with the correct names and modifiers
 * exist in a type.
 */
public void testGetMethods() throws JavaModelException {
	IType type = this.cu.getType("X");
	IMethod[] methods= type.getMethods();
	String[] methodNames = new String[] {"foo", "bar", "fred", "testIsVarArgs"};
	String[] flags = new String[] {"public", "protected static", "private", ""};
	assertEquals("Wrong number of methods returned", methodNames.length, methods.length);
	for (int i = 0; i < methods.length; i++) {
		assertEquals("Incorrect name for the " + i + " method", methodNames[i], methods[i].getElementName());
		int modifiers = methods[i].getFlags() & ~Flags.AccVarargs;
		String mod= Flags.toString(modifiers);
		assertEquals("Unexpected modifier for " + methods[i].getElementName(), flags[i], mod);
		assertTrue("Method does not exist " + methods[i], methods[i].exists());
	}
}
/**
 * Ensures that correct modifiers are reported for a method in an interface.
 */
public void testCheckInterfaceMethodModifiers() throws JavaModelException {
	IType type = this.cu.getType("I");
	IMethod method = type.getMethod("run", new String[0]);
	String expectedModifiers = "";
	String modifiers = Flags.toString(method.getFlags() & ~Flags.AccVarargs);
	assertEquals("Expected modifier for " + method.getElementName(), expectedModifiers, modifiers);
}
/**
 * Ensure that the same element is returned for the primary element of a
 * compilation unit.
 */
public void testGetPrimary() {
	IJavaElement primary = this.cu.getPrimaryElement();
	assertEquals("Primary element for a compilation unit should be the same", this.cu, primary);
	primary = this.cu.getPrimary();
	assertEquals("Primary for a compilation unit should be the same", this.cu, primary);
	
}
/**
 * Ensures that correct number of package declarations with the correct names
 * exist a compilation unit.
 */
public void testGetPackages() throws JavaModelException {
	IPackageDeclaration[] packages = this.cu.getPackageDeclarations();
	String packageName = "p";
	assertEquals("Wrong number of packages returned", 1, packages.length);
	assertEquals("Wrong package declaration returned: ", packageName, packages[0].getElementName());
}
/**
 * Ensure that type handles are returned from the
 * compilation unit.
 * Checks non-existant handle and existing handles.
 */
public void testGetType() {
	IType type = this.cu.getType("someType");
	assertTrue("Type should not exist " + type, !type.exists());
	
	type = this.cu.getType("X");
	assertTrue("Type should exist " + type, type.exists());
	
	type = this.cu.getType("I"); // secondary type
	assertTrue("Type should exist " + type, type.exists());
}
/**
 * Ensures that correct number of types with the correct names and modifiers
 * exist in a compilation unit.
 */
public void testGetTypes() throws JavaModelException {
	IType[] types = this.cu.getTypes();
	String[] typeNames = new String[] {"X", "I"};
	String[] flags = new String[] {"public", ""};
	boolean[] isClass = new boolean[] {true, false};
	boolean[] isInterface = new boolean[] {false, true};
	boolean[] isAnnotation = new boolean[] {false, false};
	boolean[] isEnum = new boolean[] {false, false};
	String[] superclassName = new String[] {null, null};
	String[] superclassType = new String[] {null, null};
	String[][] superInterfaceNames = new String[][] {
			new String[] {"Runnable"}, new String[0]
	};
	String[][] superInterfaceTypes = new String[][] {
			new String[] {"QRunnable;"}, new String[0]
	};
	String[][] formalTypeParameters = new String[][] {
		new String[0], new String[0]};
	
	assertEquals("Wrong number of types returned", typeNames.length, types.length);
	for (int i = 0; i < types.length; i++) {
		assertEquals("Incorrect name for the " + i + " type", typeNames[i], types[i].getElementName());
		String mod= Flags.toString(types[i].getFlags());
		assertEquals("Unexpected modifier for " + types[i].getElementName(), flags[i], mod);
		assertTrue("Type does not exist " + types[i], types[i].exists());
		assertEquals("Incorrect isClass for the " + i + " type", isClass[i], types[i].isClass());
		assertEquals("Incorrect isInterface for the " + i + " type", isInterface[i], types[i].isInterface());
		assertEquals("Incorrect isAnnotation for the " + i + " type", isAnnotation[i], types[i].isAnnotation());
		assertEquals("Incorrect isEnum for the " + i + " type", isEnum[i], types[i].isEnum());
		assertEquals("Incorrect superclassName for the " + i + " type", superclassName[i], types[i].getSuperclassName());
		assertEquals("Incorrect superclassType for the " + i + " type", superclassType[i], types[i].getSuperclassTypeSignature());
		assertEquals("Incorrect superInterfaceNames for the " + i + " type", superInterfaceNames[i].length, types[i].getSuperInterfaceNames().length);
		assertEquals("Incorrect superInterfaceTypes for the " + i + " type", superInterfaceTypes[i].length, types[i].getSuperInterfaceTypeSignatures().length);
		assertEquals("Incorrect formalTypeParameters for the " + i + " type", formalTypeParameters[i].length, types[i].getTypeParameters().length);
	}
}
/**
 * Ensures that a compilation unit has children.
 */
public void testHasChildren() throws JavaModelException {
	this.cu.close();
	assertTrue("A closed compilation unit should have children", this.cu.hasChildren());
	this.cu.getChildren();
	assertTrue("The compilation unit should have children", this.cu.hasChildren());
}
/**
 * Ensures that a compilation unit's resource has not changed.
 */
public void testHasResourceChanged() {
	assertTrue(
		"A compilation unit's resource should not have changed", 
		!this.cu.hasResourceChanged());
}
/**
 * Ensures that a compilation unit that does not exist responds
 * false to #exists() and #isOpen()
 */
public void testNotPresent1() {
	ICompilationUnit compilationUnit = ((IPackageFragment)this.cu.getParent()).getCompilationUnit("DoesNotExist.java");
	assertTrue("CU should not be open", !compilationUnit.isOpen());
	assertTrue("CU should not exist", !compilationUnit.exists());
	assertTrue("CU should still not be open", !compilationUnit.isOpen());
}
/**
 * Ensures that a compilation unit that does not exist
 * (because it is a child of a jar package fragment)
 * responds false to #exists() and #isOpen()
 * (regression test for PR #1G2RKD2)
 */
public void testNotPresent2() throws CoreException {
	ICompilationUnit compilationUnit = getPackageFragment("P", getExternalJCLPathString(), "java.lang").getCompilationUnit("DoesNotExist.java");
	assertTrue("CU should not be open", !compilationUnit.isOpen());
	assertTrue("CU should not exist", !compilationUnit.exists());
	assertTrue("CU should still not be open", !compilationUnit.isOpen());
}
/**
 * Ensures that the "structure is known" flag is set for a valid compilation unit. 
 */
public void testStructureKnownForCU() throws JavaModelException {
	assertTrue("Structure is unknown for valid CU", this.cu.isStructureKnown());
}
/**
 *  Ensures that the "structure is unknown" flag is set for a non valid compilation unit. 
 */
public void testStructureUnknownForCU() throws CoreException {
	try {
		this.createFile(
			"/P/src/p/Invalid.java",
			"@#D(03");
		ICompilationUnit badCU = getCompilationUnit("/P/src/p/Invalid.java");
		assertTrue("Structure is known for an invalid CU", !badCU.isStructureKnown());
	} finally {
		this.deleteFile("/P/src/p/Invalid.java");
	}
}

/*
 * Verify fix for bug 73884: [1.5] Unexpected error for class implementing generic interface
 * (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=73884)
 */
public void testBug73884() throws CoreException {
	try {
		String cuSource = 
			"package p;\n" +
			"public interface I<T> {\n" +
			"}";
		createFile("/P/src/p/I.java", cuSource);
		ITypeParameter[] typeParameters = getCompilationUnit("/P/src/p/I.java").getType("I").getTypeParameters();
		assertTypeParametersEqual(
			"T\n",
			typeParameters);
	} finally {
		deleteFile("/P/src/p/I.java");
	}
}

/*
 * Ensure that the type parameters for a type are correct.
 */
public void testTypeParameter1() throws CoreException {
	ICompilationUnit workingCopy = null;
	try {
		workingCopy = workingCopy(
			"package p;\n" +
			"public class Y<T> {\n" +
			"}"
		);
		ITypeParameter[] typeParameters = workingCopy.getType("Y").getTypeParameters();
		assertTypeParametersEqual(
			"T\n",
			typeParameters);
	} finally {
		if (workingCopy != null)
			workingCopy.discardWorkingCopy();
	}
}

/*
 * Ensure that the type parameters for a type are correct.
 */
public void testTypeParameter2() throws CoreException {
	ICompilationUnit workingCopy = null;
	try {
		workingCopy = workingCopy(
			"package p;\n" +
			"public class Y<T, U> {\n" +
			"}"
		);
		ITypeParameter[] typeParameters = workingCopy.getType("Y").getTypeParameters();
		assertTypeParametersEqual(
			"T\n" +
			"U\n",
			typeParameters);
	} finally {
		if (workingCopy != null)
			workingCopy.discardWorkingCopy();
	}
}

/*
 * Ensure that the type parameters for a type are correct.
 */
public void testTypeParameter3() throws CoreException {
	ICompilationUnit workingCopy = null;
	try {
		workingCopy = workingCopy(
			"package p;\n" +
			"public class Y<T extends List> {\n" +
			"}"
		);
		ITypeParameter[] typeParameters = workingCopy.getType("Y").getTypeParameters();
		assertTypeParametersEqual(
			"T extends List\n",
			typeParameters);
	} finally {
		if (workingCopy != null)
			workingCopy.discardWorkingCopy();
	}
}

/*
 * Ensure that the type parameters for a type are correct.
 */
public void testTypeParameter4() throws CoreException {
	ICompilationUnit workingCopy = null;
	try {
		workingCopy = workingCopy(
			"package p;\n" +
			"public class Y<T extends List & Runnable & Comparable> {\n" +
			"}"
		);
		ITypeParameter[] typeParameters = workingCopy.getType("Y").getTypeParameters();
		assertTypeParametersEqual(
			"T extends List & Runnable & Comparable\n",
			typeParameters);
	} finally {
		if (workingCopy != null)
			workingCopy.discardWorkingCopy();
	}
}

/*
 * Ensure that the type parameters for a method are correct.
 * (regression test for bug 75658 [1.5] SourceElementParser do not compute correctly bounds of type parameter)
 */
public void testTypeParameter5() throws CoreException {
	ICompilationUnit workingCopy = null;
	try {
		workingCopy = workingCopy(
			"package p;\n" +
			"public class Y {\n" +
			"  <T extends List, U extends X & Runnable> void foo() {\n" +
			"  }\n" +
			"}"
		);
		ITypeParameter[] typeParameters = workingCopy.getType("Y").getMethod("foo", new String[]{}).getTypeParameters();
		assertTypeParametersEqual(
			"T extends List\n" + 
			"U extends X & Runnable\n",
			typeParameters);
	} finally {
		if (workingCopy != null)
			workingCopy.discardWorkingCopy();
	}
}

private ICompilationUnit workingCopy(String source) throws JavaModelException {
	ICompilationUnit workingCopy = getCompilationUnit("/P/src/p/Y.java").getWorkingCopy(new WorkingCopyOwner(){}, null, null);
	workingCopy.getBuffer().setContents(source);
	workingCopy.makeConsistent(null);
	return workingCopy;
}
}
