/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.widgets;


import org.eclipse.swt.*;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.cairo.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Control is the abstract superclass of all windowed user interface classes.
 * <p>
 * <dl>
 * <dt><b>Styles:</b>
 * <dd>BORDER</dd>
 * <dd>LEFT_TO_RIGHT, RIGHT_TO_LEFT, FLIP_TEXT_DIRECTION</dd>
 * <dt><b>Events:</b>
 * <dd>DragDetect, FocusIn, FocusOut, Help, KeyDown, KeyUp, MenuDetect, MouseDoubleClick, MouseDown, MouseEnter,
 *     MouseExit, MouseHover, MouseUp, MouseMove, MouseWheel, MouseHorizontalWheel, MouseVerticalWheel, Move,
 *     Paint, Resize, Traverse</dd>
 * </dl>
 * </p><p>
 * Only one of LEFT_TO_RIGHT or RIGHT_TO_LEFT may be specified.
 * </p><p>
 * IMPORTANT: This class is intended to be subclassed <em>only</em>
 * within the SWT implementation.
 * </p>
 *
 * @see <a href="http://www.eclipse.org/swt/snippets/#control">Control snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: ControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class Control extends Widget implements Drawable {
	long /*int*/ fixedHandle;
	long /*int*/ redrawWindow, enableWindow, provider;
	int drawCount, backgroundAlpha = 255;
	long /*int*/ enterNotifyEventId;
	Composite parent;
	Cursor cursor;
	Menu menu;
	Image backgroundImage;
	Font font;
	Region region;
	String toolTipText;
	Object layoutData;
	Accessible accessible;
	Control labelRelation;

	/* these class variables are for the workaround for bug #427776 */
	static Callback enterNotifyEventFunc;
	static int enterNotifyEventSignalId;
	static int GTK_POINTER_WINDOW;
	static int SWT_GRAB_WIDGET;

Control () {
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
 * @see SWT#BORDER
 * @see SWT#LEFT_TO_RIGHT
 * @see SWT#RIGHT_TO_LEFT
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Control (Composite parent, int style) {
	super (parent, style);
	this.parent = parent;
	createWidget (0);
}

Font defaultFont () {
	return display.getSystemFont ();
}

@Override
void deregister () {
	super.deregister ();
	if (fixedHandle != 0) display.removeWidget (fixedHandle);
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) display.removeWidget (imHandle);
}

void drawBackground (Control control, long /*int*/ window, long /*int*/ region, int x, int y, int width, int height) {
	drawBackground(control, window, 0, region, x, y, width, height);
}

void drawBackground (Control control, long /*int*/ window, long /*int*/ cr, long /*int*/ region, int x, int y, int width, int height) {
	if (OS.USE_CAIRO) {
		long /*int*/ cairo = cr != 0 ? cr : OS.gdk_cairo_create(window);
		if (cairo == 0) error (SWT.ERROR_NO_HANDLES);
		if (region != 0) {
			OS.gdk_cairo_region(cairo, region);
			Cairo.cairo_clip(cairo);
		}
		if (control.backgroundImage != null) {
			Point pt = display.map (this, control, 0, 0);
			Cairo.cairo_translate (cairo, -pt.x, -pt.y);
			x += pt.x;
			y += pt.y;
			long /*int*/ pattern = Cairo.cairo_pattern_create_for_surface (control.backgroundImage.surface);
			if (pattern == 0) error (SWT.ERROR_NO_HANDLES);
			Cairo.cairo_pattern_set_extend (pattern, Cairo.CAIRO_EXTEND_REPEAT);
			if ((style & SWT.MIRRORED) != 0) {
				double[] matrix = {-1, 0, 0, 1, 0, 0};
				Cairo.cairo_pattern_set_matrix(pattern, matrix);
			}
			Cairo.cairo_set_source (cairo, pattern);
			Cairo.cairo_pattern_destroy (pattern);
		} else {
			GdkColor color = control.getBackgroundColor ();
			Cairo.cairo_set_source_rgba (cairo, (color.red & 0xFFFF) / (float)0xFFFF, (color.green & 0xFFFF) / (float)0xFFFF, (color.blue & 0xFFFF) / (float)0xFFFF, 1);
		}
		Cairo.cairo_rectangle (cairo, x, y, width, height);
		Cairo.cairo_fill (cairo);
		if (cairo != cr) Cairo.cairo_destroy(cairo);
		return;
	}
	long /*int*/ gdkGC = OS.gdk_gc_new (window);
	if (region != 0) OS.gdk_gc_set_clip_region (gdkGC, region);
	if (control.backgroundImage != null) {
		Point pt = display.map (this, control, 0, 0);
		OS.gdk_gc_set_fill (gdkGC, OS.GDK_TILED);
		OS.gdk_gc_set_ts_origin (gdkGC, -pt.x, -pt.y);
		OS.gdk_gc_set_tile (gdkGC, control.backgroundImage.pixmap);
		OS.gdk_draw_rectangle (window, gdkGC, 1, x, y, width, height);
	} else {
		GdkColor color = control.getBackgroundColor ();
		OS.gdk_gc_set_foreground (gdkGC, color);
		OS.gdk_draw_rectangle (window, gdkGC, 1, x, y, width, height);
	}
	OS.g_object_unref (gdkGC);
}

boolean drawGripper (GC gc, int x, int y, int width, int height, boolean vertical) {
	long /*int*/ paintHandle = paintHandle ();
	long /*int*/ window = gtk_widget_get_window (paintHandle);
	if (window == 0) return false;
	int orientation = vertical ? OS.GTK_ORIENTATION_HORIZONTAL : OS.GTK_ORIENTATION_VERTICAL;
	if ((style & SWT.MIRRORED) != 0) x = getClientWidth () - width - x;
	if (OS.GTK3) {
		long /*int*/ context = OS.gtk_widget_get_style_context (paintHandle);
		OS.gtk_style_context_save (context);
		OS.gtk_style_context_add_class (context, OS.GTK_STYLE_CLASS_PANE_SEPARATOR);
		OS.gtk_style_context_set_state (context, OS.GTK_STATE_FLAG_NORMAL);
		OS.gtk_render_handle (context, gc.handle, x, y, width, height);
		OS.gtk_style_context_restore (context);
	} else {
		OS.gtk_paint_handle (OS.gtk_widget_get_style (paintHandle), window, OS.GTK_STATE_NORMAL, OS.GTK_SHADOW_OUT, null, paintHandle, new byte [1], x, y, width, height, orientation);
	}
	return true;
}

void drawWidget (GC gc) {
}

void enableWidget (boolean enabled) {
	OS.gtk_widget_set_sensitive (handle, enabled);
}

long /*int*/ enterExitHandle () {
	return eventHandle ();
}

long /*int*/ eventHandle () {
	return handle;
}

long /*int*/ eventWindow () {
	long /*int*/ eventHandle = eventHandle ();
	OS.gtk_widget_realize (eventHandle);
	return gtk_widget_get_window (eventHandle);
}

void fixFocus (Control focusControl) {
	Shell shell = getShell ();
	Control control = this;
	while (control != shell && (control = control.parent) != null) {
		if (control.setFocus ()) return;
	}
	shell.setSavedFocus (focusControl);
	long /*int*/ focusHandle = shell.vboxHandle;
	OS.gtk_widget_set_can_focus (focusHandle, true);
	OS.gtk_widget_grab_focus (focusHandle);
	// widget could be disposed at this point
	if (isDisposed ()) return;
	OS.gtk_widget_set_can_focus (focusHandle, false);
}

void fixStyle () {
	if (fixedHandle != 0) fixStyle (fixedHandle);
}

void fixStyle (long /*int*/ handle) {
	/*
	* Feature in GTK.  Some GTK themes apply a different background to
	* the contents of a GtkNotebook.  However, in an SWT TabFolder, the
	* children are not parented below the GtkNotebook widget, and usually
	* have their own GtkFixed.  The fix is to look up the correct style
	* for a child of a GtkNotebook and apply its background to any GtkFixed
	* widgets that are direct children of an SWT TabFolder.
	*
	* Note that this has to be when the theme settings changes and that it
	* should not override the application background.
	*/
	if ((state & BACKGROUND) != 0) return;
	if ((state & THEME_BACKGROUND) == 0) return;
	if (!OS.GTK3) {
		long /*int*/ childStyle = parent.childStyle ();
		if (childStyle != 0) {
			GdkColor color = new GdkColor();
			OS.gtk_style_get_bg (childStyle, 0, color);
			setBackgroundColor (color);
		}
	}
}

long /*int*/ focusHandle () {
	return handle;
}

long /*int*/ fontHandle () {
	return handle;
}

/**
 * Returns the orientation of the receiver, which will be one of the
 * constants <code>SWT.LEFT_TO_RIGHT</code> or <code>SWT.RIGHT_TO_LEFT</code>.
 *
 * @return the orientation style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.7
 */
public int getOrientation () {
	checkWidget();
	return style & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT);
}

/**
 * Returns the text direction of the receiver, which will be one of the
 * constants <code>SWT.LEFT_TO_RIGHT</code> or <code>SWT.RIGHT_TO_LEFT</code>.
 *
 * @return the text direction style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.102
 */
public int getTextDirection() {
	checkWidget ();
	/* return the widget orientation */
	return style & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT);
}

boolean hasFocus () {
	return this == display.getFocusControl();
}

@Override
void hookEvents () {
	/* Connect the keyboard signals */
	long /*int*/ focusHandle = focusHandle ();
	int focusMask = OS.GDK_KEY_PRESS_MASK | OS.GDK_KEY_RELEASE_MASK | OS.GDK_FOCUS_CHANGE_MASK;
	OS.gtk_widget_add_events (focusHandle, focusMask);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [POPUP_MENU], 0, display.getClosure (POPUP_MENU), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [SHOW_HELP], 0, display.getClosure (SHOW_HELP), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [KEY_PRESS_EVENT], 0, display.getClosure (KEY_PRESS_EVENT), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [KEY_RELEASE_EVENT], 0, display.getClosure (KEY_RELEASE_EVENT), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [FOCUS], 0, display.getClosure (FOCUS), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [FOCUS_IN_EVENT], 0, display.getClosure (FOCUS_IN_EVENT), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [FOCUS_OUT_EVENT], 0, display.getClosure (FOCUS_OUT_EVENT), false);

	/* Connect the mouse signals */
	long /*int*/ eventHandle = eventHandle ();
	int eventMask = OS.GDK_POINTER_MOTION_MASK | OS.GDK_BUTTON_PRESS_MASK | OS.GDK_BUTTON_RELEASE_MASK | OS.GDK_SCROLL_MASK | OS.GDK_SMOOTH_SCROLL_MASK;
	OS.gtk_widget_add_events (eventHandle, eventMask);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [BUTTON_PRESS_EVENT], 0, display.getClosure (BUTTON_PRESS_EVENT), false);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [BUTTON_RELEASE_EVENT], 0, display.getClosure (BUTTON_RELEASE_EVENT), false);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [MOTION_NOTIFY_EVENT], 0, display.getClosure (MOTION_NOTIFY_EVENT), false);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [SCROLL_EVENT], 0, display.getClosure (SCROLL_EVENT), false);

	/* Connect enter/exit signals */
	long /*int*/ enterExitHandle = enterExitHandle ();
	int enterExitMask = OS.GDK_ENTER_NOTIFY_MASK | OS.GDK_LEAVE_NOTIFY_MASK;
	OS.gtk_widget_add_events (enterExitHandle, enterExitMask);
	OS.g_signal_connect_closure_by_id (enterExitHandle, display.signalIds [ENTER_NOTIFY_EVENT], 0, display.getClosure (ENTER_NOTIFY_EVENT), false);
	OS.g_signal_connect_closure_by_id (enterExitHandle, display.signalIds [LEAVE_NOTIFY_EVENT], 0, display.getClosure (LEAVE_NOTIFY_EVENT), false);

	/*
	* Feature in GTK.  Events such as mouse move are propagate up
	* the widget hierarchy and are seen by the parent.  This is the
	* correct GTK behavior but not correct for SWT.  The fix is to
	* hook a signal after and stop the propagation using a negative
	* event number to distinguish this case.
	*
	* The signal is hooked to the fixedHandle to catch events sent to
	* lightweight widgets.
	*/
	long /*int*/ blockHandle = fixedHandle != 0 ? fixedHandle : eventHandle;
	OS.g_signal_connect_closure_by_id (blockHandle, display.signalIds [BUTTON_PRESS_EVENT], 0, display.getClosure (BUTTON_PRESS_EVENT_INVERSE), true);
	OS.g_signal_connect_closure_by_id (blockHandle, display.signalIds [BUTTON_RELEASE_EVENT], 0, display.getClosure (BUTTON_RELEASE_EVENT_INVERSE), true);
	OS.g_signal_connect_closure_by_id (blockHandle, display.signalIds [MOTION_NOTIFY_EVENT], 0, display.getClosure (MOTION_NOTIFY_EVENT_INVERSE), true);

	/* Connect the event_after signal for both key and mouse */
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [EVENT_AFTER], 0, display.getClosure (EVENT_AFTER), false);
	if (focusHandle != eventHandle) {
		OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [EVENT_AFTER], 0, display.getClosure (EVENT_AFTER), false);
	}

	/* Connect the paint signal */
	long /*int*/ paintHandle = paintHandle ();
	int paintMask = OS.GDK_EXPOSURE_MASK;
	OS.gtk_widget_add_events (paintHandle, paintMask);

	OS.g_signal_connect_closure_by_id (paintHandle, display.signalIds [EXPOSE_EVENT], 0, display.getClosure (EXPOSE_EVENT_INVERSE), false);

	OS.g_signal_connect_closure_by_id (paintHandle, display.signalIds [EXPOSE_EVENT], 0, display.getClosure (EXPOSE_EVENT), true);

	/* Connect the Input Method signals */
	OS.g_signal_connect_closure_by_id (handle, display.signalIds [REALIZE], 0, display.getClosure (REALIZE), true);
	OS.g_signal_connect_closure_by_id (handle, display.signalIds [UNREALIZE], 0, display.getClosure (UNREALIZE), false);
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) {
		OS.g_signal_connect_closure (imHandle, OS.commit, display.getClosure (COMMIT), false);
		OS.g_signal_connect_closure (imHandle, OS.preedit_changed, display.getClosure (PREEDIT_CHANGED), false);
	}

	OS.g_signal_connect_closure_by_id (paintHandle, display.signalIds [STYLE_SET], 0, display.getClosure (STYLE_SET), false);

	long /*int*/ topHandle = topHandle ();
	OS.g_signal_connect_closure_by_id (topHandle, display.signalIds [MAP], 0, display.getClosure (MAP), true);

	if (enterNotifyEventFunc == null && OS.GTK3 && OS.GTK_VERSION < OS.VERSION (3, 11, 9)) {
		enterNotifyEventFunc = new Callback (Control.class, "enterNotifyEventProc", 4);
		if (enterNotifyEventFunc.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);

		enterNotifyEventSignalId = OS.g_signal_lookup (OS.enter_notify_event, OS.GTK_TYPE_WIDGET ());

		byte [] buffer = Converter.wcsToMbcs (null, "gtk-pointer-window", true);
		GTK_POINTER_WINDOW = OS.g_quark_from_string (buffer);
		buffer = Converter.wcsToMbcs (null, "swt-grab-widget", true);
		SWT_GRAB_WIDGET = OS.g_quark_from_string (buffer);
	}
}

boolean hooksPaint () {
	return hooks (SWT.Paint) || filters (SWT.Paint);
}

@Override
long /*int*/ hoverProc (long /*int*/ widget) {
	int [] x = new int [1], y = new int [1], mask = new int [1];
	gdk_window_get_device_position (0, x, y, mask);
	sendMouseEvent (SWT.MouseHover, 0, /*time*/0, x [0], y [0], false, mask [0]);
	/* Always return zero in order to cancel the hover timer */
	return 0;
}

@Override
long /*int*/ topHandle() {
	if (fixedHandle != 0) return fixedHandle;
	return super.topHandle ();
}

long /*int*/ paintHandle () {
	long /*int*/ topHandle = topHandle ();
	long /*int*/ paintHandle = handle;
	while (paintHandle != topHandle) {
		if (OS.gtk_widget_get_has_window (paintHandle)) break;
		paintHandle = OS.gtk_widget_get_parent (paintHandle);
	}
	return paintHandle;
}

@Override
long /*int*/ paintWindow () {
	long /*int*/ paintHandle = paintHandle ();
	OS.gtk_widget_realize (paintHandle);
	return gtk_widget_get_window (paintHandle);
}

/**
 * Prints the receiver and all children.
 *
 * @param gc the gc where the drawing occurs
 * @return <code>true</code> if the operation was successful and <code>false</code> otherwise
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the gc is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the gc has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public boolean print (GC gc) {
	checkWidget ();
	if (gc == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (gc.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
	long /*int*/ topHandle = topHandle ();
	OS.gtk_widget_realize (topHandle);
	if (OS.GTK3) {
		OS.gtk_widget_draw(topHandle, gc.handle);
		return true;
	}
	long /*int*/ window = gtk_widget_get_window (topHandle);
	GCData data = gc.getGCData ();
	OS.gdk_window_process_updates (window, true);
	long /*int*/ drawable = data.drawable;
	if (drawable == 0) drawable = OS.gdk_get_default_root_window();
	printWidget (gc, drawable, OS.gdk_drawable_get_depth (drawable), 0, 0);
	return true;
}

void printWidget (GC gc, long /*int*/ drawable, int depth, int x, int y) {
	boolean obscured = (state & OBSCURED) != 0;
	state &= ~OBSCURED;
	long /*int*/ topHandle = topHandle ();
	long /*int*/ window = gtk_widget_get_window (topHandle);
	printWindow (true, this, gc, drawable, depth, window, x, y);
	if (obscured) state |= OBSCURED;
}

