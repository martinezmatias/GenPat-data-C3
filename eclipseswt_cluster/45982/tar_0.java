/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.dnd;

 
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

/**
 *
 * Class <code>DropTarget</code> defines the target object for a drag and drop transfer.
 *
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 *
 * <p>This class identifies the <code>Control</code> over which the user must position the cursor
 * in order to drop the data being transferred.  It also specifies what data types can be dropped on 
 * this control and what operations can be performed.  You may have several DropTragets in an 
 * application but there can only be a one to one mapping between a <code>Control</code> and a <code>DropTarget</code>.
 * The DropTarget can receive data from within the same application or from other applications 
 * (such as text dragged from a text editor like Word).</p>
 *
 * <code><pre>
 *	int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
 *	Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
 *	DropTarget target = new DropTarget(label, operations);
 *	target.setTransfer(types);
 * </code></pre>
 *
 * <p>The application is notified of data being dragged over this control and of when a drop occurs by 
 * implementing the interface <code>DropTargetListener</code> which uses the class 
 * <code>DropTargetEvent</code>.  The application can modify the type of drag being performed 
 * on this Control at any stage of the drag by modifying the <code>event.detail</code> field or the 
 * <code>event.currentDataType</code> field.  When the data is dropped, it is the responsibility of 
 * the application to copy this data for its own purposes.
 *
 * <code><pre>
 *	target.addDropListener (new DropTargetListener() {
 *		public void dragEnter(DropTargetEvent event) {};
 *		public void dragOver(DropTargetEvent event) {};
 *		public void dragLeave(DropTargetEvent event) {};
 *		public void dragOperationChanged(DropTargetEvent event) {};
 *		public void dropAccept(DropTargetEvent event) {}
 *		public void drop(DropTargetEvent event) {
 *			// A drop has occurred, copy over the data
 *			if (event.data == null) { // no data to copy, indicate failure in event.detail
 *				event.detail = DND.DROP_NONE;
 *				return;
 *			}
 *			label.setText ((String) event.data); // data copied to label text
 *		}
 * 	});
 * </pre></code>
 *
 * <dl>
 *	<dt><b>Styles</b></dt> <dd>DND.DROP_NONE, DND.DROP_COPY, DND.DROP_MOVE, DND.DROP_LINK</dd>
 *	<dt><b>Events</b></dt> <dd>DND.DragEnter, DND.DragLeave, DND.DragOver, DND.DragOperationChanged, 
 *                             DND.DropAccept, DND.Drop </dd>
 * </dl>
 */
public class DropTarget extends Widget {

	private Control control;
	private Listener controlListener;
	private Transfer[] transferAgents = new Transfer[0];
	private DragUnderEffect effect;
	
	// Track application selections
	private TransferData selectedDataType;
	private int selectedOperation;
	
	// workaround - There is no event for "operation changed" so track operation based on key state
	private int keyOperation = -1;
	
	// workaround - Simulate events when the mouse is not moving
	private long dragOverStart;
	private Runnable dragOverHeartbeat;
	private DNDEvent dragOverEvent;
	
	private int drag_motion_handler;
	private int drag_leave_handler;
	private int drag_data_received_handler;
	private int drag_drop_handler;
	
	private static final String DROPTARGETID = "DropTarget"; //$NON-NLS-1$
	private static final int DRAGOVER_HYSTERESIS = 50;
	
	private static Callback Drag_Motion;
	private static Callback Drag_Leave;
	private static Callback Drag_Data_Received;
	private static Callback Drag_Drop;
	
