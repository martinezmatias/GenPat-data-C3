/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2008, JavaPLT group at Rice University (drjava@rice.edu)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of DrJava, the JavaPLT group, Rice University, nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software is Open Source Initiative approved Open Source Software.
 * Open Source Initative Approved is a trademark of the Open Source Initiative.
 * 
 * This file is part of DrJava.  Download the current version of this project
 * from http://www.drjava.org/ or http://sourceforge.net/projects/drjava/
 * 
 * END_COPYRIGHT_BLOCK*/

package  edu.rice.cs.drjava.model.definitions;

import  junit.framework.*;
import  javax.swing.text.BadLocationException;
//import java.io.File;
//import java.io.FileReader;
//import java.io.BufferedReader;
//import java.io.IOException;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.DrJavaTestCase;
import edu.rice.cs.drjava.model.DJDocument;
import edu.rice.cs.drjava.model.definitions.reducedmodel.*;
import edu.rice.cs.drjava.config.*;
import edu.rice.cs.drjava.model.definitions.indent.*;
import edu.rice.cs.drjava.model.GlobalEventNotifier;
//import edu.rice.cs.util.FileOps;
import edu.rice.cs.util.swing.Utilities;

/**
 * Test the tab/enter/squiggly indenting functionality.
 * @version $Id$
 */
public final class IndentTest extends DrJavaTestCase {
  protected DefinitionsDocument doc;

  static String noBrace = IndentInfo.noBrace;
  static String openSquiggly = IndentInfo.openSquiggly;
  static String openParen = IndentInfo.openParen;
  static String openBracket = IndentInfo.openBracket;
  private Integer indentLevel = new Integer(2);
  private GlobalEventNotifier _notifier;

  /** 
   * Tests the indent operation.
   * @param name {@inheritDoc}
   */
  public IndentTest(String name) { super(name); }

  /** Sets up the member bindings common to all tests. */
  public void setUp() throws Exception {
    super.setUp();
    DrJava.getConfig().resetToDefaults();
    _notifier = new GlobalEventNotifier();
    doc = new DefinitionsDocument(_notifier);
    DrJava.getConfig().setSetting(OptionConstants.INDENT_LEVEL,indentLevel);
  }
  
  /** Builds the suite of tests for Indent.class.
   * @return the suite.
   */
  public static Test suite() { return  new TestSuite(IndentTest.class); }

  /** Regression test for comment portion of indent tree. */
  public void testIndentComments() throws BadLocationException {
    String text =
      "  foo();\n" +
      "   // foo\n" +
      "/**\n" +
      "\n" +
      "* Comment\n" +
      "    * More comment\n" +
      "code;\n" +
      "* More comment\n" +
      "\n" +
      "*/\n" +
      "\n";

    String indented =
      "  foo();\n" +     // (skip this line)
      "  // foo\n" +     // align to start of statement
      "  /**\n" +     // start of statement
      "   * \n" +     // add a star after first line
      "   * Comment\n" +     // align to star
      "   * More comment\n" +     // align to star
      "   code;\n" +     // align commented code to stars
      "   * More comment\n" +     // align star after commented code
      "   * \n" +     // add a star after line with star
      "   */\n" +     // align star
      "  \n";     // align close comment to prev statement

    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(9, doc.getLength());
    _assertContents(indented, doc);
  }

  /** Test case for SourceForge bug# 681203. */
  public void testMultiLineStarInsertFirstLine() throws BadLocationException {
    String text =
      "/**\n" +
      "comments here blah blah\n" +
      " */";

    String noStarAdded =
      "/**\n" +
      " comments here blah blah\n" +
      " */";

    String starAdded =
      "/**\n" +
      " * comments here blah blah\n" +
      " */";

    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.gotoLine(2);
    /* First test that indentation caused not by an enter press inserts no star */
    doc._indentLine(Indenter.IndentReason.OTHER);
    _assertContents(noStarAdded, doc);
    /* Now test that indentation caused by an enter press does insert a star */
    doc._indentLine(Indenter.IndentReason.ENTER_KEY_PRESS);
    _assertContents(starAdded, doc);
  }

  /** Test case for SourceForge bug# 681203. */
  public void testMultiLineStarInsertLaterLine() throws BadLocationException {

    String text =
      "/**\n" +
      " * other comments\n" +
      "comments here blah blah\n" +
      " */";

    String noStarAdded =
      "/**\n" +
      " * other comments\n" +
      " comments here blah blah\n" +
      " */";

    String starAdded =
      "/**\n" +
      " * other comments\n" +
      " * comments here blah blah\n" +
      " */";

    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.gotoLine(3);
    /* First test that indentation caused not by an enter press inserts no star */
    doc._indentLine(Indenter.IndentReason.OTHER);
    _assertContents(noStarAdded, doc);
    /* Now test that indentation caused by an enter press does insert a star */
    doc._indentLine(Indenter.IndentReason.ENTER_KEY_PRESS);
    _assertContents(starAdded, doc);
  }

