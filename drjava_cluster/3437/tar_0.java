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

package edu.rice.cs.drjava.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import edu.rice.cs.drjava.model.repl.InteractionsScriptModel;

/**
 * Controller for an interactions script.
 * @version $Id$
 */
public class InteractionsScriptController {
  /** Associated model. */
  private InteractionsScriptModel _model;
  /** Associated view. */
  private InteractionsScriptPane _pane;
  /** Interactions pane. */
  private InteractionsPane _interactionsPane;

  /**
   * Builds a new interactions script pane and links it to the given model.
   * @param model the InteractionsScriptModel to use
   * @param closeAction how to close this script.
   */
  public InteractionsScriptController(InteractionsScriptModel model, Action closeAction,
                                      InteractionsPane interactionsPane) {
    _model = model;
    _closeScriptAction = closeAction;
    _interactionsPane = interactionsPane;
    _pane = new InteractionsScriptPane(4, 1);
    
    // Previous
    _setupAction(_prevInteractionAction, "Previous", "Insert Previous Interaction from Script");
    _pane.addButton(_prevInteractionAction);
    // Next
    _setupAction(_nextInteractionAction, "Next", "Insert Next Interaction from Script");
    _pane.addButton(_nextInteractionAction);
    // Execute
    _setupAction(_executeInteractionAction, "Execute", "Execute Current Interaction");
    _pane.addButton(_executeInteractionAction, _interactionsPane);
    // Close
    _setupAction(_closeScriptAction, "Close", "Close Interactions Script");
    _pane.addButton(_closeScriptAction);
    setActionsEnabled();
  }

  /**
   * Sets the navigation actions to be enabled, if appropriate.
   */
  public void setActionsEnabled() {
    _nextInteractionAction.setEnabled(_model.hasNextInteraction());
    _prevInteractionAction.setEnabled(_model.hasPrevInteraction());
    _executeInteractionAction.setEnabled(true);
  }

  /**
   * Disables navigation actions
   */
  public void setActionsDisabled() {
    _nextInteractionAction.setEnabled(false);
    _prevInteractionAction.setEnabled(false);
    _executeInteractionAction.setEnabled(false);
  }

  /**
   * @return the interactions script pane controlled by this controller.
   */
  public InteractionsScriptPane getPane() {
    return _pane;
  }

  /** Action to go back in the script. */
  private Action _prevInteractionAction = new AbstractAction("Previous") {
    public void actionPerformed(ActionEvent e) {
      _model.prevInteraction();
      setActionsEnabled();
      _interactionsPane.requestFocus();
    }
  };
  /** Action to go forward in the script. */
  private Action _nextInteractionAction = new AbstractAction("Next") {
    public void actionPerformed(ActionEvent e) {
      _model.nextInteraction();
      setActionsEnabled();
      _interactionsPane.requestFocus();
    }
  };
  /** Action to execute the current interaction. */
  private Action _executeInteractionAction = new AbstractAction("Execute") {
    public void actionPerformed(ActionEvent e) {
      _model.executeInteraction();      
      _interactionsPane.requestFocus();
    }
  };
  /** Action to end the script.  (Defined in constructor.) */
  private Action _closeScriptAction; /* = new AbstractAction("<=Close=>") {
    public void actionPerformed(ActionEvent e) {
      _model.closeScript();
      _pane.setMaximumSize(new Dimension(0,0));
    }
  };*/
  
  /**
   * Sets up fields on the given Action, such as the name and tooltip.
   * @param a Action to modify
   * @param name Default name for the Action (for buttons)
   * @param desc Short description of the Action (for tooltips)
   */
  protected void _setupAction(Action a, String name, String desc) {
    a.putValue(Action.DEFAULT, name);
    a.putValue(Action.SHORT_DESCRIPTION, desc);
  }
}