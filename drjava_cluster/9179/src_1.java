/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is part of DrJava.  Download the current version of this project from http://www.drjava.org/
 * or http://sourceforge.net/projects/drjava/
 *
 * DrJava Open Source License
 * 
 * Copyright (C) 2001-2005 JavaPLT group at Rice University (javaplt@rice.edu).  All rights reserved.
 *
 * Developed by:   Java Programming Languages Team, Rice University, http://www.cs.rice.edu/~javaplt/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the "Software"), to deal with the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 *     - Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 *       following disclaimers.
 *     - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
 *       following disclaimers in the documentation and/or other materials provided with the distribution.
 *     - Neither the names of DrJava, the JavaPLT, Rice University, nor the names of its contributors may be used to 
 *       endorse or promote products derived from this Software without specific prior written permission.
 *     - Products derived from this software may not be called "DrJava" nor use the term "DrJava" as part of their 
 *       names without prior written permission from the JavaPLT group.  For permission, write to javaplt@rice.edu.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * WITH THE SOFTWARE.
 * 
 *END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model;

import java.io.*;
import javax.swing.text.BadLocationException;
import javax.swing.event.*;
import java.util.Vector;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.config.*;
import edu.rice.cs.drjava.model.repl.*;
import edu.rice.cs.util.Log;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.text.EditDocumentException;
import edu.rice.cs.util.swing.Utilities;

/** A test on the GlobalModel that does deals with everything outside of simple file operations, e.g., compile, quit.
 *  @version $Id$
 */
public final class GlobalModelOtherTest extends GlobalModelTestCase implements OptionConstants {
  
  //  _log can be inherited from GlobalModelTestCase
  Log _log = new Log("GlobalModelOtherTest.txt", false);
  
  private static final String FOO_CLASS =
    "package bar;\n" +
    "public class Foo {\n" +
    "  public static void main(String[] args) {\n" +
    "    System.out.println(\"Foo\");\n" +
    "  }\n" +
    "}\n";
  
  /** Get the canonical name of a file.  If the operation fails, the test will fail. */
  private File makeCanonical(File f) {
    try { return f.getCanonicalFile(); }
    catch (IOException e) { fail("Can't get a canonical path for file " + f); return null; }
  }

  /** Tests that the undoableEditHappened event is fired if the undo manager is in use. */
  public void testUndoEventsOccur() throws BadLocationException {
    final OpenDefinitionsDocument doc = _model.newFile();

    // Have to add an undoable edit listener for Undo to work
    doc.addUndoableEditListener(new UndoableEditListener() {
      public void undoableEditHappened(UndoableEditEvent e) { doc.getUndoManager().addEdit(e.getEdit()); }
    });

    TestListener listener = new TestListener() { public void undoableEditHappened() { undoableEditCount++; } };
    
    _model.addListener(listener);
    changeDocumentText("test", doc);
    
    Utilities.clearEventQueue();
    _model.removeListener(listener);
    listener.assertUndoableEditCount(1);
//    Utilities.showDebug("testUndoEvents finished");
    
//    _log.log("testUndoEventsOccur() completed");
  }

  /** Checks that System.exit is handled appropriately from interactions pane. */
  public void testExitInteractions() throws EditDocumentException, InterruptedException{
    InteractionListener listener = new InteractionListener() {

//      public void consoleReset() { consoleResetCount++; }
    };

    _model.addListener(listener);

    listener.logInteractionStart();
    interpretIgnoreResult("System.exit(23);"); 
    listener.waitInteractionDone();
    listener.waitResetDone();
    Utilities.clearEventQueue();
      
    _model.removeListener(listener);

//    listener.assertConsoleResetCount(0);
    listener.assertInteractionStartCount(1);
    listener.assertInterpreterResettingCount(1);
    listener.assertInterpreterReadyCount(1);
    listener.assertInterpreterExitedCount(1);
    assertEquals("exit status", 23, listener.getLastExitStatus());

    _log.log("testExitInteractions() completed");
  }

