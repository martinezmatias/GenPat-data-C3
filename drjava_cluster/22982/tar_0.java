package koala.dynamicjava.tree.tiger;

import java.util.*;
import junit.framework.*;

import koala.dynamicjava.tree.*;
import koala.dynamicjava.interpreter.*;
import koala.dynamicjava.SourceInfo;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;

import java.io.StringReader;
import java.util.List;
import koala.dynamicjava.parser.wrapper.ParserFactory;
import koala.dynamicjava.parser.wrapper.JavaCCParserFactory;


public class TigerTest extends TestCase {
  private TreeInterpreter astInterpreter;
  private TreeInterpreter strInterpreter;
  
  private ParserFactory parserFactory;
  private String testString;
  
  public TigerTest(String name) {
    super(name);
  }
  
  public void setUp(){
    parserFactory = new JavaCCParserFactory();
    astInterpreter = new TreeInterpreter(null); // No ParserFactory needed to interpret an AST
    strInterpreter = new TreeInterpreter(parserFactory); // ParserFactory is needed to interpret a string
  }
  
  public List<Node> parse(String testString){
    List<Node> retval = parserFactory.createParser(new StringReader(testString),"UnitTest").parseStream();
    return retval;
  }
  
  public Object interpret(String testString) throws InterpreterException {
    return strInterpreter.interpret(new StringReader(testString), "Unit Test");
//    List<Node> exps = strInterpreter.buildStatementList(new java.io.StringReader(testString), "Unit Test");
//    return astInterpreter.interpret(exps);
  }
  
  // Interpreting static import. 
  public void testStaticImport(){
    //STATIC FIELD
    testString =
      "import static java.lang.Integer.MAX_VALUE;\n"+
      "class A{\n"+
      "  int m(){return MAX_VALUE;}\n"+
      "}\n"+
      "A a = new A(); a.m();\n";
    
    assertEquals(new Integer(java.lang.Integer.MAX_VALUE), interpret(testString));
    
    //STATIC METHOD
    testString = 
      "import static java.lang.Math.abs;\n"+
      "class B{\n"+
      "   int m(){return abs(-2);}\n"+
      "}\n"+
      "B b = new B(); b.m();\n";
    assertEquals(new Integer(2), interpret(testString));
    
  }
  
