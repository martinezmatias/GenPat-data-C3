/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is part of DrJava.  Download the current version of this project:
 * http://sourceforge.net/projects/drjava/ or http://www.drjava.org/
 *
 * DrJava Open Source License
 *
 * Copyright (C) 2001-2003 JavaPLT group at Rice University (javaplt@rice.edu)
 * All rights reserved.
 *
 * Developed by:   Java Programming Languages Team
 *                 Rice University
 *                 http://www.cs.rice.edu/~javaplt/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal with the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimers.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimers in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the names of DrJava, the JavaPLT, Rice University, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this Software without specific prior written permission.
 *     - Products derived from this software may not be called "DrJava" nor
 *       use the term "DrJava" as part of their names without prior written
 *       permission from the JavaPLT group.  For permission, write to
 *       javaplt@rice.edu.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS WITH THE SOFTWARE.
 *
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model;

import javax.swing.text.*;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.io.*;
import java.util.*;
import java.awt.Container;


import edu.rice.cs.util.swing.FindReplaceMachine;
import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.Version;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.drjava.model.definitions.*;
import edu.rice.cs.drjava.model.repl.*;
import edu.rice.cs.drjava.model.compiler.*;
import edu.rice.cs.util.docnavigation.*;

/**
 * A GlobalModel that enforces invariants associated with having
 * one active document at a time.
 *
 * Invariants:
 * <OL>
 * <LI>{@link #getDefinitionsDocuments} will always return an array of
 *     at least size 1.
 * </LI>
 * <LI>(follows from previous) If there is ever no document in the model,
 *     a new one will be created.
 * </LI>
 * <LI>There is always exactly one active document, which can be get/set
 *     via {@link #getActiveDocument} and {@link #setActiveDocument}.
 * </LI>
 * </OL>
 *
 * Other functions added by this class:
 * <OL>
 * <LI>When calling {@link #openFile}, if there is currently only one open
 *     document, and it is untitled and unchanged, it will be closed after the
 *     new document is opened. This means that, in one atomic transaction, the
 *     model goes from having one totally empty document open to having one
 *     document (the requested one) open.
 * </LI>
 * </OL>
 *
 * @version $Id$
 */
