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

package edu.rice.cs.drjava.model;

import javax.swing.text.BadLocationException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.swing.SwingUtilities;

import edu.rice.cs.util.ClassPathVector;
import edu.rice.cs.util.FileOpenSelector;
import edu.rice.cs.drjava.model.FileSaveSelector;
import edu.rice.cs.util.FileOps;
import edu.rice.cs.util.OperationCanceledException;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.text.EditDocumentException;
import edu.rice.cs.util.swing.Utilities;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.config.OptionConstants;
import edu.rice.cs.drjava.config.OptionEvent;
import edu.rice.cs.drjava.config.OptionListener;

import edu.rice.cs.drjava.model.definitions.ClassNameNotFoundException;
import edu.rice.cs.drjava.model.definitions.DefinitionsDocument;
import edu.rice.cs.drjava.model.definitions.InvalidPackageException;
import edu.rice.cs.drjava.model.debug.Breakpoint;
import edu.rice.cs.drjava.model.debug.Debugger;
import edu.rice.cs.drjava.model.debug.DebugException;
import edu.rice.cs.drjava.model.debug.JPDADebugger;
import edu.rice.cs.drjava.model.debug.NoDebuggerAvailable;
import edu.rice.cs.drjava.model.debug.DebugListener;
import edu.rice.cs.drjava.model.debug.DebugWatchData;
import edu.rice.cs.drjava.model.debug.DebugThreadData;
import edu.rice.cs.drjava.model.repl.DefaultInteractionsModel;
import edu.rice.cs.drjava.model.repl.InteractionsDocument;
import edu.rice.cs.drjava.model.repl.InteractionsDJDocument;
import edu.rice.cs.drjava.model.repl.InteractionsListener;
import edu.rice.cs.drjava.model.repl.InteractionsScriptModel;
import edu.rice.cs.drjava.model.repl.newjvm.MainJVM;
import edu.rice.cs.drjava.model.compiler.CompilerListener;
import edu.rice.cs.drjava.model.compiler.CompilerModel;
import edu.rice.cs.drjava.model.compiler.DefaultCompilerModel;
import edu.rice.cs.drjava.model.junit.DefaultJUnitModel;
import edu.rice.cs.drjava.model.junit.JUnitModel;

import java.io.*;

/** Handles the bulk of DrJava's program logic. The UI components interface with the GlobalModel through its public
 *  methods, and teh GlobalModel responds via the GlobalModelListener interface. This removes the dependency on the 
 *  UI for the logical flow of the program's features.  With the current implementation, we can finally test the compile
 *  functionality of DrJava, along with many other things. <p>
 *  @version $Id$
 */
public class DefaultGlobalModel extends AbstractGlobalModel {
  
  /* FIELDS */
  
  /* Interpreter fields */
  
  /** The document  used in the Interactions model. */
  protected final InteractionsDJDocument _interactionsDocument;
  
  /** RMI interface to the Interactions JVM. */
  final MainJVM _jvm = new MainJVM(getWorkingDirectory());
  
  /** Interface between the InteractionsDocument and the JavaInterpreter, which runs in a separate JVM. */
  protected DefaultInteractionsModel _interactionsModel;
  
  /** Core listener attached to interactions model */
  protected InteractionsListener _interactionsListener = new InteractionsListener() {
    public void interactionStarted() { }
    
    public void interactionEnded() { }
    
    public void interactionErrorOccurred(int offset, int length) { }
    
    public void interpreterResetting() { }
    
    public void interpreterReady(File wd) {
      File buildDir = _state.getBuildDirectory();
      if (buildDir != null) {
        //        System.out.println("adding for reset: " + _state.getBuildDirectory().getAbsolutePath());
        try {
          _jvm.addBuildDirectoryClassPath(new File(buildDir.getAbsolutePath()).toURL());
        } catch(MalformedURLException murle) {
          // edit this later! this is bad! we should handle this exception better!
          throw new RuntimeException(murle);
        }
      }
    }
    
    public void interpreterResetFailed(Throwable t) { }
    
    public void interpreterExited(int status) { }
    
    public void interpreterChanged(boolean inProgress) { }
    
    public void interactionIncomplete() { }
    
    public void slaveJVMUsed() { }
  };
  
