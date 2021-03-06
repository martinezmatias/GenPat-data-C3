/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is a part of DrJava. Current versions of this project are available
 * at http://sourceforge.net/projects/drjava
 *
 * Copyright (C) 2001-2002 JavaPLT group at Rice University (javaplt@rice.edu)
 * 
 * DrJava is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrJava is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * or see http://www.gnu.org/licenses/gpl.html
 *
 * In addition, as a special exception, the JavaPLT group at Rice University
 * (javaplt@rice.edu) gives permission to link the code of DrJava with
 * the classes in the gj.util package, even if they are provided in binary-only
 * form, and distribute linked combinations including the DrJava and the
 * gj.util package. You must obey the GNU General Public License in all
 * respects for all of the code used other than these classes in the gj.util
 * package: Dictionary, HashtableEntry, ValueEnumerator, Enumeration,
 * KeyEnumerator, Vector, Hashtable, Stack, VectorEnumerator.
 *
 * If you modify this file, you may extend this exception to your version of the
 * file, but you are not obligated to do so. If you do not wish to
 * do so, delete this exception statement from your version. (However, the
 * present version of DrJava depends on these classes, so you'd want to
 * remove the dependency first!)
 *
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.util.swing;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

import edu.rice.cs.util.UnexpectedException;

/**
 * Implementation of logic of find/replace over a document.
 *
 * @version $Id$
 */
public class FindReplaceMachine {
  /** The document on which FindReplaceMachine is operating. */
  private Document _doc;
  /** The position in _doc from which the searches started. */
  private Position _start;
  /** The position in _doc which the machine is currently at. */
  private Position _current;
  /** The word being sought. */
  private String _findWord;
  /** The word to replace the word being sought. */
  private String _replaceWord;
  
  /**
   * Constructor.
   * Creates a new machine to perform find/replace operations
   * on a particular document starting from a certain position.
   * @param doc the Document being operated on
   * @param position the character offset into the document
   * @exception BadLocationException
   */
  public FindReplaceMachine(Document doc, int position) 
    throws BadLocationException
  {
    // we have to put this in here because createPosition
    // will not catch these cases for us.  Better safe than
    // sorry, and we shouldn't have to deal later with positions
    // that are not within the bounds of the document.
    if ((position < 0) ||
        (position > doc.getLength())) 
    {
      throw new BadLocationException("position outside document bounds",
                                     position);
    }
    _doc = doc;
    _start = _doc.createPosition(position);
    _current = _doc.createPosition(position);
    _findWord = "";
    _replaceWord = "";
  }
 
  /**
   * Gets the character offset at which this machine started
   * operations.
   */
  public int getStartOffset() {
    return _start.getOffset();
  }
  
  /**
   * Gets the character offset to which this machine is currently pointing.
   */
  public int getCurrentOffset() {
    return _current.getOffset();
  }

  public void makeCurrentOffsetStart() {
    try {
      _start = _doc.createPosition(getCurrentOffset());
    }
    catch (BadLocationException e) {
      throw new UnexpectedException(e);
    }
  }
  
  public String getFindWord() {
    return _findWord;
  }
  
  public String getReplaceWord() {
    return _replaceWord;
  }
  
  /**
   * Change the word being sought.
   * @param word the new word to seek
   */
  public void setFindWord(String word) {
    _findWord = word;
  }
    
  /**
   * Change the replacing word.
   * @param word the new replacing word
   */
  public void setReplaceWord(String word) {
    _replaceWord = word;
  }
  
  /**
   * Determine if the machine is on an instance of the find word.
   * @return true if the current position is right after an instance
   *         of the find word.
   */
  public boolean isOnMatch() {
    try {
      String matchSpace = _doc.getText(0, _current.getOffset());
      return matchSpace.endsWith(_findWord);
    }
    catch (BadLocationException e) {
      throw new UnexpectedException(e);
    }
  }

  /**
   * Finds the next occurrence of the find word and returns an
   * offset at the end of that occurrence or -1 if the word was
   * not found.  Selectors should select backwards the length of
   * the find word from the find offset.  This position is stored
   * in the current offset of the machine, and that is why it is
   * after: in subsequent searches, the same instance won't be found
   * twice.
   */
  public int findNext(ContinueCommand startFromTop) {
    try {
      // get the search space in the document
      String findSpace = _doc.getText(_current.getOffset(), 
                                      _doc.getLength()-_current.getOffset());
      // find the first occurrence of _findWord
      int foundOffset = findSpace.indexOf(_findWord);
      // if we've found it
      if (foundOffset >= 0) {
        foundOffset += _current.getOffset() + _findWord.length();
        _current = _doc.createPosition(foundOffset);
      }
      else if ((getStartOffset() > 0) && (startFromTop.shouldContinue())) { 
        // if we haven't found it
        findSpace = _doc.getText(0, _start.getOffset());
        foundOffset = findSpace.indexOf(_findWord);
        if (foundOffset >= 0) {
          foundOffset += _findWord.length();
          _current = _doc.createPosition(foundOffset);
        }
      }
      else {
        foundOffset = -1;
      }
      return foundOffset;
    }
    catch (BadLocationException e) {
      throw new UnexpectedException(e);
    }
  }
  
  /**
   * If we're on a match for the find word, replace
   * it with the replace word.
  */
  public boolean replaceCurrent() {
    try {
      if (isOnMatch()) {
        _doc.remove(getCurrentOffset() - _findWord.length(),
                    _findWord.length());
        // the current offset will be correct since we keep track
        // of it as a Position.
        _doc.insertString(getCurrentOffset(), _replaceWord, null);
        return true;
      }
      else {
        return false;
      }
    }
    catch (BadLocationException e) {
      throw new UnexpectedException(e);
    }
  }
  
  public int replaceAll(ContinueCommand startFromTop) {
    int count = 0;
    try {
      Position midpoint = _doc.createPosition(_current.getOffset());
      count += _replaceAllWithinBounds(_current, _doc.getEndPosition());
      if (startFromTop.shouldContinue()) {
        _current = _doc.getStartPosition();
        count += _replaceAllWithinBounds(_current, midpoint);
      }
      return count;
    }
    catch (BadLocationException e) {
      throw new UnexpectedException(e);
    }
  }

  private int _replaceAllWithinBounds(Position first, Position last) {
    ContinueCommand halt = new ContinueCommand() {
      public boolean shouldContinue() {
        return false;
      }
    };
    int count = 0;
    int found = findNext(halt);
    while (found >= 0) {
      replaceCurrent();
      count++;
      found = findNext(halt);
    }
    return count;
  }
}