  public void testStaticImportOfStaticInnerClass(){
    testString = 
      "package P;\n"+
      "public class A { \n"+
      "  public static class B {\n"+
      "    public static int m(){ return 0; }\n"+
      "  }\n"+
      "}\n"+
      "package Q;\n"+
      "import static P.A.B;\n"+
      "B.m();\n";
    assertEquals(0,interpret(testString));
    
    testString = 
      "package R;\n"+
      "public class C { \n"+
      "  public static class D {\n"+
      "    public static int m(){ return 0; }\n"+
      "  }\n"+
      "}\n"+
      "package S;\n"+
      "import static R.C.*;\n"+
      "D.m();\n";
    assertEquals(0,interpret(testString));
    
    //Tests that a non-static inner class cannot be imported
    testString =
      "package T;\n"+
      "public class U {\n"+
      "  public class V { }\n"+
      "}\n"+
      "package W;\n"+
      "import static T.U.*;\n"+
      "V v = new V();";
    try {
      interpret(testString);
      fail("Non static member should not be imported");
    } 
    catch(InterpreterException e) {
      //Expected to fail
    }
    
    //Tests that a non-static inner class cannot be imported
    testString =
      "package X;\n"+
      "public class Y {\n"+
      "  public class Z { \n"+
      "    public static int m() { return 5; }\n"+
      "  }\n"+
      "}\n"+
      "package AA;\n"+
      "import static X.Y.Z;\n"+
      "Z.m()";
    try {
      assertEquals(5,interpret(testString));
      fail("Non static member should not be imported");
    } 
    catch(InterpreterException e) {
      //Expected to fail
    }
    
  }
  /**
   * Testing various forms of static importation of methods
   */
  public void testStaticImportOfMethods(){
    
    //no parameters
    testString = 
      "package Pack;\n"+
      "public class Aclass { \n"+
      "  public static int m() { return 0; }\n"+
      "}\n"+
      "package Q;\n"+
      "import static Pack.Aclass.m;\n"+
      "m();\n";
    assertEquals(0,interpret(testString));
    
    testString = 
      "package R;\n"+
      "public class C { \n"+
      "  public static int m2(){ return 0; }\n"+
      "}\n"+
      "package S;\n"+
      "import static R.C.*;\n"+
      "m2();\n";
    assertEquals(0,interpret(testString));
    
    //With a parameter
    
    testString =
      "package T;\n"+
      "public class D { \n"+
      "  public static String m3(String s) { return s; }\n"+
      "}\n"+
      "package U;\n"+
      "import static T.D.m3;\n"+
      "m3(\"5\");\n";
    assertEquals("5",interpret(testString));
    
    testString =
      "package V;\n"+
      "public class E { \n"+
      "  public static String m4(String s, String s2) { return s; }\n"+
      "}\n"+
      "package W;\n"+
      "import static V.E.*;\n"+
      "m4(\"5\",\"6\");\n";
    assertEquals("5",interpret(testString));
    
    
    testString = 
      "package AA;\n"+
      "public class BB {"+
      "  public static int m5(int i) { return i; }\n"+
      "}\n"+
      "package CC;\n"+
      "import static AA.BB.m5;"+
      "m5(1+2+3+4+5);\n";
    assertEquals(15,interpret(testString));
    
    
    //With multiple parameters of different types
    
    testString =
      "package X;\n"+
      "public class F { \n"+
      "  public static String m6(String s, Class c) { return s; }\n"+
      "}\n"+
      "package Y;\n"+
      "import static X.F.*;\n"+
      "m6(\"5\",Integer.class);\n";
    assertEquals("5",interpret(testString));
    
    
    //With unequal parameters
    testString =
      "package DD;\n"+
      "public class EE { \n"+
      "  public static int m7(int i) { return i; }\n"+
      "}\n"+
      "package FF;\n"+
      "import static DD.EE.*;\n"+
      "m7(\"5\");\n";
    try {
      assertEquals(5,interpret(testString));
      fail("Method parameter types String and int are not equal!");
    }
    catch(InterpreterException e) {
      //Expected to fail
    }
    
    //Test for context shift
    //In other words, because we run a TypeChecker on the arguments, make sure that it doesn't actually add 
    // the element 5 to the list when it is checking its type for the name visitor.
    testString = 
      "package GG;\n"+
      "public class HH {\n"+
      "  public static int m8(int i) { return i; } \n" +
      "}\n"+
      "package II;\n"+
      "import static GG.HH.m8;\n"+
      "int i = 0;"+
      "m8(i);";
    assertEquals(0,interpret(testString));
    testString =
      "m8(i++);";
    assertEquals(0,interpret(testString));
    testString =
      "m8(++i);";
    assertEquals(2,interpret(testString));
    
    
    
    testString = 
      "package KK;\n"+
      "import static java.lang.Math.*;\n"+
      "abs(-4);";
    assertEquals(4,interpret(testString));
    testString = 
      "sqrt(4);";
    assertEquals(2.0,interpret(testString));
    testString =
      "sqrt(abs(-4));";
    assertEquals(2.0,interpret(testString));
  
    testString = 
      "package LL;\n"+
      "import static java.lang.Double.*;\n"+
      "public class MM {\n"+
      "  public static double parseDouble(String s) {\n"+
      "    return 0.0; \n"+
      "  }\n"+
      "  public static double m() {\n"+
      "    return parseDouble(\"1.5\");\n"+
      "  }\n"+
      "}\n"+
      "parseDouble(\"1.5\");\n";
    assertEquals(1.5,interpret(testString));
    testString = "MM.m();";
    assertEquals("Member of class should take precedence over staticly imported member",0.0,interpret(testString));
    
    
    
    testString = 
      "package NN;\n"+
      "import static java.lang.Math.abs;\n"+
      "public abstract class OO {\n"+
      "  public int abs(int i) { return i; }\n"+
      "}\n"+
      "public class PP extends OO {\n"+
      "  public static int m() {\n"+
      "    return abs(-2);\n"+
      "  }\n"+
      "}\n"+
      "PP.m();";
    try {
      interpret(testString);
      fail("Static method cannot reference non-static members of super class");
    } catch(InterpreterException e) {
      //Expected to fail
    }
    
    
    testString = 
      "package QQ;\n"+
      "import static java.lang.Math.abs;\n"+
      "public abstract class RR {\n"+
      "  public static int abs(int i) { return i; }\n"+
      "}\n"+
      "public class SS extends RR {\n"+
      "  public static int m() {\n"+
      "    return abs(-2);\n"+
      "  }\n"+
      "}\n"+
      "SS.m();";
    assertEquals("Super class method should take precedence over staticly imported member",-2,interpret(testString));
      
    
    //Tests that a non-static method cannot be imported
    testString =
      "package TT;\n"+
      "public class UU {\n"+
      "  public int m1() { return 5;}\n"+
      "}\n"+
      "package VV;\n"+
      "import static TT.UU.*;\n"+
      "public class WW {\n"+
      "  public int m2() { return m1(); } \n"+
      "}\n"+
      "WW ww = new WW(); ww.m2();";
    try {
      assertEquals(5,interpret(testString));
      fail("Non static member should not be imported");
    } 
    catch(InterpreterException e) {
      //Expected to fail
    }
    
    //Tests that a non-static method cannot be imported
    testString =
      "package XX;\n"+
      "public class YY {\n"+
      "  public int m() { return 5;}\n"+
      "}\n"+
      "package ZZ;\n"+
      "import static XX.YY.m;\n";
    try {
      interpret(testString);
      fail("Non static member should not be imported");
    } 
    catch(InterpreterException e) {
      //Expected to fail
    }
  
  }
  
