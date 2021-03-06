package org.eclipse.swt.widgets;

/*
 * Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */

import org.eclipse.swt.internal.photon.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;

/**
 * Instances of this class are controls which are capable
 * of containing other controls.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>NO_BACKGROUND, NO_FOCUS, NO_MERGE_PAINTS, NO_REDRAW_RESIZE, NO_RADIO_GROUP</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * Note: The <code>NO_BACKGROUND</code>, <code>NO_FOCUS</code>, <code>NO_MERGE_PAINTS</code>,
 * and <code>NO_REDRAW_RESIZE</code> styles are intended for use with <code>Canvas</code>.
 * They can be used with <code>Composite</code> if you are drawing your own, but their
 * behavior is undefined if they are used with subclasses of <code>Composite</code> other
 * than <code>Canvas</code>.
 * </p><p>
 * This class may be subclassed by custom control implementors
 * who are building controls that are constructed from aggregates
 * of other controls.
 * </p>
 *
 * @see Canvas
 */
public class Composite extends Scrollable {
	Layout layout;
	Control [] tabList;
	int cornerHandle;
	
Composite () {
	/* Do nothing */
}

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
 * </ul>
 *
 * @see SWT#NO_BACKGROUND
 * @see SWT#NO_FOCUS
 * @see SWT#NO_MERGE_PAINTS
 * @see SWT#NO_REDRAW_RESIZE
 * @see SWT#NO_RADIO_GROUP
 * @see Widget#getStyle
 */
public Composite (Composite parent, int style) {
	super (parent, style);
}
	
Control [] _getChildren () {
	int count = 0;
	int parentHandle = parentingHandle ();
	int child = OS.PtWidgetChildFront (parentHandle);
	while (child != 0) {
		child = OS.PtWidgetBrotherBehind (child);
		count++;
	}
	Control [] children = new Control [count];
	int i = 0, j = 0;
	child = OS.PtWidgetChildFront (parentHandle);
	while (i < count) {
		Widget widget = WidgetTable.get (child);
		if (widget != null && widget != this) {
			if (widget instanceof Control) {
				children [j++] = (Control) widget;
			}
		}
		i++;
		child = OS.PtWidgetBrotherBehind (child);
	}
	if (i == j) return children;
	Control [] newChildren = new Control [j];
	System.arraycopy (children, 0, newChildren, 0, j);
	return newChildren;
}

Control [] _getTabList () {
	if (tabList == null) return tabList;
	int count = 0;
	for (int i=0; i<tabList.length; i++) {
		if (!tabList [i].isDisposed ()) count++;
	}
	if (count == tabList.length) return tabList;
	Control [] newList = new Control [count];
	int index = 0;
	for (int i=0; i<tabList.length; i++) {
		if (!tabList [i].isDisposed ()) {
			newList [index++] = tabList [i];
		}
	}
	tabList = newList;
	return tabList;
}

protected void checkSubclass () {
	/* Do nothing - Subclassing is allowed */
}

Control [] computeTabList () {
	Control result [] = super.computeTabList ();
	if (result.length == 0) return result;
	Control [] list = tabList != null ? _getTabList () : _getChildren ();
	for (int i=0; i<list.length; i++) {
		Control child = list [i];
		Control [] childList = child.computeTabList ();
		if (childList.length != 0) {
			Control [] newResult = new Control [result.length + childList.length];
			System.arraycopy (result, 0, newResult, 0, result.length);
			System.arraycopy (childList, 0, newResult, result.length, childList.length);
			result = newResult;
		}
	}
	return result;
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget();
	Point size;
	if (layout != null) {
		if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
			size = layout.computeSize (this, wHint, hHint, changed);
		} else {
			size = new Point (wHint, hHint);
		}
	} else {
		size = minimumSize ();
	}
	if (size.x == 0) size.x = DEFAULT_WIDTH;
	if (size.y == 0) size.y = DEFAULT_HEIGHT;
	if (wHint != SWT.DEFAULT) size.x = wHint;
	if (hHint != SWT.DEFAULT) size.y = hHint;
	Rectangle trim = computeTrim (0, 0, size.x, size.y);
	return new Point (trim.width, trim.height);
}

void createHandle (int index) {
	state |= HANDLE | CANVAS;
	int parentHandle = parent.parentingHandle ();
	createScrolledHandle (parentHandle);
}

