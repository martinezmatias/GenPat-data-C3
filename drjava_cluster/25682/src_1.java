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

package edu.rice.cs.drjava.ui;

import java.util.Vector;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Enumeration;
import java.io.File;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.text.BadLocationException;
import java.awt.event.*;
import java.awt.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import edu.rice.cs.drjava.model.RegionManagerListener;
import edu.rice.cs.drjava.model.DocumentRegion;
import edu.rice.cs.drjava.model.SimpleDocumentRegion;
import edu.rice.cs.drjava.model.OpenDefinitionsDocument;
import edu.rice.cs.drjava.model.FileMovedException;
import edu.rice.cs.drjava.model.RegionManager;
import edu.rice.cs.drjava.config.*;
import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.util.swing.Utilities;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.Pair;
import edu.rice.cs.util.StringOps;
import edu.rice.cs.drjava.config.OptionConstants;

/**
 * Panel for displaying find results.
 * This class is a swing view class and hence should only be accessed from the event-handling thread.
 * @version $Id$
 */
public class FindResultsPanel extends RegionsTreePanel<DocumentRegion> {
  protected JButton _goToButton;
  protected JButton _bookmarkButton;
  protected JButton _removeButton;
  protected JComboBox _colorBox;
  protected RegionManager<DocumentRegion> _regionManager;
  protected int _lastIndex;
  
  /** Saved option listeners kept in this field so they can be removed for garbage collection  */
  private LinkedList<Pair<Option<Color>, OptionListener<Color>>> _colorOptionListeners = 
    new LinkedList<Pair<Option<Color>, OptionListener<Color>>>();
  
  /** Constructs a new find results panel.
   *  This is swing view class and hence should only be accessed from the event-handling thread.
   *  @param frame the MainFrame
   *  @param rm the region manager associated with this panel
   *  @param title for the panel
   */
  public FindResultsPanel(MainFrame frame, RegionManager<DocumentRegion> rm, String title) {
    super(frame, title);
    _regionManager = rm;
    _regionManager.addListener(new RegionManagerListener<DocumentRegion>() {      
      public void regionAdded(DocumentRegion r, int index) {
        addRegion(r);
      }
      public void regionChanged(DocumentRegion r, int index) { 
        regionRemoved(r);
        regionAdded(r, index);
      }
      public void regionRemoved(DocumentRegion r) {
        removeRegion(r);
      }
    });
    
    OptionListener<Color> temp;
    Pair<Option<Color>, OptionListener<Color>> pair;
    for(int i=0; i<OptionConstants.FIND_RESULTS_COLORS.length; ++i) {
      temp = new FindResultsColorOptionListener(i);
      pair = new Pair<Option<Color>, OptionListener<Color>>(OptionConstants.FIND_RESULTS_COLORS[i], temp);
      _colorOptionListeners.add(pair);
      DrJava.getConfig().addOptionListener( OptionConstants.FIND_RESULTS_COLORS[i], temp);
    }
  }
  
  static class ColorComboRenderer extends JPanel implements ListCellRenderer {
    private Color m_c = Color.black;
    private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private final static Dimension preferredSize = new Dimension(0, 20);    
    
