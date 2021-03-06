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

package edu.rice.cs.util.text;

import edu.rice.cs.util.UnexpectedException;
import static edu.rice.cs.util.text.AbstractDocumentInterface.*;

import java.awt.print.Pageable;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.Position;
import javax.swing.text.BadLocationException;

import java.util.Hashtable;

/** A swing implementation of the toolkit-independent EditDocumentInterface.  This document must use the readers/writers
  * locking protocol established in its superclasses.
  * TODO: create a separate DummySwingDocument class for testing and make SwingDocument abstract.
  * @version $Id$
  */
public class SwingDocument extends DefaultStyledDocument implements EditDocumentInterface, AbstractDocumentInterface {
  
//  /** The lock state.  See ReadersWritersLocking interface for documentation. */
  protected volatile int _lockState = UNLOCKED;
  
  /** The modified state. */
  protected volatile boolean _isModifiedSinceSave = false;
  
  /** The visible locking state. */
  protected volatile boolean _readLocked = false;
  protected volatile boolean _writeLocked = false;
  
  /** Maps names to attribute sets */
  final protected Hashtable<String, AttributeSet> _styles;
  
  /** Determines which edits are legal on this document. */
  protected DocumentEditCondition _condition;
  
  /** Lock used to protect _wrappedPosListLock in DefinitionsDocument.  Placed here to ensure that it initialized before
    * use! */
  protected static final Object _wrappedPosListLock = new Object();
  
  /** Creates a new document adapter for a Swing StyledDocument. TODO: convert _styles and _condition to lazily 
    * initialized volatiles as soon as support for Java 1.4 is dropped and the double-check idiom is safe. */
  public SwingDocument() { 
    _styles = new Hashtable<String, AttributeSet>();
    _condition = new DocumentEditCondition();
  }
  
  /** Adds the given AttributeSet as a style with the given name. It can then be used in insertString.
    * @param name Name of the style, to be passed to insertString
    * @param s AttributeSet to use for the style
    */
  public void setDocStyle(String name, AttributeSet s) {
    _styles.put(name, s);  // no locking necessary: _styles is final and Hashtable is thread-safe
  }
  
  /** Returns the style with the given name, or null if no such named style exists. */
  public AttributeSet getDocStyle(String name) {
    return _styles.get(name);  // no locking necessary: _styles is final and Hashtable is thread-safe
  }
  
  /** Adds the given coloring style to the styles list.  Not supported in SwingDocument. 
    * Assumes that WriteLock is already held.
    */
  public void addColoring(int start, int end, String style) { }
  
  /** Gets the object which can determine whether an insert or remove edit should be applied, based on the inputs.
    * @return an Object to determine legality of inputs
    */
  public DocumentEditCondition getEditCondition() { return _condition; }
  
  /** Provides an object which can determine whether an insert or remove edit should be applied, based on the inputs.
    * @param condition Object to determine legality of inputs
    */
  public void setEditCondition(DocumentEditCondition condition) {
    acquireWriteLock();
    try { _condition = condition; }
    finally { releaseWriteLock(); }
  }
  
  /* Clears the document. */
  public void clear() {
    acquireWriteLock();
    try { remove(0, getLength()); }
    catch(BadLocationException e) { throw new UnexpectedException(e); }
    finally { releaseWriteLock(); }
  }
  
  /** Inserts a string into the document at the given offset and style, if the edit condition allows it.
    * @param offs Offset into the document
    * @param str String to be inserted
    * @param style Name of the style to use.  Must have been added using addStyle.
    * @throws EditDocumentException if the offset is illegal
    */
  public void insertText(int offs, String str, String style) {
    acquireWriteLock();
    try { _insertText(offs, str, style); }
    finally { releaseWriteLock(); }
  }
  
  /** Behaves exactly like insertText except for assuming that WriteLock is already held. */
  public void _insertText(int offs, String str, String style) {
    if (_condition.canInsertText(offs)) _forceInsertText(offs, str, style); 
  }
  
  /** Behaves exactly like forceInsertText except for assuming that WriteLock is already held. */
  public void _forceInsertText(int offs, String str, String style) {
    int len = getLength();
    if ((offs < 0) || (offs > len)) {
      String msg = "Offset " + offs + " passed to SwingDocument.forceInsertText is out of bounds [0, " + len + "]";
      throw new EditDocumentException(null, msg);
    }
    AttributeSet s = null;
    if (style != null) s = getDocStyle(style);
    try { super.insertString(offs, str, s); }
    catch (BadLocationException e) { throw new EditDocumentException(e); }  // should never happen
  }
  
  /** Inserts a string into the document at the given offset and style, regardless of the edit condition.
    * @param offs Offset into the document
    * @param str String to be inserted
    * @param style Name of the style to use.  Must have been added using addStyle.
    * @throws EditDocumentException if the offset is illegal
    */
  public void forceInsertText(int offs, String str, String style) {
    acquireWriteLock();
    try { _forceInsertText(offs, str, style); }
    finally { releaseWriteLock(); }
  }
  
  /** Overrides superclass's insertString to impose the edit condition. The AttributeSet is ignored in the condition, 
    * which sees a null style name.
    */
  public void insertString(int offs, String str, AttributeSet set) throws BadLocationException {
    acquireWriteLock();  // locking is used to make the test and modification atomic
    try { _insertString(offs, str, set); }
    finally { releaseWriteLock(); }
  }
  
