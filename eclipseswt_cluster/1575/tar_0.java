/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.tests.junit;


import org.eclipse.swt.custom.*;

import junit.framework.*;
import junit.textui.*;

/**
 * Automated Test Suite for class org.eclipse.swt.custom.TableTreeItem
 *
 * @see org.eclipse.swt.custom.TableTreeItem
 */
@SuppressWarnings("deprecation")
public class Test_org_eclipse_swt_custom_TableTreeItem extends Test_org_eclipse_swt_widgets_Item {

	TableTree tableTree;
	TableTreeItem tableTreeItem;
	
public Test_org_eclipse_swt_custom_TableTreeItem(String name) {
	super(name);
}

public static void main(String[] args) {
	TestRunner.run(suite());
}

@Override
protected void setUp() {
	super.setUp();
	tableTree = new TableTree(shell, 0);
	tableTreeItem = new TableTreeItem(tableTree, 0);
	setWidget(tableTreeItem);
}

@Override
public void test_setImageLorg_eclipse_swt_graphics_Image() {
}

@Override
public void test_setTextLjava_lang_String() {
}

public static Test suite() {
	TestSuite suite = new TestSuite();
	java.util.Vector<String> methodNames = methodNames();
	java.util.Enumeration<String> e = methodNames.elements();
	while (e.hasMoreElements()) {
		suite.addTest(new Test_org_eclipse_swt_custom_TableTreeItem(e.nextElement()));
	}
	return suite;
}
public static java.util.Vector<String> methodNames() {
	java.util.Vector<String> methodNames = new java.util.Vector<String>();
	methodNames.addElement("test_setImageLorg_eclipse_swt_graphics_Image");
	methodNames.addElement("test_setTextLjava_lang_String");
	methodNames.addAll(Test_org_eclipse_swt_widgets_Item.methodNames()); // add superclass method names
	return methodNames;
}
@Override
protected void runTest() throws Throwable {
	if (getName().equals("test_setImageLorg_eclipse_swt_graphics_Image")) test_setImageLorg_eclipse_swt_graphics_Image();
	else if (getName().equals("test_setTextLjava_lang_String")) test_setTextLjava_lang_String();
	else super.runTest();
}
}
