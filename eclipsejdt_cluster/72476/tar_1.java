/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.jdt.core.tests.compiler.regression;

import junit.framework.Test;
public class OverloadResolutionTest8 extends AbstractRegressionTest {

static {
//	TESTS_NAMES = new String[] { "test007"};
//	TESTS_NUMBERS = new int[] { 50 };
//	TESTS_RANGE = new int[] { 11, -1 };
}
public OverloadResolutionTest8(String name) {
	super(name);
}
public static Test suite() {
	return buildMinimalComplianceTestSuite(testClass(), F_1_8);
}
public static Class testClass() {
	return OverloadResolutionTest8.class;
}

public void test001() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	int foo(int [] a);\n" +
				"}\n" +
				"interface J  {\n" +
				"	int foo(int a);\n" +
				"}\n" +
				"public class X {\n" +
				"	public static void main(String[] args) {\n" +
				"		System.out.println(foo((a)->a.length));\n" +
				"	}\n" +
				"	static String foo(I i) {\n" +
				"		return(\"foo(I)\");\n" +
				"	}\n" +
				"	static String foo(J j) {\n" +
				"		return(\"foo(J)\");\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 9)\n" + 
			"	System.out.println(foo((a)->a.length));\n" + 
			"	                   ^^^\n" + 
			"The method foo(I) is ambiguous for the type X\n" + 
			"----------\n"
			);
}
public void test002() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"goo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"goo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		final boolean x = true;\n" +
				"		goo(()-> goo((I)null));\n" +
				"	}\n" +
				"	int f() {\n" +
				"		final boolean x = true;\n" +
				"		while (x);\n" +
				"	}\n" +
				"}\n",
			},
			"goo(I)");
}
public void test003() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"public class X {\n" +
				"   static final boolean f = true;\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"goo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		final boolean x = true;\n" +
				"		goo(()-> { \n" +
				"			final boolean y = true;\n" +
				"			while (y); \n" +
				"			});\n" +
				"		goo(()-> { \n" +
				"			while (x); \n" +
				"			});\n" +
				"		goo(()-> { \n" +
				"			while (f); \n" +
				"			});\n" +
				"	}\n" +
				"}\n",
			},
			"goo(J)\n" +
			"goo(J)\n" +
			"goo(J)");
}
public void test004() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"public class X {\n" +
				"   static boolean f = true;\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"goo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		boolean x = true;\n" +
				"		goo(()-> { \n" +
				"			boolean y = true;\n" +
				"			while (y); \n" +
				"			});\n" +
				"		goo(()-> { \n" +
				"			while (x); \n" +
				"			});\n" +
				"		goo(()-> { \n" +
				"			while (f); \n" +
				"			});\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 11)\n" + 
			"	goo(()-> { \n" + 
			"	^^^\n" + 
			"The method goo(J) in the type X is not applicable for the arguments (() -> {\n" + 
			"  boolean y = true;\n" + 
			"  while (y)    ;\n" + 
			"})\n" + 
			"----------\n" + 
			"2. ERROR in X.java (at line 15)\n" + 
			"	goo(()-> { \n" + 
			"	^^^\n" + 
			"The method goo(J) in the type X is not applicable for the arguments (() -> {\n" + 
			"  while (x)    ;\n" + 
			"})\n" + 
			"----------\n" + 
			"3. ERROR in X.java (at line 18)\n" + 
			"	goo(()-> { \n" + 
			"	^^^\n" + 
			"The method goo(J) in the type X is not applicable for the arguments (() -> {\n" + 
			"  while (f)    ;\n" + 
			"})\n" + 
			"----------\n");
}
public void test005() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"public class X {\n" +
				"   final boolean f = true;\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"goo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		final boolean x = true;\n" +
				"		goo(()-> { \n" +
				"			final boolean y = true;\n" +
				"			while (y); \n" +
				"			});\n" +
				"		goo(()-> { \n" +
				"			while (x); \n" +
				"			});\n" +
				"		goo(()-> { \n" +
				"			while (f); \n" +
				"			});\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 19)\n" + 
			"	while (f); \n" + 
			"	       ^\n" + 
			"Cannot make a static reference to the non-static field f\n" + 
			"----------\n");
}
public void test006() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"public class X {\n" +
				" public static interface StringToInt {\n" +
				"  	int stoi(String s);\n" +
				" }\n" +
				" public static interface ReduceInt {\n" +
				"     int reduce(int a, int b);\n" +
				" }\n" +
				" void foo(StringToInt s) { }\n" +
				" void bar(ReduceInt r) { }\n" +
				" void bar() {\n" +
				"     bar((int x, int y) -> x+y); //SingleVariableDeclarations are OK\n" +
				"     foo(s -> s.length());\n" +
				"     foo((s) -> s.length());\n" +
				"     foo((String s) -> s.length()); //SingleVariableDeclaration is OK\n" +
				"     bar((x, y) -> x+y);\n" +
				" }\n" +
				"}\n",
			},
			"");
}
public void test007() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface J {\n" +
				"	void foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"goo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		goo(()-> 10); \n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 9)\n" + 
			"	goo(()-> 10); \n" + 
			"	         ^^\n" + 
			"Void methods cannot return a value\n" + 
			"----------\n");
}
public void test008() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Object foo();\n" +
				"}\n" +
				"interface J  {\n" +
				"	String foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	public static void main(String[] args) {\n" +
				"		System.out.println(foo(()->null));\n" +
				"	}\n" +
				"	static String foo(I i) {\n" +
				"		return(\"foo(I)\");\n" +
				"	}\n" +
				"	static String foo(J j) {\n" +
				"		return(\"foo(J)\");\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test009() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Object foo();\n" +
				"}\n" +
				"interface J  {\n" +
				"	void foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	public static void main(String[] args) {\n" +
				"		System.out.println(foo(()-> {}));\n" +
				"	}\n" +
				"	static String foo(I i) {\n" +
				"		return(\"foo(I)\");\n" +
				"	}\n" +
				"	static String foo(J j) {\n" +
				"		return(\"foo(J)\");\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test010() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Object foo();\n" +
				"}\n" +
				"interface J  {\n" +
				"	void foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	public static void main(String[] args) {\n" +
				"		System.out.println(foo(()-> foo(()->null)));\n" +
				"	}\n" +
				"	static String foo(I i) {\n" +
				"		return(\"foo(I)\");\n" +
				"	}\n" +
				"	static String foo(J j) {\n" +
				"		return(\"foo(J)\");\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test011() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	int foo();\n" +
				"}\n" +
				"interface J  {\n" +
				"	String foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	public static void main(String[] args) {\n" +
				"		System.out.println(foo(()-> \"Hello\" ));\n" +
				"	}\n" +
				"	static String foo(I i) {\n" +
				"		return(\"foo(I)\");\n" +
				"	}\n" +
				"	static String foo(J j) {\n" +
				"		return(\"foo(J)\");\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test012() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	int foo();\n" +
				"}\n" +
				"interface J  {\n" +
				"	String foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	public static void main(String[] args) {\n" +
				"		System.out.println(foo(()-> 1234 ));\n" +
				"	}\n" +
				"	static String foo(I i) {\n" +
				"		return(\"foo(I)\");\n" +
				"	}\n" +
				"	static String foo(J j) {\n" +
				"		return(\"foo(J)\");\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test013() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	int foo();\n" +
				"}\n" +
				"interface J  {\n" +
				"	Integer foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	public static void main(String[] args) {\n" +
				"		System.out.println(foo(()-> 1234 ));\n" +
				"	}\n" +
				"	static String foo(I i) {\n" +
				"		return(\"foo(I)\");\n" +
				"	}\n" +
				"	static String foo(J j) {\n" +
				"		return(\"foo(J)\");\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test014() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Integer foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"public class X {\n" +
				" \n" +
				"	static void foo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	\n" +
				"	static void foo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(()-> new Integer(10));\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test015() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"interface I {\n" +
				"	Integer foo();\n" +
				"}\n" +
				"public class X {\n" +
				" \n" +
				"	static void foo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	\n" +
				"	static void foo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(()-> new Integer(10));\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test016() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface O {\n" +
				"	Object foo();\n" +
				"}\n" +
				"interface S {\n" +
				"	String foo();\n" +
				"}\n" +
				"interface I {\n" +
				"	O foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	S foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(()-> ()-> \"String\");\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test017() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(()-> new Integer(10));\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test018() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	X [] foo(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I x) {\n" +
				"            System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	I i = X[]::new;\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(X[]::new);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test019() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I x) {\n" +
				"            System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	I i = X[]::new;\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(X[]::new);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}

public void test020() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	Y foo();\n" +
				"}\n" +
				"class Y {\n" +
				"	Y() {\n" +
				"	}\n" +
				"	\n" +
				"	Y(int x) {\n" +
				"	}\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::new);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 20)\n" + 
			"	foo(Y::new);\n" + 
			"	^^^\n" + 
			"The method foo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
public void test021() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	Y foo();\n" +
				"}\n" +
				"class Y {\n" +
				"	private Y() {\n" +
				"	}\n" +
				"	\n" +
				"	Y(int x) {\n" +
				"	}\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"       System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"       System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::new);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test022() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	Y foo();\n" +
				"}\n" +
				"class Y {\n" +
				"	Y(float f) {\n" +
				"       System.out.println(\"Y(float)\");\n" +
				"	}\n" +
				"	\n" +
				"	Y(int x) {\n" +
				"       System.out.println(\"Y(int)\");\n" +
				"	}\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"       i.foo(10);\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"       j.foo();\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::new);\n" +
				"	}\n" +
				"}\n",
			},
			"Y(int)");
}
public void test023() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	Y foo();\n" +
				"}\n" +
				"class Y {\n" +
				"	Y(int ... x) {\n" +
				"	}\n" +
				"	\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::new);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 18)\n" + 
			"	foo(Y::new);\n" + 
			"	^^^\n" + 
			"The method foo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