  /** Checks that the interpreter can be aborted and then work correctly later. Part of what we check here is that 
   *  the interactions classpath is correctly reset after aborting interactions. That is, we ensure that the compiled
   *  class is still visible after aborting. This was broken in drjava-20020108-0958 -- or so I thought. I can't 
   *  consistently reproduce the problem in the UI (seems to show up using IBM's JDK only), and I can never reproduce 
   *  it in the test case. Grr. <p> OK, now I found the explanation: We were in some cases running two new JVMs
   *  on an abort. I fixed the problem in MainJVM#restartInterpreterJVM
   *
   *  The above method no longer exists...  Does anyone remember what this meant? -nrh
   */
  public void testInteractionAbort() throws BadLocationException, EditDocumentException, InterruptedException, 
    IOException {
    
    doCompile(setupDocument(FOO_TEXT), tempFile());
    
    Utilities.clearEventQueue();

    final String beforeAbort = interpret("DrJavaTestFoo.class.getName()"); /* interpret("17"); */
    
    _log.log("Completed initial interpret call");
    
    assertEquals("\"DrJavaTestFoo\"", beforeAbort);
    
    Utilities.clearEventQueue();

    InteractionListener listener = new InteractionListener();

    _model.addListener(listener);
    listener.logInteractionStart();
    _log.log("Starting infinite loop");
    interpretIgnoreResult("while (true) {}");
    Utilities.clearEventQueue();
    
    listener.assertInteractionStartCount(1);
    
    _log.log("Resetting interactions");
    _model.resetInteractions(FileOption.NULL_FILE);
    listener.waitResetDone();
    
    Utilities.clearEventQueue();

    listener.assertInterpreterResettingCount(1);
    listener.assertInterpreterReadyCount(1);
    listener.assertInterpreterExitedCount(0);
    listener.assertConsoleResetCount(0);

    // now make sure it still works!
    assertEquals("5", interpret("5"));
    _model.removeListener(listener);

    // make sure we can still see class foo
    final String afterAbort = interpret("DrJavaTestFoo.class.getName()");
    Utilities.clearEventQueue();
    assertEquals("\"DrJavaTestFoo\"", afterAbort);
    _log.log("testInteractionAbort() completed");
  }

  /** Checks that reset console works. */
  public void testResetConsole() throws EditDocumentException, InterruptedException {
    //System.err.println("Entering testResetConsole");
    InteractionListener listener = new InteractionListener();

    _model.addListener(listener);
    
    listener.logInteractionStart();

    _model.resetConsole();
    Utilities.clearEventQueue();
    
    assertEquals("Length of console text", 0, _model.getConsoleDocument().getLength());

    listener.assertConsoleResetCount(1);
    
    listener.logInteractionStart();    // only resets the interactionDone and resetDone flags
    listener.resetConsoleResetCount(); // resets the resetConsoleCount in the listener
    
    interpretIgnoreResult("System.out.print(\"a\");");
    listener.waitInteractionDone();
    
    Utilities.clearEventQueue();
    
    assertEquals("Length of console text", 1, _model.getConsoleDocument().getLength());

    _model.resetConsole();
    
    Utilities.clearEventQueue();
    assertEquals("Length of console text", 0, _model.getConsoleDocument().getLength());

    listener.assertConsoleResetCount(1);
    _log.log("testResetConsole() completed");
  }