  /**
   * Testing various forms of static importation of fields
   */
  public void testStaticImportOfFields(){
    //Tests simple import on demand
    testString =
      "package A;\n"+
      "public class B { \n"+
      "  public static int C = 5; \n"+
      "}\n"+
      "package D;\n"+
      "import static A.B.*;\n"+
      "C;";
    assertEquals(5,interpret(testString));
    
    //Change packages, should still work
    testString = 
      "package E;\n"+
      "C;";
    assertEquals(5,interpret(testString));
    
    //tests simple single type import
    testString = 
      "public class F{\n"+
      "  public static int C = 3; \n"+
      "}\n"+
      "package G;\n"+
      "import static E.F.C;\n"+
      "C;";
    assertEquals(3,interpret(testString));
    
    //Tests single type import has higher priority than import on demand 
    testString = 
      "public interface H{\n"+
      "  public static int C = 1; \n"+
      "}\n"+
      "public class I implements H {}\n"+
      "package J;\n"+
      "import static G.I.*;\n"+
      "C;";
    //Should not have changed C - last explicit import holds
    assertEquals(3,interpret(testString));
    
    //Tests the import of static member of super class/implemented interface
    testString = 
      "import static G.I.C;\n"+
      "C;";
    assertEquals(1,interpret(testString));
    
    //Tests the import of static member of interface
    testString = 
      "import static G.H.C;\n"+
      "C;";
    assertEquals(1,interpret(testString));
    
    //Tests that assignment to final field fails
    testString =
      "import static java.lang.Integer.MAX_VALUE;\n"+
      "MAX_VALUE;";
    assertEquals(java.lang.Integer.MAX_VALUE,interpret(testString));
    testString = 
      "MAX_VALUE = 1;"+
      "MAX_VALUE;";
    try {
      assertEquals(1,interpret(testString));
      fail("Field is final, should not be mutable");
    }
    catch(InterpreterException e) {
      //Expected to fail
    }
      
    
    //Tests that the redefinition of static final field succeeds
    testString =
      "import static javax.accessibility.AccessibleAction.*;"+
      "DECREMENT;";
    assertEquals(javax.accessibility.AccessibleAction.DECREMENT,interpret(testString));
    testString = 
      "String INCREMENT = \"BLAH!\";"+
      "INCREMENT;";
    try {
      assertEquals("BLAH!",interpret(testString));
    } 
    catch(InterpreterException e) {
      fail("Redefinition of staticly imported field should be allowed!");
    }
    
    //Tests that method parameter has preference over staticly imported field
    testString =
      "package K;\n"+
      "public class L {\n"+
      "  public static int x = 5;\n"+
      "}\n"+
      "package M;\n"+
      "import static K.L.x;\n"+
      "public class N { \n"+
      "  public static int m(int x) { return x; } \n"+
      "}\n"+
      "N.m(3);";
    assertEquals(3,interpret(testString));
      
    
    
    //Tests that a non-static field cannot be imported
    testString =
      "package N;\n"+
      "public class O {\n"+
      "  public int field = 5;\n"+
      "}\n"+
      "package P;\n"+
      "import static N.O.*;\n"+
      "public class Q {\n"+
      "  public int m() { return field; } \n"+
      "}\n"+
      "Q q = new Q(); q.m();";
    try {
      assertEquals(5,interpret(testString));
      fail("Non static member should not be imported");
    } 
    catch(InterpreterException e) {
      //Expected to fail
    }
    
    //Tests that a non-static field cannot be imported
    testString =
      "package R;\n"+
      "public class S {\n"+
      "  public int field = 5;\n"+
      "}\n"+
      "package T;\n"+
      "import static R.S.field;\n";
    try {
      interpret(testString);
      fail("Non static member should not be imported");
    } 
    catch(InterpreterException e) {
      //Expected to fail
    }
  }
      
  
  public void testParseStaticImport(){
    testString =
      "import static java.lang.Integer.MAX_VALUE;";
    
    ImportDeclaration id = new ImportDeclaration("java.lang.Integer.MAX_VALUE", false, true, null, 0, 0, 0, 0);
    assertEquals(id, parse(testString).get(0));
    
  }
  
