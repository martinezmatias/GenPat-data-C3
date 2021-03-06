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

/**
 * TO ADD:
 * 
 * x multiple selection restrict
 * x show/hide hidden files
 * x set top component
 * x set root after instantiation
 * ! check boxes at each node?
 * x create/mutate directory
 *   x when ok/cancel/close, stopEdits
 * x delete
 * x toggle editable
 * x entering edit mode
 * x right-click menus
 *   x enabling/hiding with editable
 * x <enter>/<escape> key bindings
 * x need to set the open/closed/leaf icons change
 * x need to make so changes propagate to children.
 * x make editable optional
 * x center in parent
 * x roots should not be editable
 * x null pointer when right-click in empty space
 * x add accept enable filter
 */

package edu.rice.cs.util.swing;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;

import sun.awt.shell.ShellFolder;

public class DirectoryChooser extends JDialog {
  
  public static int APPROVE_OPTION = JFileChooser.APPROVE_OPTION;
  public static int CANCEL_OPTION = JFileChooser.CANCEL_OPTION;
  public static int ERROR_OPTION = JFileChooser.ERROR_OPTION;
    
  protected JTree _tree;
  protected static JFileChooser _jfc;
  
  protected DefaultMutableTreeNode _root;
  protected File    _rootFile;
  protected boolean _allowMultiple;
  protected boolean _showHidden;
  protected boolean _showFiles;
  protected boolean _isEditable;
  protected int     _finalResult;
  protected Set<File> _offLimits;
  
  private TreeExpansionListener _expansionListener;
  private LinkedList<FileFilter> _choosableDirs;
  private LinkedList<FileFilter> _normalFileFilters;
  private Action _cancelAction;
  
  private JScrollPane _scroller;
  private JPanel   _topComponentPanel;
  private JLabel   _topLabel;
  private JPanel   _northPanel;
  private JPanel   _newButtonPanel;
  private JButton  _newFolderButton;
  private JButton  _approveButton;
  private JButton  _cancelButton;
  private JPanel   _buttonPanel;
  private JPanel   _accessoryPanel;
  private JPanel   _southPanel;
  
  protected JComponent _accessory;
  
  protected JPopupMenu _treePopup;
  protected JMenuItem _collapseItem;
  protected JMenuItem _expandItem;
  protected JMenuItem _renameItem;
  protected JMenuItem _deleteItem;
  protected JMenuItem _newFolderItem;
  protected JPopupMenu.Separator _popSep;
  
  protected String _approveText  = "Open";
  protected String _cancelText   = "Cancel";
  protected String _topLabelText = "Select a directory:";
  
  protected Icon _newFolderIcon = UIManager.getIcon("FileChooser.newFolderIcon");
    
  /**
   * Creates a DirectoryChooser whose root starts at the root of the 
   * file system and that allows only one selection at a time.
   */
  public DirectoryChooser() {
    this((Frame)null, null, false, false);
  }
  
  /**
   * Creates a DirectoryChooser whose root is the file system root, 
   * allowing only single selection.
   */
  public DirectoryChooser(Dialog owner) {
    this(owner, null, false, false);
  }
  
  /**
   * Creates a DirectoryChooser whose root is the file system root, 
   * allowing only single selection.
   */
  public DirectoryChooser(Frame owner) {
    this(owner, null, false, false);
  }
  
  /**
   * Creates a DirectoryChooser whose root is the file system root, 
   * allowing multiple selection as specified
   * @param allowMultiple whether to allow multiple selection
   */
  public DirectoryChooser(Dialog owner, boolean allowMultiple) {
    this(owner, null, allowMultiple, false);
  }
  /**
   * Creates a DirectoryChooser whose root is the file system root, 
   * allowing multiple selection as specified
   * @param allowMultiple whether to allow multiple selection
   */
  public DirectoryChooser(Frame owner, boolean allowMultiple) {
    this(owner, null, allowMultiple, false);
  }
  
  /**
   * Creates a DirectoryChooser whose root starts with the given file, 
   * allowing only single selections
   * @param root the root directory to display in the tree
   */
  public DirectoryChooser(Dialog owner, File root) {
    this(owner, root, false, false);
  }
  
  /**
   * Creates a DirectoryChooser whose root starts with the given file, 
   * allowing only single selections
   * @param root the root directory to display in the tree
   */
  public DirectoryChooser(Frame owner, File root) {
    this(owner, root, false, false);
  }
  
  /**
   * Creates a DirectoryChooser with the given root, allowing multiple
   * selections according to the second parameter.
   * @param owner the 
   * @param root the root directory to display in the tree. If null, then show entire file system
   * @param allowMultiple whether to allow multiple selection
   */
  public DirectoryChooser(Dialog owner, File root, boolean allowMultiple, boolean showHidden) {
    super(owner, "Choose Directory", true);
    _init(owner,root,allowMultiple,showHidden);
  }
  
