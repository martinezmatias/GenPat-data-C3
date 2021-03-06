package org.eclipse.swt.custom;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
*
* A TableTreeEditor is a manager for a Control that appears above a cell in a TableTree
* and tracks with the moving and resizing of that cell.  It can be used to display a
* text widget above a cell in a TableTree so that the user can edit the contents of 
* that cell.  It can also be used to display a button that can launch a dialog for 
* modifying the contents of the associated cell.
*
* <p> Here is an example of using a TableTreeEditor:
* <code><pre>
* public static void main (String [] args) {
* 	Display display = new Display ();
* 	Shell shell = new Shell (display);
* 	final TableTree tableTree = new TableTree(shell, SWT.FULL_SELECTION);
* 	Table table = tableTree.getTable();
* 	table.setLinesVisible(true);
* 	TableColumn column1 = new TableColumn(table, SWT.NONE);
* 	column1.setText("column 1");
* 	TableColumn column2 = new TableColumn(table, SWT.NONE);
* 	column2.setText("column 2");
* 	for (int i = 0; i < 40; i++) {
* 		TableTreeItem item = new TableTreeItem(tableTree, SWT.NONE);
* 		item.setText(0, "table tree item"+i);
* 		item.setText(1, "value "+i);
* 	}
* 	column1.pack();
* 	column2.pack();
* 	final TableTreeEditor editor = new TableTreeEditor (tableTree);
* 	tableTree.addSelectionListener (new SelectionAdapter() {
* 		public void widgetSelected(SelectionEvent e) {
* 			// Clean up any previous editor control
* 			Control oldEditor = editor.getEditor();
* 			if (oldEditor != null)
* 				oldEditor.dispose();	
* 			// Identify the selected row
* 			TableTreeItem[] selection = tableTree.getSelection();
* 			if (selection.length == 0) return;
* 			TableTreeItem item = selection[0];
* 			// The control that will be the editor must be a child of the Table
* 			// that underlies the TableTree
* 			Text text = new Text(tableTree.getTable(), SWT.NONE);
* 			//text.moveAbove(tableTree);
* 			//The text editor must have the same size as the cell and must
* 			//not be any smaller than 50 pixels.
* 			editor.horizontalAlignment = SWT.LEFT;
* 			editor.grabHorizontal = true;
* 			//editor.minimumWidth = 50;
* 			// Open the text editor in the second column of the selected row.
* 			editor.setEditor (text, item, 1);
* 			// Assign focus to the text control
* 			text.setFocus ();
* 		}
* 	});
* 	tableTree.setBounds(10, 10, 200, 400);
* 	shell.open ();
* 	while (!shell.isDisposed ()) {
* 		if (!display.readAndDispatch ()) display.sleep ();
* 	}
* 	display.dispose ();
* }
* </pre></code>
*/
public class TableTreeEditor extends ControlEditor {

	TableTree tableTree;
	TableTreeItem item;
	int column = -1;
	Listener columnListener;
/**
* Creates a TableEditor for the specified Table.
*
* @param table the Table Control above which this editor will be displayed
*
*/
public TableTreeEditor (TableTree tableTree) {
	super(tableTree.getTable());
	this.tableTree = tableTree;
	
	columnListener = new Listener() {
		public void handleEvent(Event e) {
			resize ();
		}
	};

}
Rectangle computeBounds () {
	if (item == null || column == -1 || item.isDisposed()) return new Rectangle(0, 0, 0, 0);
	
	Rectangle cell = item.getBounds(column);
	Rectangle editorRect = new Rectangle(cell.x, cell.y, minimumWidth, cell.height);
	Rectangle area = tableTree.getClientArea();
	if (cell.x < area.x + area.width) {
		if (cell.x + cell.width > area.x + area.width) {
			cell.width = area.width - cell.x;
		}
	}
	
	if (grabHorizontal){
		editorRect.width = Math.max(cell.width, minimumWidth);
	}
	
	if (horizontalAlignment == SWT.RIGHT) {
		editorRect.x += cell.width - editorRect.width;
	} else if (horizontalAlignment == SWT.LEFT) {
		// do nothing - cell.x is the right answer
	} else { // default is CENTER
		editorRect.x += (cell.width - editorRect.width)/2;
	}
	
	return editorRect;
}
/**
 * Removes all associations between the TableEditor and the cell in the table.  The
 * Table and the editor Control are <b>not</b> disposed.
 */
public void dispose () {
	
	Table table = tableTree.getTable();
	if (this.column > -1 && this.column < table.getColumnCount()){
		TableColumn tableColumn = table.getColumn(this.column);
		tableColumn.removeListener(SWT.Resize, columnListener);
		tableColumn.removeListener(SWT.Move, columnListener);
	}

	tableTree = null;
	item = null;
	column = -1;
	
	super.dispose();
}
/**
* Returns the zero based index of the column of the cell being tracked by this editor.
*
* @return the zero based index of the column of the cell being tracked by this editor
*/
public int getColumn () {
	return column;
}
public void setColumn(int column) {
	Table table = tableTree.getTable();
	if (this.column > -1 && this.column < table.getColumnCount()){
		TableColumn tableColumn = table.getColumn(this.column);
		tableColumn.removeListener(SWT.Resize, columnListener);
		tableColumn.removeListener(SWT.Move, columnListener);
		this.column = -1;
	}

	if (column < 0  || column >= table.getColumnCount()) return;	
		
	this.column = column;
	TableColumn tableColumn = table.getColumn(this.column);
	tableColumn.addListener(SWT.Resize, columnListener);
	tableColumn.addListener(SWT.Move, columnListener);
}
/**
* Returns the TableItem for the row of the cell being tracked by this editor.
*
* @return the TableItem for the row of the cell being tracked by this editor
*/
public TableTreeItem getItem () {
	return item;
}
public void setItem (TableTreeItem item) {	
	this.item = item;
}
public void setEditor (Control editor) {
	TableTreeItem item = null;
	if (tableTree.getItemCount() > 0) {
		item = tableTree.getItems()[0];
	}
	this.setEditor(editor, item, 0);
}

/**
* Specify the Control that is to be displayed and the cell in the table that it is to be positioned above.
*
* <p>Note: The Control provided as the editor <b>must</b> be created with its parent being the Table control
* specified in the TableEditor constructor.
* 
* @param editor the Control that is displayed above the cell being edited
* @param item the TableItem for the row of the cell being tracked by this editor
* @param column the zero based index of the column of the cell being tracked by this editor
*/
public void setEditor (Control editor, TableTreeItem item, int column) {
	setItem(item);
	setColumn(column);
	super.setEditor(editor);
}
}