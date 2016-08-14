package com.cwctravel.eclipse.plugins.tomcat.contextprocessors;

import java.util.List;

public interface TomcatContextProcessor {

	public void load(String contextXmlPath);

	public void setDocBase(String docBase);

	public void setScanAllDirectoriesForJarsFlag(boolean scanAllDirectoriesForJarsFlag);

	public void setUseHttpOnlyFlag(boolean useHttpOnlyFlag);

	public void setReloadableFlag(boolean reloadableFlag);

	public void setContainerSciFilter(String containerSciFilter);

	public void store();

	public void setParameterEntries(List<String[]> newParameterEntries);

	public void setResourceEntries(List<String[]> newResourceEntries);

	public void setClasspathEntries(List<String> newClasspathEntries);

}