  /**
   * Creates a DirectoryChooser with the given root, allowing multiple
   * selections according to the second parameter.
   * @param owner the 
   * @param root the root directory to display in the tree. If null, then show entire file system
   * @param allowMultiple whether to allow multiple selection
   */
  public DirectoryChooser(Frame owner, File root, boolean allowMultiple, boolean showHidden) {
    super(owner, "Choose Directory", true);
    _init(owner,root,allowMultiple,showHidden);
  }
  
  ////////////////// INITIALIZATION METHODS /////////////////
    
  /**
   * Sets up the GUI components of the dialog
   */
  private void _init(Component owner, File root, boolean allowMultiple, boolean showHidden) {
    if (root != null && !root.isDirectory()) {
      root = root.getAbsoluteFile().getParentFile();
    }
        
    _rootFile = root;
    _allowMultiple = allowMultiple;
    _showHidden = showHidden;
    _finalResult = ERROR_OPTION;
    _choosableDirs = new LinkedList<FileFilter>();
    _normalFileFilters = new LinkedList<FileFilter>();
    _jfc = new JFileChooser();
  
    _offLimits = new HashSet<File>();
    File[] shellRoots = (File[])ShellFolder.get("fileChooserComboBoxFolders");
    for(File f : shellRoots) {
      _offLimits.add(f);
    }
    
    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());
    
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.SOUTHWEST;
    _topComponentPanel = new JPanel(layout);
    _topComponentPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
    _topLabel = new JLabel(_topLabelText);
    layout.setConstraints(_topLabel, c);
    _topComponentPanel.add(_topLabel);
        
    _northPanel = new JPanel(new BorderLayout());
    _northPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
    _northPanel.add(_topComponentPanel, BorderLayout.WEST);
//    _northPanel.add(_newButtonPanel, BorderLayout.EAST);
    cp.add(_northPanel, BorderLayout.NORTH);
    
    _scroller = new JScrollPane();
    Border innerBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
    Border outerBorder = BorderFactory.createEmptyBorder(0,10,0,10);
    _scroller.setBorder(BorderFactory.createCompoundBorder(outerBorder,innerBorder));
    JPanel spanel = new JPanel(new BorderLayout(5,5));
    spanel.add(_scroller);
    cp.add(spanel, BorderLayout.CENTER);
    
    _accessoryPanel = new JPanel(new BorderLayout());
    _accessoryPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
    