  public void testParseStaticImportStar(){
    testString =
      "import static java.lang.Integer.*;";
    
    ImportDeclaration id = new ImportDeclaration("java.lang.Integer", true, true, null, 0, 0, 0, 0);
    assertEquals(id, parse(testString).get(0));
  }
  
  public void testNodeEquals(){
    ImportDeclaration id1 = new ImportDeclaration("java.lang.Integer.MAX_VALUE", false, true, null, 0, 0, 0, 0);
    ImportDeclaration id2 = new ImportDeclaration("java.lang.Integer.MAX_VALUE", false, true, null, 0, 0, 0, 0);
    
    assertEquals(id1, id2);
  }
  
  public void testParseVarArgs(){
    testString =
      "public void someMethod(int ... i){}";
    
    LinkedList<FormalParameter> params = new LinkedList<FormalParameter>();
    FormalParameter fp = new FormalParameter(false,new ArrayType(new IntType(),1) ,"i");
    params.add(fp);
    List<Node> statements = new LinkedList<Node>();
    //statements.add(new EmptyStatement());
    BlockStatement body = new BlockStatement(statements);
    
    MethodDeclaration md = new MethodDeclaration(
                                                 java.lang.reflect.Modifier.PUBLIC | 0x00000080, // java.lang.reflect.Modifier.VARARGS == 0x00000080 /**/
                                                 new VoidType(),"someMethod",params,new LinkedList<ReferenceType>(),body,null, 0, 0, 0, 0);
    assertEquals(md, parse(testString).get(0));
  }
  
  public void testInterpretPrimitiveVarArgs(){
    testString =
      "public class C {\n"+
      "  public int someMethod(int ... i){\n"+
      "    return i[3];\n"+
      "  }\n"+
      "}\n"+
      "new C().someMethod(0,1,2,3);";
    
    assertEquals(new Integer(3), interpret(testString));
  }
  