  /** Raw version of insertString.  Assumes write lock is already held. */
  public void _insertString(int offs, String str, AttributeSet set) throws BadLocationException {
    if (_condition.canInsertText(offs)) super.insertString(offs, str, set);
  }
  
  /** Removes a portion of the document, if the edit condition allows it.
    * @param offs Offset to start deleting from
    * @param len Number of characters to remove
    * @throws EditDocumentException if the offset or length are illegal
    */
  public void removeText(int offs, int len) {
    acquireWriteLock();  // locking is used to make the test and modification atomic
    try { _removeText(offs, len); }
    finally { releaseWriteLock(); }
  }
  
  /** Removes a portion of the document, if the edit condition allows it, as above.  Assume sthat WriteLock is held */
  public void _removeText(int offs, int len) {
    if (_condition.canRemoveText(offs)) forceRemoveText(offs, len); 
  }
  
  /** Removes a portion of the document, regardless of the edit condition.
    * @param offs Offset to start deleting from
    * @param len Number of characters to remove
    * @throws EditDocumentException if the offset or length are illegal
    */
  public void forceRemoveText(int offs, int len) {
    /* Using a writeLock is unnecessary because remove is already thread-safe */
    try { super.remove(offs, len); }
    catch (BadLocationException e) { throw new EditDocumentException(e); }
  }
  
  /** Overrides superclass's remove to impose the edit condition. */
  public void remove(int offs, int len) throws BadLocationException {
    acquireWriteLock(); // locking is used to make the test and modification atomic
    try { if (_condition.canRemoveText(offs))  super.remove(offs, len); }
    finally { releaseWriteLock(); }
  }
  
//  /** Returns the length of the document. */
//  public int getDocLength() { return getLength(); } // locking is unnecessary because getLength is already thread-safe
  
  /** Returns a portion of the document.
    * @param offs First offset of the desired text
    * @param len Number of characters to return
    * @throws EditDocumentException if the offset or length are illegal
    */
  public String getDocText(int offs, int len) {
    try { return getText(offs, len); }  // locking is unnecessary because getText is already thread-safe
    catch (BadLocationException e) { throw new EditDocumentException(e); }
  }
  
   /** Gets the document text; this method is threadsafe. */
  public String getText() {
    acquireReadLock();
    try { return _getText(); }
    finally { releaseReadLock(); }
  }
  
  /** Raw version of getText() that assumes the ReadLock is already held. */
  public String _getText() { 
    try { return getText(0, getLength()); }  // calls method defined in DefaultStyledDocument
    catch (BadLocationException e) { throw new UnexpectedException(e); }  // impossible if read lock is already held
  }
 
  /** Raw version of getText(int, int) that converts BadLocationException to UnexpectedException. */
  public String _getText(int pos, int len) { 
    try { return getText(pos, len); }  // calls method defined in DefaultStyledDocument
    catch (BadLocationException e) { throw new UnexpectedException(e); }
  }
  /** Appends given string with specified attributes to end of this document. */
  public void append(String str, AttributeSet set) {
    acquireWriteLock();
    try { _append(str, set); }
    finally { releaseWriteLock(); }
  }
  
  /** Same as append above except that it assumes the Write Lock is already held. */
  public void _append(String str, AttributeSet set) {
    try { _insertString(getLength(), str, set); }
    catch (BadLocationException e) { throw new UnexpectedException(e); }  // impossible
  }
  
  /** Appends given string with specified named style to end of this document. */
  public void append(String str, String style) { append(str, style == null ? null : getDocStyle(style)); }
  
  /** Appends given string with default style to end of this document. */
  public void append(String str) { append(str, (AttributeSet) null); }
  
  /** A SwingDocument instance does not have a default style */
  public String getDefaultStyle() { return null; }
  
  public void print() {
    throw new UnsupportedOperationException("Printing not supported");
  }
  
  public Pageable getPageable() {
    throw new UnsupportedOperationException("Printing not supported");
  }
  
  /* Locking operations */
  
  /* Swing-style readLock(). Must be renamed because inherited writeLock is final. */
  public synchronized void acquireReadLock() {
    readLock();
    if (_lockState >= UNLOCKED) _lockState++;
    // otherwise a write locked object is being recursively read locked; ignore
  }
  
  /* Swing-style readUnlock(). Must be renamed because inherited writeLock is final. */
  public synchronized void releaseReadLock() {
    readUnlock();
    if (_lockState > UNLOCKED) _lockState--;
  }
  
  /** Swing-style writeLock().  Must be renamed because inherited writeLock is final. */
  public synchronized void acquireWriteLock() {
    writeLock(); 
    if (_lockState <= UNLOCKED) _lockState--; 
  }
  
  /** Swing-style writeUnlock().  Must be renamed because inherited writeUnlock is final.*/
  public synchronized void releaseWriteLock() { 
    writeUnlock();
    if (_lockState < UNLOCKED) _lockState++; 
  }
  
  /** Returns true iff this thread holds a read lock or write lock. */
  public boolean isReadLocked() { return _lockState != UNLOCKED; }
  
  /** Returns true iff this thread holds a write lock. */
  public boolean isWriteLocked() { return _lockState < 0; }
  
  /** Performs the default behavior for createPosition in DefaultStyledDocument. */
  public Position createUnwrappedPosition(int offs) throws BadLocationException { return super.createPosition(offs); }
}