    public ColorComboRenderer() {
      super();
      setBorder(new CompoundBorder(
                                   new MatteBorder(2, 10, 2, 10, Color.white), new LineBorder(
                                                                                              Color.black)));
    }
    
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int row, boolean sel, boolean hasFocus) {
      Component renderer;
      if (value instanceof Color) {
        m_c = (Color) value;
        renderer = this;
      }
      else {
        JLabel l = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, row, sel, hasFocus);
        l.setHorizontalAlignment(JLabel.CENTER);
        renderer = l;
      }
      // Taken out because this is a 1.5 method; not sure if it's necessary
      //renderer.setPreferredSize(preferredSize);
      return renderer;
    }
    
    public void paint(Graphics g) {
      setBackground(m_c);
      super.paint(g);
    }
  }
  
  /** Creates the buttons for controlling the regions. Should be overridden. */
  protected JComponent[] makeButtons() {    
    Action goToAction = new AbstractAction("Go to") {
      public void actionPerformed(ActionEvent ae) {
        goToRegion();
      }
    };
    _goToButton = new JButton(goToAction);

    Action bookmarkAction = new AbstractAction("Bookmark") {
      public void actionPerformed(ActionEvent ae) {
        _bookmark();
      }
    };
    _bookmarkButton = new JButton(bookmarkAction);

    Action removeAction = new AbstractAction("Remove") {
      public void actionPerformed(ActionEvent ae) {
        _remove();
      }
    };
    _removeButton = new JButton(removeAction);

    // "Highlight" label panel
    final JPanel highlightPanel = new JPanel();
    final Color normalColor = highlightPanel.getBackground();
    highlightPanel.add(new JLabel("Highlight:"));
    
    _colorBox = new JComboBox();    
    for(int i=0; i<OptionConstants.FIND_RESULTS_COLORS.length; ++i) {
      _colorBox.addItem(DrJava.getConfig().getSetting(OptionConstants.FIND_RESULTS_COLORS[i]));
    }
    _colorBox.addItem("None");
    _colorBox.setRenderer(new ColorComboRenderer());
    _colorBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (_lastIndex<OptionConstants.FIND_RESULTS_COLORS.length) {
          --DefinitionsPane.FIND_RESULTS_PAINTERS_USAGE[_lastIndex];
        }
        _lastIndex = _colorBox.getSelectedIndex();
        if (_lastIndex<OptionConstants.FIND_RESULTS_COLORS.length) {
          ++DefinitionsPane.FIND_RESULTS_PAINTERS_USAGE[_lastIndex];
          highlightPanel.setBackground(DrJava.getConfig().getSetting(OptionConstants.FIND_RESULTS_COLORS[_lastIndex]));
        }
        else {
          highlightPanel.setBackground(normalColor);
        }
        _frame.refreshFindResultsHighlightPainter(FindResultsPanel.this, 
                                                  DefinitionsPane.FIND_RESULTS_PAINTERS[_lastIndex]);
      }
    });
    // find the first available color, or choose "None"
    for(_lastIndex=0; _lastIndex<OptionConstants.FIND_RESULTS_COLORS.length; ++_lastIndex) {
      if (DefinitionsPane.FIND_RESULTS_PAINTERS_USAGE[_lastIndex]==0) {
        break;
      }
    }
    if (_lastIndex<OptionConstants.FIND_RESULTS_COLORS.length) {
      ++DefinitionsPane.FIND_RESULTS_PAINTERS_USAGE[_lastIndex];
    }
    _colorBox.setSelectedIndex(_lastIndex);
    _frame.refreshFindResultsHighlightPainter(FindResultsPanel.this, 
                                              DefinitionsPane.FIND_RESULTS_PAINTERS[_lastIndex]);
    
    JComponent[] buts = new JComponent[] { 
      _goToButton, 
        _bookmarkButton,
        _removeButton,
        highlightPanel,
        _colorBox
    };
    
    return buts;
  }
  
  /** @return the selected painter for these find results. */
  public ReverseHighlighter.DefaultUnderlineHighlightPainter getSelectedPainter() {
    return DefinitionsPane.FIND_RESULTS_PAINTERS[_lastIndex];
  }
  
  /** Turn the selected regions into bookmarks. */
  private void _bookmark() {
    for (final DocumentRegion r: getSelectedRegions()) {
      DocumentRegion bookmark = _model.getBookmarkManager().getRegionOverlapping(r.getDocument(),
                                                                                 r.getStartOffset(),
                                                                                 r.getEndOffset());
      if (bookmark==null) {
        try {
          _model.getBookmarkManager().addRegion(new SimpleDocumentRegion(r.getDocument(), r.getDocument().getFile(), r.getStartOffset(), r.getEndOffset()));
        }
        catch (FileMovedException fme) {
          throw new UnexpectedException(fme);
        }
      }
    }
  }
  
  /** Action performed when the Enter key is pressed. Should be overridden. */
  protected void performDefaultAction() {
    goToRegion();
  }
  
  /** Remove the selected regions. */
  private void _remove() {
    for (DocumentRegion r: getSelectedRegions()) {
      _regionManager.removeRegion(r);
    }
    if (_regionManager.getRegions().size()==0) { _close(); }
  }

  /** Update button state and text. */
  protected void updateButtons() {
    ArrayList<DocumentRegion> regs = getSelectedRegions();
    _goToButton.setEnabled(regs.size()==1);
    _bookmarkButton.setEnabled(regs.size()>0);
    _removeButton.setEnabled(regs.size()>0);
  }
  
  /** Makes the popup menu actions. Should be overridden if additional actions besides "Go to" and "Remove" are added. */
  protected AbstractAction[] makePopupMenuActions() {
    AbstractAction[] acts = new AbstractAction[] {
      new AbstractAction("Go to") {
        public void actionPerformed(ActionEvent e) {
          goToRegion();
        }
      },
        
        new AbstractAction("Bookmark") {
          public void actionPerformed(ActionEvent e) {
            _bookmark();
          }
        },
        
        new AbstractAction("Remove") {
          public void actionPerformed(ActionEvent e) {
            _remove();
          }
        }
    };
    return acts;
  }
  
  /** Close the pane. */
  public void _close() {
    super._close();
    _regionManager.clearRegions();
    _model.disposeFindResultsManager(_regionManager);
    for (Pair<Option<Color>, OptionListener<Color>> p: _colorOptionListeners) {
      DrJava.getConfig().removeOptionListener(p.getFirst(), p.getSecond());
    }
    if (_lastIndex<OptionConstants.FIND_RESULTS_COLORS.length) {
      --DefinitionsPane.FIND_RESULTS_PAINTERS_USAGE[_lastIndex];
    }
  }

  /** Factory method to create user objects put in the tree.
   *  If subclasses extend RegionTreeUserObj, they need to override this method. */
  protected RegionTreeUserObj<DocumentRegion> makeRegionTreeUserObj(DocumentRegion r) {
    return new FindResultsRegionTreeUserObj(r);
  }

  /** Class that gets put into the tree. The toString() method determines what's displayed in the three. */
  protected static class FindResultsRegionTreeUserObj extends RegionTreeUserObj<DocumentRegion> {
    public FindResultsRegionTreeUserObj(DocumentRegion r) { super(r); }
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      _region.getDocument().acquireReadLock();
      try {
        sb.append("<html>");
        sb.append(lineNumber());
        try {
          sb.append(": ");
          int endSel = _region.getDocument().getLineEndPos(_region.getEndOffset());
          int startSel = _region.getDocument().getLineStartPos(_region.getStartOffset());
          int length = Math.min(120, endSel-startSel);
          
          // this highlights the actual region in red
          int startRed = _region.getStartOffset() - startSel;
          int endRed = _region.getEndOffset() - startSel;
          String s = _region.getDocument().getText(startSel, length);
          for(int i=0; i<s.length(); ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
              break;
            }
            --startRed;
            --endRed;
          }
          s = s.trim();
          if (startRed<0) { startRed = 0; }
          if (startRed>s.length()) { startRed = s.length(); }
          if (endRed<startRed) { endRed = startRed; }
          if (endRed>s.length()) { endRed = s.length(); }
          sb.append(StringOps.encodeHTML(s.substring(0, startRed)));
          sb.append("<font color=#ff0000>");
          sb.append(StringOps.encodeHTML(s.substring(startRed, endRed)));
          sb.append("</font>");
          sb.append(StringOps.encodeHTML(s.substring(endRed)));
          sb.append("</html>");
        } catch(BadLocationException bpe) { /* ignore, just don't display line */ }        
      } finally { _region.getDocument().releaseReadLock(); }
      return sb.toString();
    }
  }
  
  /** The OptionListener for FIND_RESULTS_COLOR. */
  private class FindResultsColorOptionListener implements OptionListener<Color> {
    private int _index;
    public FindResultsColorOptionListener(int i) { _index = i; }
    public void optionChanged(OptionEvent<Color> oce) {
      int pos = _colorBox.getSelectedIndex();
      _colorBox.removeItemAt(_index);
      _colorBox.insertItemAt(oce.value, _index);
      _colorBox.setSelectedIndex(pos);
      if (pos==_index) {
        _frame.refreshFindResultsHighlightPainter(FindResultsPanel.this, DefinitionsPane.FIND_RESULTS_PAINTERS[_index]);
      }
    }
  }
}