public void test024() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	Y(int x) {\n" +
				"	}\n" +
				"	\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::new);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 18)\n" + 
			"	foo(Y::new);\n" + 
			"	^^^\n" + 
			"The method foo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
public void test025() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	X foo(int x);\n" +
				"}\n" +
				"class Y extends X {\n" +
				"    Y(int x) {\n" +
				"    }\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"            System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"            System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::new);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test026() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	X foo(int x);\n" +
				"}\n" +
				"class Y extends X {\n" +
				"    <T> Y(int x) {\n" +
				"    }\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"            System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"            System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::new);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 19)\n" + 
			"	foo(Y::new);\n" + 
			"	^^^\n" + 
			"The method foo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
public void test027() { // javac bug: 8b115 complains of ambiguity here.
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	X foo(int x);\n" +
				"}\n" +
				"class Y extends X {\n" +
				"    <T> Y(int x) {\n" +
				"    }\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"            System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"            System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y::<String>new);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test028() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y [] foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	X [] foo();\n" +
				"}\n" +
				"class Y extends X {\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(Y []::new);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test029() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y [] foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	X [] foo();\n" +
				"}\n" +
				"class Y extends X {\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(X []::new);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 17)\n" + 
			"	foo(X []::new);\n" + 
			"	    ^^^^^^^^^\n" + 
			"Constructed array X[] cannot be assigned to Y[] as required in the interface descriptor  \n" + 
			"----------\n");
}
public void test030() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Y [] foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	X [] foo(int x);\n" +
				"}\n" +
				"class Y extends X {\n" +
				"}\n" +
				"public class X {\n" +
				"	static void foo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void foo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		foo(X []::new);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=401850, [1.8][compiler] Compiler fails to type poly allocation expressions in method invocation contexts
