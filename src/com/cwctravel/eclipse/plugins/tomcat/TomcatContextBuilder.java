package com.cwctravel.eclipse.plugins.tomcat;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class TomcatContextBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "com.cwctravel.eclipse.plugins.tomcat.TomcatContextBuilder";

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();

		IEclipsePreferences preferences = new ProjectScope(project).getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);

		TomcatContextPreferenceManager preferenceManager = new TomcatContextPreferenceManager(project, preferences);
		List<ContextConfigInfo> contextConfigs = preferenceManager.read();

		if(!contextConfigs.isEmpty()) {

			List<String> classpathEntries = TomcatContextUtil.getClasspathEntries(project);

			String docBase = TomcatContextUtil.getDocBase(project);

			for(ContextConfigInfo contextConfigInfo: contextConfigs) {
				String contextConfigFileStr = contextConfigInfo.getResolvedContextConfigFile();

				IEclipsePreferences workspacePreferences = InstanceScope.INSTANCE.getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
				String contextConfigDisabledKey = TomcatContextUtil.getContextConfigDisabledKey(contextConfigInfo);
				boolean contextDisabled = workspacePreferences.getBoolean(contextConfigDisabledKey, false);
				if(!contextDisabled) {
					TomcatContextProcessor tomcatContextProcessor = new TomcatContextProcessor(project.getWorkspace(), contextConfigFileStr);

					tomcatContextProcessor.setDocBase(docBase);
					tomcatContextProcessor.setClasspathEntries(classpathEntries);
					tomcatContextProcessor.setResourceEntries(TomcatContextUtil.getResourceEntries(contextConfigInfo.getResources()));
					tomcatContextProcessor.setParameterEntries(TomcatContextUtil.getParameterEntries(contextConfigInfo.getParameters()));
					tomcatContextProcessor.setReloadableFlag(contextConfigInfo.isReloadableFlag());
					tomcatContextProcessor.setUseHttpOnlyFlag(contextConfigInfo.isUseHttpOnlyFlag());
					tomcatContextProcessor.setScanAllDirectoriesForJarsFlag(contextConfigInfo.isScanAllDirectoriesForJarsFlag());
					tomcatContextProcessor.store();
				}
			}
		}

		return null;
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {}
}