  /** Creates a new class, compiles it and then checks that the REPL can see it.  Then checks that a compiled class
   *  file in another directory can be both accessed and extended if it is on the "extra.classpath" config option.
   */
  public void testInteractionsCanSeeCompiledClasses() throws BadLocationException, EditDocumentException,
    IOException, InterruptedException {
    // Compile Foo
    OpenDefinitionsDocument doc1 = setupDocument(FOO_TEXT);
    File dir1 = makeCanonical(new File(_tempDir, "dir1"));
    dir1.mkdir();
    File file1 = makeCanonical(new File(dir1, "TestFile1.java"));
    doCompile(doc1, file1);

    assertEquals("interactions result", "\"DrJavaTestFoo\"", interpret("new DrJavaTestFoo().getClass().getName()"));

    // Add directory 1 to extra classpath and close doc1
    Vector<File> cp = new Vector<File>();
    cp.add(dir1);
    DrJava.getConfig().setSetting(EXTRA_CLASSPATH, cp);
    
    Utilities.clearEventQueue();
    _model.closeFile(doc1);

    // Compile Baz which extends Foo in another directory.
    OpenDefinitionsDocument doc2 = setupDocument(BAZ_TEXT);
    File dir2 = makeCanonical(new File(_tempDir, "dir2"));
    dir2.mkdir();
    File file2 = makeCanonical(new File(dir2, "TestFile1.java"));
    doCompile(doc2, file2);

    // Ensure that Baz can use the Foo class from extra classpath
    assertEquals("interactions result", "\"DrJavaTestBaz\"", interpret("new DrJavaTestBaz().getClass().getName()"));

    // Ensure that static fields can be seen
    assertEquals("result of static field", "3", interpret("DrJavaTestBaz.x"));

    // Also ensure that Foo can be used directly
    assertEquals("interactions result", "\"DrJavaTestFoo\"", interpret("new DrJavaTestFoo().getClass().getName()"));
    
    _log.log("testInteractionsCanSeeCompletedClasses() completed");
  }

  /**  Compiles a new class in the default package with a mixed case name, and ensures that it can be instantiated on a
   *  variable with an identical name (but a lowercase first letter).  Catches SF bug #689026 ("DynamicJava can't handle
   *  certain variable names")
   */
  public void testInteractionsVariableWithLowercaseClassName() throws BadLocationException, EditDocumentException,
    IOException, InterruptedException {
    // Compile a test file
    OpenDefinitionsDocument doc1 = setupDocument("public class DrJavaTestClass {}");
    File file1 = makeCanonical(new File(_tempDir, "DrJavaTestClass.java"));
    doCompile(doc1, file1);

    // This shouldn't cause an error (no output should be displayed)
    assertEquals("interactions result", "", interpret("drJavaTestClass = new DrJavaTestClass();"));
    _log.log("testInteractionsVariableWithLowercaseClassName() completed");
  }

  /** Checks that updating a class and recompiling it is visible from the REPL. */
  public void testInteractionsCanSeeChangedClass() throws BadLocationException, EditDocumentException,
    IOException, InterruptedException {
    final String text_before = "class DrJavaTestFoo { public int m() { return ";
    final String text_after = "; } }";
    final int num_iterations = 3;
    File file;
    OpenDefinitionsDocument doc;

    for (int i = 0; i < num_iterations; i++) {
      doc = setupDocument(text_before + i + text_after);
      file = tempFile(i);
      doCompile(doc, file);

      assertEquals("interactions result, i=" + i, String.valueOf(i), interpret("new DrJavaTestFoo().m()"));
    }
    _log.log("testInteractionsCanSeeChangedClass() completed");
  }

  /** Checks that an anonymous inner class can be defined in the repl! */
  public void testInteractionsDefineAnonymousInnerClass() throws BadLocationException, EditDocumentException,
    IOException, InterruptedException {
    final String interface_text = "public interface I { int getValue(); }";
    final File file = createFile("I.java");

    OpenDefinitionsDocument doc;

    doc = setupDocument(interface_text);
    doCompile(doc, file);

    for (int i = 0; i < 3; i++) {
      String s = "new I() { public int getValue() { return " + i + "; } }.getValue()";

      assertEquals("interactions result, i=" + i, String.valueOf(i), interpret(s));
    }
    _log.log("testInteractionsDefineAnonymousInnerClass() completed");
  }

