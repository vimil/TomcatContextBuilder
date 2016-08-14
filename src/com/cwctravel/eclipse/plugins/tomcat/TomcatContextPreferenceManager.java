package com.cwctravel.eclipse.plugins.tomcat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

public class TomcatContextPreferenceManager {
	public static final String CONTEXT_TOMCAT_VERSION_PROPERTY = "TOMCAT_VERSION";
	public static final String CONTEXT_CONFIGS_PROPERTY = "TOMCAT_CONTEXT_CONFIGS";
	public static final String CONTEXT_CONFIG_LOCATION_PROPERTY = "TOMCAT_CONTEXT_CONFIG_LOCATION";
	public static final String RESOURCES_PROPERTY_PREFIX = "RESOURCES_";
	public static final String PARAMETERS_PROPERTY_PREFIX = "PARAMETERS_";

	public static final String CONTEXT_RELOADABLE_FLAG_PROPERTY = "TOMCAT_CONTEXT_RELOADABLE_FLAG";
	public static final String CONTEXT_USEHTTPONLY_FLAG_PROPERTY = "TOMCAT_CONTEXT_USEHTTPONLY_FLAG";
	public static final String CONTEXT_SCAN_ALL_DIRECTORIES_FOR_JARS_FLAG_PROPERTY = "TOMCAT_SCAN_ALL_DIRECTORIES_FOR_JARS_FLAG";
	public static final String CONTEXT_CONTAINER_SCI_FILTER_PROPERTY = "TOMCAT_CONTAINER_SCI_FILTER";

	private final IProject project;
	private final IEclipsePreferences preferences;

	public TomcatContextPreferenceManager(IProject project, IEclipsePreferences preferences) {
		this.project = project;
		this.preferences = preferences;
	}

	public List<ContextConfigInfo> read() {
		List<ContextConfigInfo> result = new ArrayList<ContextConfigInfo>();

		String contextNamesStr = preferences.get(CONTEXT_CONFIGS_PROPERTY, "");
		if (!contextNamesStr.isEmpty()) {
			for (String contextName : contextNamesStr.split(",")) {
				if (!contextName.isEmpty()) {
					ContextConfigInfo contextConfigInfo = new ContextConfigInfo();
					contextConfigInfo.setProjectName(project.getName());
					contextConfigInfo.setContextName(contextName);

					String tomcatVersionStr = getContextProperty(contextName, CONTEXT_TOMCAT_VERSION_PROPERTY,
							TomcatConstants.DEFAULT_TOMCAT_VERSION_STR);
					contextConfigInfo.setTomcatVersion(TomcatVersion.fromVersionStr(tomcatVersionStr));

					String contextConfigLocation = getContextProperty(contextName, CONTEXT_CONFIG_LOCATION_PROPERTY, "");
					contextConfigInfo.setContextConfigLocation(contextConfigLocation);
					contextConfigInfo.setResolvedContextConfigLocation(TomcatContextUtil.resolvePath(project, contextConfigLocation, false));

					boolean reloadableFlag = getContextProperty(contextName, CONTEXT_RELOADABLE_FLAG_PROPERTY, false);
					contextConfigInfo.setReloadableFlag(reloadableFlag);

					boolean useHttpOnlyFlag = getContextProperty(contextName, CONTEXT_USEHTTPONLY_FLAG_PROPERTY, false);
					contextConfigInfo.setUseHttpOnlyFlag(useHttpOnlyFlag);

					boolean scanAllDirectoriesForJarsFlag = getContextProperty(contextName, CONTEXT_SCAN_ALL_DIRECTORIES_FOR_JARS_FLAG_PROPERTY,
							false);
					contextConfigInfo.setScanAllDirectoriesForJarsFlag(scanAllDirectoriesForJarsFlag);

					String containerSciFilter = getContextProperty(contextName, CONTEXT_CONTAINER_SCI_FILTER_PROPERTY, "");
					contextConfigInfo.setContainerSciFilter(containerSciFilter);

					try {
						List<ResourceInfo> resources = getResources(contextName);
						contextConfigInfo.setResources(resources);
					} catch (BackingStoreException e) {
						TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
					}

					try {
						List<ParameterInfo> parameters = getParameters(contextName);
						contextConfigInfo.setParameters(parameters);
					} catch (BackingStoreException e) {
						TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
					}

					result.add(contextConfigInfo);
				}
			}
		}

		return result;
	}

