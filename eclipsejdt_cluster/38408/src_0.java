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
package org.eclipse.jdt.core.tests.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.tests.util.Util;

import junit.framework.Test;

public class TypeHierarchyTests extends ModifyingResourceTests {
	/**
	 * A placeholder for a type hierarchy used in some test cases.
	 */
	ITypeHierarchy typeHierarchy;

static {
//	TESTS_NAMES= new String[] { "testGeneric7" };
}
public static Test suite() {
	return buildModelTestSuite(TypeHierarchyTests.class);
}
public TypeHierarchyTests(String name) {
	super(name);
	this.displayName = true;
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.core.tests.model.AbstractJavaModelTests#setUpSuite()
 */
public void setUpSuite() throws Exception {
	super.setUpSuite();

	IJavaProject project = setUpJavaProject("TypeHierarchy");
	addLibrary(project, "myLib.jar", "myLibsrc.zip", new String[] {
		"my/pkg/X.java",
		"package my.pkg;\n" + 
		"public class X {\n" + 
		"}",
		"my/pkg/Y.java",
		"package my.pkg;\n" + 
		"public class Y {\n" + 
		"  void foo() {\n" +
		"    new X() {};" +
		"  }\n" +
		"}",
	}, JavaCore.VERSION_1_4);
	
	IPackageFragmentRoot root = project.getPackageFragmentRoot(project.getProject().getFile("lib.jar"));
	IRegion region = JavaCore.newRegion();
	region.add(root);
	this.typeHierarchy = project.newTypeHierarchy(region, null);
	
	IJavaProject project15 = createJavaProject("TypeHierarchy15", new String[] {"src"}, new String[] {"JCL15_LIB"}, "bin", "1.5");
	addLibrary(project15, "lib15.jar", "lib15src.zip", new String[] {
		"util/AbstractList.java",
		"package util;\n" + 
		"public class AbstractList<E> {\n" + 
		"}",
		"util/ArrayList.java",
		"package util;\n" + 
		"public class ArrayList<E> extends AbstractList<E> implements List<E> {\n" + 
		"}",
		"util/List.java",
		"package util;\n" + 
		"public interface List<E> {\n" + 
		"}",
		"util/Map.java",
		"package util;\n" + 
		"public class Map<K,V> extends AbstractList<V> {\n" + 
		"}",
	}, JavaCore.VERSION_1_5);
	createFile(
		"/TypeHierarchy15/src/X.java", 
		"import util.*;\n" +
		"public class X<E> extends ArrayList<E> implements List<E> {\n" +
		"}"
	);
	createFile(
		"/TypeHierarchy15/src/Y.java", 
		"import util.*;\n" +
		"public class Y extends ArrayList implements List {\n" +
		"}"
	);
	createFile(
		"/TypeHierarchy15/src/I.java", 
		"public interface I<E> {\n" +
		"}"
	);
	createFile(
		"/TypeHierarchy15/src/A.java", 
		"public class A<E> implements I<E> {\n" +
		"}"
	);
	createFile(
		"/TypeHierarchy15/src/X99606.java", 
		"public class X99606 extends Y99606<X99606.Color> {\n" + 
		"	static class Color {}\n" + 
		"}"
	);
	createFile(
		"/TypeHierarchy15/src/Y99606.java", 
		"public class Y99606<T> {\n" + 
		"}"
	);
	createFile(
		"/TypeHierarchy15/src/A108740.java", 
		"class A108740<T> {}"
	);
	createFile(
		"/TypeHierarchy15/src/B108740.java", 
		"class B108740<T> extends A108740<C108740> {}"
	);
	createFile(
		"/TypeHierarchy15/src/C108740.java", 
		"class C108740 extends B108740<C108740> {}"
	);
	createFile(
		"/TypeHierarchy15/src/D108740.java", 
		"class D108740 extends B108740<D108740> {}"
	);
	createFile(
		"/TypeHierarchy15/src/CycleParent.java", 
		"class CycleParent extends CycleBase<CycleChild> {}"
	);
	createFile(
		"/TypeHierarchy15/src/CycleBase.java", 
		"class CycleBase<T extends CycleBase> {}"
	);
	createFile(
		"/TypeHierarchy15/src/CycleChild.java", 
		"class CycleChild extends CycleParent implements Comparable<CycleChild> {\n" +
		"	public int compareTo(CycleChild o) { return 0; }\n" +
		"}"
	);
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.core.tests.model.SuiteOfTestCases#tearDownSuite()
 */
public void tearDownSuite() throws Exception {
	this.typeHierarchy = null;
	deleteProject("TypeHierarchy");
	deleteProject("TypeHierarchy15");
	
	super.tearDownSuite();
}
/*
 * Ensures that a hierarchy on an anonymous type in an initializer is correct.
 */
public void testAnonymousType1() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getInitializer(1).getType("", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: <anonymous #1> [in <initializer #1> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on an anonymous type in a second initializer is correct.
 */
public void testAnonymousType2() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getInitializer(2).getType("", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: <anonymous #1> [in <initializer #2> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on an anonymous type in a field declaration is correct.
 */
public void testAnonymousType3() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getField("field1").getType("", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: <anonymous #1> [in field1 [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on an anonymous type in a field declaration is correct.
 */
public void testAnonymousType4() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getField("field2").getType("", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: <anonymous #1> [in field2 [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
	type = typeA.getField("field2").getType("", 2);
	hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: <anonymous #2> [in field2 [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on an anonymous type in a method declaration is correct.
 */
public void testAnonymousType5() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getMethod("foo", new String[] {}).getType("", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: <anonymous #1> [in foo() [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on an anonymous type that uses a non-default constructor is correct.
 * (regression test for bug 44506 Type hierarchy is missing anonymous type)
 */
public void testAnonymousType6() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p8", "X.java").getType("X");
	IType type = typeA.getMethod("foo", new String[] {}).getType("", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: <anonymous #1> [in foo() [in X [in X.java [in p8 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p8 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensure that the key of an anonymous binary type in a hierarchy is correct.
 * (regression test for bug 93826 ArrayIndexOutOfBoundsException when opening type hierarchy)
 */
public void testAnonymousType7() throws CoreException {
	IType type = getClassFile("TypeHierarchy","myLib.jar", "my.pkg", "X.class").getType();
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] subtypes = hierarchy.getSubtypes(type);
	assertEquals("Unexpected key", "Lmy/pkg/Y$1;", subtypes.length < 1 ? null : subtypes[0].getKey());
}
/**
 * Ensures that the superclass can be retrieved for a binary inner type.
 */
public void testBinaryInnerTypeGetSuperclass() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "Y$Inner.class");
	IType type = cf.getType();
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType superclass = h.getSuperclass(type);
	assertTrue("Superclass not found for Y$Inner", superclass != null);
	assertEquals("Unexpected super class", "Z", superclass.getElementName());
}
/**
 * Ensures that the superinterfaces can be retrieved for a binary inner type.
 */
public void testBinaryInnerTypeGetSuperInterfaces() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "Y$Inner.class");
	IType type = cf.getType();
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	assertTypesEqual(
		"Unexpected super interfaces", 
		"binary.I\n", 
		h.getSuperInterfaces(type));
}
/*
 * Ensures that the hierarchy lookup mechanism get the right binary if it is missplaced.
 * (regression test for bug 139279 Fup of bug 134110, got CCE changing an external jar contents and refreshing the project)
 */
public void testBinaryInWrongPackage() throws CoreException {
	try {
		createJavaProject("P", new String[] {"src"}, new String[] {"JCL_LIB", "lib"}, "bin");
		createFolder("/P/src/p");
		createFile(
			"/P/src/p/X.java",
			"pakage p;\n" +
			"public class X {\n" +
			"}"
		);
		getProject("P").build(IncrementalProjectBuilder.FULL_BUILD, null);
		waitForAutoBuild();
		getFile("/P/bin/p/X.class").copy(new Path("/P/lib/X.class"), false, null);
		ITypeHierarchy hierarchy = getClassFile("P", "/P/lib", "", "X.class").getType().newSupertypeHierarchy(null);
		assertHierarchyEquals(
			"Focus: X [in X.class [in <default> [in lib [in P]]]]\n" + 
			"Super types:\n" + 
			"Sub types:\n" + 
			"Root classes:\n",
			hierarchy);
	} finally {
		deleteProject("P");
	}
}
/*
 * Ensures that a hierarchy with a binary subclass that is also referenced can be computed
 * (regression test for bug 48459 NPE in Type hierarchy)
 */
public  void testBinarySubclass() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy/src/p48459/p1/X48459.java").getType("X48459");
	ITypeHierarchy h = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: X48459 [in X48459.java [in p48459.p1 [in src [in TypeHierarchy]]]]\n" + 
		"Super types:\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n" + 
		"  <anonymous #1> [in foo [in Z48459 [in Z48459.java [in p48459.p1 [in src [in TypeHierarchy]]]]]]\n" + 
		"  Y48459 [in Y48459.class [in p48459.p2 [in lib48459 [in TypeHierarchy]]]]\n",
		h);
}
/**
 * Ensures that the superclass can be retrieved for a binary type's superclass.
 */
public void testBinaryTypeGetSuperclass() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "Y.class");
	IType type = cf.getType();
	ITypeHierarchy h= type.newSupertypeHierarchy(null);
	IType superclass= h.getSuperclass(type);
	assertTrue("Superclass not found forY", superclass != null);
	assertEquals("Unexpected superclass of Y", "X", superclass.getElementName());
}
/**
 * Ensures that the superclass can be retrieved for a binary type's superclass.
 * This is a relatively deep type hierarchy.
 */
