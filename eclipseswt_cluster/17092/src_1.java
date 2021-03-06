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


import org.eclipse.swt.*;
import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;

/**
 *  Instances of this class implement rubber banding rectangles that are
 *  drawn onto a parent <code>Composite</code> or <code>Display</code>.
 *  These rectangles can be specified to respond to mouse and key events
 *  by either moving or resizing themselves accordingly.  Trackers are
 *  typically used to represent window geometries in a lightweight manner.
 *  
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>LEFT, RIGHT, UP, DOWN, RESIZE</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Move, Resize</dd>
 * </dl>
 * <p>
 * Note: Rectangle move behavior is assumed unless RESIZE is specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 */
public class Tracker extends Widget {
	Composite parent;
	int cursor, lastCursor;
	boolean tracking, stippled;
	Rectangle [] rectangles, proportions;
	int xWindow;
	int ptrGrabResult;
	int cursorOrientation = SWT.NONE;
	final static int STEPSIZE_SMALL = 1;
	final static int STEPSIZE_LARGE = 9;

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
 * @param parent a widget which will be the parent of the new instance (cannot be null)
 * @param style the style of widget to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#LEFT
 * @see SWT#RIGHT
 * @see SWT#UP
 * @see SWT#DOWN
 * @see SWT#RESIZE
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Tracker (Composite parent, int style) {
	super (parent, checkStyle(style));
	this.parent = parent;
	xWindow = calculateWindow ();
}

/**
 * Constructs a new instance of this class given the display
 * to create it on and a style value describing its behavior
 * and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p><p>
 * Note: Currently, null can be passed in for the display argument.
 * This has the effect of creating the tracker on the currently active
 * display if there is one. If there is no current display, the 
 * tracker is created on a "default" display. <b>Passing in null as
 * the display argument is not considered to be good coding style,
 * and may not be supported in a future release of SWT.</b>
 * </p>
 *
 * @param display the display to create the tracker on
 * @param style the style of control to construct
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 * 
 * @see SWT#LEFT
 * @see SWT#RIGHT
 * @see SWT#UP
 * @see SWT#DOWN
 */
public Tracker (Display display, int style) {
	if (display == null) display = Display.getCurrent ();
	if (display == null) display = Display.getDefault ();
	if (!display.isValidThread ()) {
		error (SWT.ERROR_THREAD_INVALID_ACCESS);
	}
	this.style = checkStyle (style);
	this.display = display;
	xWindow = calculateWindow();
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the control is moved or resized, by sending
 * it one of the messages defined in the <code>ControlListener</code>
 * interface.
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
 * @see ControlListener
 * @see #removeControlListener
 */
public void addControlListener(ControlListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Move,typedListener);
}

/**
 * Stops displaying the tracker rectangles.  Note that this is not considered
 * to be a cancelation by the user.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void close () {
	checkWidget();
	tracking = false;
}

/*
 * Figure which GdkWindow we'll draw on.
 * That's normally the root X window, or the parent's GdkWindow if we have a parent.
 */
int calculateWindow() {
	int answer;
	if (parent == null) {
		answer = OS.GDK_ROOT_PARENT();
	} else {
		answer = OS.GTK_WIDGET_WINDOW(parent.paintHandle());
	} 
	if (answer==0) error(SWT.ERROR_UNSPECIFIED);
	return answer;
}

static int checkStyle (int style) {
	if ((style & (SWT.LEFT | SWT.RIGHT | SWT.UP | SWT.DOWN)) == 0) {
		style |= SWT.LEFT | SWT.RIGHT | SWT.UP | SWT.DOWN;
	}
	return style;
}

Rectangle computeBounds () {
	int xMin = rectangles [0].x;
	int yMin = rectangles [0].y;
	int xMax = rectangles [0].x + rectangles [0].width;
	int yMax = rectangles [0].y + rectangles [0].height;
	
	for (int i = 1; i < rectangles.length; i++) {
		if (rectangles [i].x < xMin) xMin = rectangles [i].x;
		if (rectangles [i].y < yMin) yMin = rectangles [i].y;
		int rectRight = rectangles [i].x + rectangles [i].width;
		if (rectRight > xMax) xMax = rectRight;		
		int rectBottom = rectangles [i].y + rectangles [i].height;
		if (rectBottom > yMax) yMax = rectBottom;
	}
	
	return new Rectangle (xMin, yMin, xMax - xMin, yMax - yMin);
}

Rectangle [] computeProportions (Rectangle [] rects) {
	Rectangle [] result = new Rectangle [rects.length];
	Rectangle bounds = computeBounds ();
	for (int i = 0; i < rects.length; i++) {
		int x = 0, y = 0, width = 0, height = 0;
		if (bounds.width != 0) {
			x = (rects [i].x - bounds.x) * 100 / bounds.width;
			width = rects [i].width * 100 / bounds.width;
		}
		if (bounds.height != 0) {
			y = (rects [i].y - bounds.y) * 100 / bounds.height;
			height = rects [i].height * 100 / bounds.height;
		}
		result [i] = new Rectangle (x, y, width, height);			
	}
	return result;
}

void drawRectangles () {
	if (parent != null) {
		if (parent.isDisposed ()) return;
		parent.getShell ().update ();
	} else {
		display.update ();
	}
	
	int gc = OS.gdk_gc_new(xWindow);
	if (gc==0) error(SWT.ERROR_UNSPECIFIED);

	/* White foreground */
	int colormap = OS.gdk_colormap_get_system();
	GdkColor color = new GdkColor();
	OS.gdk_color_white(colormap, color);
	OS.gdk_gc_set_foreground(gc, color);

	/* Draw on top of inferior widgets */
	OS.gdk_gc_set_subwindow(gc, OS.GDK_INCLUDE_INFERIORS);
	
	/* XOR */
	OS.gdk_gc_set_function(gc, OS.GDK_XOR);
	
	for (int i=0; i<rectangles.length; i++) {
		Rectangle rect = rectangles [i];
		OS.gdk_draw_rectangle(xWindow, gc, 0, rect.x, rect.y, rect.width, rect.height);
	}
	OS.g_object_unref(gc);
}

/**
 * Returns the bounds that are being drawn, expressed relative to the parent
 * widget.  If the parent is a <code>Display</code> then these are screen
 * coordinates.
 *
 * @return the bounds of the Rectangles being drawn
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Rectangle [] getRectangles () {
	checkWidget();
	return rectangles;
}

/**
 * Returns <code>true</code> if the rectangles are drawn with a stippled line, <code>false</code> otherwise.
 *
 * @return the stippled effect of the rectangles
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getStippled () {
	checkWidget();
	return stippled;
}

void grab() {
	ptrGrabResult = OS.gdk_pointer_grab(
		xWindow,
		false,
		OS.GDK_POINTER_MOTION_MASK | OS.GDK_BUTTON_RELEASE_MASK,
		xWindow,
		cursor,
		OS.GDK_CURRENT_TIME);
	lastCursor = cursor;
}

void moveRectangles (int xChange, int yChange) {
	if (xChange < 0 && ((style & SWT.LEFT) == 0)) return;
	if (xChange > 0 && ((style & SWT.RIGHT) == 0)) return;
	if (yChange < 0 && ((style & SWT.UP) == 0)) return;
	if (yChange > 0 && ((style & SWT.DOWN) == 0)) return;
	for (int i = 0; i < rectangles.length; i++) {
		rectangles [i].x += xChange;
		rectangles [i].y += yChange;
	}
}

/**
 * Displays the Tracker rectangles for manipulation by the user.  Returns when
 * the user has either finished manipulating the rectangles or has cancelled the
 * Tracker.
 * 
 * @return <code>true</code> if the user did not cancel the Tracker, <code>false</code> otherwise
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean open () {
	checkWidget();
	if (rectangles == null) return false;
	boolean cancelled=false;
	tracking = true;
	drawRectangles ();

	int[] newX = new int[1];
	int[] newY = new int[1];
	int[] oldX = new int[1];
	int[] oldY = new int[1];
	OS.gdk_window_get_pointer(xWindow, oldX,oldY, null);
	grab();
	
	// if exactly one of UP/DOWN is specified as a style then set the cursor
	// orientation accordingly (the same is done for LEFT/RIGHT styles below)
	int vStyle = style & (SWT.UP | SWT.DOWN);
	if (vStyle == SWT.UP || vStyle == SWT.DOWN) {
		cursorOrientation |= vStyle;
	}
	int hStyle = style & (SWT.LEFT | SWT.RIGHT);
	if (hStyle == SWT.LEFT || hStyle == SWT.RIGHT) {
		cursorOrientation |= hStyle;
	}

	/*
	 *  Tracker behaves like a Dialog with its own OS event loop.
	 */
	while (tracking) {
		if (parent != null && parent.isDisposed ()) break;
		// wait for an event		
		int eventPtr;
		while (true) {
			eventPtr = OS.gdk_event_get();
			if (eventPtr != 0) {
				break;
			} 
			else {
				try { Thread.sleep(50); } catch (Exception ex) {}
			}
		}

		GdkEvent osEvent = new GdkEvent();
		OS.memmove(osEvent, eventPtr, GdkEvent.sizeof);
		int eventType = osEvent.type;
		switch (eventType) {
			case OS.GDK_BUTTON_RELEASE:
			case OS.GDK_MOTION_NOTIFY:
				if (cursor != lastCursor) {
					ungrab();
					grab();
				}
				OS.gdk_window_get_pointer(xWindow, newX,newY, null);
				if (oldX [0] != newX [0] || oldY [0] != newY [0]) {
					drawRectangles ();
					Event event = new Event ();
					event.x = newX [0];
					event.y = newY [0];
					if ((style & SWT.RESIZE) != 0) {
						resizeRectangles (newX [0] - oldX [0], newY [0] - oldY [0]);
						sendEvent (SWT.Resize, event);
						/*
						 * The following is intentionally commented.  Since gtk does not currently
						 * support pointer warping, the resize cursor cannot be adjusted.  If this
						 * capability is added in the future then the following should be uncommented,
						 * and the #adjustResizeCursor method can be copied from another platform.
						 */
//						Point cursorPos = adjustResizeCursor ();
//						newX [0] = cursorPos.x; newY [0] = cursorPos.y;
					} else {
						moveRectangles (newX [0] - oldX [0], newY [0] - oldY [0]);
						sendEvent (SWT.Move, event);
					}
					/*
					 * It is possible (but unlikely) that application code
					 * could have disposed the widget in the move/resize
					 * event.  If this happens then return false to indicate
					 * that the move failed.
					 */
					if (isDisposed ()) {
						ungrab ();
						return false;
					}
					drawRectangles ();
					oldX [0] = newX [0];  oldY [0] = newY [0];
				}
				tracking = (eventType != OS.GDK_BUTTON_RELEASE);
				break;
			case OS.GDK_KEY_PRESS:
				GdkEventKey gdkEvent = new GdkEventKey ();
				OS.memmove (gdkEvent, eventPtr, GdkEventKey.sizeof);
				int stepSize = ((gdkEvent.state & OS.GDK_CONTROL_MASK) != 0) ? STEPSIZE_SMALL : STEPSIZE_LARGE;
				int xChange = 0, yChange = 0;	
				switch (gdkEvent.keyval) {
					case OS.GDK_Escape: 
						cancelled = true;
						// fallthrough
					case OS.GDK_Return:
						tracking = false;
						break;
					case OS.GDK_Left:
						xChange = -stepSize;
						break;
					case OS.GDK_Right:
						xChange = stepSize;
						break;
					case OS.GDK_Up:
						yChange = -stepSize;
						break;
					case OS.GDK_Down:
						yChange = stepSize;
						break;
				}
				if (xChange != 0 || yChange != 0) {
					drawRectangles ();
					Event event = new Event ();
					event.x = oldX[0] + xChange;
					event.y = oldY[0] + yChange;
					if ((style & SWT.RESIZE) != 0) {
						resizeRectangles (xChange, yChange);
						sendEvent (SWT.Resize, event);
						/*
						 * The following is intentionally commented.  Since gtk does not currently
						 * support pointer warping, the resize cursor cannot be adjusted.  If this
						 * capability is added in the future then the following should be uncommented,
						 * and the #adjustResizeCursor method can be copied from another platform.
						 */
//						cursorPos = adjustResizeCursor (xDisplay, xWindow);
//						oldX[0] = cursorPos.x;  oldY[0] = cursorPos.y;
					} else {
						moveRectangles (xChange, yChange);
						sendEvent (SWT.Move, event);
						/*
						 * The following is intentionally commented.  Since gtk does not currently
						 * support pointer warping, the move cursor cannot be adjusted.  If this
						 * capability is added in the future then the following should be uncommented,
						 * and the #adjustMoveCursor method can be copied from another platform.
						 */
//						cursorPos = adjustMoveCursor (xDisplay, xWindow);
//						oldX[0] = cursorPos.x;  oldY[0] = cursorPos.y;
					}
					/*
					 * It is possible (but unlikely) that application code
					 * could have disposed the widget in the move/resize
					 * event.  If this happens then return false to indicate
					 * that the move failed.
					 */
					if (isDisposed ()) {
						ungrab ();
						return false;
					}
					drawRectangles ();
				}
				break;
			}  // switch
			OS.gdk_event_free(eventPtr);
		}  // while
	drawRectangles();
	ungrab();
	return !cancelled;
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is moved or resized.
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
 * @see ControlListener
 * @see #addControlListener
 */
public void removeControlListener (ControlListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Move, listener);
}