  private CompilerListener _clearInteractionsListener =
    new CompilerListener() {
    public void compileStarted() { }
    
    public void compileEnded(File workDir, File[] excludedFiles) {
      // Only clear interactions if there were no errors and unit testing is not in progress
      if ( ((_compilerModel.getNumErrors() == 0) || (_compilerModel.getCompilerErrorModel().hasOnlyWarnings()))
            && ! _junitModel.isTestInProgress() && _resetAfterCompile) {
        resetInteractions(workDir);  // use same working directory as current interpreter
      }
    }
    public void saveBeforeCompile() { }
    public void saveUntitled() { }
  };
    
  // ---- Compiler Fields ----
  
  /** CompilerModel manages all compiler functionality. */
  private final CompilerModel _compilerModel = new DefaultCompilerModel(this);
  
  /** Whether or not to reset the interactions JVM after compiling.  Should only be false in test cases. */
  private boolean _resetAfterCompile = true;
  
  /* JUnit Fields */
  
  /** JUnitModel manages all JUnit functionality. */
  private final DefaultJUnitModel _junitModel = new DefaultJUnitModel(_jvm, _compilerModel, this);
  
  /* Javadoc Fields */
  
  /** Manages all Javadoc functionality. */
  protected JavadocModel _javadocModel = new DefaultJavadocModel(this);
  
  /* Debugger Fields */
  
  /** Interface to the integrated debugger.  If unavailable, set NoDebuggerAvailable.ONLY. */
  private Debugger _debugger = NoDebuggerAvailable.ONLY;
  
  /* CONSTRUCTORS */
  
  /** Constructs a new GlobalModel. Creates a new MainJVM and starts its Interpreter JVM. */
  public DefaultGlobalModel() {
    super();
//    Utilities.show("DefaultGlobalModel super call performed");
    _interactionsDocument = new InteractionsDJDocument();

    _interactionsModel = new DefaultInteractionsModel(this, _jvm, _interactionsDocument, getWorkingDirectory());
    _interactionsModel.addListener(_interactionsListener);
    _jvm.setInteractionsModel(_interactionsModel);
    _jvm.setJUnitModel(_junitModel);
    
    _jvm.setOptionArgs(DrJava.getConfig().getSetting(JVM_ARGS));
    DrJava.getConfig().addOptionListener(JVM_ARGS, new OptionListener<String>() {
      public void optionChanged(OptionEvent<String> oe) {
        _jvm.setOptionArgs(oe.value);
      }
    }); 

    _createDebugger();
        
    // Chain notifiers so that all events also go to GlobalModelListeners.
    _interactionsModel.addListener(_notifier);
    _compilerModel.addListener(_notifier);
    _junitModel.addListener(_notifier);
    _javadocModel.addListener(_notifier);
    
//    Utilities.show("Notifier chaining done");
        
    // Listen to compiler to clear interactions appropriately.
    // XXX: The tests need this to be registered after _notifier, sadly.
    //      This is obnoxiously order-dependent, but it works for now.
    _compilerModel.addListener(_clearInteractionsListener);
    
    // Perhaps do this in another thread to allow startup to continue...
    _jvm.startInterpreterJVM();
    
// Any lightweight parsing has been disabled until we have something that is beneficial and works better in the background.    
//    _parsingControl = new DefaultLightWeightParsingControl(this);
  }
  

//  public void compileAll() throws IOException{ 
////    ScrollableDialog sd = new ScrollableDialog(null, "DefaultGlobalModel.compileAll() called", "", "");
////    sd.show();
//    _state.compileAll(); 
//  }
  

//  public void junitAll() { _state.junitAll(); }
  
  /** Sets the class with the project's main method. */
  public void setBuildDirectory(File f) {
    _state.setBuildDirectory(f);
    if (f != null) {
      //      System.out.println("adding: " + f.getAbsolutePath());
      try {
        _jvm.addBuildDirectoryClassPath(new File(f.getAbsolutePath()).toURL());
      }
      catch(MalformedURLException murle) {
        // TODO! change this! we should handle this exception better!
        // show a popup like "invalide build directory" or something
        throw new RuntimeException(murle);
      }
    }
    
    _notifier.projectBuildDirChanged();
    setProjectChanged(true);
  }
  
  protected FileGroupingState 
    makeProjectFileGroupingState(File pr, File main, File bd, File wd, File project, File[] files, ClassPathVector cp, File cjf, int cjflags) {
    return new ProjectFileGroupingState(pr, main, bd, wd, project, files, cp, cjf, cjflags);
  }
  