public void testBinaryTypeGetSuperclass2() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "Deep.class");
	IType type = cf.getType();
	ITypeHierarchy h= type.newSupertypeHierarchy(null);
	IType superclass= h.getSuperclass(type);
	assertTrue("Superclass not found for Deep", superclass != null);
	assertEquals("Unexpected superclass of Deep", "Z", superclass.getElementName());
}
/**
 * Ensures that the superinterfaces can be retrieved for a binary type's superinterfaces.
 */
public void testBinaryTypeGetSuperInterfaces() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "X.class");
	IType type = cf.getType();
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType[] superInterfaces = h.getSuperInterfaces(type);
	assertTypesEqual(
		"Unexpected super interfaces of X", 
		"binary.I\n", 
		superInterfaces);
}
/**
 * Ensures that the superinterfaces can be retrieved for a binary type's superinterfaces.
 * Test with type that has a "rich" super hierarchy
 */
public void testBinaryTypeGetSuperInterfaces2() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "rich", "C.class");
	IType type = cf.getType();
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType[] superInterfaces = h.getSuperInterfaces(type);
	assertTypesEqual(
		"Unexpected super interfaces of C", 
		"rich.I\n" +
		"rich.I3\n", 
		superInterfaces);
}
/*
 * Ensures that a hierarchy can be constructed on a binary type in a jar that is hidden by another jar with the same type.
 * (regression test for bug 
 */
public void testBinaryTypeHiddenByOtherJar() throws CoreException, IOException {
	String externalJar1 = null;
	String externalJar2 = null;
	try {
		externalJar1 = Util.getOutputDirectory() + File.separator + "test1.jar";
		Util.createJar(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"public class X {\n" + 
				"}" ,
				"p/Y.java",
				"package p;\n" +
				"public class Y extends X {\n" + 
				"}" 
			},
			new HashMap(),
			externalJar1
		);
		externalJar2 = Util.getOutputDirectory() + File.separator + "test2.jar";
		Util.createJar(
			new String[] {
				"p/X.java",
				"package p;\n" +
				"public class X {\n" + 
				"}" ,
				"p/Y.java",
				"package p;\n" +
				"public class Y extends X {\n" + 
				"}" 
			},
			new HashMap(),
			externalJar2
		);
		IJavaProject project = createJavaProject("P", new String[] {}, new String[] {"JCL_LIB", externalJar1, externalJar2}, "");
		IType focus = project.getPackageFragmentRoot(externalJar2).getPackageFragment("p").getClassFile("Y.class").getType();
		assertHierarchyEquals(
			"Focus: Y [in Y.class [in p [in " + externalJar2 + " [in P]]]]\n" + 
			"Super types:\n" + 
			"  X [in X.class [in p [in " + externalJar1 + " [in P]]]]\n" + 
			"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in P]]]]\n" + 
			"Sub types:\n",
			focus.newTypeHierarchy(null)
		);
	} finally {
		if (externalJar1 != null)
			new File(externalJar1).delete();
		if (externalJar2 != null)
			new File(externalJar2).delete();
		deleteProject("P");
	}
}
/**
 * Ensures that the creation of a type hierarchy can be cancelled.
 */
public void testCancel() throws JavaModelException {
	boolean isCanceled = false;
	IType type = getCompilationUnit("TypeHierarchy", "src", "p1", "X.java").getType("X");
	IRegion region = JavaCore.newRegion();
	region.add(getPackageFragmentRoot("TypeHierarchy", "src"));
	try {
		TestProgressMonitor monitor = TestProgressMonitor.getInstance();
		monitor.setCancelledCounter(1);
		type.getJavaProject().newTypeHierarchy(type, region, monitor);
	} catch (OperationCanceledException e) {
		isCanceled = true;
	}
	assertTrue("Operation should have thrown an operation canceled exception", isCanceled);
}
/**
 * Ensures that contains(...) returns true for a type that is part of the
 * hierarchy and false otherwise.
 */
