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
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model.junit;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import edu.rice.cs.util.classloader.ClassFileError;
import edu.rice.cs.drjava.model.GlobalModel;
import edu.rice.cs.drjava.model.OpenDefinitionsDocument;
import edu.rice.cs.drjava.model.repl.newjvm.MainJVM;
import edu.rice.cs.drjava.model.compiler.CompilerModel;
import edu.rice.cs.drjava.model.definitions.InvalidPackageException;
import edu.rice.cs.util.ExitingNotAllowedException;
import edu.rice.cs.util.ClassPathVector;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.text.SwingDocument;
import edu.rice.cs.util.swing.Utilities;

import org.apache.bcel.classfile.*;

/** Manages unit testing via JUnit.
 *  TODO: Remove dependence on GlobalModel
 *
 *  @version $Id$
 */
public class DefaultJUnitModel implements JUnitModel, JUnitModelCallback {
  
  /** Manages listeners to this model. */
  private final JUnitEventNotifier _notifier = new JUnitEventNotifier();
  
  /** RMI interface to a secondary JVM for running tests.
   *  Using a second JVM prevents tests from disrupting normal usage of DrJava.
   */
  private final MainJVM _jvm;
  
  /** Compiler model containing a lock used to prevent simultaneous test and compile. */
  private final CompilerModel _compilerModel;
  
  /** GlobalModel, used only for getSourceFile. */
  private final GlobalModel _model;
  
  /** The error model containing all current JUnit errors. */
  private JUnitErrorModel _junitErrorModel;
  
  /** State flag to prevent starting new tests on top of old ones */
  private boolean _testInProgress = false;
  
  /** State flag to record if test classes in projects must end in "Test" */
  private boolean _forceTestSuffix = false;
  
  /** lock to protect _testInProgress */
  final private Object _testLock = new Object();
  
  /** The document used to display JUnit test results.  Used only for testing. */
  private final SwingDocument _junitDoc = new SwingDocument();
  
  /** Main constructor.
   *  @param jvm RMI interface to a secondary JVM for running tests
   *  @param compilerModel the CompilerModel, used only as a lock to prevent simultaneous test and compile
   *  @param model used only for getSourceFile
   */
  public DefaultJUnitModel(MainJVM jvm, CompilerModel compilerModel, GlobalModel model) {
    _jvm = jvm;
    _compilerModel = compilerModel;
    _model = model;
    _junitErrorModel = new JUnitErrorModel(new JUnitError[0], _model, false);
  }
  
  //-------------------------- Field Setters --------------------------------//
  
  public void setForceTestSuffix(boolean b) { _forceTestSuffix = b; }
  
  //-------------------------- Listener Management --------------------------//
  
  /** Add a JUnitListener to the model.
   *  @param listener a listener that reacts to JUnit events
   */
  public void addListener(JUnitListener listener) { _notifier.addListener(listener); }
  
  /** Remove a JUnitListener from the model.  If the listener is not currently listening to this model, this method 
   *  has no effect.
   *  @param listener a listener that reacts to JUnit events
   */
  public void removeListener(JUnitListener listener) { _notifier.removeListener(listener); }
  
  /** Removes all JUnitListeners from this model. */
  public void removeAllListeners() { _notifier.removeAllListeners(); }
  
  //-------------------------------- Triggers --------------------------------//
  
  /** Used only for testing. */
  public SwingDocument getJUnitDocument() { return _junitDoc; }
  
  /** Creates a JUnit test suite over all currently open documents and runs it.  If the class file 
   *  associated with a file is not a test case, it is ignored.  
   */
  public void junitAll() { junitDocs(_model.getOpenDefinitionsDocuments()); }
  
