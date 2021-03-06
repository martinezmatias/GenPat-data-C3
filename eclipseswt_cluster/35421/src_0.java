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
package org.eclipse.swt.custom;


import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;


/**
* UNDER CONSTRUCTION
*/

public class CTabItem extends Item {
	CTabFolder parent;
	int x,y,width,height = 0;
	Control control; // the tab page
	
	String toolTipText;
	String shortenedText;
	int shortenedTextWidth;
	
	Rectangle closeRect = new Rectangle(0, 0, 0, 0);
	int closeImageState = CTabFolder.NONE;
	boolean showClose = false;
	
	// internal constants
	static final int LEFT_MARGIN = 7;
	static final int RIGHT_MARGIN = 6;
	static final int TOP_MARGIN = 2;
	static final int BOTTOM_MARGIN = 2;
	static final int INTERNAL_SPACING = 2;
	static final int FLAGS = SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC;
	static final String ellipsis = "..."; //$NON-NLS-1$
	
/**
 * Constructs a new instance of this class given its parent
 * (which must be a <code>CTabFolder</code>) and a style value
 * describing its behavior and appearance. The item is added
 * to the end of the items maintained by its parent.
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
 * @param parent a CTabFolder which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 * </ul>
 *
 * @see SWT
 * @see Widget#getStyle()
 */
public CTabItem (CTabFolder parent, int style) {
	this(parent, style, parent.getItemCount());
}
/**
 * Constructs a new instance of this class given its parent
 * (which must be a <code>CTabFolder</code>), a style value
 * describing its behavior and appearance, and the index
 * at which to place it in the items maintained by its parent.
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
 * @param parent a CTabFolder which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 * @param index the index to store the receiver in its parent
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 * </ul>
 *
 * @see SWT
 * @see Widget#getStyle()
 */
public CTabItem (CTabFolder parent, int style, int index) {
	super (parent, checkStyle(style));
	showClose = (style & SWT.CLOSE) != 0;
	parent.createItem (this, index);
}
static int checkStyle(int style) {
	return SWT.NONE;
}
static String shortenText(GC gc, String text, int width) {
	if (gc.textExtent(text, FLAGS).x <= width) return text;
	
	int ellipseWidth = gc.textExtent(ellipsis, FLAGS).x;
	int length = text.length();
	int end = length - 1;
	while (end > 0) {
		text = text.substring(0, end);
		int l1 = gc.textExtent(text, FLAGS).x;
		if (l1 + ellipseWidth <= width) {
			return text + ellipsis;
		}
		end--;
	}
	return text + ellipsis;
}
public void dispose() {
	if (isDisposed ()) return;
	//if (!isValidThread ()) error (SWT.ERROR_THREAD_INVALID_ACCESS);
	parent.destroyItem(this);
	super.dispose();
	parent = null;
	control = null;
	toolTipText = null;
	shortenedText = null;
}
void drawClose(GC gc) {
	if (closeRect.width == 0 || closeRect.height == 0) return;
	Display display = getDisplay();

	// draw X (10x10 or 11x11)
	int indent = Math.max(1, (parent.tabHeight-11)/2);
	int x = closeRect.x + indent - 1;
	int y = closeRect.y + indent;
	switch (closeImageState) {
		case CTabFolder.NORMAL: {
			int[] shape = new int[] {x,y, x+2,y, x+4,y+2, x+5,y+2, x+7,y, x+9,y, 
					                 x+9,y+2, x+7,y+4, x+7,y+5, x+9,y+7, x+9,y+9,
			                         x+7,y+9, x+5,y+7, x+4,y+7, x+2,y+9, x,y+9,
			                         x,y+7, x+2,y+5, x+2,y+4, x,y+2};
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.fillPolygon(shape);
			gc.setForeground(CTabFolder.borderColor);
			gc.drawPolygon(shape);
			break;
		}
		case CTabFolder.HOT: {
			int[] shape = new int[] {x,y, x+2,y, x+4,y+2, x+5,y+2, x+7,y, x+9,y, 
					                 x+9,y+2, x+7,y+4, x+7,y+5, x+9,y+7, x+9,y+9,
			                         x+7,y+9, x+5,y+7, x+4,y+7, x+2,y+9, x,y+9,
			                         x,y+7, x+2,y+5, x+2,y+4, x,y+2};
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.fillPolygon(shape);
			Color border = new Color(display, CTabFolder.CLOSE_BORDER);
			gc.setForeground(border);
			gc.drawPolygon(shape);
			border.dispose();
			break;
		}
		case CTabFolder.SELECTED: {
			int[] shape = new int[] {x+1,y+1, x+3,y+1, x+5,y+3, x+6,y+3, x+8,y+1, x+10,y+1, 
					                 x+10,y+3, x+8,y+5, x+8,y+6, x+10,y+8, x+10,y+10,
			                         x+8,y+10, x+6,y+8, x+5,y+8, x+3,y+10, x+1,y+10,
			                         x+1,y+8, x+3,y+6, x+3,y+5, x+1,y+3};
			Color fill = new Color(display, CTabFolder.CLOSE_FILL);
			gc.setBackground(fill);
			gc.fillPolygon(shape);
			fill.dispose();
			Color border = new Color(display, CTabFolder.CLOSE_BORDER);
			gc.setForeground(border);
			gc.drawPolygon(shape);
			border.dispose();
			break;
		}
		case CTabFolder.NONE: {
			int[] shape = new int[] {x,y, x+10,y, x+10,y+10, x,y+10};
			parent.drawBackground(gc, shape, false);
			break;
		}
	}
}
void drawSelected(GC gc ) {
	Point size = parent.getSize();
	
	// Draw selection border across all tabs
	int xx = parent.borderLeft;
	int yy = parent.onBottom ? size.y - parent.borderBottom - parent.tabHeight - CTabFolder.HIGHLIGHT_HEADER : parent.borderTop + parent.tabHeight + 1;
	int ww = size.x - parent.borderLeft - parent.borderRight;
	int hh = CTabFolder.HIGHLIGHT_HEADER - 1;
	int[] shape = new int[] {xx,yy, xx+ww,yy, xx+ww,yy+hh, xx,yy+hh};
	parent.drawBackground(gc, shape, true);

	int rightTabEdge = parent.getRightItemEdge();
	// if selected tab scrolled out of view or partially out of view
	// just draw bottom line
	if (!parent.single && parent.selectedIndex != parent.topTabIndex && x + width > rightTabEdge){
		int x1 = Math.max(0, parent.borderLeft - 1);
		int y1 = (parent.onBottom) ? y - 1 : y + height;
		int x2 = size.x - parent.borderRight;
		gc.setForeground(CTabFolder.borderColor);
		gc.drawLine(x1, y1, x2, y1);
		return;
	}
	
	// draw selected tab background and outline
	int extra = CTabFolder.CURVE_WIDTH/2 + 4; // +4 to avoid overlapping with text in next tab
	shape = null;
	if (this.parent.onBottom) {
		int[] left = CTabFolder.BOTTOM_LEFT_CORNER;
		int[] right = parent.curve;
		shape = new int[left.length+right.length+8];
		int index = 0;
		shape[index++] = x; // first point repeated here because below we reuse shape to draw outline
		shape[index++] = y - 1;
		shape[index++] = x;
		shape[index++] = y - 1;
		for (int i = 0; i < left.length/2; i++) {
			shape[index++] = x + left[2*i];
			shape[index++] = y + height + left[2*i+1] - 1;
		}
		for (int i = 0; i < right.length/2; i++) {
			shape[index++] = x + width - extra + right[2*i];
			shape[index++] = y + right[2*i+1] - 2;
		}
		int temp = 0;
		for (int i = 0; i < shape.length/2; i++) {
			if (shape[2*i] > rightTabEdge) {
				if (temp == 0 && i > 0) {
					temp = shape[2*i-1];
				} else {
					temp = y - 1;
				}
				shape[2*i] = rightTabEdge;
				shape[2*i+1] = temp;
			}
		}
		shape[index++] = rightTabEdge;
		shape[index++] = y - 1;
		shape[index++] = x + width + extra;
		shape[index++] = y - 1;
	} else {
		int[] left = CTabFolder.TOP_LEFT_CORNER;
		int[] right = parent.curve;
		shape = new int[left.length+right.length+8];
		int index = 0;
		shape[index++] = x; // first point repeated here because below we reuse shape to draw outline
		shape[index++] = y + height + 1;
		shape[index++] = x;
		shape[index++] = y + height + 1;
		for (int i = 0; i < left.length/2; i++) {
			shape[index++] = x + left[2*i];
			shape[index++] = y + left[2*i+1];
		}
		for (int i = 0; i < right.length/2; i++) {
			shape[index++] = x + width - extra + right[2*i];
			shape[index++] = y + right[2*i+1];
		}
		int temp = 0;
		for (int i = 0; i < shape.length/2; i++) {
			if (shape[2*i] > rightTabEdge) {
				if (temp == 0 && i > 0) {
					temp = shape[2*i-1];
				} else {
					temp = y + height + 1;
				}
				shape[2*i] = rightTabEdge;
				shape[2*i+1] = temp;
			}
		}
		shape[index++] = rightTabEdge;
		shape[index++] = y + height + 1;
		shape[index++] = x + width + extra;
		shape[index++] = y + height + 1;
	}
	parent.drawBackground(gc, shape, true);
	
	if ( x < rightTabEdge) {
		// Limit drawing area of tab
		Region r = new Region();
		r.subtract(r); //clear
		Region clipping = new Region();
		gc.getClipping(clipping);
		r.add(clipping);
		r.intersect(new Rectangle(x, y, Math.min(width, rightTabEdge-x), height));
		gc.setClipping(r);
	
		// draw Image
		int xDraw = x + LEFT_MARGIN;
		Image image = getImage();
		if (image != null) {
			Rectangle imageBounds = image.getBounds();
			int imageX = xDraw;
			int imageHeight = imageBounds.height;
			int imageY = y + (height - imageHeight) / 2;
			imageY += parent.onBottom ? -1 : 1;
			int imageWidth = imageBounds.width * imageHeight / imageBounds.height;
			gc.drawImage(image, 
				         imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height,
				         imageX, imageY, imageWidth, imageHeight);
			xDraw += imageWidth + INTERNAL_SPACING;
		}
		
		// draw Text
		int textWidth = x + width - xDraw - RIGHT_MARGIN;
		if (closeRect.width > 0) textWidth -= closeRect.width + INTERNAL_SPACING;
		if (shortenedText == null || shortenedTextWidth != textWidth) {
			shortenedText = shortenText(gc, getText(), textWidth);
			shortenedTextWidth = textWidth;
		}
		Point extent = gc.textExtent(shortenedText, FLAGS);	
		int textY = y + (height - extent.y) / 2;
		textY += parent.onBottom ? -1 : 1;
		
		gc.setForeground(parent.selectionForeground);
		gc.drawText(shortenedText, xDraw, textY, FLAGS);
		
		if (parent.showClose || showClose) drawClose(gc);
		
		// draw a Focus rectangle
		if (parent.isFocusControl()) {
			Display display = getDisplay();
			gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
			gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.drawFocus(xDraw-3, textY-2, extent.x+6, extent.y+4);
		}
		
		gc.setClipping(clipping);
		r.dispose();
		clipping.dispose();
	}
	
	// draw outline
	shape[0] = Math.max(0, parent.borderLeft - 1);
	shape[shape.length - 2] = size.x - parent.borderRight + 1;
	for (int i = 0; i < shape.length/2; i++) {
		if (shape[2*i + 1] == y + height + 1) shape[2*i + 1] -= 1;
	}
	RGB inside = parent.selectionBackground.getRGB();
	if (parent.selectionBgImage != null || (parent.selectionGradientColors != null && parent.selectionGradientColors.length > 1 && !parent.selectionGradientVertical)) inside = null;
	RGB outside = parent.getBackground().getRGB();		
	if (parent.bgImage != null || (parent.gradientColors != null && parent.gradientColors.length > 1)) outside = null;
	parent.antialias(shape, CTabFolder.borderColor.getRGB(), inside, outside, gc);
	gc.setForeground(CTabFolder.borderColor);
	gc.drawPolyline(shape);
}
void drawUnselected(GC gc) {
	int rightTabEdge = parent.getRightItemEdge();
	// Do not draw partial items
	if (parent.items[parent.topTabIndex] != this && x + width > rightTabEdge){
		return;
	}
	// draw border
	if (parent.indexOf(this) != parent.selectedIndex - 1) {
		gc.setForeground(CTabFolder.borderColor);
		gc.drawLine(x + width - 1, y, x + width - 1, y + height);
	}
	// draw Text
	int textWidth = width - LEFT_MARGIN - RIGHT_MARGIN;
	if (closeRect.width > 0) textWidth -= closeRect.width + INTERNAL_SPACING;
	if (shortenedText == null || shortenedTextWidth != textWidth) {
		shortenedText = shortenText(gc, getText(), textWidth);
		shortenedTextWidth = textWidth;
	}	
	Point extent = gc.textExtent(shortenedText, FLAGS);
	int textY = y + (height - extent.y) / 2;
	textY += parent.onBottom ? -1 : 1;
	gc.setForeground(parent.getForeground());
	gc.drawText(shortenedText, x + LEFT_MARGIN, textY, FLAGS);
	// draw close
	if (parent.showClose || showClose) drawClose(gc);
}
/**
 * Returns a rectangle describing the receiver's size and location
 * relative to its parent.
 *
 * @return the receiver's bounding column rectangle
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Rectangle getBounds () {
	//checkWidget();
	return new Rectangle(x, y, width, height);
}
/**
* Gets the control that is displayed in the content are of the tab item.
*
* @return the control
*
* @exception SWTException <ul>
*    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
*    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
* </ul>
*/
public Control getControl () {
	checkWidget();
	return control;
}
/**
 * Get the image displayed in the tab if the tab is disabled.
 * 
 * @return the disabled image or null
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @deprecated
 */
public Image getDisabledImage(){
	//checkWidget();
	return null;
}
/**
 * Returns the receiver's parent, which must be a <code>CTabFolder</code>.
 *
 * @return the receiver's parent
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public CTabFolder getParent () {
	//checkWidget();
	return parent;
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
	if (toolTipText == null && shortenedText != null) {
		String text = getText();
		if (!shortenedText.equals(text)) return text;
	}
	return toolTipText;
}
void onPaint(GC gc, boolean isSelected) {
	if (width == 0 || height == 0) return;
	if (isSelected) {
		drawSelected(gc);
	} else {
		drawUnselected(gc);
	}
}
int preferredHeight(GC gc) {
	Image image = getImage();
	int h = (image == null) ? 0 : image.getBounds().height;
	String text = getText();
	h = Math.max(h, gc.textExtent(text, FLAGS).y);
	return h + TOP_MARGIN + BOTTOM_MARGIN;
}
int preferredWidth(GC gc, boolean isSelected) {
	int w = 0;
	Image image = getImage();
	if (isSelected && image != null) w += image.getBounds().width;
	String text = getText();
	if (text != null) {
		if (w > 0) w += INTERNAL_SPACING;
		w += gc.textExtent(text, FLAGS).x;
	}
	if (parent.showClose || showClose) {
		if (w > 0) w += INTERNAL_SPACING;
		w += CTabFolder.BUTTON_SIZE;
	}
	if (isSelected) w += 8; // why 8?
	return w + LEFT_MARGIN + RIGHT_MARGIN;
}
/**
 * Sets the control that is used to fill the client area of
 * the tab folder when the user selects the tab item.
 * <p>
 * @param control the new control (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the control has been disposed</li> 
 *    <li>ERROR_INVALID_PARENT - if the control is not in the same widget tree</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setControl (Control control) {
	checkWidget();
	if (control != null) {
		if (control.isDisposed()) SWT.error (SWT.ERROR_INVALID_ARGUMENT);
		if (control.getParent() != parent) SWT.error (SWT.ERROR_INVALID_PARENT);
	}
	if (this.control != null && !this.control.isDisposed()) {
		this.control.setVisible(false);
	}
	this.control = control;
	if (this.control != null) {
		int index = parent.indexOf (this);
		if (index == parent.getSelectionIndex ()){
			this.control.setBounds(parent.getClientArea ());
			this.control.setVisible(true);
		} else {
			this.control.setVisible(false);
		}
	}
}
/**
 * Sets the image that is displayed if the tab item is disabled.
 * Null will clear the image.
 * 
 * @param image the image to be displayed when the item is disabled or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @deprecated
 */
public void setDisabledImage (Image image) {
	checkWidget();
}
public void setImage (Image image) {
	checkWidget();
	if (image != null && image.equals(getImage())) return;
	super.setImage(image);
	if (!parent.updateTabHeight(parent.tabHeight, false)) {
		parent.updateItems();
	}
	parent.redraw();
}
/**
 * Set the widget text.
 * <p>
 * This method sets the widget label.  The label may include
 * mnemonic characters but must not contain line delimiters.
 *
 * @param string the new label for the widget
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the text is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setText (String string) {
	checkWidget();
	if (string.equals(getText())) return;
	super.setText(string);
	shortenedText = null;
	shortenedTextWidth = 0;
	parent.updateItems();
	parent.redraw();
}
/**
 * Sets the receiver's tool tip text to the argument, which
 * may be null indicating that no tool tip text should be shown.
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
	toolTipText = string;
}
}