	public void store(List<ContextConfigInfo> contextConfigs) throws BackingStoreException {
		boolean preferencesModified = false;

		Set<String> originalContextConfigs = new HashSet<String>();
		String originalContextConfigsStr = preferences.get(CONTEXT_CONFIGS_PROPERTY, null);
		if (originalContextConfigsStr != null) {
			originalContextConfigs.addAll(Arrays.asList(originalContextConfigsStr.split(",")));
		}

		Set<String> currentContextConfigs = new HashSet<String>();
		for (ContextConfigInfo contextConfigInfo : contextConfigs) {
			currentContextConfigs.add(contextConfigInfo.getContextName());
		}

		preferencesModified = !currentContextConfigs.containsAll(originalContextConfigs)
				|| !originalContextConfigs.containsAll(currentContextConfigs);
		if (preferencesModified) {
			preferences.put(CONTEXT_CONFIGS_PROPERTY, TomcatContextUtil.joinCollection(currentContextConfigs, ","));
			originalContextConfigs.removeAll(currentContextConfigs);
			for (String contextName : originalContextConfigs) {
				for (String key : preferences.keys()) {
					if (isContextProperty(key, contextName, "")) {
						preferences.remove(key);
					}
				}
			}
		}

		for (ContextConfigInfo contextConfigInfo : contextConfigs) {
			String contextName = contextConfigInfo.getContextName();
			String originalTomcatVersionStr = getContextProperty(contextName, CONTEXT_TOMCAT_VERSION_PROPERTY, null);

			TomcatVersion tomcatVersion = contextConfigInfo.getTomcatVersion();
			if (originalTomcatVersionStr == null || !originalTomcatVersionStr.equals(tomcatVersion.getVersionStr())) {
				setContextProperty(contextName, CONTEXT_TOMCAT_VERSION_PROPERTY, tomcatVersion.getVersionStr());
				preferencesModified = true;
			}

			String originalContextConfigLocationStr = getContextProperty(contextName, CONTEXT_CONFIG_LOCATION_PROPERTY, null);
			String contextConfigLocationStr = contextConfigInfo.getContextConfigLocation();
			if (contextConfigLocationStr != null && !contextConfigLocationStr.equals(originalContextConfigLocationStr)) {
				setContextProperty(contextName, CONTEXT_CONFIG_LOCATION_PROPERTY, contextConfigLocationStr);
				preferencesModified = true;
			}

			List<ResourceInfo> originalResources = getResources(contextName);
			List<ResourceInfo> resources = contextConfigInfo.getResources();
			preferencesModified = preferencesModified || !TomcatContextUtil.areResourcesSame(originalResources, resources);

			if (preferencesModified) {
				for (String key : preferences.keys()) {
					if (isContextProperty(key, contextName, RESOURCES_PROPERTY_PREFIX)) {
						preferences.remove(key);
					}
				}

				for (int i = 0; i < resources.size(); i++) {
					ResourceInfo resourceInfo = resources.get(i);
					String resourceKey = RESOURCES_PROPERTY_PREFIX + (i + 1);
					String resourceValue = resourceInfo.getPath() + ";" + resourceInfo.getLocation();
					setContextProperty(contextName, resourceKey, resourceValue);
				}
			}

			List<ParameterInfo> originalParameters = getParameters(contextName);
			List<ParameterInfo> parameters = contextConfigInfo.getParameters();
			preferencesModified = preferencesModified || !TomcatContextUtil.areParametersSame(originalParameters, parameters);

			if (preferencesModified) {
				for (String key : preferences.keys()) {
					if (isContextProperty(key, contextName, PARAMETERS_PROPERTY_PREFIX)) {
						preferences.remove(key);
					}
				}

				for (int i = 0; i < parameters.size(); i++) {
					ParameterInfo parameterInfo = parameters.get(i);
					String parameterKey = PARAMETERS_PROPERTY_PREFIX + parameterInfo.getParamName();
					String paramValueValue = parameterInfo.getParamValue();
					setContextProperty(contextName, parameterKey, paramValueValue);
				}
			}

			boolean originalReloadableFlag = getContextProperty(contextName, CONTEXT_RELOADABLE_FLAG_PROPERTY, false);
			boolean reloadableFlag = contextConfigInfo.isReloadableFlag();
			preferencesModified = preferencesModified || (originalReloadableFlag != reloadableFlag);
			if (preferencesModified) {
				setContextProperty(contextName, CONTEXT_RELOADABLE_FLAG_PROPERTY, reloadableFlag);
			}

			boolean originalUseHttpOnlyFlag = getContextProperty(contextName, CONTEXT_USEHTTPONLY_FLAG_PROPERTY, false);
			boolean useHttpOnlyFlag = contextConfigInfo.isUseHttpOnlyFlag();
			preferencesModified = preferencesModified || (originalUseHttpOnlyFlag != useHttpOnlyFlag);
			if (preferencesModified) {
				setContextProperty(contextName, CONTEXT_USEHTTPONLY_FLAG_PROPERTY, useHttpOnlyFlag);
			}

			boolean originalScanAllDirectoriesForJarsFlag = getContextProperty(contextName, CONTEXT_SCAN_ALL_DIRECTORIES_FOR_JARS_FLAG_PROPERTY,
					false);
			boolean scanAllDirectoriesForJarsFlag = contextConfigInfo.isScanAllDirectoriesForJarsFlag();

			preferencesModified = preferencesModified || (originalScanAllDirectoriesForJarsFlag != scanAllDirectoriesForJarsFlag);
			if (preferencesModified) {
				setContextProperty(contextName, CONTEXT_SCAN_ALL_DIRECTORIES_FOR_JARS_FLAG_PROPERTY, scanAllDirectoriesForJarsFlag);
			}

			String originalContainerSciFilter = getContextProperty(contextName, CONTEXT_CONTAINER_SCI_FILTER_PROPERTY, null);
			String containerSciFilter = contextConfigInfo.getContainerSciFilter();
			if (containerSciFilter != null && !containerSciFilter.equals(originalContainerSciFilter)) {
				setContextProperty(contextName, CONTEXT_CONTAINER_SCI_FILTER_PROPERTY, containerSciFilter);
				preferencesModified = true;
			}
		}

		if (preferencesModified) {
			preferences.flush();
			preferences.sync();
		}
	}