void printWindow (boolean first, Control control, GC gc, long /*int*/ drawable, int depth, long /*int*/ window, int x, int y) {
	if (OS.gdk_drawable_get_depth (window) != depth) return;
	GdkRectangle rect = new GdkRectangle ();
	int [] width = new int [1], height = new int [1];
	gdk_window_get_size (window, width, height);
	rect.width = width [0];
	rect.height = height [0];
	OS.gdk_window_begin_paint_rect (window, rect);
	long /*int*/ [] real_drawable = new long /*int*/ [1];
	int [] x_offset = new int [1], y_offset = new int [1];
	OS.gdk_window_get_internal_paint_info (window, real_drawable, x_offset, y_offset);
	long /*int*/ [] userData = new long /*int*/ [1];
	OS.gdk_window_get_user_data (window, userData);
	if (userData [0] != 0) {
		long /*int*/ eventPtr = OS.gdk_event_new (OS.GDK_EXPOSE);
		GdkEventExpose event = new GdkEventExpose ();
		event.type = OS.GDK_EXPOSE;
		event.window = OS.g_object_ref (window);
		event.area_width = rect.width;
		event.area_height = rect.height;
		event.region = OS.gdk_region_rectangle (rect);
		OS.memmove (eventPtr, event, GdkEventExpose.sizeof);
		OS.gtk_widget_send_expose (userData [0], eventPtr);
		OS.gdk_event_free (eventPtr);
	}
	int srcX = x_offset [0], srcY = y_offset [0];
	int destX = x, destY = y, destWidth = width [0], destHeight = height [0];
	if (!first) {
		int [] cX = new int [1], cY = new int [1];
		OS.gdk_window_get_position (window, cX, cY);
		long /*int*/ parentWindow = OS.gdk_window_get_parent (window);
		int [] pW = new int [1], pH = new int [1];
		gdk_window_get_size (parentWindow, pW, pH);
		srcX = x_offset [0] - cX [0];
		srcY = y_offset [0] - cY [0];
		destX = x - cX [0];
		destY = y - cY [0];
		destWidth = Math.min (cX [0] + width [0], pW [0]);
		destHeight = Math.min (cY [0] + height [0], pH [0]);
	}
	GCData gcData = gc.getGCData();
	long /*int*/ cairo = gcData.cairo;
	if (cairo != 0) {
		long /*int*/ xDisplay = OS.gdk_x11_display_get_xdisplay(OS.gdk_display_get_default());
		long /*int*/ xVisual = OS.gdk_x11_visual_get_xvisual(OS.gdk_visual_get_system());
		long /*int*/ xDrawable = OS.GDK_PIXMAP_XID(real_drawable [0]);
		long /*int*/ surface = Cairo.cairo_xlib_surface_create(xDisplay, xDrawable, xVisual, width [0], height [0]);
		if (surface == 0) error(SWT.ERROR_NO_HANDLES);
		Cairo.cairo_save(cairo);
		Cairo.cairo_rectangle(cairo, destX , destY, destWidth, destHeight);
		Cairo.cairo_clip(cairo);
		Cairo.cairo_translate(cairo, destX, destY);
		long /*int*/ pattern = Cairo.cairo_pattern_create_for_surface(surface);
		if (pattern == 0) error(SWT.ERROR_NO_HANDLES);
		Cairo.cairo_pattern_set_filter(pattern, Cairo.CAIRO_FILTER_BEST);
		Cairo.cairo_set_source(cairo, pattern);
		if (gcData.alpha != 0xFF) {
			Cairo.cairo_paint_with_alpha(cairo, gcData.alpha / (float)0xFF);
		} else {
			Cairo.cairo_paint(cairo);
		}
		Cairo.cairo_restore(cairo);
		Cairo.cairo_pattern_destroy(pattern);
		Cairo.cairo_surface_destroy(surface);
	} else {
		OS.gdk_draw_drawable (drawable, gc.handle, real_drawable [0], srcX, srcY, destX, destY, destWidth, destHeight);
	}
	OS.gdk_window_end_paint (window);
	long /*int*/ children = OS.gdk_window_get_children (window);
	if (children != 0) {
		long /*int*/ windows = children;
		while (windows != 0) {
			long /*int*/ child = OS.g_list_data (windows);
			if (OS.gdk_window_is_visible (child)) {
				long /*int*/ [] data = new long /*int*/ [1];
				OS.gdk_window_get_user_data (child, data);
				if (data [0] != 0) {
					Widget widget = display.findWidget (data [0]);
					if (widget == null || widget == control) {
						int [] x_pos = new int [1], y_pos = new int [1];
						OS.gdk_window_get_position (child, x_pos, y_pos);
						printWindow (false, control, gc, drawable, depth, child, x + x_pos [0], y + y_pos [0]);
					}
				}
			}
			windows = OS.g_list_next (windows);
		}
		OS.g_list_free (children);
	}
}

/**
 * Returns the preferred size of the receiver.
 * <p>
 * The <em>preferred size</em> of a control is the size that it would
 * best be displayed at. The width hint and height hint arguments
 * allow the caller to ask a control questions such as "Given a particular
 * width, how high does the control need to be to show all of the contents?"
 * To indicate that the caller does not wish to constrain a particular
 * dimension, the constant <code>SWT.DEFAULT</code> is passed for the hint.
 * </p>
 *
 * @param wHint the width hint (can be <code>SWT.DEFAULT</code>)
 * @param hHint the height hint (can be <code>SWT.DEFAULT</code>)
 * @return the preferred size of the control
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Layout
 * @see #getBorderWidth
 * @see #getBounds
 * @see #getSize
 * @see #pack(boolean)
 * @see "computeTrim, getClientArea for controls that implement them"
 */
public Point computeSize (int wHint, int hHint) {
	return computeSize (wHint, hHint, true);
}

Widget computeTabGroup () {
	if (isTabGroup()) return this;
	return parent.computeTabGroup ();
}

Widget[] computeTabList() {
	if (isTabGroup()) {
		if (getVisible() && getEnabled()) {
			return new Widget[] {this};
		}
	}
	return new Widget[0];
}

Control computeTabRoot () {
	Control[] tabList = parent._getTabList();
	if (tabList != null) {
		int index = 0;
		while (index < tabList.length) {
			if (tabList [index] == this) break;
			index++;
		}
		if (index == tabList.length) {
			if (isTabGroup ()) return this;
		}
	}
	return parent.computeTabRoot ();
}

void checkBuffered () {
	style |= SWT.DOUBLE_BUFFERED;
}

void checkBackground () {
	Shell shell = getShell ();
	if (this == shell) return;
	state &= ~PARENT_BACKGROUND;
	Composite composite = parent;
	do {
		int mode = composite.backgroundMode;
		if (mode != SWT.INHERIT_NONE || backgroundAlpha == 0) {
			if (mode == SWT.INHERIT_DEFAULT || backgroundAlpha == 0) {
				Control control = this;
				do {
					if ((control.state & THEME_BACKGROUND) == 0) {
						return;
					}
					control = control.parent;
				} while (control != composite);
			}
			state |= PARENT_BACKGROUND;
			return;
		}
		if (composite == shell) break;
		composite = composite.parent;
	} while (true);
}

void checkForeground () {
	/*
	* Feature in GTK 3. The widget foreground is inherited from the immediate
	* parent. This is not the expected behavior for SWT. The fix is to avoid
	* the inheritance by explicitly setting the default foreground on the widget.
	*/
	if (OS.GTK3) {
		setForegroundColor (topHandle (), display.COLOR_WIDGET_FOREGROUND);
	}
}

void checkBorder () {
	if (getBorderWidth () == 0) style &= ~SWT.BORDER;
}

void checkMirrored () {
	if ((style & SWT.RIGHT_TO_LEFT) != 0) style |= SWT.MIRRORED;
}

long /*int*/ childStyle () {
	return parent.childStyle ();
}

@Override
void createWidget (int index) {
	state |= DRAG_DETECT;
	checkOrientation (parent);
	super.createWidget (index);
	checkBackground ();
	checkForeground ();
	if ((state & PARENT_BACKGROUND) != 0) setParentBackground ();
	checkBuffered ();
	showWidget ();
	setInitialBounds ();
	setZOrder (null, false, false);
	setRelations ();
	checkMirrored ();
	checkBorder ();
}

/**
 * Returns the preferred size of the receiver.
 * <p>
 * The <em>preferred size</em> of a control is the size that it would
 * best be displayed at. The width hint and height hint arguments
 * allow the caller to ask a control questions such as "Given a particular
 * width, how high does the control need to be to show all of the contents?"
 * To indicate that the caller does not wish to constrain a particular
 * dimension, the constant <code>SWT.DEFAULT</code> is passed for the hint.
 * </p><p>
 * If the changed flag is <code>true</code>, it indicates that the receiver's
 * <em>contents</em> have changed, therefore any caches that a layout manager
 * containing the control may have been keeping need to be flushed. When the
 * control is resized, the changed flag will be <code>false</code>, so layout
 * manager caches can be retained.
 * </p>
 *
 * @param wHint the width hint (can be <code>SWT.DEFAULT</code>)
 * @param hHint the height hint (can be <code>SWT.DEFAULT</code>)
 * @param changed <code>true</code> if the control's contents have changed, and <code>false</code> otherwise
 * @return the preferred size of the control.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Layout
 * @see #getBorderWidth
 * @see #getBounds
 * @see #getSize
 * @see #pack(boolean)
 * @see "computeTrim, getClientArea for controls that implement them"
 */
public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget();
	if (wHint != SWT.DEFAULT && wHint < 0) wHint = 0;
	if (hHint != SWT.DEFAULT && hHint < 0) hHint = 0;
	return computeNativeSize (handle, wHint, hHint, changed);
}

Point computeNativeSize (long /*int*/ h, int wHint, int hHint, boolean changed) {
	int width = wHint, height = hHint;
	if (OS.GTK3){
		if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
			GtkRequisition requisition = new GtkRequisition ();
			OS.gtk_widget_get_preferred_size (h, null, requisition);
			width = requisition.width;
			height = requisition.height;
		} else if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
			int [] natural_size = new int [1];
			if (wHint == SWT.DEFAULT) {
				OS.gtk_widget_get_preferred_width_for_height (h, height, null, natural_size);
				width = natural_size [0];
			} else {
				OS.gtk_widget_get_preferred_height_for_width (h, width, null, natural_size);
				height = natural_size [0];
			}
		}
		return new Point(width, height);
	}
	if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_size_request (h, requisition);
		width = OS.GTK_WIDGET_REQUISITION_WIDTH (h);
		height = OS.GTK_WIDGET_REQUISITION_HEIGHT (h);
	} else if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
		int [] reqWidth = new int [1], reqHeight = new int [1];
		OS.gtk_widget_get_size_request (h, reqWidth, reqHeight);
		OS.gtk_widget_set_size_request (h, wHint, hHint);
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_size_request (h, requisition);
		OS.gtk_widget_set_size_request (h, reqWidth [0], reqHeight [0]);
		width = wHint == SWT.DEFAULT ? requisition.width : wHint;
		height = hHint == SWT.DEFAULT ? requisition.height : hHint;
	}
	return new Point (width, height);
}

void forceResize () {
	/*
	* Force size allocation on all children of this widget's
	* topHandle.  Note that all calls to gtk_widget_size_allocate()
	* must be preceded by a call to gtk_widget_size_request().
	*/
	long /*int*/ topHandle = topHandle ();
	GtkRequisition requisition = new GtkRequisition ();
	gtk_widget_size_request (topHandle, requisition);
	GtkAllocation allocation = new GtkAllocation ();
	OS.gtk_widget_get_allocation(topHandle, allocation);
	OS.gtk_widget_size_allocate (topHandle, allocation);
}

/**
 * Returns the accessible object for the receiver.
 * <p>
 * If this is the first time this object is requested,
 * then the object is created and returned. The object
 * returned by getAccessible() does not need to be disposed.
 * </p>
 *
 * @return the accessible object
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Accessible#addAccessibleListener
 * @see Accessible#addAccessibleControlListener
 *
 * @since 2.0
 */
public Accessible getAccessible () {
	checkWidget ();
	return _getAccessible ();
}

Accessible _getAccessible () {
	if (accessible == null) {
		accessible = Accessible.internal_new_Accessible (this);
	}
	return accessible;
}

/**
 * Returns a rectangle describing the receiver's size and location
 * relative to its parent (or its display if its parent is null),
 * unless the receiver is a shell. In this case, the location is
 * relative to the display.
 *
 * @return the receiver's bounding rectangle
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Rectangle getBounds () {
	checkWidget();
	long /*int*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	OS.gtk_widget_get_allocation (topHandle, allocation);
	int x = allocation.x;
	int y = allocation.y;
	int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
	int height = (state & ZERO_HEIGHT) != 0 ? 0 :allocation.height;
	if ((parent.style & SWT.MIRRORED) != 0) x = parent.getClientWidth () - width - x;
	return new Rectangle (x, y, width, height);
}

/**
 * Sets the receiver's size and location to the rectangular
 * area specified by the argument. The <code>x</code> and
 * <code>y</code> fields of the rectangle are relative to
 * the receiver's parent (or its display if its parent is null).
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause that
 * value to be set to zero instead.
 * </p>
  *<p>
 * Note: Attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param rect the new bounds for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setBounds (Rectangle rect) {
	checkWidget ();
	if (rect == null) error (SWT.ERROR_NULL_ARGUMENT);
	setBounds (rect.x, rect.y, Math.max (0, rect.width), Math.max (0, rect.height), true, true);
}

/**
 * Sets the receiver's size and location to the rectangular
 * area specified by the arguments. The <code>x</code> and
 * <code>y</code> arguments are relative to the receiver's
 * parent (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the <code>x</code>
 * and <code>y</code> arguments are relative to the display.
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause that
 * value to be set to zero instead.
 * </p>
  *<p>
 * Note: Attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param x the new x coordinate for the receiver
 * @param y the new y coordinate for the receiver
 * @param width the new width for the receiver
 * @param height the new height for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setBounds (int x, int y, int width, int height) {
	checkWidget();
	setBounds (x, y, Math.max (0, width), Math.max (0, height), true, true);
}

void markLayout (boolean changed, boolean all) {
	/* Do nothing */
}

@Override
void modifyStyle (long /*int*/ handle, long /*int*/ style) {
	super.modifyStyle(handle, style);
	/*
	* Bug in GTK.  When changing the style of a control that
	* has had a region set on it, the region is lost.  The
	* fix is to set the region again.
	*/
	if (region != null) OS.gdk_window_shape_combine_region (gtk_widget_get_window (topHandle ()), region.handle, 0, 0);
}

void moveHandle (int x, int y) {
	long /*int*/ topHandle = topHandle ();
	long /*int*/ parentHandle = parent.parentingHandle ();
	if (OS.GTK3) {
		OS.swt_fixed_move (parentHandle, topHandle, x, y);
	} else {
		/*
		* Feature in GTK.  Calling gtk_fixed_move() to move a child causes
		* the whole parent to redraw.  This is a performance problem. The
		* fix is temporarily mark the parent not visible during the move.
		*
		* NOTE: Because every widget in SWT has an X window, the new and
		* old bounds of the child are correctly redrawn.
		*
		* NOTE: There is no API in GTK 3 to only set the GTK_VISIBLE bit.
		*/
		boolean reset = OS.gtk_widget_get_visible (parentHandle);
		gtk_widget_set_visible (parentHandle, false);
		OS.gtk_fixed_move (parentHandle, topHandle, x, y);
		gtk_widget_set_visible (parentHandle, reset);
	}
}

void resizeHandle (int width, int height) {
	long /*int*/ topHandle = topHandle ();
	if (OS.GTK3) {
		OS.swt_fixed_resize (OS.gtk_widget_get_parent (topHandle), topHandle, width, height);
		if (topHandle != handle) {
			OS.swt_fixed_resize (OS.gtk_widget_get_parent (handle), handle, width, height);
		}
	} else {
		OS.gtk_widget_set_size_request (topHandle, width, height);
		if (topHandle != handle) OS.gtk_widget_set_size_request (handle, width, height);
	}
}

int setBounds (int x, int y, int width, int height, boolean move, boolean resize) {
	// bug in GTK2 crashes JVM, in GTK3 the new shell only. See bug 472743
	width = Math.min(width, (2 << 14) - 1);
	height = Math.min(height, (2 << 14) - 1);

	long /*int*/ topHandle = topHandle ();
	boolean sendMove = move;
	GtkAllocation allocation = new GtkAllocation ();
	OS.gtk_widget_get_allocation (topHandle, allocation);
	if ((parent.style & SWT.MIRRORED) != 0) {
		int clientWidth = parent.getClientWidth ();
		int oldWidth = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		int oldX = clientWidth - oldWidth - allocation.x;
		if (move) {
			sendMove &= x != oldX;
			x = clientWidth - (resize ? width : oldWidth) - x;
		} else {
			move = true;
			x = clientWidth - (resize ? width : oldWidth) - oldX;
			y = allocation.y;
		}
	}
	boolean sameOrigin = true, sameExtent = true;
	if (move) {
		int oldX = allocation.x;
		int oldY = allocation.y;
		sameOrigin = x == oldX && y == oldY;
		if (!sameOrigin) {
			if (enableWindow != 0) {
				OS.gdk_window_move (enableWindow, x, y);
			}
			moveHandle (x, y);
		}
	}
	int clientWidth = 0;
	if (resize) {
		int oldWidth = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		int oldHeight = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
		sameExtent = width == oldWidth && height == oldHeight;
		if (!sameExtent && (style & SWT.MIRRORED) != 0) clientWidth = getClientWidth ();
		if (!sameExtent && !(width == 0 && height == 0)) {
			int newWidth = Math.max (1, width);
			int newHeight = Math.max (1, height);
			if (redrawWindow != 0) {
				OS.gdk_window_resize (redrawWindow, newWidth, newHeight);
			}
			if (enableWindow != 0) {
				OS.gdk_window_resize (enableWindow, newWidth, newHeight);
			}
			resizeHandle (newWidth, newHeight);
		}
	}
	if (!sameOrigin || !sameExtent) {
		/*
		* Cause a size allocation this widget's topHandle.  Note that
		* all calls to gtk_widget_size_allocate() must be preceded by
		* a call to gtk_widget_size_request().
		*/
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_size_request (topHandle, requisition);
		if (move) {
			allocation.x = x;
			allocation.y = y;
		}
		if (resize) {
			allocation.width = width;
			allocation.height = height;
		}
		/*
		 * The widget needs to be shown before its size is allocated
		 * in GTK 3.8 otherwise its allocation return 0
		 */
		if (OS.GTK_VERSION >= OS.VERSION (3, 8, 0) && !OS.gtk_widget_get_visible(handle))  {
			OS.gtk_widget_show(handle);
			OS.gtk_widget_size_allocate (topHandle, allocation);
			OS.gtk_widget_hide(handle);
		} else {
			OS.gtk_widget_size_allocate (topHandle, allocation);
		}
	}
	/*
	* Bug in GTK.  Widgets cannot be sized smaller than 1x1.
	* The fix is to hide zero-sized widgets and show them again
	* when they are resized larger.
	*/
	if (!sameExtent) {
		state = (width == 0) ? state | ZERO_WIDTH : state & ~ZERO_WIDTH;
		state = (height == 0) ? state | ZERO_HEIGHT : state & ~ZERO_HEIGHT;
		if ((state & (ZERO_WIDTH | ZERO_HEIGHT)) != 0) {
			if (enableWindow != 0) {
				OS.gdk_window_hide (enableWindow);
			}
			OS.gtk_widget_hide (topHandle);
		} else {
			if ((state & HIDDEN) == 0) {
				if (enableWindow != 0) {
					OS.gdk_window_show_unraised (enableWindow);
				}
				OS.gtk_widget_show (topHandle);
			}
		}
		if ((style & SWT.MIRRORED) != 0) moveChildren (clientWidth);
	}
	int result = 0;
	if (move && !sameOrigin) {
		Control control = findBackgroundControl ();
		if (control != null && control.backgroundImage != null) {
			if (isVisible ()) redrawWidget (0, 0, 0, 0, true, true, true);
		}
		if (sendMove) sendEvent (SWT.Move);
		result |= MOVED;
	}
	if (resize && !sameExtent) {
		sendEvent (SWT.Resize);
		result |= RESIZED;
	}
	return result;
}

