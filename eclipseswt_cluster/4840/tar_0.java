/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.cocoa;

public class NSAttributedString extends NSObject {

public NSAttributedString() {
	super();
}

public NSAttributedString(int /*long*/ id) {
	super(id);
}

public NSAttributedString(id id) {
	super(id);
}

public NSDictionary attributesAtIndex(int location, int range, NSRange rangeLimit) {
	int result = OS.objc_msgSend(this.id, OS.sel_attributesAtIndex_longestEffectiveRange_inRange_, location, range, rangeLimit);
	return result != 0 ? new NSDictionary(result) : null;
}

public NSRange doubleClickAtIndex(int location) {
	NSRange result = new NSRange();
	OS.objc_msgSend_stret(result, this.id, OS.sel_doubleClickAtIndex_, location);
	return result;
}

public void drawAtPoint(NSPoint point) {
	OS.objc_msgSend(this.id, OS.sel_drawAtPoint_, point);
}

public void drawInRect(NSRect rect) {
	OS.objc_msgSend(this.id, OS.sel_drawInRect_, rect);
}

public NSAttributedString initWithString(NSString str, NSDictionary attrs) {
	int result = OS.objc_msgSend(this.id, OS.sel_initWithString_attributes_, str != null ? str.id : 0, attrs != null ? attrs.id : 0);
	return result == this.id ? this : (result != 0 ? new NSAttributedString(result) : null);
}

public int nextWordFromIndex(int location, boolean isForward) {
	return OS.objc_msgSend(this.id, OS.sel_nextWordFromIndex_forward_, location, isForward);
}

public NSSize size() {
	NSSize result = new NSSize();
	OS.objc_msgSend_stret(result, this.id, OS.sel_size);
	return result;
}

public NSAttributedString attributedSubstringFromRange(NSRange range) {
	int result = OS.objc_msgSend(this.id, OS.sel_attributedSubstringFromRange_, range);
	return result == this.id ? this : (result != 0 ? new NSAttributedString(result) : null);
}

public NSAttributedString initWithString(NSString str) {
	int result = OS.objc_msgSend(this.id, OS.sel_initWithString_, str != null ? str.id : 0);
	return result == this.id ? this : (result != 0 ? new NSAttributedString(result) : null);
}

public int length() {
	return OS.objc_msgSend(this.id, OS.sel_length);
}

public NSString string() {
	int result = OS.objc_msgSend(this.id, OS.sel_string);
	return result != 0 ? new NSString(result) : null;
}

}
