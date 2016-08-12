package com.cwctravel.eclipse.plugins.tomcat;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class ProjectEventListener implements IResourceChangeListener {
	private static ProjectEventListener _instance;

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResource resource = event.getResource();
		int eventType = event.getType();
		if(eventType == IResourceChangeEvent.PRE_CLOSE || eventType == IResourceChangeEvent.PRE_DELETE) {
			if(resource instanceof IProject) {
				IProject project = (IProject)resource;
				IEclipsePreferences preferences = new ProjectScope(project).getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
				if(preferences != null) {
					TomcatContextPreferenceManager preferenceManager = new TomcatContextPreferenceManager(project, preferences);
					List<ContextConfigInfo> contextConfigs = preferenceManager.read();
					for(ContextConfigInfo contextConfig: contextConfigs) {
						String contextConfigFileStr = TomcatContextUtil.resolvePath(project, contextConfig.getContextConfigFile());
						if(contextConfigFileStr != null) {
							File contextConfigFile = new File(contextConfigFileStr);
							contextConfigFile.delete();
						}
					}
				}
			}
		}
	}

	public static synchronized ProjectEventListener getInstance() {
		if(_instance == null) {
			_instance = new ProjectEventListener();
		}
		return _instance;
	}

}
