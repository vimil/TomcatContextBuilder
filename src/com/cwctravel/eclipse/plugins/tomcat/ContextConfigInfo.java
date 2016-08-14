package com.cwctravel.eclipse.plugins.tomcat;

import java.util.ArrayList;
import java.util.List;

public class ContextConfigInfo {
	private TomcatVersion tomcatVersion;

	private String contextName;
	private String contextConfigLocation;
	private String resolvedContextConfigLocation;
	private String containerSciFilter;

	private boolean reloadableFlag;
	private boolean useHttpOnlyFlag;
	private boolean scanAllDirectoriesForJarsFlag;

	private String projectName;

	private List<ResourceInfo> resources;

	private List<ParameterInfo> parameters;

	public TomcatVersion getTomcatVersion() {
		return tomcatVersion;
	}

	public void setTomcatVersion(TomcatVersion tomcatVersion) {
		this.tomcatVersion = tomcatVersion;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getContextName() {
		return contextName;
	}

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	public String getContextConfigLocation() {
		return contextConfigLocation;
	}

	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	public String getResolvedContextConfigLocation() {
		return resolvedContextConfigLocation;
	}

	public void setResolvedContextConfigLocation(String resolvedContextConfigLocation) {
		this.resolvedContextConfigLocation = resolvedContextConfigLocation;
	}

	public boolean isReloadableFlag() {
		return reloadableFlag;
	}

	public void setReloadableFlag(boolean reloadableFlag) {
		this.reloadableFlag = reloadableFlag;
	}

	public boolean isUseHttpOnlyFlag() {
		return useHttpOnlyFlag;
	}

	public void setUseHttpOnlyFlag(boolean useHttpOnlyFlag) {
		this.useHttpOnlyFlag = useHttpOnlyFlag;
	}

	public boolean isScanAllDirectoriesForJarsFlag() {
		return scanAllDirectoriesForJarsFlag;
	}

	public void setScanAllDirectoriesForJarsFlag(boolean scanAllDirectoriesForJarsFlag) {
		this.scanAllDirectoriesForJarsFlag = scanAllDirectoriesForJarsFlag;
	}

	public String getContainerSciFilter() {
		return containerSciFilter;
	}

	public void setContainerSciFilter(String containerSciFilter) {
		this.containerSciFilter = containerSciFilter;
	}

	public List<ResourceInfo> getResources() {
		if (resources == null) {
			resources = new ArrayList<ResourceInfo>();
		}
		return resources;
	}

	public void setResources(List<ResourceInfo> resources) {
		this.resources = resources;
	}

	public List<ParameterInfo> getParameters() {
		if (parameters == null) {
			parameters = new ArrayList<ParameterInfo>();
		}
		return parameters;
	}

	public void setParameters(List<ParameterInfo> parameters) {
		this.parameters = parameters;
	}

	public String getContextConfigFile() {
		return getContextConfigLocation() + "/" + getContextName() + ".xml";
	}

	public String getResolvedContextConfigFile() {
		return getResolvedContextConfigLocation() + "/" + getContextName() + ".xml";
	}

}
