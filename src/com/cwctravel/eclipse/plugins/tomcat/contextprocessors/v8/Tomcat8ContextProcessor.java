package com.cwctravel.eclipse.plugins.tomcat.contextprocessors.v8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cwctravel.eclipse.plugins.tomcat.DOMUtil;
import com.cwctravel.eclipse.plugins.tomcat.TomcatContextBuilderPlugin;
import com.cwctravel.eclipse.plugins.tomcat.TomcatContextUtil;
import com.cwctravel.eclipse.plugins.tomcat.contextprocessors.TomcatContextProcessor;

public class Tomcat8ContextProcessor implements TomcatContextProcessor {
	private String docBase;

	private final IWorkspace workspace;

	private boolean isModified;
	private boolean reloadableFlag;
	private boolean useHttpOnlyFlag;
	private boolean scanAllDirectoriesForJarsFlag;

	private String contextXmlPath;
	private String containerSciFilter;

	private final List<String[]> parameterEntries;

	private final List<ResourceEntryInfo> resourceEntries;

	private final List<String> newClasspathEntries;
	private final List<String[]> newResourceEntries;

	private Document contextXmlDocument;

	public Tomcat8ContextProcessor(IWorkspace workspace) {
		this.workspace = workspace;
		this.newClasspathEntries = new ArrayList<String>();
		this.newResourceEntries = new ArrayList<String[]>();
		this.resourceEntries = new ArrayList<ResourceEntryInfo>();
		this.parameterEntries = new ArrayList<String[]>();
	}

	@Override
	public void load(String contextXmlPath) {
		this.contextXmlPath = contextXmlPath;
		resourceEntries.clear();
		parameterEntries.clear();
		newClasspathEntries.clear();
		newResourceEntries.clear();

		if (contextXmlPath != null) {
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder;
				dBuilder = dbFactory.newDocumentBuilder();

				File contextXmlFile = new File(contextXmlPath);
				if (contextXmlFile.isFile()) {
					InputStream contents = new FileInputStream(contextXmlFile);
					try {
						contextXmlDocument = dBuilder.parse(contents);
						contextXmlDocument.getDocumentElement().normalize();

						readContextNode();
						readResourcesNode();
						readParameterNodes();
						readJarScannerNode();
					} finally {
						contents.close();
					}
				} else {
					isModified = true;
					contextXmlDocument = dBuilder.newDocument();
					Element contextNode = contextXmlDocument.createElement("Context");
					contextNode.setAttribute("reloadable", "false");
					contextNode.setAttribute("useHttpOnly", "false");

					contextXmlDocument.appendChild(contextNode);

					Element resourcesNode = contextXmlDocument.createElement("Resources");
					resourcesNode.setAttribute("className", "org.apache.catalina.webresources.StandardRoot");
					contextNode.appendChild(resourcesNode);
				}
			} catch (ParserConfigurationException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			} catch (SAXException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			} catch (IOException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}

	}

	private void readResourcesNode() {
		Node resourcesNode = DOMUtil.findFirstNode(contextXmlDocument, "Resources");
		if (resourcesNode != null) {
			NodeList childNodes = resourcesNode.getChildNodes();
			int nodeListSize = childNodes.getLength();
			for (int i = 0; i < nodeListSize; i++) {
				Node resourceEntryNode = childNodes.item(i);
				String nodeName = resourceEntryNode.getNodeName();
				ResourceEntryInfo resourceEntryInfo = null;
				if ("PreResources".equals(nodeName)) {
					resourceEntryInfo = new ResourceEntryInfo();
					resourceEntryInfo.setType(ResourceEntryInfo.TYPE_PRE);
				} else if ("JarResources".equals(nodeName)) {
					resourceEntryInfo = new ResourceEntryInfo();
					resourceEntryInfo.setType(ResourceEntryInfo.TYPE_JAR);
				} else if ("PostResources".equals(nodeName)) {
					resourceEntryInfo = new ResourceEntryInfo();
					resourceEntryInfo.setType(ResourceEntryInfo.TYPE_POST);
				}
				if (resourceEntryInfo != null) {
					Attr baseAttr = (Attr) resourceEntryNode.getAttributes().getNamedItem("base");
					if (baseAttr != null) {
						resourceEntryInfo.setBase(baseAttr.getValue());
					}

					Attr classNameAttr = (Attr) resourceEntryNode.getAttributes().getNamedItem("className");
					if (classNameAttr != null) {
						resourceEntryInfo.setClassName(classNameAttr.getValue());
					}

					Attr webAppMountAttr = (Attr) resourceEntryNode.getAttributes().getNamedItem("webAppMount");
					if (webAppMountAttr != null) {
						resourceEntryInfo.setWebAppMount(webAppMountAttr.getValue());
					}

					resourceEntries.add(resourceEntryInfo);
				}
			}
		}
	}

