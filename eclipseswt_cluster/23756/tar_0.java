/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.dnd;

 
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.Converter;
import org.eclipse.swt.internal.gtk.GtkSelectionData;
import org.eclipse.swt.internal.gtk.GtkTargetEntry;
import org.eclipse.swt.internal.gtk.OS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

class ClipboardProxy {
	/* Data is not flushed to the clipboard immediately.
	 * This class will remember the data and provide it when requested. 
	 */
	Object[] clipboardData;
	Transfer[] clipboardDataTypes;
	Object[] primaryClipboardData;
	Transfer[] primaryClipboardDataTypes;

	Display display;
	boolean onPrimary = false;
	boolean onClipboard = false;
	Clipboard activeClipboard = null;
	Callback getFunc;
	Callback clearFunc;
	
	static String ID = "CLIPBOARD PROXY OBJECT"; //$NON-NLS-1$

static ClipboardProxy _getInstance(final Display display) {
	ClipboardProxy proxy = (ClipboardProxy) display.getData(ID);
	if (proxy != null) return proxy;
	proxy = new ClipboardProxy(display);
	display.setData(ID, proxy);
	display.addListener(SWT.Dispose, new Listener() {
		public void handleEvent(Event event) {
			ClipboardProxy clipbordProxy = (ClipboardProxy)display.getData(ID);
			if (clipbordProxy == null) return;
			display.setData(ID, null);
			clipbordProxy.dispose();
		}
	});
	return proxy;
}	

ClipboardProxy(Display display) {	
	this.display = display;
	getFunc = new Callback( this, "getFunc", 4); //$NON-NLS-1$
	clearFunc = new Callback( this, "clearFunc", 2); //$NON-NLS-1$
}

void clear (int clipboards) {
	if ((clipboards & DND.CLIPBOARD) != 0 && onClipboard) {
		OS.gtk_clipboard_clear(Clipboard.GTKCLIPBOARD);
	}
	if ((clipboards & DND.SELECTION_CLIPBOARD) != 0 && onPrimary) {
		OS.gtk_clipboard_clear(Clipboard.GTKPRIMARYCLIPBOARD);
	}
}

int /*long*/ clearFunc(int /*long*/ clipboard,int /*long*/ user_data_or_owner){
	if (clipboard == Clipboard.GTKCLIPBOARD) {
		onClipboard = false;
		clipboardData = null;
		clipboardDataTypes = null;
	}
	if (clipboard == Clipboard.GTKPRIMARYCLIPBOARD) {
		onPrimary = false;
		primaryClipboardData = null;
		primaryClipboardDataTypes = null;
	}
	return 1;
}

void dispose () {
	if (display == null) return;
	if (onPrimary) OS.gtk_clipboard_clear(Clipboard.GTKPRIMARYCLIPBOARD);
	onPrimary = false;
	if (onClipboard) OS.gtk_clipboard_clear(Clipboard.GTKCLIPBOARD);
	onClipboard = false;
	display = null;
	if (getFunc != null ) getFunc.dispose();
	getFunc = null;
	if (clearFunc != null) clearFunc.dispose();
	clearFunc = null;
	clipboardData = null;
	clipboardDataTypes = null;
	primaryClipboardData = null;
	primaryClipboardDataTypes = null;
}

/**
 * This function provides the data to the clipboard on request.
 * When this clipboard is disposed, the data will no longer be available.
 */
int /*long*/ getFunc( int /*long*/ clipboard, int /*long*/ selection_data, int /*long*/ info, int /*long*/ user_data_or_owner){
	if (selection_data == 0) return 0;
	GtkSelectionData selectionData = new GtkSelectionData();
	OS.memmove(selectionData, selection_data, GtkSelectionData.sizeof);
	TransferData tdata = new TransferData();
	tdata.type = selectionData.target;
	Transfer[] types = (clipboard == Clipboard.GTKCLIPBOARD) ? clipboardDataTypes : primaryClipboardDataTypes;
	int index = -1;
	for (int i = 0; i < types.length; i++) {
		if (types[i].isSupportedType(tdata)) {
			index = i;
			break;
		}
	}
	if (index == -1) return 0;
	Object[] data = (clipboard == Clipboard.GTKCLIPBOARD) ? clipboardData : primaryClipboardData;
	types[index].javaToNative(data[index], tdata);
	if (tdata.format < 8 || tdata.format % 8 != 0) {
		return 0;
	}
	OS.gtk_selection_data_set(selection_data, tdata.type, tdata.format, tdata.pValue, tdata.length);	
	return 1;
}

boolean setData(Object[] data, Transfer[] dataTypes) {
	return setData(data, dataTypes, DND.CLIPBOARD);
}

boolean setData(Object[] data, Transfer[] dataTypes, int clipboards) {	
	GtkTargetEntry[] entries = new  GtkTargetEntry [0];
	int /*long*/ pTargetsList = 0;
	boolean result = false;
	try {
		for (int i = 0; i < dataTypes.length; i++) {
			Transfer transfer = dataTypes[i];
			int[] typeIds = transfer.getTypeIds();
			String[] typeNames = transfer.getTypeNames();
			for (int j = 0; j < typeIds.length; j++) {
				GtkTargetEntry	entry = new GtkTargetEntry();						
				entry.info = typeIds[j];
				byte[] buffer = Converter.wcsToMbcs(null, typeNames[j], true);
				int /*long*/ pName = OS.g_malloc(buffer.length);
				OS.memmove(pName, buffer, buffer.length);
				entry.target = pName;
				GtkTargetEntry[] tmp = new GtkTargetEntry [entries.length + 1];
				System.arraycopy(entries, 0, tmp, 0, entries.length);
				tmp[entries.length] = entry;
				entries = tmp;				
			}	
		}
		
		pTargetsList = OS.g_malloc(GtkTargetEntry.sizeof * entries.length);
		int offset = 0;
		for (int i = 0; i < entries.length; i++) {
			OS.memmove(pTargetsList + offset, entries[i], GtkTargetEntry.sizeof);
			offset += GtkTargetEntry.sizeof;
		}
		if ((clipboards & DND.CLIPBOARD) != 0) {
			if (onClipboard) OS.gtk_clipboard_clear(Clipboard.GTKCLIPBOARD);
			clipboardData = data;
			clipboardDataTypes = dataTypes;
			result = onClipboard = OS.gtk_clipboard_set_with_data(Clipboard.GTKCLIPBOARD, pTargetsList, entries.length, getFunc.getAddress(), clearFunc.getAddress(), 0);
		}
		if ((clipboards & DND.SELECTION_CLIPBOARD) != 0) {
			if (onPrimary) OS.gtk_clipboard_clear(Clipboard.GTKPRIMARYCLIPBOARD);
			primaryClipboardData = data;
			primaryClipboardDataTypes = dataTypes;
			result = onPrimary = OS.gtk_clipboard_set_with_data(Clipboard.GTKPRIMARYCLIPBOARD, pTargetsList, entries.length, getFunc.getAddress(), clearFunc.getAddress(), 0);
		}
		
	} finally {
		for (int i = 0; i < entries.length; i++) {
			GtkTargetEntry entry = entries[i];
			if( entry.target != 0) OS.g_free(entry.target);
		}
		if (pTargetsList != 0) OS.g_free(pTargetsList);
	}
	return result;
}
}
