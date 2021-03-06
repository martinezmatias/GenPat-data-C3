/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.dom;

import java.util.List;

import junit.framework.Test;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ASTConverter18Test extends ConverterTestSetup {

	ICompilationUnit workingCopy;

	public void setUpSuite() throws Exception {
		super.setUpSuite();
		this.ast = AST.newAST(AST.JLS8);
	}

	public ASTConverter18Test(String name) {
		super(name);
	}

	static {
//		TESTS_NUMBERS = new int[] { 19 };
//		TESTS_RANGE = new int[] { 1, -1 };
//		TESTS_NAMES = new String[] {"test0001"};
	}
	public static Test suite() {
		return buildModelTestSuite(ASTConverter18Test.class);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (this.workingCopy != null) {
			this.workingCopy.discardWorkingCopy();
			this.workingCopy = null;
		}
	}

	/*
	 * Type Annotations on Variable Arguments
	 */
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391898
	public void test0001() throws JavaModelException {
		String contents =
			"    @java.lang.annotation.Target (java.lang.annotation.ElementType.TYPE_USE)\n" +
			"    @interface Marker {\n" +
			"    }\n" +
			"public class X {\n" +
			"	public void foo(int @Marker... args) {\n" +
			"	}\n" +
			" 	public void bar(@Marker int @Marker... args) {\n" +
			" 	}\n" +
			"}";
		this.workingCopy = getWorkingCopy("/Converter18/src/X.java", true/*resolve*/);
		ASTNode node = buildAST(
			contents,
			this.workingCopy);
		assertEquals("Not a compilation unit", ASTNode.COMPILATION_UNIT, node.getNodeType());
		CompilationUnit compilationUnit = (CompilationUnit) node;
		assertProblemsSize(compilationUnit, 0);
		node = getASTNode(compilationUnit, 1, 0);
		assertTrue("Not a method declaration", node.getNodeType() == ASTNode.METHOD_DECLARATION);
		MethodDeclaration methodDeclaration = (MethodDeclaration) node;
		List parameters = methodDeclaration.parameters();
		assertEquals("wrong size", 1, parameters.size());
		SingleVariableDeclaration parameter = (SingleVariableDeclaration) parameters.get(0);
		List annotations = parameter.varargsAnnotations();
		assertEquals("Wrong number of annotations", 1, annotations.size());
		ASTNode annotation = (ASTNode) annotations.get(0);
		checkSourceRange(annotation, "@Marker", contents);
		node = getASTNode(compilationUnit,1,1);
		assertTrue("Not a method declaration", node.getNodeType() == ASTNode.METHOD_DECLARATION);
		parameters = methodDeclaration.parameters();
		assertEquals("Wrong number of parameters", 1, parameters.size());
		parameter = (SingleVariableDeclaration) parameters.get(0);
		annotations = parameter.varargsAnnotations();
		assertEquals("Wrong number of annotations", 1, annotations.size());
		annotation = (ASTNode) annotations.get(0);
		checkSourceRange(annotation, "@Marker", contents);
	}
	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=395886 tests annotations on
	 * QTR in multiple scenarios of occurrence.
	 * 
	 * @throws JavaModelException
	 */
	public void test0002() throws JavaModelException {
		this.workingCopy = getWorkingCopy("/Converter18/src/test0002/X.java",
				true/* resolve */);
		String contents = "package test0002;\n"
				+ "import java.lang.annotation.Target;\n"
				+ "public class X {\n"
				+ "	public static void main(String[] args) {\n"
				+ "		Outer outer = new Outer();\n"
				+ "		Object myObject = new Object();\n"
				+ "		String myString;\n"
				+ "		myString = (java.lang.@Marker String) myObject;\n"
				+ "		Outer.Inner first = outer.new Inner();\n"
				+ "		Outer. @Marker2 Inner second = outer.new Inner() ;\n"
				+ "		Outer.Inner. @Marker1 Deeper deeper = second.new Deeper();\n"
				+ "		Outer.@Marker1 Inner.@Marker2 Deeper deeper2 =  second.new Deeper();\n"
				+ "	}\n" + "}\n" + "class Outer {\n"
				+ "	public class Inner {\n" + "		public class Deeper {\n"
				+ "		}\n" + "	}\n" + "}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker {}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker1 {}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker2 {}\n";
		CompilationUnit cu = (CompilationUnit) buildAST(contents,
				this.workingCopy);
		MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(cu, 0, 0);
		List statements = methodDeclaration.getBody().statements();
		int sCount = 3;

		// case 1 - annotation just before the last field
		ExpressionStatement expressionStatement = (ExpressionStatement) statements.get(sCount++);
		Assignment assignment = (Assignment) expressionStatement.getExpression();
		assertNotNull(assignment);
		CastExpression castExpression = (CastExpression) assignment.getRightHandSide();
		assertNotNull(castExpression);
		SimpleType simpleType = (SimpleType) castExpression.getType();
		assertNotNull(simpleType);
		assertEquals("java.lang.@Marker String", simpleType.toString());
		List annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		assertEquals("@Marker", annotations.get(0).toString());

		// case 2 - QualifiedType without annotations.
		VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statements.get(sCount++);
		Type type = variableDeclarationStatement.getType();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		assertEquals("Outer.Inner", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 0);

		// case 3 - Qaulified Type with outer without annotations and inner with
		// annotations.
		variableDeclarationStatement = (VariableDeclarationStatement) statements.get(sCount++);
		type = variableDeclarationStatement.getType();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		assertNotNull(simpleType);
		assertEquals("Outer.@Marker2 Inner", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		assertEquals("@Marker2", annotations.get(0).toString());

		// case 4 - Multiple levels with annotations at the last only.
		variableDeclarationStatement = (VariableDeclarationStatement) statements.get(sCount++);
		type = variableDeclarationStatement.getType();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		assertNotNull(simpleType);
		assertEquals("Outer.Inner.@Marker1 Deeper", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		assertEquals("@Marker1", annotations.get(0).toString());

		// case 5 - Multiple annotations
		variableDeclarationStatement = (VariableDeclarationStatement) statements.get(sCount++);
		type = variableDeclarationStatement.getType();
		assertTrue(type.isQualifiedType());
		QualifiedType qualifiedType = (QualifiedType) type;
		assertNotNull(qualifiedType);
		assertEquals("Outer.@Marker1 Inner.@Marker2 Deeper", qualifiedType.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 1);
		assertEquals("@Marker2", annotations.get(0).toString());
		SimpleName simpleName = qualifiedType.getName();
		assertEquals("Deeper", simpleName.toString());
		Type qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isSimpleType());
		simpleType = (SimpleType) qualifierType;
		assertEquals("Outer.@Marker1 Inner", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		assertEquals("@Marker1", annotations.get(0).toString());
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=395886 tests the
	 * representation of type annotations on a possible JAVA 7 and 8 place.
	 * 
	 * @throws JavaModelException
	 */
	public void test0003() throws JavaModelException {
		this.workingCopy = getWorkingCopy("/Converter18/src/test0003/X.java",
				true/* resolve */);
		String contents = "package test0003;\n"
				+ "import java.lang.annotation.Target;\n"
				+ "public class X {\n"
				+ "	public static void main(String[] args) {\n"
				+ "		@Marker Outer.Inner first[] = new Outer.Inner[1];\n"
				+ "	}\n" + "}\n" + "class Outer {\n"
				+ "	public class Inner {\n" + "	}\n" + "}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker {}\n";
		CompilationUnit cu = (CompilationUnit) buildAST(contents, this.workingCopy);
		MethodDeclaration methodDeclaration = (MethodDeclaration) getASTNode(cu, 0, 0);
		List statements = methodDeclaration.getBody().statements();
		int sCount = 0;

		// Current design expects annotation only at the JAVA 7 place if it is
		// expected at JAVA 8.
		VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statements.get(sCount);
		List modifiers = variableDeclarationStatement.modifiers();
		assertTrue(modifiers.size() == 1);
		Annotation annotation = (Annotation) modifiers.get(0);
		assertEquals("@Marker", annotation.toString());
		Type type = variableDeclarationStatement.getType();
		assertTrue(type.isSimpleType());
		SimpleType simpleType = (SimpleType) type;
		assertEquals("Outer.Inner", simpleType.toString());
		List annotations = simpleType.annotations();
		assertTrue(annotations.size() == 0);
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=395886 tests QTR with
	 * annotations
	 * 
	 * @throws JavaModelException
	 */
	public void test0004() throws JavaModelException {
		this.workingCopy = getWorkingCopy("/Converter18/src/test0004/X.java",
				true/* resolve */);
		String contents = "package test0004;"
				+ "import java.lang.annotation.Target;\n"
				+ "public class X implements One</*start*/@Marker1 Outer<Integer>. @Marker2 Inner<Double>[]/*end*/> {\n"
				+ "}\n" + "interface One<T> {}\n" + "class Outer<T> {\n"
				+ "	public class Inner<S> {}\n" + "}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker1 {}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker2 {}\n";
		CompilationUnit cu = (CompilationUnit) buildAST(contents, this.workingCopy);
		TypeDeclaration typedeclaration = (TypeDeclaration) getASTNode(cu, 0);
		ArrayType type = (ArrayType) ((ParameterizedType) typedeclaration.superInterfaceTypes().get(0)).typeArguments().get(0);
		assertNotNull("No annotation", type);
		ITypeBinding binding = type.resolveBinding();
		assertNotNull("No binding", binding);
		assertEquals("Wrong qualified name", "test0004.Outer<java.lang.Integer>.Inner<java.lang.Double>[]", binding.getQualifiedName());
		Type componentType = type.getComponentType();
		binding = componentType.resolveBinding();
		assertNotNull("No binding", binding);
		assertEquals("Wrong qualified name",
				"test0004.Outer<java.lang.Integer>.Inner<java.lang.Double>", binding.getQualifiedName());
		assertTrue("Not parameterized", componentType.isParameterizedType());
		ParameterizedType parameterizedType = (ParameterizedType) componentType;
		Type type2 = parameterizedType.getType();
		assertTrue("Not qualified", type2.isQualifiedType());
		QualifiedType qualifiedType = (QualifiedType) type2;
		binding = qualifiedType.resolveBinding();
		assertNotNull("No binding", binding);
		assertEquals("Wrong qualified name","test0004.Outer<java.lang.Integer>.Inner<java.lang.Double>", binding.getQualifiedName());
		Type qualifier = qualifiedType.getQualifier();
		assertTrue("Not parameterized", qualifier.isParameterizedType());
		binding = qualifier.resolveBinding();
		assertNotNull("No binding", binding);
		assertEquals("Wrong qualified name", "test0004.Outer<java.lang.Integer>", binding.getQualifiedName());
		parameterizedType = (ParameterizedType) qualifier;
		type2 = parameterizedType.getType();
		assertTrue("Not simple type", type2.isSimpleType());
		binding = type2.resolveBinding();
		assertNotNull("No binding", binding);
		assertEquals("Wrong qualified name","test0004.Outer<java.lang.Integer>", binding.getQualifiedName());
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=395886 tests QTR with
	 * annotations
	 * 
	 * @throws JavaModelException
	 */
	public void test0005() throws JavaModelException {
		this.workingCopy = getWorkingCopy("/Converter18/src/test0005/X.java",
				true/* resolve */);
		String contents = "package test0005;"
				+ "import java.lang.annotation.Target;\n"
				+ "public class X implements One< Outer.Inner > {\n"
				+ "}\n"
				+ "class Y implements One< Outer. @Marker1 Inner > {\n"
				+ "}\n"
				+ "class Z implements One< @Marker1 Outer.Inner > {\n"
				+ "}\n"
				+ "class W implements One< @Marker1 Outer. @Marker2 Inner > {\n"
				+ "}\n" + "interface One<T> {}\n" + "class Outer {\n"
				+ "	public class Inner {}\n" + "}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker1 {}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker2 {}\n";
		CompilationUnit cu = (CompilationUnit) buildAST(contents,
				this.workingCopy);
		int tCount = 0;

		// case 1 - no annotations Outer.Inner
		TypeDeclaration typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		ParameterizedType parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		List typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		Type type = (Type) typeArguments.get(0);
		assertTrue(type.isSimpleType());
		assertEquals("Outer.Inner", type.toString());

		// case 2 - QTR with one annotation Outer.@Marker1 Inner
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration
				.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		type = (Type) typeArguments.get(0);
		assertTrue(type.isSimpleType());
		SimpleType simpleType = (SimpleType) type;
		assertEquals("Outer.@Marker1 Inner", simpleType.toString());
		List annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		Annotation annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

		// case 3 - QTR with one annotation at the beginning @Marker1
		// Outer.Inner
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		type = (Type) typeArguments.get(0);
		assertTrue(type.isQualifiedType());
		assertEquals("@Marker1 Outer.Inner", type.toString());
		QualifiedType qualifiedType = (QualifiedType) type;
		assertEquals("Inner", qualifiedType.getName().toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		Type qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isSimpleType());
		simpleType = (SimpleType) qualifierType;
		assertEquals("@Marker1 Outer", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

		// case 4 - QTR with annotations at both the types @Marker1
		// Outer.@Marker2 Inner
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		type = (Type) typeArguments.get(0);
		assertTrue(type.isQualifiedType());
		assertEquals("@Marker1 Outer.@Marker2 Inner", type.toString());
		qualifiedType = (QualifiedType) type;
		assertEquals("Inner", qualifiedType.getName().toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker2", annotation.toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isSimpleType());
		simpleType = (SimpleType) qualifierType;
		assertEquals("@Marker1 Outer", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=395886 tests PQTR with
	 * annotations part
	 * 
	 * @throws JavaModelException
	 */
	public void test0006() throws JavaModelException {
		this.workingCopy = getWorkingCopy("/Converter18/src/test0006/X.java",
				true);
		String contents = "package test0006;"
				+ "import java.lang.annotation.Target;\n"
				+ "public class X implements One<Outer.  Inner.Deeper<Double>> {\n"
				+ "}\n"
				+ "class X1 implements One<Outer. @Marker1 Inner.Deeper<Double>> {\n"
				+ "}\n"
				+ "class X2 implements One<Outer. @Marker1 Inner.@Marker2 Deeper<Double>> {\n"
				+ "}\n"
				+ "class X3 implements One<@Marker1 Outer. @Marker2 Inner. Deeper<Double>> {\n"
				+ "}\n"
				+ "class Y implements One<Outer1. Inner<Integer>. Deeper<Double>> {\n"
				+ "}\n"
				+ "class Y1 implements One<Outer1. Inner<Integer>. @Marker1 Deeper<Double>> {\n"
				+ "}\n"
				+ "class Y2 implements One<Outer1. @Marker1 Inner<Integer>. Deeper<Double>> {\n"
				+ "}\n"
				+ "class Y3 implements One<@Marker1 Outer1. Inner<Integer>. Deeper<Double>> {\n"
				+ "}\n"
				+ "class Y4 implements One<@Marker1 Outer1. @Marker2 Inner<Integer>. Deeper<Double>> {\n"
				+ "}\n"
				+ "class Z implements One<Outer2<Integer>.Inner.Deeper<Double>> {\n"
				+ "}\n"
				+ "class Z1 implements One<@Marker1 Outer2<Integer>.Inner.Deeper<Double>> {\n"
				+ "}\n"
				+ "class Z2 implements One<Outer2<Integer>. @Marker1 Inner.@Marker2 Deeper<Double>> {\n"
				+ "}\n"
				+ "class W implements One<Outer3<Double>. @Marker1 @Marker2 Inner<Integer, Character>. Deeper<Double>> {\n"
				+ "}\n" + "interface One<T> {}\n" + "class Outer {\n"
				+ "	public class Inner {\n"
				+ "       public class Deeper<S> {\n" + "       }\n" + "   }\n"
				+ "}\n" + "class Outer1 {\n" + "	public class Inner<T> {\n"
				+ "       public class Deeper<S> {\n" + "       }\n" + "   }\n"
				+ "}\n" + "class Outer2 <T> {\n" + "	public class Inner {\n"
				+ "       public class Deeper<S> {}\n" + "   }\n" + "}\n"
				+ "class Outer3 <T> {\n" + "	public class Inner<K, V> {\n"
				+ "       public class Deeper<S> {}\n" + "   }\n" + "}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker1 {}\n"
				+ "@Target (java.lang.annotation.ElementType.TYPE_USE)\n"
				+ "@interface Marker2 {}\n";
		CompilationUnit cu = (CompilationUnit) buildAST(contents,
				this.workingCopy);
		int tCount = 0;

		// case 1: vanilla case without annotations and with single typeArgument
		// Outer.Inner.Deeper<Double>
		TypeDeclaration typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		ParameterizedType parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		List typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer.Inner.Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		Type type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isSimpleType());
		SimpleType simpleType = (SimpleType) type;
		assertEquals("Outer.Inner.Deeper", simpleType.toString());
		Name name = simpleType.getName();
		assertTrue(name.isQualifiedName());
		QualifiedName qualifiedName = (QualifiedName) name;
		assertEquals("Outer.Inner.Deeper", qualifiedName.toString());

		// case 2 - One annotation after the first class
		// Outer. @Marker1 Inner.Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer.@Marker1 Inner.Deeper<Double>",	parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		QualifiedType qualifiedType = (QualifiedType) type;
		assertEquals("Outer.@Marker1 Inner.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		List annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		Type qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isSimpleType());
		simpleType = (SimpleType) qualifierType;
		assertEquals("Outer.@Marker1 Inner", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		Annotation annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

		// case 3 - Inner types annotated with outer not annotated with last
		// type arg
		// Outer. @Marker1 Inner.@Marker2 Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer.@Marker1 Inner.@Marker2 Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Outer.@Marker1 Inner.@Marker2 Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker2", annotation.toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isSimpleType());
		simpleType = (SimpleType) qualifierType;
		assertEquals("Outer.@Marker1 Inner", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

		// case 4 - one annotation on the outermost, one in middle and one
		// typearg in innermost
		// @Marker1 Outer. @Marker2 Inner. Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration
				.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("@Marker1 Outer.@Marker2 Inner.Deeper<Double>", parametrizedType.toString());
		ITypeBinding typeBinding = parametrizedType.resolveBinding();
		assertNotNull("Binding non-null", typeBinding);
		assertEquals("wrong qualified name", "test0006.Outer.Inner.Deeper<java.lang.Double>", typeBinding.getQualifiedName());
		assertTrue("Not a Parameterized Type", typeBinding.isParameterizedType());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("@Marker1 Outer.@Marker2 Inner.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isQualifiedType());
		qualifiedType = (QualifiedType) qualifierType;
		assertEquals("@Marker1 Outer.@Marker2 Inner", qualifierType.toString());
		typeBinding = qualifiedType.resolveBinding();
		assertNotNull("Binding non-null", typeBinding);
		typeBinding = qualifiedType.resolveBinding();
		assertEquals("wrong qualified name", "test0006.Outer.Inner", typeBinding.getQualifiedName());
		assertTrue(qualifierType.isAnnotatable());
		AnnotatableType annotatableType = (AnnotatableType) qualifierType;
		annotations = annotatableType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker2", annotation.toString());
		name = qualifiedType.getName();
		assertEquals("Inner", name.toString());
		type = qualifiedType.getQualifier();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		assertEquals("@Marker1 Outer", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

		// case 5 - without annotations, but with typeargs at second and third
		// types
		// Outer1. Inner<Integer>. Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer1.Inner<Integer>.Deeper<Double>",parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Outer1.Inner<Integer>.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		assertEquals("Outer1.Inner<Integer>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		name = simpleType.getName();
		assertTrue(name.isQualifiedName());
		qualifiedName = (QualifiedName) name;
		assertEquals("Outer1.Inner", qualifiedName.toString());

		// case 6 - Annot in between two PQRT with outermost neither annotated
		// nor having typeargs
		// Outer1. Inner<Integer>. @Marker1 Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer1.Inner<Integer>.@Marker1 Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Outer1.Inner<Integer>.@Marker1 Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		assertEquals("Outer1.Inner<Integer>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		name = simpleType.getName();
		assertTrue(name.isQualifiedName());
		qualifiedName = (QualifiedName) name;
		assertEquals("Outer1.Inner", qualifiedName.toString());

		// case 7 - Outermost still empty (no annotations, no type args),
		// followed by annotation, and then typeargs
		// Outer1. @Marker1 Inner<Integer>. Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer1.@Marker1 Inner<Integer>.Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Outer1.@Marker1 Inner<Integer>.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		assertEquals("Outer1.@Marker1 Inner<Integer>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		assertEquals("Outer1.@Marker1 Inner", simpleType.toString());
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

		// case 8 - Similar to above, but with the major difference of
		// annotation shifted to outermost.
		// @Marker1 Outer1. Inner<Integer>. Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("@Marker1 Outer1.Inner<Integer>.Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("@Marker1 Outer1.Inner<Integer>.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		assertEquals("@Marker1 Outer1.Inner<Integer>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("@Marker1 Outer1.Inner", qualifiedType.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		name = qualifiedType.getName();
		assertTrue(name.isSimpleName());
		assertEquals("Inner", name.toString());
		type = qualifiedType.getQualifier();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());
		name = simpleType.getName();
		assertTrue(name.isSimpleName());
		assertEquals("Outer1", name.toString());

		// case 9: scenario of the above case plus another annotation at
		// mid-level.
		// @Marker1 Outer1.@Marker2 Inner<Integer>. Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("@Marker1 Outer1.@Marker2 Inner<Integer>.Deeper<Double>",parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("@Marker1 Outer1.@Marker2 Inner<Integer>.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		assertEquals("@Marker1 Outer1.@Marker2 Inner<Integer>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("@Marker1 Outer1.@Marker2 Inner", qualifiedType.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker2", annotation.toString());
		name = qualifiedType.getName();
		assertTrue(name.isSimpleName());
		assertEquals("Inner", name.toString());
		type = qualifiedType.getQualifier();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());
		name = simpleType.getName();
		assertTrue(name.isSimpleName());
		assertEquals("Outer1", name.toString());

		// case 10 - PQRT with two type args but without annotations
		// Outer2<Integer>.Inner.Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer2<Integer>.Inner.Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Outer2<Integer>.Inner.Deeper", qualifiedType.toString());
		ITypeBinding binding = qualifiedType.resolveBinding();
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isQualifiedType());
		qualifiedType = (QualifiedType) qualifierType;
		binding = qualifiedType.resolveBinding();
		assertEquals("Outer2<Integer>.Inner", qualifiedType.toString());
		assertEquals("wrong qualified binding", "test0006.Outer2<java.lang.Integer>.Inner",	binding.getQualifiedName());
		name = qualifiedType.getName();
		assertEquals("Inner", name.toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertEquals("Outer2", type.toString());

		// case 11 - annotation at outermost in addition to scenario in case 10.
		// @Marker1 Outer2<Integer>.Inner.Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("@Marker1 Outer2<Integer>.Inner.Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("@Marker1 Outer2<Integer>.Inner.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isQualifiedType());
		qualifiedType = (QualifiedType) qualifierType;
		assertEquals("@Marker1 Outer2<Integer>.Inner", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Inner", name.toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertEquals("@Marker1 Outer2", type.toString());
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		annotations = simpleType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());

		// case 12 - No annotations at outermost, but outermost has
		// typeAnnotations.
		// Outer2<Integer>. @Marker1 Inner.@Marker2 Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer2<Integer>.@Marker1 Inner.@Marker2 Deeper<Double>", parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Outer2<Integer>.@Marker1 Inner.@Marker2 Deeper",qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker2", annotation.toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isQualifiedType());
		qualifiedType = (QualifiedType) qualifierType;
		assertEquals("Outer2<Integer>.@Marker1 Inner", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Inner", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 1);
		annotation = (Annotation) annotations.get(0);
		assertEquals("@Marker1", annotation.toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Integer", type.toString());
		type = parametrizedType.getType();
		assertEquals("Outer2", type.toString());
		assertTrue(type.isSimpleType());

		// case 13 - a list of annotations and multiple typeArgument element
		// lists.
		// Outer3<Double>. @Marker1 @Marker2 Inner<Integer, Character>.
		// Deeper<Double>
		typedeclaration = (TypeDeclaration) getASTNode(cu, tCount++);
		parametrizedType = (ParameterizedType) typedeclaration.superInterfaceTypes().get(0);
		typeArguments = parametrizedType.typeArguments();
		assertEquals(1, typeArguments.size());
		parametrizedType = (ParameterizedType) typeArguments.get(0);
		assertEquals("Outer3<Double>.@Marker1 @Marker2 Inner<Integer,Character>.Deeper<Double>",parametrizedType.toString());
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		type = (Type) typeArguments.get(0);
		assertEquals("Double", type.toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Outer3<Double>.@Marker1 @Marker2 Inner<Integer,Character>.Deeper", qualifiedType.toString());
		name = qualifiedType.getName();
		assertEquals("Deeper", name.toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 0);
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 2);
		assertEquals("Integer", typeArguments.get(0).toString());
		assertEquals("Character", typeArguments.get(1).toString());
		type = parametrizedType.getType();
		assertTrue(type.isQualifiedType());
		qualifiedType = (QualifiedType) type;
		assertEquals("Inner", qualifiedType.getName().toString());
		annotations = qualifiedType.annotations();
		assertTrue(annotations.size() == 2);
		assertEquals("@Marker1", annotations.get(0).toString());
		assertEquals("@Marker2", annotations.get(1).toString());
		qualifierType = qualifiedType.getQualifier();
		assertTrue(qualifierType.isParameterizedType());
		parametrizedType = (ParameterizedType) qualifierType;
		typeArguments = parametrizedType.typeArguments();
		assertTrue(typeArguments.size() == 1);
		assertEquals("Double", typeArguments.get(0).toString());
		type = parametrizedType.getType();
		assertTrue(type.isSimpleType());
		simpleType = (SimpleType) type;
		assertEquals("Outer3", simpleType.toString());
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391893
	public void test0007() throws JavaModelException {
		String contents =
			"public class X {\n" +
			"	public void foo(@Marker @Marker2 X this, @Marker2 @Marker int i){}\n" +
			"}\n" +
			"@java.lang.annotation.Target (java.lang.annotation.ElementType.TYPE_USE)\n" +
			"@interface Marker {}\n" +
			"@java.lang.annotation.Target (java.lang.annotation.ElementType.TYPE_USE)\n" +
			"@interface Marker2 {}";
		this.workingCopy = getWorkingCopy("/Converter18/src/X.java", true);
		ASTNode node = buildAST(contents, this.workingCopy);
		assertEquals("Not a compilation unit", ASTNode.COMPILATION_UNIT, node.getNodeType());
		CompilationUnit unit = (CompilationUnit) node;
		node = getASTNode(unit, 0, 0);
		assertEquals("Not a method Declaration", ASTNode.METHOD_DECLARATION, node.getNodeType());
		MethodDeclaration method = (MethodDeclaration) node;
		AnnotatableType receiver = method.getReceiverType();
		assertEquals("Not an annotatable type", ASTNode.SIMPLE_TYPE, receiver.getNodeType());
		assertEquals("Incorrect receiver signature", "@Marker @Marker2 X", ((SimpleType) receiver).toString());
		assertEquals("Incorrect annotations on receiver", 2, ((SimpleType) receiver).annotations().size());
		assertNull("Incorrect receiver qualfier", method.getReceiverQualifier());
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391893
	public void test0008() throws JavaModelException {
		String contents =
			"public class X {\n" +
			"	class Y {\n" +
			"		public Y(@Marker @Marker2 X X.this, @Marker2 @Marker int i){}\n" +
			"	}\n" +
			"}\n" +
			"@java.lang.annotation.Target (java.lang.annotation.ElementType.TYPE_USE)\n" +
			"@interface Marker {}\n" +
			"@java.lang.annotation.Target (java.lang.annotation.ElementType.TYPE_USE)\n" +
			"@interface Marker2 {}";

		this.workingCopy = getWorkingCopy("/Converter18/src/X.java", true);
		ASTNode node = buildAST(contents, this.workingCopy);
		assertEquals("Not a compilation unit", ASTNode.COMPILATION_UNIT, node.getNodeType());
		CompilationUnit unit = (CompilationUnit) node;
		node = getASTNode(unit, 0, 0);
		assertEquals("Not a type Declaration", ASTNode.TYPE_DECLARATION, node.getNodeType());
		TypeDeclaration innerType = (TypeDeclaration) node;
		assertEquals("Incorrect no of methods", 1, innerType.getMethods().length);
		MethodDeclaration method = innerType.getMethods()[0];
		AnnotatableType receiver = method.getReceiverType();
		assertEquals("Not an annotatable type", ASTNode.SIMPLE_TYPE, receiver.getNodeType());
		assertEquals("Incorrect receiver signature", "@Marker @Marker2 X", ((SimpleType) receiver).toString());
		assertEquals("Incorrect annotations on receiver", 2, ((SimpleType) receiver).annotations().size());
		assertNotNull("Incorrect receiver qualfier", method.getReceiverQualifier());
		assertEquals("Incorrect receiver qualfier", "X", method.getReceiverQualifier().getFullyQualifiedName());
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391895
	public void test0009() throws JavaModelException {
		String contents =
				"import java.lang.annotation.ElementType;\n" +
						"public class X {\n" +
						" 	class Y {\n" +
						"		@Annot int @Annot1 [] a @Annot2 @Annot3 [] @Annot3 @Annot2 [] @Annot4 [], b @Annot2 @Annot3 [] @Annot4 [], c [][][];\n" +  
						"		public void foo1(@Annot int @Annot1 [] p @Annot2 @Annot3 [] @Annot3 @Annot2 [] @Annot4 @Annot3 []) {}\n" +
						"		public void foo2(@Annot int p [][]) {}\n" +
						"		@Annot String @Annot1 [] foo3() @Annot1 @Annot2 [][] { return null; }\n" + 
						"	}\n" +
						"}\n" +
						"@java.lang.annotation.Target(value = {ElementType.TYPE_USE})\n" + 
						"@interface Annot {}\n" +
						"@java.lang.annotation.Target(value = {ElementType.TYPE_USE})\n" + 
						"@interface Annot1 {}\n" +
						"@java.lang.annotation.Target(value = {ElementType.TYPE_USE})\n" +
						"@interface Annot2 {}\n" +
						"@java.lang.annotation.Target(value = {ElementType.TYPE_USE})\n" + 
						"@interface Annot3 {}\n" +
						"@java.lang.annotation.Target(value = {ElementType.TYPE_USE})\n" +
						"@interface Annot4 {}";
		this.workingCopy = getWorkingCopy("/Converter18/src/X.java", true);
		ASTNode node = buildAST(contents, this.workingCopy);
		assertEquals("Not a compilation unit", ASTNode.COMPILATION_UNIT, node.getNodeType());
		CompilationUnit unit = (CompilationUnit) node;
		node = getASTNode(unit, 0, 0);
		assertEquals("Not a type Declaration", ASTNode.TYPE_DECLARATION, node.getNodeType());
		TypeDeclaration type = (TypeDeclaration) node;
		FieldDeclaration field = type.getFields()[0];
		List fragments = field.fragments();
		assertEquals("Incorrect no of fragments", 3, fragments.size());
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
		assertExtraDimensionsEqual("Incorrect dimension info", fragment.getExtraDimensionInfos(), "@Annot2 @Annot3 [] @Annot3 @Annot2 [] @Annot4 []");
		fragment = (VariableDeclarationFragment) fragments.get(1);
		assertExtraDimensionsEqual("Incorrect dimension info", fragment.getExtraDimensionInfos(), "@Annot2 @Annot3 [] @Annot4 []");		
		fragment = (VariableDeclarationFragment) fragments.get(2);
		assertExtraDimensionsEqual("Incorrect dimension info", fragment.getExtraDimensionInfos(), "[] [] []");
		MethodDeclaration[] methods = type.getMethods();
		assertEquals("Incorrect no of methods", 3, methods.length);
		MethodDeclaration method = methods[0];
		List parameters = method.parameters();
		assertEquals("Incorrect no of parameters", 1, parameters.size());
		assertExtraDimensionsEqual("Incorrect dimension info", ((SingleVariableDeclaration) parameters.get(0)).getExtraDimensionInfos(), "@Annot2 @Annot3 [] @Annot3 @Annot2 [] @Annot4 @Annot3 []");

		method = methods[1];
		parameters = method.parameters();
		assertEquals("Incorrect no of parameters", 1, parameters.size());
		assertExtraDimensionsEqual("Incorrect dimension info", ((SingleVariableDeclaration) parameters.get(0)).getExtraDimensionInfos(), "[] []");

		method = methods[2];
		assertExtraDimensionsEqual("Incorrect dimension info", method.getExtraDimensionInfos(), "@Annot1 @Annot2 [] []");
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=399600
	public void test0010() throws JavaModelException {
		String contents =
				"import java.lang.annotation.ElementType;\n" +
						"public class X {\n" +
						"	@Marker int foo(@Marker(\"Blah\") int z) @Marker [] @Marker [] {\n" +
						"		return null;\n" +
						"	}\n" +
						"}\n" +
						"@java.lang.annotation.Target (ElementType.TYPE_USE)\n" +
						"@interface Marker {\n" +
						"	String value() default \"Blah\";\n" +
						"}";
		this.workingCopy = getWorkingCopy("/Converter18/src/X.java", true);
		ASTNode node = buildAST(contents, this.workingCopy);
		assertEquals("Not a compilation unit", ASTNode.COMPILATION_UNIT, node.getNodeType());
		CompilationUnit unit = (CompilationUnit) node;
		node = getASTNode(unit, 0, 0);
		assertEquals("Not a method declaration", ASTNode.METHOD_DECLARATION, node.getNodeType());
		MethodDeclaration method = (MethodDeclaration) node;
		assertExtraDimensionsEqual("Incorrect dimension info", method.getExtraDimensionInfos(), "@Marker [] @Marker []");
	}
}
