package org.eclipse.swt.examples.controlexample;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import java.text.*;
import java.util.*;

public class ControlExample {
	private static ResourceBundle resourceBundle =
		ResourceBundle.getBundle("examples_control");
	private ShellTab shellTab;
	private TabFolder tabFolder;
	private EventConsole eventConsole;
	static boolean standAlone = false;

	static final int
		ciClosedFolder = 0,
		ciOpenFolder = 1,
		ciTarget = 2;
	static final String[] imageLocations = {
		"closedFolder.gif",
		"openFolder.gif",
		"target.gif" };
	Image images[];

	/**
	 * Creates an instance of a ControlExample embedded inside
	 * the supplied parent Composite.
	 * 
	 * @param parent the container of the example
	 */
	public ControlExample(Composite parent) {
		initResources();
		tabFolder = new TabFolder (parent, SWT.NULL);
		Tab [] tabs = createTabs();
		for (int i=0; i<tabs.length; i++) {
			TabItem item = new TabItem (tabFolder, SWT.NULL);
		    item.setText (tabs [i].getTabText ());
		    item.setControl (tabs [i].createTabFolderPage (tabFolder));
		}
		if (standAlone) {
			createMenus (parent.getShell ());
		}
	}

	/**
	 * Open the event logger.
	 */
	private void closeEventConsole() {
		if (eventConsole != null) {
			eventConsole.close ();
			eventConsole = null;
		}
	}

	/**
	 * Create this example's menus.
	 */
	void createMenus(final Shell shell) {
		Menu bar = new Menu (shell, SWT.BAR);
		MenuItem consoleItem = new MenuItem (bar, SWT.CASCADE);
		consoleItem.setText ("Controls");
		shell.setMenuBar (bar);
		Menu dropDown = new Menu (bar);
		consoleItem.setMenu (dropDown);

		final MenuItem showEvents = new MenuItem (dropDown, SWT.CHECK);
		showEvents.setAccelerator(SWT.MOD1 + 'C');
		showEvents.setText ("&Show Events");
		showEvents.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				if (showEvents.getSelection()) {
					openEventConsole (shell);
				} else {
					closeEventConsole ();
				}
			}
		});
		
		final MenuItem exit = new MenuItem (dropDown, SWT.NONE);
		exit.setText ("E&xit");
		exit.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				shell.dispose();
			}
		});
	}

	/**
	 * Answers the set of example Tabs
	 */
	Tab[] createTabs() {
		return new Tab [] {
			new ButtonTab (this),
			new ComboTab (this),
			new CoolBarTab (this),
			new DialogTab (this),
			new LabelTab (this),
			new ListTab (this),
			new ProgressBarTab (this),
			new SashTab (this),
			shellTab = new ShellTab(this),
			new SliderTab (this),
			new TableTab (this),
			new TextTab (this),
			new ToolBarTab (this),
			new TreeTab (this),
		};
	}

	/**
	 * Disposes of all resources associated with a particular
	 * instance of the ControlExample.
	 */	
	public void dispose() {
		/*
		 * Destroy any shells that may have been created
		 * by the Shells tab.  When a shell is disposed,
		 * all child shells are also disposed.  Therefore
		 * it is necessary to check for disposed shells
		 * in the shells list to avoid disposing a shell
		 * twice.
		 */
		if (shellTab != null) shellTab.closeAllShells ();
		shellTab = null;
		tabFolder = null;
		freeResources();
	}

	/**
	 * Frees the resources
	 */
	void freeResources() {
		if (images != null) {
			for (int i = 0; i < images.length; ++i) {
				final Image image = images[i];
				if (image != null) image.dispose();
			}
			images = null;
		}
	}
	
	/**
	 * Gets a string from the resource bundle.
	 * We don't want to crash because of a missing String.
	 * Returns the key if not found.
	 */
	static String getResourceString(String key) {
		try {
			return resourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!";
		}			
	}

	/**
	 * Gets a string from the resource bundle and binds it
	 * with the given arguments. If the key is not found,
	 * return the key.
	 */
	static String getResourceString(String key, Object[] args) {
		try {
			return MessageFormat.format(getResourceString(key), args);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!";
		}
	}

	/**
	 * Loads the resources
	 */
	void initResources() {
		final Class clazz = ControlExample.class;
		if (resourceBundle != null) {
			try {
				if (images == null) {
					images = new Image[imageLocations.length];
					
					for (int i = 0; i < imageLocations.length; ++i) {
						ImageData source = new ImageData(clazz.getResourceAsStream(
							imageLocations[i]));
						ImageData mask = source.getTransparencyMask();
						images[i] = new Image(null, source, mask);
					}
				}
				return;
			} catch (Throwable t) {
			}
		}
		String error = (resourceBundle != null) ?
			getResourceString("error.CouldNotLoadResources") :
			"Unable to load resources";
		freeResources();
		throw new RuntimeException(error);
	}

	/**
	 * Logs an event to the event console.
	 */
	void log(Event event) {
		if (eventConsole != null) {
			eventConsole.log(event);
		}
	}
	
	/**
	 * Invokes as a standalone program.
	 */
	public static void main(String[] args) {
		standAlone = true;
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		ControlExample instance = new ControlExample(shell);
		shell.setText(getResourceString("window.title"));
		shell.open();
		while (! shell.isDisposed()) {
			if (! display.readAndDispatch()) display.sleep();
		}
		instance.dispose();
	}
	
	/**
	 * Open the event logger.
	 */
	void openEventConsole(Shell shell) {
		eventConsole = new EventConsole (shell);
		eventConsole.open ();
	}
	
	/**
	 * Grabs input focus.
	 */
	public void setFocus() {
		tabFolder.setFocus();
	}
}