void createScrollBars () {
	if (scrolledHandle == 0) return;
	if ((style & SWT.H_SCROLL) != 0) {
		horizontalBar = new ScrollBar (this, SWT.HORIZONTAL);
	}
	if ((style & SWT.V_SCROLL) != 0) {
		verticalBar = new ScrollBar (this, SWT.VERTICAL);
	}
}

byte [] defaultFont () {
	Display display = getDisplay ();
	return display.TITLE_FONT;
}

void createScrolledHandle (int parentHandle) {
	int etches = OS.Pt_ALL_ETCHES | OS.Pt_ALL_OUTLINES;
	int [] args = new int [] {
		OS.Pt_ARG_FLAGS, hasBorder () ? OS.Pt_HIGHLIGHTED : 0, OS.Pt_HIGHLIGHTED,
		OS.Pt_ARG_BASIC_FLAGS, hasBorder () ? etches : 0, etches,
		OS.Pt_ARG_CONTAINER_FLAGS, 0, OS.Pt_ENABLE_CUA | OS.Pt_ENABLE_CUA_ARROWS,
		OS.Pt_ARG_FILL_COLOR, OS.Pg_TRANSPARENT, 0,
		OS.Pt_ARG_RESIZE_FLAGS, 0, OS.Pt_RESIZE_XY_BITS,
	};
	scrolledHandle = OS.PtCreateWidget (OS.PtContainer (), parentHandle, args.length / 3, args);
	if (scrolledHandle == 0) error (SWT.ERROR_NO_HANDLES);
	Display display = getDisplay ();
	if ((style & SWT.H_SCROLL) != 0 && (style & SWT.V_SCROLL) != 0) {
		etches = OS.Pt_TOP_OUTLINE | OS.Pt_LEFT_OUTLINE;
		args = new int [] {
			OS.Pt_ARG_FLAGS, OS.Pt_HIGHLIGHTED, OS.Pt_HIGHLIGHTED,
			OS.Pt_ARG_BASIC_FLAGS, etches, etches,
			OS.Pt_ARG_WIDTH, display.SCROLLBAR_WIDTH, 0,
			OS.Pt_ARG_HEIGHT, display.SCROLLBAR_HEIGHT, 0,
			OS.Pt_ARG_RESIZE_FLAGS, 0, OS.Pt_RESIZE_XY_BITS,
		};
		cornerHandle = OS.PtCreateWidget (OS.PtContainer (), scrolledHandle, args.length / 3, args);
	}
	int clazz = display.PtContainer;
	args = new int [] {
		OS.Pt_ARG_CONTAINER_FLAGS, 0, OS.Pt_ENABLE_CUA | OS.Pt_ENABLE_CUA_ARROWS,
		OS.Pt_ARG_RESIZE_FLAGS, 0, OS.Pt_RESIZE_XY_BITS,
	};
	handle = OS.PtCreateWidget (clazz, scrolledHandle, args.length / 3, args);
	if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	createScrollBars ();
}

public boolean forceFocus () {
	checkWidget();
	if ((state & CANVAS) == 0) return super.forceFocus ();
	/*
	* Bug in Photon. PtContainerGiveFocus() is supposed to give
	* focus to the widget even if the widget's Pt_GET_FOCUS flag
	* is not set. This does not happen when the widget is a
	* PtContainer. The fix is to set the flag before calling it.
	*/
	int flags = OS.PtWidgetFlags (handle);
	OS.PtSetResource (handle, OS.Pt_ARG_FLAGS, OS.Pt_GETS_FOCUS, OS.Pt_GETS_FOCUS);
	boolean result = super.forceFocus ();
	OS.PtSetResource (handle, OS.Pt_ARG_FLAGS, flags, OS.Pt_GETS_FOCUS);
	return result;
}

public Rectangle getClientArea () {
	checkWidget();
	if (scrolledHandle == 0) return super.getClientArea ();
	PhArea_t area = new PhArea_t ();
	OS.PtWidgetArea (handle, area);
	return new Rectangle (area.pos_x, area.pos_y, area.size_w, area.size_h);
}