  public void testGetSourceRootDefaultPackage() throws BadLocationException, IOException {

    // Get source root (current directory only)
    File[] roots = _model.getSourceRootSet();
    assertEquals("number of source roots", 0, roots.length);

     // Create temp directory
    File baseTempDir = tempDirectory();

    // Now make subdirectory a/b/c
    File subdir = makeCanonical(new File(baseTempDir, "a"));
    subdir = makeCanonical(new File(subdir, "b"));
    subdir = makeCanonical(new File(subdir, "c"));
    subdir.mkdirs();

    // Save the footext to DrJavaTestFoo.java in the subdirectory
    File fooFile = makeCanonical(new File(subdir, "DrJavaTestFoo.java"));
    OpenDefinitionsDocument doc = setupDocument(FOO_TEXT);
    doc.saveFileAs(new FileSelector(fooFile));

    // No events should fire
    _model.addListener(new TestListener());

    // Get source roots
    roots = _model.getSourceRootSet();
    assertEquals("number of source roots", 1, roots.length);
    // Get the source root for the new file in directory subdir
    assertEquals("source root", subdir, roots[0]);
    
    _log.log("testGetSourceRootDefaultPackage() completed");
  }

  public void testGetSourceRootPackageThreeDeepValid() throws BadLocationException, IOException {
    // Create temp directory
    File baseTempDir = tempDirectory();

    // Now make subdirectory a/b/c
    File subdir = makeCanonical(new File(baseTempDir, "a"));
    subdir = makeCanonical(new File(subdir, "b").getCanonicalFile());
    subdir = makeCanonical(new File(subdir, "c").getCanonicalFile());
    subdir.mkdirs();

    // Save the footext to DrJavaTestFoo.java in the subdirectory
    File fooFile = makeCanonical(new File(subdir, "DrJavaTestFoo.java"));
    OpenDefinitionsDocument doc = setupDocument("package a.b.c;\n" + FOO_TEXT);
    doc.saveFileAs(new FileSelector(fooFile));
//    System.err.println("Package name is: " + _model.getPackageName());

    // No events should fire
    _model.addListener(new TestListener());

    // Since we had the package statement the source root should be base dir
    File[] roots = _model.getSourceRootSet();
    assertEquals("number of source roots", 1, roots.length);
    assertEquals("source root", baseTempDir.getCanonicalFile(), roots[0].getCanonicalFile());
    
    _log.log("testGetSourceRootPackageThreeDeepValid() completed");
  }

  /** Tests that getSourceRoot works with a relative path when a package name is present. */
  public void testGetSourceRootPackageThreeDeepValidRelative() throws BadLocationException, IOException {
    // Create temp directory
    File baseTempDir = tempDirectory();
    File subdir = makeCanonical(new File(baseTempDir, "a"));
    subdir = makeCanonical(new File(subdir, "b"));
    subdir = makeCanonical(new File(subdir, "c"));
    subdir.mkdirs();

    // Save the footext to DrJavaTestFoo.java in a relative directory
    //   temp/./a/b/../b/c == temp/a/b/c
    File relDir = makeCanonical(new File(baseTempDir, "./a/b/../b/c"));
    File fooFile = makeCanonical(new File(relDir, "DrJavaTestFoo.java"));
    OpenDefinitionsDocument doc =
      setupDocument("package a.b.c;\n" + FOO_TEXT);
    doc.saveFileAs(new FileSelector(fooFile));

    // No events should fire
    _model.addListener(new TestListener());

    // Since we had the package statement the source root should be base dir
    File[] roots = _model.getSourceRootSet();
    assertEquals("number of source roots", 1, roots.length);
    assertEquals("source root", baseTempDir.getCanonicalFile(), roots[0].getCanonicalFile());
    
    _log.log("testGetSourceRootPackageThreeDeepValidRelative() completed");
  }