  class ProjectFileGroupingState extends AbstractGlobalModel.ProjectFileGroupingState {
      
    ProjectFileGroupingState(File pr, File main, File bd, File wd, File project, File[] files, ClassPathVector cp, File cjf, int cjflags) {
      super(pr, main, bd, wd, project, files, cp, cjf, cjflags);
    }

    // ----- FIND ALL DEFINED CLASSES IN FOLDER ---
//    public void junitAll() {
//      // Is this code reachable? I don't think so.  MainFrame bypasses it by calling junitProject() on the junit model
//      // instead of junitAll on the global model
//      File dir = getProjectRoot();
////        ArrayList<String> classNames = new ArrayList<String>();
//      final ArrayList<File> files = FileOps.getFilesInDir(dir, true, new FileFilter() {
//        public boolean accept(File pathname) {
//          return pathname.isDirectory() || 
//            pathname.getPath().toLowerCase().endsWith(".java") ||
//            pathname.getPath().toLowerCase().endsWith(".dj0") ||
//            pathname.getPath().toLowerCase().endsWith(".dj1") ||
//            pathname.getPath().toLowerCase().endsWith(".dj2");
//        }
//      });
//      ClassAndInterfaceFinder finder;
//      List<String> los = new LinkedList<String>();
//      List<File> lof = new LinkedList<File>();
//      for (File f: files) {
//        finder = new ClassAndInterfaceFinder(f);
//        String classname = finder.getClassName();
//        if (classname.length() > 0) {
//          los.add(classname);
//          lof.add(f);
//        }
//      }
//      List<OpenDefinitionsDocument> lod = getOpenDefinitionsDocuments();
//      for (OpenDefinitionsDocument d: lod) {
//        if (d.isAuxiliaryFile()) {
//          try {
//            File f;
//            String classname = d.getQualifiedClassName();
//            try {
//              f = d.getFile();
//              lof.add(f);
//              los.add(classname);
//            }
//            catch(FileMovedException fme) {
//              // the file's not on disk, but send it in anyways
//              f = fme.getFile();
//              if (f != null) {
//                lof.add(f);
//                los.add(classname);
//              }
//            }
//          }
//          catch(ClassNameNotFoundException e) {
//            // don't add it if we don't have a classname
//          }
//        }
//      }
//      getJUnitModel().junitClasses(los, lof);
//    }
    
    /** Jars all the files in this project */
    public void jarAll() { }
  }
  
  protected FileGroupingState makeFlatFileGroupingState() { return new FlatFileGroupingState(); }
  
  class FlatFileGroupingState extends AbstractGlobalModel.FlatFileGroupingState {
    
//    public void junitAll() { getJUnitModel().junitAll(); }
    public void jarAll() { }
  }
  
  /** Gives the title of the source bin for the navigator.
   *  @return The text used for the source bin in the tree navigator
   */
  public String getSourceBinTitle() { return "[ Source Files ]"; }
  
  /** Gives the title of the external files bin for the navigator
   *  @return The text used for the external files bin in the tree navigator.
   */
  public String getExternalBinTitle() { return "[ External Files ]"; }
  
  /** Gives the title of the aux files bin for the navigator.
   *  @return The text used for the aux files bin in the tree navigator.
   */
  public String getAuxiliaryBinTitle() { return "[ Included External Files ]"; }
  
  // ----- METHODS -----
  
//  /** Add a listener to this global model.
//   *  @param listener a listener that reacts on events generated by the GlobalModel.
//   */
//  public void addListener(GlobalModelListener listener) { _notifier.addListener(listener); }
//  
//  /** Remove a listener from this global model.
//   *  @param listener a listener that reacts on events generated by the GlobalModel
//   *  This method is synchronized using the readers/writers event protocol incorporated in EventNotifier<T>.
//   */
//  public void removeListener(GlobalModelListener listener) { _notifier.removeListener(listener); }
//  
//  // getter methods for the private fields
//  
//  public DefinitionsEditorKit getEditorKit() { return _editorKit; }
  
  /** @return the interactions model. */
  public DefaultInteractionsModel getInteractionsModel() { return _interactionsModel; }
  