  /** Creates a JUnit test suite over all currently open documents and runs it.  If a class file associated with a 
   *  source file is not a test case, it will be ignored.  Synchronized against the compiler model to prevent 
   *  testing and compiling at the same time, which would create invalid results.
   */
  public void junitProject() {
    LinkedList<OpenDefinitionsDocument> lod = new LinkedList<OpenDefinitionsDocument>();
    
    for (OpenDefinitionsDocument doc : _model.getOpenDefinitionsDocuments()) { 
      if (doc.isInProjectPath() || doc.isAuxiliaryFile())  lod.add(doc);
    }
    junitDocs(lod);
  }
  
  /** Forwards the classnames and files to the test manager to test all of them; does not notify 
   *  since we don't have ODD's to send out with the notification of junit start.
   *  @param qualifiedClassnames a list of all the qualified class names to test.
   *  @param files a list of their source files in the same order as qualified class names.
   */
  public void junitClasses(List<String> qualifiedClassnames, List<File> files) {
//    Utilities.showDebug("junitClasses(" + qualifiedClassnames + ", " + files);
    synchronized(_compilerModel.getSlaveJVMLock()) {
      synchronized(_testLock) {
        if (_testInProgress) return;
        _testInProgress = true;
      }
      List<String> testClasses;
      try { testClasses = _jvm.findTestClasses(qualifiedClassnames, files); }
      catch(IOException e) { throw new UnexpectedException(e); }
      
//      System.err.println("Found test classes: " + testClasses);
      
      if (testClasses.isEmpty()) {
        nonTestCase(true);
        return;
      }
      _notifier.junitClassesStarted(); 
      try { _jvm.runTestSuite(); } 
      catch(Throwable t) {
//        System.err.println("Threw exception " + t);
        _notifier.junitEnded();
        synchronized(_testLock) { _testInProgress = false;}
        throw new UnexpectedException(t); 
      }
    }
  }
  
  public void junitDocs(List<OpenDefinitionsDocument> lod) { junitOpenDefDocs(lod, true); }
  
  /** Runs JUnit on the current document as already compiled.  If the classes being tested are out of sync, 
   *  a message is displayed.
   */
  public void junit(OpenDefinitionsDocument doc) throws ClassNotFoundException, IOException {
//    new ScrollableDialog(null, "junit(" + doc + ") called in DefaultJunitModel", "", "").show();
    try {
      // try to get the file, to make sure it's not untitled. if it is, it'll throw an IllegalStateException
      File testFile = doc.getFile();
      LinkedList<OpenDefinitionsDocument> lod = new LinkedList<OpenDefinitionsDocument>();
      lod.add(doc);
      junitOpenDefDocs(lod, false);
    }
    catch (IllegalStateException e) {
      // No file exists, don't try to compile and test
      nonTestCase(false);
      return;
    }
    catch (NoClassDefFoundError e) {
      // Method getTest in junit.framework.BaseTestRunner can throw a NoClassDefFoundError (via reflection).
        _notifier.junitEnded();  // balances junitStarted()
        synchronized(_testLock) { _testInProgress = false; }
        throw e;
    }
    catch (ExitingNotAllowedException enae) {  // test attempted to call System.exit
      _notifier.junitEnded();  // balances junitStarted()
      synchronized(_testLock) { _testInProgress = false; }
      throw enae;
    }
  }
  