	private void readContextNode() {
		Node contextNode = DOMUtil.findFirstNode(contextXmlDocument, "Context");
		if (contextNode != null) {
			Attr docBaseAttr = (Attr) contextNode.getAttributes().getNamedItem("docBase");
			if (docBaseAttr != null) {
				docBase = docBaseAttr.getValue();
			}

			Attr reloadableFlagAttr = (Attr) contextNode.getAttributes().getNamedItem("reloadable");
			if (reloadableFlagAttr != null) {
				reloadableFlag = Boolean.parseBoolean(reloadableFlagAttr.getValue());
			}

			Attr useHttpOnlyFlagAttr = (Attr) contextNode.getAttributes().getNamedItem("useHttpOnly");
			if (useHttpOnlyFlagAttr != null) {
				useHttpOnlyFlag = Boolean.parseBoolean(useHttpOnlyFlagAttr.getValue());
			}

			Attr containerSciFilterAttr = (Attr) contextNode.getAttributes().getNamedItem("containerSciFilter");
			if (containerSciFilterAttr != null) {
				containerSciFilter = containerSciFilterAttr.getValue();
			}
		}
	}

	private void readJarScannerNode() {
		Node jarScannerNode = DOMUtil.findFirstNode(contextXmlDocument, "JarScanner");
		if (jarScannerNode != null) {
			Attr scanAllDirectoriesForJarsFlagAttr = (Attr) jarScannerNode.getAttributes().getNamedItem("scanAllDirectories");
			if (scanAllDirectoriesForJarsFlagAttr != null) {
				scanAllDirectoriesForJarsFlag = Boolean.parseBoolean(scanAllDirectoriesForJarsFlagAttr.getValue());
			}
		}
	}

	private void readParameterNodes() {
		List<Node> parameterNodes = DOMUtil.findAllNodes(contextXmlDocument, "Parameter");
		if (!parameterNodes.isEmpty()) {
			for (Node parameterNode : parameterNodes) {
				Attr paramNameAttr = (Attr) parameterNode.getAttributes().getNamedItem("name");
				if (paramNameAttr != null) {
					String[] paramEntry = new String[2];
					paramEntry[0] = paramNameAttr.getValue();

					Attr paramValueAttr = (Attr) parameterNode.getAttributes().getNamedItem("value");
					if (paramValueAttr != null) {
						paramEntry[1] = paramValueAttr.getValue();
					}

					parameterEntries.add(paramEntry);
				}
			}
		}
	}