int getClipping(int widget, int topWidget, boolean clipChildren, boolean clipSiblings) {
	int child_tile = 0;
	int widget_tile = OS.PhGetTile(); // NOTE: PhGetTile native initializes the tile

	PhRect_t rect = new PhRect_t ();
	int args [] = {OS.Pt_ARG_FLAGS, 0, 0, OS.Pt_ARG_BASIC_FLAGS, 0, 0};
	
	/* Get the rectangle of all siblings in front of the widget */
	if (clipSiblings && OS.PtWidgetClass(topWidget) != OS.PtWindow()) {
		int temp_widget = topWidget;
		while ((temp_widget = OS.PtWidgetBrotherInFront(temp_widget)) != 0) {
			if (OS.PtWidgetIsRealized(temp_widget)) {
				int tile = OS.PhGetTile();
				if (child_tile == 0) child_tile = tile;			
				else child_tile = OS.PhAddMergeTiles(tile, child_tile, null);
				OS.PtWidgetExtent(temp_widget, tile); // NOTE: tile->rect
				args [1] = args [4] = 0;
				OS.PtGetResources(temp_widget, args.length / 3, args);
				if ((args [1] & OS.Pt_HIGHLIGHTED) != 0) {
					int basic_flags = args [4];
					OS.memmove(rect, tile, PhRect_t.sizeof);
					if ((basic_flags & OS.Pt_TOP_ETCH) != 0) rect.ul_y++;
					if ((basic_flags & OS.Pt_BOTTOM_ETCH) != 0) rect.lr_y--;
					if ((basic_flags & OS.Pt_RIGHT_ETCH) != 0) rect.ul_x++;
					if ((basic_flags & OS.Pt_LEFT_ETCH) != 0) rect.lr_x--;
					OS.memmove(tile, rect, PhRect_t.sizeof);
				}
			}
		}
		/* Translate the siblings rectangles to the widget's coordinates */
		OS.PtWidgetCanvas(topWidget, widget_tile); // NOTE: widget_tile->rect
		OS.PhDeTranslateTiles(child_tile, widget_tile); // NOTE: widget_tile->rect.ul
	}
			
	/* Get the rectangle of the widget's children */
	if (clipChildren) {
		int temp_widget = OS.PtWidgetChildBack(widget);
		while (temp_widget != 0) {
			if (OS.PtWidgetIsRealized(temp_widget)) {
				int tile = OS.PhGetTile();
				if (child_tile == 0) child_tile = tile;			
				else child_tile = OS.PhAddMergeTiles(tile, child_tile, null);
				OS.PtWidgetExtent(temp_widget, tile); // NOTE: tile->rect
				args [1] = args [4] = 0;
				OS.PtGetResources(temp_widget, args.length / 3, args);
				if ((args [1] & OS.Pt_HIGHLIGHTED) != 0) {
					int basic_flags = args [4];
					OS.memmove(rect, tile, PhRect_t.sizeof);
					if ((basic_flags & OS.Pt_TOP_ETCH) != 0) rect.ul_y++;
					if ((basic_flags & OS.Pt_BOTTOM_ETCH) != 0) rect.lr_y--;
					if ((basic_flags & OS.Pt_RIGHT_ETCH) != 0) rect.ul_x++;
					if ((basic_flags & OS.Pt_LEFT_ETCH) != 0) rect.lr_x--;
					OS.memmove(tile, rect, PhRect_t.sizeof);
				}
			}
			temp_widget = OS.PtWidgetBrotherInFront(temp_widget);
		}
	}

	/* Get the widget's rectangle */
	OS.PtWidgetCanvas(widget, widget_tile); // NOTE: widget_tile->rect
	OS.PhDeTranslateTiles(widget_tile, widget_tile); // NOTE: widget_tile->rect.ul

	/* Clip the widget's rectangle from the child/siblings rectangle's */
	if (child_tile != 0) {
		int clip_tile = OS.PhClipTilings(widget_tile, child_tile, null);
		OS.PhFreeTiles(child_tile);
		return clip_tile;
	}
	return widget_tile;
}

/**
 * Returns an array containing the receiver's children.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of children, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return an array of children
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Control [] getChildren () {
	checkWidget();
	return _getChildren ();
}

int getChildrenCount () {
	int count = 0;
	int parentHandle = parentingHandle ();
	int child = OS.PtWidgetChildFront (parentHandle);
	while (child != 0) {
		child = OS.PtWidgetBrotherBehind (child);
		count++;
	}
	return count;
}

/**
 * Returns layout which is associated with the receiver, or
 * null if one has not been set.
 *
 * @return the receiver's layout or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Layout getLayout () {
	checkWidget();
	return layout;
}

boolean hasBorder () {
	return (style & SWT.BORDER) != 0;
}

boolean hasFocus () {
	return OS.PtIsFocused (handle) == 2;
}

void hookEvents () {
	super.hookEvents ();
	if ((state & CANVAS) != 0) {
		int windowProc = getDisplay ().windowProc;
		OS.PtAddEventHandler (handle, OS.Ph_EV_DRAG, windowProc, SWT.MouseMove);
	}
}

/**
 * Gets the last specified tabbing order for the control.
 *
 * @return tabList the ordered list of controls representing the tab order
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see #setTabList
 */