  /** Determines which class files in the built directories are test cases and correspond to open source
   *  files.  At this point (if no fatal errors are encountered) it informs listeners that junit has started
   *  (JUnitStarted) and performs the tests. */
  private void junitOpenDefDocs(List<OpenDefinitionsDocument> lod, boolean allTests) {
    // If a test is running, don't start another one.
    
//    System.err.println("junitOpenDefDocs(" + lod + "," + allTests + ")");
    
    // Set _testInProgress flag
    synchronized(_testLock) { 
      if (_testInProgress) return; 
      _testInProgress = true;
    }
      
    //reset the JUnitErrorModel, fixes bug #907211 "Test Failures Not Cleared Properly".
    _junitErrorModel = new JUnitErrorModel(new JUnitError[0], null, false);
    
    File buildDir = _model.getBuildDirectory();
//    System.err.println("Build directory is " + buildDir);
    
    /** Open java source files */
    HashSet<String> openDocFiles = new HashSet<String>();
    
    /** A map whose keys are directories containing class files corresponding to open java source files.
     *  Their values are the corresponding source roots. 
     */
    HashMap<File, File> classDirsAndRoots = new HashMap<File, File>();

    // Gets system classpaths from the main JVM so that junit tests can find every class file.
    // Given as one long String, this separates the paths into a list of strings. 3/12/05

//    LinkedList<String> classpaths = separateClasspath(getClasspath().toString());
    
    // new ScrollableDialog(null, "classpaths assembled in junitOpenDefDocs: " + classpaths, "", "").show();
    
    if (_model.hasOutOfSyncDocuments()) {
//      System.err.println("Calling compileBeforeJunit");
      _notifier.compileBeforeJUnit();
      if (_model.hasOutOfSyncDocuments()) {
        nonTestCase(true);
        return;
      }
      // We should probably display some form of dialog box saying that testing is being abandoned
    }

    // Initialize openDocFiles and classDirsAndRoots  
    
    for (OpenDefinitionsDocument doc: lod) /* for all nonEmpty documents in lod */ {
      if (! doc.isUntitled())
        try {
        String canonPathName = doc.getFile().getCanonicalPath();
        // Ensure that file name ends with ".java" or ".dji" extension to .java
        if (! (canonPathName.endsWith(".java") || canonPathName.endsWith(".dj0") || canonPathName.endsWith(".dj1") || 
               canonPathName.endsWith(".dj2"))) 
              continue;
//        System.err.println("Adding " + canonPathName + " to openDocFiles");
        
        String packageName = null;
        try { packageName = doc.getPackageName(); }
        catch(InvalidPackageException e) { 
//          System.out.println(doc + " has illegal package name");
          continue; 
        }
        
        // doc has valid package name; add it to list of open java source doc files
        openDocFiles.add(canonPathName);
        
        packageName = packageName.replace('.', File.separatorChar);
        
        // Add (canonical path name for) build directory for doc to classDirs
        
        File sourceRoot = doc.getSourceRoot();
        File buildRoot = (buildDir == null) ? sourceRoot: buildDir;
        
        File classFileDir = new File(buildRoot.getCanonicalPath() + File.separator + packageName);
        
        File sourceDir = (buildDir == null) ? classFileDir : new File(sourceRoot.getCanonicalPath() + File.separator + packageName);
        
        if (! classDirsAndRoots.containsKey(classFileDir)) {
          classDirsAndRoots.put(classFileDir, sourceDir);
//          System.err.println("Adding " + classFileDir + " with source root " + sourceRoot + " to list of class directories");
        }
      }
      
      catch(IOException e) { /* do nothing b/c the directory doesn't exist */ }
      catch(InvalidPackageException e) { /* do nothing, b/c it's package is bogus */ }
    }
    
//    System.err.println("classDirs = " + classDirsAndRoots.keySet());
    
    /** set of dirs potentially containing test classes */
    Set<File> classDirs = classDirsAndRoots.keySet();
    
//    System.err.println("openDocFiles = " + openDocFiles);
        
    /* Names of test classes. */
    ArrayList<String> classNames = new ArrayList<String>();
    
    /* Source files corresonding to potential test class files */
    ArrayList<File> files = new ArrayList<File>();
    
    /* Flag indicating if project is open */
    boolean isProject = _model.isProjectActive();

    try {
      for (File dir: classDirs) { // foreach class file directory
//        System.err.println("Examining directory " + dir);
        
        File[] listing = dir.listFiles();
        
//        System.err.println("Directory contains the files: " + Arrays.asList(listing));
        
        if (listing != null) {
          for (File entry : listing) { /* for each class file in the build directory */        
            

//            System.err.println("Examining file " + entry);
            
            /* ignore non-class files */
            String name = entry.getName();
            if (! name.endsWith(".class")) continue;
            
            /* In projects, ignore class names that do not end in "Test" if FORCE_TEST_SUFFIX option is set */
            if (_forceTestSuffix) {
              String noExtName = name.substring(0, name.length() - 6);  // remove ".class" from name
              int indexOfLastDot = noExtName.lastIndexOf('.');
              String simpleClassName = noExtName.substring(indexOfLastDot + 1);
//            System.err.println("Simple class name is " + simpleClassName);
              if (isProject && ! simpleClassName.endsWith("Test")) continue;
            }
            
//            System.err.println("Found test class: " + noExtName);
            
            /* ignore entries that do not correspond to files?  Can this happen? */
            if (! entry.isFile()) continue;
            
            try {
              JavaClass clazz = new ClassParser(entry.getCanonicalPath()).parse();
              String className = clazz.getClassName(); // get classfile name
//              System.err.println("looking for source file for: " + className);
              int indexOfDot = className.lastIndexOf('.');
              
              /** The absolute pathname for the file (including the file name) */
              File rootDir = classDirsAndRoots.get(dir);

              String javaSourceFileName = rootDir.getCanonicalPath() + File.separator + clazz.getSourceFileName();
//              System.err.println("Full java source fileName = " + javaSourceFileName);
              
              /* The index in fileName of the dot preceding the extension ".java", ".dj0*, ".dj1", or ".dj2" */
              int indexOfExtDot = javaSourceFileName.lastIndexOf('.');
//              System.err.println("indexOfExtDot = " + indexOfExtDot);
              if (indexOfExtDot == -1) continue;  // RMI stub class files return source file names without extensions
//              System.err.println("File found in openDocFiles = "  + openDocFiles.contains(sourceFileName));
              
              /* Determine if this java source file was generated from a language levels file. */
              String strippedName = javaSourceFileName.substring(0, indexOfExtDot);
//              System.err.println("Stripped name = " + strippedName);
              
              String sourceFileName;
              
              if (openDocFiles.contains(javaSourceFileName)) sourceFileName = javaSourceFileName;
              else if (openDocFiles.contains(strippedName + ".dj0")) sourceFileName = strippedName + ".dj0";
              else if (openDocFiles.contains(strippedName + ".dj1")) sourceFileName = strippedName + ".dj1";
              else if (openDocFiles.contains(strippedName + ".dj2")) sourceFileName = strippedName + ".dj2";
              else continue; // no matching source file is open
              
              classNames.add(className);
              files.add(new File(sourceFileName));
//              System.err.println("Class " + className + "added to classNames.   File " + sourceFileName + " added to files.");
            }
            catch(IOException e) { /* ignore it; can't read class file */ }
            catch(ClassFormatException e) { /* ignore it; class file is bad */ }
          }
        }
      }
    }
    catch(Throwable t) {
//      new ScrollableDialog(null, "UnexceptedExceptionThrown", t.toString(), "").show();
      throw new UnexpectedException(t); 
    }
    finally { 
//      new ScrollableDialog(null, "junit setup loop terminated", classNames.toString(), "").show();
    }
    
    // synchronized over _compilerModel to ensure that compilation and junit testing are mutually exclusive.
    // This is the WRONG way to ensure mutually exclusive compilation and testing because it invites deadlock.
    // Simply disable compile commands while testing.
   
    synchronized(_compilerModel.getSlaveJVMLock()) {
      /** Set up junit test suite on slave JVM; get TestCase classes forming that suite */
      List<String> tests;
      try { tests = _jvm.findTestClasses(classNames, files); }
      catch(IOException e) { throw new UnexpectedException(e); }
      
      if (tests == null || tests.isEmpty()) {
        nonTestCase(allTests);
        return;
      }
      
      try {
        /** Run the junit test suite that has already been set up on the slave JVM */
        _notifier.junitStarted(); // notify listeners that JUnit testing has finally started!
        //          new ScrollableDialog(null, "junitStarted executed in DefaultJunitModel", "", "").show();
        _jvm.runTestSuite();
        
      }
      catch(Throwable t) {
        // Probably a java.rmi.UnmarshalException caused by the interruption of unit testing.
        // Swallow the exception and proceed.
        _notifier.junitEnded();  // balances junitStarted()
        synchronized(_testLock) { _testInProgress = false;}
        throw new UnexpectedException(t);
      }
    }
  }
  
