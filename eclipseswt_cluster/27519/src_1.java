package org.eclipse.swt.layout;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */
import org.eclipse.swt.*;

/**
 * Instances of this class are used to define the attachments 
 * of a control in a <code>FormLayout</code>. 
 * <p>
 * To set a <code>FormData</code> object into a control, you use the 
 * <code>setLayoutData ()</code> method. To define attachments for the 
 * <code>FormData</code>, set the fields directly, like this:
 * <pre>
 * 		FormData data = new FormData();
 * 		data.left = new FormAttachment(0,5);
 * 		data.right = new FormAttachment(100,-5);
 * 		button.setLayoutData(formData);
 * </pre>
 * </p>
 * <p>
 * <code>FormData</code> contains the <code>FormAttachments</code> for 
 * each edge of the control that the <code>FormLayout</code> uses to
 * determine the size and position of the control. <code>FormData</code>
 * objects also allow you to set the width and height of controls within
 * a <code>FormLayout</code>. 
 * </p>
 * 
 * @see FormLayout
 * @see FormAttachment
 * 
 * @since 2.0
 */
public final class FormData {
	/**
	 * height specifies the desired height in pixels given
	 * by the user
	 */
	public int height;
	/**
	 * width specifies the desired width in pixels given
	 * by the user
	 */
	public int width;
	/**
	 * left specifies the attachment of the left side of 
	 * the control.
	 */
	public FormAttachment left;
	/**
	 * right specifies the attachment of the right side of
	 * the control.
	 */
	public FormAttachment right;
	/**
	 * top specifies the attachment of the top of the control.
	 */
	public FormAttachment top;
	/**
	 * bottom specifies the attachment of the bottom of the
	 * control.
	 */
	public FormAttachment bottom;
	
	int cacheHeight, cacheWidth;
	boolean isVisited;
	
public FormData () {
	this (SWT.DEFAULT, SWT.DEFAULT);
}
	
public FormData (int width, int height) {
	this.width = width;
	this.height = height;
}

FormAttachment getBottomAttachment () {
	if (isVisited) return new FormAttachment (0, cacheHeight);
	if (bottom == null) {
		if (top == null) return new FormAttachment (0, cacheHeight);
		return getTopAttachment ().plus (cacheHeight);
	}
	if (bottom.control == null) return bottom;
	isVisited = true;
	FormData bottomData = (FormData) bottom.control.getLayoutData ();
	FormAttachment topAttachment = bottomData.getTopAttachment ();
	isVisited = false;
	return topAttachment.plus (bottom.offset);	
}

FormAttachment getLeftAttachment () {
	if (isVisited) return new FormAttachment (0, 0);
	if (left == null) {
		if (right == null) return new FormAttachment (0, 0);
		return getRightAttachment ().minus (cacheWidth);
	}
	if (left.control == null) return left;
	isVisited = true;
	FormData leftData = (FormData) left.control.getLayoutData ();
	FormAttachment rightAttachment = leftData.getRightAttachment ();
	isVisited = false; 
	return rightAttachment.plus (left.offset); 
}	

FormAttachment getRightAttachment () {
	if (isVisited) return new FormAttachment (0, cacheWidth);
	if (right == null) {
		if (left == null) return new FormAttachment (0, cacheWidth);
		return getLeftAttachment ().plus (cacheWidth);
	}
	if (right.control == null) return right;
	isVisited = true;
	FormData rightData = (FormData) right.control.getLayoutData ();
	FormAttachment leftAttachment = rightData.getLeftAttachment ();
	isVisited = false;
	return leftAttachment.plus (right.offset);
}

FormAttachment getTopAttachment () {
	if (isVisited) return new FormAttachment (0, 0);
	if (top == null) {
		if (bottom == null) return new FormAttachment (0, 0);
		return getBottomAttachment ().minus (cacheHeight);
	}
	if (top.control == null) return top;
	isVisited = true;
	FormData topData = (FormData) top.control.getLayoutData ();
	FormAttachment bottomAttachment = topData.getBottomAttachment ();
	isVisited = false;
	return bottomAttachment.plus (top.offset);
}

}