public void test031() {
	this.runConformTest(
			new String[] {
				"X.java",
				"public class X<T> {\n" +
				"	void foo(X<String> s) {\n" +
				"       System.out.println(\"foo(X<String>)\");\n" +
				"   }\n" +
				"	public static void main(String[] args) {\n" +
				"		new X<String>().foo(new X<>());\n" +
				"	}\n" +
				"}\n",
			},
			"foo(X<String>)");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=401850, [1.8][compiler] Compiler fails to type poly allocation expressions in method invocation contexts
public void test032() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"public class X<T> {\n" +
				"    void foo(X<String> s, Object o) {\n" +
				"        System.out.println(\"foo(X<String>)\");\n" +
				"    }\n" +
				"    void foo(X xs, String s) {\n" +
				"        System.out.println(\"foo(X<String>)\");\n" +
				"    }\n" +
				"    public static void main(String[] args) {\n" +
				"        new X<String>().foo(new X<>(), \"Hello\");\n" +
				"    }\n" +
				"}\n",
			},
			"----------\n" + 
			"1. WARNING in X.java (at line 5)\n" + 
			"	void foo(X xs, String s) {\n" + 
			"	         ^\n" + 
			"X is a raw type. References to generic type X<T> should be parameterized\n" + 
			"----------\n" + 
			"2. ERROR in X.java (at line 9)\n" + 
			"	new X<String>().foo(new X<>(), \"Hello\");\n" + 
			"	                ^^^\n" + 
			"The method foo(X<String>, Object) is ambiguous for the type X<String>\n" + 
			"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=401850, [1.8][compiler] Compiler fails to type poly allocation expressions in method invocation contexts