  //-------------------------------- Helpers --------------------------------//
  
  //----------------------------- Error Results -----------------------------//
  
  /** Gets the JUnitErrorModel, which contains error info for the last test run. */
  public JUnitErrorModel getJUnitErrorModel() { return _junitErrorModel; }
  
  /** Resets the junit error state to have no errors. */
  public void resetJUnitErrors() {
    _junitErrorModel = new JUnitErrorModel(new JUnitError[0], _model, false);
  }
  
  //---------------------------- Model Callbacks ----------------------------//
  
  /** Called from the JUnitTestManager if its given className is not a test case.
   *  @param isTestAll whether or not it was a use of the test all button
   */
  public void nonTestCase(final boolean isTestAll) {
    // NOTE: junitStarted is called in a different thread from the testing thread.  The _testInProgress flag
    //       is used to prevent a new test from being started and overrunning the existing one.
      _notifier.nonTestCase(isTestAll);
      synchronized(_testLock) { _testInProgress = false;}
  }
  
  /** Called to indicate that an illegal class file was encountered
   *  @param e the ClassFileObject describing the error.
   */
  public void classFileError(ClassFileError e) { _notifier.classFileError(e); }
  
  /** Called to indicate that a suite of tests has started running.
   *  @param numTests The number of tests in the suite to be run.
   */
  public void testSuiteStarted(final int numTests) { _notifier.junitSuiteStarted(numTests); }
  
