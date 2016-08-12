package com.cwctravel.eclipse.plugins.tomcat;

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

public class TomcatContextProcessor {

	private String docBase;
	private final List<String> classpathEntries;

	private final IWorkspace workspace;

	private boolean isModified;
	private boolean reloadableFlag;
	private boolean useHttpOnlyFlag;
	private boolean scanAllDirectoriesForJarsFlag;
	private final String contextXmlPath;
	private final List<String[]> resourceEntries;
	private final List<String[]> parameterEntries;

	private Document contextXmlDocument;

	public TomcatContextProcessor(IWorkspace workspace, String contextXmlPath) {
		this.workspace = workspace;
		this.contextXmlPath = contextXmlPath;
		this.classpathEntries = new ArrayList<String>();
		this.resourceEntries = new ArrayList<String[]>();
		this.parameterEntries = new ArrayList<String[]>();

		if(contextXmlPath != null) {
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder;
				dBuilder = dbFactory.newDocumentBuilder();

				File contextXmlFile = new File(contextXmlPath);
				if(contextXmlFile.isFile()) {
					InputStream contents = new FileInputStream(contextXmlFile);
					try {
						contextXmlDocument = dBuilder.parse(contents);
						contextXmlDocument.getDocumentElement().normalize();

						Node contextNode = findFirstNode(contextXmlDocument, "Context");
						if(contextNode != null) {
							Attr docBaseAttr = (Attr)contextNode.getAttributes().getNamedItem("docBase");
							if(docBaseAttr != null) {
								docBase = docBaseAttr.getValue();
							}

							Attr reloadableFlagAttr = (Attr)contextNode.getAttributes().getNamedItem("reloadable");
							if(reloadableFlagAttr != null) {
								reloadableFlag = Boolean.parseBoolean(reloadableFlagAttr.getValue());
							}

							Attr useHttpOnlyFlagAttr = (Attr)contextNode.getAttributes().getNamedItem("useHttpOnly");
							if(useHttpOnlyFlagAttr != null) {
								useHttpOnlyFlag = Boolean.parseBoolean(useHttpOnlyFlagAttr.getValue());
							}
						}

						Node resourcesNode = findFirstNode(contextXmlDocument, "Resources");
						if(resourcesNode != null) {
							Attr extraResourcePathsAttr = (Attr)resourcesNode.getAttributes().getNamedItem("extraResourcePaths");
							if(extraResourcePathsAttr != null) {
								String resourceEntriesStr = extraResourcePathsAttr.getValue();
								if(resourceEntriesStr != null && !resourceEntriesStr.isEmpty()) {
									for(String resourceEntry: resourceEntriesStr.split(",")) {
										String[] resourceParts = resourceEntry.split("=");
										resourceEntries.add(resourceParts);
									}
								}
							}
						}

						List<Node> parameterNodes = findAllNodes(contextXmlDocument, "Parameter");
						if(!parameterNodes.isEmpty()) {
							for(Node parameterNode: parameterNodes) {
								Attr paramNameAttr = (Attr)parameterNode.getAttributes().getNamedItem("name");
								if(paramNameAttr != null) {
									String[] paramEntry = new String[2];
									paramEntry[0] = paramNameAttr.getValue();

									Attr paramValueAttr = (Attr)parameterNode.getAttributes().getNamedItem("value");
									if(paramValueAttr != null) {
										paramEntry[1] = paramValueAttr.getValue();
									}

									parameterEntries.add(paramEntry);
								}
							}
						}

						Node loaderNode = findFirstNode(contextXmlDocument, "Loader");
						if(loaderNode != null) {
							Attr virtualClasspathAttr = (Attr)loaderNode.getAttributes().getNamedItem("virtualClasspath");
							if(virtualClasspathAttr != null) {
								String classpathEntriesStr = virtualClasspathAttr.getValue();
								if(classpathEntriesStr != null) {
									for(String classpathEntry: classpathEntriesStr.split(";")) {
										classpathEntries.add(classpathEntry);
									}
								}
							}
						}

						Node jarScannerNode = findFirstNode(contextXmlDocument, "JarScanner");
						if(jarScannerNode != null) {
							Attr scanAllDirectoriesForJarsFlagAttr = (Attr)jarScannerNode.getAttributes().getNamedItem("scanAllDirectories");
							if(scanAllDirectoriesForJarsFlagAttr != null) {
								scanAllDirectoriesForJarsFlag = Boolean.parseBoolean(scanAllDirectoriesForJarsFlagAttr.getValue());
							}
						}
					}
					finally {
						contents.close();
					}
				}
				else {
					isModified = true;
					contextXmlDocument = dBuilder.newDocument();
					Element contextNode = contextXmlDocument.createElement("Context");
					contextNode.setAttribute("reloadable", "false");
					contextNode.setAttribute("useHttpOnly", "false");

					contextXmlDocument.appendChild(contextNode);

					Element resourcesNode = contextXmlDocument.createElement("Resources");
					resourcesNode.setAttribute("className", "org.apache.naming.resources.VirtualDirContext");
					contextNode.appendChild(resourcesNode);

					Element loaderNode = contextXmlDocument.createElement("Loader");
					loaderNode.setAttribute("className", "org.apache.catalina.loader.VirtualWebappLoader");
					contextNode.appendChild(loaderNode);
				}
			}
			catch(ParserConfigurationException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
			catch(SAXException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
			catch(IOException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
	}

	public void setClasspathEntries(List<String> newClasspathEntries) {
		isModified = isModified || !classpathEntries.equals(newClasspathEntries);
		if(isModified) {
			classpathEntries.clear();
			if(newClasspathEntries != null) {
				classpathEntries.addAll(newClasspathEntries);
			}
		}
	}

	public void setResourceEntries(List<String[]> newResourceEntries) {
		isModified = isModified || !TomcatContextUtil.areEntriesSame(resourceEntries, newResourceEntries);
		if(isModified) {
			resourceEntries.clear();
			if(newResourceEntries != null) {
				resourceEntries.addAll(newResourceEntries);
			}
		}
	}

	public void setParameterEntries(List<String[]> newParameterEntries) {
		isModified = isModified || !TomcatContextUtil.areEntriesSame(parameterEntries, newParameterEntries);
		if(isModified) {
			parameterEntries.clear();
			if(newParameterEntries != null) {
				parameterEntries.addAll(newParameterEntries);
			}
		}
	}

	public void store() {
		if(isModified && workspace != null && contextXmlDocument != null && !classpathEntries.isEmpty()) {
			processContextNode();
			processLoaderNode();
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
				}
				finally {
					outputWriter.close();
				}

			}
			catch(TransformerConfigurationException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
			catch(TransformerException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
			catch(IOException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
	}

	private void processParameterNodes() {
		List<Node> parameterNodes = findAllNodes(contextXmlDocument, "Parameter");
		Map<String, String> parametersMap = new HashMap<String, String>();
		for(String[] parameterEntry: parameterEntries) {
			parametersMap.put(parameterEntry[0], parameterEntry[1]);
		}

		for(Node parameterNode: parameterNodes) {
			String paramName = null;
			Attr parameterNameNodeAttr = (Attr)parameterNode.getAttributes().getNamedItem("name");
			if(parameterNameNodeAttr != null) {
				paramName = parameterNameNodeAttr.getValue();
			}

			if(parametersMap.containsKey(paramName)) {
				String paramValue = null;
				Attr parameterValueNodeAttr = (Attr)parameterNode.getAttributes().getNamedItem("value");
				if(parameterValueNodeAttr != null) {
					paramValue = parameterValueNodeAttr.getValue();
				}

				String currentParamValue = parametersMap.get(paramName);
				if(paramValue != currentParamValue && (paramValue == null || !paramValue.equals(currentParamValue))) {
					isModified = true;
					((Element)parameterNode).setAttribute("value", currentParamValue);
					parametersMap.remove(paramName);
				}

			}
			else {
				isModified = true;
				parameterNode.getParentNode().removeChild(parameterNode);
			}
		}

		if(!parametersMap.isEmpty()) {
			isModified = true;
			Node contextNode = findFirstNode(contextXmlDocument, "Context");
			for(Map.Entry<String, String> parametersMapEntry: parametersMap.entrySet()) {
				Element parameterNode = contextXmlDocument.createElement("Parameter");
				parameterNode.setAttribute("name", parametersMapEntry.getKey());
				parameterNode.setAttribute("value", parametersMapEntry.getValue());
				contextNode.appendChild(parameterNode);
			}
		}
	}

	private void processJarScannerNode() {
		Node contextNode = findFirstNode(contextXmlDocument, "Context");
		Element jarScannerNode = (Element)findFirstNode(contextXmlDocument, "JarScanner");
		if(jarScannerNode == null && scanAllDirectoriesForJarsFlag) {
			jarScannerNode = contextXmlDocument.createElement("JarScanner");
			contextNode.appendChild(jarScannerNode);
		}

		if(jarScannerNode != null) {
			jarScannerNode.setAttribute("scanAllDirectories", Boolean.toString(scanAllDirectoriesForJarsFlag));
		}
	}

	private Node processContextNode() {
		Node contextNode = findFirstNode(contextXmlDocument, "Context");
		if(contextNode != null) {
			Attr docBaseAttr = (Attr)contextNode.getAttributes().getNamedItem("docBase");
			if(docBaseAttr == null) {
				docBaseAttr = contextXmlDocument.createAttribute("docBase");
				contextNode.getAttributes().setNamedItem(docBaseAttr);
			}
			docBaseAttr.setNodeValue(docBase);

			Attr reloadableFlagAttr = (Attr)contextNode.getAttributes().getNamedItem("reloadable");
			if(reloadableFlagAttr == null) {
				reloadableFlagAttr = contextXmlDocument.createAttribute("reloadable");
				contextNode.getAttributes().setNamedItem(reloadableFlagAttr);
			}
			reloadableFlagAttr.setNodeValue(Boolean.toString(reloadableFlag));

			Attr useHttpOnlyFlagAttr = (Attr)contextNode.getAttributes().getNamedItem("useHttpOnly");
			if(useHttpOnlyFlagAttr == null) {
				useHttpOnlyFlagAttr = contextXmlDocument.createAttribute("useHttpOnly");
				contextNode.getAttributes().setNamedItem(useHttpOnlyFlagAttr);
			}
			useHttpOnlyFlagAttr.setNodeValue(Boolean.toString(useHttpOnlyFlag));
		}
		return contextNode;
	}

	private void processLoaderNode() {
		Node loaderNode = findFirstNode(contextXmlDocument, "Loader");
		if(loaderNode != null) {
			Attr virtualClasspathAttr = (Attr)loaderNode.getAttributes().getNamedItem("virtualClasspath");
			if(virtualClasspathAttr == null) {
				virtualClasspathAttr = contextXmlDocument.createAttribute("virtualClasspath");
				loaderNode.getAttributes().setNamedItem(virtualClasspathAttr);
			}

			StringBuilder classpathEntriesBuilder = new StringBuilder();
			for(String classpathEntry: classpathEntries) {
				classpathEntriesBuilder.append(classpathEntry);
				classpathEntriesBuilder.append(";");
			}
			virtualClasspathAttr.setNodeValue(classpathEntriesBuilder.toString());
		}
	}

	private void processResourcesNode() {
		Node resourcesNode = findFirstNode(contextXmlDocument, "Resources");
		if(resourcesNode != null) {
			Attr extraResourcePathsAttr = (Attr)resourcesNode.getAttributes().getNamedItem("extraResourcePaths");
			if(extraResourcePathsAttr == null) {
				extraResourcePathsAttr = contextXmlDocument.createAttribute("extraResourcePaths");
				resourcesNode.getAttributes().setNamedItem(extraResourcePathsAttr);
			}

			StringBuilder resourceEntriesBuilder = new StringBuilder();
			int resourceEntriesCount = resourceEntries.size();
			for(int i = 0; i < resourceEntriesCount; i++) {
				String[] resourceEntry = resourceEntries.get(i);
				resourceEntriesBuilder.append(resourceEntry[0] + "=" + resourceEntry[1]);
				if(i < resourceEntriesCount - 1) {
					resourceEntriesBuilder.append(",");
				}
			}
			extraResourcePathsAttr.setNodeValue(resourceEntriesBuilder.toString());
		}
	}

	private Node findFirstNode(Node node, String nodeName) {
		Node result = null;
		if(node != null && nodeName != null) {
			if(nodeName.equals(node.getNodeName())) {
				result = node;
			}
			else {
				NodeList childNodes = node.getChildNodes();
				for(int i = 0; result == null && i < childNodes.getLength(); i++) {
					result = findFirstNode(childNodes.item(i), nodeName);
				}
			}
		}

		return result;
	}

	private List<Node> findAllNodes(Node node, String nodeName) {
		List<Node> result = new ArrayList<Node>();
		findAllNodes(node, nodeName, result);
		return result;
	}

	private void findAllNodes(Node node, String nodeName, List<Node> result) {
		if(node != null && nodeName != null) {
			if(nodeName.equals(node.getNodeName())) {
				result.add(node);
			}
			else {
				NodeList childNodes = node.getChildNodes();
				for(int i = 0; i < childNodes.getLength(); i++) {
					findAllNodes(childNodes.item(i), nodeName, result);
				}
			}
		}
	}

	public void setReloadableFlag(boolean reloadableFlag) {
		isModified = isModified || (this.reloadableFlag != reloadableFlag);
		this.reloadableFlag = reloadableFlag;

	}

	public void setUseHttpOnlyFlag(boolean useHttpOnlyFlag) {
		isModified = isModified || (this.useHttpOnlyFlag != useHttpOnlyFlag);
		this.useHttpOnlyFlag = useHttpOnlyFlag;
	}

	public void setScanAllDirectoriesForJarsFlag(boolean scanAllDirectoriesForJarsFlag) {
		isModified = isModified || (this.scanAllDirectoriesForJarsFlag != scanAllDirectoriesForJarsFlag);
		this.scanAllDirectoriesForJarsFlag = scanAllDirectoriesForJarsFlag;
	}

	public void setDocBase(String docBase) {
		isModified = isModified || (docBase != null && !docBase.equals(this.docBase));
		this.docBase = docBase;
	}

}
