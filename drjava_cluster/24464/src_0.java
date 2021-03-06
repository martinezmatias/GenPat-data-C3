/* $Id$ */

package edu.rice.cs.drjava;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.JMenuItem;
import javax.swing.JComponent;

import javax.swing.text.DefaultEditorKit;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Cursor;


import java.io.File;

/** Main DrJava window.
 *  It has a menu and then a scroll pane with three components:
 *    Definitions, Output and Interactions. */
public class MainFrame extends JFrame
{
  private CompilerErrorPanel _errorPanel;
  private DefinitionsView _definitionsView;
  private OutputView _outputView;
  private InteractionsView _interactionsView;
  private JTextField _fileNameField;

  private JMenuBar _menuBar;
  private JMenu _fileMenu;
  private JMenu _editMenu;
    // status bar at bottom of window
    public static Label _status = new Label("");
	
    JButton _saveButton;
    JButton _compileButton;
    
  // Make some actions for menus
  private Action _quitAction = new AbstractAction("Quit")
  {
    public void actionPerformed(ActionEvent ae)
    {			
			boolean wantToExit = true;
			if (_definitionsView.modifiedSinceSave()) {
				wantToExit = _definitionsView.checkAbandoningChanges();
			}
			if (wantToExit) {
				System.exit(0);
			}
    }
  };

  private Action _openAction = new AbstractAction("Open")
  {
    public void actionPerformed(ActionEvent ae)
    {
      boolean opened = _definitionsView.open();
      if (opened) {
        _resetInteractions();
				_saveButton.setEnabled(false);
				_compileButton.setEnabled(true);
      }
    }
  };

  private Action _newAction = new AbstractAction("New")
  {
    public void actionPerformed(ActionEvent ae)
    {
      boolean createdNew = _definitionsView.newFile();
      if (createdNew) {
        _resetInteractions();
				_saveButton.setEnabled(false);
				_compileButton.setEnabled(true);
      }
    }
  };

    boolean saveToFile(String fileName) 
    {
	boolean result = _definitionsView.saveToFile(fileName);
	if (result) {
	    updateEnablesAfterSave();
	}
	return result;
    }

    boolean save()
    {
	boolean result = _definitionsView.save();
	if (result) {
	    updateEnablesAfterSave();
	}
	return result;
    }

    boolean saveAs() 
    {
       boolean result = _definitionsView.saveAs();
       if (result) {
	   updateEnablesAfterSave();
       }
       return result;
    }

    void updateEnablesAfterSave() 
    {
      _saveButton.setEnabled(false);
      _compileButton.setEnabled(true);
    }

  private Action _saveAction = new AbstractAction("Save")
  {
    // This doesn't seem to ever re-enable once disabled!
    /*
    public boolean isEnabled() {
      return ! _definitionsView.modifiedSinceSave();
    }
    */

    public void actionPerformed(ActionEvent ae)
    {
			if (_definitionsView.getCurrentFileName() == "")
			saveAs();
			else
			saveToFile(_definitionsView.getCurrentFileName());
    }
  };

  private Action _saveAsAction = new AbstractAction("Save as")
  {
    public void actionPerformed(ActionEvent ae)
    {
			saveAs();
    }
  };

	void compile() 
    {
			_compileButton.setEnabled(false);
			

      String filename = _definitionsView.getCurrentFileName();
			
      if (filename.length() == 0) {
        // the file has never been saved. we can only get here
        // if the file was never changed and never saved.
        return;
      }

      // Clear the output window before compilation
      _outputView.clear();
      
      File file = new File(filename);
      CompilerError[] errors = DrJava.compiler.compile(new File[] { file });
      _errorPanel.resetErrors(errors);

      if (errors.length == 0) {
        // Success doesn't print anything, so we should print something
        // to let them know it worked.
        System.out.println(file.getName() + " compiled successfully.");
        _resetInteractions();
      }
    }

