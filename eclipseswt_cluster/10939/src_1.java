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

import junit.framework.*;
import junit.textui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;

/**
 * Automated Test Suite for class org.eclipse.swt.widgets.Menu
 *
 * @see org.eclipse.swt.widgets.Menu
 */
public class Test_org_eclipse_swt_widgets_Menu extends Test_org_eclipse_swt_widgets_Widget {

public Test_org_eclipse_swt_widgets_Menu(String name) {
	super(name);
}

public static void main(String[] args) {
	TestRunner.run(suite());
}

protected void setUp() {
	super.setUp();
	menu = new Menu(shell);
	setWidget(menu);
}

protected void tearDown() {
	super.tearDown();
}

public void test_ConstructorLorg_eclipse_swt_widgets_Control() {
	Composite comp = new Composite(shell, SWT.NULL);
	Menu testMenu = new Menu(comp);
	comp.dispose();
}

public void test_ConstructorLorg_eclipse_swt_widgets_DecorationsI() {
	Menu newMenu;
	MenuItem mItem = new MenuItem(menu, SWT.NULL);
	newMenu = new Menu(mItem);
}

public void test_ConstructorLorg_eclipse_swt_widgets_Menu() {
	Menu newMenu;
	newMenu = new Menu(menu);
}

public void test_ConstructorLorg_eclipse_swt_widgets_MenuItem() {
	warnUnimpl("Test test_ConstructorLorg_eclipse_swt_widgets_MenuItem not written");
}

public void test_addHelpListenerLorg_eclipse_swt_events_HelpListener() {
	warnUnimpl("Test test_addHelpListenerLorg_eclipse_swt_events_HelpListener not written");
}

public void test_addMenuListenerLorg_eclipse_swt_events_MenuListener() {
	warnUnimpl("Test test_addMenuListenerLorg_eclipse_swt_events_MenuListener not written");
}

public void test_getDefaultItem() {
	warnUnimpl("Test test_getDefaultItem not written");
}

public void test_getEnabled() {
	warnUnimpl("Test test_getEnabled not written");
}

public void test_getItemCount() {
	int number = 10;
	MenuItem ti;
	for (int i = 0; i<number ; i++){
		assertEquals(menu.getItemCount(), i);
	  	ti = new MenuItem(menu, 0);
	}
}

public void test_getItemI() {
	MenuItem mItem0 = new MenuItem(menu, SWT.NULL);
	MenuItem mItem1 = new MenuItem(menu, SWT.NULL);
	assertEquals(menu.getItem(0), mItem0);
	assertEquals(menu.getItem(1), mItem1);
}

public void test_getItems() {
	int number = 5;
	MenuItem[] items = new MenuItem[number];
	for (int i = 0; i<number ; i++){
	  	items[i] = new MenuItem(menu, 0);
	}
	assertEquals(":a:", items, menu.getItems());
	
	menu.getItems()[0].dispose();
	assertEquals(":b:", new MenuItem[]{items[1], items[2], items[3], items[4]}, menu.getItems());

	menu.getItems()[3].dispose();
	assertEquals(":c:", new MenuItem[]{items[1], items[2], items[3]}, menu.getItems());

	menu.getItems()[1].dispose();
	assertEquals(":d:", new MenuItem[]{items[1], items[3]}, menu.getItems());
}

public void test_getParent() {
	assertEquals(menu.getParent(), shell);
}

public void test_getParentItem() {
	MenuItem mItem = new MenuItem(menu, SWT.CASCADE);
	Menu newMenu = new Menu(shell, SWT.DROP_DOWN);
	assertNull(newMenu.getParentItem());
	mItem.setMenu(newMenu);
	assertEquals(newMenu.getParentItem(), mItem);
}

public void test_getParentMenu() {
	MenuItem mItem = new MenuItem(menu, SWT.CASCADE);
	Menu newMenu = new Menu(shell, SWT.DROP_DOWN);
	assertNull(newMenu.getParentMenu());
	mItem.setMenu(newMenu);
	assertEquals(newMenu.getParentMenu(), menu);
}

public void test_getShell() {
	assertEquals(menu.getShell(), shell);
}

public void test_getVisible() {
	warnUnimpl("Test test_getVisible not written");
}

public void test_indexOfLorg_eclipse_swt_widgets_MenuItem() {
	int number = 10;
	MenuItem[] mis = new MenuItem[number];
	for (int i = 0; i<number ; i++){
	  	mis[i] = new MenuItem(menu, SWT.NULL);
	}
	for (int i = 0; i<number ; i++){
		assertEquals(menu.indexOf(mis[i]), i);
		if (i>1)
			assertTrue(menu.indexOf(mis[i-1]) != i);
	}
}

public void test_isEnabled() {
	warnUnimpl("Test test_isEnabled not written");
}

public void test_isVisible() {
	// This test can not be run as it currently is written.  On Windows, if a 
	// menu has no menu items, it will not become visible.
	// If we add menu items to the menu then a second problem is encountered 
	// because menu.setVisible() enters into a modal loop and execution of 
	// the JUnit test case will not continue until the menu is selected and closed.
	if (true) return;
	menu.setVisible(true);
	assertTrue(menu.isVisible());

	// api not implemented yet
	if (fCheckVisibility) {
		menu.setVisible(false);
		assertEquals(menu.isVisible(), false);
	}
}

public void test_removeHelpListenerLorg_eclipse_swt_events_HelpListener() {
	warnUnimpl("Test test_removeHelpListenerLorg_eclipse_swt_events_HelpListener not written");
}

public void test_removeMenuListenerLorg_eclipse_swt_events_MenuListener() {
	warnUnimpl("Test test_removeMenuListenerLorg_eclipse_swt_events_MenuListener not written");
}

public void test_setDefaultItemLorg_eclipse_swt_widgets_MenuItem() {
	MenuItem mItem0 = new MenuItem(menu, SWT.NULL);
	MenuItem mItem1 = new MenuItem(menu, SWT.NULL);
	menu.setDefaultItem(mItem0);
	assertEquals(menu.getDefaultItem(), mItem0);
	assertTrue("After setDefaultItem(mItem0):", menu.getDefaultItem() != mItem1);
	menu.setDefaultItem(mItem1);
	assertEquals(menu.getDefaultItem(), mItem1);
	assertTrue("After setDefaultItem(mItem1):", menu.getDefaultItem() != mItem0);
}

public void test_setEnabledZ() {
	warnUnimpl("Test test_setEnabledZ not written");
}

public void test_setLocationII() {
	menu.setLocation(0,0);
}

public void test_setLocationLorg_eclipse_swt_graphics_Point() {
	menu.setLocation(new Point(0,0));
	try {
		menu.setLocation(null);
		fail("No exception thrown for null argument");
	}
	catch (IllegalArgumentException e) {
	}	
}

public void test_setVisibleZ() {
	// This test can not be run as it currently is written.  On Windows, if a 
	// menu has no menu items, it will not become visible.
	// If we add menu items to the menu then a second problem is encountered 
	// because menu.setVisible() enters into a modal loop and execution of 
	// the JUnit test case will not continue until the menu is selected and closed.
	if (true) return;
	menu.setVisible(true);
	assertTrue(menu.getVisible());
	// API not implemented yet 
	if (fCheckVisibility) {
		menu.setVisible(false);
		assertEquals(menu.getVisible(), false);
	}
}

public static Test suite() {
	TestSuite suite = new TestSuite();
	java.util.Vector methodNames = methodNames();
	java.util.Enumeration e = methodNames.elements();
	while (e.hasMoreElements()) {
		suite.addTest(new Test_org_eclipse_swt_widgets_Menu((String)e.nextElement()));
	}
	return suite;
}
public static java.util.Vector methodNames() {
	java.util.Vector methodNames = new java.util.Vector();
	methodNames.addElement("test_ConstructorLorg_eclipse_swt_widgets_Control");
	methodNames.addElement("test_ConstructorLorg_eclipse_swt_widgets_DecorationsI");
	methodNames.addElement("test_ConstructorLorg_eclipse_swt_widgets_Menu");
	methodNames.addElement("test_ConstructorLorg_eclipse_swt_widgets_MenuItem");
	methodNames.addElement("test_addHelpListenerLorg_eclipse_swt_events_HelpListener");
	methodNames.addElement("test_addMenuListenerLorg_eclipse_swt_events_MenuListener");
	methodNames.addElement("test_getDefaultItem");
	methodNames.addElement("test_getEnabled");
	methodNames.addElement("test_getItemCount");
	methodNames.addElement("test_getItemI");
	methodNames.addElement("test_getItems");
	methodNames.addElement("test_getParent");
	methodNames.addElement("test_getParentItem");
	methodNames.addElement("test_getParentMenu");
	methodNames.addElement("test_getShell");
	methodNames.addElement("test_getVisible");
	methodNames.addElement("test_indexOfLorg_eclipse_swt_widgets_MenuItem");
	methodNames.addElement("test_isEnabled");
	methodNames.addElement("test_isVisible");
	methodNames.addElement("test_removeHelpListenerLorg_eclipse_swt_events_HelpListener");
	methodNames.addElement("test_removeMenuListenerLorg_eclipse_swt_events_MenuListener");
	methodNames.addElement("test_setDefaultItemLorg_eclipse_swt_widgets_MenuItem");
	methodNames.addElement("test_setEnabledZ");
	methodNames.addElement("test_setLocationII");
	methodNames.addElement("test_setLocationLorg_eclipse_swt_graphics_Point");
	methodNames.addElement("test_setVisibleZ");
	methodNames.addAll(Test_org_eclipse_swt_widgets_Widget.methodNames()); // add superclass method names
	return methodNames;
}
protected void runTest() throws Throwable {
	if (getName().equals("test_ConstructorLorg_eclipse_swt_widgets_Control")) test_ConstructorLorg_eclipse_swt_widgets_Control();
	else if (getName().equals("test_ConstructorLorg_eclipse_swt_widgets_DecorationsI")) test_ConstructorLorg_eclipse_swt_widgets_DecorationsI();
	else if (getName().equals("test_ConstructorLorg_eclipse_swt_widgets_Menu")) test_ConstructorLorg_eclipse_swt_widgets_Menu();
	else if (getName().equals("test_ConstructorLorg_eclipse_swt_widgets_MenuItem")) test_ConstructorLorg_eclipse_swt_widgets_MenuItem();
	else if (getName().equals("test_addHelpListenerLorg_eclipse_swt_events_HelpListener")) test_addHelpListenerLorg_eclipse_swt_events_HelpListener();
	else if (getName().equals("test_addMenuListenerLorg_eclipse_swt_events_MenuListener")) test_addMenuListenerLorg_eclipse_swt_events_MenuListener();
	else if (getName().equals("test_getDefaultItem")) test_getDefaultItem();
	else if (getName().equals("test_getEnabled")) test_getEnabled();
	else if (getName().equals("test_getItemCount")) test_getItemCount();
	else if (getName().equals("test_getItemI")) test_getItemI();
	else if (getName().equals("test_getItems")) test_getItems();
	else if (getName().equals("test_getParent")) test_getParent();
	else if (getName().equals("test_getParentItem")) test_getParentItem();
	else if (getName().equals("test_getParentMenu")) test_getParentMenu();
	else if (getName().equals("test_getShell")) test_getShell();
	else if (getName().equals("test_getVisible")) test_getVisible();
	else if (getName().equals("test_indexOfLorg_eclipse_swt_widgets_MenuItem")) test_indexOfLorg_eclipse_swt_widgets_MenuItem();
	else if (getName().equals("test_isEnabled")) test_isEnabled();
	else if (getName().equals("test_isVisible")) test_isVisible();
	else if (getName().equals("test_removeHelpListenerLorg_eclipse_swt_events_HelpListener")) test_removeHelpListenerLorg_eclipse_swt_events_HelpListener();
	else if (getName().equals("test_removeMenuListenerLorg_eclipse_swt_events_MenuListener")) test_removeMenuListenerLorg_eclipse_swt_events_MenuListener();
	else if (getName().equals("test_setDefaultItemLorg_eclipse_swt_widgets_MenuItem")) test_setDefaultItemLorg_eclipse_swt_widgets_MenuItem();
	else if (getName().equals("test_setEnabledZ")) test_setEnabledZ();
	else if (getName().equals("test_setLocationII")) test_setLocationII();
	else if (getName().equals("test_setLocationLorg_eclipse_swt_graphics_Point")) test_setLocationLorg_eclipse_swt_graphics_Point();
	else if (getName().equals("test_setVisibleZ")) test_setVisibleZ();
	else super.runTest();
}

/* custom */
Menu menu;
}