public Control [] getTabList () {
	checkWidget ();
	Control [] tabList = _getTabList ();
	if (tabList == null) {
		int count = 0;
		Control [] list =_getChildren ();
		for (int i=0; i<list.length; i++) {
			if (list [i].isTabGroup ()) count++;
		}
		tabList = new Control [count];
		int index = 0;
		for (int i=0; i<list.length; i++) {
			if (list [i].isTabGroup ()) {
				tabList [index++] = list [i];
			}
		}
	}
	return tabList;
}

/**
 * If the receiver has a layout, asks the layout to <em>lay out</em>
 * (that is, set the size and location of) the receiver's children. 
 * If the receiver does not have a layout, do nothing.
 * <p>
 * This is equivalent to calling <code>layout(true)</code>.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void layout () {
	checkWidget();
	layout (true);
}

Point minimumSize () {
	Control [] children = _getChildren ();
	int width = 0, height = 0;
	for (int i=0; i<children.length; i++) {
		Rectangle rect = children [i].getBounds ();
		width = Math.max (width, rect.x + rect.width);
		height = Math.max (height, rect.y + rect.height);
	}
	return new Point (width, height);
}

void moveToBack (int child) {
	OS.PtWidgetToBack (child);
}

/**
 * If the receiver has a layout, asks the layout to <em>lay out</em>
 * (that is, set the size and location of) the receiver's children. 
 * If the the argument is <code>true</code> the layout must not rely
 * on any cached information it is keeping about the children. If it
 * is <code>false</code> the layout may (potentially) simplify the
 * work it is doing by assuming that the state of the none of the
 * receiver's children has changed since the last layout.
 * If the receiver does not have a layout, do nothing.
 *
 * @param changed <code>true</code> if the layout must flush its caches, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void layout (boolean changed) {
	checkWidget();
	if (layout == null) return;
	int count = getChildrenCount ();
	if (count == 0) return;
	layout.layout (this, changed);
}

int parentingHandle () {
	return handle;
}

int processMouse (int info) {
	if ((state & CANVAS) != 0) {
		if (info == 0) return OS.Pt_END;
		PtCallbackInfo_t cbinfo = new PtCallbackInfo_t ();
		OS.memmove (cbinfo, info, PtCallbackInfo_t.sizeof);
		if (cbinfo.event == 0) return OS.Pt_END;
		PhEvent_t ev = new PhEvent_t ();
		OS.memmove (ev, cbinfo.event, PhEvent_t.sizeof);
		if (ev.type == OS.Ph_EV_BUT_PRESS) {
			int data = OS.PhGetData (cbinfo.event);
			if (data == 0) return OS.Pt_END;
			PhPointerEvent_t pe = new PhPointerEvent_t ();
			OS.memmove (pe, data, PhPointerEvent_t.sizeof);
	
			/* Grab pointer */
			if (!(menu != null && pe.buttons == OS.Ph_BUTTON_MENU)) {
				if (pe.click_count == 1) {
					PhRect_t rect = new PhRect_t ();
					PhPoint_t pos = new PhPoint_t();
					pos.x = pe.pos_x;
					pos.y = pe.pos_y;
					rect.ul_x = rect.lr_x = (short) (pos.x + ev.translation_x);
					rect.ul_y = rect.lr_y = (short) (pos.y + ev.translation_y);
					int rid = OS.PtWidgetRid (handle);
					int input_group = OS.PhInputGroup (0);
					int flags = OS.Ph_DRAG_KEY_MOTION | OS.Ph_DRAG_TRACK | OS.Ph_TRACK_DRAG;
					OS.PhInitDrag (rid, flags, rect, null, input_group, null, null, null, pos, null);
				}
			}
	
			int result = super.processMouse (info);
	
			/* Set focus for the a CANVAS with no children */
			if ((style & SWT.NO_FOCUS) == 0) {
				if (pe.buttons == OS.Ph_BUTTON_SELECT) {
					if (OS.PtWidgetChildFront (handle) == 0) {
						setFocus ();
					}
				}
			}
			return result;
		}
	}
	return super.processMouse (info);
}