  private Action _compileAction = new AbstractAction("Compile")
  {
    // This doesn't seem to ever re-enable once disabled!
    /*
    public boolean isEnabled() {
      return _definitionsView.getDocument().getLength() > 0;
    }
    */

    public void actionPerformed(ActionEvent ae)
    {
      boolean modified = _definitionsView.modifiedSinceSave();

      if (modified) {
        // file was not saved -- tell user they must save before compiling
        String msg = "The definitions must be saved before compiling. " + 
                     "Would you like to save and compile now?";
        int rc = JOptionPane.showConfirmDialog(MainFrame.this,
                                               msg,
                                               "File not saved",
                                               JOptionPane.YES_NO_OPTION);
        if (rc == JOptionPane.YES_OPTION) {
	    save();
          // Check if they cancelled the save. If they did, exit!
          if (_definitionsView.modifiedSinceSave()) {
            return;
          }
        }
        else {
          return; // user wants to do nothing
        }
      }
			hourglassOn();
      compile();
			hourglassOff();
    }
  };

  private Action _findReplaceAction = new AbstractAction("Find/Replace")
		{
			public void actionPerformed(ActionEvent ae)
			{
				_definitionsView.findReplace();
			}
		};
	
	private Action _clearOutputAction = new AbstractAction("Clear Output")
		{
			public void actionPerformed(ActionEvent ae)
			{
				_outputView.clear();
			}
		};

  private void _resetInteractions() {
    // Reset the interactions window, and add the source directory
    // of the file we just compiled to the class path.
    _interactionsView.reset();

    String filename = _definitionsView.getCurrentFileName();

    if (filename == "") {
      return; // no file, so no source path to add to classpath.
    }

    File file = new File(filename);
    String sourceDir = file.getAbsoluteFile().getParent();
    _interactionsView.addClassPath(sourceDir);
  }

	private WindowListener _windowCloseListener = new WindowListener() {
		public void windowActivated(WindowEvent ev) {}
		public void windowClosed(WindowEvent ev) {}
		public void windowClosing(WindowEvent ev) {
			boolean wantToExit = true;
			if (_definitionsView.modifiedSinceSave()) {
				wantToExit = _definitionsView.checkAbandoningChanges();
			}
			if (wantToExit) {
				System.exit(0);
			}
		}
		public void windowDeactivated(WindowEvent ev) {}
		public void windowDeiconified(WindowEvent ev) {}
		public void windowIconified(WindowEvent ev) {}

		public void windowOpened(WindowEvent ev) {
      _definitionsView.requestFocus();
    }
	};