void resizeRectangles (int xChange, int yChange) {
	/*
	* If the cursor orientation has not been set in the orientation of
	* this change then try to set it here.
	*/
	if (xChange < 0 && ((style & SWT.LEFT) != 0) && ((cursorOrientation & SWT.RIGHT) == 0)) {
		cursorOrientation |= SWT.LEFT;
	} else if (xChange > 0 && ((style & SWT.RIGHT) != 0) && ((cursorOrientation & SWT.LEFT) == 0)) {
		cursorOrientation |= SWT.RIGHT;
	} else if (yChange < 0 && ((style & SWT.UP) != 0) && ((cursorOrientation & SWT.DOWN) == 0)) {
		cursorOrientation |= SWT.UP;
	} else if (yChange > 0 && ((style & SWT.DOWN) != 0) && ((cursorOrientation & SWT.UP) == 0)) {
		cursorOrientation |= SWT.DOWN;
	}
	Rectangle bounds = computeBounds ();
	if ((cursorOrientation & SWT.LEFT) != 0) {
		bounds.x += xChange;
		bounds.width -= xChange;
	} else if ((cursorOrientation & SWT.RIGHT) != 0) {
		bounds.width += xChange;
	}
	if ((cursorOrientation & SWT.UP) != 0) {
		bounds.y += yChange;
		bounds.height -= yChange;
	} else if ((cursorOrientation & SWT.DOWN) != 0) {
		bounds.height += yChange;
	}
	/*
	* The following are conditions under which the resize should not be applied
	*/
	if (bounds.width < 0 || bounds.height < 0) return;
	
	Rectangle [] newRects = new Rectangle [rectangles.length];
	for (int i = 0; i < rectangles.length; i++) {
		Rectangle proportion = proportions[i];
		newRects[i] = new Rectangle (
			proportion.x * bounds.width / 100 + bounds.x,
			proportion.y * bounds.height / 100 + bounds.y,
			proportion.width * bounds.width / 100,
			proportion.height * bounds.height / 100);
	}
	rectangles = newRects;	
}

/**
 * Sets the <code>Cursor</code> of the Tracker.  If this cursor is <code>null</code>
 * then the cursor reverts to the default.
 *
 * @param newCursor the new <code>Cursor</code> to display
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCursor (Cursor value) {
	checkWidget ();
	cursor = 0;
	if (value != null) cursor = value.handle;
}

/**
 * Specifies the rectangles that should be drawn, expressed relative to the parent
 * widget.  If the parent is a Display then these are screen coordinates.
 *
 * @param rectangles the bounds of the rectangles to be drawn
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the set of rectangles is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setRectangles (Rectangle [] rectangles) {
	checkWidget();
	if (rectangles == null) error (SWT.ERROR_NULL_ARGUMENT);
	this.rectangles = rectangles;
	proportions = computeProportions (rectangles);
}

/**
 * Changes the appearance of the line used to draw the rectangles.
 *
 * @param stippled <code>true</code> if rectangle should appear stippled
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setStippled (boolean stippled) {
	checkWidget();
	this.stippled = stippled;
}

void ungrab() {
	if (ptrGrabResult == OS.GDK_GRAB_SUCCESS) {
		OS.gdk_pointer_ungrab(OS.GDK_CURRENT_TIME);
	}
}

}