/**
 * Returns a point describing the receiver's location relative
 * to its parent (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the point is
 * relative to the display.
 *
 * @return the receiver's location
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point getLocation () {
	checkWidget();
	long /*int*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	OS.gtk_widget_get_allocation (topHandle, allocation);
	int x = allocation.x;
	int y = allocation.y;
	if ((parent.style & SWT.MIRRORED) != 0) {
		int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		x = parent.getClientWidth () - width - x;
	}
	return new Point (x, y);
}

/**
 * Sets the receiver's location to the point specified by
 * the arguments which are relative to the receiver's
 * parent (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the point is
 * relative to the display.
 *
 * @param location the new location for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLocation (Point location) {
	checkWidget ();
	if (location == null) error (SWT.ERROR_NULL_ARGUMENT);
	setBounds (location.x, location.y, 0, 0, true, false);
}

/**
 * Sets the receiver's location to the point specified by
 * the arguments which are relative to the receiver's
 * parent (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the point is
 * relative to the display.
 *
 * @param x the new x coordinate for the receiver
 * @param y the new y coordinate for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLocation(int x, int y) {
	checkWidget();
	setBounds (x, y, 0, 0, true, false);
}

/**
 * Returns a point describing the receiver's size. The
 * x coordinate of the result is the width of the receiver.
 * The y coordinate of the result is the height of the
 * receiver.
 *
 * @return the receiver's size
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point getSize () {
	checkWidget();
	long /*int*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	OS.gtk_widget_get_allocation (topHandle, allocation);
	int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
	int height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
	return new Point (width, height);
}

/**
 * Sets the receiver's size to the point specified by the argument.
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause them to be
 * set to zero instead.
 * </p>
  *<p>
 * Note: Attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param size the new size for the receiver
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSize (Point size) {
	checkWidget ();
	if (size == null) error (SWT.ERROR_NULL_ARGUMENT);
	setBounds (0, 0, Math.max (0, size.x), Math.max (0, size.y), false, true);
}

/**
 * Sets the shape of the control to the region specified
 * by the argument.  When the argument is null, the
 * default shape of the control is restored.
 *
 * @param region the region that defines the shape of the control (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the region has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public void setRegion (Region region) {
	checkWidget ();
	if (region != null && region.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	long /*int*/ window = gtk_widget_get_window (topHandle ());
	long /*int*/ shape_region = (region == null) ? 0 : region.handle;
	OS.gdk_window_shape_combine_region (window, shape_region, 0, 0);
	this.region = region;
}

void setRelations () {
	long /*int*/ parentHandle = parent.parentingHandle ();
	long /*int*/ list = OS.gtk_container_get_children (parentHandle);
	if (list == 0) return;
	int count = OS.g_list_length (list);
	if (count > 1) {
		/*
		 * the receiver is the last item in the list, so its predecessor will
		 * be the second-last item in the list
		 */
		long /*int*/ handle = OS.g_list_nth_data (list, count - 2);
		if (handle != 0) {
			Widget widget = display.getWidget (handle);
			if (widget != null && widget != this) {
				if (widget instanceof Control) {
					Control sibling = (Control)widget;
					sibling.addRelation (this);
				}
			}
		}
	}
	OS.g_list_free (list);
}

/**
 * Sets the receiver's size to the point specified by the arguments.
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause that
 * value to be set to zero instead.
 * </p>
  *<p>
 * Note: Attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param width the new width for the receiver
 * @param height the new height for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSize (int width, int height) {
	checkWidget();
	setBounds (0, 0, Math.max (0, width), Math.max (0, height), false, true);
}


@Override
boolean isActive () {
	return getShell ().getModalShell () == null && display.getModalDialog () == null;
}

/*
 * Answers a boolean indicating whether a Label that precedes the receiver in
 * a layout should be read by screen readers as the recevier's label.
 */
boolean isDescribedByLabel () {
	return true;
}

boolean isFocusHandle (long /*int*/ widget) {
	return widget == focusHandle ();
}

/**
 * Moves the receiver above the specified control in the
 * drawing order. If the argument is null, then the receiver
 * is moved to the top of the drawing order. The control at
 * the top of the drawing order will not be covered by other
 * controls even if they occupy intersecting areas.
 *
 * @param control the sibling control (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the control has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Control#moveBelow
 * @see Composite#getChildren
 */