public void testContains() throws JavaModelException {
	// regular class
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "X.class");
	IType type = cf.getType();
	assertTrue("X must be included", this.typeHierarchy.contains(type));

	// root class
	cf = getClassFile("TypeHierarchy", getExternalJCLPathString(), "java.lang", "Object.class");
	type = cf.getType();
	assertTrue("Object must be included", this.typeHierarchy.contains(type));

	// interface
	cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "I.class");
	assertTrue("I must be included", this.typeHierarchy.contains(type));
}
public void testCycle() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy/src/cycle/X.java").getType("X");
	ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: X [in X.java [in cycle [in src [in TypeHierarchy]]]]\n" + 
		"Super types:\n" + 
		"  Y [in Y.java [in cycle [in src [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
public void testCycle2() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy15/src/CycleParent.java").getType("CycleParent");
	ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: CycleParent [in CycleParent.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"  CycleBase [in CycleBase.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
/*
 * Ensures that a hierarchy can be created with a potential subtype in an empty primary working copy
 * (regression test for bug 65677 Creating hierarchy failed. See log for details. 0)
 */
public void testEmptyWorkingCopyPotentialSubtype() throws JavaModelException {
    ICompilationUnit workingCopy = null;
    try {
        workingCopy = getCompilationUnit("/TypeHierarchy/src/q4/Y.java");
        workingCopy.becomeWorkingCopy(null, null);
        workingCopy.getBuffer().setContents("");
        workingCopy.makeConsistent(null);
        
        IType type = getCompilationUnit("/TypeHierarchy/src/q4/X.java").getType("X");
		ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
		assertHierarchyEquals(
			"Focus: X [in X.java [in q4 [in src [in TypeHierarchy]]]]\n" + 
			"Super types:\n" + 
			"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
			"Sub types:\n",
			hierarchy);
    } finally {
        if (workingCopy != null)
            workingCopy.discardWorkingCopy();
    }
}
/*
 * Ensures that a hierarchy on a type with local and anonymous types is correct.
 */
public void testFocusWithLocalAndAnonymousTypes() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p7", "X.java").getType("X");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"Super types:\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n" + 
		"  <anonymous #1> [in <initializer #2> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"    Y2 [in foo() [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"  <anonymous #1> [in field1 [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"  <anonymous #1> [in field2 [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"  <anonymous #1> [in foo() [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"  <anonymous #1> [in <initializer #1> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"  <anonymous #2> [in field2 [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"  Y1 [in foo() [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"    Y2 [in <initializer #1> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"  Y1 [in <initializer #1> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on a generic type can be opened
 */
public void testGeneric01() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy15/src/X.java").getType("X");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: X [in X.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"  ArrayList [in ArrayList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"  List [in List.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"    AbstractList [in AbstractList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"    List [in List.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"      Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
/*
 * Ensures that a hierarchy on a generic type can be opened
 */
public void testGeneric02() throws JavaModelException {
	IType type = getPackageFragmentRoot("/TypeHierarchy15/lib15.jar").getPackageFragment("util").getClassFile("ArrayList.class").getType();
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: ArrayList [in ArrayList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"  AbstractList [in AbstractList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"  List [in List.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
		"Sub types:\n" + 
		"  X [in X.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"  Y [in Y.java [in <default> [in src [in TypeHierarchy15]]]]\n",
		hierarchy
	);
}
/*
 * Ensures that a hierarchy on a generic type can be opened
 */
public void testGeneric03() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy15/src/Y.java").getType("Y");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y [in Y.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"  ArrayList [in ArrayList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"  List [in List.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"    AbstractList [in AbstractList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"    List [in List.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"      Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
/*
 * Ensures that a super type hierarchy on a generic type can be opened
 * (regression test for bug 72348 [1.5][Type Hierarchy] Super type hierarchy of class extending generic type is empty)
 */
public void testGeneric04() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy15/src/X.java").getType("X");
	ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: X [in X.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"  ArrayList [in ArrayList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"  List [in List.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"    AbstractList [in AbstractList.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"    List [in List.class [in util [in lib15.jar [in TypeHierarchy15]]]]\n" + 
		"      Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
/*
 * Ensures that a hierarchy on a generic interface can be opened
 * (regression test for bug 82004 [model][5.0] 3.1M4 type hierarchy for generic interface)
 */
public void testGeneric05() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy15/src/I.java").getType("I");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: I [in I.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"Sub types:\n" + 
		"  A [in A.java [in <default> [in src [in TypeHierarchy15]]]]\n",
		hierarchy
	);
}
/*
 * Ensure that the key of a binary type in a hierarchy is correct when this type is not part of the Java model cache.
 * (regression test for bug 93854 IAE in Util.scanTypeSignature when scanning a signature retrieved from a binding key)
 */
public void testGeneric06() throws CoreException {
	getJavaProject("TypeHierarcht15").close();
	IType type = getClassFile("TypeHierarchy15","lib15.jar", "util", "AbstractList.class").getType();
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] subtypes = hierarchy.getSubtypes(type);
	assertEquals("Unexpected key", "Lutil/Map<TK;TV;>;", subtypes.length < 2 ? null : subtypes[1].getKey());
}
/*
 * Ensures that a hierarchy on a generic type that is extended using a member as a type parameter can be opened
 * (regression test for bug 99606 Subtype not found if parameterized on inner class)
 */
public void testGeneric07() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy15/src/Y99606.java").getType("Y99606");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y99606 [in Y99606.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
		"Sub types:\n" + 
		"  X99606 [in X99606.java [in <default> [in src [in TypeHierarchy15]]]]\n",
		hierarchy
	);
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=108740
public void testGeneric08() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy15/src/D108740.java").getType("D108740");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: D108740 [in D108740.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"Super types:\n" + 
		"  B108740 [in B108740.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"    A108740 [in A108740.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
		"      Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
/*
 * Ensures that a hierarchy is where a type inherits conflicting paratemerized types is still correctly reported
 * (regression test for bug 136095 Type Hierarchy incomplete with illegally parameterized superinterfaces)
 */
public void testGeneric09() throws CoreException {
	try {
		createFile(
			"/TypeHierarchy15/src/I1_136095.java", 
			"public interface I1_136095<E> {\n" + 
			"}"
		);
		createFile(
			"/TypeHierarchy15/src/I2_136095.java", 
			"public interface I2_136095 extends I1_136095<String>{\n" + 
			"}"
		);
		createFile(
			"/TypeHierarchy15/src/X_136095.java", 
			"public abstract class X_136095 implements I1_136095<Integer>, I2_136095 {\n" + 
			"}"
		);
		IType type = getCompilationUnit("/TypeHierarchy15/src/X_136095.java").getType("X_136095");
		ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
		assertHierarchyEquals(
			"Focus: X_136095 [in X_136095.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
			"Super types:\n" + 
			"  I1_136095 [in I1_136095.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
			"  I2_136095 [in I2_136095.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
			"    I1_136095 [in I1_136095.java [in <default> [in src [in TypeHierarchy15]]]]\n" + 
			"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString("1.5") + " [in TypeHierarchy15]]]]\n" + 
			"Sub types:\n",
			hierarchy
		);
	} finally {
		deleteFile("/TypeHierarchy15/src/I1_136095.java");
		deleteFile("/TypeHierarchy15/src/I2_136095.java");
		deleteFile("/TypeHierarchy15/src/X_136095.java");
	}
}
/**
 * Ensures the correctness of all classes in a type hierarchy based on a region.
 */
public void testGetAllClassesInRegion() {
	IType[] types = this.typeHierarchy.getAllClasses();
	assertTypesEqual(
		"Unexpected all classes in hierarchy", 
		"binary.Deep\n" +
		"binary.X\n" +
		"binary.Y\n" +
		"binary.Y$Inner\n" +
		"binary.Z\n" +
		"java.lang.Object\n" +
		"rich.A\n" +
		"rich.B\n" +
		"rich.C\n", 
		types);
}
/**
 * Ensures the correctness of all interfaces in a type hierarchy based on a region.
 */
public void testGetAllInterfacesInRegion() {
	IType[] types = this.typeHierarchy.getAllInterfaces();
	assertTypesEqual(
		"Unexpected all interfaces in hierarchy", 
		"binary.I\n" +
		"rich.I\n" +
		"rich.I2\n" +
		"rich.I3\n", 
		types);
}
/**
 * Ensures that the correct subtypes of a type exist in the type 
 * hierarchy.
 */
public void testGetAllSubtypes() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p1", "X.java").getType("X");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getAllSubtypes(type);
	this.assertTypesEqual(
		"Unexpected sub types of X",
		"p1.Deep\n" +
		"p1.Y\n" +
		"p1.Z\n",
		types
	);
}
/**
 * Ensures that the correct subtypes of a binary type
 * exit in the type hierarchy created on a region.
 */
public void testGetAllSubtypesFromBinary() throws JavaModelException {
	IType type = getClassFile("TypeHierarchy", "lib.jar", "binary", "X.class").getType();
	IRegion region = JavaCore.newRegion();
	region.add(type.getPackageFragment());
	ITypeHierarchy hierarchy = type.getJavaProject().newTypeHierarchy(type, region, null);
	IType[] types = hierarchy.getAllSubtypes(type);
	assertTypesEqual(
		"Unexpected all subtypes of binary.X", 
		"binary.Deep\n" +
		"binary.Y\n" +
		"binary.Y$Inner\n" +
		"binary.Z\n", 
		types);
}

/**
 * Ensures that the correct superclasses of a type exist in the type 
 * hierarchy.
 */
public void testGetAllSuperclasses() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p1", "Z.java").getType("Z");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getAllSuperclasses(type);
	assertTypesEqual(
		"Unexpected all super classes of Z", 
		"java.lang.Object\n" + 
		"p1.X\n" + 
		"p1.Y\n",
		types);
}

/**
 * Ensures that the correct superclasses of a binary type exist in the type  hierarchy.
 * (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=53095)
 */
public void testGetAllSuperclassesFromBinary() throws JavaModelException {
	String fileName = "TypeHierarchy/lib53095/p53095/X53095.class";	//$NON-NLS-1$
	IJavaElement javaElement = JavaCore.create(getFile(fileName));
	assertNotNull("Problem to get class file \""+fileName+"\"", javaElement);
	assertTrue("Invalid type for class file \""+fileName+"\"", javaElement instanceof IClassFile);
	IType type = ((IClassFile) javaElement).getType();
	ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null); // it works when we use newTypeHierarchy(null)
	IType[] types = hierarchy.getAllSupertypes(type);
	assertTypesEqual(
		"Unexpected all super classes of X53095", 
		"java.lang.RuntimeException\n" +
		"java.lang.Exception\n" +
		"java.lang.Throwable\n" +
		"java.lang.Object\n",
		types,
		false);
}

/**
 * Ensures that the correct superclasses of a binary type exist in the type  hierarchy.
 * (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=54043)
 */
public void testGetAllSuperclassesFromBinary2() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "test54043.jar", "p54043", "X54043.class");
	IType type = cf.getType();
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getAllSupertypes(type);
	assertTypesEqual(
		"Unexpected all super classes of X54043", 
		"java.lang.RuntimeException\n" +
		"java.lang.Exception\n" +
		"java.lang.Throwable\n" +
		"java.lang.Object\n",
		types,
		false);
}
/**
 * Ensures that the correct superinterfaces of a type exist in the type 
 * hierarchy.
 */
