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
package org.eclipse.jdt.core.tests.dom;

import java.io.IOException;
import java.util.Map;

import junit.framework.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.*;

public class ASTConverterBugsTest extends ConverterTestSetup {

public void setUpSuite() throws Exception {
	PROJECT_SETUP = true; // do not copy Converter* directories
	super.setUpSuite();
	setUpJCLClasspathVariables("1.5");
	waitUntilIndexesReady();
}

public ASTConverterBugsTest(String name) {
	super(name);
}

public static Test suite() {
	return buildModelTestSuite(ASTConverterBugsTest.class);
}

public ASTNode runConversion(ICompilationUnit unit, boolean resolveBindings) {
	return runConversion(this.testLevel, unit, resolveBindings);
}

public ASTNode runConversion(ICompilationUnit unit, int position, boolean resolveBindings) {
	return runConversion(this.testLevel, unit, position, resolveBindings);
}

public ASTNode runConversion(IClassFile classFile, int position, boolean resolveBindings) {
	return runConversion(this.testLevel, classFile, position, resolveBindings);
}

public ASTNode runConversion(char[] source, String unitName, IJavaProject project) {
	return runConversion(this.testLevel, source, unitName, project);
}

public ASTNode runConversion(char[] source, String unitName, IJavaProject project, boolean resolveBindings) {
	return runConversion(this.testLevel, source, unitName, project, resolveBindings);
}

public ASTNode runConversion(char[] source, String unitName, IJavaProject project, Map options, boolean resolveBindings) {
	return runConversion(this.testLevel, source, unitName, project, options, resolveBindings);
}
public ASTNode runConversion(char[] source, String unitName, IJavaProject project, Map options) {
	return runConversion(this.testLevel, source, unitName, project, options);
}

public ASTNode runConversion(
		ICompilationUnit unit,
		boolean resolveBindings,
		boolean statementsRecovery,
		boolean bindingsRecovery) {
	ASTParser parser = createASTParser();
	parser.setSource(unit);
	parser.setResolveBindings(resolveBindings);
	parser.setStatementsRecovery(statementsRecovery);
	parser.setBindingsRecovery(bindingsRecovery);
	return parser.createAST(null);
}

/**
 * @bug 186410: [dom] StackOverflowError due to endless superclass bindings hierarchy
 * @test Ensures that the superclass of "java.lang.Object" class is null even when it's a recovered binding
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=186410"
 */
public void testBug186410() throws CoreException, IOException {
	try {
		createJavaProject("P", new String[] { "" }, new String[0], "");
			createFile("P/A.java",
				"public class A {\n" +
				"	void method(){}\n" +
				"}"
			);
		ICompilationUnit cuA = getCompilationUnit("P/A.java");
		CompilationUnit unitA = (CompilationUnit) runConversion(cuA, true, false, true);
		AbstractTypeDeclaration typeA = (AbstractTypeDeclaration) unitA.types().get(0);
		ITypeBinding objectType = typeA.resolveBinding().getSuperclass();
		assertEquals("Unexpected superclass", "Object", objectType.getName());
		ITypeBinding objectSuperclass = objectType.getSuperclass();
		assertNull("java.lang.Object should  not have any superclass", objectSuperclass);
	} finally {
		deleteProject("P");
	}
}

public void testBug186410b() throws CoreException, IOException {
	try {
		createJavaProject("P", new String[] { "" }, new String[0], "");
			createFile("P/A.java",
				"public class A {\n" +
				"	Object field;\n" +
				"}"
			);
		ICompilationUnit cuA = getCompilationUnit("P/A.java");
		CompilationUnit unitA = (CompilationUnit) runConversion(cuA, true, false, true);
		AbstractTypeDeclaration type = (AbstractTypeDeclaration) unitA.types().get(0);
		FieldDeclaration field = (FieldDeclaration) type.bodyDeclarations().get(0);
		Type fieldType = field.getType();
		ITypeBinding typeBinding = fieldType.resolveBinding();
		ITypeBinding objectType = typeBinding.createArrayType(2).getElementType();
		assertEquals("Unexpected superclass", "Object", objectType.getName());
		ITypeBinding objectSuperclass = objectType.getSuperclass();
		assertNull("java.lang.Object should  not have any superclass", objectSuperclass);
	} finally {
		deleteProject("P");
	}
}

/**
 * @bug 209510: [dom] Recovered type binding for "java.lang.Object" information are not complete
 * @test Ensures that getPackage() and getQualifiedName() works properly for the "java.lang.Object" recovered binding
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=209510"
 */
public void testBug209510a() throws CoreException, IOException {
	try {
		createJavaProject("P", new String[] { "" }, new String[0], "");
			createFile("P/A.java",
				"public class A {\n" +
				"	void method(){}\n" +
				"}"
			);
		ICompilationUnit cuA = getCompilationUnit("P/A.java");
		CompilationUnit unitA = (CompilationUnit) runConversion(cuA, true, false, true);
		AbstractTypeDeclaration typeA = (AbstractTypeDeclaration) unitA.types().get(0);
		ITypeBinding objectType = typeA.resolveBinding().getSuperclass();
		assertTrue("'java.lang.Object' should be recovered!", objectType.isRecovered());
		assertEquals("Unexpected package for recovered 'java.lang.Object'", "java.lang", objectType.getPackage().getName());
		assertEquals("Unexpected qualified name for recovered 'java.lang.Object'",
		    "java.lang.Object",
		    objectType.getQualifiedName());
	} finally {
		deleteProject("P");
	}
}

public void testBug209510b() throws CoreException, IOException {
	try {
		createJavaProject("P", new String[] { "" }, new String[0], "");
			createFile("P/A.java",
				"public class A {\n" +
				"	Object field;\n" +
				"}"
			);
		ICompilationUnit cuA = getCompilationUnit("P/A.java");
		CompilationUnit unitA = (CompilationUnit) runConversion(cuA, true, false, true);
		AbstractTypeDeclaration type = (AbstractTypeDeclaration) unitA.types().get(0);
		FieldDeclaration field = (FieldDeclaration) type.bodyDeclarations().get(0);
		Type fieldType = field.getType();
		ITypeBinding typeBinding = fieldType.resolveBinding();
		ITypeBinding arrayType = typeBinding.createArrayType(2);
		assertTrue("'java.lang.Object' should be recovered!", arrayType.isRecovered());
		assertNull("Unexpected package for recovered 'array of java.lang.Object'", arrayType.getPackage());
		assertEquals("Unexpected qualified name for recovered 'java.lang.Object'",
		    "java.lang.Object[][]",
		    arrayType.getQualifiedName());
	} finally {
		deleteProject("P");
	}
}

public void testBug209510c() throws CoreException, IOException {
	try {
		createJavaProject("P", new String[] { "" }, new String[0], "");
			createFile("P/A.java",
				"public class A {\n" +
				"	Object[] array;\n" +
				"}"
			);
		ICompilationUnit cuA = getCompilationUnit("P/A.java");
		CompilationUnit unitA = (CompilationUnit) runConversion(cuA, true, false, true);
		AbstractTypeDeclaration type = (AbstractTypeDeclaration) unitA.types().get(0);
		FieldDeclaration field = (FieldDeclaration) type.bodyDeclarations().get(0);
		Type fieldType = field.getType();
		ITypeBinding arrayType = fieldType.resolveBinding();
		assertTrue("'java.lang.Object' should be recovered!", arrayType.isRecovered());
		assertNull("Unexpected package for recovered 'array of java.lang.Object'", arrayType.getPackage());
		assertEquals("Unexpected qualified name for recovered 'java.lang.Object'",
		    "java.lang.Object[]",
		    arrayType.getQualifiedName());
	} finally {
		deleteProject("P");
	}
}

/**
 * @bug 212857: [dom] AST has wrong source range after parameter with array-valued annotation
 * @test Ensures that the method body has the right source range even when there's braces in its header
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=212857"
 */
public void testBug212857() throws CoreException, IOException {
	workingCopies = new ICompilationUnit[1];
	String source = "package xy;\n" + 
	"public class C {\n" + 
	"	void m(@SuppressWarnings({\"unused\", \"bla\"}) int arg) {\n" + 
	"		int local;\n" + 
	"	}\n" + 
	"}\n";
	workingCopies[0] = getWorkingCopy("/Converter15/src/xy/C.java", source);
	CompilationUnit unit = (CompilationUnit) runConversion(workingCopies[0], true, false, true);
	MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(unit, 0, 0);
	checkSourceRange(methodDeclaration.getBody(),
		"{\n" + 
		"		int local;\n" + 
		"	}",
		source
	);
}
public void testBug212857a() throws CoreException, IOException {
	workingCopies = new ICompilationUnit[1];
	String source = "package xy;\n" + 
	"public class C {\n" + 
	"	@SuppressWarnings({\"unused\", \"bla\"}) void m() {\n" + 
	"		int local;\n" + 
	"	}\n" + 
	"}\n";
	workingCopies[0] = getWorkingCopy("/Converter15/src/xy/C.java", source);
	CompilationUnit unit = (CompilationUnit) runConversion(workingCopies[0], true, false, true);
	MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(unit, 0, 0);
	checkSourceRange(methodDeclaration.getBody(),
		"{\n" + 
		"		int local;\n" + 
		"	}",
		source
	);
}
// tests with recovery
public void testBug212857b() throws CoreException, IOException {
	workingCopies = new ICompilationUnit[1];
	String source = "package test;\n" + 
	"public class X {\n" + 
	"	void m() \n" + 
	"		if (arg == 0) {}\n" + 
	"	}\n" + 
	"}\n";
	workingCopies[0] = getWorkingCopy("/Converter15/src/test/X.java", source);
	CompilationUnit unit = (CompilationUnit) runConversion(workingCopies[0], true, false, true);
	MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(unit, 0, 0);
	checkSourceRange(methodDeclaration.getBody(),
		" \n" + 
		"		if (arg == 0) {}\n" + 
		"	}",
		source
	);
}
public void testBug212857c() throws CoreException, IOException {
	workingCopies = new ICompilationUnit[1];
	String source = "package test;\n" + 
	"public class X {\n" + 
	"	void m() \n" + 
	"	}\n" + 
	"}\n";
	workingCopies[0] = getWorkingCopy("/Converter15/src/test/X.java", source);
	CompilationUnit unit = (CompilationUnit) runConversion(workingCopies[0], true, false, true);
	MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(unit, 0, 0);
	checkSourceRange(methodDeclaration.getBody(),
		" \n" + 
		"	}",
		source
	);
}
public void testBug212857d() throws CoreException, IOException {
	workingCopies = new ICompilationUnit[1];
	String source = "package test;\n" + 
	"public class X {\n" + 
	"	void m(String str) \n" + 
	"		if (arg == 0) {}\n" + 
	"	}\n" + 
	"}\n";
	workingCopies[0] = getWorkingCopy("/Converter15/src/test/X.java", source);
	CompilationUnit unit = (CompilationUnit) runConversion(workingCopies[0], true, false, true);
	MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(unit, 0, 0);
	checkSourceRange(methodDeclaration.getBody(),
		" \n" + 
		"		if (arg == 0) {}\n" + 
		"	}",
		source
	);
}
public void testBug212857e() throws CoreException, IOException {
	workingCopies = new ICompilationUnit[1];
	String source = "package test;\n" + 
	"public class X {\n" + 
	"	void m(Object obj, int x) \n" + 
	"	}\n" + 
	"}\n";
	workingCopies[0] = getWorkingCopy("/Converter15/src/test/X.java", source);
	CompilationUnit unit = (CompilationUnit) runConversion(workingCopies[0], true, false, true);
	MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(unit, 0, 0);
	checkSourceRange(methodDeclaration.getBody(),
		" \n" + 
		"	}",
		source
	);
}

}