  /** @return InteractionsDJDocument in use by the InteractionsDocument. */
  public InteractionsDJDocument getSwingInteractionsDocument() {
    return _interactionsDocument;
  }
  
  public InteractionsDocument getInteractionsDocument() { return _interactionsModel.getDocument(); }
  
  /** Gets the CompilerModel, which provides all methods relating to compilers. */
  public CompilerModel getCompilerModel() { return _compilerModel; }
  
  /** Gets the JUnitModel, which provides all methods relating to JUnit testing. */
  public JUnitModel getJUnitModel() { return _junitModel; }
  
  /** Gets the JavadocModel, which provides all methods relating to Javadoc. */
  public JavadocModel getJavadocModel() { return _javadocModel; }
  
  /** Prepares this model to be thrown away.  Never called in practice outside of quit(), except in tests. */
  public void dispose() {
    // Kill the interpreter
    _jvm.killInterpreter(null);
    
    super.dispose();
  }
  
  public void resetInteractions(File wd) { resetInteractions(wd, false); }
 
  /** Clears and resets the slave JVM with working directory wd. Also clears the console if the option is 
   *  indicated (on by default).  The reset operation is suppressed if the existing slave JVM has not been
   *  used, {@code wd} matches its working directory, and forceResest is false.
   */
  public void resetInteractions(File wd, boolean forceReset) {
    if (! _jvm.slaveJVMUsed() && wd.equals(_interactionsModel.getWorkingDirectory())) {
      // eliminate resetting interpreter (slaveJVM) since it has already been reset appropriately.
//      Utilities.show("Suppressing resetting of interactions pane");
      _interactionsModel._notifyInterpreterReady(wd);
      return; 
    }
//    Utilities.show("Resetting interactions with working directory = " + wd);
    _interactionsModel.resetInterpreter(wd);
  }

  /** Interprets the current given text at the prompt in the interactions pane. */
  public void interpretCurrentInteraction() { _interactionsModel.interpretCurrentInteraction(); }

  /** Interprets file selected in the FileOpenSelector. Assumes strings have no trailing whitespace. Interpretation is
   *  aborted after the first error.
   */
  public void loadHistory(FileOpenSelector selector) throws IOException { _interactionsModel.loadHistory(selector); }

  /** Loads the history/histories from the given selector. */
  public InteractionsScriptModel loadHistoryAsScript(FileOpenSelector selector)
    throws IOException, OperationCanceledException {
    return _interactionsModel.loadHistoryAsScript(selector);
  }

  /** Clears the interactions history */
  public void clearHistory() { _interactionsModel.getDocument().clearHistory(); }

  /** Saves the unedited version of the current history to a file
   *  @param selector File to save to
   */
  public void saveHistory(FileSaveSelector selector) throws IOException {
    _interactionsModel.getDocument().saveHistory(selector);
  }

  /** Saves the edited version of the current history to a file
   *  @param selector File to save to
   *  @param editedVersion Edited verison of the history which will be saved to file instead of the lines saved in 
   *         the history. The saved file will still include any tags needed to recognize it as a history file.
   */
  public void saveHistory(FileSaveSelector selector, String editedVersion) throws IOException {
    _interactionsModel.getDocument().saveHistory(selector, editedVersion);
  }

  /** Returns the entire history as a String with semicolons as needed. */
  public String getHistoryAsStringWithSemicolons() {
    return _interactionsModel.getDocument().getHistoryAsStringWithSemicolons();
  }

  /** Returns the entire history as a String. */
  public String getHistoryAsString() {
    return _interactionsModel.getDocument().getHistoryAsString();
  }

  /** Called when the debugger wants to print a message.  Inserts a newline. */
  public void printDebugMessage(String s) {
    _interactionsModel.getDocument().
      insertBeforeLastPrompt(s + "\n", InteractionsDocument.DEBUGGER_STYLE);
  }

  /** Blocks until the interpreter has registered. */
  public void waitForInterpreter() { _jvm.ensureInterpreterConnected(); }


  /** Returns the current classpath in use by the Interpreter JVM. */
  public ClassPathVector getClassPath() { return _jvm.getClassPath(); }
  
  /** Sets whether or not the Interactions JVM will be reset after a compilation succeeds.  This should ONLY be used 
   *  in tests!  This method is not supported by AbstractGlobalModel.
   *  @param shouldReset Whether to reset after compiling
   */
  void setResetAfterCompile(boolean shouldReset) { _resetAfterCompile = shouldReset; }