public void testGetAllSuperInterfaces() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p1", "Z.java").getType("Z");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getAllSuperInterfaces(type);
	assertTypesEqual(
		"Unexpected super interfaces of Z", 
		"p1.I1\n" + 
		"p1.I2\n",
		types);
}
/**
 * Ensures that the correct supertypes of a type exist in the type 
 * hierarchy.
 */
public void testGetAllSupertypes() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p1", "Z.java").getType("Z");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getAllSupertypes(type);
	assertTypesEqual(
		"Unexpected all super types of Z", 
		"java.lang.Object\n" + 
		"p1.I1\n" + 
		"p1.I2\n" + 
		"p1.X\n" + 
		"p1.Y\n",
		types);
}
/**
 * Ensures that the correct supertypes of a type exist in the type 
 * hierarchy.
 * (regression test for bug 23644 hierarchy: getAllSuperTypes does not include all superinterfaces?)
 */
public void testGetAllSupertypes2() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p3", "B.java").getType("B");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getAllSupertypes(type);
	assertTypesEqual(
		"Unexpected all super types of B", 
		"java.lang.Object\n" +
		"p3.A\n" +
		"p3.I\n" +
		"p3.I1\n",
		types);
}
/**
 * Ensures that the correct types exist in the type 
 * hierarchy.
 */
public void testGetAllTypes() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p1", "Y.java").getType("Y");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	this.assertTypesEqual(
		"Unexpected types in hierarchy of Y",
		"java.lang.Object\n" + 
		"p1.Deep\n" + 
		"p1.I1\n" + 
		"p1.I2\n" + 
		"p1.X\n" + 
		"p1.Y\n" + 
		"p1.Z\n",
		hierarchy.getAllTypes()
	);
}
/**
 * Ensures that the flags for an interface hierarchy are correctly cached
 * (regression test for bug 60365 hierarchy view shows some interfaces as classes [type hierarchy])
 */