public void test033() {
	this.runConformTest(
			new String[] {
				"X.java",
				"class Y<T> {}\n" +
				"public class X<T> extends Y<T> {\n" +
				"    void foo(X<String> s) {\n" +
				"        System.out.println(\"foo(X<String>)\");\n" +
				"    }\n" +
				"    void foo(Y<String> y) {\n" +
				"        System.out.println(\"foo(Y<String>)\");\n" +
				"    }\n" +
				"    public static void main(String[] args) {\n" +
				"        new X<String>().foo(new X<>());\n" +
				"    }\n" +
				"}\n",
			},
			"foo(X<String>)");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=422050, [1.8][compiler] Overloaded method call with poly-conditional expression rejected by the compiler
public void test422050() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I { \n" +
				"	int foo(); \n" +
				"}\n" +
				"interface J { \n" +
				"	double foo(); \n" +
				"}\n" +
				"public class X {\n" +
				"	static int foo(I i) {\n" +
				"		return 0;\n" +
				"	}\n" +
				"	static int foo(J j) {\n" +
				"		return 1;\n" +
				"	}\n" +
				"	public static void main(String argv[]) {\n" +
				"		System.out.println(foo (() -> true ? 0 : 1));\n" +
				"	}\n" +
				"}\n",
			},
			"0");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=400871, [1.8][compiler] Overhaul overload resolution to reconcile with JLS8 15.12.2
public void test400871() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"public class X {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	static int foo() {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(X::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=400871, [1.8][compiler] Overhaul overload resolution to reconcile with JLS8 15.12.2
public void test400871a() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	int foo(int y) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=400871, [1.8][compiler] Overhaul overload resolution to reconcile with JLS8 15.12.2
public void test400871b() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	<T> int foo(int y) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=400871, [1.8][compiler] Overhaul overload resolution to reconcile with JLS8 15.12.2
public void test400871c() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	<T> int foo(String y) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 23)\n" + 
			"	goo(new X()::foo);\n" + 
			"	^^^\n" + 
			"The method goo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=400871, [1.8][compiler] Overhaul overload resolution to reconcile with JLS8 15.12.2
public void test400871d() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	int foo(String y) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	<T> int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 23)\n" + 
			"	goo(new X()::foo);\n" + 
			"	^^^\n" + 
			"The method goo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=400871, [1.8][compiler] Overhaul overload resolution to reconcile with JLS8 15.12.2
public void test4008712() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	int foo(String y) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	<T> int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 23)\n" + 
			"	goo(new X()::foo);\n" + 
			"	^^^\n" + 
			"The method goo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
public void test4008712e() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	int foo(int y) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test4008712f() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	int foo(int ... x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 20)\n" + 
			"	goo(new X()::foo);\n" + 
			"	^^^\n" + 
			"The method goo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
