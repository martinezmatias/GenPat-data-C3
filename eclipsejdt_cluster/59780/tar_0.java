/*******************************************************************************
 * Copyright (c) 2005 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    jgarms@bea.com, wharley@bea.com - initial API and implementation
 *    
 *******************************************************************************/
package org.eclipse.jdt.apt.core.util;

import java.io.IOException;
import java.util.*;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.apt.core.AptPlugin;
import org.eclipse.jdt.apt.core.internal.FactoryContainer;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Accesses configuration data for APT.
 * Note that some of the code in org.eclipse.jdt.ui reads and writes settings
 * data directly, rather than calling into the methods of this class. 
 * 
 * Helpful information about the Eclipse preferences mechanism can be found at:
 * http://dev.eclipse.org/viewcvs/index.cgi/~checkout~/platform-core-home/documents/user_settings/faq.html
 * 
 * TODO: synchronization of maps
 * TODO: NLS
 * TODO: rest of settings
 */
public class AptConfig {
	/**
	 * Update the factory list and other apt settings
	 */
	private static class EclipsePreferencesListener implements IEclipsePreferences.IPreferenceChangeListener {
		/**
		 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
		 */
		public void preferenceChange(IEclipsePreferences.PreferenceChangeEvent event) {
			// TODO: something, anything
		}
	}
	
	/**
	 * Used to set initial size of some maps.
	 */
	private static final int INITIAL_PROJECTS_GUESS = 5;
	
	/**
	 * Holds the containers for each project
	 */
	private static Map<IJavaProject, Map<FactoryContainer, Boolean>> _containerMaps = 
		new HashMap<IJavaProject, Map<FactoryContainer, Boolean>>(INITIAL_PROJECTS_GUESS);
	
	/**
	 * Holds the options maps for each project.
	 */
	private static Map<IJavaProject, Map<String, String>> _optionsMaps = 
		new HashMap<IJavaProject, Map<String, String>>(INITIAL_PROJECTS_GUESS);
	
	private static final Set<IJavaProject> _projectsWithFactoryPathLoaded = 
		new HashSet<IJavaProject>(INITIAL_PROJECTS_GUESS);
	
	private static Map<FactoryContainer, Boolean> _workspaceFactories = null;
	
	private static boolean _workspaceFactoryPathLoaded = false;
	
	private static final String FACTORYPATH_FILE = ".factorypath";
	
	/**
     * Add the equivalent of -Akey=val to the list of processor options.
     * @param key must be a nonempty string.  It should only include the key;
     * that is, it should not start with "-A".
     * @param jproj a project, or null to set the option workspace-wide.
     * @param val can be null (equivalent to -Akey).
     * @return the old value, or null if the option was not previously set.
     */
    public static String addProcessorOption(IJavaProject jproj, String key, String val) {
    	// TODO
    	return null;
    }
	
