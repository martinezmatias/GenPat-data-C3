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
package org.eclipse.swt.widgets;


import org.eclipse.swt.internal.carbon.OS;
import org.eclipse.swt.internal.carbon.Rect;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;

/**
 * Instances of the receiver represent a selectable user interface object
 * that allows the user to drag a rubber banded outline of the sash within
 * the parent control.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>HORIZONTAL, VERTICAL</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles HORIZONTAL and VERTICAL may be specified.
 * </p><p>
 * IMPORTANT: This class is intended to be subclassed <em>only</em>
 * within the SWT implementation.
 * </p>
 */
public class Sash extends Control {
	Cursor sizeCursor;

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#HORIZONTAL
 * @see SWT#VERTICAL
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Sash (Composite parent, int style) {
	super (parent, checkStyle (style));
	int cursorStyle = (style & SWT.VERTICAL) != 0 ? SWT.CURSOR_SIZEWE : SWT.CURSOR_SIZENS;
	sizeCursor = new Cursor (display, cursorStyle);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the control is selected, by sending
 * it one of the messages defined in the <code>SelectionListener</code>
 * interface.
 * <p>
 * When <code>widgetSelected</code> is called, the x, y, width, and height fields of the event object are valid.
 * If the reciever is being dragged, the event object detail field contains the value <code>SWT.DRAG</code>.
 * <code>widgetDefaultSelected</code> is not called.
 * </p>
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
 * @see SelectionListener
 * @see #removeSelectionListener
 * @see SelectionEvent
 */
public void addSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Selection,typedListener);
	addListener(SWT.DefaultSelection,typedListener);
}

static int checkStyle (int style) {
	return checkBits (style, SWT.HORIZONTAL, SWT.VERTICAL, 0, 0, 0, 0);
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget();
	int width = 0, height = 0;
	if ((style & SWT.HORIZONTAL) != 0) {
		width += DEFAULT_WIDTH;  height += 5;
	} else {
		width += 5; height += DEFAULT_HEIGHT;
	}
	if (wHint != SWT.DEFAULT) width = wHint;
	if (hHint != SWT.DEFAULT) height = hHint;
	return new Point (width, height);
}

void createHandle () {
	int features = OS.kControlSupportsEmbedding | OS.kControlSupportsFocus | OS.kControlGetsFocusOnClick;
	int [] outControl = new int [1];
	int window = OS.GetControlOwner (parent.handle);
	OS.CreateUserPaneControl (window, null, features, outControl);
	if (outControl [0] == 0) error (SWT.ERROR_NO_HANDLES);
	handle = outControl [0];
}

void drawBackground (int control) {
	drawBackground (control, background);
}

int kEventControlClick (int nextHandler, int theEvent, int userData) {
	int result = super.kEventControlClick (nextHandler, theEvent, userData);
	if (result == OS.noErr) return result;
	if (!isEnabled ()) return OS.noErr;
	return result;
}

int kEventControlSetCursor (int nextHandler, int theEvent, int userData) {
	int result = super.kEventControlSetCursor (nextHandler, theEvent, userData);
	if (result == OS.noErr) return result;
	display.setCursor (sizeCursor.handle);
	return OS.noErr;
}

int kEventMouseDown (int nextHandler, int theEvent, int userData) {
	int result = super.kEventMouseDown (nextHandler, theEvent, userData);
	if (result == OS.noErr) return result;
	
	Rect rect = new Rect ();
	OS.GetControlBounds (handle, rect);
	int startX = rect.left;
	int startY = rect.top;			
	int width = rect.right - rect.left;
	int height = rect.bottom - rect.top;
	OS.GetControlBounds (parent.handle, rect);
	Event event = new Event ();
	event.x = startX -= rect.left;
	event.y = startY -= rect.top;
	event.width = width;
	event.height = height;
	sendEvent (SWT.Selection, event);
	if (isDisposed ()) return result;
	if (!event.doit) return result;
	
	int sizeof = org.eclipse.swt.internal.carbon.Point.sizeof;
	org.eclipse.swt.internal.carbon.Point pt = new org.eclipse.swt.internal.carbon.Point ();
	OS.GetEventParameter (theEvent, OS.kEventParamMouseLocation, OS.typeQDPoint, null, sizeof, null, pt);
	int window = OS.GetControlOwner (handle);
	OS.GetWindowBounds (window, (short) OS.kWindowContentRgn, rect);
	int offsetX = pt.h - rect.left;
	int offsetY = pt.v - rect.top;
	OS.GetControlBounds (handle, rect);
	offsetX -= rect.left;
	offsetY -= rect.top;
	
	int port = OS.GetWindowPort (window);
	int [] outModifiers = new int [1];
	short [] outResult = new short [1];
	org.eclipse.swt.internal.carbon.Point outPt = new org.eclipse.swt.internal.carbon.Point ();
	while (outResult [0] != OS.kMouseTrackingMouseUp) {
		OS.TrackMouseLocationWithOptions (port, 0, OS.kEventDurationForever, outPt, outModifiers, outResult);
		switch (outResult [0]) {
			case OS.kMouseTrackingMouseDown:
			case OS.kMouseTrackingMouseUp:
			case OS.kMouseTrackingMouseDragged: {
				OS.GetControlBounds (parent.handle, rect);
				int x = outPt.h - rect.left;
				int y = outPt.v - rect.top;				
				int newX = startX, newY = startY;
				if ((style & SWT.VERTICAL) != 0) {
					int clientWidth = rect.right - rect.left;
					newX = Math.min (Math.max (0, x - offsetX), clientWidth - width);
				} else {
					int clientHeight = rect.bottom - rect.top;
					newY = Math.min (Math.max (0, y - offsetY), clientHeight - height);
				}
				event = new Event ();
				event.x = newX;
				event.y = newY;
				event.width = width;
				event.height = height;
				event.detail = 0; //outResult [0] == OS.kMouseTrackingMouseDragged ? SWT.DRAG : 0;
				sendEvent (SWT.Selection, event);
				if (isDisposed ()) return result;
				if (event.doit) {
					setBounds (newX, newY, width, height);
					Shell shell = parent.getShell ();
					shell.update (true);
				}
				break;
			}
			default:
				outResult [0] = OS.kMouseTrackingMouseUp;
				break;
		}
	}
	return result;
}

void releaseWidget () {
	super.releaseWidget ();
	if (sizeCursor != null) sizeCursor.dispose ();
	sizeCursor = null;
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is selected.
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
 * @see SelectionListener
 * @see #addSelectionListener
 */
public void removeSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook(SWT.Selection, listener);
	eventTable.unhook(SWT.DefaultSelection,listener);
}

int traversalCode (int key, int theEvent) {
	return 0;
}

}