  /** Called when a particular test is started.
   *  @param testName The name of the test being started.
   */
  public void testStarted(final String testName) { _notifier.junitTestStarted(testName); }
  
  /** Called when a particular test has ended.
   *  @param testName The name of the test that has ended.
   *  @param wasSuccessful Whether the test passed or not.
   *  @param causedError If not successful, whether the test caused an error
   *  or simply failed.
   */
  public void testEnded(final String testName, final boolean wasSuccessful, final boolean causedError) {
     _notifier.junitTestEnded(testName, wasSuccessful, causedError);
  }
  
  /** Called when a full suite of tests has finished running.
   *  @param errors The array of errors from all failed tests in the suite.
   */
  public void testSuiteEnded(JUnitError[] errors) {
//    new ScrollableDialog(null, "DefaultJUnitModel.testSuiteEnded(...) called", "", "").show();
    _junitErrorModel = new JUnitErrorModel(errors, _model, true);
    _notifier.junitEnded();
    synchronized(_testLock) { _testInProgress = false; }
//    new ScrollableDialog(null, "DefaultJUnitModel.testSuiteEnded(...) finished", "", "").show();
  }
  
  /**
   * Called when the JUnitTestManager wants to open a file that is not
   * currently open.
   * TODO: this is the only call to _model
   *       - remove it to remove GlobalModel dependence
   * @param className the name of the class for which we want to find the file
   * @return the file associated with the given class
   */
  public File getFileForClassName(String className) {
    return _model.getSourceFile(className + ".java");
  }
  
  /** Returns the current classpath in use by the JUnit JVM, in the form of a path-separator delimited string. */
  public ClassPathVector getClassPath() {  return _jvm.getClassPath(); }
  
  /** Called when the JVM used for unit tests has registered. */
  public void junitJVMReady() {
    synchronized(_testLock) { if (! _testInProgress) return; }
    JUnitError[] errors = new JUnitError[1];
    errors[0] = new JUnitError("Previous test suite was interrupted", true, "");
    _junitErrorModel = new JUnitErrorModel(errors, _model, true);
    _notifier.junitEnded();
    synchronized(_testLock) { _testInProgress = false; }
  }
}