    public void testInterpretObjectVarArgs(){
    testString =
      "public class C {\n"+
      "  public String someMethod(String ... s){\n"+
      "    String returnStr=\"\";\n"+
      "    for(int i=0;i<s.length;i++) {\n"+
      "      returnStr = returnStr+s[i];\n"+
      "    }\n"+
      "    return returnStr;\n"+
      "  }\n"+
      "}\n"+
      "new C().someMethod(\"Str1\", \"Str2\", \"Str3\");\n";
    
    assertEquals("Str1Str2Str3", interpret(testString));
  }
    
    
    //Testing Constructor with varargs
    public void testInterpretConstructorVarArgs(){
      testString =
        "public class C {\n"+
        "  String str = \"\";\n"+
        "  public C(String ... s){\n"+
        "    for(int i=0;i<s.length;i++) {\n"+
        "      str = str+s[i];\n"+
        "    }\n"+
        "  }\n"+
        "  public String getStr(){\n"+
        "    return str;\n"+
        "  }\n"+
        "}\n"+
        "new C(\"Str1\",\"Str2\",\"Str3\").getStr();\n";
    
      assertEquals("Str1Str2Str3", interpret(testString));
    }
    
    // Testing constructor of an inner class with Varargs
    public void testInterpretInnerClassConstructorVarArgs(){
      testString =
        "public class B {\n"+
        "  public class C {\n"+
        "    String str = \"\";\n"+
        "    public C(String ... s){\n"+
        "      for(int i=0;i<s.length;i++) {\n"+
        "        str = str+s[i];\n"+
        "      }\n"+
        "    }\n"+
        "    public String getStr(){\n"+
        "      return str;\n"+
        "    }\n"+
        "  }\n"+
        "}\n"+
        "B b = new B();\n"+
        "b.new C(\"Str1\",\"Str2\",\"Str3\").getStr();\n";
    
      assertEquals("Str1Str2Str3", interpret(testString));
    }
    
    // Testing static method with varargs
    public void testInterpretStaticMethodVarArgs(){
      testString =
        "public class C {\n"+
        "  public static String someMethod(String ... s){\n"+
        "    String returnStr=\"\";\n"+
        "    for(int i=0;i<s.length;i++) {\n"+
        "      returnStr = returnStr+s[i];\n"+
        "    }\n"+
        "    return returnStr;\n"+
        "  }\n"+
        "}\n"+
        "C.someMethod(\"Str1\", \"Str2\", \"Str3\");\n";
      
      assertEquals("Str1Str2Str3", interpret(testString));
    }
        
    //This fails until autoboxing works.
    //Using ByteArrayOutputStream to avoid printing to console (and ByteArrayOutputStream is a non abstract subclass of OutputStream(
    public void testInterpretPrimitivePrintf(){
      testString =
        "import java.io.PrintStream;\n"+
        "import java.io.ByteArrayOutputStream;\n"+
        "PrintStream ps = new PrintStream(new ByteArrayOutputStream());\n"+
        "ps.printf(\"SomeStr %d somestr\",2652)\n;";
      interpret(testString);
    }
      
    //Using ByteArrayOutputStream to avoid printing to console (and ByteArrayOutputStream is a non abstract subclass of OutputStream(
    public void testInterpretMultiplePrintf(){
      testString =
        "import java.io.PrintStream;\n"+
        "import java.io.ByteArrayOutputStream;\n"+
        "PrintStream ps = new PrintStream(new ByteArrayOutputStream());\n"+
        "ps.printf(\"SomeStr %d somestr\",new Integer(2))\n;"+
        "ps.printf(\"SomeStr %s somestr\",\"str\")\n;"+
        "ps.printf(\"SomeStr\",null)\n;"+
        "ps.printf(\"SomeStr %d %s somestr\",new Integer(26),\"str\")\n;\n"+
        "ps.printf(\"SomeStr\");";
        
        interpret(testString);
        
        /** 
         * This test was originally expected to fail, but we found that 
         * this actually was an acceptable varargs behavior
         */
//        try {
//          testString = 
//            "ps.printf(\"SomeStr\")\n;";
//          interpret(testString);
//          fail("Should have failed, as Printf needs some parameters");
//        }
//        catch(InterpreterException ie){
//          //Expected to fail.
//        }
    }
    
}