int processPaint (int damage) {
	if ((state & CANVAS) != 0) {
		if ((style & SWT.NO_BACKGROUND) == 0) {
			
			/* Get the clipping tiles for children and siblings */
			int clip_tile = getClipping (handle, topHandle (), true, true);

			/* Translate the clipping to the current GC coordinates */
			short [] abs_x = new short [1], abs_y = new short [1];
			OS.PtGetAbsPosition (handle, abs_x, abs_y);
			short [] dis_abs_x = new short [1], dis_abs_y = new short [1];
			OS.PtGetAbsPosition (OS.PtFindDisjoint (handle), dis_abs_x, dis_abs_y);
			PhPoint_t delta = new PhPoint_t ();
			delta.x = (short) (abs_x [0] - dis_abs_x [0]);
			delta.y = (short) (abs_y [0] - dis_abs_y [0]);
			OS.PhTranslateTiles(clip_tile, delta);

			/* Set the clipping */
			int[] clip_rects_count = new int [1];
			int clip_rects = OS.PhTilesToRects (clip_tile, clip_rects_count);
			OS.PhFreeTiles (clip_tile);	
			if (clip_rects_count [0] == 0) {
				clip_rects_count [0] = 1;
				OS.free (clip_rects);
				clip_rects = OS.malloc (PhRect_t.sizeof);
			}
			OS.PgSetMultiClip (clip_rects_count[0], clip_rects);
			OS.free (clip_rects);
			
			/* Draw the widget */
			OS.PtSuperClassDraw (OS.PtContainer (), handle, damage);
			
			/* Reset the clipping */
			OS.PgSetMultiClip (0, 0);
		}
	}
	return super.processPaint (damage);
}

void releaseChildren () {
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		if (!child.isDisposed ()) child.releaseResources ();
	}
}

void releaseHandle () {
	super.releaseHandle ();
	cornerHandle = 0;
}

void releaseWidget () {
	releaseChildren ();
	super.releaseWidget ();
	layout = null;
	tabList = null;
}

void resizeClientArea () {
	int [] args = {OS.Pt_ARG_WIDTH, 0, 0, OS.Pt_ARG_HEIGHT, 0, 0};
	OS.PtGetResources (scrolledHandle, args.length / 3, args);
	resizeClientArea (args [1], args [4]);
}

void resizeClientArea (int width, int height) {
	if (scrolledHandle == 0) return;
	
	/* Calculate the insets */
	int [] args = {
		OS.Pt_ARG_BASIC_FLAGS, 0, 0,
		OS.Pt_ARG_BEVEL_WIDTH, 0, 0,
	};
	OS.PtGetResources (scrolledHandle, args.length / 3, args);
	int flags = args [1];
	int bevel = args [4];
	int top = 0, left = 0, right = 0, bottom = 0;
	if ((flags & OS.Pt_TOP_ETCH) != 0) top++;
	if ((flags & OS.Pt_TOP_OUTLINE) != 0) top++;
	if ((flags & OS.Pt_TOP_INLINE) != 0) top++;
	if ((flags & OS.Pt_TOP_BEVEL) != 0) top += bevel;
	if ((flags & OS.Pt_BOTTOM_ETCH) != 0) bottom++;
	if ((flags & OS.Pt_BOTTOM_OUTLINE) != 0) bottom++;
	if ((flags & OS.Pt_BOTTOM_INLINE) != 0) bottom++;
	if ((flags & OS.Pt_BOTTOM_BEVEL) != 0) bottom += bevel;
	if ((flags & OS.Pt_RIGHT_ETCH) != 0) right++;
	if ((flags & OS.Pt_RIGHT_OUTLINE) != 0) right++;
	if ((flags & OS.Pt_RIGHT_INLINE) != 0) right++;
	if ((flags & OS.Pt_RIGHT_BEVEL) != 0) right += bevel;
	if ((flags & OS.Pt_LEFT_ETCH) != 0) left++;
	if ((flags & OS.Pt_LEFT_OUTLINE) != 0) left++;
	if ((flags & OS.Pt_LEFT_INLINE) != 0) left++;
	if ((flags & OS.Pt_LEFT_BEVEL) != 0) left += bevel;
	
	int clientWidth = width - (left + right);
	int clientHeight = height - (top + bottom);

	int vBarWidth = 0, hBarHeight = 0;
	boolean isVisibleHBar = horizontalBar != null && horizontalBar.getVisible ();
	boolean isVisibleVBar = verticalBar != null && verticalBar.getVisible ();
	if (isVisibleHBar) {
		args = new int [] {OS.Pt_ARG_HEIGHT, 0, 0};
		OS.PtGetResources (horizontalBar.handle, args.length / 3, args);
		clientHeight -= (hBarHeight = args [1]);
	}
	if (isVisibleVBar) {
		args = new int [] {OS.Pt_ARG_WIDTH, 0, 0};
		OS.PtGetResources (verticalBar.handle, args.length / 3, args);
		clientWidth -= (vBarWidth = args [1]);
	}
	if (isVisibleHBar) {
		horizontalBar.setBounds (0, clientHeight, clientWidth, hBarHeight);
	}
	if (isVisibleVBar) {
		verticalBar.setBounds (clientWidth, 0, vBarWidth, clientHeight);
	}
	args = new int [] {
		OS.Pt_ARG_WIDTH, Math.max (clientWidth, 0), 0,
		OS.Pt_ARG_HEIGHT, Math.max (clientHeight, 0), 0,
	};
	OS.PtSetResources (handle, args.length / 3, args);
	if (cornerHandle != 0) {
		PhPoint_t pt = new PhPoint_t ();
		pt.x = (short) clientWidth;
		pt.y = (short) clientHeight;
		int ptr = OS.malloc (PhPoint_t.sizeof);
		OS.memmove (ptr, pt, PhPoint_t.sizeof);
		OS.PtSetResource (cornerHandle, OS.Pt_ARG_POS, ptr, 0);
		OS.free (ptr);
	}
}

