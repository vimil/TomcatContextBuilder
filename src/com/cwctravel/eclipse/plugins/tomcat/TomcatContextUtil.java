package com.cwctravel.eclipse.plugins.tomcat;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

public class TomcatContextUtil {
	public static boolean areResourcesSame(List<ResourceInfo> resources1, List<ResourceInfo> resources2) {
		List<String[]> resourceEntries1 = getResourceEntries(resources1);
		List<String[]> resourceEntries2 = getResourceEntries(resources2);
		return areEntriesSame(resourceEntries1, resourceEntries2);

	}

	public static boolean areParametersSame(List<ParameterInfo> parameters1, List<ParameterInfo> parameters2) {
		List<String[]> parameterEntries1 = getParameterEntries(parameters1);
		List<String[]> parameterEntries2 = getParameterEntries(parameters2);
		return areEntriesSame(parameterEntries1, parameterEntries2);

	}

	public static List<String[]> getResourceEntries(List<ResourceInfo> resources) {
		List<String[]> result = new ArrayList<String[]>();
		if(resources != null) {
			for(ResourceInfo resourceInfo: resources) {
				result.add(new String[] {resourceInfo.getPath(), resourceInfo.getLocation()});
			}
		}
		return result;
	}

	public static List<String[]> getParameterEntries(List<ParameterInfo> parameters) {
		List<String[]> result = new ArrayList<String[]>();
		if(parameters != null) {
			for(ParameterInfo parameterInfo: parameters) {
				result.add(new String[] {parameterInfo.getParamName(), parameterInfo.getParamValue()});
			}
		}
		return result;
	}

	public static boolean areEntriesSame(List<String[]> entries1, List<String[]> entries2) {
		boolean result = false;
		if(entries1 == entries2) {
			result = true;
		}
		else if(entries1 == null || entries2 == null) {
			result = true;
		}
		else {
			Map<String, String> entriesMap1 = createResourceMap(entries1);
			Map<String, String> entriesMap2 = createResourceMap(entries2);

			if(entriesMap1.size() != entriesMap2.size()) {
				result = false;
			}
			else {
				result = true;

				for(Map.Entry<String, String> entriesMapEntry1: entriesMap1.entrySet()) {
					String resourcePath = entriesMapEntry1.getKey();
					String resourceLocation = entriesMapEntry1.getValue();
					if(!resourceLocation.equals(entriesMap2.get(resourcePath))) {
						result = false;
						break;
					}
				}
			}
		}

		return result;
	}

	private static Map<String, String> createResourceMap(List<String[]> resourceEntries) {
		Map<String, String> result = new HashMap<String, String>();
		if(resourceEntries != null) {
			for(String[] resourceEntry: resourceEntries) {
				result.put(resourceEntry[0], resourceEntry[1]);
			}
		}

		return result;
	}

	public static <T> String joinCollection(Collection<T> collection, String sepStr) {
		StringBuilder sB = new StringBuilder();
		if(collection != null) {
			Iterator<T> iter = collection.iterator();
			while(iter.hasNext()) {
				T t = iter.next();
				if(t != null) {
					sB.append(t.toString());
				}
				if(iter.hasNext()) {
					sB.append(sepStr);
				}
			}
		}
		return sB.toString();
	}

	public static String getContextConfigDisabledKey(ContextConfigInfo contextConfigInfo) {
		String contextConfigKey = contextConfigInfo.getResolvedContextConfigFile();
		contextConfigKey = contextConfigKey.replaceAll(".*:", "");
		contextConfigKey = contextConfigKey.replace("/", ".");
		contextConfigKey = contextConfigKey.replace("\\", ".");
		if(contextConfigKey.startsWith(".")) {
			contextConfigKey = contextConfigKey.substring(1);
		}
		contextConfigKey = contextConfigKey.toLowerCase();
		contextConfigKey += ".disabled";
		return contextConfigKey;
	}

	public static String getDocBase(IProject project) {
		String result = null;
		IResource resource = project.findMember("WebContent");
		if(resource != null) {
			result = resource.getLocation().toOSString();
		}

		return result;
	}

	public static List<String> getClasspathEntries(IProject project) {
		List<String> result = new ArrayList<>();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint("com.cwctravel.eclipse.plugins.dependencies.classpathEntriesProvider");
		if(extensionPoint != null) {
			IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
			for(IConfigurationElement element: configurationElements) {
				try {
					Object classpathEntriesProvider = element.createExecutableExtension("class");
					Method m = classpathEntriesProvider.getClass().getMethod("getClasspathEntries", IProject.class, Set.class);
					@SuppressWarnings("unchecked") List<String> classpathEntries = (List<String>)m.invoke(classpathEntriesProvider, project, null);
					if(classpathEntries != null) {
						result.addAll(classpathEntries);
					}
				}
				catch(CoreException e) {
					TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				catch(NoSuchMethodException e) {
					TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				catch(SecurityException e) {
					TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				catch(IllegalAccessException e) {
					TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				catch(IllegalArgumentException e) {
					TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				catch(InvocationTargetException e) {
					TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}
		}

		return result;
	}

	public static String resolvePath(final IResource project, String path) {
		return resolvePath(project, path, true);
	}

	public static String resolvePath(final IResource project, String path, boolean checkIfExists) {
		String result = null;
		if(path != null) {
			if(new File(path).exists()) {
				result = path;
			}
			else {
				IPath resolvedPath = URIUtil.toPath(project.getPathVariableManager().resolveURI(URIUtil.toURI(path)));
				result = resolvedPath.toOSString();
				if(checkIfExists && !new File(result).exists()) {
					result = null;
				}
			}
		}
		return result;
	}

	public static boolean hasTomcatNature(IProject project) {
		try {
			if(project != null) {
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				for(int i = 0; i < natures.length; ++i) {
					if(TomcatConstants.TOMCAT_NATURE_ID.equals(natures[i])) {
						return true;
					}
				}
			}
			return false;
		}
		catch(CoreException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