	void installNewDocumentListener(DefinitionsDocument d) {
		d.addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) {
				_saveButton.setEnabled(true);
				_compileButton.setEnabled(false);
	    }
	    public void insertUpdate(DocumentEvent e) {
 				_saveButton.setEnabled(true);
				_compileButton.setEnabled(false);
	    }
	    public void removeUpdate(DocumentEvent e) {
				_saveButton.setEnabled(true);
				_compileButton.setEnabled(false);
	    }
		});
	}
	
	private class GlassPane extends JComponent 
	{
		public GlassPane() 
			{
				addKeyListener(new KeyAdapter() { });
				addMouseListener(new MouseAdapter() { });
				super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}
	}

	public void hourglassOn ()
		{
			getGlassPane().setVisible(true);
		}

	public void hourglassOff ()
		{
			getGlassPane().setVisible(false);
		}
	
  /** Creates the main window, and shows it. */
  public MainFrame()
  {
		//set up the hourglass cursor
		setGlassPane(new GlassPane());

		_fileNameField = new JTextField();
    _fileNameField.setEditable(false);

    _definitionsView = new DefinitionsView(this);
    _outputView = new OutputView();
    _errorPanel = new CompilerErrorPanel(_definitionsView);

    // Make the menu bar, and stub file and edit menus
    _menuBar = new JMenuBar();
    _fileMenu = new JMenu("File");
    _editMenu = new JMenu("Edit");

    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.addWindowListener(_windowCloseListener);
		
    // Add items to menus
    JMenuItem tmpItem = _fileMenu.add(_newAction);
		tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
																									ActionEvent.CTRL_MASK));
		tmpItem = _fileMenu.add(_openAction);
 		tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
																									ActionEvent.CTRL_MASK));
		tmpItem = _fileMenu.add(_saveAction);
		tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
																									ActionEvent.CTRL_MASK));
    tmpItem = _fileMenu.add(_saveAsAction);
		tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
																									ActionEvent.CTRL_MASK));
    _fileMenu.addSeparator();
    tmpItem = _fileMenu.add(_compileAction);
		tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B,
																									ActionEvent.CTRL_MASK));
    _fileMenu.addSeparator();
    tmpItem = _fileMenu.add(_quitAction);
		tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
																									ActionEvent.CTRL_MASK));

    Action cutAction = new DefaultEditorKit.CutAction();
    cutAction.putValue(Action.NAME, "Cut");
    Action copyAction = new DefaultEditorKit.CopyAction();
    copyAction.putValue(Action.NAME, "Copy");
    Action pasteAction = new DefaultEditorKit.PasteAction();
    pasteAction.putValue(Action.NAME, "Paste");

		/*The undo/redo menus and key action
    //tmpItem = _editMenu.add(_definitionsView.getUndoAction());
		//tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
		//																							ActionEvent.CTRL_MASK));		
    //tmpItem = _editMenu.add(_definitionsView.getRedoAction());
		//tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
		//																							ActionEvent.CTRL_MASK));

		*/
		_editMenu.addSeparator();
		_editMenu.add(cutAction);
		_editMenu.add(copyAction);
		_editMenu.add(pasteAction);
		_editMenu.addSeparator();
		tmpItem = _editMenu.add(_findReplaceAction);
		tmpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
																									ActionEvent.CTRL_MASK));
		_editMenu.add(_clearOutputAction);





    // Add the menus to the menu bar
    _menuBar.add(_fileMenu);
    _menuBar.add(_editMenu);
    
    // Menu bars can actually hold anything!
    _menuBar.add(_fileNameField);

    // Add buttons.
    _saveButton = new JButton(_saveAction); 
    _saveButton.setEnabled(false);
    _menuBar.add(_saveButton);

    _compileButton = new JButton(_compileAction);
    _menuBar.add(_compileButton);
    _compileButton.setEnabled(false);

    setJMenuBar(_menuBar);
   
    // Make the output view the active one
    //_outputView.makeActive();
    
    _interactionsView = new InteractionsView();
    // Split2 has output view and the interactions view
		/*
			OLD WAY OF SPLITTING SCREEN!!!!!!!!!
			
    JSplitPane split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                       true,
                                       new JScrollPane(_outputView),
                                        new JScrollPane(_interactionsView));
		
    // Create split pane with defs and split2
    JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                       true,
                                       new JScrollPane(_definitionsView),
                                       split2);
		*/
		JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                       true,
                                       new JScrollPane(_definitionsView),
																			 new JScrollPane(_interactionsView));
		
    /*
		JSplitPane split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
																			 true,
																			 _errorPanel,
																			 _status);
    */


    setBounds(25, 25, 300, 500);


    getContentPane().add(_errorPanel, BorderLayout.SOUTH);
    getContentPane().add(split1, BorderLayout.CENTER);
    setSize(640, 480);

    // This is annoyingly order-dependent. Since split2 contains split1,
    // we need to get split2's divider set up first to give split1 an overall
    // size. Then we can set split1's divider. Ahh, Swing.
    // Also, according to the Swing docs, we need to set these dividers AFTER
    // we have shown the window. How annoying.
    split1.setDividerLocation(200);
    //split2.setDividerLocation(50);

    updateFileTitle("Untitled");
  }

  public void updateFileTitle(String filename)
  {
    setTitle(filename + " - DrJava");
    _fileNameField.setText(filename);
  }
		
	DefinitionsView getDefView()
		{
			return _definitionsView;
		}

	OutputView getOutView()
		{
			return _outputView;
		}
}

