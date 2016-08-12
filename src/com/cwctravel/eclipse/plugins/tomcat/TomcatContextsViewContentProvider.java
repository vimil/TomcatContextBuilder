package com.cwctravel.eclipse.plugins.tomcat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TomcatContextsViewContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	@Override
	public Object[] getElements(Object inputElement) {
		List<ContextConfigInfo> result = new ArrayList<ContextConfigInfo>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject project: projects) {
			if(project.isOpen() && TomcatContextUtil.hasTomcatNature(project)) {
				IEclipsePreferences preferences = new ProjectScope(project).getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
				TomcatContextPreferenceManager tomcatPreferencesManager = new TomcatContextPreferenceManager(project, preferences);
				List<ContextConfigInfo> contextConfigs = tomcatPreferencesManager.read();
				result.addAll(contextConfigs);
			}
		}

		return result.toArray();
	}
}
