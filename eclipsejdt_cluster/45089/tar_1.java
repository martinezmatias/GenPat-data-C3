/*******************************************************************************
 * Copyright (c) 2005 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    mkaufman@bea.com - initial API and implementation
 *******************************************************************************/


package org.eclipse.jdt.apt.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.apt.core.FactoryContainer;

public class PluginFactoryContainer extends FactoryContainer
{
	/** The label of the plug that owns this factory container.  */
	private final String id;
	
	/** Whether the plugin's factories are enabled by default */
	private final boolean enableDefault;
	
	/**
	 * In general clients should not construct this object.  This c'tor should
	 * only be called from @see FactoryPathUtil#loadPluginFactories().
	 * @param pluginId
	 * @param enableDefault
	 */
	public PluginFactoryContainer(final String pluginId, boolean enableDefault) {
		this.id = pluginId;
		this.enableDefault = enableDefault;
	}
	
	public void addFactoryName( String n ) {
		getFactoryNames().add( n ); 
	}
	
	protected List<String> loadFactoryNames() { 
		return new ArrayList<String>();
	}
	
	public String getId() {
		return id;
	}
	
	public boolean getEnableDefault() {
		return enableDefault;
	}
	
	@Override
	public FactoryType getType() {
		return FactoryType.PLUGIN;
	}
}