public void testGetCachedFlags() throws JavaModelException {
	IType type1 = getClassFile("TypeHierarchy", "test60365.jar", "q4", "I1.class").getType();
	ITypeHierarchy hierarchy = type1.newTypeHierarchy(null);
	IType type2 = getClassFile("TypeHierarchy", "test60365.jar", "q4", "I2.class").getType();
	int flags = hierarchy.getCachedFlags(type2);
	assertTrue("Cached flags for I2 should indicate interface", Flags.isInterface(flags));
}
/**
 * Ensures that the correct extending interfaces exist in the type 
 * hierarchy.
 */
public void testGetExtendingInterfaces() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p2", "I.java").getType("I");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getExtendingInterfaces(type);
	this.assertTypesEqual(
		"Unexpected extending interfaces of I",
		"p2.I1\n" + 
		"p2.I2\n",
		types
	);

	type = getCompilationUnit("TypeHierarchy", "src", "p2", "X.java").getType("X");
	hierarchy = type.newTypeHierarchy(null);
	types = hierarchy.getExtendingInterfaces(type);
	this.assertTypesEqual(
		"Unexpected extending interfaces of X",
		"", // interfaces cannot extend a class
		types
	);
}
/**
 * Ensures that the correct implementing interfaces exist in the type 
 * hierarchy.
 */
public void testGetImplementingClasses() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p2", "I.java").getType("I");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getImplementingClasses(type);
	this.assertTypesEqual(
		"Unexpected implementing classes of I",
		"p2.X\n",
		types
	);

	type = getCompilationUnit("TypeHierarchy", "src", "p2", "X.java").getType("X");
	hierarchy = type.newTypeHierarchy(null);
	types = hierarchy.getImplementingClasses(type);
	this.assertTypesEqual(
		"Unexpected implementing classes of X",
		"", // classes cannot implement a class
		types
	);
}
/**
 * Ensures that the correct root classes exist in the type 
 * hierarchy.
 */
public void testGetRootClasses() {
	IType[] types = this.typeHierarchy.getRootClasses();
	assertTypesEqual(
		"Unexpected root classes",
		"java.lang.Object\n",
		types);
}
/**
 * Ensures that the correct root interfaces exist in the type 
 * hierarchy.
 */
public void testGetRootInterfaces() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p2", "Y.java").getType("Y");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	IType[] types = hierarchy.getRootInterfaces();
	assertTypesEqual(
		"Unexpected root classes",
		"p2.I\n",
		types);
}
/**
 * Ensures that getRootInterfaces() works on a IRegion.
 */
public void testGetRootInterfacesFromRegion() {
	IType[] types = this.typeHierarchy.getRootInterfaces();
	assertTypesEqual(
		"Unexpected root classes",
		"binary.I\n" + 
		"rich.I\n" + 
		"rich.I3\n",
		types);
}
/**
 * Ensures that the correct number of subclasses exist in the type 
 * hierarchy created on a region.
 */
public void testGetSubclasses() throws JavaModelException {
	IType type = getClassFile("TypeHierarchy", "lib.jar", "binary", "X.class").getType();
	IType[] types = this.typeHierarchy.getSubclasses(type);
	this.assertTypesEqual(
		"Unexpected subclasses of binary.X",
		"binary.Y\n",
		types
	);
	
	type = getClassFile("TypeHierarchy", "lib.jar", "binary", "I.class").getType();
	types = this.typeHierarchy.getSubclasses(type);
	this.assertTypesEqual(
		"Unexpected subclasses of binary.I",
		"", // interfaces cannot have a subclass
		types
	);
}
/**
 * Ensures that the correct number of subtypes exist in the type 
 * hierarchy created on a region.
 */
public void testGetSubtypes() throws JavaModelException {
	IType type = getClassFile("TypeHierarchy", "lib.jar", "binary", "X.class").getType();
	IType[] types = this.typeHierarchy.getSubtypes(type);
	this.assertTypesEqual(
		"Unexpected subtypes of binary.X",
		"binary.Y\n",
		types
	);
	
	type = getClassFile("TypeHierarchy", "lib.jar", "binary", "I.class").getType();
	types = this.typeHierarchy.getSubtypes(type);
	this.assertTypesEqual(
		"Unexpected subtypes of binary.I",
		"binary.X\n" + 
		"binary.Y$Inner\n",
		types
	);
}

/**
 * Ensures that the superclass is correct in the type 
 * hierarchy a type created on a region containing a package.
 */
public void testGetSuperclassInRegion() throws JavaModelException {
	IRegion r = JavaCore.newRegion();
	IPackageFragment p = getPackageFragment("TypeHierarchy", "src", "p1");
	r.add(p);
	ITypeHierarchy hierarchy = p.getJavaProject().newTypeHierarchy(r, null);

	IType type = getCompilationUnit("TypeHierarchy", "src", "p1", "Y.java").getType("Y");
	IType superclass= hierarchy.getSuperclass(type);
	assertEquals("Unexpected super class of Y", "X", superclass.getElementName());
}
/**
 * Ensures that the correct supertypes exist in the type 
 * hierarchy created on a region.
 */
public void testGetSupertypesInRegion() throws JavaModelException {
	IType type = getClassFile("TypeHierarchy", "lib.jar", "binary", "Y.class").getType();
	IType[] superTypes = this.typeHierarchy.getSupertypes(type);
	assertTypesEqual(
		"Unexpected super types of Y",
		"binary.X\n",
		superTypes);
}
/**
 * Ensures that the correct supertypes exist in the type 
 * hierarchy created on a region containing a project.
 */
public void testGetSupertypesWithProjectRegion() throws JavaModelException {
	IJavaProject project = getJavaProject("TypeHierarchy");
	IRegion region= JavaCore.newRegion();
	region.add(project);
	IType type = getClassFile("TypeHierarchy", "lib.jar", "binary", "Y.class").getType();
	ITypeHierarchy hierarchy = project.newTypeHierarchy(type, region, null);
	IType[] superTypes = hierarchy.getSupertypes(type);
	assertTypesEqual(
		"Unexpected super types of Y",
		"binary.X\n",
		superTypes);
}
/**
 * Ensures that getType() returns the type the hierarchy was created for.
 */
