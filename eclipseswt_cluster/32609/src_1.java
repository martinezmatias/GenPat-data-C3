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
package org.eclipse.swt.tests.junit;


/**
 * Automated Test Suite for class org.eclipse.swt.dnd.Transfer
 *
 * @see org.eclipse.swt.dnd.Transfer
 */
public class Test_org_eclipse_swt_dnd_Transfer extends SwtTestCase {

public Test_org_eclipse_swt_dnd_Transfer(String name) {
	super(name);
}


protected void setUp() {
}

protected void tearDown() {
}

public void test_Constructor() {
	warnUnimpl("Test test_Constructor not written");
}

public void test_getSupportedTypes() {
	warnUnimpl("Test test_getSupportedTypes not written");
}

public void test_isSupportedTypeLorg_eclipse_swt_dnd_TransferData() {
	warnUnimpl("Test test_isSupportedTypeLorg_eclipse_swt_dnd_TransferData not written");
}

public void test_getTypeIds() {
	warnUnimpl("Test test_getTypeIds not written");
}

public void test_getTypeNames() {
	warnUnimpl("Test test_getTypeNames not written");
}

public void test_javaToNativeLjava_lang_ObjectLorg_eclipse_swt_dnd_TransferData() {
	warnUnimpl("Test test_javaToNativeLjava_lang_ObjectLorg_eclipse_swt_dnd_TransferData not written");
}

public void test_nativeToJavaLorg_eclipse_swt_dnd_TransferData() {
	warnUnimpl("Test test_nativeToJavaLorg_eclipse_swt_dnd_TransferData not written");
}

public void test_registerTypeLjava_lang_String() {
	warnUnimpl("Test test_registerTypeLjava_lang_String not written");
}

public static java.util.Vector methodNames() {
	java.util.Vector methodNames = new java.util.Vector();
	methodNames.addElement("test_Constructor");
	methodNames.addElement("test_getSupportedTypes");
	methodNames.addElement("test_isSupportedTypeLorg_eclipse_swt_dnd_TransferData");
	methodNames.addElement("test_getTypeIds");
	methodNames.addElement("test_getTypeNames");
	methodNames.addElement("test_javaToNativeLjava_lang_ObjectLorg_eclipse_swt_dnd_TransferData");
	methodNames.addElement("test_nativeToJavaLorg_eclipse_swt_dnd_TransferData");
	methodNames.addElement("test_registerTypeLjava_lang_String");
	return methodNames;
}
protected void runTest() throws Throwable {
	if (getName().equals("test_Constructor")) test_Constructor();
	else if (getName().equals("test_getSupportedTypes")) test_getSupportedTypes();
	else if (getName().equals("test_isSupportedTypeLorg_eclipse_swt_dnd_TransferData")) test_isSupportedTypeLorg_eclipse_swt_dnd_TransferData();
	else if (getName().equals("test_getTypeIds")) test_getTypeIds();
	else if (getName().equals("test_getTypeNames")) test_getTypeNames();
	else if (getName().equals("test_javaToNativeLjava_lang_ObjectLorg_eclipse_swt_dnd_TransferData")) test_javaToNativeLjava_lang_ObjectLorg_eclipse_swt_dnd_TransferData();
	else if (getName().equals("test_nativeToJavaLorg_eclipse_swt_dnd_TransferData")) test_nativeToJavaLorg_eclipse_swt_dnd_TransferData();
	else if (getName().equals("test_registerTypeLjava_lang_String")) test_registerTypeLjava_lang_String();
}
}
