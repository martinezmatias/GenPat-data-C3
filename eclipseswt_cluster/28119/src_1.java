/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.dnd;

import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * The class <code>TextTransfer</code> provides a platform specific mechanism 
 * for converting plain text represented as a java <code>String</code> 
 * to a platform specific representation of the data and vice versa.
 * 
 * <p>An example of a java <code>String</code> containing plain text is shown 
 * below:</p>
 * 
 * <code><pre>
 *     String textData = "Hello World";
 * </code></pre>
 * 
 * @see Transfer
 */
public class TextTransfer extends ByteArrayTransfer {

	private static TextTransfer _instance = new TextTransfer();
	private static final String COMPOUND_TEXT = "COMPOUND_TEXT"; //$NON-NLS-1$
	private static final String UTF8_STRING = "UTF8_STRING"; //$NON-NLS-1$
	private static final int COMPOUND_TEXT_ID = registerType(COMPOUND_TEXT);
	private static final int UTF8_STRING_ID = registerType(UTF8_STRING);

private TextTransfer() {}

/**
 * Returns the singleton instance of the TextTransfer class.
 *
 * @return the singleton instance of the TextTransfer class
 */
public static TextTransfer getInstance () {
	return _instance;
}

/**
 * This implementation of <code>javaToNative</code> converts plain text
 * represented by a java <code>String</code> to a platform specific representation.
 * 
 * @param object a java <code>String</code> containing text
 * @param transferData an empty <code>TransferData</code> object; this object
 *  will be filled in on return with the platform specific format of the data
 *  
 * @see Transfer#javaToNative
 */
public void javaToNative (Object object, TransferData transferData) {
	transferData.result = 0;
	if (!checkText(object) || !isSupportedType(transferData)) {
		DND.error(DND.ERROR_INVALID_DATA);
	}
	String string = (String)object;
	byte[] buffer = Converter.wcsToMbcs (null, string, true);
	if  (transferData.type ==  COMPOUND_TEXT_ID) {
		int /*long*/[] encoding = new int /*long*/[1];
		int[] format = new int[1];
		int /*long*/[] ctext = new int /*long*/[1];
		int[] length = new int[1];
		boolean result = OS.gdk_utf8_to_compound_text(buffer, encoding, format, ctext, length);
		if (!result) return;
		transferData.type = encoding[0];
		transferData.format = format[0];
		transferData.length = length[0];
		transferData.pValue = ctext[0];
		transferData.result = 1;
	} 
	if (transferData.type == UTF8_STRING_ID) {
		int /*long*/ pValue = OS.g_malloc(buffer.length);
		if (pValue ==  0) return;
		OS.memmove(pValue, buffer, buffer.length);
		transferData.type = UTF8_STRING_ID;
		transferData.format = 8;
		transferData.length = buffer.length - 1;
		transferData.pValue = pValue;
		transferData.result = 1;
	}
}

/**
 * This implementation of <code>nativeToJava</code> converts a platform specific 
 * representation of plain text to a java <code>String</code>.
 * 
 * @param transferData the platform specific representation of the data to be converted
 * @return a java <code>String</code> containing text if the conversion was successful; otherwise null
 * 
 * @see Transfer#nativeToJava
 */
public Object nativeToJava(TransferData transferData){
	if (!isSupportedType(transferData) ||  transferData.pValue == 0) return null;
	byte[] buffer = null;
	if (transferData.type == COMPOUND_TEXT_ID) { 	
		int /*long*/[] list = new int /*long*/[1];
		int count = OS.gdk_text_property_to_utf8_list(transferData.type, transferData.format, transferData.pValue, transferData.length, list);
		if (count == 0) return null;
		int /*long*/[] ptr = new int /*long*/[1];
		OS.memmove(ptr, list[0], OS.PTR_SIZEOF);
		int length = OS.strlen(ptr[0]);
		buffer = new byte[length];
		OS.memmove(buffer, ptr[0], length);
		OS.g_strfreev(list[0]);
	}
	if (transferData.type == UTF8_STRING_ID) {
		int size = transferData.format * transferData.length / 8;
		if (size == 0) return null;
		buffer = new byte[size];
		OS.memmove(buffer, transferData.pValue, size);
	}
	if (buffer == null) return null;
	// convert byte array to a string
	char [] unicode = Converter.mbcsToWcs (null, buffer);
	String string = new String (unicode);
	int end = string.indexOf('\0');
	return (end == -1) ? string : string.substring(0, end);
}

protected int[] getTypeIds() {
	return new int[] {UTF8_STRING_ID, COMPOUND_TEXT_ID};
}

protected String[] getTypeNames() {
	return new String[] {UTF8_STRING, COMPOUND_TEXT};
}

boolean checkText(Object object) {
	return (object != null && object instanceof String && ((String)object).length() > 0);
}

protected boolean validate(Object object) {
	return checkText(object);
}
}
