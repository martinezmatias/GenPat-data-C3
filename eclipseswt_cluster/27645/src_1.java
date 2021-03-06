/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.opengl;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.internal.cocoa.*;
import org.eclipse.swt.opengl.GLData;

/**
 * GLCanvas is a widget capable of displaying OpenGL content.
 * 
 * @see GLData
 * @see <a href="http://www.eclipse.org/swt/snippets/#opengl">OpenGL snippets</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 3.2
 */

public class GLCanvas extends Canvas {
	SWTOpenGLView glView;
	NSOpenGLPixelFormat pixelFormat;
	
	static final int MAX_ATTRIBUTES = 32;
	static final String ADD_WIDGET_KEY = "org.eclipse.swt.internal.addWidget"; //$NON-NLS-1$
	static final String EVENT_VIEW_KEY = "org.eclipse.swt.internal.eventView"; //$NON-NLS-1$
	static final String PAINT_VIEW_KEY = "org.eclipse.swt.internal.paintView"; //$NON-NLS-1$

/**
 * Create a GLCanvas widget using the attributes described in the GLData
 * object provided.
 *
 * @param parent a composite widget
 * @param style the bitwise OR'ing of widget styles
 * @param data the requested attributes of the GLCanvas
 *
 * @exception IllegalArgumentException
 * <ul><li>ERROR_NULL_ARGUMENT when the data is null
 *     <li>ERROR_UNSUPPORTED_DEPTH when the requested attributes cannot be provided</ul> 
 * </ul>
 */
public GLCanvas (Composite parent, int style, GLData data) {
	super (parent, style);
	if (data == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	int attrib [] = new int [MAX_ATTRIBUTES];
	int pos = 0;

	if (data.doubleBuffer) attrib [pos++] = OS.NSOpenGLPFADoubleBuffer;
	
	if (data.stereo) attrib [pos++] = OS.NSOpenGLPFAStereo;

	/*
	 * Feature in Cocoa: NSOpenGL/CoreOpenGL only supports specifying the total number of bits
	 * in the size of the color component. If specified, the color size is the sum of the red, green
	 * and blue values in the GLData. 
	 */
	if ((data.redSize + data.blueSize + data.greenSize) > 0) {
		attrib [pos++] = OS.NSOpenGLPFAColorSize;
		attrib [pos++] = data.redSize + data.greenSize + data.blueSize;
	}
	
	if (data.alphaSize > 0) {
		attrib [pos++] = OS.NSOpenGLPFAAlphaSize;
		attrib [pos++] = data.alphaSize;
	}
	
	if (data.depthSize > 0) {
		attrib [pos++] = OS.NSOpenGLPFADepthSize;
		attrib [pos++] = data.depthSize;
	}
	
	if (data.stencilSize > 0) {
		attrib [pos++] = OS.NSOpenGLPFAStencilSize;
		attrib [pos++] = data.stencilSize;
	}
	
	/*
	 * Feature in Cocoa: NSOpenGL/CoreOpenGL only supports specifying the total number of bits
	 * in the size of the color accumulator component. If specified, the color size is the sum of the red, green,
	 * blue and alpha accum values in the GLData. 
	 */
	if ((data.accumRedSize + data.accumBlueSize + data.accumGreenSize) > 0) {
		attrib [pos++] = OS.NSOpenGLPFAAccumSize;
		attrib [pos++] = data.accumRedSize + data.accumGreenSize + data.accumBlueSize + data.accumAlphaSize;
	}
	
	if (data.sampleBuffers > 0) {
		attrib [pos++] = OS.NSOpenGLPFASampleBuffers;
		attrib [pos++] = data.sampleBuffers;
	}
	
	if (data.samples > 0) {
		attrib [pos++] = OS.NSOpenGLPFASamples;
		attrib [pos++] = data.samples;
	}
	
	attrib [pos++] = 0;
	
	pixelFormat = (NSOpenGLPixelFormat)new NSOpenGLPixelFormat().alloc();
	
	if (pixelFormat == null) {		
		dispose ();
		SWT.error (SWT.ERROR_UNSUPPORTED_DEPTH);
	}
	
	pixelFormat.initWithAttributes(attrib);
	
	glView = (SWTOpenGLView)new SWTOpenGLView().alloc();
	if (glView == null) {		
		dispose ();
		SWT.error (SWT.ERROR_UNSUPPORTED_DEPTH);
	}
	glView.initWithFrame(view.bounds(), pixelFormat);
	
	if (data.shareContext != null) {
		NSOpenGLContext ctx = data.shareContext.glView.openGLContext();
		NSOpenGLContext newContext = (NSOpenGLContext) new NSOpenGLContext().alloc();
		newContext.initWithFormat(pixelFormat, ctx);
		glView.setOpenGLContext(newContext);
		newContext.release();
	}
	glView.setAutoresizingMask(OS.NSViewWidthSizable | OS.NSViewHeightSizable);
	view.addSubview(glView);
	Display display = getDisplay();
	display.setData(ADD_WIDGET_KEY, new Object[] {glView, this});
	display.setData(EVENT_VIEW_KEY, glView);
	display.setData(PAINT_VIEW_KEY, view);
	
	Listener listener = new Listener () {
		public void handleEvent (Event event) {
			switch (event.type) {
				case SWT.Dispose:
					event.display.setData(ADD_WIDGET_KEY, new Object[] {glView, null});
					event.display.setData(EVENT_VIEW_KEY, null);
					event.display.setData(PAINT_VIEW_KEY, null);

					if (glView != null) {
						glView.clearGLContext();
						glView.release();
					}
					glView = null;
					if (pixelFormat != null) pixelFormat.release();
					pixelFormat = null;
					break;
			}
		}
	};
	addListener (SWT.Dispose, listener);
}

/**
 * Returns a GLData object describing the created context.
 *  
 * @return GLData description of the OpenGL context attributes
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public GLData getGLData () {
	checkWidget ();
	GLData data = new GLData ();
	int /*long*/ [] value = new int /*long*/ [1];
	pixelFormat.getValues(value, OS.NSOpenGLPFADoubleBuffer, 0);
	data.doubleBuffer = value [0] != 0;
	pixelFormat.getValues(value, OS.NSOpenGLPFAStereo, 0);
	data.stereo = value [0] != 0;

	pixelFormat.getValues(value, OS.NSOpenGLPFAAlphaSize, 0);
	data.alphaSize = (int/*64*/)value [0];

	/*
	 * Feature in Cocoa: NSOpenGL/CoreOpenGL only supports specifying the total number of bits
	 * in the size of the color component. For compatibility we split the color size less any alpha
	 * into thirds and allocate a third to each color.
	 */
	pixelFormat.getValues(value, OS.NSOpenGLPFAColorSize, 0);

	int colorSize = ((int/*64*/)(value[0] - data.alphaSize)) / 3;

	data.redSize = colorSize;
	data.greenSize = colorSize;
	data.blueSize = colorSize;
	
	pixelFormat.getValues(value, OS.NSOpenGLPFADepthSize, 0);
	data.depthSize = (int/*64*/)value [0];
	pixelFormat.getValues(value, OS.NSOpenGLPFAStencilSize, 0);
	data.stencilSize = (int/*64*/)value [0];
	
	/*
	 * Feature(?) in Cocoa: NSOpenGL/CoreOpenGL doesn't support setting an accumulation buffer alpha, but
	 * has an alpha if the color values for the accumulation buffer were set. Allocate the values evenly
	 * in that case.
	 */
	pixelFormat.getValues(value, OS.NSOpenGLPFAAccumSize, 0);

	int accumColorSize = (int/*64*/)(value[0]) / 4;	
	data.accumRedSize = accumColorSize;
	data.accumGreenSize = accumColorSize;
	data.accumBlueSize = accumColorSize;
	data.accumAlphaSize = accumColorSize;

	pixelFormat.getValues(value, OS.NSOpenGLPFASampleBuffers, 0);
	data.sampleBuffers = (int/*64*/)value [0];
	pixelFormat.getValues(value, OS.NSOpenGLPFASamples, 0);
	data.samples = (int/*64*/)value [0];
	return data;
}

/**
 * Returns a boolean indicating whether the receiver's OpenGL context
 * is the current context.
 *  
 * @return true if the receiver holds the current OpenGL context,
 * false otherwise
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean isCurrent () {
	checkWidget ();
	return NSOpenGLContext.currentContext().id == glView.openGLContext().id;
}

/**
 * Sets the OpenGL context associated with this GLCanvas to be the
 * current GL context.
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCurrent () {
	checkWidget ();
	glView.openGLContext().makeCurrentContext();
}

/**
 * Swaps the front and back color buffers.
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void swapBuffers () {
	checkWidget ();
	glView.openGLContext().flushBuffer();
}
}