    _approveButton = new JButton(_approveText);
    _approveButton.setEnabled(false);
    _approveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // If the button was clicked when none were selected, it's an error.
        // The error option is default and doesn't need to be set.
        _tree.cancelEditing();
        if (_tree.getSelectionCount() > 0) {
          _finalResult = APPROVE_OPTION;
        }
        DirectoryChooser.this.setVisible(false);
      }
    });
    getRootPane().setDefaultButton(_approveButton);
    
    _cancelAction = new AbstractAction(_cancelText) {
      public void actionPerformed(ActionEvent e) {
        _finalResult = CANCEL_OPTION;
        _tree.cancelEditing();
        DirectoryChooser.this.setVisible(false);
      }
    };
    _cancelButton = new JButton(_cancelAction);
    String key = "dc_cancel";
    getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), key);
    getRootPane().getActionMap().put(key, _cancelAction);
    
    JPanel _okCancelPanel = new JPanel(new FlowLayout());
    _okCancelPanel.add(_approveButton);
    _okCancelPanel.add(_cancelButton);
    
    _newFolderButton = new JButton("Make New Folder"/**,_newFolderIcon**/);
    _newFolderButton.setEnabled(false);
    _newFolderButton.setMargin(new Insets(2,2,2,2));
    _newFolderButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        launchCreateNewDirectory();
      }
    });
    
    _newButtonPanel = new JPanel();
    _newButtonPanel.add(_newFolderButton);
    _newButtonPanel.setVisible(false);
    
    _buttonPanel = new JPanel(new BorderLayout());
    _buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    _buttonPanel.add(_okCancelPanel, BorderLayout.EAST);
    _buttonPanel.add(_newButtonPanel, BorderLayout.WEST);
    
    
    _southPanel = new JPanel(new BorderLayout());
    _southPanel.add(_accessoryPanel, BorderLayout.CENTER);
    _southPanel.add(_buttonPanel, BorderLayout.SOUTH);
    cp.add(_southPanel, BorderLayout.SOUTH);
    
    // This sets the root file and generates the tree
    setRootFile(_rootFile);
    
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        _finalResult = CANCEL_OPTION;
      }
    });
    
    _treePopup = new JPopupMenu();
    
    _collapseItem = new JMenuItem("Collapse");
    _treePopup.add(_collapseItem);
    _collapseItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _tree.collapsePath(_tree.getSelectionPath());
      }
    });
    _expandItem = new JMenuItem("Expand");
    _treePopup.add(_expandItem);
    _expandItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _tree.expandPath(_tree.getSelectionPath());
      }
    });
    
    _treePopup.addSeparator();
    _popSep = (JPopupMenu.Separator)_treePopup.getComponent(2);
    
    _renameItem = new JMenuItem("Rename");
    _treePopup.add(_renameItem);
    _renameItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _tree.startEditingAtPath(_tree.getSelectionPath());
      }
    });
    _deleteItem = new JMenuItem("Delete");
    _treePopup.add(_deleteItem);
    _deleteItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {    
        tryToDeletePath(_tree.getSelectionPath());
      }
    });
    _newFolderItem = new JMenuItem("New Folder");
    _treePopup.add(_newFolderItem);
    _newFolderItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        launchCreateNewDirectory(_tree.getSelectionPath());
      }
    });
        
    setEditable(false);
    setLocationByPlatform(true);
    setSize(330, 400);
  }
  
  protected CellEditorListener _cellEditorListener = new CellEditorListener() {
    public void editingCanceled(ChangeEvent e) {
      CustomCellEditor cce = (CustomCellEditor)e.getSource();
      TreePath tp =  cce.getCurrentPath();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)tp.getLastPathComponent();
      FileDisplay fd = (FileDisplay)node.getUserObject();
      
      if (fd.isNew()) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
        node.removeFromParent();
        ((DefaultTreeModel)_tree.getModel()).nodeStructureChanged(parent);
      }
    }
    public void editingStopped(ChangeEvent e) {
      CustomCellEditor cce = (CustomCellEditor)e.getSource();
      TreePath tp =  cce.getCurrentPath();
      
      if (_tree.getRowForPath(tp) < 0) return;  // was previously removed
      
      renameFileForPath(cce.getFileBeforeEdit(), tp);
    }
  };
  
  /**
   * Creates a new tree and registers all necessary listeners,
   * renderers, and editors with it so it behaves like a directory
   * tree.
   */
  protected void generateDirTree() {
    _root = makeFileNode(_rootFile);
    _tree = new CustomJTree(_root);
    
    _expansionListener = new TreeExpansionListener() {
      public void  treeCollapsed(TreeExpansionEvent event) {
        // do nothing for now
      }
      public void  treeExpanded(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        ensureHasChildren(node);
      }
    };
    
    // dissable the accept button when no directories are selected
    _tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        boolean enable = _tree.getSelectionCount() > 0;
        boolean canChoose = enable;
        File f = getFileForTreePath(_tree.getSelectionPath());
        for (FileFilter filter : _choosableDirs) {
          canChoose &= (f != null && filter.accept(f));
        }
        _approveButton.setEnabled(canChoose);
        boolean canSubDir = enable && f.isDirectory() && f.canWrite();
        _newFolderButton.setEnabled(canSubDir);
      }
    });
    
    _tree.addTreeExpansionListener(_expansionListener);
    CustomTreeCellRenderer _cellRenderer = new CustomTreeCellRenderer();
    _tree.setCellRenderer(_cellRenderer);
    
    // This should be optional.
    FileTextField textField = new FileTextField();
    CustomCellEditor cce =  new CustomCellEditor(textField);
    DefaultTreeCellEditor _cellEditor = new CustomTreeCellEditor(_tree, _cellRenderer,cce);
    
    cce.addCellEditorListener(_cellEditorListener);
    _tree.setCellEditor(_cellEditor);
    _tree.setEditable(_isEditable);
    
    if (_allowMultiple) {
      _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }
    else {
      _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); 
    }
    
    // Context menu setup
    _tree.addMouseListener(new RightClickMouseAdapter() {
      protected void _popupAction(MouseEvent e) { 
        if (e.isPopupTrigger()) {
          TreePath tp = _tree.getPathForLocation(e.getX(), e.getY());
          if (tp == null) return;
          
          boolean isExp = _tree.isExpanded(tp);
          _collapseItem.setVisible(isExp);
          _expandItem.setVisible(!isExp);
          boolean canSubDir = false;
          boolean canAlter = false;
          try {
            File f = getFileForTreePath(tp);
            boolean canWrite = f.canWrite();
            canSubDir = canWrite && f.isDirectory();
            canAlter = canWrite && !_offLimits.contains(f);
          } catch(IllegalArgumentException iae) { }
          _renameItem.setEnabled(canAlter);
          _deleteItem.setEnabled(canAlter);
          _newFolderItem.setEnabled(canSubDir);
          _newFolderButton.setEnabled(canSubDir);
          _tree.setSelectionPath(tp);
          _treePopup.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    });
    
    _tree.collapseRow(0);
    _tree.expandRow(0);
    
    _scroller.setViewportView(_tree);
    
  }
  
  ////////////////////// PUBLIC METHODS //////////////////////
  
  public void addChoosableFileFilter(FileFilter filter) {
    if (filter != null) _choosableDirs.add(filter);
  }
  
  public void removeChoosableFileFilter(FileFilter filter) {
    if (filter != null) _choosableDirs.remove(filter);
  }
  
  public void clearChoosableFileFilters() {
    _choosableDirs.clear();
  }
  
  /**
   * Adds a file filter for non-directory files.  These 
   * filters do not apply to directories and thus would not
   * have an effect if the chooser was not set to show files
   * as well as directories.
   */
  public void addFileFilter(FileFilter filter) {
    if (filter != null) _normalFileFilters.add(filter);
  }
  
  /**
   * Removes a file filter for non-directory files
   */
  public void removeFileFilter(FileFilter filter) {
    if (filter != null) _normalFileFilters.remove(filter);
  }
  
  /**
   * Clears all non-directory file filters
   */
  public void clearFileFilters() {
    _normalFileFilters.clear();
  }
  
  /**
   * returns the directory at the root of the tree
   * @return the file denoting the root directory of the directory tree
   */
  public File getRootFile() {
    return _rootFile;
  }
  
  /**
   * Sets the selection mode in the tree.  If set to true then multiple directories
   * may be selected.  Otherwise, only single directories may be selected
   * @param allow Whether to allow multiple selection
   */
  public void setAllowMultipleSelection(boolean allow) {
    _allowMultiple = allow;
    if (_allowMultiple) {
      _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }
    else {
      _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); 
    }  
  }
  
  /**
   * Sets the root directory of the tree. No nodes below the root are selectable since
   * they are not ever displayed in the tree at all
   * @param root the directory to set the root at
   */
  public void setRootFile(File root) {
    if (root == null) {
      File[] roots = (File[])ShellFolder.get("fileChooserComboBoxFolders");
      if (roots != null && roots.length > 0) {
        _rootFile = roots[0];
      }
    }
    else {
      if (root.exists()) {
        _rootFile = formatFile(root);
      }
      else {
        throw new IllegalArgumentException("The proposed root does not exist");
      }
    }
    generateDirTree();
  }
  
  /**
   * Sets whether to show hidden directories in the tree
   */
  public void setShowHiddenDirectories(boolean show) {
    if (_showHidden != show) {
      generateDirTree();
      _showHidden = show;
    }
  }
  
  public void setShowFiles(boolean show) {
    if (_showFiles != show) {
      generateDirTree();
      _showFiles = show;
    }
  }
  
  /**
   * Sets whether to allow the creation of new directories and
   * the renaming of old ones.
   */
  public void setEditable(boolean editable) {
    _isEditable = editable;
    _tree.setEditable(editable);
    
    _newButtonPanel.setVisible(editable);
    _popSep.setVisible(editable); // separator
    _newFolderItem.setVisible(editable);
    _deleteItem.setVisible(editable);
    _renameItem.setVisible(editable);
  }
  
  /**
   * Set which directory in the tree is to be selected
   * @param dir the directory to select
   * @return true if the directory was successfully set
   */
  public boolean setSelectedDirectory(File dir) {
    if (dir == null) return false;
    
    LinkedList<File> path = new LinkedList<File>();
    File tmp = formatFile(dir);
    do {
      path.addFirst(tmp);
    } while ((tmp = tmp.getParentFile()) != null);
    
    HashSet<File> rootSet = new HashSet<File>();
    tmp = _rootFile;
    do {
      rootSet.add(tmp);
    } while ((tmp = tmp.getParentFile()) != null);
    
    // remove the root from the path
    ListIterator<File> it = path.listIterator();
    while (it.hasNext()) {
      tmp = it.next();
      if (rootSet.contains(tmp)) {
        it.remove();
      }
    }
    
    DefaultMutableTreeNode currNode = _root;
    for (File currFile : path) {
      DefaultMutableTreeNode n = findMatchingChild(currNode, currFile);
      if (n == null) {
        ensureHasChildren(currNode);
      }
      n = findMatchingChild(currNode, currFile); // search again
      if (n == null) {
        return false; // not found
      }
      else {
        currNode = n;
      }
    }
    TreePath tp = new TreePath(currNode.getPath());
    _tree.setSelectionPath(tp);
    // expand the tree bounds out so it doesn't
    // get placed on the bottom of the tree.
    Rectangle bounds = _tree.getPathBounds(tp);
    int x = 0;
    int y = bounds.y - 100;
    int w = bounds.width + bounds.x;
    int h = bounds.height + 200;
    Rectangle scrollBounds = new Rectangle(x,y,w,h);
    _tree.makeVisible(tp);
    _tree.scrollRectToVisible(scrollBounds);
    _tree.repaint();
    return true;
  }
  
  /**
   * Sets the text shown in the accept button on the bottom.  The default
   * is "OK"
   * @param the text to show
   */
  public void setApproveButtonText(String text) {
    _approveText = text;
    _approveButton.setText(text);
  }
  
  /**
   * Sets the text of the label at the top of the dialog box.  To change
   * the entire component that sits at the top, use the method
   * <code>setTopComponent</code>
   * @param msg the message to show above the directory tree
   */
  public void setTopMessage(String msg) {
    _topLabelText = msg;
    _topLabel.setText(msg);
  }
  
  /**
   * Sets the component to display above the directory tree.
   * If the given component is null, the original label is replaced.
   * @param comp the component to place there
   */
  public void setTopComponent(JComponent comp) {
    _topComponentPanel.removeAll();
    if (comp == null) {
      _topComponentPanel.add(_topLabel);
    }
    else {
      _topComponentPanel.add(comp);
    }
  }
  
  /**
   * Sets which component should occupy the space between the buttons and
   * the directory tree.
   * @param comp The component to add below the directory tree
   */
  public void setAccessory(JComponent comp) {
    if (_accessory != null) {
      _accessoryPanel.remove(_accessory);
    }
    _accessory = comp;
    _accessoryPanel.add(comp, BorderLayout.CENTER);
  }
  
  public JComponent getAccessory() {
    return _accessory;
  }
  
  /**
   * Shows the dialog and returns the result (APPROVE, CANCELED, or ERROR).
   * To get the directory/directories selected, call <code>getSelectedDirectories</code>
   * @param initialSelection the directory to select at first.
   * @return the state in which the dialog exits (APPROVE, CANCELED, or ERROR)
   */
  public int showDialog(File initialSelection) {
    if (initialSelection != null) {
      generateDirTree();
      setSelectedDirectory(initialSelection);
    }
    boolean enable = _tree.getSelectionCount() > 0;
    _approveButton.setEnabled(enable);
    _newFolderButton.setEnabled(enable);
    this.setVisible(true);
    int res = _finalResult;
    _finalResult = ERROR_OPTION;
    try {
      setAlwaysOnTop(true);
    } catch(SecurityException se) { }
    return res;
  }
  
  /**
   * Shows the dialog with the same selection as the last time the dialog was
   * shown. If this is the first time it is shown, then the root is selected.
   */
  public int showDialog() {
    return showDialog(null);
  }
  
  /**
   * returns which directories were selected in the tree
   * @return an array of files for the selected directories
   */
  public File[] getSelectedDirectories() {
    TreePath[] sels = _tree.getSelectionPaths();
    if (sels == null) {
      return new File[0];
    }
    else {
      Vector<File> v = new Vector<File>();
      for (TreePath tp : sels) {
        v.add(getFileForTreePath(tp));
      }
      return v.toArray(new File[0]);
    }
  }
  
  /**
   * returns which directory was selected in the tree
   * @return the file for the selected directory, null if none selected
   */
  public File getSelectedDirectory() {
    return getFileForTreePath(_tree.getSelectionPath());
  }
  
  /**
   * Attempts to delete the directory denoted by the given tree path.
   * It first displays confirmation dialog then tries to delete.  If the
   * delete is unsuccessful, displays a message dialog stating that fact.
   * @param tp the path in the tree whose file to delete
   */
  public void tryToDeletePath(TreePath tp) {
    String msg = "Are you sure you want to delete this directory?";
    int res = JOptionPane.showConfirmDialog(DirectoryChooser.this,
                                            msg, "Delete Directory?", 
                                            JOptionPane.YES_NO_OPTION);
    if (res != JOptionPane.YES_OPTION) return;
    
    File f = getFileForTreePath(tp);
    boolean couldDelete = false;
    try {
      couldDelete = f.delete();
    }
    catch (SecurityException e) { }
    
    if (couldDelete) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)tp.getLastPathComponent();
      TreeNode parent = node.getParent();
      node.removeFromParent();
      ((DefaultTreeModel)_tree.getModel()).nodeStructureChanged(parent);
    }
    else {
      String errMsg = 
        "The directory was unable to be deleted.\n"+
        "Directories may only be deleted if they are\n"+
        "empty and if there is sufficient access to\n"+
        "to the directory.";
      JOptionPane.showMessageDialog(DirectoryChooser.this, errMsg, "Unable to delete",
                                    JOptionPane.WARNING_MESSAGE);
    }
  }
        
  /**
   * Adds a new directory node under that of the given tree path and starts editing
   * on the node.  The node value is not set using this method.  The user must 
   * enter it in the tree gui.
   * @param tp The path under which to create the new directory
   */
  public void launchCreateNewDirectory(TreePath tp) {
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)tp.getLastPathComponent();
    File f = getFileForTreeNode(parent);
    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(FileDisplay.newFile(f));
    ensureHasChildren(parent);
    parent.insert(newNode, 0);
    ((DefaultTreeModel)_tree.getModel()).nodeStructureChanged(parent);
    TreePath newPath = new TreePath(newNode.getPath());
    _tree.startEditingAtPath(newPath);
  }
  
  /**
   * Launches the creation of a new directory under the currently selected node.
   * If no nodes are selected, nothing happens.
   */
  public void launchCreateNewDirectory() {
    TreePath tp = _tree.getSelectionPath();
    if (tp != null) {
      launchCreateNewDirectory(tp);
    }
  }
  
  //////////////////// PROTECTED UTILITY METHODS /////////////////
  
  protected File getFileForTreeNode(DefaultMutableTreeNode node) {
    if (node == null) return null;
    Object o = node.getUserObject();
    if (o instanceof FileDisplay)
      return ((FileDisplay)o).getFile();
    else
      throw new IllegalArgumentException("The tree node didn't have a file display: " + node);
  }
  
  protected File getFileForTreePath(TreePath tp) {
    if (tp == null) return null;
    Object comp = (DefaultMutableTreeNode)tp.getLastPathComponent();
    if (comp instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)comp;
      return getFileForTreeNode(node);
    }
    else {
      throw new IllegalArgumentException("The tree path does not yeild a mutable tree node: " + tp);
    }
  }
  
  /**
   * makes the format have the same parent directories as are shown in the tree
   */
  protected File formatFile(File f) {
    try {
      return ShellFolder.getShellFolder(f);
    }
    catch (FileNotFoundException fnfe) {
      try {
        return f.getCanonicalFile();
      }
      catch (IOException ioe) {
        return f.getAbsoluteFile();
      }
    }
  }
  
  /**
   * @param node the node whose children to search
   * @param theFile the file to search for
   * @return the child that has the given file as its user data
   */
  protected DefaultMutableTreeNode findMatchingChild(DefaultMutableTreeNode node, File theFile) {
    Enumeration e = node.children();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
      try {
        File f = getFileForTreeNode(child);
        if (f.equals(theFile)) {
          return child;
        }
      } catch(IllegalArgumentException iae) { } // ignore non-file nodes
    }
    return null; // not found
  }
  
  /**
   * If the node has exactly one EmptyTreeNode (meaning its children have not been
   * looked up yet), then this method adds the correct children to the node according
   * to its stored file.
   * @param node the node to set up
   */
  protected void  ensureHasChildren(DefaultMutableTreeNode node) {
    if (node.getChildCount() == 1 && node.getChildAt(0) instanceof EmptyTreeNode) {
      
      File parentFile = getFileForTreeNode(node);
      node.removeAllChildren(); // get rid of dummy node
      
      File[] childFiles = parentFile.listFiles();
      if (childFiles != null) {
        Arrays.sort(childFiles, _fileComparator);
        
        for (File f : childFiles) {
          if ((f.isDirectory() || (_showFiles && allowFile(f))) && 
              (_showHidden || !f.isHidden())) {
            DefaultMutableTreeNode n = makeFileNode(f);
            node.add(n);
            ((DefaultTreeModel)_tree.getModel()).nodeStructureChanged(node);
          }
        }
      }
      ((DefaultTreeModel)_tree.getModel()).nodeStructureChanged(node);
    }
  }
  
  protected boolean allowFile(File f) {
    try{
      for (FileFilter ff : _normalFileFilters) {
        if (!ff.accept(f)) return false;
      }
      return true;
    }
    catch(Exception e) {
      return false;
    }
  }
  
  protected Comparator<File> _fileComparator = new Comparator<File>() {
    public int compare(File f1, File f2) {
      boolean d1 = f1.isDirectory();
      boolean d2 = f2.isDirectory();
      if (!(d1 ^ d2))
        return f1.getName().compareToIgnoreCase(f2.getName());
      else if (d1)
        return -1;
      else
        return 1;
    }
  };
  
  /**
   * Renames the file for the given node and propagates that
   * change to all its children.  If the prev is null, then this
   * creates a new directory.
   * @param prev The file that was in the node before editing
   * @param tp The path to the node that was just renamed.
   */
  protected void renameFileForPath(File prev, TreePath tp) {
    DefaultMutableTreeNode top = (DefaultMutableTreeNode) tp.getLastPathComponent();
    
    File f = getFileForTreeNode(top);
    if (prev == null) {
      try {
        f.mkdir();
      }
      catch (SecurityException se) {
        top.removeFromParent(); // undo changes if not renamable
      }
    }
    else if (prev.equals(f)) {
      return;
    }
    else if (f.equals(prev.getParentFile())) {
      top.setUserObject(new FileDisplay(prev)); // undo changes if not renamable
    }
    else {
      try {
        prev.renameTo(f);
      }
      catch (SecurityException se) {
        top.setUserObject(new FileDisplay(prev)); // undo changes if not renamable
      }
    }
    // resort into tree
    resortNode(top);
    _jfc.updateUI(); // so that the icons will paint correctly
    // propagate to children.
    updateChildFiles(top);
  }
  
  protected void updateChildFiles(DefaultMutableTreeNode parent) {
    if (parent.getChildCount() == 1 && parent.getChildAt(0) instanceof EmptyTreeNode) {
      return; // children are not generated yet
    }
    File parentFile = ((FileDisplay)parent.getUserObject()).getFile();
    Enumeration<TreeNode> e = parent.children();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
      File oldFile = getFileForTreeNode(child);
      File newFile = new File(parentFile, oldFile.getName());
      child.setUserObject(new FileDisplay(newFile));
      updateChildFiles(child);
    }
  }
  
  
  protected void resortNode(DefaultMutableTreeNode node) {
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
    node.removeFromParent();
    Enumeration<TreeNode> e = parent.children();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
      if (node.toString().compareTo(child.toString()) < 0) {
        int idx = parent.getIndex(child);
        parent.insert(node, idx);
        ((DefaultTreeModel)_tree.getModel()).nodeStructureChanged(parent);
        return;
      }
    }
    parent.add(node); // add to end if no other node should go after
    ((DefaultTreeModel)_tree.getModel()).nodeStructureChanged(parent);
  }
  
  protected DefaultMutableTreeNode makeFileNode(File f) {
    DefaultMutableTreeNode n = new DefaultMutableTreeNode(new FileDisplay(f));
    if (f.isDirectory()) n.add(new EmptyTreeNode()); // dummy node so it's not a leaf
    return n;
  }
  
  /////////////////////// INNER CLASS DECLARATIoNS /////////////////////
  
  /**
   * the cell renderer for this tree.  This is responsible for 
   * selecting the correct icon to put on each directory in the tree.
   */
  private class CustomTreeCellRenderer extends DefaultTreeCellRenderer{
    
    /**
     * returns the component for a cell
     * @param tree
     */
    public Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean sel,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus) {
      
      super.getTreeCellRendererComponent(tree, value, sel,
                                         expanded, leaf, row,
                                         hasFocus);
      
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      try {
        File f = getFileForTreeNode(node);
        Icon ico = _jfc.getIcon(f);
        setIcon(ico);
      }
      catch(IllegalArgumentException e) {
        // ignore non-file nodes
      }
      
      return this;
    }
    
  }
  
  /**
   * The only reason the <code>DefaultTreeCellEditor</code> was overridden
   * is to change the way it retrieves the icons for edit mode.  Previously,
   * the icon was retrieved by calling <code>getLeafIcon,getOpenIcon,</code> 
   * or <code>getClosedIcon</code> in the renderer.  Now the icons are retrieved 
   * by calling a method on the object that is being rendered.
   */
  private class CustomTreeCellEditor extends DefaultTreeCellEditor {
    
    public CustomTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer,
                                 TreeCellEditor editor) {
      super(tree,renderer,editor);
    }
    
    /**
     * This is the method that is responsible for setting the icon (for some reason)
     * @param value should be a DefaultMutableTreeNode holding a FileDisplay
     */
    protected void determineOffset(JTree tree, Object value,
                                   boolean isSelected, boolean expanded,
                                   boolean leaf, int row) {
      File f = null;
      try {
        f = getFileForTreeNode((DefaultMutableTreeNode)value);
      }
      catch (Exception e) { 
      }
      
      if(renderer != null) {
        if (f != null){
          editingIcon = _jfc.getIcon(f);
        }
        else
          editingIcon = renderer.getOpenIcon();
        
        if(editingIcon != null)
          offset = renderer.getIconTextGap() +
          editingIcon.getIconWidth();
        else
          offset = renderer.getIconTextGap();
      }
      else {
        editingIcon = null;
        offset = 0;
      }
    }
    
  }
  
  /**
   * This cell editor is responsible formaking sure the correct
   * data gets passed into and out of the editor component (the text field).
   * This is done in order to maintain the file structures stored in 
   * the tree nodes.
   */
  private class CustomCellEditor extends DefaultCellEditor {
    TreePath _currentPath = null;
    File _currentFile = null;
    
    public CustomCellEditor(final FileTextField textField) {
      super(textField);
      delegate = new EditorDelegate() {
        public void setValue(Object value) {
          
          if (value != null && value instanceof FileDisplay) {
            FileDisplay fd = (FileDisplay)value;
            if (fd.isNew())
              _currentFile = null;
            else 
              _currentFile = fd.getFile();
            
            textField.setFile(fd);
          }
          else {
            _currentFile = null;
            textField.noFileAvailable(value);
          }
        }
        
        public Object getCellEditorValue() {
          File f = textField.getFile();
          if (f == null)
            return textField.getText();
          else
            return new FileDisplay(f);
        }
      };
      textField.addActionListener(delegate);
    }
    
    public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                boolean isSelected,
                                                boolean expanded,
                                                boolean leaf, int row) {
      _currentPath = tree.getPathForRow(row);
      
      Object userValue = value;
      if (value instanceof DefaultMutableTreeNode) {
        userValue = ((DefaultMutableTreeNode)value).getUserObject();
      }
      delegate.setValue(userValue);
      
      return editorComponent;
    }
        
    public TreePath getCurrentPath() { return _currentPath; }
    public File getFileBeforeEdit() { return _currentFile; }
  }
  
  /**
   * This subclass is made in order to control which nodes are
   * editable and which are not.  By overriding the isPathEditable
   * method, it can ask the FileDisplay in the node whether it
   * is editable.
   */
  private class CustomJTree extends JTree {
    public CustomJTree(TreeNode node) {
      super(node);
    }
    
    public boolean isPathEditable(TreePath path) {
      if (!isEditable()) return false;
      try {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode)path.getLastPathComponent();
        FileDisplay fd = (FileDisplay)n.getUserObject();
        return fd.isNew() || fd.getFile().canWrite();
      }
      catch (Exception e) {
        return false;
      }
    }
  }
  
  /**
   * This class allows for the text field to still edit
   * a string while preserving the file that is truely being
   * edited.  This is done by saving the parent file while 
   * the text editor is given the name of the file.  when the
   * text is finished being edited, the new name is appended 
   * to the stored parent directory.
   */
  private class FileTextField extends JTextField {
    File _parent;
    
    public void setFile(FileDisplay fd) {
      if (fd.isNew()) {
        _parent = fd.getFile();
        setText(fd.toString());
      }
      else {
        File f = fd.getFile();
        _parent = f.getParentFile();
        setText(f.getName());
      }
    }
    public void setFile(File f) {
      _parent = f.getParentFile();
      setText(f.getName());
    }
    public void noFileAvailable(Object value) {
      _parent = null;
      if (value == null)
        setText("");
      else
        setText(value.toString());
    }
    public File getFile() {
      if (_parent == null)
        return null;
      else
        return new File(_parent, getText());
    }
  }
  
  /**
   * This class is a wrapper for a file whose <code>toString</code> method
   * outputs only the last element in the file path.  If it's a file, then
   * it outputs the file name without its parent directories.  If it's a 
   * directory, then it outputs the name of that directory only
   */
  static class FileDisplay {
    private File _file;
    private String _rep;
    private boolean _isNew;
    
    public FileDisplay(File f) { setFile(f); }
    
    public static FileDisplay newFile(File parent) {
      FileDisplay fd = new FileDisplay(parent);
      fd._rep = "New Folder";
      fd._isNew = true;
      return fd;
    }
    
    public void setFile(File f) {
      _isNew = false;
      _file = f;
      _rep = _jfc.getName(f);
    }
    
    public File getFile() { return _file; }
    
    public boolean isEditable() { return _file.canWrite() || _isNew; }
    
    public boolean isNew() { return _isNew; }
    
    public String toString() { return _rep; }
  }
  
  /**
   * Allows the DirectoryChooser to recongnize any of the 
   * temporary leaves put on the directories in order to 
   * facilitate the lazy directory retrieval.
   */
  static class EmptyTreeNode extends DefaultMutableTreeNode {
    public EmptyTreeNode() {
      super("[empty]");
    }
  }
  
  /////////////////////////////////////////////////////////////////
  
  public static void main(String[] args) {
    try {
      String lafName = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
      LookAndFeel laf = (LookAndFeel)Class.forName(lafName).newInstance();
      UIManager.setLookAndFeel(laf);
    }catch (Exception e) { System.err.println("unable to set windows laf"); }
    
    File dir = null;
    if (args.length > 0) {
      dir = new File(args[0]);
    }
    else {
      dir = new File("/home/jlugo");
      if (!dir.exists()) dir = null;
    }
    
    final DirectoryChooser d = new DirectoryChooser((Frame)null,dir);
    d._showFiles = true;
    final JCheckBox cb = new JCheckBox("Enable edits");
    cb.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        d.setEditable(cb.isSelected());
      }
    });
    d.setAccessory(cb);
    d.setTopComponent(new JLabel("Select a folder so open", UIManager.getIcon("OptionPane.informationIcon"), JLabel.LEFT));
    d.addChoosableFileFilter(new FileFilter() {
      public boolean accept(File f) {
        try {
          return !f.isDirectory();
        }
        catch (Exception e) {
          System.out.println(f);
          throw new RuntimeException(e);
        }
      }
      public String getDescription() { return "Only select the file whose name is foo"; }
    });
    d.addFileFilter(new FileFilter() {
      public boolean accept(File f) {
        String name = f.getName();
        int idx = name.lastIndexOf(".");
        return (name.substring(idx+1).equalsIgnoreCase("java"));
      }
      public String getDescription() { return "Only allow java files"; };
    });
    int res = d.showDialog();
    System.out.println("done with success: " + res);
    File f = d.getSelectedDirectory();
    System.out.println("directory: " + f);
    System.out.println("exists: " + ((f != null) ? f.exists() : false));
    System.out.println("Open recursive: " + ((JCheckBox)d.getAccessory()).isSelected());
    System.exit(1);
  }
}