  public void testGetSourceRootPackageThreeDeepInvalid() throws BadLocationException, IOException {
    // Create temp directory
    File baseTempDir = tempDirectory();

    // Now make subdirectory a/b/d
    File subdir = makeCanonical(new File(baseTempDir, "a"));
    subdir = makeCanonical(new File(subdir, "b"));
    subdir = makeCanonical(new File(subdir, "d"));
    subdir.mkdirs();

    // Save the footext to DrJavaTestFoo.java in the subdirectory
    File fooFile = makeCanonical(new File(subdir, "DrJavaTestFoo.java"));
    OpenDefinitionsDocument doc =
      setupDocument("package a.b.c;\n" + FOO_TEXT);
    doc.saveFileAs(new FileSelector(fooFile));

    // No events should fire
    _model.addListener(new TestListener());

    // The package name is wrong so this should return only currDir
    File[] roots = _model.getSourceRootSet();
    assertEquals("number of source roots", 0, roots.length);
    
    _log.log("testGetSourceRootPackageThreeDeepInvalid() completed");
  }

  public void testGetSourceRootPackageOneDeepValid() throws BadLocationException, IOException {
    // Create temp directory
    File baseTempDir = tempDirectory();

    // Now make subdirectory a
    File subdir = makeCanonical(new File(baseTempDir, "a"));
    subdir.mkdir();

    // Save the footext to DrJavaTestFoo.java in the subdirectory
    File fooFile = makeCanonical(new File(subdir, "DrJavaTestFoo.java"));
    OpenDefinitionsDocument doc = setupDocument("package a;\n" + FOO_TEXT);
    doc.saveFileAs(new FileSelector(fooFile));

    // No events should fire
    _model.addListener(new TestListener());

    // Since we had the package statement the source root should be base dir
    File[] roots = _model.getSourceRootSet();
    assertEquals("number of source roots", 1, roots.length);
    assertEquals("source root", baseTempDir.getCanonicalFile(), roots[0].getCanonicalFile());
    
    _log.log("testGetSourceRootPackageOneDeepValid() completed");
  }


  public void testGetMultipleSourceRootsDefaultPackage() throws BadLocationException, IOException {
    // Create temp directory
    File baseTempDir = tempDirectory();

    // Now make subdirectories a, b
    File subdir1 = makeCanonical(new File(baseTempDir, "a"));
    subdir1.mkdir();
    File subdir2 = makeCanonical(new File(baseTempDir, "b"));
    subdir2.mkdir();

    // Save the footext to DrJavaTestFoo.java in subdirectory 1
    File file1 = makeCanonical(new File(subdir1, "DrJavaTestFoo.java"));
    OpenDefinitionsDocument doc1 = setupDocument(FOO_TEXT);
    doc1.saveFileAs(new FileSelector(file1));

    // Save the bartext to Bar.java in subdirectory 1
    File file2 = makeCanonical(new File(subdir1, "Bar.java"));
    OpenDefinitionsDocument doc2 = setupDocument(BAR_TEXT);
    doc2.saveFileAs(new FileSelector(file2));

    // Save the bartext to Bar.java in subdirectory 2
    File file3 = makeCanonical(new File(subdir2, "Bar.java"));
    OpenDefinitionsDocument doc3 = setupDocument(BAR_TEXT);
    doc3.saveFileAs(new FileSelector(file3));
    
    Utilities.clearEventQueue();

    // No events should fire
    _model.addListener(new TestListener());

    // Get source roots (should be 2: no duplicates)
    File[] roots = _model.getSourceRootSet();
    assertEquals("number of source roots", 2, roots.length);
    File root1 = roots[0];
    File root2 = roots[1];

    // Make sure both source roots are in set
    // But we don't care about the order
    if (!( (root1.equals(subdir1) && root2.equals(subdir2)) || (root1.equals(subdir2) && root2.equals(subdir1)) )) {
      fail("source roots did not match");
    }
    
    _log.log("testGetMultipleSourceRootsDefaultPackage() completed");
  }

