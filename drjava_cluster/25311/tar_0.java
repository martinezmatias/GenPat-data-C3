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

package edu.rice.cs.drjava.plugins.eclipse.views;

import org.eclipse.ui.part.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.SWT;

import edu.rice.cs.drjava.plugins.eclipse.repl.EclipseInteractionsModel;
import edu.rice.cs.drjava.model.repl.SimpleInteractionsModel;
import edu.rice.cs.util.text.SWTDocumentAdapter;

/**
 * This class is the main view component for the Interactions Pane
 * Eclipse plugin.
 * @version $Id$
 */
public class InteractionsView extends ViewPart {
  
  /**
   * Glue code between the model and view.
   * We have to have a reference to it so we can dispose it.
   */
  protected InteractionsController _controller;
  
  /**
   * The widget displaying the text.  Equivalent of a JTextPane.
   */
  protected StyledText _styledText;
  
  /**
   * An arrow cursor to display when the pane is not busy.
   */
  protected Cursor _arrowCursor;
  
  /**
   * An hourglass cursor to display when the pane is busy.
   */
  protected Cursor _waitCursor;
  
  /**
   * A runnable command to sound a beep as an alert.
   */
  protected Runnable _beep;
  
  /**
   * Constructor.
   */
  public InteractionsView() {
    _beep = new Runnable() {
      public void run() {
        _styledText.getDisplay().beep();
      }
    };
  }

  /**
   * Cleans up any resources this view created.
   */
  public void dispose() {
    _arrowCursor.dispose();
    _waitCursor.dispose();
    _controller.dispose();
    _styledText.dispose();
    super.dispose();
  }
  
  /**
   * Accessor method for the text widget.
   */
  public StyledText getTextPane() {
    return _styledText;
  }
  
  /**
   * Returns a command that creates a beep when run.
   */
  public Runnable getBeep() {
    return _beep;
  }
  
  /**
   * Sets the command that creates a beep when run.
   */
  public void setBeep(Runnable beep) {
    _beep = beep;
  }
  
  /**
   * Callback method that creates and initializes the view.
   */
  public void createPartControl(Composite parent) {
    // NOTE: Do not let anything instantiate the DrJava config framework here...
    setTextPane(new StyledText(parent, SWT.WRAP | SWT.V_SCROLL));
    
    SWTDocumentAdapter adapter = new SWTDocumentAdapter(_styledText);
    EclipseInteractionsModel model = new EclipseInteractionsModel(adapter);
    setController(new InteractionsController(model, adapter, this));
    
  }
  
  /**
   * Sets which text pane this view uses.  Package-private; for tests only.
   * NOTE: Should only be called once!
   * @param text A StyledText pane to use for the text
   */
  void setTextPane(StyledText text) {
    _styledText = text;
    _arrowCursor = new Cursor(_styledText.getDisplay(), SWT.CURSOR_ARROW);
    _waitCursor = new Cursor(_styledText.getDisplay(), SWT.CURSOR_WAIT);
  }
  
  /**
   * Sets which controller is managing this view, so we can dispose it.
   * Package-private; for tests only.
   * @param controller An InteractionsController managing the view
   */
  void setController(InteractionsController controller) {
    _controller = controller;
  }
  
  /**
   * Returns any text that is after the current prompt.
   */
  public String getCurrentInteraction(int promptPos) {
    int length = _styledText.getText().length();
    return _styledText.getText(promptPos, length - 1);
  }
  
  /**
   * Sets whether the StyledText widget is editable.
   * @param editable whether the widget should be editable
   */
  public void setEditable(final boolean editable) {
    _styledText.getDisplay().syncExec(new Runnable() {
      public void run() {
        _styledText.setEditable(editable);
      }
    });
  }
  
  /**
   * Sets whether a busy (hourglass) cursor is currently shown.
   * @param busy Whether to show a busy cursor
   */
  public void setBusyCursorShown(final boolean busy) {
    _styledText.getDisplay().syncExec(new Runnable() {
      public void run() {
        if (busy) {
          _styledText.setCursor(_waitCursor);
        }
        else {
          _styledText.setCursor(_arrowCursor);
        }
      }
    });
  }
  
  /**
   * Shows a modal dialog without halting the thread of execution.
   * @param title Title of the dialog box
   * @param msg Message to display
   */
  public void showInfoDialog(final String title, final String msg) {
    _styledText.getDisplay().asyncExec(new Runnable() {
      public void run() {
        MessageBox box = new MessageBox(_styledText.getShell(), 
                                        SWT.ICON_INFORMATION | SWT.OK);
        box.setMessage(msg);
        box.open();
      }
    });
  }
  
  /**
   * Gives the focus to this component.
   */
  public void setFocus() {
    _styledText.setFocus();
  }
}