	private List<ResourceInfo> getResources(String contextName) throws BackingStoreException {
		List<ResourceInfo> resources = new ArrayList<ResourceInfo>();
		String[] keys = preferences.keys();
		for (String key : keys) {
			if (isContextProperty(key, contextName, RESOURCES_PROPERTY_PREFIX)) {
				String value = preferences.get(key, null);
				if (value != null) {
					String[] parts = value.split(";");
					String path = parts[0];
					String location = parts[1];
					ResourceInfo resourceInfo = new ResourceInfo(path, location, TomcatContextUtil.resolvePath(project, location));
					resources.add(resourceInfo);
				}
			}
		}
		return resources;
	}

	private List<ParameterInfo> getParameters(String contextName) throws BackingStoreException {
		List<ParameterInfo> parameters = new ArrayList<ParameterInfo>();
		String[] keys = preferences.keys();
		String contextParametersPropertyPrefix = getContextPropertyKey(contextName, PARAMETERS_PROPERTY_PREFIX);
		for (String key : keys) {
			if (isContextProperty(key, contextName, PARAMETERS_PROPERTY_PREFIX)) {
				String parameterName = key.substring(contextParametersPropertyPrefix.length());
				String parameterValue = preferences.get(key, null);
				ParameterInfo parameterInfo = new ParameterInfo(parameterName, parameterValue);
				parameters.add(parameterInfo);
			}
		}
		return parameters;
	}

	private boolean isContextProperty(String key, String contextName, String keyPrefix) {
		return keyPrefix != null && contextName != null && key != null && key.startsWith(contextName + "." + keyPrefix);
	}

	private String getContextPropertyKey(String contextName, String property) {
		return contextName + "." + property;
	}

	private String getContextProperty(String contextName, String property, String defaultValue) {
		if (contextName != null && property != null) {
			String contextProperty = getContextPropertyKey(contextName, property);
			return preferences.get(contextProperty, defaultValue);
		}
		return defaultValue;
	}

	private void setContextProperty(String contextName, String property, String value) {
		if (contextName != null && property != null) {
			String contextProperty = getContextPropertyKey(contextName, property);
			preferences.put(contextProperty, value);
		}
	}

	private boolean getContextProperty(String contextName, String property, boolean defaultValue) {
		if (contextName != null && property != null) {
			String contextProperty = getContextPropertyKey(contextName, property);
			return preferences.getBoolean(contextProperty, defaultValue);
		}
		return defaultValue;
	}

	private void setContextProperty(String contextName, String property, boolean value) {
		if (contextName != null && property != null) {
			String contextProperty = getContextPropertyKey(contextName, property);
			preferences.putBoolean(contextProperty, value);
		}
	}
}
