/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.core.tests.compiler.parser;

import java.io.File;
import java.io.IOException;
import junit.framework.Test;

import org.eclipse.jdt.core.tests.util.CompilerTestSetup;

public class LambdaExpressionSyntaxTest extends AbstractSyntaxTreeTest {

	private static String  jsr335TestScratchArea = "c:\\Jsr335TestScratchArea";
	private static String referenceCompiler = "C:\\jdk-7-ea-bin-b75-windows-i586-30_oct_2009\\jdk7\\bin\\javac.exe"; // TODO: Patch when RI becomes available.

	public static Class testClass() {
		return LambdaExpressionSyntaxTest.class;
	}
	public void initialize(CompilerTestSetup setUp) {
		super.initialize(setUp);
	}
	public static Test suite() {
		return buildMinimalComplianceTestSuite(testClass(), F_1_8);
	}

	public LambdaExpressionSyntaxTest(String testName){
		super(testName, referenceCompiler, jsr335TestScratchArea);
		if (referenceCompiler != null) {
			File f = new File(jsr335TestScratchArea);
			if (!f.exists()) {
				f.mkdir();
			}
			CHECK_ALL |= CHECK_JAVAC_PARSER;
		}
	}

	static {
		//		TESTS_NAMES = new String[] { "test0012" };
		//		TESTS_NUMBERS = new int[] { 133, 134, 135 };
		if (!(new File(referenceCompiler).exists())) {
			referenceCompiler = null;
			jsr335TestScratchArea = null;
		}
	}
	// type elided, unparenthesized parameter + expression body lambda in casting context.
	public void test0001() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        System.out.println(((I) x -> x * x).square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    System.out.println(((I) (<no type> x) -> (x * x)).square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0001", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda as initializer.
	public void test0002() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i =  x -> x * x;\n" +
				"        System.out.println(i.square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = (<no type> x) ->     (x * x);\n" + 
				"    System.out.println(i.square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0002", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda as initializer, full lambda is parenthesized.
	public void test0003() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i =  ((((x -> x * x))));\n" +
				"        System.out.println(i.square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = (((((<no type> x) ->     (x * x)))));\n" + 
				"    System.out.println(i.square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0003", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda as RHS of assignment, full lambda is parenthesized.
	public void test0004() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i;\n" +
				"        i =  (x -> x * x);\n" +
				"        System.out.println(i.square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i;\n" + 
				"    i = ((<no type> x) -> (x * x));\n" + 
				"    System.out.println(i.square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0004", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda in return statement, full lambda is parenthesized.
	public void test0005() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    static I getI() {\n" +
				"        return (x -> x * x);\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = getI();\n" +
				"        System.out.println(i.square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  static I getI() {\n" + 
				"    return ((<no type> x) -> (x * x));\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = getI();\n" + 
				"    System.out.println(i.square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0005", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda in conditional expression.
	public void test0006() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = args == null ? x -> x * x : x -> x * x * x;\n" +
				"        System.out.println(i.square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = ((args == null) ? (<no type> x) -> (x * x) : (<no type> x) -> ((x * x) * x));\n" + 
				"    System.out.println(i.square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0006", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda in message send.
	public void test0007() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    static void foo(I i1, I i2) {\n" +
				"        System.out.println(i1.square(10));\n" +
				"        System.out.println(i2.square(10));\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        foo(x -> x * x, x -> x * x * x);\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  static void foo(I i1, I i2) {\n" + 
				"    System.out.println(i1.square(10));\n" + 
				"    System.out.println(i2.square(10));\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    foo((<no type> x) -> (x * x), (<no type> x) -> ((x * x) * x));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0007", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda in constructor call.
	public void test0008() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    X (I i1, I i2) {\n" +
				"        System.out.println(i1.square(10));\n" +
				"        System.out.println(i2.square(10));\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        new X(x -> x * x, x -> x * x * x);\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  X(I i1, I i2) {\n" + 
				"    super();\n" + 
				"    System.out.println(i1.square(10));\n" + 
				"    System.out.println(i2.square(10));\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    new X((<no type> x) -> (x * x), (<no type> x) -> ((x * x) * x));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0008", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda in lambda.
	public void test0009() throws IOException {
		String source = 
				"interface I {\n" +
				"    I square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"      System.out.println (((I) a->b->c->d->e->f->g-> null).square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  I square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    System.out.println(((I) (<no type> a) -> (<no type> b) -> (<no type> c) -> (<no type> d) -> (<no type> e) -> (<no type> f) -> (<no type> g) -> null).square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0009", expectedUnitToString);
	}
	// type elided, unparenthesized parameter + expression body lambda in an initializer block
	public void test00010() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    static I i = x -> x * x;\n" +
				"    {\n" +
				"        i = x -> x * x * x;\n" +
				"    }\n" +
				"    static {\n" +
				"        i = x -> x * x * x;\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  static I i = (<no type> x) ->   (x * x);\n" + 
				"  {\n" + 
				"    i = (<no type> x) -> ((x * x) * x);\n" + 
				"  }\n" + 
				"  static {\n" + 
				"    i = (<no type> x) -> ((x * x) * x);\n" + 
				"  }\n" + 
				"  <clinit>() {\n" + 
				"  }\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test00010", expectedUnitToString);
	}
	// type elided, parenthesized parameter + expression body lambda in casting context.
	public void test0011() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        System.out.println(((I) (x) -> x * x).square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    System.out.println(((I) (<no type> x) -> (x * x)).square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0011", expectedUnitToString);
	}
	// Normal & minimal parameter list + expression body lambda in assignment context.
	public void test0012() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = (int x) -> x * x;\n" +
				"        System.out.println(i.square(10));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = (int x) ->     (x * x);\n" + 
				"    System.out.println(i.square(10));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0012", expectedUnitToString);
	}
	// Normal parameter list, with modifiers & annotations  + expression body lambda in invocation context.
	public void test0013() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int x);\n" +
				"}\n" +
				"public class X {\n" +
				"    @interface Positive {}\n" +
				"    static void foo(I i1, I i2) {\n" +
				"        System.out.println(i1.square(10));\n" +
				"        System.out.println(i2.square(10));\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        foo((final int x) -> x * x, (final @Positive int x) -> x * x * x);\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  @interface Positive {\n" + 
				"  }\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  static void foo(I i1, I i2) {\n" + 
				"    System.out.println(i1.square(10));\n" + 
				"    System.out.println(i2.square(10));\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    foo((final int x) -> (x * x), (final @Positive int x) -> ((x * x) * x));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0013", expectedUnitToString);
	}
	// Vararg parameter list, with modifiers & annotations + expression body lambda in message send context.
	public void test0014() throws IOException {
		String source = 
				"interface I {\n" +
				"    int square(int ... x);\n" +
				"}\n" +
				"public class X {\n" +
				"    @interface Positive {}\n" +
				"    static void foo(I i1, I i2) {\n" +
				"        System.out.println(i1.square(10));\n" +
				"        System.out.println(i2.square(10));\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        foo((final int ... x) -> 10, (final @Positive int [] x) -> 20);\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int square(int... x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  @interface Positive {\n" + 
				"  }\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  static void foo(I i1, I i2) {\n" + 
				"    System.out.println(i1.square(10));\n" + 
				"    System.out.println(i2.square(10));\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    foo((final int... x) -> 10, (final @Positive int[] x) -> 20);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0014", expectedUnitToString);
	}
	// multi parameter type elided list + expression body lambda in return statement.
	public void test0015() throws IOException {
		String source = 
				"interface I {\n" +
				"    int product(int x, int y);\n" +
				"}\n" +
				"public class X {\n" +
				"    static I getI() {\n" +
				"        return ((x, y) -> x * y);\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = getI();\n" +
				"        System.out.println(i.product(5, 6));\n" +
				"    }\n" +
				"};\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int product(int x, int y);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  static I getI() {\n" + 
				"    return ((<no type> x, <no type> y) -> (x * y));\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = getI();\n" + 
				"    System.out.println(i.product(5, 6));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0015", expectedUnitToString);
	}
	// multi parameter type specified list + block body lambda in return statement.
	public void test0016() throws IOException {
		String source = 
				"interface I {\n" +
				"    int product(int x, int y);\n" +
				"}\n" +
				"public class X {\n" +
				"    static I getI() {\n" +
				"        return (int x, int y) -> { return x * y; };\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = getI();\n" +
				"        System.out.println(i.product(5, 6));\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  int product(int x, int y);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  static I getI() {\n" + 
				"    return (int x, int y) -> {\n" + 
				"  return (x * y);\n" + 
				"};\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = getI();\n" + 
				"    System.out.println(i.product(5, 6));\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0016", expectedUnitToString);
	}
	// noarg + block body lambda 
	public void test0017() throws IOException {
		String source = 
				"interface I {\n" +
				"    String noarg();\n" +
				"}\n" +
				"public class X {\n" +
				"    public static void main(String [] args) {\n" +
				"        System.out.println( ((I) () -> { return \"noarg\"; }).noarg());\n" +
				"    }\n" +
				"}\n";

		String expectedUnitToString = 
				"interface I {\n" + 
				"  String noarg();\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    System.out.println(((I) () -> {\n" + 
				"  return \"noarg\";\n" + 
				"}).noarg());\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0017", expectedUnitToString);
	}
	// Reference expression - super:: form, without type arguments. 
	public void test0018() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"    public static void main(String [] args) {\n" +
				"	new X().doit();\n" +
				"    }\n" +
				"    void doit() {\n" +
				"        I i = super::foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"    public void foo(int x) {\n" +
				"	System.out.println(x);\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X extends Y {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    new X().doit();\n" + 
				"  }\n" + 
				"  void doit() {\n" + 
				"    I i = super::foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0018", expectedUnitToString);
	}
	// Reference expression - super:: form, with type arguments. 
	public void test0019() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X extends Y {\n" +
				"    public static void main(String [] args) {\n" +
				"	new X().doit();\n" +
				"    }\n" +
				"    void doit() {\n" +
				"        I i = super::<String>foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"    public void foo(int x) {\n" +
				"	System.out.println(x);\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X extends Y {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    new X().doit();\n" + 
				"  }\n" + 
				"  void doit() {\n" + 
				"    I i = super::<String>foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0019", expectedUnitToString);
	}
	// Reference expression - SimpleName:: form, without type arguments.
	public void test0020() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y::foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"    public static void foo(int x) {\n" +
				"	System.out.println(x);\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y::foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0020", expectedUnitToString);
	}
	// Reference expression - SimpleName:: form, with type arguments.
	public void test0021() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y::<String>foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"    public static void foo(int x) {\n" +
				"	System.out.println(x);\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y::<String>foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0021", expectedUnitToString);
	}
	// Reference expression - QualifiedName:: form, without type arguments.
	public void test0022() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y.Z::foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"    static class Z {\n" +
				"        public static void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y.Z::foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  static class Z {\n" + 
				"    Z() {\n" + 
				"      super();\n" + 
				"    }\n" + 
				"    public static void foo(int x) {\n" + 
				"      System.out.println(x);\n" + 
				"    }\n" + 
				"  }\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0022", expectedUnitToString);
	}
	// Reference expression - QualifiedName:: form, with type arguments.
	public void test0023() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y.Z::<String>foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"    static class Z {\n" +
				"        public static void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y.Z::<String>foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  static class Z {\n" + 
				"    Z() {\n" + 
				"      super();\n" + 
				"    }\n" + 
				"    public static void foo(int x) {\n" + 
				"      System.out.println(x);\n" + 
				"    }\n" + 
				"  }\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0023", expectedUnitToString);
	}
	// Reference expression - Primary:: form, without type arguments.
	public void test0024() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = new Y()::foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = new Y()::foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0024", expectedUnitToString);
	}
	// Reference expression - primary:: form, with type arguments.
	public void test0025() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = new Y()::<String>foo;\n" +
				"        i.foo(10); \n" +
				"    }\n" +
				"}\n" +
				"class Y {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = new Y()::<String>foo;\n" + 
				"    i.foo(10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0025", expectedUnitToString);
	}
	// Reference expression - X<T>:: form, without type arguments.
	public void test0026() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(Y<String> y, int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y<String>::foo;\n" +
				"        i.foo(new Y<String>(), 10); \n" +
				"    }\n" +
				"}\n" +
				"class Y<T> {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(Y<String> y, int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y<String>::foo;\n" + 
				"    i.foo(new Y<String>(), 10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y<T> {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0026", expectedUnitToString);
	}
	// Reference expression - X<T>:: form, with type arguments.
	public void test0027() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(Y<String> y, int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y<String>::<String>foo;\n" +
				"        i.foo(new Y<String>(), 10); \n" +
				"    }\n" +
				"}\n" +
				"class Y<T> {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(Y<String> y, int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y<String>::<String>foo;\n" + 
				"    i.foo(new Y<String>(), 10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y<T> {\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  void foo(int x) {\n" + 
				"    System.out.println(x);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0027", expectedUnitToString);
	}
	// Reference expression - X<T>.Name:: form, without type arguments.
	public void test0028() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(Y<String>.Z z, int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y<String>.Z::foo;\n" +
				"        i.foo(new Y<String>().new Z(), 10); \n" +
				"    }\n" +
				"}\n" +
				"class Y<T> {\n" +
				"    class Z {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(Y<String>.Z z, int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y<String>.Z::foo;\n" + 
				"    i.foo(new Y<String>().new Z(), 10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y<T> {\n" + 
				"  class Z {\n" + 
				"    Z() {\n" + 
				"      super();\n" + 
				"    }\n" + 
				"    void foo(int x) {\n" + 
				"      System.out.println(x);\n" + 
				"    }\n" + 
				"  }\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0028", expectedUnitToString);
	}
	// Reference expression - X<T>.Name:: form, with type arguments.
	public void test0029() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(Y<String>.Z z, int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y<String>.Z::<String>foo;\n" +
				"        i.foo(new Y<String>().new Z(), 10); \n" +
				"    }\n" +
				"}\n" +
				"class Y<T> {\n" +
				"    class Z {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(Y<String>.Z z, int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y<String>.Z::<String>foo;\n" + 
				"    i.foo(new Y<String>().new Z(), 10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y<T> {\n" + 
				"  class Z {\n" + 
				"    Z() {\n" + 
				"      super();\n" + 
				"    }\n" + 
				"    void foo(int x) {\n" + 
				"      System.out.println(x);\n" + 
				"    }\n" + 
				"  }\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0029", expectedUnitToString);
	}
	// Reference expression - X<T>.Y<K>:: form, without type arguments.
	public void test0030() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(Y<String>.Z<Integer> z, int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y<String>.Z<Integer>::foo;\n" +
				"        i.foo(new Y<String>().new Z<Integer>(), 10); \n" +
				"    }\n" +
				"}\n" +
				"class Y<T> {\n" +
				"    class Z<K> {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(Y<String>.Z<Integer> z, int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y<String>.Z<Integer>::foo;\n" + 
				"    i.foo(new Y<String>().new Z<Integer>(), 10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y<T> {\n" + 
				"  class Z<K> {\n" + 
				"    Z() {\n" + 
				"      super();\n" + 
				"    }\n" + 
				"    void foo(int x) {\n" + 
				"      System.out.println(x);\n" + 
				"    }\n" + 
				"  }\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0030", expectedUnitToString);
	}
	// Reference expression - X<T>.Y<K>:: form, with type arguments.
	public void test0031() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(Y<String>.Z<Integer> z, int x);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y<String>.Z<Integer>::<String>foo;\n" +
				"        i.foo(new Y<String>().new Z<Integer>(), 10); \n" +
				"    }\n" +
				"}\n" +
				"class Y<T> {\n" +
				"    class Z<K> {\n" +
				"        void foo(int x) {\n" +
				"	    System.out.println(x);\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(Y<String>.Z<Integer> z, int x);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = Y<String>.Z<Integer>::<String>foo;\n" + 
				"    i.foo(new Y<String>().new Z<Integer>(), 10);\n" + 
				"  }\n" + 
				"}\n" + 
				"class Y<T> {\n" + 
				"  class Z<K> {\n" + 
				"    Z() {\n" + 
				"      super();\n" + 
				"    }\n" + 
				"    void foo(int x) {\n" + 
				"      System.out.println(x);\n" + 
				"    }\n" + 
				"  }\n" + 
				"  Y() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0031", expectedUnitToString);
	}
	// Constructor reference expression - X<T>.Y<K>::new form, with type arguments.
	public void test0032() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(Y<String> y);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = Y<String>.Z<Integer>::<String>new;\n" +
				"        i.foo(new Y<String>()); \n" +
				"    }\n" +
				"}\n" +
				"class Y<T> {\n" +
				"    class Z<K> {\n" +
				"        Z() {\n" +
				"            System.out.println(\"Y<T>.Z<K>::new\");\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
					"  void foo(Y<String> y);\n" + 
					"}\n" + 
					"public class X {\n" + 
					"  public X() {\n" + 
					"    super();\n" + 
					"  }\n" + 
					"  public static void main(String[] args) {\n" + 
					"    I i = Y<String>.Z<Integer>::<String>new;\n" + 
					"    i.foo(new Y<String>());\n" + 
					"  }\n" + 
					"}\n" + 
					"class Y<T> {\n" + 
					"  class Z<K> {\n" + 
					"    Z() {\n" + 
					"      super();\n" + 
					"      System.out.println(\"Y<T>.Z<K>::new\");\n" + 
					"    }\n" + 
					"  }\n" + 
					"  Y() {\n" + 
					"    super();\n" + 
					"  }\n" + 
					"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0032", expectedUnitToString);
	}
	// Reference expression - PrimitiveType[]:: form, with type arguments.
	public void test0033() throws IOException {
		String source = 
				"interface I {\n" +
				"    Object copy(int [] ia);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = int[]::<String>clone;\n" +
				"        i.copy(new int[10]); \n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  Object copy(int[] ia);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = int[]::<String>clone;\n" + 
				"    i.copy(new int[10]);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0033", expectedUnitToString);
	}	
	// Reference expression - Name[]:: form, with type arguments.
	public void test0034() throws IOException {
		String source = 
				"interface I {\n" +
				"    Object copy(X [] ia);\n" +
				"}\n" +
				"public class X  {\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = X[]::<String>clone;\n" +
				"        i.copy(new X[10]); \n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  Object copy(X[] ia);\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = X::<String>clone;\n" + 
				"    i.copy(new X[10]);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0034", expectedUnitToString);
	}	
	// Reference expression - X<T>.Y<K>[]:: form, with type arguments.
	public void test0035() throws IOException {
		String source = 
				"interface I {\n" +
				"    Object copy(X<String>.Y<Integer> [] p);\n" +
				"}\n" +
				"public class X<T>  {\n" +
				"    class Y<K> {\n" +
				"    }\n" +
				"    public static void main(String [] args) {\n" +
				"        I i = X<String>.Y<Integer>[]::<String>clone;\n" +
				"        X<String>.Y<Integer>[] xs = null;\n" +
				"        i.copy(xs); \n" +
				"    }\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  Object copy(X<String>.Y<Integer>[] p);\n" + 
				"}\n" + 
				"public class X<T> {\n" + 
				"  class Y<K> {\n" + 
				"    Y() {\n" + 
				"      super();\n" + 
				"    }\n" + 
				"  }\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  public static void main(String[] args) {\n" + 
				"    I i = X<String>.Y<Integer>[]::<String>clone;\n" + 
				"    X<String>.Y<Integer>[] xs = null;\n" + 
				"    i.copy(xs);\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0035", expectedUnitToString);
	}
	// Assorted tests.
	public void test0036() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo();\n" +
				"}\n" +
				"\n" +
				"interface J {\n" +
				"    int foo();\n" +
				"}\n" +
				"public class X {\n" +
				"  I i1 = ()->{}; \n" +
				"  J j1 = ()->0;\n" +
				"  J j2 = ()->{ return 0; };\n" +
				"  I i2 = ()->{ System.gc(); };\n" +
				"  J j3 = ()->{\n" +
				"    if (true) return 0;\n" +
				"    else {\n" +
				"      int r = 12;\n" +
				"      for (int i = 1; i < 8; i++)\n" +
				"        r += i;\n" +
				"      return r;\n" +
				"    }\n" +
				"  };\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo();\n" + 
				"}\n" + 
				"interface J {\n" + 
				"  int foo();\n" + 
				"}\n" + 
				"public class X {\n" + 
				"  I i1 = () ->   {\n" + 
				"  };\n" + 
				"  J j1 = () ->   0;\n" + 
				"  J j2 = () ->   {\n" + 
				"    return 0;\n" + 
				"  };\n" + 
				"  I i2 = () ->   {\n" + 
				"    System.gc();\n" + 
				"  };\n" + 
				"  J j3 = () ->   {\n" + 
				"    if (true)\n" + 
				"        return 0;\n" + 
				"    else\n" + 
				"        {\n" + 
				"          int r = 12;\n" + 
				"          for (int i = 1;; (i < 8); i ++) \n" + 
				"            r += i;\n" + 
				"          return r;\n" + 
				"        }\n" + 
				"  };\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0036", expectedUnitToString);
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=384320, syntax error while mixing 308 and 335.
	public void test0037() throws IOException {
		String source = 
				"interface I {\n" +
				"    void foo(X<String> s, int x);\n" +
				"}\n" +
				"public class X<T> {\n" +
				"    I i = X<@Foo({\"hello\"}) String>::foo;\n" +
				"    void foo(int x) {\n" +
				"    }\n" +
				"}\n" +
				"@interface Foo {\n" +
				"    String [] value();\n" +
				"}\n";
		String expectedUnitToString = 
				"interface I {\n" + 
				"  void foo(X<String> s, int x);\n" + 
				"}\n" + 
				"public class X<T> {\n" + 
				"  I i = X<@Foo({\"hello\"}) String>::foo;\n" + 
				"  public X() {\n" + 
				"    super();\n" + 
				"  }\n" + 
				"  void foo(int x) {\n" + 
				"  }\n" + 
				"}\n" + 
				"@interface Foo {\n" + 
				"  String[] value();\n" + 
				"}\n";
		checkParse(CHECK_PARSER | CHECK_JAVAC_PARSER , source.toCharArray(), null, "test0037", expectedUnitToString);
	}
}