  /** Gets the Debugger used by DrJava. */
  public Debugger getDebugger() { return _debugger; }

  /** Returns an available port number to use for debugging the interactions JVM.
   *  @throws IOException if unable to get a valid port number.
   */
  public int getDebugPort() throws IOException { return _interactionsModel.getDebugPort(); }

  // ---------- ConcreteOpenDefDoc inner class ----------

  /** Inner class to handle operations on each of the open DefinitionsDocuments by the GlobalModel. <br><br>
   *  This was at one time called the <code>DefinitionsDocumentHandler</code>
   *  but was renamed (2004-Jun-8) to be more descriptive/intuitive.
   */
  class ConcreteOpenDefDoc extends AbstractGlobalModel.ConcreteOpenDefDoc {
   
    /** Standard constructor for a document read from a file.  Initializes this ODD's DD.
     *  @param f file describing DefinitionsDocument to manage
     */
    ConcreteOpenDefDoc(File f) throws IOException { super(f); }
    
    /* Standard constructor for a new document (no associated file) */
    ConcreteOpenDefDoc() { super(); }
    
    /** Starting compiling this document.  Used only for unit testing */
    public void startCompile() throws IOException { _compilerModel.compile(ConcreteOpenDefDoc.this); }
    
    private InteractionsListener _runMain;

    /** Runs the main method in this document in the interactions pane after resetting interactions with the source
     *  root for this document as the working directory.  Warns the use if the class files for the doucment are not 
     *  up to date.  Fires an event to signal when execution is about to begin.
     *  NOTE: this code normally runs in the event thread; it cannot block waiting for an event that is triggered by
     *  event thread execution!
     *  @exception ClassNameNotFoundException propagated from getFirstTopLevelClass()
     *  @exception IOException propagated from GlobalModel.compileAll()
     */
    public void runMain() throws ClassNameNotFoundException, IOException {
      
      // Get the class name for this document, the first top level class in the document.
      final String className = getDocument().getQualifiedClassName();
      final InteractionsDocument iDoc = _interactionsModel.getDocument();
      if (! checkIfClassFileInSync()) {
        iDoc.insertBeforeLastPrompt(DOCUMENT_OUT_OF_SYNC_MSG, InteractionsDocument.ERROR_STYLE);
        return;
      }
      
      final boolean wasDebuggerEnabled = getDebugger().isReady();
      
      _runMain = new DummyGlobalModelListener() {
        public void interpreterReady(File wd) {
          // Restart debugger if it was previously enabled and is now off
          if (wasDebuggerEnabled && (!getDebugger().isReady())) {
            try { getDebugger().startup(); } catch(DebugException de) { /* ignore, continue without debugger */ }
          }
          
          // Load the proper text into the interactions document
          iDoc.clearCurrentInput();
          iDoc.append("java " + className, null);
          
          // Finally, execute the new interaction and record that event
          _interactionsModel.interpretCurrentInteraction();
          _notifier.runStarted(ConcreteOpenDefDoc.this);
          SwingUtilities.invokeLater(new Runnable() {
            public void run() { 
              /* Remove _runMain listener AFTER this interpreterReady listener completes and DROPS it readLock on
               * _interactionsModel._notifier. */
              _interactionsModel.removeListener(_runMain);
            }
          });
          
        }
      };
      
      _interactionsModel.addListener(_runMain);
      
      // Reset interactions to the soure root for this document; class will be executed when new interpreter is ready
      resetInteractions(getSourceRoot());  
    }

    /** Runs JUnit on the current document.  Requires that all source documents are compiled before proceeding. */
    public void startJUnit() throws ClassNotFoundException, IOException { _junitModel.junit(this); }

    /** Generates Javadoc for this document, saving the output to a temporary
     *  directory.  The location is provided to the javadocEnded event on
     *  the given listener.
     *  @param saver FileSaveSelector for saving the file if it needs to be saved
     */
    public void generateJavadoc(FileSaveSelector saver) throws IOException {
      // Use the model's classpath, and use the EventNotifier as the listener
      _javadocModel.javadocDocument(this, saver, getClassPath().toString());
    }
    