  /**
   * Regression test for paren phrases.
   */
  public void testIndentParenPhrases() throws BadLocationException {
    String text =
      "foo(i,\n" +
      "j.\n" +
      "bar().\n" +
      "// foo();\n" +
      "baz(),\n" +
      "cond1 ||\n" +
      "cond2);\n" +
      "i = myArray[x *\n" +
      "y.\n" +
      "foo() +\n" +
      "z\n" +
      "];\n";

    String indented =
      "foo(i,\n" +
      "    j.\n" +     // new paren phrase
      "      bar().\n" +     // not new paren phrase
      "// foo();\n" +     // not new
      "      baz(),\n" +     // not new (after comment)
      "    cond1 ||\n" +     // new
      "    cond2);\n" +     // new (after operator)
      "i = myArray[x *\n" +     // new statement
      "            y.\n" +     // new phrase
      "              foo() +\n" +     // not new phrase
      "            z\n" +     // new phrase
      "              ];\n";     // not new phrase (debatable)

    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indented, doc);
  }

  /**
   * Regression test for braces.
   */
 public void testIndentBraces() throws BadLocationException {
   String text =
     "{\n" +
     "class Foo\n" +
     "extends F {\n" +
     "int i;   \n" +
     "void foo() {\n" +
     "if (true) {\n" +
     "bar();\n" +
     "}\n" +
     "}\n" +
     "/* comment */ }\n" +
     "class Bar {\n" +
     "/* comment\n" +
     "*/ }\n" +
     "int i;\n" +
     "}\n";

   String indented =
     "{\n" +
     "  class Foo\n" +     // After open brace
     "    extends F {\n" +     // Not new statement
     "    int i;   \n" +     // After open brace
     "    void foo() {\n" +     // After statement
     "      if (true) {\n" +     // Nested brace
     "        bar();\n" +     // Nested brace
     "      }\n" +     // Close nested brace
     "    }\n" +     // Close nested brace
     "  /* comment */ }\n" +     // Close brace after comment
     "  class Bar {\n" +     // After close brace
     "    /* comment\n" +     // After open brace
     "     */ }\n" +      // In comment
     "  int i;\n" +     // After close brace
     "}\n";


   doc.insertString(0, text, null);
   _assertContents(text, doc);
   doc.indentLines(0, doc.getLength());
   _assertContents(indented, doc);
 }

  /**
   * Regression test for arrays.
   */
 public void testIndentArray() throws BadLocationException {
   String text =
     "int[2][] a ={\n" +
     "{\n"  +
     "1,\n" +
     "2,\n" +
     "3},\n" +
     "{\n" +
     "4,\n" +
     "5}\n" +
     "};\n";

   String indented =
     "int[2][] a ={\n" +
     "  {\n"  +
     "    1,\n" +
     "    2,\n" +
     "    3},\n" +
     "  {\n" +
     "    4,\n" +
     "    5}\n" +
     "};\n";




   doc.insertString(0, text, null);
   _assertContents(text, doc);
   doc.indentLines(0, doc.getLength());
   _assertContents(indented, doc);
 }

  /**
   * Regression test for common cases.
   */
  public void testIndentCommonCases() throws BadLocationException {
    String text =
      "int x;\n" +
      "      int y;\n" +
      "  class Foo\n" +
      "     extends F\n" +
      " {\n" +
      "   }";

    String indented =
      "int x;\n" +
      "int y;\n" +
      "class Foo\n" +
      "  extends F\n" +
      "{\n" +
      "}";

    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indented, doc);
  }

  /**
   * Regression test for switch statements.
   */
  public void testIndentSwitch() throws BadLocationException {
    String text =
      "switch (x) {\n" +
      "case 1:\n" +
      "foo();\n" +
      "break;\n" +
      "case 2: case 3:\n" +
      "case 4: case 5:\n" +
      "bar();\n" +
      "break;\n" +
      "}\n";

    String indented =
      "switch (x) {\n" +
      "  case 1:\n" +     // Starting new statement after brace
      "    foo();\n" +     // Not new statement
      "    break;\n" +     // Indent to prev statement
      "  case 2: case 3:\n" +     // Case (indent to stmt of brace)
      "  case 4: case 5:\n" +     // Case (not new stmt)
      "    bar();\n" +     // Not new stmt
      "    break;\n" +     // Indent to prev stmt
      "}\n";     // Close brace


    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indented, doc);
  }

  /**
   * Regression test for ternary operators.
   */
  public void testIndentTernary() throws BadLocationException {
    String text =
      "test1 = x ? y : z;\n" +
      "test2 = x ? y :\n" +
      "z;\n" +
      "foo();\n" +
      "test3 =\n" +
      "x ?\n" +
      "y :\n" +
      "z;\n" +
      "bar();\n" +
      "test4 = (x ?\n" +
      "y :\n" +
      "z);\n";

    String indented =
      "test1 = x ? y : z;\n" +     // ternary on one line
      "test2 = x ? y :\n" +     // ? and : on one line
      "  z;\n" +     // unfinished ternary
      "foo();\n" +     // new stmt
      "test3 =\n" +     // new stmt
      "  x ?\n" +     // not new stmt
      "  y :\n" +     // : with ? in stmt
      "  z;\n" +     // in ternary op
      "bar();\n" +     // new stmt
      "test4 = (x ?\n" +     // ternary in paren
      "           y :\n" +     // : with ? in paren stmt
      "           z);\n";     // in ternary in paren


    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indented, doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoSquiggly() throws BadLocationException {
    //empty document
    BraceReduction _reduced = doc.getReduced();
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, noBrace, -1, -1, -1);
    //single newline
    doc.insertString(0, "\n", null);
    _assertContents("\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, noBrace, -1, -1, 0);
    //single layer brace
    doc.insertString(0, "{\n\n", null);
    // {\n\n#\n
    _assertContents("{\n\n\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, -1, 3, 0);
    //another squiggly
    doc.insertString(3, "{\n\n", null);
    // {\n\n{\n\n#\n
    _assertContents("{\n\n{\n\n\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, 3, 3, 0);
    //brace with whitespace
    doc.insertString(6, "  {\n\n", null);
    // {\n\n{\n\n  {\n\n#\n
    _assertContents("{\n\n{\n\n  {\n\n\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, 5, 3, 0);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoParen() throws BadLocationException {
    // just paren
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "\n(\n", null);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openParen, 2, 2, 0);
    // paren with stuff in front
    doc.insertString(1, "  helo ", null);
    doc.move(2);
    // \n  helo (\n#
    _assertContents("\n  helo (\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openParen, 9, 2, 0);
    //single layer brace
    doc.move(-1);
    doc.insertString(9, " (", null);
    doc.move(1);
    // \n  helo ( (\n#
    _assertContents("\n  helo ( (\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openParen, 11, 2, 0);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoBracket() throws BadLocationException {
    // just bracket
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "\n[\n", null);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openBracket, 2, 2, 0);
    // bracket with stuff in front
    doc.insertString(1, "  helo ", null);
    doc.move(2);
    // \n  helo (\n#
    _assertContents("\n  helo [\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openBracket, 9, 2, 0);
    //single layer brace
    doc.move(-1);
    doc.insertString(9, " [", null);
    doc.move(1);
    // \n  helo ( (\n#
    _assertContents("\n  helo [ [\n", doc);
    ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openBracket, 11, 2, 0);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoPrevNewline () throws BadLocationException {
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "{\n  {\nhello", null);
    // {\n  {\nhello#
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, 9, 7, 5);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testEndOfBlockComment () throws BadLocationException {
    doc.insertString(0, "\n{\n  hello;\n /*\n hello\n */", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n{\n  hello;\n /*\n hello\n */", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testAfterBlockComment () throws BadLocationException {
    doc.insertString(0, "\n{\n  hello;\n  /*\n  hello\n  */\nhello", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n{\n  hello;\n  /*\n  hello\n  */\n  hello", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testAfterBlockComment3 () throws BadLocationException {
    doc.insertString(0, "\n{\n  hello;\n  /*\n  hello\n  grr*/\nhello", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n{\n  hello;\n  /*\n  hello\n  grr*/\n  hello", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testAfterBlockComment4 () throws BadLocationException {
    doc.insertString(0, "\n{\n  hello;\n /*\n  hello\n */ hello", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n{\n  hello;\n /*\n  hello\n  */ hello", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testAfterBlockComment2 () throws BadLocationException {
    doc.insertString(0, "\n{\n  hello;\n  /*\n  hello\n  */ (\nhello", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n{\n  hello;\n  /*\n  hello\n  */ (\n      hello", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoBlockComments () throws BadLocationException {
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "(\n /*\n*\n", null);
    // (\n/*\n*#\n
    _reduced.move(-1);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openParen, -1, 7, 1);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoBlockComments2 () throws BadLocationException {
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "\n(\n /*\n*\n", null);
    // \n(\n/*\n*#\n
    _reduced.move(-1);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openParen, 7, 7, 1);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoBlockComments3 () throws BadLocationException {
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "{\n  /*\n*\n", null);
    // (\n/*\n*#\n
    _reduced.move(-1);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, -1, 8, 1);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInfoBlockComments4 () throws BadLocationException {
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "\n{\n  /*\n*\n", null);
    // \n(\n/*\n*#\n
    _reduced.move(-1);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, 8, 8, 1);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSkippingBraces () throws BadLocationException {
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "\n{\n   { ()}\n}", null);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, 12, 12, 1);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSkippingComments () throws BadLocationException {
    // just paren
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "\n{\n   //{ ()\n}", null);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, 13, 13, 1);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSkippingCommentsBraceAtBeginning () throws BadLocationException {
    // just paren
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "{\n   //{ ()}{", null);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, openSquiggly, -1, 13, 11);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testNothingToIndentOn () throws BadLocationException {
    // just paren
    BraceReduction _reduced = doc.getReduced();
    doc.insertString(0, "   //{ ()}{", null);
    IndentInfo ii = _reduced.getIndentInformation();
    _assertIndentInfo(ii, noBrace, -1, -1, -1);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testStartSimple () throws BadLocationException {
    // just paren
    doc.insertString(0, "abcde", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("abcde", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testStartSpaceIndent () throws BadLocationException {
    // just paren
    doc.insertString(0, "  abcde", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("abcde", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testStartBrace () throws BadLocationException {
    // just paren
    doc.insertString(0, "public class temp \n {", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("public class temp \n{", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testEndBrace () throws BadLocationException {
    // just paren
    doc.insertString(0, "public class temp \n{ \n  }", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("public class temp \n{ \n}", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testInsideClass () throws BadLocationException {
    // just paren
    doc.insertString(0, "public class temp \n{ \ntext here", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("public class temp \n{ \n  text here", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testInsideClassWithBraceSets () throws BadLocationException {
    // just paren
    doc.insertString(0, "public class temp \n{  ()\ntext here", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("public class temp \n{  ()\n  text here", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIgnoreBraceOnSameLine () throws BadLocationException {
    // just paren
    doc.insertString(0, "public class temp \n{  ()\n{text here", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("public class temp \n{  ()\n  {text here", doc);
  }

  /**
   * Not supported any more.
   *
  public void testLargerIndent () throws BadLocationException {
    // just paren
    BraceReduction rm = doc.getReduced();
    doc.insertString(0, "public class temp \n  {  ()\n { text here", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("public class temp \n  {  ()\n    { text here", doc);
  }*/

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testWeird () throws BadLocationException {
    // just paren
    doc.insertString(0, "hello\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("hello\n  ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testWierd2 () throws BadLocationException {
    // just paren
    doc.insertString(0, "hello", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("hello", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testMotion () throws BadLocationException {
    // just paren
    doc.insertString(0, "hes{\n{abcde", null);
    doc.insertString(11, "\n{", null);
    // hes{\n{abcde\n{#
    doc.move(-8);
    // hes{\n#{abcde\n{
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    // hes{\n  #{abcde\n{
    _assertContents("hes{\n  {abcde\n{", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testNextCharIsNewline () throws BadLocationException {
    // just paren
    doc.insertString(0, "hes{\n{abcde", null);
    doc.insertString(11, "\n{", null);
    // hes{\n{abcde\n{#
    doc.move(-2);
    // hes{\n{abcde#\n{
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    // hes{\n  {abcde#\n{
    _assertContents("hes{\n  {abcde\n{", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testFor () throws BadLocationException {
    // just paren
    doc.insertString(0, "for(;;)\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("for(;;)\n  ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testFor2 () throws BadLocationException {
    // just paren
    doc.insertString(0, "{\n  for(;;)\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("{\n  for(;;)\n    ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testOpenParen () throws BadLocationException {
    // just paren
    doc.insertString(0, "hello(\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("hello(\n      ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testPrintString () throws BadLocationException {
    // just paren
    doc.insertString(0, "Sys.out(\"hello\"\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("Sys.out(\"hello\"\n          ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testOpenBracket () throws BadLocationException {
    // just paren
    doc.insertString(0, "hello[\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("hello[\n      ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSquigglyAlignment () throws BadLocationException {
    // just paren
    doc.insertString(0, "{\n  }", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("{\n}", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSpaceBrace () throws BadLocationException {
    // just paren
    doc.insertString(0, "   {\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("   {\n     ", doc);
  }

  /**
   * Cascading indent is not used anymore.
   *
  public void testOpenSquigglyCascade () throws BadLocationException {
    // just paren
    BraceReduction rm = doc.getReduced();
    doc.insertString(0, "if\n  if\n    if\n{", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("if\n  if\n    if\n    {", doc);
  }*/

  /**
   * Cascading indent is not used anymore.
   *
  public void testOpenSquigglyCascade2 () throws BadLocationException {
    // just paren
    BraceReduction rm = doc.getReduced();
    doc.insertString(0, "{\n  if\n    if\n      if\n{", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("{\n  if\n    if\n      if\n      {", doc);
  }*/

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testEnter () throws BadLocationException {
    // just paren
    doc.insertString(0, "\n\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n\n", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testEnter2 () throws BadLocationException {
    // just paren
    doc.insertString(0, "\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testNotRecognizeComments () throws BadLocationException {
    // just paren
    doc.insertString(0, "\nhello //bal;\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\nhello //bal;\n  ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testNotRecognizeComments2 () throws BadLocationException {
    // just paren
    doc.insertString(0, "\nhello; /*bal*/\n ", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\nhello; /*bal*/\n", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testBlockIndent () throws BadLocationException {
    // just paren
    doc.insertString(0, "hello\n{\n{\n  {", null);
    doc.indentLines(8, 13);
    _assertContents("hello\n{\n  {\n    {", doc);
  }

  /**
   * Regression test for bug in drjava-20010802-1020:
   * Indent block on a file containing just "  x;\n  y;\n" would throw an
   * exception.
   * @exception BadLocationException
   */
  public void testBlockIndent2 () throws BadLocationException {
    doc.insertString(0, "  x;\n  y;\n", null);
    doc.indentLines(0, doc.getLength());
    _assertContents("x;\ny;\n", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testIndentInsideCommentBlock () throws BadLocationException {
    doc.insertString(0, "hello\n{\n/*{\n{\n*/\nhehe", null);
    doc.indentLines(0, 21);
    _assertContents("hello\n{\n  /*{\n   {\n   */\n  hehe", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSecondLineProblem () throws BadLocationException {
    // just paren
    doc.insertString(0, "\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSecondLineProblem2 () throws BadLocationException {
    // just paren
    doc.insertString(0, "a\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("a\n  ", doc);
  }

  /**
   * put your documentation comment here
   * @exception BadLocationException
   */
  public void testSmallFileProblem () throws BadLocationException {
    // just paren
    doc.insertString(0, "\n\n", null);
    doc.indentLines(doc.getCurrentLocation(), doc.getCurrentLocation());
    _assertContents("\n\n", doc);
  }

  /**
   * Regression test for arrays.
   */
  public void testAnonymousInnerClass() throws BadLocationException {
    String text =
      "addWindowListener(new WindowAdapter() {\n" +
      "public void windowClosing(WindowEvent e) {\n" +
      "dispose();\n" +
      "}\n" +
      "void x() {\n" +
      "\n" +
      "}\n" +
      "\n" +
      "}\n" +
      ");\n";
    String indented =
      "addWindowListener(new WindowAdapter() {\n" +
      "  public void windowClosing(WindowEvent e) {\n" +
      "    dispose();\n" +
      "  }\n" +
      "  void x() {\n" +
      "    \n" +
      "  }\n" +
      "  \n" +
      "}\n" +
      ");\n";


    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indented, doc);
  }


//  /** Regression test for Bug #627753.  Uncomment when it is fixed.
//   */
//  public void testNestedUnbracedFor() throws BadLocationException {
//    String text =
//      "for (int a =0; a < 5; a++)\n" +
//      "for (int b = 0; b < 5; b++) {\n" +
//      "System.out.println(a + b);";
//    String indented =
//      "for (int a =0; a < 5; a++)\n" +
//      "  for (int b = 0; b < 5; b++) {\n" +
//      "    System.out.println(a + b);";
//    doc.insertString(0, text, null);
//    _assertContents(text, doc);
//    doc.indentLines(0, doc.getLength());
//    _assertContents(indented, doc);
//    doc.remove(0,doc.getLength() - 1);
//
//    text =
//      "if (true)\n" +
//      "if (true)\n" +
//      "System.out.println(\"Hello\");";
//    indented =
//      "if (true)\n" +
//      "  if (true)\n" +
//      "    System.out.println(\"Hello\");";
//    doc.insertString(0, text, null);
//    _assertContents(text, doc);
//    doc.indentLines(0, doc.getLength());
//    _assertContents(indented, doc);
//    doc.remove(0,doc.getLength() - 1);
//
//    text =
//      "{\n" +
//      "while (a < 5)\n" +
//      "while (b < 5) {\n" +
//      "System.out.println(a + b);";
//    indented =
//      "{\n" +
//      "  while (a < 5)\n" +
//      "    while (b < 5) {\n" +
//      "      System.out.println(a + b);";
//    doc.insertString(0, text, null);
//    _assertContents(text, doc);
//    doc.indentLines(0, doc.getLength());
//    _assertContents(indented, doc);
//    doc.remove(0,doc.getLength() - 1);
//
//    text =
//      "while (a < 5)\n" +
//      "while (b < 5);\n" +
//      "System.out.println(a + b);";
//    indented =
//      "while (a < 5)\n" +
//      "  while (b < 5);\n" +
//      "System.out.println(a + b);";
//    doc.insertString(0, text, null);
//    _assertContents(text, doc);
//    doc.indentLines(0, doc.getLength());
//    _assertContents(indented, doc);
//    doc.remove(0,doc.getLength() - 1);
//
//    text =
//      "do\n" +
//      "do\n" +
//      "x=5;\n" +
//      "while(false);\n" +
//      "while(false);\n";
//    indented =
//      "do\n" +
//      "  do\n" +
//      "    x=5;\n" +
//      "  while(false);\n" +
//      "while(false);\n";
//    doc.insertString(0, text, null);
//    _assertContents(text, doc);
//    doc.indentLines(0, doc.getLength());
//    _assertContents(indented, doc);
//    doc.remove(0,doc.getLength() - 1);
//  }

  public void testLiveUpdateOfIndentLevel() throws BadLocationException {

    String text =
      "int[2][] a ={\n" +
      "{\n"  +
      "1,\n" +
      "2,\n" +
      "3},\n" +
      "{\n" +
      "4,\n" +
      "5}\n" +
      "};\n";

    String indentedBefore =
      "int[2][] a ={\n" +
      "  {\n"  +
      "    1,\n" +
      "    2,\n" +
      "    3},\n" +
      "  {\n" +
      "    4,\n" +
      "    5}\n" +
      "};\n";

    String indentedAfter =
      "int[2][] a ={\n" +
      "        {\n" +
      "                1,\n" +
      "                2,\n" +
      "                3},\n" +
      "        {\n" +
      "                4,\n" +
      "                5}\n" +
      "};\n";

    doc.insertString(0, text, null);

    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indentedBefore, doc);
    DrJava.getConfig().setSetting(OptionConstants.INDENT_LEVEL, new Integer(8));
    
    Utilities.clearEventQueue();
    doc.indentLines(0, doc.getLength());
    _assertContents(indentedAfter, doc);
  }

  /**
   * tests that an if statment nested in a switch will be indented properly
   * @throws BadLocationException
   */
  public void testNestedIfInSwitch() throws BadLocationException {
    String text =
      "switch(cond) {\n" +
      "case 1:\n" +
      "object.doStuff();\n" +
      "if(object.hasDoneStuff()) {\n" +
      "thingy.doOtherStuff();\n" +
      "lion.roar(\"raaargh\");\n" +
      "}\n" +
      "break;\n" +
      "}\n";

    String indented =
      "switch(cond) {\n" +
      "  case 1:\n" +
      "    object.doStuff();\n" +
      "    if(object.hasDoneStuff()) {\n" +
      "      thingy.doOtherStuff();\n" +
      "      lion.roar(\"raaargh\");\n" +
      "    }\n" +
      "    break;\n" +
      "}\n";

    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indented, doc);
  }

//  Commented out because reference files are missing!
//  /** Tests a list of files when indented match their correct indentations */
//  public void testIndentationFromFile() throws IOException {
//    File directory = new File("testFiles");
//
//    File[] unindentedFiles = {new File(directory, "IndentSuccesses.indent")
//         /*, new File(directory, "IndentProblems.indent")*/};
//    File[] correctFiles = {new File(directory, "IndentSuccessesCorrect.indent")
//      /*, new File(directory, "IndentProblemsCorrect.indent")*/};
//
//    for (int x = 0; x < correctFiles.length; x++) {
//      _indentAndCompare(unindentedFiles[x], correctFiles[x]);
//    }
//
//    //We know the following test file should (currently) fail, so we assert that it will fail to check
//    //our _indentAndCompare(...) function
//    boolean threwAFE = false;
//    try {
//      _indentAndCompare(new File(directory, "IndentProblems.indent"),
//                        new File(directory, "IndentProblemsCorrect.indent"));
//    }
//    catch(AssertionFailedError afe) {
//      threwAFE = true;
//    }
//    if (!threwAFE) {
//      fail("_indentAndCompare should have failed for IndentProblems.indent");
//    }
//  }
  
  public void testIndentingCorrectLine() throws BadLocationException {
    String test1 = 
      "class A {\n" +
      "  int a = 5;\n" +
      "     }";
    
    String test1Correct =
      "class A {\n" +
      "  int a = 5;\n" +
      "}";
    
    String test2 = 
      "     {\n" +
      "  int a = 5;\n" +
      "  }\n";
    
    String test2Correct =
      "{\n" +
      "  int a = 5;\n" +
      "  }\n";
    
    doc.insertString(0, test1, null);
    _assertContents(test1, doc);
    doc.setCurrentLocation(20);
    doc.indentLines(20,20);
//    System.out.println("test1 = \n" + test1 + "\n length = " + test1.length());
//    System.out.println("test1 = \n" + doc.getText() + "\n length = " + doc.getLength());
    _assertContents(test1, doc);
    
    doc = new DefinitionsDocument(_notifier);
    
    doc.insertString(0, test1, null);
    _assertContents(test1, doc);
    doc.indentLines(28,28);
    _assertContents(test1Correct, doc);
    
    doc = new DefinitionsDocument(_notifier);
    
    doc.insertString(0, test2, null);
    _assertContents(test2, doc);
    doc.setCurrentLocation(5);
    doc.indentLines(5,5);
    _assertContents(test2Correct, doc);
  }
  
  /**
   * Tests that annotations do not change the indent level of the lines following.
   * @throws BadLocationException
   */
  public void testAnnotationsAfterOpenCurly() throws BadLocationException {
    String textToIndent =
      "@Annotation\n" +
      "public class TestClass {\n" +
      "public TestClass() {}\n" +
      "\n" +
      "@Annotation(WithParens)\n" +
      "private int _classField = 42;\n" +
      "\n" +
      "@Override\n" +
      "public String toString() {\n" +
      "@LocalVariableAnnotation\n" +
      "String msg = \"hello\";\n" +
      "return msg;\n" +
      "}\n" +
      "\n" +
      "public int methodAfterAnnotation() {\n" +
      "return 0;\n" +
      "}\n" +
      "}\n" +
      "\n";
    String textIndented = 
      "@Annotation\n" +
      "public class TestClass {\n" +
      "  public TestClass() {}\n" +
      "  \n" +
      "  @Annotation(WithParens)\n" +
      "  private int _classField = 42;\n" +
      "  \n" +
      "  @Override\n" +
      "  public String toString() {\n" +
      "    @LocalVariableAnnotation\n" +
      "    String msg = \"hello\";\n" +
      "    return msg;\n" +
      "  }\n" +
      "  \n" +
      "  public int methodAfterAnnotation() {\n" +
      "    return 0;\n" +
      "  }\n" +
      "}\n" +
      "\n";
    
    doc.insertString(0, textToIndent, null);
    _assertContents(textToIndent, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(textIndented, doc);
  }

  /**
   * Tests that annotations do not change the indent level of the lines following.
   * @throws BadLocationException
   */
  public void testAnnotationsAfterDefinition() throws BadLocationException {
    String textToIndent =
      "@Annotation\n" +
      "public class TestClass {\n" +
      "public TestClass() {}\n" +
      "\n" +
      "private int _classField = 0;\n" +
      "\n" +
      "@Annotation(WithParens)\n" +
      "private int _classField2 = 42;\n" +
      "\n" +
      "@Override\n" +
      "public String toString() {\n" +
      "@LocalVariableAnnotation\n" +
      "String msg = \"hello\";\n" +
      "return msg;\n" +
      "}\n" +
      "\n" +
      "public int methodAfterAnnotation() {\n" +
      "return 0;\n" +
      "}\n" +
      "}\n";
    String textIndented = 
      "@Annotation\n" +
      "public class TestClass {\n" +
      "  public TestClass() {}\n" +
      "  \n" +
      "  private int _classField = 0;\n" +
      "  \n" +
      "  @Annotation(WithParens)\n" +
      "  private int _classField2 = 42;\n" +
      "  \n" +
      "  @Override\n" +
      "  public String toString() {\n" +
      "    @LocalVariableAnnotation\n" +
      "    String msg = \"hello\";\n" +
      "    return msg;\n" +
      "  }\n" +
      "  \n" +
      "  public int methodAfterAnnotation() {\n" +
      "    return 0;\n" +
      "  }\n" +
      "}\n";
    
    doc.insertString(0, textToIndent, null);
    _assertContents(textToIndent, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(textIndented, doc);
  }
  
  /**
   * tests that an if statment nested in a switch will be indented properly
   * this, as opposed to the previous test, does not have any code in that case
   * except the if statement
   * @throws BadLocationException
   */
/*  public void testNestedIfInSwitch2() throws BadLocationException {
    String text =
      "switch(c) {\n" +
      "case 2:\n" +
      "break;\n" +
      "case 3:\n" +
      "if(owner.command() == ROLL_OVER) {\n" +
      "dog.rollOver();\n" +
      "}\n" +
      "break;\n" +
      "}\n";

    String indented =
      "switch(c) {\n" +
      "  case 2:\n" +
      "    break;\n" +
      "  case 3:\n" +
      "    if(owner.command() == ROLL_OVER) {\n" +
      "      dog.rollOver();\n" +
      "    }\n" +
      "    break;\n" +
      "}\n";

    doc.insertString(0, text, null);
    _assertContents(text, doc);
    doc.indentLines(0, doc.getLength());
    _assertContents(indented, doc);
  }
*/
  private void _assertContents(String expected, DJDocument document) throws BadLocationException {
    assertEquals("document contents", expected, document.getText());
  }

  private void _assertIndentInfo(IndentInfo ii, String braceType, int distToNewline, int distToBrace, int distToPrevNewline) {
    assertEquals("indent info: brace type", braceType, ii.braceType);
    assertEquals("indent info: dist to new line", distToNewline, ii.distToNewline);
    assertEquals("indent info: dist to brace", distToBrace, ii.distToBrace);
    assertEquals("indent info: dist to prev new line", distToPrevNewline, ii.distToPrevNewline);
  }

//  /** Copies fromFile to toFile, assuming both files exist. */
//  private void _copyFile(File fromFile, File toFile) throws IOException {
//    String text = FileOps.readFileAsString(fromFile);
//    FileOps.writeStringToFile(toFile, text);
//    String newText = FileOps.readFileAsString(toFile);
//    assertEquals("File copy verify", text, newText);
//  }

//  /**
//   * indents one file, compares it to the other, reindents and recompares
//   * to make sure indent(x) = indent(indent(x))
//   */
//  private void _indentAndCompare(File unindented, File correct)
//    throws IOException
//  {
//    File test = null;
//    try {
//      test = File.createTempFile("test", ".java");
//      _copyFile(unindented, test);
//      test.deleteOnExit();
//      IndentFiles.main(new String[] {"-silent", test.toString()});
//      _fileCompare(test, correct);
//      IndentFiles.main(new String[] {"-silent", test.toString()});
//      _fileCompare(test, correct);
//    }
//    finally {
//      if (test != null) {
//        test.delete();
//      }
//    }
//
//  }

//  /**
//   * @throws AssertionFailedError if the files are not identical
//   */
//  private void _fileCompare(File test, File correct) throws IOException {
//    FileReader fr = new FileReader(correct);
//    FileReader fr2 = new FileReader(test);
//    BufferedReader correctBufferedReader = new BufferedReader(fr);
//    BufferedReader testBufferedReader = new BufferedReader(fr2);
//
//    String correctString = correctBufferedReader.readLine();
//    String testString = testBufferedReader.readLine();
//    int lineNo = 1;
//    while (correctString != null && testString != null) {
//      assertEquals("File: " + correct + " line: " + lineNo, correctString, testString);
//      correctString = correctBufferedReader.readLine();
//      testString = testBufferedReader.readLine();
//      lineNo++;
//    }
//    assertTrue("Indented file longer than expected", correctString == null);
//    assertTrue("Indented file shorter than expected", testString == null);
//
//    testBufferedReader.close();
//    correctBufferedReader.close();
//    fr.close();
//    fr2.close();
//  }

/*
  public void testNoParameters() throws BadLocationException
  {
    IndentRuleAction _action = new ActionBracePlus("");

    String _text =
      "method(\n"+
      ")\n";

    String _aligned =
      "method(\n"+
      "      )\n";

    doc.insertString(0, _text, null);
    _action.indentLine(doc, 0); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());
    doc.indentLines(0, 7); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());

    doc.indentLines(0, doc.getLength()); // Aligns second line, a second time.
    System.out.println(doc.getText());
    _assertContents(_aligned, doc);
    assertEquals("Line aligned to open paren.", _aligned.length(), doc.getLength());
  }
*/
/*
  public void testArrayInit() throws BadLocationException
  {
    IndentRuleAction _action = new ActionBracePlus("");

    String _text =
      "int[] ar = new int[] {\n"+
      "1,1,1,1,1,1,1,1,1 };";

    String _aligned =
      "int[] ar = new int[] {\n"+
      "                      1,1,1,1,1,1,1,1,1 };";

    doc.insertString(0, _text, null);
    _action.indentLine(doc, 0); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());
    doc.indentLines(0, 7); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());

    doc.indentLines(0, doc.getLength()); // Aligns second line, a second time.
    System.out.println(doc.getText());
    _assertContents(_aligned, doc);
    assertEquals("Line aligned to open paren.", _aligned.length(), doc.getLength());
  }
*/
/*
  public void testArrayInitNewline() throws BadLocationException
  {
    IndentRuleAction _action = new ActionBracePlus("");

    String _text =
      "int[] ar = new int[] { 1,1,1,\n"+
      "1,1,1,1,1,1 };";

    String _aligned =
      "int[] ar = new int[] { 1,1,1,\n"+
      "                       1,1,1,1,1,1 };";

    doc.insertString(0, _text, null);
    _action.indentLine(doc, 0); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());
    doc.indentLines(0, 7); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());

    doc.indentLines(0, doc.getLength()); // Aligns second line, a second time.
    System.out.println(doc.getText());
    _assertContents(_aligned, doc);
    assertEquals("Line aligned to open paren.", _aligned.length(), doc.getLength());
  }
*/
/*
  public void testArrayInitBraceNewline() throws BadLocationException
  {
    IndentRuleAction _action = new ActionBracePlus("");

    String _text =
      "int[] blah = new int[] {1, 2, 3\n"+
      "};";

    String _aligned =
      "int[] blah = new int[] {1, 2, 3\n"+
      "                        };";

    doc.insertString(0, _text, null);
    _action.indentLine(doc, 0); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());
    doc.indentLines(0, 7); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());

    doc.indentLines(0, doc.getLength()); // Aligns second line, a second time.
    System.out.println(doc.getText());
    _assertContents(_aligned, doc);
    assertEquals("Line aligned to open paren.", _aligned.length(), doc.getLength());
  }
*/
/*
  public void testArrayInitAllNewline() throws BadLocationException
  {
    IndentRuleAction _action = new ActionBracePlus("");

    String _text =
      "int[] blah = new int[]\n"+
      "{4, 5, 6};";

    String _aligned =
      "int[] blah = new int[]\n"+
      "  {4, 5, 6};";

    doc.insertString(0, _text, null);
    _action.indentLine(doc, 0); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());
    doc.indentLines(0, 7); // Does nothing.
    assertEquals("START has no brace.", _text.length(), doc.getLength());

    doc.indentLines(0, doc.getLength()); // Aligns second line, a second time.
    System.out.println(doc.getText());
    _assertContents(_aligned, doc);
    assertEquals("Line aligned to open paren.", _aligned.length(), doc.getLength());
  }
*/
}