	 static {	
		Drag_Motion = new Callback(DropTarget.class, "Drag_Motion", 5); //$NON-NLS-1$
		Drag_Leave = new Callback(DropTarget.class, "Drag_Leave", 3); //$NON-NLS-1$
		Drag_Data_Received = new Callback(DropTarget.class, "Drag_Data_Received", 7); //$NON-NLS-1$
		Drag_Drop = new Callback(DropTarget.class, "Drag_Drop", 5); //$NON-NLS-1$
	}

/**
 * Creates a new <code>DropTarget</code> to allow data to be dropped on the specified 
 * <code>Control</code>.
 * Creating an instance of a DropTarget may cause system resources to be allocated 
 * depending on the platform.  It is therefore mandatory that the DropTarget instance 
 * be disposed when no longer required.
 * 
 * @param control the <code>Control</code> over which the user positions the cursor to drop the data
 * @param style the bitwise OR'ing of allowed operations; this may be a combination of any of 
 *		   DND.DROP_NONE, DND.DROP_COPY, DND.DROP_MOVE, DND.DROP_LINK
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * @exception SWTError <ul>
 *    <li>ERROR_CANNOT_INIT_DROP - unable to initiate drop target; this will occur if more than one
 *        drop target is created for a control or if the operating system will not allow the creation
 *        of the drop target</li>
 * </ul>
 * 
 * @see DropTarget#dispose
 * @see DropTarget#checkSubclass
 * @see DND#DROP_NONE
 * @see DND#DROP_COPY
 * @see DND#DROP_MOVE
 * @see DND#DROP_LINK
 */	
public DropTarget(Control control, int style) {
	super(control, checkStyle(style));
	this.control = control;
	if (Drag_Motion == null || Drag_Leave == null || Drag_Data_Received == null || Drag_Drop == null) {
		 DND.error(DND.ERROR_CANNOT_INIT_DROP);
	}
	if (control.getData(DROPTARGETID) != null) {
		DND.error(DND.ERROR_CANNOT_INIT_DROP);
	}
	control.setData(DROPTARGETID, this);
	
	byte[] buffer = Converter.wcsToMbcs(null, "drag_motion", true); //$NON-NLS-1$
	drag_motion_handler = OS.g_signal_connect(control.handle, buffer, Drag_Motion.getAddress(), 0);
	buffer = Converter.wcsToMbcs(null, "drag_leave", true); //$NON-NLS-1$
	drag_leave_handler = OS.g_signal_connect(control.handle, buffer, Drag_Leave.getAddress(), 0);
	buffer = Converter.wcsToMbcs(null, "drag_data_received", true); //$NON-NLS-1$
	drag_data_received_handler = OS.g_signal_connect(control.handle, buffer, Drag_Data_Received.getAddress(), 0);
	buffer = Converter.wcsToMbcs(null, "drag_drop", true); //$NON-NLS-1$
	drag_drop_handler = OS.g_signal_connect(control.handle, buffer, Drag_Drop.getAddress(), 0);

	// Dispose listeners	
	controlListener = new Listener(){
		public void handleEvent(Event event){
			if (!DropTarget.this.isDisposed()){
				DropTarget.this.dispose();
			}
		}
	};
	control.addListener(SWT.Dispose, controlListener);
		
	this.addListener(SWT.Dispose, new Listener(){
		public void handleEvent(Event event){
			onDispose();
		}	
	});

	// Drag under effect
	if (control instanceof Tree) {
		effect = new TreeDragUnderEffect((Tree)control);
	} else if (control instanceof Table) {
		effect = new TableDragUnderEffect((Table)control);
	} else {
		effect = new NoDragUnderEffect(control);
	}

	dragOverHeartbeat = new Runnable() {
		public void run() {
			if (DropTarget.this.control.isDisposed() || dragOverStart == 0) return;
			long time = System.currentTimeMillis();
			int delay = DRAGOVER_HYSTERESIS;
			if (time < dragOverStart) {
				delay = (int)(dragOverStart - time);
			} else {	
				int allowedOperations = dragOverEvent.operations;
				TransferData[] allowedTypes = dragOverEvent.dataTypes;
				//pass a copy of data types in to listeners in case application modifies it
				TransferData[] dataTypes = new TransferData[allowedTypes.length];
				System.arraycopy(allowedTypes, 0, dataTypes, 0, dataTypes.length);
	
				DNDEvent event = new DNDEvent();
				event.widget = dragOverEvent.widget;
				event.x = dragOverEvent.x;
				event.y = dragOverEvent.y;
				event.time = (int)time;
				event.feedback = DND.FEEDBACK_SELECT;
				event.dataTypes = dataTypes;
				event.dataType = selectedDataType;
				event.operations = dragOverEvent.operations;
				event.detail  = selectedOperation;
				
				try {
					notifyListeners(DND.DragOver, event);
				} catch (Throwable e) {
					event.dataType = null;
					event.detail  = DND.DROP_NONE;
				}
				
				effect.show(event.feedback, event.x, event.y);
				
				selectedDataType = null;
				if (event.dataType != null) {
					for (int i = 0; i < allowedTypes.length; i++) {
						if (allowedTypes[i].type == event.dataType.type) {
							selectedDataType = event.dataType;
							break;
						}
					}
				}

				selectedOperation = DND.DROP_NONE;
				if (selectedDataType != null && (event.detail & allowedOperations) != 0) {
					selectedOperation = event.detail;
				}
			}
			DropTarget.this.control.getDisplay().timerExec(delay, dragOverHeartbeat);
		}
	};
}

static int checkStyle (int style) {
	if (style == SWT.NONE) return DND.DROP_MOVE;	
	return style;
}

private static int Drag_Data_Received ( int widget, int context, int x, int y, int data, int info, int time){
	DropTarget target = FindDropTarget(widget);
	if (target == null) return 0;
	return target.drag_data_received (widget, context, x, y, data, info, time);
}

private static int Drag_Drop(int widget, int context, int x, int y, int time) {
	DropTarget target = FindDropTarget(widget);
	if (target == null) return 0;
	return target.drag_drop (widget, context, x, y, time);
}

private static int Drag_Leave ( int widget, int context, int time){
	DropTarget target = FindDropTarget(widget);
	if (target == null) return 0;
	return target.drag_leave (widget, context, time);
}

private static int Drag_Motion ( int widget, int context, int x, int y, int time){
	DropTarget target = FindDropTarget(widget);
	if (target == null) return 0;
	return target.drag_motion (widget, context, x, y, time);
}
	
private static DropTarget FindDropTarget(int handle) {
	Display display = Display.findDisplay(Thread.currentThread());
	if (display == null || display.isDisposed()) return null;
	Widget widget = display.findWidget(handle);
	if (widget == null) return null;
	return (DropTarget)widget.getData(DROPTARGETID);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when a drag and drop operation is in progress, by sending
 * it one of the messages defined in the <code>DropTargetListener</code>
 * interface.
 * 
 * <p><ul>
 * <li><code>dragEnter</code> is called when the cursor has entered the drop target boundaries
 * <li><code>dragLeave</code> is called when the cursor has left the drop target boundaries and just before
 * the drop occurs or is cancelled.
 * <li><code>dragOperationChanged</code> is called when the operation being performed has changed 
 * (usually due to the user changing the selected modifier key(s) while dragging)
 * <li><code>dragOver</code> is called when the cursor is moving over the drop target
 * <li><code>dropAccept</code> is called just before the drop is performed.  The drop target is given 
 * the chance to change the nature of the drop or veto the drop by setting the <code>event.detail</code> field
 * <li><code>drop</code> is called when the data is being dropped
 * </ul></p>
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see DropTargetListener
 * @see #removeDropListener
 * @see DropTargetEvent
 */
public void addDropListener(DropTargetListener listener) {	
	if (listener == null) DND.error (SWT.ERROR_NULL_ARGUMENT);
	DNDListener typedListener = new DNDListener (listener);
	addListener (DND.DragEnter, typedListener);
	addListener (DND.DragLeave, typedListener);
	addListener (DND.DragOver, typedListener);
	addListener (DND.DragOperationChanged, typedListener);
	addListener (DND.Drop, typedListener);
	addListener (DND.DropAccept, typedListener);	
}	

protected void checkSubclass () {
	String name = getClass().getName ();
	String validName = DropTarget.class.getName();
	if (!validName.equals(name)) {
		DND.error (SWT.ERROR_INVALID_SUBCLASS);
	}
}

private int drag_data_received ( int widget, int context, int x, int y, int data, int info, int time){
	DNDEvent event = new DNDEvent();
	if (data == 0 || !setEventData(context, x, y, time, event)) {
		keyOperation = -1;
		return 0;
	}
	keyOperation = -1;
	
	int allowedOperations = event.operations;
	
	// Get data in a Java format	
	Object object = null;
	TransferData transferData = new TransferData();
	GtkSelectionData selectionData = new GtkSelectionData(); 
	OS.memmove(selectionData, data, GtkSelectionData.sizeof);
	if (selectionData.data != 0) {
		transferData.type = selectionData.type;
		transferData.length = selectionData.length;
		transferData.pValue = selectionData.data;
		transferData.format = selectionData.format;
		for (int i = 0; i < transferAgents.length; i++) {
			if (transferAgents[i].isSupportedType(transferData)) {
				object = transferAgents[i].nativeToJava(transferData);
				break;
			}
		}
	}
	if (object == null) {
		selectedOperation = DND.DROP_NONE;
	}
	
	event.detail = selectedOperation;
	event.dataType = transferData;
	event.data = object;
	try {
		notifyListeners(DND.Drop, event);
		selectedOperation = DND.DROP_NONE;
		if ((allowedOperations & event.detail) == event.detail) {
			selectedOperation = event.detail;
		}
	} catch (Throwable e) {
		selectedOperation = DND.DROP_NONE;
	}
	
	//notify source of action taken
	OS.gtk_drag_finish(context, selectedOperation != DND.DROP_NONE, selectedOperation== DND.DROP_MOVE, time); 			
	return 1;	
}

private int drag_drop(int widget, int context, int x, int y, int time) {
	DNDEvent event = new DNDEvent();
	if (!setEventData(context, x, y, time, event)) {
		keyOperation = -1;
		return 0;
	}
	keyOperation = -1;
	
	int allowedOperations = event.operations;
	TransferData[] allowedDataTypes = new TransferData[event.dataTypes.length];
	System.arraycopy(event.dataTypes, 0, allowedDataTypes, 0, allowedDataTypes.length);
	
	event.dataType = selectedDataType;
	event.detail = selectedOperation;
	try {
		notifyListeners(DND.DropAccept,event);
	} catch (Throwable err) {
		event.detail = DND.DROP_NONE;
		event.dataType = null;
	}

	selectedDataType = null;
	if (event.dataType != null) {
		for (int i = 0; i < allowedDataTypes.length; i++) {
			if (allowedDataTypes[i].type == event.dataType.type) {
				selectedDataType = allowedDataTypes[i];
				break;
			}
		}
	}
	
	selectedOperation = DND.DROP_NONE;
	if (selectedDataType != null && ((event.detail & allowedOperations) == event.detail)) {
		selectedOperation = event.detail;
	}
	
	if (selectedOperation == DND.DROP_NONE) {
		// this was not a successful drop
		return 0;
	}

	// ask drag source for dropped data
	OS.gtk_drag_get_data(widget, context, selectedDataType.type, time);
	return 1;
}

private int drag_leave ( int widget, int context, int time){
	updateDragOverHover(0, null);
	effect.show(DND.FEEDBACK_NONE, 0, 0);
	
	if (keyOperation == -1) return 1;
	keyOperation = -1;
	
	DNDEvent event = new DNDEvent();
	event.widget = this;
	event.time = (int)(long)time;
	event.detail = DND.DROP_NONE;
	try {
		notifyListeners(DND.DragLeave, event);
	} catch (Throwable e) {}
	return 1;
}

private int drag_motion ( int widget, int context, int x, int y, int time){
	int oldKeyOperation = keyOperation;
	
	if (oldKeyOperation == -1) { //drag enter
		selectedDataType = null;
		selectedOperation = DND.DROP_NONE;
	}
		
	DNDEvent event = new DNDEvent();
	if (!setEventData(context, x, y, time, event)) {
		keyOperation = -1;
		OS.gdk_drag_status(context, 0, time);
		return 0;
	}
	
	int allowedOperations = event.operations;
	TransferData[] allowedDataTypes = new TransferData[event.dataTypes.length];
	System.arraycopy(event.dataTypes, 0, allowedDataTypes, 0, allowedDataTypes.length);

	if (oldKeyOperation == -1) {
		event.type = DND.DragEnter;
	} else {
		if (keyOperation == oldKeyOperation) {
			event.type = DND.DragOver;
			event.dataType = selectedDataType;
			event.detail = selectedOperation;
		} else {
			event.type = DND.DragOperationChanged;
			event.dataType = selectedDataType;
		}
	}
	
	updateDragOverHover(DRAGOVER_HYSTERESIS, event);
						
	try {
		notifyListeners(event.type, event);	
	} catch (Throwable e) {
		OS.gdk_drag_status(context, 0, time);
		return 0;
	}

	if (event.detail == DND.DROP_DEFAULT) {
		event.detail = (allowedOperations & DND.DROP_MOVE) != 0 ? DND.DROP_MOVE : DND.DROP_NONE;
	}
	
	selectedDataType = null;
	if (event.dataType != null) {
		for (int i = 0; i < allowedDataTypes.length; i++) {
			if (allowedDataTypes[i].type == event.dataType.type) {
				selectedDataType = allowedDataTypes[i];
				break;
			}
		}
	}

	selectedOperation = DND.DROP_NONE;
	if (selectedDataType != null && (allowedOperations & event.detail) != 0) {
		selectedOperation = event.detail;
	}
	
	effect.show(event.feedback, event.x, event.y);

	switch (selectedOperation) {
		case DND.DROP_NONE:
			OS.gdk_drag_status(context, 0, time);
			break;		
		case DND.DROP_COPY:
			OS.gdk_drag_status(context, OS.GDK_ACTION_COPY, time);
			break;
		case DND.DROP_MOVE:
			OS.gdk_drag_status(context, OS.GDK_ACTION_MOVE, time);
			break;
		case DND.DROP_LINK:
			OS.gdk_drag_status(context, OS.GDK_ACTION_LINK, time);
			break;
	}
	
	if (oldKeyOperation == -1) {
		dragOverHeartbeat.run();
	}
	return 1;
}

/**
 * Returns the Control which is registered for this DropTarget.  This is the control over which the 
 * user positions the cursor to drop the data.
 *
 * @return the Control which is registered for this DropTarget
 */
public Control getControl () {
	return control;
}

private int getOperationFromKeyState() {
	int[] state = new int[1];
	OS.gdk_window_get_pointer(0, null, null, state);
	boolean ctrl = (state[0] & OS.GDK_CONTROL_MASK) != 0;
	boolean shift = (state[0] & OS.GDK_SHIFT_MASK) != 0;
	if (ctrl && shift) return DND.DROP_LINK;
	if (ctrl)return DND.DROP_COPY;
	if (shift)return DND.DROP_MOVE;
	return DND.DROP_DEFAULT;
}

/**
 * Returns a list of the data types that can be transferred to this DropTarget.
 *
 * @return a list of the data types that can be transferred to this DropTarget
 */ 
public Transfer[] getTransfer() { 
	return transferAgents; 
}

public void notifyListeners (int eventType, Event event) {
	Point coordinates = new Point(event.x, event.y);
	coordinates = control.toControl(coordinates);
	if (this.control instanceof Tree) {
		Tree tree = (Tree)control;
		event.item = tree.getItem(coordinates);
		if (event.item == null) {
			Rectangle area = tree.getClientArea();
			if (area.contains(coordinates)) {
				// Scan across the width of the tree.
				for (int x1 = area.x; x1 < area.x + area.width; x1++) {
					Point pt = new Point(x1, coordinates.y);
					event.item = tree.getItem(pt);
					if (event.item != null) {
						break;
					}
				}
			}
		}
	}
	if (this.control instanceof Table) {
		Table table = (Table)control;
		event.item = table.getItem(coordinates);
		if (event.item == null) {
			Rectangle area = table.getClientArea();
			if (area.contains(coordinates)) {
				// Scan across the width of the tree.
				for (int x1 = area.x; x1 < area.x + area.width; x1++) {
					Point pt = new Point(x1, coordinates.y);
					event.item = table.getItem(pt);
					if (event.item != null) {
						break;
					}
				}
			}
		}
	}
	super.notifyListeners(eventType, event);
}

private void onDispose(){
	if (control == null) return;
	OS.g_signal_handler_disconnect(control.handle, drag_motion_handler);
	OS.g_signal_handler_disconnect(control.handle, drag_leave_handler);
	OS.g_signal_handler_disconnect(control.handle, drag_data_received_handler);
	OS.g_signal_handler_disconnect(control.handle, drag_drop_handler);
	if (transferAgents.length != 0)
		OS.gtk_drag_dest_unset(control.handle);
	transferAgents = null;
	if (controlListener != null)
		control.removeListener(SWT.Dispose, controlListener);
	control.setData(DROPTARGETID, null);
	control = null;
	controlListener = null;
}

private int opToOsOp(int operation){
	int osOperation = 0;
	if ((operation & DND.DROP_COPY) == DND.DROP_COPY)
		osOperation |= OS.GDK_ACTION_COPY;
	if ((operation & DND.DROP_MOVE) == DND.DROP_MOVE)
		osOperation |= OS.GDK_ACTION_MOVE;
	if ((operation & DND.DROP_LINK) == DND.DROP_LINK)
		osOperation |= OS.GDK_ACTION_LINK;
	return osOperation;
}

private int osOpToOp(int osOperation){
	int operation = DND.DROP_NONE;
	if ((osOperation & OS.GDK_ACTION_COPY) == OS.GDK_ACTION_COPY)
		operation |= DND.DROP_COPY;
	if ((osOperation & OS.GDK_ACTION_MOVE) == OS.GDK_ACTION_MOVE)
		operation |= DND.DROP_MOVE;
	if ((osOperation & OS.GDK_ACTION_LINK) == OS.GDK_ACTION_LINK)
		operation |= DND.DROP_LINK;
	return operation;
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when a drag and drop operation is in progress.
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see DropTargetListener
 * @see #addDropListener
 */
public void removeDropListener(DropTargetListener listener) {
	if (listener == null) DND.error (SWT.ERROR_NULL_ARGUMENT);
	removeListener (DND.DragEnter, listener);
	removeListener (DND.DragLeave, listener);
	removeListener (DND.DragOver, listener);
	removeListener (DND.DragOperationChanged, listener);
	removeListener (DND.Drop, listener);
	removeListener (DND.DropAccept, listener);
}

/**
 * Specifies the data types that can be transferred to this DropTarget.  If data is 
 * being dragged that does not match one of these types, the drop target will be notified of 
 * the drag and drop operation but the currentDataType will be null and the operation 
 * will be DND.NONE.
 *
 * @param transferAgents a list of Transfer objects which define the types of data that can be
 *						 dropped on this target
 * 
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if transferAgents is null</li>
 * </ul>
 */
public void setTransfer(Transfer[] transferAgents){
	if (transferAgents == null) DND.error(SWT.ERROR_NULL_ARGUMENT);
	
	if (this.transferAgents.length != 0) {
		OS.gtk_drag_dest_unset(control.handle);
	}
	this.transferAgents = transferAgents;
	
	GtkTargetEntry[] targets = new GtkTargetEntry[0];
	for (int i = 0; i < transferAgents.length; i++) {
		Transfer transfer = transferAgents[i];
		int[] typeIds = transfer.getTypeIds();
		String[] typeNames = transfer.getTypeNames();
		for (int j = 0; j < typeIds.length; j++) {
			GtkTargetEntry entry = new GtkTargetEntry();
			byte[] buffer = Converter.wcsToMbcs(null, typeNames[j], true);
			entry.target = OS.g_malloc(buffer.length);
			OS.memmove(entry.target, buffer, buffer.length);						
			entry.info = typeIds[j];
			GtkTargetEntry[] newTargets = new GtkTargetEntry[targets.length + 1];
			System.arraycopy(targets, 0, newTargets, 0, targets.length);
			newTargets[targets.length] = entry;
			targets = newTargets;
		}	
	}
	
	int pTargets = OS.g_malloc(targets.length * GtkTargetEntry.sizeof);
	for (int i = 0; i < targets.length; i++) {
		OS.memmove(pTargets + i*GtkTargetEntry.sizeof, targets[i], GtkTargetEntry.sizeof);		
	}			
	
	int actions = opToOsOp(getStyle());
	OS.gtk_drag_dest_set(control.handle, 0, pTargets, targets.length, actions);
	
	for (int i = 0; i < targets.length; i++) {
		OS.g_free(targets[i].target);
	}
}

private boolean setEventData(int context, int x, int y, int time, DNDEvent event) {
	if (context == 0) return false;
	GdkDragContext dragContext = new GdkDragContext();
	OS.memmove(dragContext, context, GdkDragContext.sizeof);
	if (dragContext.targets == 0) return false;
	
	// get allowed operations
	int style = getStyle();
	int operations = osOpToOp(dragContext.actions) & style;
	if (operations == DND.DROP_NONE) return false;
	
	// get current operation
	int operation = getOperationFromKeyState();
	keyOperation = operation;
	if (operation == DND.DROP_DEFAULT) {
		if ((style & DND.DROP_DEFAULT) == 0) {
			operation = (operations & DND.DROP_MOVE) != 0 ? DND.DROP_MOVE : DND.DROP_NONE;
		}
	} else {
		if ((operation & operations) == 0) operation = DND.DROP_NONE;
	}

	// Get allowed transfer types
	int length = OS.g_list_length(dragContext.targets);
	TransferData[] dataTypes = new TransferData[0];
	for (int i = 0; i < length; i++) {
		int pData = OS.g_list_nth(dragContext.targets, i);
		GtkTargetPair gtkTargetPair = new GtkTargetPair();
		OS.memmove(gtkTargetPair, pData, GtkTargetPair.sizeof);
		TransferData data = new TransferData();
		data.type = gtkTargetPair.target;
		for (int j = 0; j < transferAgents.length; j++) {
			if (transferAgents[j].isSupportedType(data)) {
				TransferData[] newDataTypes = new TransferData[dataTypes.length + 1];
				System.arraycopy(dataTypes, 0, newDataTypes, 0, dataTypes.length);
				newDataTypes[dataTypes.length] = data;
				dataTypes = newDataTypes;	
				break;
			}
		}
	}
	if (dataTypes.length == 0) return false;

	Point coordinates = control.toDisplay( new Point( x, y) );
	
	event.widget = this;
	event.x = coordinates.x;
	event.y = coordinates.y;
	event.time = (int)(long)time;
	event.feedback = DND.FEEDBACK_SELECT;
	event.dataTypes = dataTypes;
	event.dataType = dataTypes[0];
	event.operations = operations;
	event.detail = operation;
	return true;
}

private void updateDragOverHover(long delay, DNDEvent event) {
	if (delay == 0) {
		dragOverStart = 0;
		dragOverEvent = null;
		return;
	}
	dragOverStart = System.currentTimeMillis() + delay;
	if (dragOverEvent == null) dragOverEvent = new DNDEvent();
	dragOverEvent.x = event.x;
	dragOverEvent.y = event.y;
	TransferData[] dataTypes = new TransferData[ event.dataTypes.length];
	System.arraycopy( event.dataTypes, 0, dataTypes, 0, dataTypes.length);
	dragOverEvent.dataTypes  = dataTypes;
	dragOverEvent.operations = event.operations;
}

}