boolean sendResize () {
	return scrolledHandle == 0;
}

boolean setBounds (int x, int y, int width, int height, boolean move, boolean resize) {
	boolean changed = super.setBounds (x, y, width, height, move, resize);
	if (changed && resize) {
		resizeClientArea (width, height);
		sendEvent(SWT.Resize);
		if (layout != null) layout (false);
	}
	return changed;
}

public boolean setFocus () {
	checkWidget();
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		if (child.getVisible () && child.setFocus ()) return true;
	}
	return super.setFocus ();
}

/**
 * Sets the layout which is associated with the receiver to be
 * the argument which may be null.
 *
 * @param layout the receiver's new layout or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLayout (Layout layout) {
	checkWidget();
	this.layout = layout;
}

boolean setTabGroupFocus () {
	if (isTabItem ()) return setTabItemFocus ();
	if ((style & SWT.NO_FOCUS) == 0) {
		boolean takeFocus = true;
		if ((state & CANVAS) != 0) {
			takeFocus = hooks (SWT.KeyDown) || hooks (SWT.KeyUp);
		}
		if (takeFocus && setTabItemFocus ()) return true;
	}
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		if (child.isTabItem () && child.setTabItemFocus ()) return true;
	}
	return false;
}

boolean setTabItemFocus () {
	if ((style & SWT.NO_FOCUS) == 0) {
		boolean takeFocus = true;
		if ((state & CANVAS) != 0) {
			takeFocus = hooks (SWT.KeyDown) || hooks (SWT.KeyUp);
		}
		if (takeFocus) {
			if (!isShowing ()) return false;
			if (forceFocus ()) return true;
		}
	}
	return super.setTabItemFocus ();
}

/**
 * Sets the tabbing order for the specified controls to
 * match the order that they occur in the argument list.
 *
 * @param tabList the ordered list of controls representing the tab order or null
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if a widget in the tabList is null or has been disposed</li> 
 *    <li>ERROR_INVALID_PARENT - if widget in the tabList is not in the same widget tree</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setTabList (Control [] tabList) {
	checkWidget ();
	if (tabList == null) error (SWT.ERROR_NULL_ARGUMENT);
	for (int i=0; i<tabList.length; i++) {
		Control control = tabList [i];
		if (control == null) error (SWT.ERROR_INVALID_ARGUMENT);
		if (control.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
		if (control.parent != this) error (SWT.ERROR_INVALID_PARENT);
	}
	this.tabList = tabList;
}

int traversalCode (int key_sym, PhKeyEvent_t ke) {
	if ((state & CANVAS) != 0) {
		if ((style & SWT.NO_FOCUS) != 0) return 0;
		if (hooks (SWT.KeyDown) || hooks (SWT.KeyUp)) return 0;
	}
	return super.traversalCode (key_sym, ke);
}

}