public void test4008712g() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	private int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. WARNING in X.java (at line 8)\n" + 
			"	private int foo(int x) {\n" + 
			"	            ^^^^^^^^^^\n" + 
			"The method foo(int) from the type Y is never used locally\n" + 
			"----------\n" + 
			"2. ERROR in X.java (at line 20)\n" + 
			"	goo(new X()::foo);\n" + 
			"	    ^^^^^^^^^^^^\n" + 
			"The type X does not define foo(int) that is applicable here\n" + 
			"----------\n");
}
public void test4008712h() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	public <T> int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 20)\n" + 
			"	goo(new X()::foo);\n" + 
			"	^^^\n" + 
			"The method goo(I) is ambiguous for the type X\n" + 
			"----------\n");
}
public void test4008712i() { // javac bug: 8b115 complains of ambiguity here.
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(int x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(int x);\n" +
				"}\n" +
				"class Y {\n" +
				"	public <T> int foo(int x) {\n" +
				"		 return 0;\n" +
				"	}\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::<String>foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test4008712j() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<T> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X<String>()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test4008712k() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<T> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X<String>()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test4008712l() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X<String>()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test4008712m() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"   public void foo() {}\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X<String>()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 21)\n" + 
			"	goo(new X<String>()::foo);\n" + 
			"	^^^\n" + 
			"The method goo(I) is ambiguous for the type X<T>\n" + 
			"----------\n");
}
public void test4008712n() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"   public String foo(String s) { return null; }\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X<String>()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test4008712o() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"interface K<T> {\n" +
				"	public T foo(T x);\n" +
				"}\n" +
				"class Y<T> implements K {\n" +
				"	public Object foo(Object x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"   public Object foo(Object s) { return null; }\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X<String>()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test4008712p() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"   public String foo(String s) { return null; }\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 21)\n" + 
			"	goo(new X()::foo);\n" + 
			"	^^^\n" + 
			"The method goo(I) is ambiguous for the type X<T>\n" + 
			"----------\n" + 
			"2. WARNING in X.java (at line 21)\n" + 
			"	goo(new X()::foo);\n" + 
			"	        ^\n" + 
			"X is a raw type. References to generic type X<T> should be parameterized\n" + 
			"----------\n");
}
public void test4008712q() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test4008712r() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo();\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X[0]::clone);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test4008712s() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo();\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X[0]::toString);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test4008712t() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	Class foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	Object foo();\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X[0]::getClass);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(I)");
}
public void test4008712u() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(I::clone);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 20)\n" + 
			"	goo(I::clone);\n" + 
			"	    ^^^^^^^^\n" + 
			"The type I does not define clone() that is applicable here\n" + 
			"----------\n");
}
public void test4008712v() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"       I i = () -> {};\n" +
				"		goo(i::hashCode);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
public void test4008712w() {
	this.runNegativeTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo();\n" +
				"}\n" +
				"interface J {\n" +
				"	int foo();\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"       I i = () -> {};\n" +
				"		goo(i::clone);\n" +
				"	}\n" +
				"}\n",
			},
			"----------\n" + 
			"1. ERROR in X.java (at line 21)\n" + 
			"	goo(i::clone);\n" + 
			"	    ^^^^^^^^\n" + 
			"The type I does not define clone() that is applicable here\n" + 
			"----------\n");
}
public void test4008712x() {
	this.runConformTest(
			new String[] {
				"X.java",
				"interface I {\n" +
				"	void foo(String x);\n" +
				"}\n" +
				"interface J {\n" +
				"	String foo(String x);\n" +
				"}\n" +
				"class Y<T> {\n" +
				"	public T foo(T x) {\n" +
				"		 return null;\n" +
				"	}\n" +
				"   private void foo() {}\n" +
				"}\n" +
				"public class X<T> extends Y<String> {\n" +
				"   public String foo(String s) { return null; }\n" +
				"	static void goo(I i) {\n" +
				"		System.out.println(\"foo(I)\");\n" +
				"	}\n" +
				"	static void goo(J j) {\n" +
				"		System.out.println(\"foo(J)\");\n" +
				"	}\n" +
				"	public static void main(String[] args) { \n" +
				"		goo(new X<String>()::foo);\n" +
				"	}\n" +
				"}\n",
			},
			"foo(J)");
}
}