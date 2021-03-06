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

package edu.rice.cs.drjava.model.definitions;


import javax.swing.text.*;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import edu.rice.cs.drjava.model.GlobalEventNotifier;

/** The editor kit class for editing Java source files. It functions as the controller in an MVC hierarchy.  It also
  * implements a factory for new documents and a factory for Views (the things that render the document).
  * @version $Id$
  */
public class DefinitionsEditorKit extends StyledEditorKit {
  
  private GlobalEventNotifier _notifier;
  private Action[] _actions;
  
  public static edu.rice.cs.util.Log LOG = new edu.rice.cs.util.Log("actions.txt",true);
  
  /** Creates a new editor kit with the given listeners.
    * @param notifier Keeps track of the listeners to the model
    */
  public DefinitionsEditorKit(GlobalEventNotifier notifier) {
    _notifier = notifier;
    Action[] supActions = super.getActions();
    _actions = new Action[supActions.length];
    LOG.log("DefinitionsEditorKit ctor");
    for(int i=0; i<_actions.length; ++i) {
      Action a = supActions[i];
      Object name = a.getValue("Name");
      LOG.log(name.toString());
      if (name.equals(beginWordAction)) {
        _actions[i] = new BeginWordAction(beginWordAction, false);
        LOG.log("\treplacing");
      }
      else if (name.equals(endWordAction)) {
        _actions[i] = new EndWordAction(endWordAction, false);
        LOG.log("\treplacing");
      }
      else if (name.equals(nextWordAction)){
        _actions[i] = new NextWordAction(nextWordAction, false);
        LOG.log("\treplacing");
      }
      else if (name.equals(previousWordAction)) {
        _actions[i] = new PreviousWordAction(previousWordAction, false);
        LOG.log("\treplacing");
      }
      else if (name.equals(selectWordAction)) {
        _actions[i] = new SelectWordAction();
        LOG.log("\treplacing");
      }
      else _actions[i] = a;
    }
  }
  
  public Action[] getActions() { return _actions; }
  
  private static ViewFactory _factory = new ViewFactory() {
    public View create(Element elem) {
      // The following line is for performance analysis only!
      // return new WrappedPlainView(elem, true);
      return new ColoringView(elem);
    }
  };
  
  /** Creates a new DefinitionsDocument.  Formerly named createDefaultDocument() because the view (DefinitionsPane)
    * would create a DefinitionsDocument by default when it was constructed.  However, this default document was  
    * immediately discarded because a DefinitionsDocument for the constructed DefinitionsPane already existed. 
    * Unfortunately, JEditorPane does not have a constructor that takes a Document as input.  We conceivably could
    * design this EditorKit to return the pre-existing document when the JEditorPane requests a new one, but the 
    * EditorKit is specified by a static field of DefinitionsPane so there is no clean way to install the proper
    * EditorKit before the JEditorPane constructor asks for the Document.
    *
    * As an easier alternative, we just let the DefaultEditorKit return a PlainDocument (much lighter weight),
    * which is thrown away when the true DefinitionsDocument is assigned.
    *
    * Improvements to this approach are welcome...  :)
    */
  public DefinitionsDocument createNewDocument() { return  _createDefaultTypedDocument(); }
  
  /** Creates a new DefinitionsDocument.
    * @return a new DefinitionsDocument.
    */
  private DefinitionsDocument _createDefaultTypedDocument() { return new DefinitionsDocument(_notifier); }
  
  /** Get the MIME content type of the document.
    * @return "text/java"
    */
  public String getContentType() { return "text/java"; }
  
  /** We want to use our ColoringView to render text, so here we return
    * a factory that creates ColoringViews.
    */
  public final ViewFactory getViewFactory() { return _factory; }
  
  static class BeginWordAction extends TextAction {
    BeginWordAction(String nm, boolean select) {
      super(nm);
      this.select = select;
    }

    public void actionPerformed(ActionEvent e) {
      LOG.log("BeginWordAction.actionPerformed");
      JTextComponent target = getTextComponent(e);
      if (target != null) {
        final String text = target.getText();
        int offs = target.getCaretPosition();
        while(offs>0) {
          char chPrev = text.charAt(offs - 1);
          if (("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(chPrev)>=0) || (Character.isWhitespace(chPrev))) {
            break;
          }
          --offs;
          char ch = text.charAt(offs);
          chPrev = text.charAt(offs - 1);
          if (("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(ch)>=0) || ("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(chPrev)>=0) || Character.isWhitespace(ch) || Character.isWhitespace(chPrev)) {
            break;
          }
        }
        if (select) {
          target.moveCaretPosition(offs);
        } else {
          target.setCaretPosition(offs);
        }
      }
    }
    private boolean select;
  }
  
  static class EndWordAction extends TextAction {
    EndWordAction(String nm, boolean select) {
      super(nm);
      this.select = select;
    }

    public void actionPerformed(ActionEvent e) {
      LOG.log("EndWordAction.actionPerformed");
      JTextComponent target = getTextComponent(e);
      if (target != null) {
        final String text = target.getText();
        int offs = target.getCaretPosition();
        while(offs<text.length()-1) {
          ++offs;
          char ch = text.charAt(offs);
          if (("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(ch)>=0) || Character.isWhitespace(ch)) {
            break;
          }
        }
        if (select) {
          target.moveCaretPosition(offs);
        } else {
          target.setCaretPosition(offs);
        }
      }
    }
    private boolean select;
  }
  
  static class PreviousWordAction extends TextAction {
    PreviousWordAction(String nm, boolean select) {
      super(nm);
      this.select = select;
    }

    public void actionPerformed(ActionEvent e) {
      LOG.log("PreviousWordAction.actionPerformed");
      JTextComponent target = getTextComponent(e);
      if (target != null) {
        final String text = target.getText();
        int offs = target.getCaretPosition();
        while(offs>=0) {
          --offs;
          char ch = text.charAt(offs);
          char chPrev = text.charAt(offs - 1);
          if (("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(ch)>=0) || ("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(chPrev)>=0) || Character.isWhitespace(chPrev)) {
            break;
          }
        }
        if (select) {
          target.moveCaretPosition(offs);
        } else {
          target.setCaretPosition(offs);
        }
      }
    }
    private boolean select;
  }
  
  static class NextWordAction extends TextAction {
    NextWordAction(String nm, boolean select) {
      super(nm);
      this.select = select;
    }

    public void actionPerformed(ActionEvent e) {
      LOG.log("NextWordAction.actionPerformed");
      JTextComponent target = getTextComponent(e);
      if (target != null) {
        final String text = target.getText();
        int offs = target.getCaretPosition();
        while(offs>=0) {
          ++offs;
          char ch = text.charAt(offs);
          char chPrev = text.charAt(offs - 1);
          if (("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(ch)>=0) || ("!@%^&*()-=+[]{};:'\",.<>/?".indexOf(chPrev)>=0) || Character.isWhitespace(chPrev)) {
            break;
          }
        }
        if (select) {
          target.moveCaretPosition(offs);
        } else {
          target.setCaretPosition(offs);
        }
      }
    }
    private boolean select;
  }
  
  static class SelectWordAction extends TextAction {
    public SelectWordAction() {
      super(selectWordAction);
      start = new BeginWordAction("pigdog", false);
      end = new EndWordAction("pigdog", true);
    }
    public void actionPerformed(ActionEvent e) {
      LOG.log("SelectWordAction.actionPerformed");
      start.actionPerformed(e);
      end.actionPerformed(e);
    }
    private Action start;
    private Action end;    
  }
}