    /** Returns the first Breakpoint in this OpenDefinitionsDocument whose region includes the given offset, or null
     *  if one does not exist.
     *  @param offset an offset at which to search for a breakpoint
     *  @return the Breakpoint at the given lineNumber, or null if it does not exist.
     */
    public Breakpoint getBreakpointAt(int offset) {
      //return _breakpoints.get(new Integer(lineNumber));

      for (int i = 0; i < _breakpoints.size(); i++) {
        Breakpoint bp = _breakpoints.get(i);
        if (offset >= bp.getStartOffset() && offset <= bp.getEndOffset()) return bp;
      }
      return null;
    }

    /** Inserts the given Breakpoint into the list, sorted by region
     *  @param breakpoint the Breakpoint to be inserted
     */
    public void addBreakpoint(Breakpoint breakpoint) {
      //_breakpoints.put( new Integer(breakpoint.getLineNumber()), breakpoint);

      for (int i=0; i< _breakpoints.size();i++) {
        Breakpoint bp = _breakpoints.get(i);
        int oldStart = bp.getStartOffset();
        int newStart = breakpoint.getStartOffset();
        
        if ( newStart < oldStart) {
          // Starts before, add here
          _breakpoints.add(i, breakpoint);
          return;
        }
        if ( newStart == oldStart) {
          // Starts at the same place
          int oldEnd = bp.getEndOffset();
          int newEnd = breakpoint.getEndOffset();
          
          if ( newEnd < oldEnd) {
            // Ends before, add here
            _breakpoints.add(i, breakpoint);
            return;
          }
        }
      }
      _breakpoints.add(breakpoint);
    }
    
    /** Remove the given Breakpoint from our list (but not the debug manager)
     *  @param breakpoint the Breakpoint to be removed.
     */
    public void removeBreakpoint(Breakpoint breakpoint) { _breakpoints.remove(breakpoint); }
    
    /** Returns a Vector<Breakpoint> that contains all of the Breakpoint objects in this document. */
    public Vector<Breakpoint> getBreakpoints() { return _breakpoints; }
    
    /** Tells the document to remove all breakpoints (without removing themfrom the debug manager). */
    public void clearBreakpoints() { _breakpoints.clear(); }
    
    /** Called to indicate the document is being closed, so to remove all related state from the debug manager. */
    public void removeFromDebugger() {
      if (_debugger.isAvailable() && (_debugger.isReady())) {
        try {
          while (_breakpoints.size() > 0) {
            _debugger.removeBreakpoint(_breakpoints.get(0));
          }
        }
        catch (DebugException de) {
          // Shouldn't happen if debugger is active
          throw new UnexpectedException(de);
        }
      }
      else clearBreakpoints();
    }
  } /* End of ConcreteOpenDefDoc */
  
  /** Creates a ConcreteOpenDefDoc for a new DefinitionsDocument.
   *  @return OpenDefinitionsDocument object for a new document
   */
  protected ConcreteOpenDefDoc _createOpenDefinitionsDocument() { return new ConcreteOpenDefDoc(); }
  
   /** Creates a ConcreteOpenDefDoc for a given file f
   *  @return OpenDefinitionsDocument object for f
   */
  protected ConcreteOpenDefDoc _createOpenDefinitionsDocument(File f) throws IOException { return new ConcreteOpenDefDoc(f); }
  
  /** Adds the source root for doc to the interactions classpath; this function is a helper to _openFiles.
   *  @param doc the document to add to the classpath
   */
  protected void addDocToClassPath(OpenDefinitionsDocument doc) {
    try {
      File classPath = doc.getSourceRoot();
      try {
        if (doc.isAuxiliaryFile())
          _interactionsModel.addProjectFilesClassPath(classPath.toURL());
        else _interactionsModel.addExternalFilesClassPath(classPath.toURL());
      }
      catch(MalformedURLException murle) {  /* fail silently */ }
    }
    catch (InvalidPackageException e) {
      // Invalid package-- don't add it to classpath
    }
  }
   