public void testGetType() throws JavaModelException {
	// hierarchy created on a type
	IClassFile cf = getClassFile("TypeHierarchy", "lib.jar", "binary", "Y.class");
	IType type = cf.getType();
	ITypeHierarchy hierarchy = null;
	try {
		hierarchy = type.newSupertypeHierarchy(null);
	} catch (IllegalArgumentException iae) {
		assertTrue("IllegalArgumentException", false);
	}
	assertEquals("Unexpected focus type", type, hierarchy.getType());

	// hierarchy created on a region
	assertTrue("Unexpected focus type for hierarchy on region", this.typeHierarchy.getType() == null);
}
/*
 * Ensures that a hierarchy on an type that implements a binary inner interface is correct.
 * (regression test for bug 58440 type hierarchy incomplete when implementing fully qualified interface)
 */
public void testImplementBinaryInnerInterface() throws JavaModelException {
	IClassFile cf = getClassFile("TypeHierarchy", "test58440.jar", "p58440", "Y.class");
	IType type = cf.getType();
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y [in Y.class [in p58440 [in test58440.jar [in TypeHierarchy]]]]\n" + 
		"Super types:\n" + 
		"  Inner [in X$Inner.class [in p58440 [in test58440.jar [in TypeHierarchy]]]]\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/**
 * Ensures that a hierarchy on an inner type is correctly rooted.
 */
public void testInnerType1() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p5", "X.java").getType("X").getType("Inner");
	ITypeHierarchy hierarchy = null;
	try {
		hierarchy = type.newTypeHierarchy(null);
	} catch (IllegalArgumentException iae) {
		assertTrue("IllegalArgumentException", false);
	}
	assertHierarchyEquals(
		"Focus: Inner [in X [in X.java [in p5 [in src [in TypeHierarchy]]]]]\n" + 
		"Super types:\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}


/**
 * Ensures that a hierarchy on an inner type has the correct subtype.
 * (regression test for bug 43274 Type hierarchy broken)
 */
public void testInnerType2() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p6", "A.java").getType("A").getType("Inner");
	ITypeHierarchy hierarchy = null;
	try {
		hierarchy = type.newTypeHierarchy(null);
	} catch (IllegalArgumentException iae) {
		assertTrue("IllegalArgumentException", false);
	}
	assertHierarchyEquals(
		"Focus: Inner [in A [in A.java [in p6 [in src [in TypeHierarchy]]]]]\n" + 
		"Super types:\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n" + 
		"  B [in A.java [in p6 [in src [in TypeHierarchy]]]]\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on a local type in an initializer is correct.
 */
public void testLocalType1() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getInitializer(1).getType("Y1", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y1 [in <initializer #1> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n" + 
		"  Y2 [in <initializer #1> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on a local type in a second initializer is correct.
 */
public void testLocalType2() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getInitializer(2).getType("Y3", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y3 [in <initializer #2> [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a hierarchy on a local type in a method declaration is correct.
 */
public void testLocalType3() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getMethod("foo", new String[] {}).getType("Y2", 1);
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y2 [in foo() [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  Y1 [in foo() [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"    X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"      Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a super type hierarchy on a local type in a method declaration is correct.
 * (regression test for bug 44073 Override methods action does not work for local types [code manipulation])
 */
public void testLocalType4() throws JavaModelException {
	IType typeA = getCompilationUnit("TypeHierarchy", "src", "p7", "A.java").getType("A");
	IType type = typeA.getMethod("foo", new String[] {}).getType("Y1", 1);
	ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y1 [in foo() [in A [in A.java [in p7 [in src [in TypeHierarchy]]]]]]\n" + 
		"Super types:\n" + 
		"  X [in X.java [in p7 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/*
 * Ensures that a type hierarchy on a member type with subtypes in another project is correct
 * (regression test for bug 101019 RC3: Type Hierarchy does not find implementers/extenders of inner class/interface in other project)
 */
public void testMemberTypeSubtypeDifferentProject() throws CoreException {
	try {
		createJavaProject("P1");
		createFile(
			"/P1/X.java",
			"public class X {\n" +
			"  public class Member {\n" +
			"  }\n" +
			"}"
			);
		createJavaProject("P2", new String[] {""}, new String[] {"JCL_LIB"}, new String[] {"/P1"}, "");
		createFile(
			"/P2/Y.java",
			"public class Y extends X.Member {\n" +
			"}"
		);
		IType focus = getCompilationUnit("/P1/X.java").getType("X").getType("Member");
		ITypeHierarchy hierarchy = focus.newTypeHierarchy(null/*no progress*/);
		assertHierarchyEquals(
			"Focus: Member [in X [in X.java [in <default> [in <project root> [in P1]]]]]\n" + 
			"Super types:\n" + 
			"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in P2]]]]\n" + 
			"Sub types:\n" + 
			"  Y [in Y.java [in <default> [in <project root> [in P2]]]]\n",
			hierarchy);
	} finally {
		deleteProjects(new String[] {"P1", "P2"});
	}
}
/**
 * Ensures that a hierarchy on a type that implements a missing interface is correctly rooted.
 * (regression test for bug 24691 Missing interface makes hierarchy incomplete)
 */
public void testMissingInterface() throws JavaModelException {
	IType type = getCompilationUnit("TypeHierarchy", "src", "p4", "X.java").getType("X");
	ITypeHierarchy hierarchy = null;
	try {
		hierarchy = type.newTypeHierarchy(null);
	} catch (IllegalArgumentException iae) {
		assertTrue("IllegalArgumentException", false);
	}
	assertHierarchyEquals(
		"Focus: X [in X.java [in p4 [in src [in TypeHierarchy]]]]\n" + 
		"Super types:\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy);
}
/**
 * Ensures that a potential subtype that is not in the classpth is handle correctly.
 * (Regression test for PR #1G4GL9R)
 */
public void testPotentialSubtypeNotInClasspath() throws JavaModelException {
	IJavaProject project = getJavaProject("TypeHierarchy");
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "X.java");
	IType type = cu.getType("X");
	ITypeHierarchy h = type.newTypeHierarchy(project, null);
	IType[] types = h.getSubtypes(type);
	this.assertTypesEqual(
		"Unexpected sub types of X",
		"p1.Y\n",
		types
	);
}
/*
 * Ensures that a type hierarchy on a region contains all subtypes
 * (regression test for bug 47743 Open type hiearchy problems [type hierarchy])
 */
public void testRegion1() throws JavaModelException {
	IPackageFragment pkg = getPackageFragment("TypeHierarchy", "src", "q1");
	IRegion region = JavaCore.newRegion();
	region.add(pkg);
	ITypeHierarchy h = pkg.getJavaProject().newTypeHierarchy(region, null);
	assertTypesEqual(
		"Unexpected types in hierarchy",
		"java.lang.Object\n" + 
		"q1.X\n" +
		"q1.Z\n" +
		"q2.Y\n",
		h.getAllTypes()
	);
}
/*
 * Ensures that a type hierarchy on a region contains all subtypes
 * (regression test for bug 47743 Open type hiearchy problems [type hierarchy])
 */
public void testRegion2() throws JavaModelException {
	IPackageFragment pkg = getPackageFragment("TypeHierarchy", "src", "q2");
	IRegion region = JavaCore.newRegion();
	region.add(pkg);
	ITypeHierarchy h = pkg.getJavaProject().newTypeHierarchy(region, null);
	assertTypesEqual(
		"Unexpected types in hierarchy",
		"java.lang.Object\n" + 
		"q1.X\n" +
		"q2.Y\n",
		h.getAllTypes()
	);
}
/*
 * Ensures that a type hierarchy on a region contains anonymous/local types in this region
 * (regression test for bug 48395 Hierarchy on region misses local classes)
 */
public void testRegion3() throws JavaModelException {
	IPackageFragment pkg = getPackageFragment("TypeHierarchy", "src", "p9");
	IRegion region = JavaCore.newRegion();
	region.add(pkg);
	ITypeHierarchy h = pkg.getJavaProject().newTypeHierarchy(region, null);
	assertTypesEqual(
		"Unexpected types in hierarchy",
		"java.lang.Object\n" + 
		"p9.X\n" + 
		"p9.X$1\n" + 
		"p9.X$Y\n",
		h.getAllTypes()
	);
}
public void testRegion4() throws CoreException {
	try {
		IJavaProject p1 = createJavaProject("P1");
		IJavaProject p2 = createJavaProject("P2", new String[] {""}, new String[] {"JCL_LIB"}, new String[] {"/P1"}, "");
		IJavaProject p3 = createJavaProject("P3", new String[] {""}, new String[] {"JCL_LIB"}, new String[] {"/P1"}, "");
		createFile(
			"/P1/X.java",
			"public class X {\n" +
			"}"
		);
		createFile(
			"/P2/Y.java",
			"public class Y extends X X {\n" +
			"}"
		);
		createFile(
			"/P3/Z.java",
			"public class Z extends X X {\n" +
			"}"
		);
		IRegion region = JavaCore.newRegion();
		region.add(p1);
		region.add(p2);
		region.add(p3);
		ITypeHierarchy hierarchy = JavaCore.newTypeHierarchy(region, null, null);
		assertHierarchyEquals(
			"Focus: <NONE>\n" + 
			"Sub types of root classes:\n" + 
			"  Class [in Class.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"    Y [in Y.java [in <default> [in <project root> [in P2]]]]\n" + 
			"    Z [in Z.java [in <default> [in <project root> [in P3]]]]\n" + 
			"  String [in String.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"    Error [in Error.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"      CloneNotSupportedException [in CloneNotSupportedException.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"        IllegalMonitorStateException [in IllegalMonitorStateException.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"      InterruptedException [in InterruptedException.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"      RuntimeException [in RuntimeException.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"    Exception [in Exception.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"  Throwable [in Throwable.class [in java.lang [in "+ getExternalJCLPathString() + " [in P1]]]]\n" + 
			"  X [in X.java [in <default> [in <project root> [in P1]]]]\n",
			hierarchy);
	} finally {
		deleteProjects(new String[] {"P1", "P2", "P3"});
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=144976
public void testResilienceToMissingBinaries() throws CoreException {
	try {
		createJavaProject("P", new String[] {"src"}, new String[] {"JCL_LIB", "/TypeHierarchy/test144976.jar"}, "bin");
		createFolder("/P/src/tools/");
		createFile(
			"/P/src/tools/DisplayTestResult2.java",
			"pakage tools;\n" +
			"import servlet.*;\n" +
			"public class DisplayTestResult2 extends TmrServlet2 {\n" +
			"}"
		);
		createFolder("/P/src/servlet/");
		createFile(
				"/P/src/servlet/TmrServlet2.java",
				"pakage servlet;\n" +
				"public class TmrServlet2 extends TmrServlet {\n" +
				"}"
			);
		createFile(
				"/P/src/servlet/TmrServlet.java",
				"pakage servlet;\n" +
				"import gk.*;\n" +
				"public class TmrServlet extends GKServlet {\n" +
				"}"
			);
		IType type = getCompilationUnit("P", "src", "tools", "DisplayTestResult2.java").getType("DisplayTestResult2");		
		ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
		System.out.println(hierarchy.getSupertypes(type));
		assertHierarchyEquals(
				"Focus: DisplayTestResult2 [in DisplayTestResult2.java [in tools [in src [in P]]]]\n" + 
				"Super types:\n" + 
				"  TmrServlet2 [in TmrServlet2.java [in servlet [in src [in P]]]]\n" + 
				"    TmrServlet [in TmrServlet.java [in servlet [in src [in P]]]]\n" + 
				"      GKServlet [in GKServlet.class [in gk [in /TypeHierarchy/test144976.jar [in P]]]]\n" + 
				"Sub types:\n",
			hierarchy);
	} finally {
		deleteProject("P");
	}
}
/*
 * Ensures that the focus type is put as a non-resolved type
 * (regression test for bug 92357 ITypeHierarchy#getType() should return an unresolved handle)
 */
public void testResolvedTypeAsFocus() throws CoreException {
	try {
		createJavaProject("P", new String[] {""}, new String[] {"JCL15_LIB"}, "", "1.5");
		String source =
			"public class X {\n" +
			"  Y<String> field;\n" +
			"}\n" +
			"class Y<E> {\n" +
			"}";
		createFile("/P/X.java", source);
		int start = source.indexOf("Y");
		int end = source.indexOf("<String>");
		IJavaElement[] elements = getCompilationUnit("/P/X.java").codeSelect(start, end-start);
		IType focus = (IType) elements[0];
		ITypeHierarchy hierarchy = focus.newTypeHierarchy(null);
		assertElementsEqual(
			"Unexpected focus type in hierarchy", 
			"Y [in X.java [in <default> [in <project root> [in P]]]]",
			new IJavaElement[] {hierarchy.getType()},
			true/*show resolved info*/);
	} finally {
		deleteProject("P");
	}
}
/**
 * Ensures that the superclass can be retrieved for a source type's unqualified superclass.
 */
public void testSourceTypeGetSuperclass() throws JavaModelException {
	//unqualified superclass in a source type
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "Y.java");
	IType type = cu.getType("Y");
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType superclass = h.getSuperclass(type);
	assertTrue("Superclass not found for Y", superclass != null);
	assertEquals("Unexpected super class for Y", "X", superclass.getElementName());
}
/**
 * Ensures that the superclass can be retrieved for a source type's superclass when no superclass is specified
 * in the source type.
 */
public void testSourceTypeGetSuperclass2() throws JavaModelException {
	//no superclass specified for a source type
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "X.java");
	IType type = cu.getType("X");
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType superclass = h.getSuperclass(type);
	assertTrue("Superclass not found for X", superclass != null);
	assertEquals("Unexpected super class for X", "Object", superclass.getElementName());
}
/**
 * Ensures that the superclass can be retrieved for a source type's superclass.
 * This type hierarchy is relatively deep.
 */
public void testSourceTypeGetSuperclass3() throws JavaModelException {
	//no superclass specified for a source type
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "Deep.java");
	IType type = cu.getType("Deep");
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType superclass = h.getSuperclass(type);
	assertTrue("Superclass not found for Deep", superclass != null);
	assertEquals("Unexpected super class for Deep", "Z", superclass.getElementName());
}
/**
 * Ensures that the superclass can be retrieved when it is defined
 * in the same compilation unit.
 */
public void testSourceTypeGetSuperclass4() throws JavaModelException {
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "A.java");
	IType type = cu.getType("A");
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType superclass = h.getSuperclass(type);
	assertTrue("Superclass not found for A", superclass != null);
	assertEquals("Unexpected super class for A", "B", superclass.getElementName());
}

/**
 * Ensures that the superinterfaces can be retrieved for a source type's superinterfaces.
 */
public void testSourceTypeGetSuperInterfaces() throws JavaModelException {
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "Y.java");
	IType type = cu.getType("Y");
	ITypeHierarchy h = type.newSupertypeHierarchy(null);
	IType[] superInterfaces = h.getSuperInterfaces(type);
	assertTypesEqual("Unexpected super interfaces for Y", 
		"p1.I1\n" +
		"p1.I2\n", 
		superInterfaces);
}

/**
 * Ensures that no subclasses exist in a super type hierarchy for the focus type.
 */
public void testSupertypeHierarchyGetSubclasses() throws JavaModelException {
	IType type = getClassFile("TypeHierarchy", getExternalJCLPathString(), "java.lang", "Object.class").getType();
	ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
	IType[] types = hierarchy.getSubclasses(type);
	assertTypesEqual(
		"Unexpected subclasses of Object", 
		"", 
		types);
	
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "Y.java");
	type = cu.getType("Y");
	hierarchy = type.newSupertypeHierarchy(null);
	types = hierarchy.getSubclasses(type);
	assertTypesEqual(
		"Unexpected subclasses of Y", 
		"", 
		types);
}
/**
 * Ensures that no subtypes exist in a super type hierarchy for the focus type.
 */
public void testSupertypeHierarchyGetSubtypes() throws JavaModelException {
	IType type = getClassFile("TypeHierarchy", getExternalJCLPathString(), "java.lang", "Object.class").getType();
	ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
	IType[] types = hierarchy.getSubtypes(type);
	assertTypesEqual(
		"Unexpected subtypes of Object", 
		"", 
		types);
	
	ICompilationUnit cu = getCompilationUnit("TypeHierarchy", "src", "p1", "Y.java");
	type = cu.getType("Y");
	hierarchy = type.newSupertypeHierarchy(null);
	types = hierarchy.getSubtypes(type);
	assertTypesEqual(
		"Unexpected subtypes of Y", 
		"", 
		types);
}
/**
 * Ensures that a super type hierarchy can be created on a working copy.
 * (regression test for bug 3446 type hierarchy: incorrect behavior wrt working copies (1GLDHOA))
 */
public void testSupertypeHierarchyOnWorkingCopy() throws JavaModelException {
	ICompilationUnit cu = this.getCompilationUnit("TypeHierarchy", "src", "wc", "X.java");
	ICompilationUnit workingCopy = null;
	try {
		workingCopy = cu.getWorkingCopy(null);
		workingCopy.createType(
			"class B{\n" +
			"	void m(){\n" +
			"	}\n" +
			"	void f(){\n" +
			"		m();\n" +
			"	}\n" +
			"}\n",
			null,
			true,
			null);
		workingCopy.createType(
			"class A extends B{\n" +
			"	void m(){\n" +
			"	}\n" +
			"}",
			null,
			true,
			null);
		IType typeA = workingCopy.getType("A");
		ITypeHierarchy hierarchy = typeA.newSupertypeHierarchy(null);
		IType typeB = workingCopy.getType("B");
		assertTrue("hierarchy should contain B", hierarchy.contains(typeB));
	} finally {
		if (workingCopy != null) {
			workingCopy.discardWorkingCopy();
		}
	}
}
/*
 * Ensures that creating a hierarchy on a project with classpath problem doesn't throw a NPE
 * (regression test for bug 49809  NPE from MethodVerifier)
 */
public void testSuperTypeHierarchyWithMissingBinary() throws JavaModelException {
	IJavaProject project = getJavaProject("TypeHierarchy");
	IClasspathEntry[] originalClasspath = project.getRawClasspath();
	try {
		int length = originalClasspath.length;
		IClasspathEntry[] newClasspath = new IClasspathEntry[length+1];
		System.arraycopy(originalClasspath, 0, newClasspath, 0, length);
		newClasspath[length] = JavaCore.newLibraryEntry(new Path("/TypeHierarchy/test49809.jar"), null, null);
		project.setRawClasspath(newClasspath, null);
		ICompilationUnit cu = getCompilationUnit("/TypeHierarchy/src/q3/Z.java");
		IType type = cu.getType("Z");
		ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
		assertHierarchyEquals(
				"Focus: Z [in Z.java [in q3 [in src [in TypeHierarchy]]]]\n" + 
				"Super types:\n" + 
				"  Y49809 [in Y49809.class [in p49809 [in test49809.jar [in TypeHierarchy]]]]\n" + 
				"Sub types:\n",
			hierarchy
		);
	} finally {
		project.setRawClasspath(originalClasspath, null);
	}
}
/*
 * Ensures that a hierarchy where the super type is not visible can still be constructed.
 */
public void testVisibility1() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy/src/q6/Y.java").getType("Y");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Y [in Y.java [in q6 [in src [in TypeHierarchy]]]]\n" + 
		"Super types:\n" + 
		"  NonVisibleClass [in X.java [in q5 [in src [in TypeHierarchy]]]]\n" + 
		"    Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
/*
 * Ensures that a hierarchy where the super interface is not visible can still be constructed.
 */
public void testVisibility2() throws JavaModelException {
	IType type = getCompilationUnit("/TypeHierarchy/src/q6/Z.java").getType("Z");
	ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
	assertHierarchyEquals(
		"Focus: Z [in Z.java [in q6 [in src [in TypeHierarchy]]]]\n" + 
		"Super types:\n" + 
		"  NonVisibleInterface [in X.java [in q5 [in src [in TypeHierarchy]]]]\n" + 
		"  Object [in Object.class [in java.lang [in "+ getExternalJCLPathString() + " [in TypeHierarchy]]]]\n" + 
		"Sub types:\n",
		hierarchy
	);
}
}