	@Override
	public void store() {
		if (!isModified) {
			isModified = updateResourceEntries();
		}

		if (isModified && workspace != null && contextXmlDocument != null) {
			processContextNode();
			processResourcesNode();
			processParameterNodes();
			processJarScannerNode();

			TransformerFactory tFactory = TransformerFactory.newInstance();
			tFactory.setAttribute("indent-number", 2);
			Transformer transformer;
			try {
				transformer = tFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				DOMSource source = new DOMSource(contextXmlDocument);
				File contextXmlFile = new File(contextXmlPath);
				contextXmlFile.getParentFile().mkdirs();
				FileWriter outputWriter = new FileWriter(contextXmlFile);

				try {
					StreamResult result = new StreamResult(outputWriter);
					transformer.transform(source, result);
				} finally {
					outputWriter.close();
				}

			} catch (TransformerConfigurationException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			} catch (TransformerException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			} catch (IOException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
	}

	private void processParameterNodes() {
		List<Node> parameterNodes = DOMUtil.findAllNodes(contextXmlDocument, "Parameter");
		Map<String, String> parametersMap = new HashMap<String, String>();
		for (String[] parameterEntry : parameterEntries) {
			parametersMap.put(parameterEntry[0], parameterEntry[1]);
		}

		for (Node parameterNode : parameterNodes) {
			String paramName = null;
			Attr parameterNameNodeAttr = (Attr) parameterNode.getAttributes().getNamedItem("name");
			if (parameterNameNodeAttr != null) {
				paramName = parameterNameNodeAttr.getValue();
			}

			if (parametersMap.containsKey(paramName)) {
				String paramValue = null;
				Attr parameterValueNodeAttr = (Attr) parameterNode.getAttributes().getNamedItem("value");
				if (parameterValueNodeAttr != null) {
					paramValue = parameterValueNodeAttr.getValue();
				}

				String currentParamValue = parametersMap.get(paramName);
				if (paramValue != currentParamValue && (paramValue == null || !paramValue.equals(currentParamValue))) {
					((Element) parameterNode).setAttribute("value", currentParamValue);
					parametersMap.remove(paramName);
				}

			} else {
				parameterNode.getParentNode().removeChild(parameterNode);
			}
		}

		if (!parametersMap.isEmpty()) {
			Node contextNode = DOMUtil.findFirstNode(contextXmlDocument, "Context");
			for (Map.Entry<String, String> parametersMapEntry : parametersMap.entrySet()) {
				Element parameterNode = contextXmlDocument.createElement("Parameter");
				parameterNode.setAttribute("name", parametersMapEntry.getKey());
				parameterNode.setAttribute("value", parametersMapEntry.getValue());
				contextNode.appendChild(parameterNode);
			}
		}
	}

	private void processJarScannerNode() {
		Node contextNode = DOMUtil.findFirstNode(contextXmlDocument, "Context");
		Element jarScannerNode = (Element) DOMUtil.findFirstNode(contextXmlDocument, "JarScanner");
		if (jarScannerNode == null && scanAllDirectoriesForJarsFlag) {
			jarScannerNode = contextXmlDocument.createElement("JarScanner");
			contextNode.appendChild(jarScannerNode);
		}

		if (jarScannerNode != null) {
			jarScannerNode.setAttribute("scanAllDirectories", Boolean.toString(scanAllDirectoriesForJarsFlag));
		}
	}

	private Node processContextNode() {
		Node contextNode = DOMUtil.findFirstNode(contextXmlDocument, "Context");
		if (contextNode != null) {
			Attr docBaseAttr = (Attr) contextNode.getAttributes().getNamedItem("docBase");
			if (docBaseAttr == null) {
				docBaseAttr = contextXmlDocument.createAttribute("docBase");
				contextNode.getAttributes().setNamedItem(docBaseAttr);
			}
			docBaseAttr.setNodeValue(docBase);

			Attr reloadableFlagAttr = (Attr) contextNode.getAttributes().getNamedItem("reloadable");
			if (reloadableFlagAttr == null) {
				reloadableFlagAttr = contextXmlDocument.createAttribute("reloadable");
				contextNode.getAttributes().setNamedItem(reloadableFlagAttr);
			}
			reloadableFlagAttr.setNodeValue(Boolean.toString(reloadableFlag));

			Attr useHttpOnlyFlagAttr = (Attr) contextNode.getAttributes().getNamedItem("useHttpOnly");
			if (useHttpOnlyFlagAttr == null) {
				useHttpOnlyFlagAttr = contextXmlDocument.createAttribute("useHttpOnly");
				contextNode.getAttributes().setNamedItem(useHttpOnlyFlagAttr);
			}
			useHttpOnlyFlagAttr.setNodeValue(Boolean.toString(useHttpOnlyFlag));

			if (containerSciFilter != null && !containerSciFilter.isEmpty()) {
				Attr containerSciFilterAttr = (Attr) contextNode.getAttributes().getNamedItem("containerSciFilter");
				if (containerSciFilterAttr == null) {
					containerSciFilterAttr = contextXmlDocument.createAttribute("containerSciFilter");
					contextNode.getAttributes().setNamedItem(containerSciFilterAttr);
				}
				containerSciFilterAttr.setNodeValue(containerSciFilter);
			}
		}
		return contextNode;
	}

	private void processResourcesNode() {
		Node resourcesNode = DOMUtil.findFirstNode(contextXmlDocument, "Resources");
		if (resourcesNode != null) {
			DOMUtil.removeAllChildren(resourcesNode);

			for (ResourceEntryInfo resourceEntryInfo : resourceEntries) {
				Element resourceEntryNode = null;
				int resourceEntryType = resourceEntryInfo.getType();

				if (resourceEntryType == ResourceEntryInfo.TYPE_PRE) {
					resourceEntryNode = contextXmlDocument.createElement("PreResources");
				} else if (resourceEntryType == ResourceEntryInfo.TYPE_JAR) {
					resourceEntryNode = contextXmlDocument.createElement("JarResources");
				} else if (resourceEntryType == ResourceEntryInfo.TYPE_POST) {
					resourceEntryNode = contextXmlDocument.createElement("PostResources");
				}

				if (resourceEntryNode != null) {
					resourceEntryNode.setAttribute("base", resourceEntryInfo.getBase());
					resourceEntryNode.setAttribute("className", resourceEntryInfo.getClassName());
					resourceEntryNode.setAttribute("webAppMount", resourceEntryInfo.getWebAppMount());
					resourcesNode.appendChild(resourceEntryNode);
				}
			}
		}
	}

	private boolean updateResourceEntries() {
		boolean modified = true;
		List<ResourceEntryInfo> updatedResourceEntries = new ArrayList<ResourceEntryInfo>();

		for (String[] resourceEntry : newResourceEntries) {
			ResourceEntryInfo resourceEntryInfo = new ResourceEntryInfo();
			resourceEntryInfo.setType(ResourceEntryInfo.TYPE_POST);
			resourceEntryInfo.setWebAppMount(resourceEntry[0]);
			resourceEntryInfo.setBase(resourceEntry[1]);
			resourceEntryInfo.setClassName("org.apache.catalina.webresources.DirResourceSet");
			updatedResourceEntries.add(resourceEntryInfo);
		}

		for (String classpathEntry : newClasspathEntries) {
			ResourceEntryInfo resourceEntryInfo = new ResourceEntryInfo();
			resourceEntryInfo.setType(ResourceEntryInfo.TYPE_POST);
			resourceEntryInfo.setWebAppMount("/WEB-INF/classes");
			resourceEntryInfo.setBase(classpathEntry);
			File file = new File(classpathEntry);
			if (file.isDirectory()) {
				resourceEntryInfo.setClassName("org.apache.catalina.webresources.DirResourceSet");
			} else {
				resourceEntryInfo.setClassName("org.apache.catalina.webresources.JarResourceSet");
			}
			updatedResourceEntries.add(resourceEntryInfo);
		}

		if (resourceEntries.size() == updatedResourceEntries.size()) {
			modified = false;
			for (int i = 0; i < resourceEntries.size(); i++) {
				ResourceEntryInfo resourceEntryInfo1 = resourceEntries.get(i);
				ResourceEntryInfo resourceEntryInfo2 = updatedResourceEntries.get(i);
				if (!resourceEntryInfo1.equals(resourceEntryInfo2)) {
					modified = true;
					break;
				}
			}
		}

		if (modified) {
			resourceEntries.clear();
			resourceEntries.addAll(updatedResourceEntries);
		}

		return modified;
	}

	@Override
	public void setReloadableFlag(boolean reloadableFlag) {
		isModified = isModified || (this.reloadableFlag != reloadableFlag);
		this.reloadableFlag = reloadableFlag;

	}

	@Override
	public void setUseHttpOnlyFlag(boolean useHttpOnlyFlag) {
		isModified = isModified || (this.useHttpOnlyFlag != useHttpOnlyFlag);
		this.useHttpOnlyFlag = useHttpOnlyFlag;
	}

	@Override
	public void setScanAllDirectoriesForJarsFlag(boolean scanAllDirectoriesForJarsFlag) {
		isModified = isModified || (this.scanAllDirectoriesForJarsFlag != scanAllDirectoriesForJarsFlag);
		this.scanAllDirectoriesForJarsFlag = scanAllDirectoriesForJarsFlag;
	}

	@Override
	public void setContainerSciFilter(String containerSciFilter) {
		isModified = isModified || (containerSciFilter != null && !containerSciFilter.equals(this.containerSciFilter));
		this.containerSciFilter = containerSciFilter;

	}

	@Override
	public void setDocBase(String docBase) {
		isModified = isModified || (docBase != null && !docBase.equals(this.docBase));
		this.docBase = docBase;
	}

	@Override
	public void setParameterEntries(List<String[]> newParameterEntries) {
		isModified = isModified || !TomcatContextUtil.areEntriesSame(parameterEntries, newParameterEntries);
		if (isModified) {
			parameterEntries.clear();
			if (newParameterEntries != null) {
				parameterEntries.addAll(newParameterEntries);
			}
		}
	}

	@Override
	public void setResourceEntries(List<String[]> newResourceEntries) {
		this.newResourceEntries.clear();
		if (newResourceEntries != null) {
			this.newResourceEntries.addAll(newResourceEntries);
		}
	}

	@Override
	public void setClasspathEntries(List<String> newClasspathEntries) {
		this.newClasspathEntries.clear();
		if (newClasspathEntries != null) {
			this.newClasspathEntries.addAll(newClasspathEntries);
		}
	}

}