  /** Instantiates the integrated debugger if the "debugger.enabled" config option is set to true.  Leaves it 
   *  at null if not.
   */
  private void _createDebugger() {
    try {
      _debugger = new JPDADebugger(this);
      _jvm.setDebugModel((JPDADebugger) _debugger);
      
      // add listener to set the project file to "changed" when a breakpoint or watch is added, removed, or changed
      _debugger.addListener(new DebugListener() {
        public void debuggerStarted() { }
        public void debuggerShutdown() { }
        public void threadLocationUpdated(OpenDefinitionsDocument doc, int lineNumber, boolean shouldHighlight) { }
        public void breakpointSet(final Breakpoint bp) {
          setProjectChanged(true);
        }
        public void breakpointReached(final Breakpoint bp) { }
        public void breakpointChanged(final Breakpoint bp) {
          setProjectChanged(true);
        }    
        public void breakpointRemoved(final Breakpoint bp) {
          setProjectChanged(true);
        }    
        public void watchSet(final DebugWatchData w) {
          setProjectChanged(true);
        }
        public void watchRemoved(final DebugWatchData w) {
          setProjectChanged(true);
        }    
        public void stepRequested() { }
        public void currThreadSuspended() { }
        public void currThreadResumed() { }
        public void threadStarted() { }
        public void currThreadDied() { }
        public void nonCurrThreadDied() {  }
        public void currThreadSet(DebugThreadData thread) { }
      });
    }
    catch( NoClassDefFoundError ncdfe ) {
      // JPDA not available, so we won't use it.
      _debugger = NoDebuggerAvailable.ONLY;
    }
    catch( UnsupportedClassVersionError ucve ) {
      // Wrong version of JPDA, so we won't use it.
      _debugger = NoDebuggerAvailable.ONLY;
    }
    catch( Throwable t ) {
      // Something went wrong in initialization, don't use debugger
      _debugger = NoDebuggerAvailable.ONLY;
    }
  }
  
  /** Adds the source roots for all open documents and the paths on the "extra classpath" config option, as well
   *  as any project-specific classpaths to the interpreter's classpath. This method is called when the interpreter 
   *  becomes ready
   */
  public void resetInteractionsClassPath() {
    ClassPathVector projectExtras = getExtraClassPath();
    //System.out.println("Adding project classpath vector to interactions classpath: " + projectExtras);
    if (projectExtras != null)  for (URL cpE : projectExtras) { _interactionsModel.addProjectClassPath(cpE); }
    
    Vector<File> cp = DrJava.getConfig().getSetting(EXTRA_CLASSPATH);
    if (cp != null) {
      for (File f : cp) {
        try { _interactionsModel.addExtraClassPath(f.toURL()); }
        catch(MalformedURLException murle) {
          System.out.println("File " + f + " in your extra classpath could not be parsed to a URL; " +
                             "it may contain un-URL-encodable characters.");
        }
      }
    }
    
    List<OpenDefinitionsDocument> odds = getAuxiliaryDocuments();
    for (OpenDefinitionsDocument odd: odds) {
      // this forwards directly to InterpreterJVM.addClassPath(String)
      try { _interactionsModel.addProjectFilesClassPath(odd.getSourceRoot().toURL()); }
      catch(MalformedURLException murle) { /* fail silently */ }
      catch(InvalidPackageException e) {  /* ignore it */ }
    }
    
    odds = getNonProjectDocuments();
//    Utilities.show(odds.toString());
    for (OpenDefinitionsDocument odd: odds) {
      // this forwards directly to InterpreterJVM.addClassPath(String)
      try { 
        File sourceRoot = odd.getSourceRoot();
        if (sourceRoot != null) _interactionsModel.addExternalFilesClassPath(sourceRoot.toURL()); 
      }
      catch(MalformedURLException murle) { /* ignore it */ }
      catch(InvalidPackageException e) { /* ignore it */ }
    }
    
    // add project source root to projectFilesClassPath.  All files in project tree have this root.
    
    try { _interactionsModel.addProjectFilesClassPath(getProjectRoot().toURL()); }
    catch(MalformedURLException murle) { /* fail silently */ } 
  }
  
//  private class ExtraClasspathOptionListener implements OptionListener<Vector<File>> {
//    public void optionChanged (OptionEvent<Vector<File>> oce) {
//      Vector<File> cp = oce.value;
//      if (cp != null) {
//        for (File f: cp) {
//          // this forwards directly to InterpreterJVM.addClassPath(String)
//          try { _interactionsModel.addExtraClassPath(f.toURL()); }
//          catch(MalformedURLException murle) { 
//            /* do nothing; findbugs signals a bug unless this catch clause spans more than two lines */ 
//          }
//        }
//      }
//    }
//  }
  
}