  /** Creates a new class, compiles it and then checks that the REPL can see it. */
  public void testInteractionsLiveUpdateClassPath() throws BadLocationException, EditDocumentException,
    IOException, InterruptedException {

    OpenDefinitionsDocument doc = setupDocument(FOO_TEXT);
    Utilities.clearEventQueue();
        
    File f = tempFile();

    doCompile(doc, f);

    // Rename the directory so it's not on the classpath anymore
    String tempPath = f.getParent();
    File tempDir = makeCanonical(new File(tempPath));
    tempDir.renameTo(makeCanonical(new File(tempPath + "a")));

    String result = interpret("new DrJavaTestFoo().getClass().getName()");

    // Should cause a NoClassDefFound, but we shouldn't check exact syntax.
    //  Instead, make sure it isn't "DrJavaTestFoo", as if the class was found.
    assertFalse("interactions should have an error, not the correct answer", "\"DrJavaTestFoo\"".equals(result));
//    System.err.println("Result1 is: " + result);

    // Add new directory to classpath through Config
    Vector<File> cp = new Vector<File>();
    cp.add(makeCanonical(new File(tempPath + "a")));
    DrJava.getConfig().setSetting(EXTRA_CLASSPATH, cp);
    
    Utilities.clearEventQueue();
    _model.resetInteractionsClassPath();

    result = interpret("new DrJavaTestFoo().getClass().getName()");

    // Now it should be on the classpath
    assertEquals("interactions result", "\"DrJavaTestFoo\"", result);

    // Rename directory back to clean up
    tempDir = makeCanonical(new File(tempPath + "a"));
    boolean renamed = tempDir.renameTo(makeCanonical(new File(tempPath)));
    
    _log.log("testInteractionsLiveUpdateClasspath() completed");
  }

  /** Tests that the appropriate event is fired when the model's interpreter changes.*/
  public void testSwitchInterpreters() {
    TestListener listener = new TestListener() {
      public void interpreterChanged(boolean inProgress) {
        assertTrue("should not be in progress", !inProgress);
        interpreterChangedCount++;
      }
    };
    _model.addListener(listener);

    DefaultInteractionsModel dim = _model.getInteractionsModel();

    // Create a new Java interpreter, and set it to be active
    dim.addJavaInterpreter("testInterpreter");
    
    dim.setActiveInterpreter("testInterpreter", "myPrompt>");
    
    Utilities.clearEventQueue();
    listener.assertInterpreterChangedCount(1);
    _model.removeListener(listener);
    
    _log.log("testSwitchInterpreters() completed");
  }

  public void testRunMainMethod() throws Exception {
    File dir = makeCanonical(new File(_tempDir, "bar"));
    dir.mkdir();
    File file = makeCanonical(new File(dir, "Foo.java"));
    final OpenDefinitionsDocument doc = doCompile(FOO_CLASS, file);
    Utilities.invokeAndWait(new Runnable() { 
      public void run() { 
        try { doc.runMain(); }
        catch(Exception e) { throw new UnexpectedException(e); }
      }
    });
    
//    Utilities.clearEventQueue();
    assertInteractionsContains(InteractionsModel.BANNER_PREFIX);
    doc.insertString(doc.getLength(), " ", null);
    
    Utilities.invokeAndWait(new Runnable() { 
      public void run() { 
        try { doc.runMain(); }
        catch(Exception e) { throw new UnexpectedException(e); }
      }
    });
    
//    Utilities.clearEventQueue();
    assertInteractionsContains(DefaultGlobalModel.DOCUMENT_OUT_OF_SYNC_MSG);
    Utilities.clearEventQueue();  
    // Killing time here; Slave JVM may not have released Foo.class so that the file can be deleted on Windows.
    
    _log.log("testRunMainMethod() completed");
  }
}