public void moveAbove (Control control) {
	checkWidget();
	if (control != null) {
		if (control.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
		if (parent != control.parent) return;
		if (this == control) return;
	}
	setZOrder (control, true, true);
}

/**
 * Moves the receiver below the specified control in the
 * drawing order. If the argument is null, then the receiver
 * is moved to the bottom of the drawing order. The control at
 * the bottom of the drawing order will be covered by all other
 * controls which occupy intersecting areas.
 *
 * @param control the sibling control (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the control has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Control#moveAbove
 * @see Composite#getChildren
 */
public void moveBelow (Control control) {
	checkWidget();
	if (control != null) {
		if (control.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
		if (parent != control.parent) return;
		if (this == control) return;
	}
	setZOrder (control, false, true);
}

void moveChildren (int oldWidth) {
}

/**
 * Causes the receiver to be resized to its preferred size.
 * For a composite, this involves computing the preferred size
 * from its layout, if there is one.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #computeSize(int, int, boolean)
 */
public void pack () {
	pack (true);
}

/**
 * Causes the receiver to be resized to its preferred size.
 * For a composite, this involves computing the preferred size
 * from its layout, if there is one.
 * <p>
 * If the changed flag is <code>true</code>, it indicates that the receiver's
 * <em>contents</em> have changed, therefore any caches that a layout manager
 * containing the control may have been keeping need to be flushed. When the
 * control is resized, the changed flag will be <code>false</code>, so layout
 * manager caches can be retained.
 * </p>
 *
 * @param changed whether or not the receiver's contents have changed
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #computeSize(int, int, boolean)
 */
public void pack (boolean changed) {
	setSize (computeSize (SWT.DEFAULT, SWT.DEFAULT, changed));
}

/**
 * Sets the layout data associated with the receiver to the argument.
 *
 * @param layoutData the new layout data for the receiver.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLayoutData (Object layoutData) {
	checkWidget();
	this.layoutData = layoutData;
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in display relative coordinates,
 * to coordinates relative to the receiver.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param x the x coordinate to be translated
 * @param y the y coordinate to be translated
 * @return the translated coordinates
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 2.1
 */
public Point toControl (int x, int y) {
	checkWidget ();
	long /*int*/ window = eventWindow ();
	int [] origin_x = new int [1], origin_y = new int [1];
	OS.gdk_window_get_origin (window, origin_x, origin_y);
	x -= origin_x [0];
	y -= origin_y [0];
	if ((style & SWT.MIRRORED) != 0) x = getClientWidth () - x;
	return new Point (x, y);
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in display relative coordinates,
 * to coordinates relative to the receiver.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param point the point to be translated (must not be null)
 * @return the translated coordinates
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point toControl (Point point) {
	checkWidget ();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	return toControl (point.x, point.y);
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in coordinates relative to
 * the receiver, to display relative coordinates.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param x the x coordinate to be translated
 * @param y the y coordinate to be translated
 * @return the translated coordinates
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 2.1
 */
public Point toDisplay (int x, int y) {
	checkWidget();
	long /*int*/ window = eventWindow ();
	int [] origin_x = new int [1], origin_y = new int [1];
	OS.gdk_window_get_origin (window, origin_x, origin_y);
	if ((style & SWT.MIRRORED) != 0) x = getClientWidth () - x;
	x += origin_x [0];
	y += origin_y [0];
	return new Point (x, y);
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in coordinates relative to
 * the receiver, to display relative coordinates.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param point the point to be translated (must not be null)
 * @return the translated coordinates
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point toDisplay (Point point) {
	checkWidget();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	return toDisplay (point.x, point.y);
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
	addListener (SWT.Resize,typedListener);
	addListener (SWT.Move,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when a drag gesture occurs, by sending it
 * one of the messages defined in the <code>DragDetectListener</code>
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
 * @see DragDetectListener
 * @see #removeDragDetectListener
 *
 * @since 3.3
 */
public void addDragDetectListener (DragDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.DragDetect,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the control gains or loses focus, by sending
 * it one of the messages defined in the <code>FocusListener</code>
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
 * @see FocusListener
 * @see #removeFocusListener
 */
public void addFocusListener(FocusListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.FocusIn,typedListener);
	addListener(SWT.FocusOut,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when gesture events are generated for the control,
 * by sending it one of the messages defined in the
 * <code>GestureListener</code> interface.
 * <p>
 * NOTE: If <code>setTouchEnabled(true)</code> has previously been
 * invoked on the receiver then <code>setTouchEnabled(false)</code>
 * must be invoked on it to specify that gesture events should be
 * sent instead of touch events.
 * </p>
 * <p>
 * <b>Warning</b>: This API is currently only implemented on Windows and Cocoa.
 * SWT doesn't send Gesture or Touch events on GTK.
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
 * @see GestureListener
 * @see #removeGestureListener
 * @see #setTouchEnabled
 *
 * @since 3.7
 */
public void addGestureListener (GestureListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Gesture, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when help events are generated for the control,
 * by sending it one of the messages defined in the
 * <code>HelpListener</code> interface.
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
 * @see HelpListener
 * @see #removeHelpListener
 */
public void addHelpListener (HelpListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Help, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when keys are pressed and released on the system keyboard, by sending
 * it one of the messages defined in the <code>KeyListener</code>
 * interface.
 * <p>
 * When a key listener is added to a control, the control
 * will take part in widget traversal.  By default, all
 * traversal keys (such as the tab key and so on) are
 * delivered to the control.  In order for a control to take
 * part in traversal, it should listen for traversal events.
 * Otherwise, the user can traverse into a control but not
 * out.  Note that native controls such as table and tree
 * implement key traversal in the operating system.  It is
 * not necessary to add traversal listeners for these controls,
 * unless you want to override the default traversal.
 * </p>
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
 * @see KeyListener
 * @see #removeKeyListener
 */
public void addKeyListener(KeyListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.KeyUp,typedListener);
	addListener(SWT.KeyDown,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the platform-specific context menu trigger
 * has occurred, by sending it one of the messages defined in
 * the <code>MenuDetectListener</code> interface.
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
 * @see MenuDetectListener
 * @see #removeMenuDetectListener
 *
 * @since 3.3
 */
public void addMenuDetectListener (MenuDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.MenuDetect, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when mouse buttons are pressed and released, by sending
 * it one of the messages defined in the <code>MouseListener</code>
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
 * @see MouseListener
 * @see #removeMouseListener
 */
public void addMouseListener(MouseListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.MouseDown,typedListener);
	addListener(SWT.MouseUp,typedListener);
	addListener(SWT.MouseDoubleClick,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the mouse moves, by sending it one of the
 * messages defined in the <code>MouseMoveListener</code>
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
 * @see MouseMoveListener
 * @see #removeMouseMoveListener
 */
public void addMouseMoveListener(MouseMoveListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.MouseMove,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the mouse passes or hovers over controls, by sending
 * it one of the messages defined in the <code>MouseTrackListener</code>
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
 * @see MouseTrackListener
 * @see #removeMouseTrackListener
 */
public void addMouseTrackListener (MouseTrackListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.MouseEnter,typedListener);
	addListener (SWT.MouseExit,typedListener);
	addListener (SWT.MouseHover,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the mouse wheel is scrolled, by sending
 * it one of the messages defined in the
 * <code>MouseWheelListener</code> interface.
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
 * @see MouseWheelListener
 * @see #removeMouseWheelListener
 *
 * @since 3.3
 */
public void addMouseWheelListener (MouseWheelListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.MouseWheel, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver needs to be painted, by sending it
 * one of the messages defined in the <code>PaintListener</code>
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
 * @see PaintListener
 * @see #removePaintListener
 */
public void addPaintListener(PaintListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.Paint,typedListener);
}

void addRelation (Control control) {
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when touch events occur, by sending it
 * one of the messages defined in the <code>TouchListener</code>
 * interface.
 * <p>
 * NOTE: You must also call <code>setTouchEnabled(true)</code> to
 * specify that touch events should be sent, which will cause gesture
 * events to not be sent.
 * </p>
 * <p>
 * <b>Warning</b>: This API is currently only implemented on Windows and Cocoa.
 * SWT doesn't send Gesture or Touch events on GTK.
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
 * @see TouchListener
 * @see #removeTouchListener
 * @see #setTouchEnabled
 *
 * @since 3.7
 */
public void addTouchListener (TouchListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Touch,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when traversal events occur, by sending it
 * one of the messages defined in the <code>TraverseListener</code>
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
 * @see TraverseListener
 * @see #removeTraverseListener
 */
public void addTraverseListener (TraverseListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Traverse,typedListener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is moved or resized.
 *
 * @param listener the listener which should no longer be notified
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
	eventTable.unhook (SWT.Resize, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when a drag gesture occurs.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see DragDetectListener
 * @see #addDragDetectListener
 *
 * @since 3.3
 */
public void removeDragDetectListener(DragDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.DragDetect, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control gains or loses focus.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see FocusListener
 * @see #addFocusListener
 */
public void removeFocusListener(FocusListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.FocusIn, listener);
	eventTable.unhook (SWT.FocusOut, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when gesture events are generated for the control.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see GestureListener
 * @see #addGestureListener
 *
 * @since 3.7
 */
public void removeGestureListener (GestureListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook(SWT.Gesture, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the help events are generated for the control.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see HelpListener
 * @see #addHelpListener
 */
public void removeHelpListener (HelpListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Help, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when keys are pressed and released on the system keyboard.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see KeyListener
 * @see #addKeyListener
 */
public void removeKeyListener(KeyListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.KeyUp, listener);
	eventTable.unhook (SWT.KeyDown, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the platform-specific context menu trigger has
 * occurred.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see MenuDetectListener
 * @see #addMenuDetectListener
 *
 * @since 3.3
 */
public void removeMenuDetectListener (MenuDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MenuDetect, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when mouse buttons are pressed and released.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see MouseListener
 * @see #addMouseListener
 */
public void removeMouseListener (MouseListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseDown, listener);
	eventTable.unhook (SWT.MouseUp, listener);
	eventTable.unhook (SWT.MouseDoubleClick, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the mouse moves.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see MouseMoveListener
 * @see #addMouseMoveListener
 */
public void removeMouseMoveListener(MouseMoveListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseMove, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the mouse passes or hovers over controls.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see MouseTrackListener
 * @see #addMouseTrackListener
 */
public void removeMouseTrackListener(MouseTrackListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseEnter, listener);
	eventTable.unhook (SWT.MouseExit, listener);
	eventTable.unhook (SWT.MouseHover, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the mouse wheel is scrolled.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see MouseWheelListener
 * @see #addMouseWheelListener
 *
 * @since 3.3
 */
public void removeMouseWheelListener (MouseWheelListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseWheel, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the receiver needs to be painted.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see PaintListener
 * @see #addPaintListener
 */
public void removePaintListener(PaintListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook(SWT.Paint, listener);
}

/*
 * Remove "Labelled by" relation from the receiver.
 */
void removeRelation () {
	if (!isDescribedByLabel ()) return;		/* there will not be any */
	if (labelRelation != null) {
		_getAccessible().removeRelation (ACC.RELATION_LABELLED_BY, labelRelation._getAccessible());
		labelRelation = null;
	}
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when touch events occur.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see TouchListener
 * @see #addTouchListener
 *
 * @since 3.7
 */
public void removeTouchListener(TouchListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Touch, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when traversal events occur.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see TraverseListener
 * @see #addTraverseListener
 */
public void removeTraverseListener(TraverseListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Traverse, listener);
}

/**
 * Detects a drag and drop gesture.  This method is used
 * to detect a drag gesture when called from within a mouse
 * down listener.
 *
 * <p>By default, a drag is detected when the gesture
 * occurs anywhere within the client area of a control.
 * Some controls, such as tables and trees, override this
 * behavior.  In addition to the operating system specific
 * drag gesture, they require the mouse to be inside an
 * item.  Custom widget writers can use <code>setDragDetect</code>
 * to disable the default detection, listen for mouse down,
 * and then call <code>dragDetect()</code> from within the
 * listener to conditionally detect a drag.
 * </p>
 *
 * @param event the mouse down event
 *
 * @return <code>true</code> if the gesture occurred, and <code>false</code> otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see DragDetectListener
 * @see #addDragDetectListener
 *
 * @see #getDragDetect
 * @see #setDragDetect
 *
 * @since 3.3
 */
public boolean dragDetect (Event event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return dragDetect (event.button, event.count, event.stateMask, event.x, event.y);
}

/**
 * Detects a drag and drop gesture.  This method is used
 * to detect a drag gesture when called from within a mouse
 * down listener.
 *
 * <p>By default, a drag is detected when the gesture
 * occurs anywhere within the client area of a control.
 * Some controls, such as tables and trees, override this
 * behavior.  In addition to the operating system specific
 * drag gesture, they require the mouse to be inside an
 * item.  Custom widget writers can use <code>setDragDetect</code>
 * to disable the default detection, listen for mouse down,
 * and then call <code>dragDetect()</code> from within the
 * listener to conditionally detect a drag.
 * </p>
 *
 * @param event the mouse down event
 *
 * @return <code>true</code> if the gesture occurred, and <code>false</code> otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see DragDetectListener
 * @see #addDragDetectListener
 *
 * @see #getDragDetect
 * @see #setDragDetect
 *
 * @since 3.3
 */
public boolean dragDetect (MouseEvent event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return dragDetect (event.button, event.count, event.stateMask, event.x, event.y);
}

boolean dragDetect (int button, int count, int stateMask, int x, int y) {
	if (button != 1 || count != 1) return false;
	if (!dragDetect (x, y, false, true, null)) return false;
	return sendDragEvent (button, stateMask, x, y, true);
}

boolean dragDetect (int x, int y, boolean filter, boolean dragOnTimeout, boolean [] consume) {
	boolean quit = false, dragging = false;

	//428852 DND workaround for GTk3.
	//Gtk3 no longer sends motion events on the same control during thread sleep
	//before a drag started. This is due to underlying gdk changes.
	//Thus for gtk3 we check mouse coords manually
	//Note, input params x/y are relative, the two points below are absolute coords.
	Point startPos = null;
	Point currPos = null;
	if (OS.GTK3) {
		startPos = display.getCursorLocation();
	}

	while (!quit) {
		long /*int*/ eventPtr = 0;
		/*
		* There should be an event on the queue already, but
		* in cases where there isn't one, stop trying after
		* half a second.
		*/
		long timeout = System.currentTimeMillis() + 500;
		display.sendPreExternalEventDispatchEvent();
		while (System.currentTimeMillis() < timeout) {
			eventPtr = OS.gdk_event_get ();
			if (eventPtr != 0) {
				break;
			} else {
				if (OS.GTK3) { //428852
					currPos = display.getCursorLocation();
					dragging = OS.gtk_drag_check_threshold (handle,
								startPos.x, startPos.y, currPos.x, currPos.y);
					if (dragging) break;
				} else {
				try {Thread.sleep(50);}
				catch (Exception ex) {}
				}
			}
		}
		display.sendPostExternalEventDispatchEvent();
		if (dragging) return true;  //428852
		if (eventPtr == 0) return dragOnTimeout;
		switch (OS.GDK_EVENT_TYPE (eventPtr)) {
			case OS.GDK_MOTION_NOTIFY: {
				GdkEventMotion gdkMotionEvent = new GdkEventMotion ();
				OS.memmove (gdkMotionEvent, eventPtr, GdkEventMotion.sizeof);
				if ((gdkMotionEvent.state & OS.GDK_BUTTON1_MASK) != 0) {
					if (OS.gtk_drag_check_threshold (handle, x, y, (int) gdkMotionEvent.x, (int) gdkMotionEvent.y)) {
						dragging = true;
						quit = true;
					}
				} else {
					quit = true;
				}
				int [] newX = new int [1], newY = new int [1];
				gdk_window_get_device_position (gdkMotionEvent.window, newX, newY, null);
				break;
			}
			case OS.GDK_KEY_PRESS:
			case OS.GDK_KEY_RELEASE: {
				GdkEventKey gdkEvent = new GdkEventKey ();
				OS.memmove (gdkEvent, eventPtr, GdkEventKey.sizeof);
				if (gdkEvent.keyval == OS.GDK_Escape) quit = true;
				break;
			}
			case OS.GDK_BUTTON_RELEASE:
			case OS.GDK_BUTTON_PRESS:
			case OS.GDK_2BUTTON_PRESS:
			case OS.GDK_3BUTTON_PRESS: {
				OS.gdk_event_put (eventPtr);
				quit = true;
				break;
			}
			default:
				OS.gtk_main_do_event (eventPtr);
		}
		OS.gdk_event_free (eventPtr);
	}
	return dragging;
}

boolean filterKey (int keyval, long /*int*/ event) {
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) {
		return OS.gtk_im_context_filter_keypress (imHandle, event);
	}
	return false;
}

Control findBackgroundControl () {
	if (((state & BACKGROUND) != 0 || backgroundImage != null) && backgroundAlpha > 0) return this;
	return (state & PARENT_BACKGROUND) != 0 ? parent.findBackgroundControl () : null;
}

Menu [] findMenus (Control control) {
	if (menu != null && this != control) return new Menu [] {menu};
	return new Menu [0];
}

void fixChildren (Shell newShell, Shell oldShell, Decorations newDecorations, Decorations oldDecorations, Menu [] menus) {
	oldShell.fixShell (newShell, this);
	oldDecorations.fixDecorations (newDecorations, this, menus);
}

@Override
long /*int*/ fixedMapProc (long /*int*/ widget) {
	gtk_widget_set_mapped (widget, true);
	long /*int*/ widgetList = OS.gtk_container_get_children (widget);
	if (widgetList != 0) {
		long /*int*/ widgets = widgetList;
		while (widgets != 0) {
			long /*int*/ child = OS.g_list_data (widgets);
			if (OS.gtk_widget_get_visible (child) && OS.gtk_widget_get_child_visible (child) && !gtk_widget_get_mapped (child)) {
				OS.gtk_widget_map (child);
			}
			widgets = OS.g_list_next (widgets);
		}
		OS.g_list_free (widgetList);
	}
	if (OS.gtk_widget_get_has_window (widget)) {
		OS.gdk_window_show_unraised (gtk_widget_get_window (widget));
	}
	return 0;
}

void fixModal(long /*int*/ group, long /*int*/ modalGroup) {
}

/**
 * Forces the receiver to have the <em>keyboard focus</em>, causing
 * all keyboard events to be delivered to it.
 *
 * @return <code>true</code> if the control got focus, and <code>false</code> if it was unable to.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setFocus
 */
public boolean forceFocus () {
	checkWidget();
	if (display.focusEvent == SWT.FocusOut) return false;
	Shell shell = getShell ();
	shell.setSavedFocus (this);
	if (!isEnabled () || !isVisible ()) return false;
	shell.bringToTop (false);
	return forceFocus (focusHandle ());
}

boolean forceFocus (long /*int*/ focusHandle) {
	if (OS.gtk_widget_has_focus (focusHandle)) return true;
	/* When the control is zero sized it must be realized */
	OS.gtk_widget_realize (focusHandle);
	OS.gtk_widget_grab_focus (focusHandle);
	// widget could be disposed at this point
	if (isDisposed ()) return false;
	Shell shell = getShell ();
	long /*int*/ shellHandle = shell.shellHandle;
	long /*int*/ handle = OS.gtk_window_get_focus (shellHandle);
	while (handle != 0) {
		if (handle == focusHandle) {
			/* Cancel any previous ignoreFocus requests */
			display.ignoreFocus = false;
			return true;
		}
		Widget widget = display.getWidget (handle);
		if (widget != null && widget instanceof Control) {
			return widget == this;
		}
		handle = OS.gtk_widget_get_parent (handle);
	}
	return false;
}

/**
 * Returns the receiver's background color.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * For example, on some versions of Windows the background of a TabFolder,
 * is a gradient rather than a solid color.
 * </p>
 * @return the background color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Color getBackground () {
	checkWidget();
	if (backgroundAlpha == 0) {
		Color color = Color.gtk_new (display, this.getBackgroundColor (), 0);
		return color;
	}
	else {
		Control control = findBackgroundControl ();
		if (control == null) control = this;
		return Color.gtk_new (display, control.getBackgroundColor (), backgroundAlpha);
	}
}

GdkColor getBackgroundColor () {
	return getBgColor ();
}

/**
 * Returns the receiver's background image.
 *
 * @return the background image
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.2
 */
public Image getBackgroundImage () {
	checkWidget ();
	Control control = findBackgroundControl ();
	if (control == null) control = this;
	return control.backgroundImage;
}

GdkColor getContextBackground () {
	long /*int*/ fontHandle = fontHandle ();
	long /*int*/ context = OS.gtk_widget_get_style_context (fontHandle);
	GdkRGBA rgba = new GdkRGBA ();
	OS.gtk_style_context_get_background_color (context, OS.GTK_STATE_FLAG_NORMAL, rgba);
	if (rgba.alpha == 0) {
		return display.COLOR_WIDGET_BACKGROUND;
	}
	GdkColor color = new GdkColor ();
	color.red = (short)(rgba.red * 0xFFFF);
	color.green = (short)(rgba.green * 0xFFFF);
	color.blue = (short)(rgba.blue * 0xFFFF);
	return color;
}

GdkColor getContextColor () {
	long /*int*/ fontHandle = fontHandle ();
	long /*int*/ context = OS.gtk_widget_get_style_context (fontHandle);
	GdkRGBA rgba = new GdkRGBA ();
	rgba = display.styleContextGetColor (context, OS.GTK_STATE_FLAG_NORMAL, rgba);
	GdkColor color = new GdkColor ();
	color.red = (short)(rgba.red * 0xFFFF);
	color.green = (short)(rgba.green * 0xFFFF);
	color.blue = (short)(rgba.blue * 0xFFFF);
	return color;
}

GdkColor getBgColor () {
	if (OS.GTK3) {
		return getContextBackground ();
	}
	long /*int*/ fontHandle = fontHandle ();
	OS.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();

	long /*int*/ style = OS.gtk_widget_get_style (fontHandle);
	if (style != 0){
		OS.gtk_style_get_bg (style, OS.GTK_STATE_NORMAL, color);
	}

	return color;
}

GdkColor getBaseColor () {
	if (OS.GTK3) {
		return getContextBackground ();
	}
	long /*int*/ fontHandle = fontHandle ();
	OS.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();
	OS.gtk_style_get_base (OS.gtk_widget_get_style (fontHandle), OS.GTK_STATE_NORMAL, color);
	return color;
}

/**
 * Returns the receiver's border width.
 *
 * @return the border width
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getBorderWidth () {
	checkWidget();
	return 0;
}

int getClientWidth () {
	if (handle == 0 || (state & ZERO_WIDTH) != 0) return 0;
	GtkAllocation allocation = new GtkAllocation();
	OS.gtk_widget_get_allocation (handle, allocation);
	return allocation.width;
}

/**
 * Returns the receiver's cursor, or null if it has not been set.
 * <p>
 * When the mouse pointer passes over a control its appearance
 * is changed to match the control's cursor.
 * </p>
 *
 * @return the receiver's cursor or <code>null</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.3
 */
public Cursor getCursor () {
	checkWidget ();
	return cursor;
}

/**
 * Returns <code>true</code> if the receiver is detecting
 * drag gestures, and  <code>false</code> otherwise.
 *
 * @return the receiver's drag detect state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.3
 */
public boolean getDragDetect () {
	checkWidget ();
	return (state & DRAG_DETECT) != 0;
}

/**
 * Returns <code>true</code> if the receiver is enabled, and
 * <code>false</code> otherwise. A disabled control is typically
 * not selectable from the user interface and draws with an
 * inactive or "grayed" look.
 *
 * @return the receiver's enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #isEnabled
 */
public boolean getEnabled () {
	checkWidget ();
	return (state & DISABLED) == 0;
}

/**
 * Returns the font that the receiver will use to paint textual information.
 *
 * @return the receiver's font
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Font getFont () {
	checkWidget();
	return font != null ? font : defaultFont ();
}

long /*int*/ getFontDescription () {
	long /*int*/ fontHandle = fontHandle ();
	if (OS.GTK3) {
		long /*int*/ context = OS.gtk_widget_get_style_context (fontHandle);
		return OS.gtk_style_context_get_font(context, OS.GTK_STATE_FLAG_NORMAL);
	}
	OS.gtk_widget_realize (fontHandle);
	return OS.gtk_style_get_font_desc (OS.gtk_widget_get_style (fontHandle));
}

/**
 * Returns the foreground color that the receiver will use to draw.
 *
 * @return the receiver's foreground color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Color getForeground () {
	checkWidget();
	return Color.gtk_new (display, getForegroundColor ());
}

GdkColor getForegroundColor () {
	return getFgColor ();
}

GdkColor getFgColor () {
	if (OS.GTK3) {
		return getContextColor ();
	}
	long /*int*/ fontHandle = fontHandle ();
	OS.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();

	long /*int*/ style = OS.gtk_widget_get_style (fontHandle);
	if (style != 0){
		OS.gtk_style_get_fg (style, OS.GTK_STATE_NORMAL, color);
	}
	return color;
}

Point getIMCaretPos () {
	return new Point (0, 0);
}

GdkColor getTextColor () {
	if (OS.GTK3) {
		return getContextColor ();
	}
	long /*int*/ fontHandle = fontHandle ();
	OS.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();
	OS.gtk_style_get_text (OS.gtk_widget_get_style (fontHandle), OS.GTK_STATE_NORMAL, color);
	return color;
}

/**
 * Returns layout data which is associated with the receiver.
 *
 * @return the receiver's layout data
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Object getLayoutData () {
	checkWidget();
	return layoutData;
}

/**
 * Returns the receiver's pop up menu if it has one, or null
 * if it does not. All controls may optionally have a pop up
 * menu that is displayed when the user requests one for
 * the control. The sequence of key strokes, button presses
 * and/or button releases that are used to request a pop up
 * menu is platform specific.
 *
 * @return the receiver's menu
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Menu getMenu () {
	checkWidget();
	return menu;
}

/**
 * Returns the receiver's monitor.
 *
 * @return the receiver's monitor
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.0
 */
public Monitor getMonitor () {
	checkWidget ();
	long /*int*/ screen = OS.gdk_screen_get_default ();
	if (screen != 0) {
		int monitorNumber = OS.gdk_screen_get_monitor_at_window (screen, paintWindow ());
		Monitor[] monitors = display.getMonitors ();

		if (monitorNumber >= 0 && monitorNumber < monitors.length) {
			return monitors [monitorNumber];
		}
	}
	return display.getPrimaryMonitor ();
}

/**
 * Returns the receiver's parent, which must be a <code>Composite</code>
 * or null when the receiver is a shell that was created with null or
 * a display for a parent.
 *
 * @return the receiver's parent
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Composite getParent () {
	checkWidget();
	return parent;
}

Control [] getPath () {
	int count = 0;
	Shell shell = getShell ();
	Control control = this;
	while (control != shell) {
		count++;
		control = control.parent;
	}
	control = this;
	Control [] result = new Control [count];
	while (control != shell) {
		result [--count] = control;
		control = control.parent;
	}
	return result;
}

/**
 * Returns the region that defines the shape of the control,
 * or null if the control has the default shape.
 *
 * @return the region that defines the shape of the shell (or null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public Region getRegion () {
	checkWidget ();
	return region;
}

/**
 * Returns the receiver's shell. For all controls other than
 * shells, this simply returns the control's nearest ancestor
 * shell. Shells return themselves, even if they are children
 * of other shells.
 *
 * @return the receiver's shell
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #getParent
 */
public Shell getShell() {
	checkWidget();
	return _getShell();
}

Shell _getShell() {
	return parent._getShell();
}

/**
 * Returns the receiver's tool tip text, or null if it has
 * not been set.
 *
 * @return the receiver's tool tip text
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public String getToolTipText () {
	checkWidget();
	return toolTipText;
}

/**
 * Returns <code>true</code> if this control is set to send touch events, or
 * <code>false</code> if it is set to send gesture events instead.  This method
 * also returns <code>false</code> if a touch-based input device is not detected
 * (this can be determined with <code>Display#getTouchEnabled()</code>).  Use
 * {@link #setTouchEnabled(boolean)} to switch the events that a control sends
 * between touch events and gesture events.
 *
 * @return <code>true</code> if the control is set to send touch events, or <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setTouchEnabled
 * @see Display#getTouchEnabled
 *
 * @since 3.7
 */
public boolean getTouchEnabled() {
	checkWidget();
	return false;
}

/**
 * Returns <code>true</code> if the receiver is visible, and
 * <code>false</code> otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, this method
 * may still indicate that it is considered visible even though
 * it may not actually be showing.
 * </p>
 *
 * @return the receiver's visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getVisible () {
	checkWidget();
	return (state & HIDDEN) == 0;
}

Point getThickness (long /*int*/ widget) {
	if (OS.GTK3) {
		int xthickness = 0, ythickness = 0;
		GtkBorder tmp = new GtkBorder();
		long /*int*/ context = OS.gtk_widget_get_style_context (widget);
		OS.gtk_style_context_save (context);
		OS.gtk_style_context_add_class (context, OS.GTK_STYLE_CLASS_FRAME);
		OS.gtk_style_context_get_padding (context, OS.GTK_STATE_FLAG_NORMAL, tmp);
		xthickness += tmp.left;
		ythickness += tmp.top;
		OS.gtk_style_context_get_border (context, OS.GTK_STATE_FLAG_NORMAL, tmp);
		xthickness += tmp.left;
		ythickness += tmp.top;
		OS.gtk_style_context_restore (context);
		return new Point (xthickness, ythickness);
	}
	long /*int*/ style = OS.gtk_widget_get_style (widget);
	return new Point (OS.gtk_style_get_xthickness (style), OS.gtk_style_get_ythickness (style));
}

@Override
long /*int*/ gtk_button_press_event (long /*int*/ widget, long /*int*/ event) {
	return gtk_button_press_event (widget, event, true);
}

long /*int*/ gtk_button_press_event (long /*int*/ widget, long /*int*/ event, boolean sendMouseDown) {
	GdkEventButton gdkEvent = new GdkEventButton ();
	OS.memmove (gdkEvent, event, GdkEventButton.sizeof);
	if (gdkEvent.type == OS.GDK_3BUTTON_PRESS) return 0;

	/*
	* When a shell is created with SWT.ON_TOP and SWT.NO_FOCUS,
	* do not activate the shell when the user clicks on the
	* the client area or on the border or a control within the
	* shell that does not take focus.
	*/
	Shell shell = _getShell ();
	if (((shell.style & SWT.ON_TOP) != 0) && (((shell.style & SWT.NO_FOCUS) == 0) || ((style & SWT.NO_FOCUS) == 0))) {
		shell.forceActive();
	}
	long /*int*/ result = 0;
	if (gdkEvent.type == OS.GDK_BUTTON_PRESS) {
		display.clickCount = 1;
		long /*int*/ nextEvent = OS.gdk_event_peek ();
		if (nextEvent != 0) {
			int eventType = OS.GDK_EVENT_TYPE (nextEvent);
			if (eventType == OS.GDK_2BUTTON_PRESS) display.clickCount = 2;
			if (eventType == OS.GDK_3BUTTON_PRESS) display.clickCount = 3;
			OS.gdk_event_free (nextEvent);
		}
		boolean dragging = false;
		if ((state & DRAG_DETECT) != 0 && hooks (SWT.DragDetect)) {
			if (gdkEvent.button == 1) {
				boolean [] consume = new boolean [1];
				if (dragDetect ((int) gdkEvent.x, (int) gdkEvent.y, true, true, consume)) {
					dragging = true;
					if (consume [0]) result = 1;
				}
				if (isDisposed ()) return 1;
			}
		}
		if (sendMouseDown && !sendMouseEvent (SWT.MouseDown, gdkEvent.button, display.clickCount, 0, false, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state)) {
			result = 1;
		}
		if (isDisposed ()) return 1;
		if (dragging) {
			sendDragEvent (gdkEvent.button, gdkEvent.state, (int) gdkEvent.x, (int) gdkEvent.y, false);
			if (isDisposed ()) return 1;
		}
		/*
		* Pop up the context menu in the button press event for widgets
		* that have default operating system menus in order to stop the
		* operating system from displaying the menu if necessary.
		*/
		if ((state & MENU) != 0) {
			if (gdkEvent.button == 3) {
				if (showMenu ((int)gdkEvent.x_root, (int)gdkEvent.y_root)) {
					result = 1;
				}
			}
		}
	} else {
		display.clickCount = 2;
		result = sendMouseEvent (SWT.MouseDoubleClick, gdkEvent.button, display.clickCount, 0, false, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		if (isDisposed ()) return 1;
	}
	if (!shell.isDisposed ()) shell.setActiveControl (this, SWT.MouseDown);
	return result;
}

@Override
long /*int*/ gtk_button_release_event (long /*int*/ widget, long /*int*/ event) {
	GdkEventButton gdkEvent = new GdkEventButton ();
	OS.memmove (gdkEvent, event, GdkEventButton.sizeof);
	return sendMouseEvent (SWT.MouseUp, gdkEvent.button, display.clickCount, 0, false, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
}

@Override
long /*int*/ gtk_commit (long /*int*/ imcontext, long /*int*/ text) {
	if (text == 0) return 0;
	int length = OS.strlen (text);
	if (length == 0) return 0;
	byte [] buffer = new byte [length];
	OS.memmove (buffer, text, length);
	char [] chars = Converter.mbcsToWcs (null, buffer);
	sendIMKeyEvent (SWT.KeyDown, null, chars);
	return 0;
}

@Override
long /*int*/ gtk_enter_notify_event (long /*int*/ widget, long /*int*/ event) {
	/*
	 * Feature in GTK. Children of a shell will inherit and display the shell's
	 * tooltip if they do not have a tooltip of their own. The fix is to use the
	 * new tooltip API in GTK 2.12 to null the shell's tooltip when the control
	 * being entered does not have any tooltip text set.
	 */
	byte [] buffer = null;
	if (toolTipText != null && toolTipText.length() != 0) {
		char [] chars = fixMnemonic (toolTipText, false);
		buffer = Converter.wcsToMbcs (null, chars, true);
	}
	long /*int*/ toolHandle = getShell().handle;
	OS.gtk_widget_set_tooltip_text (toolHandle, buffer);

	if (display.currentControl == this) return 0;
	GdkEventCrossing gdkEvent = new GdkEventCrossing ();
	OS.memmove (gdkEvent, event, GdkEventCrossing.sizeof);
	/*
	 * It is possible to send out too many enter/exit events if entering a
	 * control through a subwindow. The fix is to return without sending any
	 * events if the GdkEventCrossing subwindow field is set and the control
	 * requests to check the field.
	 */
	if (gdkEvent.subwindow != 0 && checkSubwindow ()) return 0;
	if (gdkEvent.mode != OS.GDK_CROSSING_NORMAL && gdkEvent.mode != OS.GDK_CROSSING_UNGRAB) return 0;
	if ((gdkEvent.state & (OS.GDK_BUTTON1_MASK | OS.GDK_BUTTON2_MASK | OS.GDK_BUTTON3_MASK)) != 0) return 0;
	if (display.currentControl != null && !display.currentControl.isDisposed ()) {
		display.removeMouseHoverTimeout (display.currentControl.handle);
		display.currentControl.sendMouseEvent (SWT.MouseExit,  0, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state);
	}
	if (!isDisposed ()) {
		display.currentControl = this;
		return sendMouseEvent (SWT.MouseEnter, 0, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
	}
	return 0;
}

boolean checkSubwindow () {
	return false;
}

@Override
long /*int*/ gtk_event_after (long /*int*/ widget, long /*int*/ gdkEvent) {
	GdkEvent event = new GdkEvent ();
	OS.memmove (event, gdkEvent, GdkEvent.sizeof);
	switch (event.type) {
		case OS.GDK_BUTTON_PRESS: {
			if (widget != eventHandle ()) break;
			/*
			* Pop up the context menu in the event_after signal to allow
			* the widget to process the button press.  This allows widgets
			* such as GtkTreeView to select items before a menu is shown.
			*/
			if ((state & MENU) == 0) {
				GdkEventButton gdkEventButton = new GdkEventButton ();
				OS.memmove (gdkEventButton, gdkEvent, GdkEventButton.sizeof);
				if (gdkEventButton.button == 3) {
					showMenu ((int) gdkEventButton.x_root, (int) gdkEventButton.y_root);
				}
			}
			break;
		}
		case OS.GDK_FOCUS_CHANGE: {
			if (!isFocusHandle (widget)) break;
			GdkEventFocus gdkEventFocus = new GdkEventFocus ();
			OS.memmove (gdkEventFocus, gdkEvent, GdkEventFocus.sizeof);

			/*
			 * Feature in GTK. The GTK combo box popup under some window managers
			 * is implemented as a GTK_MENU.  When it pops up, it causes the combo
			 * box to lose focus when focus is received for the menu.  The
			 * fix is to check the current grab handle and see if it is a GTK_MENU
			 * and ignore the focus event when the menu is both shown and hidden.
			 *
			 * NOTE: This code runs for all menus.
			 */
			Display display = this.display;
			if (gdkEventFocus.in != 0) {
				if (display.ignoreFocus) {
					display.ignoreFocus = false;
					break;
				}
			} else {
				display.ignoreFocus = false;
				long /*int*/ grabHandle = OS.gtk_grab_get_current ();
				if (grabHandle != 0) {
					if (OS.G_OBJECT_TYPE (grabHandle) == OS.GTK_TYPE_MENU ()) {
						display.ignoreFocus = true;
						break;
					}
				}
			}

			sendFocusEvent (gdkEventFocus.in != 0 ? SWT.FocusIn : SWT.FocusOut);
			break;
		}
	}
	return 0;
}

@Override
long /*int*/ gtk_draw (long /*int*/ widget, long /*int*/ cairo) {
	if ((state & OBSCURED) != 0) return 0;
	if (!hooksPaint ()) return 0;
	GdkRectangle rect = new GdkRectangle ();
	OS.gdk_cairo_get_clip_rectangle (cairo, rect);
	Event event = new Event ();
	event.count = 1;
	event.x = rect.x;
	event.y = rect.y;
	event.width = rect.width;
	event.height = rect.height;
	if ((style & SWT.MIRRORED) != 0) event.x = getClientWidth () - event.width - event.x;
	GCData data = new GCData ();
//	data.damageRgn = gdkEvent.region;
	if (OS.GTK_VERSION <= OS.VERSION (3, 9, 0)) {
		data.cairo = cairo;
	}
	GC gc = event.gc = GC.gtk_new (this, data);
	gc.setClipping (rect.x, rect.y, rect.width, rect.height);
	drawWidget (gc);
	sendEvent (SWT.Paint, event);
	gc.dispose ();
	event.gc = null;
	return 0;
}

@Override
long /*int*/ gtk_expose_event (long /*int*/ widget, long /*int*/ eventPtr) {
	if ((state & OBSCURED) != 0) return 0;
	if (!hooksPaint ()) return 0;
	GdkEventExpose gdkEvent = new GdkEventExpose ();
	OS.memmove(gdkEvent, eventPtr, GdkEventExpose.sizeof);
	Event event = new Event ();
	event.count = gdkEvent.count;
	event.x = gdkEvent.area_x;
	event.y = gdkEvent.area_y;
	event.width = gdkEvent.area_width;
	event.height = gdkEvent.area_height;
	if ((style & SWT.MIRRORED) != 0) event.x = getClientWidth () - event.width - event.x;
	GCData data = new GCData ();
	data.damageRgn = gdkEvent.region;
	GC gc = event.gc = GC.gtk_new (this, data);
	drawWidget (gc);
	sendEvent (SWT.Paint, event);
	gc.dispose ();
	event.gc = null;
	return 0;
}

@Override
long /*int*/ gtk_focus (long /*int*/ widget, long /*int*/ directionType) {
	/* Stop GTK traversal for every widget */
	return 1;
}

@Override
long /*int*/ gtk_focus_in_event (long /*int*/ widget, long /*int*/ event) {
	// widget could be disposed at this point
	if (handle != 0) {
		Control oldControl = display.imControl;
		if (oldControl != this)  {
			if (oldControl != null && !oldControl.isDisposed ()) {
				long /*int*/ oldIMHandle = oldControl.imHandle ();
				if (oldIMHandle != 0) OS.gtk_im_context_reset (oldIMHandle);
			}
		}
		if (hooks (SWT.KeyDown) || hooks (SWT.KeyUp)) {
			long /*int*/ imHandle = imHandle ();
			if (imHandle != 0) OS.gtk_im_context_focus_in (imHandle);
		}
	}
	return 0;
}

@Override
long /*int*/ gtk_focus_out_event (long /*int*/ widget, long /*int*/ event) {
	// widget could be disposed at this point
	if (handle != 0) {
		if (hooks (SWT.KeyDown) || hooks (SWT.KeyUp)) {
			long /*int*/ imHandle = imHandle ();
			if (imHandle != 0) {
				OS.gtk_im_context_focus_out (imHandle);
			}
		}
	}
	return 0;
}

@Override
long /*int*/ gtk_key_press_event (long /*int*/ widget, long /*int*/ event) {
	if (!hasFocus ()) {
		/*
		* Feature in GTK.  On AIX, the IME window deactivates the current shell and even
		* though the widget receiving the key event is not in focus, it should filter the input in
		* order to get it committed.  The fix is to detect that the widget shell is not active
		* and call filterKey() only.
		*/
		if (display.getActiveShell () == null) {
			GdkEventKey gdkEvent = new GdkEventKey ();
			OS.memmove (gdkEvent, event, GdkEventKey.sizeof);
			if (filterKey (gdkEvent.keyval, event)) return 1;
		}
		return 0;
	}
	GdkEventKey gdkEvent = new GdkEventKey ();
	OS.memmove (gdkEvent, event, GdkEventKey.sizeof);

	if (translateMnemonic (gdkEvent.keyval, gdkEvent)) return 1;
	// widget could be disposed at this point
	if (isDisposed ()) return 0;

	if (filterKey (gdkEvent.keyval, event)) return 1;
	// widget could be disposed at this point
	if (isDisposed ()) return 0;

	if (translateTraversal (gdkEvent)) return 1;
	// widget could be disposed at this point
	if (isDisposed ()) return 0;
	return super.gtk_key_press_event (widget, event);
}

@Override
long /*int*/ gtk_key_release_event (long /*int*/ widget, long /*int*/ event) {
	if (!hasFocus ()) return 0;
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) {
		if (OS.gtk_im_context_filter_keypress (imHandle, event)) return 1;
	}
	return super.gtk_key_release_event (widget, event);
}

@Override
long /*int*/ gtk_leave_notify_event (long /*int*/ widget, long /*int*/ event) {
	if (display.currentControl != this) return 0;
	display.removeMouseHoverTimeout (handle);
	int result = 0;
	if (sendLeaveNotify () || display.getCursorControl () == null) {
		GdkEventCrossing gdkEvent = new GdkEventCrossing ();
		OS.memmove (gdkEvent, event, GdkEventCrossing.sizeof);
		if (gdkEvent.mode != OS.GDK_CROSSING_NORMAL && gdkEvent.mode != OS.GDK_CROSSING_UNGRAB) return 0;
		if ((gdkEvent.state & (OS.GDK_BUTTON1_MASK | OS.GDK_BUTTON2_MASK | OS.GDK_BUTTON3_MASK)) != 0) return 0;
		result = sendMouseEvent (SWT.MouseExit, 0, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		display.currentControl = null;
	}
	return result;
}

@Override
long /*int*/ gtk_mnemonic_activate (long /*int*/ widget, long /*int*/ arg1) {
	int result = 0;
	long /*int*/ eventPtr = OS.gtk_get_current_event ();
	if (eventPtr != 0) {
		GdkEventKey keyEvent = new GdkEventKey ();
		OS.memmove (keyEvent, eventPtr, GdkEventKey.sizeof);
		if (keyEvent.type == OS.GDK_KEY_PRESS) {
			Control focusControl = display.getFocusControl ();
			long /*int*/ focusHandle = focusControl != null ? focusControl.focusHandle () : 0;
			if (focusHandle != 0) {
				display.mnemonicControl = this;
				OS.gtk_widget_event (focusHandle, eventPtr);
				display.mnemonicControl = null;
			}
			result = 1;
		}
		OS.gdk_event_free (eventPtr);
	}
	return result;
}

@Override
long /*int*/ gtk_motion_notify_event (long /*int*/ widget, long /*int*/ event) {
	GdkEventMotion gdkEvent = new GdkEventMotion ();
	OS.memmove (gdkEvent, event, GdkEventMotion.sizeof);
	if (this == display.currentControl && (hooks (SWT.MouseHover) || filters (SWT.MouseHover))) {
		display.addMouseHoverTimeout (handle);
	}
	double x = gdkEvent.x_root, y = gdkEvent.y_root;
	int state = gdkEvent.state;
	if (gdkEvent.is_hint != 0) {
		int [] pointer_x = new int [1], pointer_y = new int [1], mask = new int [1];
		long /*int*/ window = eventWindow ();
		gdk_window_get_device_position (window, pointer_x, pointer_y, mask);
		x = pointer_x [0];
		y = pointer_y [0];
		state = mask [0];
	}
	if (OS.GTK3 && this != display.currentControl) {
		if (display.currentControl != null && !display.currentControl.isDisposed ()) {
			display.removeMouseHoverTimeout (display.currentControl.handle);
			display.currentControl.sendMouseEvent (SWT.MouseExit,  0, gdkEvent.time, x, y, false, state);
		}
		if (!isDisposed ()) {
			display.currentControl = this;
			sendMouseEvent (SWT.MouseEnter, 0, gdkEvent.time, x, y, false, state);
		}
	}
	int result = sendMouseEvent (SWT.MouseMove, 0, gdkEvent.time, x, y, gdkEvent.is_hint != 0, state) ? 0 : 1;
	return result;
}

@Override
long /*int*/ gtk_popup_menu (long /*int*/ widget) {
	if (!hasFocus()) return 0;
	int [] x = new int [1], y = new int [1];
	gdk_window_get_device_position (0, x, y, null);
	return showMenu (x [0], y [0], SWT.MENU_KEYBOARD) ? 1 : 0;
}

@Override
long /*int*/ gtk_preedit_changed (long /*int*/ imcontext) {
	display.showIMWindow (this);
	return 0;
}

@Override
long /*int*/ gtk_realize (long /*int*/ widget) {
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) {
		long /*int*/ window = gtk_widget_get_window (paintHandle ());
		OS.gtk_im_context_set_client_window (imHandle, window);
	}
	if (backgroundImage != null) {
		setBackgroundPixmap (backgroundImage);
	}
	return 0;
}

@Override
long /*int*/ gtk_scroll_event (long /*int*/ widget, long /*int*/ eventPtr) {
	GdkEventScroll gdkEvent = new GdkEventScroll ();
	OS.memmove (gdkEvent, eventPtr, GdkEventScroll.sizeof);
	switch (gdkEvent.direction) {
		case OS.GDK_SCROLL_UP:
			return sendMouseEvent (SWT.MouseWheel, 0, 3, SWT.SCROLL_LINE, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case OS.GDK_SCROLL_DOWN:
			return sendMouseEvent (SWT.MouseWheel, 0, -3, SWT.SCROLL_LINE, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case OS.GDK_SCROLL_LEFT:
			return sendMouseEvent (SWT.MouseHorizontalWheel, 0, 3, 0, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case OS.GDK_SCROLL_RIGHT:
			return sendMouseEvent (SWT.MouseHorizontalWheel, 0, -3, 0, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case OS.GDK_SCROLL_SMOOTH:
			long /*int*/ result = 0;
			double[] delta_x = new double[1], delta_y = new double [1];
			if (OS.gdk_event_get_scroll_deltas (eventPtr, delta_x, delta_y)) {
				if (delta_x [0] != 0) {
					result = (sendMouseEvent (SWT.MouseHorizontalWheel, 0, (int)(-3 * delta_x [0]), 0, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1);
				}
				if (delta_y [0] != 0) {
					result = (sendMouseEvent (SWT.MouseWheel, 0, (int)(-3 * delta_y [0]), SWT.SCROLL_LINE, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1);
				}
			}
			return result;
	}
	return 0;
}

@Override
long /*int*/ gtk_show_help (long /*int*/ widget, long /*int*/ helpType) {
	if (!hasFocus ()) return 0;
	return sendHelpEvent (helpType) ? 1 : 0;
}

@Override
long /*int*/ gtk_style_set (long /*int*/ widget, long /*int*/ previousStyle) {
	if (backgroundImage != null) {
		setBackgroundPixmap (backgroundImage);
	}
	return 0;
}

@Override
long /*int*/ gtk_unrealize (long /*int*/ widget) {
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) OS.gtk_im_context_set_client_window (imHandle, 0);
	return 0;
}

void gtk_widget_size_request (long /*int*/ widget, GtkRequisition requisition) {
	gtk_widget_get_preferred_size (widget, requisition);
}

/**
 * Invokes platform specific functionality to allocate a new GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Control</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param data the platform specific GC data
 * @return the platform specific GC handle
 *
 * @noreference This method is not intended to be referenced by clients.
 */
public long /*int*/ internal_new_GC (GCData data) {
	checkWidget ();
	long /*int*/ window = paintWindow ();
	if (window == 0) error (SWT.ERROR_NO_HANDLES);
	long /*int*/ gc = data.cairo;
	if (gc != 0) {
		Cairo.cairo_reference (gc);
	} else {
		if (OS.USE_CAIRO) {
			gc = OS.gdk_cairo_create (window);
		} else {
			gc = OS.gdk_gc_new (window);
		}
	}
	if (gc == 0) error (SWT.ERROR_NO_HANDLES);
	if (data != null) {
		int mask = SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		if ((data.style & mask) == 0) {
			data.style |= style & (mask | SWT.MIRRORED);
		} else {
			if ((data.style & SWT.RIGHT_TO_LEFT) != 0) {
				data.style |= SWT.MIRRORED;
			}
		}
		data.drawable = window;
		data.device = display;
		data.foreground = getForegroundColor ();
		Control control = findBackgroundControl ();
		if (control == null) control = this;
		data.background = control.getBackgroundColor ();
		data.font = font != null ? font : defaultFont ();
	}
	return gc;
}

long /*int*/ imHandle () {
	return 0;
}

/**
 * Invokes platform specific functionality to dispose a GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Control</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param hDC the platform specific GC handle
 * @param data the platform specific GC data
 *
 * @noreference This method is not intended to be referenced by clients.
 */
public void internal_dispose_GC (long /*int*/ hDC, GCData data) {
	checkWidget ();
	long /*int*/ gc = hDC;
	if (OS.USE_CAIRO) {
		Cairo.cairo_destroy (gc);
	} else {
		OS.g_object_unref (gc);
	}
}

/**
 * Returns <code>true</code> if the underlying operating
 * system supports this reparenting, otherwise <code>false</code>
 *
 * @return <code>true</code> if the widget can be reparented, otherwise <code>false</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean isReparentable () {
	checkWidget();
	return true;
}
boolean isShowing () {
	/*
	* This is not complete.  Need to check if the
	* widget is obscurred by a parent or sibling.
	*/
	if (!isVisible ()) return false;
	Control control = this;
	while (control != null) {
		Point size = control.getSize ();
		if (size.x == 0 || size.y == 0) {
			return false;
		}
		control = control.parent;
	}
	return true;
}
boolean isTabGroup () {
	Control [] tabList = parent._getTabList ();
	if (tabList != null) {
		for (int i=0; i<tabList.length; i++) {
			if (tabList [i] == this) return true;
		}
	}
	int code = traversalCode (0, null);
	if ((code & (SWT.TRAVERSE_ARROW_PREVIOUS | SWT.TRAVERSE_ARROW_NEXT)) != 0) return false;
	return (code & (SWT.TRAVERSE_TAB_PREVIOUS | SWT.TRAVERSE_TAB_NEXT)) != 0;
}
boolean isTabItem () {
	Control [] tabList = parent._getTabList ();
	if (tabList != null) {
		for (int i=0; i<tabList.length; i++) {
			if (tabList [i] == this) return false;
		}
	}
	int code = traversalCode (0, null);
	return (code & (SWT.TRAVERSE_ARROW_PREVIOUS | SWT.TRAVERSE_ARROW_NEXT)) != 0;
}

/**
 * Returns <code>true</code> if the receiver is enabled and all
 * ancestors up to and including the receiver's nearest ancestor
 * shell are enabled.  Otherwise, <code>false</code> is returned.
 * A disabled control is typically not selectable from the user
 * interface and draws with an inactive or "grayed" look.
 *
 * @return the receiver's enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #getEnabled
 */
public boolean isEnabled () {
	checkWidget ();
	return getEnabled () && parent.isEnabled ();
}

boolean isFocusAncestor (Control control) {
	while (control != null && control != this && !(control instanceof Shell)) {
		control = control.parent;
	}
	return control == this;
}

/**
 * Returns <code>true</code> if the receiver has the user-interface
 * focus, and <code>false</code> otherwise.
 *
 * @return the receiver's focus state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean isFocusControl () {
	checkWidget();
	Control focusControl = display.focusControl;
	if (focusControl != null && !focusControl.isDisposed ()) {
		return this == focusControl;
	}
	return hasFocus ();
}

/**
 * Returns <code>true</code> if the receiver is visible and all
 * ancestors up to and including the receiver's nearest ancestor
 * shell are visible. Otherwise, <code>false</code> is returned.
 *
 * @return the receiver's visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #getVisible
 */
public boolean isVisible () {
	checkWidget();
	return getVisible () && parent.isVisible ();
}

Decorations menuShell () {
	return parent.menuShell ();
}

boolean mnemonicHit (char key) {
	return false;
}

boolean mnemonicMatch (char key) {
	return false;
}

@Override
void register () {
	super.register ();
	if (fixedHandle != 0) display.addWidget (fixedHandle, this);
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) display.addWidget (imHandle, this);
}

/**
 * Causes the entire bounds of the receiver to be marked
 * as needing to be redrawn. The next time a paint request
 * is processed, the control will be completely painted,
 * including the background.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #update()
 * @see PaintListener
 * @see SWT#Paint
 * @see SWT#NO_BACKGROUND
 * @see SWT#NO_REDRAW_RESIZE
 * @see SWT#NO_MERGE_PAINTS
 * @see SWT#DOUBLE_BUFFERED
 */
public void redraw () {
	checkWidget();
	redraw (false);
}

void redraw (boolean all) {
//	checkWidget();
	if (!OS.gtk_widget_get_visible (topHandle ())) return;
	redrawWidget (0, 0, 0, 0, true, all, false);
}

/**
 * Causes the rectangular area of the receiver specified by
 * the arguments to be marked as needing to be redrawn.
 * The next time a paint request is processed, that area of
 * the receiver will be painted, including the background.
 * If the <code>all</code> flag is <code>true</code>, any
 * children of the receiver which intersect with the specified
 * area will also paint their intersecting areas. If the
 * <code>all</code> flag is <code>false</code>, the children
 * will not be painted.
 *
 * @param x the x coordinate of the area to draw
 * @param y the y coordinate of the area to draw
 * @param width the width of the area to draw
 * @param height the height of the area to draw
 * @param all <code>true</code> if children should redraw, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #update()
 * @see PaintListener
 * @see SWT#Paint
 * @see SWT#NO_BACKGROUND
 * @see SWT#NO_REDRAW_RESIZE
 * @see SWT#NO_MERGE_PAINTS
 * @see SWT#DOUBLE_BUFFERED
 */
public void redraw (int x, int y, int width, int height, boolean all) {
	checkWidget();
	if (!OS.gtk_widget_get_visible (topHandle ())) return;
	if ((style & SWT.MIRRORED) != 0) x = getClientWidth () - width - x;
	redrawWidget (x, y, width, height, false, all, false);
}

void redrawChildren () {
}

void redrawWidget (int x, int y, int width, int height, boolean redrawAll, boolean all, boolean trim) {
	if (!gtk_widget_get_realized(handle)) return;
	long /*int*/ window = paintWindow ();
	GdkRectangle rect = new GdkRectangle ();
	if (redrawAll) {
		int [] w = new int [1], h = new int [1];
		gdk_window_get_size (window, w, h);
		rect.width = w [0];
		rect.height = h [0];
	} else {
		rect.x = x;
		rect.y = y;
		rect.width = Math.max (0, width);
		rect.height = Math.max (0, height);
	}
	OS.gdk_window_invalidate_rect (window, rect, all);
}

@Override
void release (boolean destroy) {
	Control next = null, previous = null;
	if (destroy && parent != null) {
		Control[] children = parent._getChildren ();
		int index = 0;
		while (index < children.length) {
			if (children [index] == this) break;
			index++;
		}
		if (index > 0) {
			previous = children [index - 1];
		}
		if (index + 1 < children.length) {
			next = children [index + 1];
			next.removeRelation ();
		}
		removeRelation ();
	}
	super.release (destroy);
	if (destroy) {
		if (previous != null && next != null) previous.addRelation (next);
	}
}

@Override
void releaseHandle () {
	super.releaseHandle ();
	fixedHandle = 0;
	parent = null;
}

@Override
void releaseParent () {
	parent.removeControl (this);
}

@Override
void releaseWidget () {
	super.releaseWidget ();
	if (display.currentControl == this) display.currentControl = null;
	display.removeMouseHoverTimeout (handle);
	long /*int*/ imHandle = imHandle ();
	if (imHandle != 0) {
		OS.gtk_im_context_reset (imHandle);
		OS.gtk_im_context_set_client_window (imHandle, 0);
	}
	if (enableWindow != 0) {
		OS.gdk_window_set_user_data (enableWindow, 0);
		OS.gdk_window_destroy (enableWindow);
		enableWindow = 0;
	}
	redrawWindow = 0;
	if (menu != null && !menu.isDisposed ()) {
		menu.dispose ();
	}
	menu = null;
	cursor = null;
	toolTipText = null;
	layoutData = null;
	if (accessible != null) {
		accessible.internal_dispose_Accessible ();
	}
	accessible = null;
	region = null;
}

void restackWindow (long /*int*/ window, long /*int*/ sibling, boolean above) {
   	OS.gdk_window_restack (window, sibling, above);
}

boolean sendDragEvent (int button, int stateMask, int x, int y, boolean isStateMask) {
	Event event = new Event ();
	event.button = button;
	event.x = x;
	event.y = y;
	if ((style & SWT.MIRRORED) != 0) event.x = getClientWidth () - event.x;
	if (isStateMask) {
		event.stateMask = stateMask;
	} else {
		setInputState (event, stateMask);
	}
	postEvent (SWT.DragDetect, event);
	if (isDisposed ()) return false;
	return event.doit;
}

void sendFocusEvent (int type) {
	Shell shell = _getShell ();
	Display display = this.display;
	display.focusControl = this;
	display.focusEvent = type;
	sendEvent (type);
	display.focusControl = null;
	display.focusEvent = SWT.None;
	/*
	* It is possible that the shell may be
	* disposed at this point.  If this happens
	* don't send the activate and deactivate
	* events.
	*/
	if (!shell.isDisposed ()) {
		switch (type) {
			case SWT.FocusIn:
				shell.setActiveControl (this);
				break;
			case SWT.FocusOut:
				if (shell != display.activeShell) {
					shell.setActiveControl (null);
				}
				break;
		}
	}
}

boolean sendHelpEvent (long /*int*/ helpType) {
	Control control = this;
	while (control != null) {
		if (control.hooks (SWT.Help)) {
			control.postEvent (SWT.Help);
			return true;
		}
		control = control.parent;
	}
	return false;
}

boolean sendLeaveNotify() {
	return false;
}

boolean sendMouseEvent (int type, int button, int time, double x, double y, boolean is_hint, int state) {
	return sendMouseEvent (type, button, 0, 0, false, time, x, y, is_hint, state);
}

boolean sendMouseEvent (int type, int button, int count, int detail, boolean send, int time, double x, double y, boolean is_hint, int state) {
	if (!hooks (type) && !filters (type)) return true;
	Event event = new Event ();
	event.time = time;
	event.button = button;
	event.detail = detail;
	event.count = count;
	if (is_hint) {
		event.x = (int)x;
		event.y = (int)y;
	} else {
		long /*int*/ window = eventWindow ();
		int [] origin_x = new int [1], origin_y = new int [1];
		OS.gdk_window_get_origin (window, origin_x, origin_y);
		event.x = (int)x - origin_x [0];
		event.y = (int)y - origin_y [0];
	}
	if ((style & SWT.MIRRORED) != 0) event.x = getClientWidth () - event.x;
	setInputState (event, state);
	if (send) {
		sendEvent (type, event);
		if (isDisposed ()) return false;
	} else {
		postEvent (type, event);
	}
	return event.doit;
}

/**
 * Not direct gtk api, but useful to have them combined as they are usually called together.
 * @param widget the GTK reference.
 * @param hAlign is of type GTKAlign enum. See OS.java
 * @param vAlign is of type GTKAlign enum. See OS.java
 */
void gtk_widget_set_align(long /*int*/ widget, int hAlign, int vAlign) {
	OS.gtk_widget_set_halign (widget, hAlign);
	OS.gtk_widget_set_valign (widget, vAlign);
}

void setBackground () {
	if ((state & BACKGROUND) == 0 && backgroundImage == null) {
		if ((state & PARENT_BACKGROUND) != 0) {
			setParentBackground ();
		} else {
			setWidgetBackground ();
		}
		redrawWidget (0, 0, 0, 0, true, false, false);
	}
}

/**
 * Sets the receiver's background color to the color specified
 * by the argument, or to the default system color for the control
 * if the argument is null.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * For example, on Windows the background of a Button cannot be changed.
 * </p>
 * @param color the new color (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setBackground (Color color) {
	checkWidget ();
	_setBackground (color);
	if (color != null) {
		this.updateBackgroundMode ();
	}
}

private void _setBackground (Color color) {
	if (((state & BACKGROUND) == 0) && color == null) return;
	GdkColor gdkColor = null;
	if (color != null) {
		if (color.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
		gdkColor = color.handle;
		backgroundAlpha = color.getAlpha ();
	}
	boolean set = false;
	if (OS.GTK3) {
		// TODO: Some widgets like GtkLabel (and others) are transparent
		// in GTK3, so one needs to set their background. For now, always
		// set the background. See bug 421836.
		// set = !getBackground().equals(color);
		set = true;
	} else {
		if (gdkColor == null) {
			long /*int*/ style = OS.gtk_widget_get_modifier_style (handle);
			set = (OS.gtk_rc_style_get_color_flags (style, OS.GTK_STATE_NORMAL) & OS.GTK_RC_BG) != 0;
		} else {
			GdkColor oldColor = getBackgroundColor ();
			set = oldColor.pixel != gdkColor.pixel;
		}
	}
	if (set) {
		if (color == null) {
			state &= ~BACKGROUND;
		} else {
			state |= BACKGROUND;
		}
		setBackgroundColor (gdkColor);
		redrawChildren ();
	}
}

void setBackgroundColor (long /*int*/ context, long /*int*/ handle, GdkRGBA rgba) {
	OS.gtk_widget_override_background_color (handle, OS.GTK_STATE_FLAG_NORMAL, rgba);
}

void setBackgroundColorGradient (long /*int*/ context, long /*int*/ handle, GdkRGBA rgba) {
	String css ="* {\n";
	if (rgba != null) {
		String color = gtk_rgba_to_css_string (rgba);
		//Note, use 'background-image' CSS class with caution. Not all themes/widgets support it. (e.g button doesn't).
		//Use 'background' CSS class where possible instead unless 'background-image' is explicidly supported.
		css += "background-image: -gtk-gradient (linear, 0 0, 0 1, color-stop(0, " + color + "), color-stop(1, " + color + "));\n";
	}
	css += "}\n";
	gtk_css_provider_load_from_css (context, css);
}

void gtk_css_provider_load_from_css (long /*int*/ context, String css) {
	/* Utility function. */
	//@param css : a 'css java' string like "{\nbackground: red;\n}".

	if (provider == 0) {
		provider = OS.gtk_css_provider_new ();
		OS.gtk_style_context_add_provider (context, provider, OS.GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		OS.g_object_unref (provider);
	}
	OS.gtk_css_provider_load_from_data (provider, Converter.wcsToMbcs (null, css, true), -1, null);
}

String gtk_rgba_to_css_string (GdkRGBA rgba) {
	/*
	 * In GdkRGBA, values are a double between 0-1.<br>
     * In CSS, values are typically integers between 0-255.<br>
     * I.e, note, there is a slight loss of precision.
     * Thus setting/getting color *might* return slight differences.
	 */
	String color = "rgba(" + (int)(rgba.red * 255) + "," + (int)(rgba.green * 255) + "," + (int)(rgba.blue * 255) + "," + (int)(rgba.alpha * 255) + ")";
	return color;
}

void setBackgroundColor (long /*int*/ handle, GdkColor color) {
	if (OS.GTK3) {
		GdkRGBA rgba = null;
		double alpha = 1;
		if (color == null) {
			if ((state & PARENT_BACKGROUND) != 0) {
				alpha = 0;
				Control control = findBackgroundControl();
				if (control == null) control = this;
				color = control.getBackgroundColor();
			}
		}
		else {
			alpha = backgroundAlpha;
		}
		if (color != null) {
			rgba = new GdkRGBA ();
			rgba.alpha = alpha / (float)255;
			rgba.red = (color.red & 0xFFFF) / (float)0xFFFF;
			rgba.green = (color.green & 0xFFFF) / (float)0xFFFF;
			rgba.blue = (color.blue & 0xFFFF) / (float)0xFFFF;
		}
		long /*int*/ context = OS.gtk_widget_get_style_context (handle);
		setBackgroundColor (context, handle, rgba);
		OS.gtk_style_context_invalidate (context);
		return;
	}
	int index = OS.GTK_STATE_NORMAL;
	long /*int*/ style = OS.gtk_widget_get_modifier_style (handle);
	long /*int*/ ptr = OS.gtk_rc_style_get_bg_pixmap_name (style, index);
	if (ptr != 0) OS.g_free (ptr);
	ptr = 0;

	String pixmapName = null;
	int flags = OS.gtk_rc_style_get_color_flags (style, index);
	if (color != null) {
		flags |= OS.GTK_RC_BG;
		pixmapName = "<none>"; //$NON-NLS-1$
	} else {
		flags &= ~OS.GTK_RC_BG;
		if (backgroundImage == null && (state & PARENT_BACKGROUND) != 0) {
			pixmapName = "<parent>"; //$NON-NLS-1$
		}
	}
	if (pixmapName != null) {
		byte[] buffer = Converter.wcsToMbcs (null, pixmapName, true);
		ptr = OS.g_malloc (buffer.length);
		OS.memmove (ptr, buffer, buffer.length);
	}

	OS.gtk_rc_style_set_bg_pixmap_name (style, index, ptr);
	OS.gtk_rc_style_set_bg (style, index, color);
	OS.gtk_rc_style_set_color_flags (style, index, flags);
	modifyStyle (handle, style);
}

void setBackgroundColor (GdkColor color) {
	setBackgroundColor (handle, color);
}

/**
 * Sets the receiver's background image to the image specified
 * by the argument, or to the default system color for the control
 * if the argument is null.  The background image is tiled to fill
 * the available space.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * For example, on Windows the background of a Button cannot be changed.
 * </p>
 * @param image the new image (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument is not a bitmap</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.2
 */
public void setBackgroundImage (Image image) {
	checkWidget ();
	if (image != null && image.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
	if (image == backgroundImage && backgroundAlpha > 0) return;
	backgroundAlpha = 255;
	this.backgroundImage = image;
	if (backgroundImage != null) {
		setBackgroundPixmap (backgroundImage);
		redrawWidget (0, 0, 0, 0, true, false, false);
	} else {
		setWidgetBackground ();
	}
	redrawChildren ();
}

void setBackgroundPixmap (Image image) {
	long /*int*/ window = gtk_widget_get_window (paintHandle ());
	if (window != 0) {
		if (image.pixmap != 0) {
			OS.gdk_window_set_back_pixmap (window, image.pixmap, false);
		} else if (image.surface != 0) {
			if (OS.GTK3) {
				long /*int*/ pattern = Cairo.cairo_pattern_create_for_surface(image.surface);
				if (pattern == 0) SWT.error(SWT.ERROR_NO_HANDLES);
				Cairo.cairo_pattern_set_extend(pattern, Cairo.CAIRO_EXTEND_REPEAT);
				OS.gdk_window_set_background_pattern(window, pattern);
				Cairo.cairo_pattern_destroy(pattern);
			}
			/*
			* TODO This code code is commented because it does not work since the pixmap
			* created with gdk_pixmap_foreign_new() does not have colormap. Another option
			* would be to create a pixmap on the fly from the surface.
			*
			* For now draw background in windowProc().
			*/
//			long /*int*/ surface = image.surface;
//			int type = Cairo.cairo_surface_get_type(surface);
//			switch (type) {
//				case Cairo.CAIRO_SURFACE_TYPE_XLIB:
//					long /*int*/ pixmap = OS.gdk_pixmap_foreign_new(Cairo.cairo_xlib_surface_get_drawable(surface));
//					OS.gdk_window_set_back_pixmap (window, pixmap, false);
//					OS.g_object_unref(pixmap);
//					break;
//			}
		}
	}
}

/**
 * If the argument is <code>true</code>, causes the receiver to have
 * all mouse events delivered to it until the method is called with
 * <code>false</code> as the argument.  Note that on some platforms,
 * a mouse button must currently be down for capture to be assigned.
 *
 * @param capture <code>true</code> to capture the mouse, and <code>false</code> to release it
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCapture (boolean capture) {
	checkWidget();
	/* FIXME !!!!! */
	/*
	if (capture) {
		OS.gtk_widget_grab_focus (handle);
	} else {
		OS.gtk_widget_grab_default (handle);
	}
	*/
}
/**
 * Sets the receiver's cursor to the cursor specified by the
 * argument, or to the default cursor for that kind of control
 * if the argument is null.
 * <p>
 * When the mouse pointer passes over a control its appearance
 * is changed to match the control's cursor.
 * </p>
 *
 * @param cursor the new cursor (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCursor (Cursor cursor) {
	checkWidget();
	if (cursor != null && cursor.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
	this.cursor = cursor;
	setCursor (cursor != null ? cursor.handle : 0);
}

void setCursor (long /*int*/ cursor) {
	long /*int*/ window = eventWindow ();
	if (window != 0) {
		OS.gdk_window_set_cursor (window, cursor);
		if (!OS.isX11()) {
			OS.gdk_flush ();
		} else {
			long /*int*/ xDisplay = OS.gdk_x11_display_get_xdisplay(OS.gdk_display_get_default());
			OS.XFlush (xDisplay);
		}
	}
}

/**
 * Sets the receiver's drag detect state. If the argument is
 * <code>true</code>, the receiver will detect drag gestures,
 * otherwise these gestures will be ignored.
 *
 * @param dragDetect the new drag detect state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.3
 */
public void setDragDetect (boolean dragDetect) {
	checkWidget ();
	if (dragDetect) {
		state |= DRAG_DETECT;
	} else {
		state &= ~DRAG_DETECT;
	}
}

static long /*int*/ enterNotifyEventProc (long /*int*/ ihint, long /*int*/ n_param_values, long /*int*/ param_values, long /*int*/ data) {
	/* 427776: this workaround listens to the enter-notify-event signal on all
	 * GtkWidgets. If enableWindow (the data parameter) has been added to the
	 * internal hash table of the widget, a record is kept as the lifetime of
	 * enableWindow is controlled here, so we'll need to remove that reference
	 * when we destroy enableWindow. this internal hash table was removed in
	 * GTK 3.11.9 so once only newer GTK is targeted, this workaround can be
	 * removed. */
	long /*int*/ instance = OS.g_value_peek_pointer (param_values);
	long /*int*/ hashTable = OS.g_object_get_qdata (instance, GTK_POINTER_WINDOW);

	// there will only ever be one item in the hash table
	if (hashTable != 0) {
		long /*int*/ firstItem = OS.g_hash_table_get_values (hashTable);
		long /*int*/ gdkWindow = OS.g_list_data (firstItem);
		// data is actually enableWindow
		if (gdkWindow == data)
			OS.g_object_set_qdata(gdkWindow, SWT_GRAB_WIDGET, instance);
	}

	return 1; // keep the signal connected
}

/**
 * Enables the receiver if the argument is <code>true</code>,
 * and disables it otherwise. A disabled control is typically
 * not selectable from the user interface and draws with an
 * inactive or "grayed" look.
 *
 * @param enabled the new enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setEnabled (boolean enabled) {
	checkWidget();
	if (((state & DISABLED) == 0) == enabled) return;
	Control control = null;
	boolean fixFocus = false;
	if (!enabled) {
		if (display.focusEvent != SWT.FocusOut) {
			control = display.getFocusControl ();
			fixFocus = isFocusAncestor (control);
		}
	}
	if (enabled) {
		state &= ~DISABLED;
	} else {
		state |= DISABLED;
	}
	enableWidget (enabled);
	if (isDisposed ()) return;
	if (enabled) {
		if (enableWindow != 0) {
			cleanupEnableWindow();
		}
	} else {
		OS.gtk_widget_realize (handle);
		long /*int*/ parentHandle = parent.eventHandle ();
		long /*int*/ window = parent.eventWindow ();
		long /*int*/ topHandle = topHandle ();
		GdkWindowAttr attributes = new GdkWindowAttr ();
		GtkAllocation allocation = new GtkAllocation ();
		OS.gtk_widget_get_allocation (topHandle, allocation);
		attributes.x = allocation.x;
		attributes.y = allocation.y;
		attributes.width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		attributes.height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
		attributes.event_mask = (0xFFFFFFFF & ~OS.ExposureMask);
		attributes.wclass = OS.GDK_INPUT_ONLY;
		attributes.window_type = OS.GDK_WINDOW_CHILD;
		enableWindow = OS.gdk_window_new (window, attributes, OS.GDK_WA_X | OS.GDK_WA_Y);
		if (enableWindow != 0) {
			/* 427776: we need to listen to all enter-notify-event signals to
			 * see if this new GdkWindow has been added to a widget's internal
			 * hash table, so when the GdkWindow is destroyed we can also remove
			 * that reference. */
			if (enterNotifyEventFunc != null)
				enterNotifyEventId = OS.g_signal_add_emission_hook (enterNotifyEventSignalId, 0, enterNotifyEventFunc.getAddress (), enableWindow, 0);

			OS.gdk_window_set_user_data (enableWindow, parentHandle);
			if (!OS.GDK_WINDOWING_X11 ()) {
				OS.gdk_window_raise (enableWindow);
			} else {
				restackWindow (enableWindow, gtk_widget_get_window (topHandle), true);
			}
			if (OS.gtk_widget_get_visible (topHandle)) OS.gdk_window_show_unraised (enableWindow);
		}
	}
	if (fixFocus) fixFocus (control);
}

void cleanupEnableWindow() {
	if (enterNotifyEventFunc != null) {
		if (enterNotifyEventId > 0)
			OS.g_signal_remove_emission_hook(enterNotifyEventSignalId, enterNotifyEventId);
		enterNotifyEventId = 0;

		/*
		 * 427776: now we can remove any reference to the GdkWindow
		 * in a widget's internal hash table. this internal hash
		 * table was removed in GTK 3.11.9 so once only newer GTK is
		 * targeted, this workaround can be removed.
		 */
		long /*int*/ grabWidget = OS.g_object_get_qdata(enableWindow, SWT_GRAB_WIDGET);
		if (grabWidget != 0) {
			OS.g_object_set_qdata(grabWidget, GTK_POINTER_WINDOW, 0);
			OS.g_object_set_qdata(enableWindow, SWT_GRAB_WIDGET, 0);
		}
	}

	OS.gdk_window_set_user_data (enableWindow, 0);
	OS.gdk_window_destroy (enableWindow);
	enableWindow = 0;
}

/**
 * Causes the receiver to have the <em>keyboard focus</em>,
 * such that all keyboard events will be delivered to it.  Focus
 * reassignment will respect applicable platform constraints.
 *
 * @return <code>true</code> if the control got focus, and <code>false</code> if it was unable to.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #forceFocus
 */
public boolean setFocus () {
	checkWidget();
	if ((style & SWT.NO_FOCUS) != 0) return false;
	return forceFocus ();
}

/**
 * Sets the font that the receiver will use to paint textual information
 * to the font specified by the argument, or to the default font for that
 * kind of control if the argument is null.
 *
 * @param font the new font (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setFont (Font font) {
	checkWidget();
	if (((state & FONT) == 0) && font == null) return;
	this.font = font;
	long /*int*/ fontDesc;
	if (font == null) {
		fontDesc = defaultFont ().handle;
	} else {
		if (font.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
		fontDesc = font.handle;
	}
	if (font == null) {
		state &= ~FONT;
	} else {
		state |= FONT;
	}
	setFontDescription (fontDesc);
}

void setFontDescription (long /*int*/ font) {
	setFontDescription (handle, font);
}

/**
 * Sets the receiver's foreground color to the color specified
 * by the argument, or to the default system color for the control
 * if the argument is null.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * </p>
 * @param color the new color (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setForeground (Color color) {
	checkWidget();
	if (((state & FOREGROUND) == 0) && color == null) return;
	GdkColor gdkColor = null;
	if (color != null) {
		if (color.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
		gdkColor = color.handle;
	}
	boolean set = false;
	if (OS.GTK3) {
		set = !getForeground().equals(color);
	} else {
		if (gdkColor == null) {
			long /*int*/ style = OS.gtk_widget_get_modifier_style (handle);
			set = (OS.gtk_rc_style_get_color_flags (style, OS.GTK_STATE_NORMAL) & OS.GTK_RC_FG) != 0;
		} else {
			GdkColor oldColor = getForegroundColor ();
			set = oldColor.pixel != gdkColor.pixel;
		}
	}
	if (set) {
		if (color == null) {
			state &= ~FOREGROUND;
		} else {
			state |= FOREGROUND;
		}
		setForegroundColor (gdkColor);
	}
}

void setForegroundColor (GdkColor color) {
	setForegroundColor (handle, color);
}

void setInitialBounds () {
	if ((state & ZERO_WIDTH) != 0 && (state & ZERO_HEIGHT) != 0) {
		/*
		* Feature in GTK.  On creation, each widget's allocation is
		* initialized to a position of (-1, -1) until the widget is
		* first sized.  The fix is to set the value to (0, 0) as
		* expected by SWT.
		*/
		long /*int*/ topHandle = topHandle ();
		GtkAllocation allocation = new GtkAllocation();
		if ((parent.style & SWT.MIRRORED) != 0) {
			allocation.x = parent.getClientWidth ();
		} else {
			allocation.x = 0;
		}
		allocation.y = 0;
		if (OS.GTK3) {
			OS.gtk_widget_set_visible(topHandle, true);
		}
		OS.gtk_widget_set_allocation(topHandle, allocation);
	} else {
		resizeHandle (1, 1);
		forceResize ();
	}
}

/**
 * Sets the receiver's pop up menu to the argument.
 * All controls may optionally have a pop up
 * menu that is displayed when the user requests one for
 * the control. The sequence of key strokes, button presses
 * and/or button releases that are used to request a pop up
 * menu is platform specific.
 * <p>
 * Note: Disposing of a control that has a pop up menu will
 * dispose of the menu.  To avoid this behavior, set the
 * menu to null before the control is disposed.
 * </p>
 *
 * @param menu the new pop up menu
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_MENU_NOT_POP_UP - the menu is not a pop up menu</li>
 *    <li>ERROR_INVALID_PARENT - if the menu is not in the same widget tree</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the menu has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMenu (Menu menu) {
	checkWidget();
	if (menu != null) {
		if ((menu.style & SWT.POP_UP) == 0) {
			error (SWT.ERROR_MENU_NOT_POP_UP);
		}
		if (menu.parent != menuShell ()) {
			error (SWT.ERROR_INVALID_PARENT);
		}
	}
	this.menu = menu;
}

@Override
void setOrientation (boolean create) {
	if ((style & SWT.RIGHT_TO_LEFT) != 0 || !create) {
		int dir = (style & SWT.RIGHT_TO_LEFT) != 0 ? OS.GTK_TEXT_DIR_RTL : OS.GTK_TEXT_DIR_LTR;
		if (handle != 0) OS.gtk_widget_set_direction (handle, dir);
		if (fixedHandle != 0) OS.gtk_widget_set_direction (fixedHandle, dir);
	}
}

/**
 * Sets the orientation of the receiver, which must be one
 * of the constants <code>SWT.LEFT_TO_RIGHT</code> or <code>SWT.RIGHT_TO_LEFT</code>.
 * <p>
 *
 * @param orientation new orientation style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.7
 */
public void setOrientation (int orientation) {
	checkWidget ();
	int flags = SWT.RIGHT_TO_LEFT | SWT.LEFT_TO_RIGHT;
	if ((orientation & flags) == 0 || (orientation & flags) == flags) return;
	style &= ~flags;
	style |= orientation & flags;
	setOrientation (false);
	style &= ~SWT.MIRRORED;
	checkMirrored ();
}

/**
 * Changes the parent of the widget to be the one provided if
 * the underlying operating system supports this feature.
 * Returns <code>true</code> if the parent is successfully changed.
 *
 * @param parent the new parent for the control.
 * @return <code>true</code> if the parent is changed and <code>false</code> otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is <code>null</code></li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *	</ul>
 */
public boolean setParent (Composite parent) {
	checkWidget ();
	if (parent == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (parent.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	if (this.parent == parent) return true;
	if (!isReparentable ()) return false;
	OS.gtk_widget_realize (parent.handle);
	long /*int*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	OS.gtk_widget_get_allocation (topHandle, allocation);
	int x = allocation.x;
	int y = allocation.y;
	int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
	int height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
	if ((this.parent.style & SWT.MIRRORED) != 0) {
		x =  this.parent.getClientWidth () - width - x;
	}
	if ((parent.style & SWT.MIRRORED) != 0) {
		x = parent.getClientWidth () - width - x;
	}
	releaseParent ();
	Shell newShell = parent.getShell (), oldShell = getShell ();
	Decorations newDecorations = parent.menuShell (), oldDecorations = menuShell ();
	Menu [] menus = oldShell.findMenus (this);
	if (oldShell != newShell || oldDecorations != newDecorations) {
		fixChildren (newShell, oldShell, newDecorations, oldDecorations, menus);
		newDecorations.fixAccelGroup ();
		oldDecorations.fixAccelGroup ();
	}
	long /*int*/ newParent = parent.parentingHandle();
	OS.gtk_widget_reparent(topHandle, newParent);
	if (OS.GTK3) {
		OS.swt_fixed_move (newParent, topHandle, x, y);
	} else {
		OS.gtk_fixed_move (newParent, topHandle, x, y);
	}
	/*
	* Restore the original widget size since GTK does not keep it.
	*/
	resizeHandle(width, height);
	/*
	* Cause a size allocation this widget's topHandle.  Note that
	* all calls to gtk_widget_size_allocate() must be preceded by
	* a call to gtk_widget_size_request().
	*/
	GtkRequisition requisition = new GtkRequisition ();
	gtk_widget_size_request (topHandle, requisition);
	allocation.x = x;
	allocation.y = y;
	allocation.width = width;
	allocation.height = height;
	OS.gtk_widget_size_allocate (topHandle, allocation);
	this.parent = parent;
	setZOrder (null, false, true);
	reskin (SWT.ALL);
	return true;
}

void setParentBackground () {
	setBackgroundColor (handle, null);
	if (fixedHandle != 0) setBackgroundColor (fixedHandle, null);
}

void setParentWindow (long /*int*/ widget) {
}

boolean setRadioSelection (boolean value) {
	return false;
}

/**
 * If the argument is <code>false</code>, causes subsequent drawing
 * operations in the receiver to be ignored. No drawing of any kind
 * can occur in the receiver until the flag is set to true.
 * Graphics operations that occurred while the flag was
 * <code>false</code> are lost. When the flag is set to <code>true</code>,
 * the entire widget is marked as needing to be redrawn.  Nested calls
 * to this method are stacked.
 * <p>
 * Note: This operation is a hint and may not be supported on some
 * platforms or for some widgets.
 * </p>
 *
 * @param redraw the new redraw state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #redraw(int, int, int, int, boolean)
 * @see #update()
 */
public void setRedraw (boolean redraw) {
	checkWidget();
	if (redraw) {
		if (--drawCount == 0) {
			if (redrawWindow != 0) {
				long /*int*/ window = paintWindow ();
				/* Explicitly hiding the window avoids flicker on GTK+ >= 2.6 */
				OS.gdk_window_hide (redrawWindow);
				OS.gdk_window_destroy (redrawWindow);
				OS.gdk_window_set_events (window, OS.gtk_widget_get_events (paintHandle ()));
				redrawWindow = 0;
			}
		}
	} else {
		if (drawCount++ == 0) {
			if (gtk_widget_get_realized (handle)) {
				long /*int*/ window = paintWindow ();
				Rectangle rect = getBounds ();
				GdkWindowAttr attributes = new GdkWindowAttr ();
				attributes.width = rect.width;
				attributes.height = rect.height;
				attributes.event_mask = OS.GDK_EXPOSURE_MASK;
				attributes.window_type = OS.GDK_WINDOW_CHILD;
				redrawWindow = OS.gdk_window_new (window, attributes, 0);
				if (redrawWindow != 0) {
					int mouseMask = OS.GDK_BUTTON_PRESS_MASK | OS.GDK_BUTTON_RELEASE_MASK |
						OS.GDK_ENTER_NOTIFY_MASK | OS.GDK_LEAVE_NOTIFY_MASK |
						OS.GDK_POINTER_MOTION_MASK | OS.GDK_POINTER_MOTION_HINT_MASK |
						OS.GDK_BUTTON_MOTION_MASK | OS.GDK_BUTTON1_MOTION_MASK |
						OS.GDK_BUTTON2_MOTION_MASK | OS.GDK_BUTTON3_MOTION_MASK;
					OS.gdk_window_set_events (window, OS.gdk_window_get_events (window) & ~mouseMask);
					if (OS.GTK3) {
						OS.gdk_window_set_background_pattern(redrawWindow, 0);
					} else {
						OS.gdk_window_set_back_pixmap (redrawWindow, 0, false);
					}
					OS.gdk_window_show (redrawWindow);
				}
			}
		}
	}
}

@Override
boolean setTabItemFocus (boolean next) {
	if (!isShowing ()) return false;
	return forceFocus ();
}

/**
 * Sets the base text direction (a.k.a. "paragraph direction") of the receiver,
 * which must be one of the constants <code>SWT.LEFT_TO_RIGHT</code>,
 * <code>SWT.RIGHT_TO_LEFT</code>, or a bitwise disjunction
 * <code>SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT</code>. The latter stands for an
 * "auto" direction, which implies that a control containing text derives the
 * direction from the directionality of the first strong bidi character.
 * <p>
 * <code>setOrientation</code> would override this value with the text direction
 * that is consistent with the new orientation.
 * </p>
 * <p>
 * <b>Warning</b>: This API is currently only implemented on Windows.
 * It doesn't set the base text direction on GTK and Cocoa.
 * </p>
 *
 * @param textDirection the base text direction style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SWT#LEFT_TO_RIGHT
 * @see SWT#RIGHT_TO_LEFT
 * @see SWT#FLIP_TEXT_DIRECTION
 *
 * @since 3.102
 */
public void setTextDirection(int textDirection) {
	checkWidget ();
}

/**
 * Sets the receiver's tool tip text to the argument, which
 * may be null indicating that the default tool tip for the
 * control will be shown. For a control that has a default
 * tool tip, such as the Tree control on Windows, setting
 * the tool tip text to an empty string replaces the default,
 * causing no tool tip text to be shown.
 * <p>
 * The mnemonic indicator (character '&amp;') is not displayed in a tool tip.
 * To display a single '&amp;' in the tool tip, the character '&amp;' can be
 * escaped by doubling it in the string.
 * </p>
 *
 * @param string the new tool tip text (or null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setToolTipText (String string) {
	checkWidget();
	setToolTipText (_getShell (), string);
	toolTipText = string;
}

void setToolTipText (Shell shell, String newString) {
	/*
	* Feature in GTK.  In order to prevent children widgets
	* from inheriting their parent's tooltip, the tooltip is
	* a set on a shell only. In order to force the shell tooltip
	* to update when a new tip string is set, the existing string
	* in the tooltip is set to null, followed by running a query.
	* The real tip text can then be set.
	*
	* Note that this will only run if the control for which the
	* tooltip is being set is the current control (i.e. the control
	* under the pointer).
	*/
	if (display.currentControl == this) {
		shell.setToolTipText (shell.handle, eventHandle (), newString);
	}
}

/**
 * Sets whether this control should send touch events (by default controls do not).
 * Setting this to <code>false</code> causes the receiver to send gesture events
 * instead.  No exception is thrown if a touch-based input device is not
 * detected (this can be determined with <code>Display#getTouchEnabled()</code>).
 *
 * @param enabled the new touch-enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *
 * @see Display#getTouchEnabled
 *
 * @since 3.7
 */
public void setTouchEnabled(boolean enabled) {
	checkWidget();
}

/**
 * Marks the receiver as visible if the argument is <code>true</code>,
 * and marks it invisible otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, marking
 * it visible may not actually cause it to be displayed.
 * </p>
 *
 * @param visible the new visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setVisible (boolean visible) {
	checkWidget();
	if (((state & HIDDEN) == 0) == visible) return;
	long /*int*/ topHandle = topHandle();
	if (visible) {
		/*
		* It is possible (but unlikely), that application
		* code could have disposed the widget in the show
		* event.  If this happens, just return.
		*/
		sendEvent (SWT.Show);
		if (isDisposed ()) return;
		state &= ~HIDDEN;
		if ((state & (ZERO_WIDTH | ZERO_HEIGHT)) == 0) {
			if (enableWindow != 0) OS.gdk_window_show_unraised (enableWindow);
			OS.gtk_widget_show (topHandle);
		}
	} else {
		/*
		* Bug in GTK.  Invoking gtk_widget_hide() on a widget that has
		* focus causes a focus_out_event to be sent. If the client disposes
		* the widget inside the event, GTK GP's.  The fix is to reassign focus
		* before hiding the widget.
		*
		* NOTE: In order to stop the same widget from taking focus,
		* temporarily clear and set the GTK_VISIBLE flag.
		*/
		Control control = null;
		boolean fixFocus = false;
		if (display.focusEvent != SWT.FocusOut) {
			control = display.getFocusControl ();
			fixFocus = isFocusAncestor (control);
		}
		state |= HIDDEN;
		if (fixFocus) {
			if (OS.GTK3) {
				OS.gtk_widget_set_can_focus (topHandle, false);
			} else {
				gtk_widget_set_visible (topHandle, false);
			}
			fixFocus (control);
			if (isDisposed ()) return;
			if (OS.GTK3) {
				OS.gtk_widget_set_can_focus (topHandle, true);
			} else {
				gtk_widget_set_visible (topHandle, true);
			}
		}
		OS.gtk_widget_hide (topHandle);
		if (isDisposed ()) return;
		if (enableWindow != 0) OS.gdk_window_hide (enableWindow);
		sendEvent (SWT.Hide);
	}
}

void setZOrder (Control sibling, boolean above, boolean fixRelations) {
	 setZOrder (sibling, above, fixRelations, true);
}

void setZOrder (Control sibling, boolean above, boolean fixRelations, boolean fixChildren) {
	int index = 0, siblingIndex = 0, oldNextIndex = -1;
	Control[] children = null;
	if (fixRelations) {
		/* determine the receiver's and sibling's indexes in the parent */
		children = parent._getChildren ();
		while (index < children.length) {
			if (children [index] == this) break;
			index++;
		}
		if (sibling != null) {
			while (siblingIndex < children.length) {
				if (children [siblingIndex] == sibling) break;
				siblingIndex++;
			}
		}
		/* remove "Labelled by" relationships that will no longer be valid */
		removeRelation ();
		if (index + 1 < children.length) {
			oldNextIndex = index + 1;
			children [oldNextIndex].removeRelation ();
		}
		if (sibling != null) {
			if (above) {
				sibling.removeRelation ();
			} else {
				if (siblingIndex + 1 < children.length) {
					children [siblingIndex + 1].removeRelation ();
				}
			}
		}
	}

	long /*int*/ topHandle = topHandle ();
	long /*int*/ siblingHandle = sibling != null ? sibling.topHandle () : 0;
	long /*int*/ window = gtk_widget_get_window (topHandle);
	if (window != 0) {
		long /*int*/ siblingWindow = 0;
		if (sibling != null) {
			if (above && sibling.enableWindow != 0) {
				siblingWindow = enableWindow;
			} else {
				siblingWindow = OS.gtk_widget_get_window (siblingHandle);
			}
		}
		long /*int*/ redrawWindow = fixChildren ? parent.redrawWindow : 0;
		if (!OS.GDK_WINDOWING_X11 () || (siblingWindow == 0 && (!above || redrawWindow == 0))) {
			if (above) {
				OS.gdk_window_raise (window);
				if (redrawWindow != 0) OS.gdk_window_raise (redrawWindow);
				if (enableWindow != 0) OS.gdk_window_raise (enableWindow);
			} else {
				if (enableWindow != 0) OS.gdk_window_lower (enableWindow);
				OS.gdk_window_lower (window);
			}
		} else {
			long /*int*/ siblingW = siblingWindow != 0 ? siblingWindow : redrawWindow;
			boolean stack_mode = above;
			if (redrawWindow != 0 && siblingWindow == 0) stack_mode = false;
			restackWindow (window, siblingW, stack_mode);
			if (enableWindow != 0) {
				 restackWindow (enableWindow, window, true);
			}
		}
	}
	if (fixChildren) {
		if (above) {
			parent.moveAbove (topHandle, siblingHandle);
		} else {
			parent.moveBelow (topHandle, siblingHandle);
		}
	}
	/*  Make sure that the parent internal windows are on the bottom of the stack	*/
	if (!above && fixChildren) 	parent.fixZOrder ();

	if (fixRelations) {
		/* determine the receiver's new index in the parent */
		if (sibling != null) {
			if (above) {
				index = siblingIndex - (index < siblingIndex ? 1 : 0);
			} else {
				index = siblingIndex + (siblingIndex < index ? 1 : 0);
			}
		} else {
			if (above) {
				index = 0;
			} else {
				index = children.length - 1;
			}
		}

		/* add new "Labelled by" relations as needed */
		children = parent._getChildren ();
		if (0 < index) {
			children [index - 1].addRelation (this);
		}
		if (index + 1 < children.length) {
			addRelation (children [index + 1]);
		}
		if (oldNextIndex != -1) {
			if (oldNextIndex <= index) oldNextIndex--;
			/* the last two conditions below ensure that duplicate relations are not hooked */
			if (0 < oldNextIndex && oldNextIndex != index && oldNextIndex != index + 1) {
				children [oldNextIndex - 1].addRelation (children [oldNextIndex]);
			}
		}
	}
}

void setWidgetBackground  () {
	GdkColor color = (state & BACKGROUND) != 0 ? getBackgroundColor () : null;
	if (fixedHandle != 0) setBackgroundColor (fixedHandle, color);
	setBackgroundColor (handle, color);
}

boolean showMenu (int x, int y) {
	return showMenu (x, y, SWT.MENU_MOUSE);
}

boolean showMenu (int x, int y, int detail) {
	Event event = new Event ();
	event.x = x;
	event.y = y;
	event.detail = detail;
	sendEvent (SWT.MenuDetect, event);
	//widget could be disposed at this point
	if (isDisposed ()) return false;
	if (event.doit) {
		if (menu != null && !menu.isDisposed ()) {
			boolean hooksKeys = hooks (SWT.KeyDown) || hooks (SWT.KeyUp);
			menu.createIMMenu (hooksKeys ? imHandle() : 0);
			if (event.x != x || event.y != y) {
				menu.setLocation (event.x, event.y);
			}
			menu.setVisible (true);
			return true;
		}
	}
	return false;
}

void showWidget () {
	// Comment this line to disable zero-sized widgets
	state |= ZERO_WIDTH | ZERO_HEIGHT;
	long /*int*/ topHandle = topHandle ();
	long /*int*/ parentHandle = parent.parentingHandle ();
	parent.setParentWindow (topHandle);
	OS.gtk_container_add (parentHandle, topHandle);
	if (handle != 0 && handle != topHandle) OS.gtk_widget_show (handle);
	if ((state & (ZERO_WIDTH | ZERO_HEIGHT)) == 0) {
		if (fixedHandle != 0) OS.gtk_widget_show (fixedHandle);
	}
	if (fixedHandle != 0) fixStyle (fixedHandle);
}

void sort (int [] items) {
	/* Shell Sort from K&R, pg 108 */
	int length = items.length;
	for (int gap=length/2; gap>0; gap/=2) {
		for (int i=gap; i<length; i++) {
			for (int j=i-gap; j>=0; j-=gap) {
		   		if (items [j] <= items [j + gap]) {
					int swap = items [j];
					items [j] = items [j + gap];
					items [j + gap] = swap;
		   		}
	    	}
	    }
	}
}

/**
 * Based on the argument, perform one of the expected platform
 * traversal action. The argument should be one of the constants:
 * <code>SWT.TRAVERSE_ESCAPE</code>, <code>SWT.TRAVERSE_RETURN</code>,
 * <code>SWT.TRAVERSE_TAB_NEXT</code>, <code>SWT.TRAVERSE_TAB_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_ARROW_NEXT</code>, <code>SWT.TRAVERSE_ARROW_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_PAGE_NEXT</code> and <code>SWT.TRAVERSE_PAGE_PREVIOUS</code>.
 *
 * @param traversal the type of traversal
 * @return true if the traversal succeeded
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean traverse (int traversal) {
	checkWidget ();
	Event event = new Event ();
	event.doit = true;
	event.detail = traversal;
	return traverse (event);
}

/**
 * Performs a platform traversal action corresponding to a <code>KeyDown</code> event.
 *
 * <p>Valid traversal values are
 * <code>SWT.TRAVERSE_NONE</code>, <code>SWT.TRAVERSE_MNEMONIC</code>,
 * <code>SWT.TRAVERSE_ESCAPE</code>, <code>SWT.TRAVERSE_RETURN</code>,
 * <code>SWT.TRAVERSE_TAB_NEXT</code>, <code>SWT.TRAVERSE_TAB_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_ARROW_NEXT</code>, <code>SWT.TRAVERSE_ARROW_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_PAGE_NEXT</code> and <code>SWT.TRAVERSE_PAGE_PREVIOUS</code>.
 * If <code>traversal</code> is <code>SWT.TRAVERSE_NONE</code> then the Traverse
 * event is created with standard values based on the KeyDown event.  If
 * <code>traversal</code> is one of the other traversal constants then the Traverse
 * event is created with this detail, and its <code>doit</code> is taken from the
 * KeyDown event.
 * </p>
 *
 * @param traversal the type of traversal, or <code>SWT.TRAVERSE_NONE</code> to compute
 * this from <code>event</code>
 * @param event the KeyDown event
 *
 * @return <code>true</code> if the traversal succeeded
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.6
 */
public boolean traverse (int traversal, Event event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return traverse (traversal, event.character, event.keyCode, event.keyLocation, event.stateMask, event.doit);
}

/**
 * Performs a platform traversal action corresponding to a <code>KeyDown</code> event.
 *
 * <p>Valid traversal values are
 * <code>SWT.TRAVERSE_NONE</code>, <code>SWT.TRAVERSE_MNEMONIC</code>,
 * <code>SWT.TRAVERSE_ESCAPE</code>, <code>SWT.TRAVERSE_RETURN</code>,
 * <code>SWT.TRAVERSE_TAB_NEXT</code>, <code>SWT.TRAVERSE_TAB_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_ARROW_NEXT</code>, <code>SWT.TRAVERSE_ARROW_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_PAGE_NEXT</code> and <code>SWT.TRAVERSE_PAGE_PREVIOUS</code>.
 * If <code>traversal</code> is <code>SWT.TRAVERSE_NONE</code> then the Traverse
 * event is created with standard values based on the KeyDown event.  If
 * <code>traversal</code> is one of the other traversal constants then the Traverse
 * event is created with this detail, and its <code>doit</code> is taken from the
 * KeyDown event.
 * </p>
 *
 * @param traversal the type of traversal, or <code>SWT.TRAVERSE_NONE</code> to compute
 * this from <code>event</code>
 * @param event the KeyDown event
 *
 * @return <code>true</code> if the traversal succeeded
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.6
 */
public boolean traverse (int traversal, KeyEvent event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return traverse (traversal, event.character, event.keyCode, event.keyLocation, event.stateMask, event.doit);
}

boolean traverse (int traversal, char character, int keyCode, int keyLocation, int stateMask, boolean doit) {
	if (traversal == SWT.TRAVERSE_NONE) {
		switch (keyCode) {
			case SWT.ESC: {
				traversal = SWT.TRAVERSE_ESCAPE;
				doit = true;
				break;
			}
			case SWT.CR: {
				traversal = SWT.TRAVERSE_RETURN;
				doit = true;
				break;
			}
			case SWT.ARROW_DOWN:
			case SWT.ARROW_RIGHT: {
				traversal = SWT.TRAVERSE_ARROW_NEXT;
				doit = false;
				break;
			}
			case SWT.ARROW_UP:
			case SWT.ARROW_LEFT: {
				traversal = SWT.TRAVERSE_ARROW_PREVIOUS;
				doit = false;
				break;
			}
			case SWT.TAB: {
				traversal = (stateMask & SWT.SHIFT) != 0 ? SWT.TRAVERSE_TAB_PREVIOUS : SWT.TRAVERSE_TAB_NEXT;
				doit = true;
				break;
			}
			case SWT.PAGE_DOWN: {
				if ((stateMask & SWT.CTRL) != 0) {
					traversal = SWT.TRAVERSE_PAGE_NEXT;
					doit = true;
				}
				break;
			}
			case SWT.PAGE_UP: {
				if ((stateMask & SWT.CTRL) != 0) {
					traversal = SWT.TRAVERSE_PAGE_PREVIOUS;
					doit = true;
				}
				break;
			}
			default: {
				if (character != 0 && (stateMask & (SWT.ALT | SWT.CTRL)) == SWT.ALT) {
					traversal = SWT.TRAVERSE_MNEMONIC;
					doit = true;
				}
				break;
			}
		}
	}

	Event event = new Event ();
	event.character = character;
	event.detail = traversal;
	event.doit = doit;
	event.keyCode = keyCode;
	event.keyLocation = keyLocation;
	event.stateMask = stateMask;
	Shell shell = getShell ();

	boolean all = false;
	switch (traversal) {
		case SWT.TRAVERSE_ESCAPE:
		case SWT.TRAVERSE_RETURN:
		case SWT.TRAVERSE_PAGE_NEXT:
		case SWT.TRAVERSE_PAGE_PREVIOUS: {
			all = true;
			// FALL THROUGH
		}
		case SWT.TRAVERSE_ARROW_NEXT:
		case SWT.TRAVERSE_ARROW_PREVIOUS:
		case SWT.TRAVERSE_TAB_NEXT:
		case SWT.TRAVERSE_TAB_PREVIOUS: {
			/* traversal is a valid traversal action */
			break;
		}
		case SWT.TRAVERSE_MNEMONIC: {
			return translateMnemonic (event, null) || shell.translateMnemonic (event, this);
		}
		default: {
			/* traversal is not a valid traversal action */
			return false;
		}
	}

	Control control = this;
	do {
		if (control.traverse (event)) return true;
		if (!event.doit && control.hooks (SWT.Traverse)) return false;
		if (control == shell) return false;
		control = control.parent;
	} while (all && control != null);
	return false;
}

boolean translateMnemonic (Event event, Control control) {
	if (control == this) return false;
	if (!isVisible () || !isEnabled ()) return false;
	event.doit = this == display.mnemonicControl || mnemonicMatch (event.character);
	return traverse (event);
}

boolean translateMnemonic (int keyval, GdkEventKey gdkEvent) {
	int key = OS.gdk_keyval_to_unicode (keyval);
	if (key < 0x20) return false;
	if (gdkEvent.state == 0) {
		int code = traversalCode (keyval, gdkEvent);
		if ((code & SWT.TRAVERSE_MNEMONIC) == 0) return false;
	} else {
		Shell shell = _getShell ();
		int mask = OS.GDK_CONTROL_MASK | OS.GDK_SHIFT_MASK | OS.GDK_MOD1_MASK;
		if ((gdkEvent.state & mask) != OS.gtk_window_get_mnemonic_modifier (shell.shellHandle)) return false;
	}
	Decorations shell = menuShell ();
	if (shell.isVisible () && shell.isEnabled ()) {
		Event event = new Event ();
		event.detail = SWT.TRAVERSE_MNEMONIC;
		if (setKeyState (event, gdkEvent)) {
			return translateMnemonic (event, null) || shell.translateMnemonic (event, this);
		}
	}
	return false;
}

boolean translateTraversal (GdkEventKey keyEvent) {
	int detail = SWT.TRAVERSE_NONE;
	int key = keyEvent.keyval;
	int code = traversalCode (key, keyEvent);
	boolean all = false;
	switch (key) {
		case OS.GDK_Escape: {
			all = true;
			detail = SWT.TRAVERSE_ESCAPE;
			break;
		}
		case OS.GDK_KP_Enter:
		case OS.GDK_Return: {
			all = true;
			detail = SWT.TRAVERSE_RETURN;
			break;
		}
		case OS.GDK_ISO_Left_Tab:
		case OS.GDK_Tab: {
			boolean next = (keyEvent.state & OS.GDK_SHIFT_MASK) == 0;
			detail = next ? SWT.TRAVERSE_TAB_NEXT : SWT.TRAVERSE_TAB_PREVIOUS;
			break;
		}
		case OS.GDK_Up:
		case OS.GDK_Left:
		case OS.GDK_Down:
		case OS.GDK_Right: {
			boolean next = key == OS.GDK_Down || key == OS.GDK_Right;
			if (parent != null && (parent.style & SWT.MIRRORED) != 0) {
				if (key == OS.GDK_Left || key == OS.GDK_Right) next = !next;
			}
			detail = next ? SWT.TRAVERSE_ARROW_NEXT : SWT.TRAVERSE_ARROW_PREVIOUS;
			break;
		}
		case OS.GDK_Page_Up:
		case OS.GDK_Page_Down: {
			all = true;
			if ((keyEvent.state & OS.GDK_CONTROL_MASK) == 0) return false;
			detail = key == OS.GDK_Page_Down ? SWT.TRAVERSE_PAGE_NEXT : SWT.TRAVERSE_PAGE_PREVIOUS;
			break;
		}
		default:
			return false;
	}
	Event event = new Event ();
	event.doit = (code & detail) != 0;
	event.detail = detail;
	event.time = keyEvent.time;
	if (!setKeyState (event, keyEvent)) return false;
	Shell shell = getShell ();
	Control control = this;
	do {
		if (control.traverse (event)) return true;
		if (!event.doit && control.hooks (SWT.Traverse)) return false;
		if (control == shell) return false;
		control = control.parent;
	} while (all && control != null);
	return false;
}

int traversalCode (int key, GdkEventKey event) {
	int code = SWT.TRAVERSE_RETURN | SWT.TRAVERSE_TAB_NEXT |  SWT.TRAVERSE_TAB_PREVIOUS | SWT.TRAVERSE_PAGE_NEXT | SWT.TRAVERSE_PAGE_PREVIOUS;
	Shell shell = getShell ();
	if (shell.parent != null) code |= SWT.TRAVERSE_ESCAPE;
	return code;
}

boolean traverse (Event event) {
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in the traverse
	* event.  If this happens, return true to stop further
	* event processing.
	*/
	sendEvent (SWT.Traverse, event);
	if (isDisposed ()) return true;
	if (!event.doit) return false;
	switch (event.detail) {
		case SWT.TRAVERSE_NONE:			return true;
		case SWT.TRAVERSE_ESCAPE:			return traverseEscape ();
		case SWT.TRAVERSE_RETURN:			return traverseReturn ();
		case SWT.TRAVERSE_TAB_NEXT:		return traverseGroup (true);
		case SWT.TRAVERSE_TAB_PREVIOUS:	return traverseGroup (false);
		case SWT.TRAVERSE_ARROW_NEXT:		return traverseItem (true);
		case SWT.TRAVERSE_ARROW_PREVIOUS:	return traverseItem (false);
		case SWT.TRAVERSE_MNEMONIC:		return traverseMnemonic (event.character);
		case SWT.TRAVERSE_PAGE_NEXT:		return traversePage (true);
		case SWT.TRAVERSE_PAGE_PREVIOUS:	return traversePage (false);
	}
	return false;
}

boolean traverseEscape () {
	return false;
}

boolean traverseGroup (boolean next) {
	Control root = computeTabRoot ();
	Widget group = computeTabGroup ();
	Widget [] list = root.computeTabList ();
	int length = list.length;
	int index = 0;
	while (index < length) {
		if (list [index] == group) break;
		index++;
	}
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in focus in
	* or out events.  Ensure that a disposed widget is
	* not accessed.
	*/
	if (index == length) return false;
	int start = index, offset = (next) ? 1 : -1;
	while ((index = ((index + offset + length) % length)) != start) {
		Widget widget = list [index];
		if (!widget.isDisposed () && widget.setTabGroupFocus (next)) {
			return true;
		}
	}
	if (group.isDisposed ()) return false;
	return group.setTabGroupFocus (next);
}

boolean traverseItem (boolean next) {
	Control [] children = parent._getChildren ();
	int length = children.length;
	int index = 0;
	while (index < length) {
		if (children [index] == this) break;
		index++;
	}
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in focus in
	* or out events.  Ensure that a disposed widget is
	* not accessed.
	*/
	if (index == length) return false;
	int start = index, offset = (next) ? 1 : -1;
	while ((index = (index + offset + length) % length) != start) {
		Control child = children [index];
		if (!child.isDisposed () && child.isTabItem ()) {
			if (child.setTabItemFocus (next)) return true;
		}
	}
	return false;
}

boolean traverseReturn () {
	return false;
}

boolean traversePage (boolean next) {
	return false;
}

boolean traverseMnemonic (char key) {
	return mnemonicHit (key);
}

/**
 * Forces all outstanding paint requests for the widget
 * to be processed before this method returns. If there
 * are no outstanding paint request, this method does
 * nothing.
 * <p>
 * Note: This method does not cause a redraw.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #redraw()
 * @see #redraw(int, int, int, int, boolean)
 * @see PaintListener
 * @see SWT#Paint
 */
public void update () {
	checkWidget ();
	update (false, true);
}

void update (boolean all, boolean flush) {
//	checkWidget();
	if (!OS.gtk_widget_get_visible (topHandle ())) return;
	if (!gtk_widget_get_realized (handle)) return;
	long /*int*/ window = paintWindow ();
	if (flush) display.flushExposes (window, all);
	/*
	 * Do not send expose events on GTK 3.16.0+
	 * It's worth checking whether can be removed on all GTK 3 versions.
	 */
	if (OS.GTK_VERSION < OS.VERSION(3, 16, 0)) {
		OS.gdk_window_process_updates (window, all);
	}
	OS.gdk_flush ();
}

void updateBackgroundMode () {
	int oldState = state & PARENT_BACKGROUND;
	checkBackground ();
	if (oldState != (state & PARENT_BACKGROUND)) {
		setBackground ();
	}
}

void updateLayout (boolean all) {
	/* Do nothing */
}

@Override
long /*int*/ windowProc (long /*int*/ handle, long /*int*/ arg0, long /*int*/ user_data) {
	switch ((int)/*64*/user_data) {
		case EXPOSE_EVENT_INVERSE: {
			if ((state & OBSCURED) != 0) break;
			if (OS.USE_CAIRO) {
				Control control = findBackgroundControl ();
				boolean draw = control != null && control.backgroundImage != null;
				if (OS.GTK3 && !draw && (state & CANVAS) != 0) {
					GdkRGBA rgba = new GdkRGBA();
					long /*int*/ context = OS.gtk_widget_get_style_context (handle);
					OS.gtk_style_context_get_background_color (context, OS.GTK_STATE_FLAG_NORMAL, rgba);
					draw = rgba.alpha == 0;
				}
				if (draw) {
					if (OS.GTK3) {
						long /*int*/ cairo = arg0;
						GdkRectangle rect = new GdkRectangle ();
						OS.gdk_cairo_get_clip_rectangle (cairo, rect);
						if (control == null) control = this;
						long /*int*/ window = OS.gtk_widget_get_window (handle);
						if (window != 0) {
							drawBackground (control, window, 0, 0, rect.x, rect.y, rect.width, rect.height);
						} else {
							drawBackground (control, 0, cairo, 0, rect.x, rect.y, rect.width, rect.height);
						}
					} else {
						GdkEventExpose gdkEvent = new GdkEventExpose ();
						OS.memmove (gdkEvent, arg0, GdkEventExpose.sizeof);
						long /*int*/ paintWindow = paintWindow();
						long /*int*/ window = gdkEvent.window;
						if (window != paintWindow) break;
						drawBackground(control, window, gdkEvent.region, gdkEvent.area_x, gdkEvent.area_y, gdkEvent.area_width, gdkEvent.area_height);
					}
				}
			}
			break;
		}
	}
	return super.windowProc (handle, arg0, user_data);
}

/**
 * Gets the position of the top left corner of the control in root window (display) coordinates.
 *
 * @return the origin
 */
Point getWindowOrigin () {
	int [] x = new int [1];
	int [] y = new int [1];

	long /*int*/ window = eventWindow ();
	OS.gdk_window_get_origin (window, x, y);

	return new Point (x [0], y [0]);
}
}