public class DefaultSingleDisplayModel extends DefaultGlobalModel
    implements SingleDisplayModel{

  /**
   * The active document pointer, which will never be null once
   * the constructor is done.
   */
  private OpenDefinitionsDocument _activeDocument;

  /**
   * Creates a SingleDisplayModel.
   *
   * <ol>
   *   <li>A new document is created to satisfy the invariant.
   *   <li>The first document in the list is set as the active document.
   * </ol>
   */
  public DefaultSingleDisplayModel() {
    super();
    _init();
  }

  /**
   * Initiates this SingleDisplayModel.  Should only be called
   * from the constructor.
   */
  private void _init() {
    
    final NodeDataVisitor<Boolean> _gainVisitor = new NodeDataVisitor<Boolean>() {
      public Boolean itemCase(INavigatorItem docu){
        _setActiveDoc(docu);
        return true; 
      }
      public Boolean fileCase(File f){
        if (!f.isAbsolute()) {
          File root = _state.getProjectFile().getParentFile().getAbsoluteFile();
          f = new File(root, f.getPath());
        }
        _notifier.currentDirectoryChanged(f);
        return true;
      }
      public Boolean stringCase(String s){ 
        return false; 
      }
    };
    
    _documentNavigator.addNavigationListener(new INavigationListener() {
      public void gainedSelection(NodeData dat) {
        dat.execute(_gainVisitor);
      }
      public void lostSelection(NodeData dat) {
        // not important, only one document selected at a time
      }
    });
    
    _isClosingAllDocs = false;
    _ensureNotEmpty();
    setActiveFirstDocument();
  }

  /**
   * Add a listener to this global model.
   * TODO: is this ever actually used?
   * @param listener a listener that reacts on events generated by the GlobalModel
   */
  public void addListener(GlobalModelListener listener) {
    if (! (listener instanceof SingleDisplayModelListener)) {
      throw new IllegalArgumentException("Must use SingleDisplayModelListener");
    }

    super.addListener(listener);
  }

  //----------------------- New SingleDisplay Methods -----------------------//

  /**
   * Returns the currently active document.
   */
  public OpenDefinitionsDocument getActiveDocument() {
    return _activeDocument;
  }
  
  /**
   * Sets the currently active document by updating the selection model.
   * @param doc Document to set as active
   */
  public void setActiveDocument(OpenDefinitionsDocument doc) {
    try {
      _documentNavigator.setActiveDoc(getIDocGivenODD(doc));
      //    _setActiveDoc(getIDocGivenODD(doc));
    } catch(DocumentClosedException dce) {
      //noop
    }
  }
  
  public Container getDocCollectionWidget() {
    return _documentNavigator.asContainer();
  }
  
  /**
   * Sets the active document to be the next one in the collection
   */
  public void setActiveNextDocument() {
    INavigatorItem key = getIDocGivenODD(_activeDocument);
    INavigatorItem nextKey =_documentNavigator.getNext(key);
      if( key != nextKey ) {
        /* this will select the active document in the navigator, which
         * will signal a listener to call _setActiveDoc(...)
         */
          _documentNavigator.setActiveDoc(nextKey);
//   _setActiveDoc(nextKey);
      }
  }

  /**
   * Sets the active document to be the previous one in the collection
   */
  public void setActivePreviousDocument() {
    INavigatorItem key = getIDocGivenODD(_activeDocument);
    INavigatorItem prevKey =_documentNavigator.getPrevious(key);
    if( key != prevKey ) {
      /* this will select the active document in the navigator, which
       * will signal a listener to call _setActiveDoc(...)
       */
      _documentNavigator.setActiveDoc(prevKey);
      //   _setActiveDoc(prevKey);
    }
  }

  /**
   * Returns whether we are in the process of closing all documents.
   * (Don't want to prompt the user to revert files that have become
   * modified on disk if we're just closing everything.)
   * TODO: Move to DGM?  Make private?
   */
  public boolean isClosingAllFiles() {
    return _isClosingAllDocs;
  }

  //----------------------- End SingleDisplay Methods -----------------------//

  /**
   * Creates a new document, adds it to the list of open documents,
   * and sets it to be active.
   * @return The new open document
   */
  public OpenDefinitionsDocument newFile() {
    OpenDefinitionsDocument doc = super.newFile();
    setActiveDocument(doc);
    return doc;
  }

  //---------------------- Specified by ILoadDocuments ----------------------//

  /**
   * Open a file and read it into the definitions.
   * The provided file selector chooses a file, and on a successful
   * open, the fileOpened() event is fired.
   * This also checks if there was previously a single, unchanged,
   * untitled document open, and if so, closes it after a successful
   * opening.
   * @param com a command pattern command that selects what file
   *            to open
   * @return The open document, or null if unsuccessful
   * @exception IOException
   * @exception OperationCanceledException if the open was canceled
   * @exception AlreadyOpenException if the file is already open
   */
  public OpenDefinitionsDocument openFile(FileOpenSelector com)
    throws IOException, OperationCanceledException, AlreadyOpenException
  {
    // Close an untitled, unchanged document if it is the only one open
    boolean closeUntitled = _hasOneEmptyDocument();
    OpenDefinitionsDocument oldDoc = _activeDocument;

    OpenDefinitionsDocument openedDoc = super.openFile(com);
    if (closeUntitled) {
      super.closeFile(oldDoc);
    }

    setActiveDocument(openedDoc);
    return openedDoc;
  }

 /**
   * Open multiple files and read it into the definitions.
   * The provided file selector chooses a file, and on a successful
   * open, the fileOpened() event is fired.
   * This also checks if there was previously a single, unchanged,
   * untitled document open, and if so, closes it after a successful
   * opening.
   * @param com a command pattern command that selects what file
   *            to open
   * @return The open document, or null if unsuccessful
   * @exception IOException
   * @exception OperationCanceledException if the open was canceled
   * @exception AlreadyOpenException if the file is already open
   */
  public OpenDefinitionsDocument openFiles(FileOpenSelector com)
    throws IOException, OperationCanceledException, AlreadyOpenException
  {
    // Close an untitled, unchanged document if it is the only one open
    boolean closeUntitled = _hasOneEmptyDocument();
    OpenDefinitionsDocument oldDoc = _activeDocument;

    OpenDefinitionsDocument openedDoc = super.openFiles(com);
    if (closeUntitled) {
      super.closeFile(oldDoc);
    }
    setActiveDocument(openedDoc);
    return openedDoc;
  }

  //----------------------- End ILoadDocuments Methods -----------------------//

  /**
   * Saves all open files, prompting for names if necessary.
   * When prompting (ie, untitled document), set that document as active.
   * @param com a FileSaveSelector
   * @exception IOException
   */
   public void saveAllFiles(FileSaveSelector com) throws IOException {
     OpenDefinitionsDocument curdoc = getActiveDocument();
     super.saveAllFiles(com);
     setActiveDocument(curdoc); // Return focus to previously active doc
   }

  /**
   * If the document is untitled, brings it to the top so that the
   * user will know which file she is saving
   */
   public void aboutToSaveFromSaveAll(OpenDefinitionsDocument doc) {
     if (doc.isUntitled()) {
       setActiveDocument(doc);
     }
   }

  /**
   * Closes an open definitions document, prompting to save if
   * the document has been changed.  Returns whether the file
   * was successfully closed.
   * Also ensures the invariant that there is always at least
   * one open document holds by creating a new file if necessary.
   * @return true if the document was closed
   */
   public boolean closeFile(OpenDefinitionsDocument doc) {
//     INavigatorItem switchTo =_documentNavigator.getNext(getIDocGivenODD(doc));
//     /** if we can't move forward, go backwards */
//     if( switchTo == getIDocGivenODD(doc)) {
//       switchTo = _documentNavigator.getPrevious(switchTo);
//     }
     
     if( super.closeFile(doc) ) {
       // Select next document if not closing all documents
       if (!_isClosingAllDocs) {
         _ensureNotEmpty();
         
         // if( _hasOneEmptyDocument() ) {
         if(getDocumentCount() == 1){
           setActiveFirstDocument();
         }
//         else {
//           /* this will select the active document in the navigator, which
//            * will signal a listener to call _setActiveDoc(...)
//            */
//           _documentNavigator.setActiveDoc(switchTo);
//           //_setActiveDoc(switchTo);
//         }
       }
       return true;
     }
     return false;
   }
   
   /**
    * Attempts to close all open documents.
    * Also ensures the invariant that there is always at least
    * one open document holds by creating a new file if necessary.
    //Bug when the first document, in list view, is selected:
    //When "close all" documents is selected, each document in turn is set active
    //Fix: close the currently active document last
    * @return true if all documents were closed
    */
  public boolean closeAllFiles() {
    _isClosingAllDocs = true;
    
    List<OpenDefinitionsDocument> docs = getDefinitionsDocuments();
    OpenDefinitionsDocument active = getActiveDocument();
    boolean keepClosing = true;
    for(OpenDefinitionsDocument d: docs){
      if(d != active && keepClosing){
        keepClosing = closeFile(d);
      }
    }
    if(keepClosing){
      keepClosing = closeFile(active);
    }
    _isClosingAllDocs = false;
    _ensureNotEmpty();
    setActiveFirstDocument();
    return keepClosing;
  }
    
  
  
  /**
   * Shared code between close project and close All files which only sets 
   * the new active document after all documents to be closed have been closed
   */
  public boolean closeFiles(List<OpenDefinitionsDocument> docList) {
    OpenDefinitionsDocument last = null;
    Iterator<OpenDefinitionsDocument> it = docList.iterator();
    while(it.hasNext()) {
      last = it.next();
      if(it.hasNext())
        closeFile(last);      
    }
    if(last != null) {
      IDocumentNavigator nav = getDocumentNavigator();
      INavigatorItem switchTo = nav.getNext(getIDocGivenODD(last));
      /** if we can't move forward, go backwards */
      if( switchTo == getIDocGivenODD(last)) {
        switchTo = nav.getPrevious(switchTo);
      }
      
      //close the last file
      if (closeFile(last))
      {        
        if(getDocumentCount() == 1){
          setActiveFirstDocument();
        }
        else {
          /* this will select the active document in the navigator, which
           * will signal a listener to call _setActiveDoc(...)
           */
          nav.setActiveDoc(switchTo);
        }
      }
      else
      {
        nav.setActiveDoc(getIDocGivenODD(last)); 
      }
    }
    return true;
  }

  /**
   * Returns whether there is currently only one open document
   * which is untitled and unchanged.
   */
  private boolean _hasOneEmptyDocument() {
    return ((getDefinitionsDocumentsSize() == 1) &&
            (_activeDocument.isUntitled()) &&
            (!_activeDocument.isModifiedSinceSave()));
  }

  /**
   * Creates a new document if there are currently no documents open.
   */
  private void _ensureNotEmpty() {
    if ((!_isClosingAllDocs) &&
        (getDefinitionsDocumentsSize() == 0)) {
      super.newFile();
    }
  }
  
  /**
   * some duplicated work, but avoids duplicated code, which is our nemesis
   */
  public void setActiveFirstDocument() {
    List<OpenDefinitionsDocument> docs = getDefinitionsDocuments();
    /* this will select the active document in the navigator, which
     * will signal a listener to call _setActiveDoc(...)
     */
    setActiveDocument(docs.get(0));
    //      _documentNavigator.setActiveDoc(getIDocGivenODD(docs.get(0)));
    //      _setActiveDoc(getIDocGivenODD(docs.get(0)));
  }
  
  private void _setActiveDoc(INavigatorItem idoc) {
    //Hashtable<INavigatorItem, OpenDefinitionsDocument> docs = getDefinitionsDocumentsTable();
    
    _activeDocument = super.getODDGivenIDoc(idoc);
    try {
    _activeDocument.checkIfClassFileInSync();
    
//    _documentNavigator.setActiveDoc(idoc);

    
    // notify single display model listeners   // notify single display model listeners
    _notifier.notifyListeners(new GlobalEventNotifier.Notifier() {
      public void notifyListener(GlobalModelListener l) {
        // If it is a SingleDisplayModelListener, let it know that the
        //  active doc changed
        if (l instanceof SingleDisplayModelListener) {
          SingleDisplayModelListener sl = (SingleDisplayModelListener) l;
          sl.activeDocumentChanged(_activeDocument);
        }
      }
    });
    } catch(DocumentClosedException dce) {
      //noop
    }
  }
  

}
