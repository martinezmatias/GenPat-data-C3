package org.eclipse.swt.widgets;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */

import org.eclipse.swt.*;
import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.graphics.*;

/**
 * This class is the abstract superclass of all classes which
 * represent controls that have standard scroll bars.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>H_SCROLL, V_SCROLL</dd>
 * <dt><b>Events:</b>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * IMPORTANT: This class is intended to be subclassed <em>only</em>
 * within the SWT implementation.
 * </p>
 */

public abstract class Scrollable extends Control {
	int scrolledHandle;
	ScrollBar horizontalBar, verticalBar;

/**
 * Prevents uninitialized instances from being created outside the package.
 */
Scrollable () {}

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * for all SWT widget classes should include a comment which
 * describes the style constants which are applicable to the class.
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
 * @see SWT
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Scrollable (Composite parent, int style) {
	super (parent, style);
}

int clientHandle () {
	return handle;
}

/**
 * Given a desired <em>client area</em> for the receiver
 * (as described by the arguments), returns the bounding
 * rectangle which would be required to produce that client
 * area.
 * <p>
 * In other words, it returns a rectangle such that, if the
 * receiver's bounds were set to that rectangle, the area
 * of the receiver which is capable of displaying data
 * (that is, not covered by the "trimmings") would be the
 * rectangle described by the arguments (relative to the
 * receiver's parent).
 * </p>
 * 
 * @return the required bounds to produce the given client area
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #getClientArea
 */
public Rectangle computeTrim (int x, int y, int width, int height) {
	checkWidget ();
	int hs=0, vs=0;
	/* FIXME - just measured the width on one particular theme */
	if ((style&SWT.H_SCROLL)!=0) hs=18;
	if ((style&SWT.V_SCROLL)!=0) vs=18;	
	return new Rectangle (x, y, width+vs, height+hs);
}

ScrollBar createScrollBar (int style) {
	if (scrolledHandle == 0) return null;
	ScrollBar bar = new ScrollBar ();
	bar.parent = this;
	bar.style = style;
	bar.state |= HANDLE;
	if ((style & SWT.H_SCROLL) != 0) {
		bar.handle = OS.gtk_scrolled_window_get_hadjustment (scrolledHandle);
	} else {
		bar.handle = OS.gtk_scrolled_window_get_vadjustment (scrolledHandle);
	}
	bar.hookEvents ();
	bar.register ();
	return bar;
}

void createWidget (int index) {
	super.createWidget (index);
	if ((style & SWT.H_SCROLL) != 0) horizontalBar = createScrollBar (SWT.H_SCROLL);
	if ((style & SWT.V_SCROLL) != 0) verticalBar = createScrollBar (SWT.V_SCROLL);
}

void deregister () {
	super.deregister ();
	if (scrolledHandle != 0) WidgetTable.remove (scrolledHandle);
}

/**
 * Returns a rectangle which describes the area of the
 * receiver which is capable of displaying data (that is,
 * not covered by the "trimmings").
 * 
 * @return the client area
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #computeTrim
 */
public Rectangle getClientArea () {
	checkWidget ();
	//FIXME - List, Table, Tree, ...
	int clientHandle = clientHandle ();
	int width = OS.GTK_WIDGET_WIDTH (clientHandle);
	int height = OS.GTK_WIDGET_HEIGHT (clientHandle);
	if ((state & CANVAS) != 0) {
		return new Rectangle (0, 0, width, height);
	}
	int x = OS.GTK_WIDGET_X (clientHandle);
	int y = OS.GTK_WIDGET_Y (clientHandle);
	return new Rectangle (x, y, width, height);
}
/**
 * Returns the receiver's horizontal scroll bar if it has
 * one, and null if it does not.
 *
 * @return the horizontal scroll bar (or null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public ScrollBar getHorizontalBar () {
	checkWidget ();
	return horizontalBar;
}
/**
 * Returns the receiver's vertical scroll bar if it has
 * one, and null if it does not.
 *
 * @return the vertical scroll bar (or null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public ScrollBar getVerticalBar () {
	checkWidget ();
	return verticalBar;
}

void register () {
	super.register ();
	if (scrolledHandle != 0) WidgetTable.put (scrolledHandle, this);
}

void releaseHandle () {
	super.releaseHandle ();
	scrolledHandle = 0;
}

void releaseWidget () {
	if (horizontalBar != null) {
		horizontalBar.releaseWidget ();
		horizontalBar.releaseHandle ();
	}	
	if (verticalBar != null) {
		verticalBar.releaseWidget ();
		verticalBar.releaseHandle ();
	}			
	horizontalBar = verticalBar = null;
	super.releaseWidget ();
}

void resizeHandle (int width, int height) {
	int topHandle = topHandle ();
	int flags = OS.GTK_WIDGET_FLAGS (topHandle);
	OS.GTK_WIDGET_SET_FLAGS(topHandle, OS.GTK_VISIBLE);
	int parentHandle = parent.parentingHandle ();
	if (fixedHandle != 0) {
		OS.gtk_widget_set_size_request (fixedHandle, width, height);
	}
	if (scrolledHandle != 0) {
		OS.gtk_widget_set_size_request (scrolledHandle, width, height);
	} else {
		OS.gtk_widget_set_size_request (handle, width, height);
	}
	Display display = getDisplay ();
	boolean warnings = display.getWarnings ();
	display.setWarnings (false);
	OS.gtk_container_resize_children (parentHandle);
	display.setWarnings (warnings);
	if ((flags & OS.GTK_VISIBLE) == 0) {
		OS.GTK_WIDGET_UNSET_FLAGS(topHandle, OS.GTK_VISIBLE);	
	}
}

int topHandle () {
	if (fixedHandle != 0) return fixedHandle;
	if (scrolledHandle != 0) return scrolledHandle;
	return super.topHandle ();
}

}