	/**
	 * Returns all containers for the provided project, including disabled ones
	 * @param jproj The java project in question, or null for the workspace
	 */
	public static synchronized Map<FactoryContainer, Boolean> getAllContainers(IJavaProject jproj) {
		if (jproj != null) {
			Map<FactoryContainer, Boolean> projectContainers = null;
			if (_projectsWithFactoryPathLoaded.contains(jproj)) {
				projectContainers = _containerMaps.get(jproj);
			}
			else {
				// Load project-level containers
				try {
					projectContainers = FactoryPathUtil.readFactoryPathFile(jproj);
				}
				catch (CoreException ce) {
					ce.printStackTrace();
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				_projectsWithFactoryPathLoaded.add(jproj);
				_containerMaps.put(jproj, projectContainers);
			}
			if (projectContainers != null) {
				return projectContainers;
			}
		}
		// Workspace
		if (!_workspaceFactoryPathLoaded) {
			// Load the workspace
			try {
				_workspaceFactories = FactoryPathUtil.readFactoryPathFile(null);
				if (_workspaceFactories == null) {
					// TODO: Need to get the default set of factories -- plugins only
				}
			}
			catch (CoreException ce) {
				ce.printStackTrace();
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return new LinkedHashMap(_workspaceFactories);
	}
	
	/**
	 * Get the factory containers for this project. If no project-level configuration
	 * is set, the workspace config will be returned. Any disabled containers
	 * will not be returned.
	 * 
	 * @param jproj The java project in question. 
	 * @param getDisabled if set, 
	 */
	public static synchronized List<FactoryContainer> getContainers(IJavaProject jproj) {
		Map<FactoryContainer, Boolean> containers = getAllContainers(jproj);
		List<FactoryContainer> result = new ArrayList<FactoryContainer>(containers.size());
		for (Map.Entry<FactoryContainer, Boolean> entry : containers.entrySet()) {
			if (entry.getValue()) {
				result.add(entry.getKey());
			}
		}
		return result;
	}
	
	/**
     * Get the options that are the equivalent of the -A command line options
     * for apt.  The -A and = are stripped out, so (key, value) is the
     * equivalent of -Akey=value.  
     * @param jproj a project, or null to query the workspace-wide setting.
     * @return a map of (key, value) pairs.  Value can be null (equivalent to
     * "-Akey").
     */
    public static Map<String, String> getProcessorOptions(IJavaProject jproj) {
    	// TODO
    	return null;
    }

	/**
	 * Initialize preferences lookups, and register change listeners.
	 * This is called once, from AptPlugin.start().
	 * TODO: the whole change-listener thing is still just copied and pasted from JDT without comprehension.
	 */
	public static void initialize() {
		/* TODO: figure out listeners - here's some stolen sample code for ideas:
		
		// Create lookups
		preferencesLookup[PREF_INSTANCE] = new InstanceScope().getNode(AptPlugin.PLUGIN_ID);
		// Calling this line will cause AptCorePreferenceInitializer to run,
		// via the runtime.preferences extension point.
		preferencesLookup[PREF_DEFAULT] = new DefaultScope().getNode(AptPlugin.PLUGIN_ID);

		// Listen to instance preferences node removal from parent in order to refresh stored one
		IEclipsePreferences.INodeChangeListener listener = new IEclipsePreferences.INodeChangeListener() {
			public void added(IEclipsePreferences.NodeChangeEvent event) {
				// do nothing
			}
			public void removed(IEclipsePreferences.NodeChangeEvent event) {
				if (event.getChild() == preferencesLookup[PREF_INSTANCE]) {
					preferencesLookup[PREF_INSTANCE] = new InstanceScope().getNode(AptPlugin.PLUGIN_ID);
					preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());
				}
			}
		};
		((IEclipsePreferences) preferencesLookup[PREF_INSTANCE].parent()).addNodeChangeListener(listener);
		preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());

		// Listen to default preferences node removal from parent in order to refresh stored one
		listener = new IEclipsePreferences.INodeChangeListener() {
			public void added(IEclipsePreferences.NodeChangeEvent event) {
				// do nothing
			}
			public void removed(IEclipsePreferences.NodeChangeEvent event) {
				if (event.getChild() == preferencesLookup[PREF_DEFAULT]) {
					preferencesLookup[PREF_DEFAULT] = new DefaultScope().getNode(AptPlugin.PLUGIN_ID);
				}
			}
		};
		((IEclipsePreferences) preferencesLookup[PREF_DEFAULT].parent()).addNodeChangeListener(listener);
*/
	}
	
	/**
	 * Is annotation processing turned on for this project?
	 * @param jproject an IJavaProject, or null to request workspace preferences.
	 * @return
	 */
	public static boolean isEnabled(IJavaProject jproject) {
		return getBoolean(jproject, AptPreferenceConstants.APT_ENABLED);
	}
	
	/**
	 * Turn annotation processing on or off for this project.
	 * @param jproject an IJavaProject, or null to set workspace preferences.
	 * @param enabled
	 */
	public static void setEnabled(IJavaProject jproject, boolean enabled) {
		setBoolean(jproject, AptPreferenceConstants.APT_ENABLED, enabled);
	}
	
	private static synchronized boolean getBoolean(IJavaProject jproject, String optionName) {
		Map options = getOptions(jproject);
		return "true".equals(options.get(optionName));
	}
	
    /**
	 * Return the apt settings for this project, or the workspace settings
	 * if they are not overridden by project settings.
	 * TODO: should jproject be allowed to be NULL?
	 * TODO: efficiently handle the case of projects that don't have per-project settings
	 * (e.g., only cache one workspace-wide map, not a separate copy for each project).
	 * @param jproject
	 * @return
	 */
	private static Map getOptions(IJavaProject jproject) {
		Map options = _optionsMaps.get(jproject);
		if (null != options) {
			return options;
		}
		// We didn't already have an options map for this project, so create one.
		IPreferencesService service = Platform.getPreferencesService();
		// Don't need to do this, because it's the default-default already:
		//service.setDefaultLookupOrder(AptPlugin.PLUGIN_ID, null, lookupOrder);
		options = new HashMap(AptPreferenceConstants.NSETTINGS);
		if (jproject != null) {
			IScopeContext projContext = new ProjectScope(jproject.getProject());
			IScopeContext[] contexts = new IScopeContext[] { projContext };
			for (String optionName : AptPreferenceConstants.OPTION_NAMES) {
				String val = service.getString(AptPlugin.PLUGIN_ID, optionName, null, contexts);
				if (val != null) {
					options.put(optionName, val);
				}
			}
		}
		else {
			// TODO: do we need to handle this case?
			return null;
		}
		
		return options;
	}

    private static synchronized String getString(IJavaProject jproject, String optionName) {
		Map options = getOptions(jproject);
		return (String)options.get(optionName);
	}
	
	private static synchronized void setBoolean(IJavaProject jproject, String optionName, boolean value) {
		// TODO: should we try to determine whether a project has no per-project settings,
		// and if so, set the workspace settings?  Or, do we want the caller to tell us
		// explicitly by setting jproject == null in that case?
		
		// TODO: when there are listeners, the following two lines will be superfluous:
		Map options = getOptions(jproject);
		options.put(AptPreferenceConstants.APT_ENABLED, value ? "true" : "false");
		
		IScopeContext context;
		if (null != jproject) {
			context = new ProjectScope(jproject.getProject());
		}
		else {
			context = new InstanceScope();
		}
		IEclipsePreferences node = context.getNode(AptPlugin.PLUGIN_ID);
		node.putBoolean(optionName, value);
	}
	
	private static synchronized void setString(IJavaProject jproject, String optionName, String value) {
		// TODO: should we try to determine whether a project has no per-project settings,
		// and if so, set the workspace settings?  Or, do we want the caller to tell us
		// explicitly by setting jproject == null in that case?
		
		// TODO: when there are listeners, the following two lines will be superfluous:
		Map options = getOptions(jproject);
		options.put(AptPreferenceConstants.APT_ENABLED, value);
		
		IScopeContext context;
		if (null != jproject) {
			context = new ProjectScope(jproject.getProject());
		}
		else {
			context = new InstanceScope();
		}
		IEclipsePreferences node = context.getNode(AptPlugin.PLUGIN_ID);
		node.put(optionName, value);
	}

    /**
	 * Set the factory containers for a given project or the workspace.
	 * @param jproj the java project, or null for the workspace
	 */
	public synchronized void setContainers(IJavaProject jproj, Map<FactoryContainer, Boolean> containers) 
	throws IOException, CoreException 
	{
		if (jproj == null) {
			// workspace
			_workspaceFactories = new HashMap(containers);
			_workspaceFactoryPathLoaded = true;
		}
		else {
			_containerMaps.put(jproj, new HashMap(containers));
			_projectsWithFactoryPathLoaded.add(jproj);
		}
		FactoryPathUtil.saveFactoryPathFile(jproj, containers);
		
	}
 

	
}
