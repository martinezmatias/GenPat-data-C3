package org.eclipse.swt.widgets;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */

import org.eclipse.swt.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.graphics.*;

/**
 * Instances of this class provide an etched border
 * with an optional title.
 * <p>
 * Shadow styles are hints and may not be honoured
 * by the platform.  To create a group with the
 * default shadow style for the platform, do not
 * specify a shadow style.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SHADOW_ETCHED_IN, SHADOW_ETCHED_OUT, SHADOW_IN, SHADOW_OUT, SHADOW_NONE</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * Note: Only one of the above styles may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 */
public class Group extends Composite {
	int clientHandle, labelHandle;
	String text = "";

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
public Group (Composite parent, int style) {
	super (parent, checkStyle (style));
}

static int checkStyle (int style) {
	/*
	* Even though it is legal to create this widget
	* with scroll bars, they serve no useful purpose
	* because they do not automatically scroll the
	* widget's client area.  The fix is to clear
	* the SWT style.
	*/
	return style & ~(SWT.H_SCROLL | SWT.V_SCROLL);
}

int clientHandle () {
	return clientHandle;
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget ();
	if (layout==null) return computeNativeSize(handle, wHint, hHint, changed);

	Point size = layout.computeSize (this, wHint, hHint, changed);
	if (size.x == 0) size.x = DEFAULT_WIDTH;
	if (size.y == 0) size.y = DEFAULT_HEIGHT;
	if (wHint != SWT.DEFAULT) size.x = wHint;
	if (hHint != SWT.DEFAULT) size.y = hHint;
	Rectangle trim = computeTrim (0, 0, size.x, size.y);
	return new Point (trim.width, trim.height);
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
	checkWidget();	
	int fixedWidth = OS.GTK_WIDGET_WIDTH (fixedHandle);
	int fixedHeight = OS.GTK_WIDGET_HEIGHT (fixedHandle);
	int clientX = OS.GTK_WIDGET_X (clientHandle);
	int clientY = OS.GTK_WIDGET_Y (clientHandle);
	int clientWidth = OS.GTK_WIDGET_WIDTH (clientHandle);
	int clientHeight = OS.GTK_WIDGET_HEIGHT (clientHandle);	
	x -= clientX;
	y -= clientY;
	//FIXME - why we have to add extra space
	width += 15 + fixedWidth - clientWidth;
	height += 15 + fixedHeight - clientHeight;
	return new Rectangle (x, y, width, height);
}

void createHandle(int index) {
	state |= HANDLE;
	fixedHandle = OS.gtk_fixed_new ();
	if (fixedHandle == 0) error (SWT.ERROR_NO_HANDLES);
	OS.gtk_fixed_set_has_window (fixedHandle, true);
	handle = OS.gtk_frame_new (null);
	if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	labelHandle = OS.gtk_label_new (null);
	if (labelHandle == 0) error (SWT.ERROR_NO_HANDLES);
	clientHandle = OS.gtk_fixed_new();
	if (clientHandle == 0) error (SWT.ERROR_NO_HANDLES);
	int parentHandle = parent.parentingHandle ();
	OS.gtk_container_add (parentHandle, fixedHandle);
	OS.gtk_container_add (fixedHandle, handle);
	OS.gtk_container_add (handle, clientHandle);
	OS.gtk_widget_show (handle);
	OS.gtk_widget_show (clientHandle);
	OS.gtk_widget_show (fixedHandle);
	
	OS.gtk_frame_set_label_widget (handle, labelHandle);
	
	if ((style & SWT.SHADOW_IN) != 0) {
		OS.gtk_frame_set_shadow_type (handle, OS.GTK_SHADOW_IN);
	}
	if ((style & SWT.SHADOW_OUT) != 0) {
		OS.gtk_frame_set_shadow_type (handle, OS.GTK_SHADOW_OUT);
	}
	if ((style & SWT.SHADOW_ETCHED_IN) != 0) {
		OS.gtk_frame_set_shadow_type (handle, OS.GTK_SHADOW_ETCHED_IN);
	}
	if ((style & SWT.SHADOW_ETCHED_OUT) != 0) {
		OS.gtk_frame_set_shadow_type (handle, OS.GTK_SHADOW_ETCHED_OUT);
	}
}

void deregister () {
	super.deregister ();
	WidgetTable.remove (clientHandle);
	WidgetTable.remove (labelHandle);
}

int eventHandle () {
	return fixedHandle;
}

public Rectangle getClientArea () {
	checkWidget();
	int width = OS.GTK_WIDGET_WIDTH (clientHandle);
	int height = OS.GTK_WIDGET_HEIGHT (clientHandle);
	return new Rectangle (0, 0, width, height);
}

String getNameText () {
	return getText ();
}

/**
 * Returns the receiver's text, which is the string that the
 * is used as the <em>title</em>. If the text has not previously
 * been set, returns an empty string.
 *
 * @return the text
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public String getText () {
	checkWidget();
	return text;
}

int parentingHandle() {
	return clientHandle;
}

void register () {
	super.register ();
	WidgetTable.put (clientHandle, this);
	WidgetTable.put (labelHandle, this);
}

void releaseHandle () {
	super.releaseHandle ();
	clientHandle = labelHandle = 0;
}

void releaseWidget () {
	super.releaseWidget ();
	text = null;
}

void setFontDescription (int font) {
	super.setFontDescription (font);
	OS.gtk_widget_modify_font (labelHandle, font);
}

void setForegroundColor (GdkColor color) {
	super.setForegroundColor (color);
	OS.gtk_widget_modify_fg (labelHandle, 0, color);
}

/**
 * Sets the receiver's text, which is the string that will
 * be displayed as the receiver's <em>title</em>, to the argument,
 * which may not be null. 
 *
 * @param text the new text
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setText (String string) {
	checkWidget();
	if (string == null) error (SWT.ERROR_NULL_ARGUMENT);
	text = string;
	int length = string.length ();
	char [] text = new char [length + 1];
	string.getChars (0, length, text, 0);
	for (int i=0; i<length; i++) {
		if (text [i] == '&') text [i] = '_';
	}
	byte [] buffer = Converter.wcsToMbcs (null, text);
	OS.gtk_label_set_text_with_mnemonic (labelHandle, buffer);
	OS.gtk_widget_show (labelHandle);
}

}
