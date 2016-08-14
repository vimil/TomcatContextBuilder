package com.cwctravel.eclipse.plugins.tomcat.contextprocessors.v7;

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
import org.xml.sax.SAXException;

import com.cwctravel.eclipse.plugins.tomcat.DOMUtil;
import com.cwctravel.eclipse.plugins.tomcat.TomcatContextBuilderPlugin;
import com.cwctravel.eclipse.plugins.tomcat.TomcatContextUtil;
import com.cwctravel.eclipse.plugins.tomcat.contextprocessors.TomcatContextProcessor;

public class Tomcat7ContextProcessor implements TomcatContextProcessor {

	private String docBase;
	private final List<String> classpathEntries;

	private final IWorkspace workspace;

	private boolean isModified;
	private boolean reloadableFlag;
	private boolean useHttpOnlyFlag;
	private boolean scanAllDirectoriesForJarsFlag;
	private String contextXmlPath;
	private final List<String[]> resourceEntries;
	private final List<String[]> parameterEntries;

	private Document contextXmlDocument;

	public Tomcat7ContextProcessor(IWorkspace workspace) {
		this.workspace = workspace;
		this.classpathEntries = new ArrayList<String>();
		this.resourceEntries = new ArrayList<String[]>();
		this.parameterEntries = new ArrayList<String[]>();
	}

	@Override
	public void load(String contextXmlPath) {
		this.contextXmlPath = contextXmlPath;
		classpathEntries.clear();
		resourceEntries.clear();
		parameterEntries.clear();

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
						}

						Node resourcesNode = DOMUtil.findFirstNode(contextXmlDocument, "Resources");
						if (resourcesNode != null) {
							Attr extraResourcePathsAttr = (Attr) resourcesNode.getAttributes().getNamedItem("extraResourcePaths");
							if (extraResourcePathsAttr != null) {
								String resourceEntriesStr = extraResourcePathsAttr.getValue();
								if (resourceEntriesStr != null && !resourceEntriesStr.isEmpty()) {
									for (String resourceEntry : resourceEntriesStr.split(",")) {
										String[] resourceParts = resourceEntry.split("=");
										resourceEntries.add(resourceParts);
									}
								}
							}
						}

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

						Node loaderNode = DOMUtil.findFirstNode(contextXmlDocument, "Loader");
						if (loaderNode != null) {
							Attr virtualClasspathAttr = (Attr) loaderNode.getAttributes().getNamedItem("virtualClasspath");
							if (virtualClasspathAttr != null) {
								String classpathEntriesStr = virtualClasspathAttr.getValue();
								if (classpathEntriesStr != null) {
									for (String classpathEntry : classpathEntriesStr.split(";")) {
										classpathEntries.add(classpathEntry);
									}
								}
							}
						}

						Node jarScannerNode = DOMUtil.findFirstNode(contextXmlDocument, "JarScanner");
						if (jarScannerNode != null) {
							Attr scanAllDirectoriesForJarsFlagAttr = (Attr) jarScannerNode.getAttributes().getNamedItem("scanAllDirectories");
							if (scanAllDirectoriesForJarsFlagAttr != null) {
								scanAllDirectoriesForJarsFlag = Boolean.parseBoolean(scanAllDirectoriesForJarsFlagAttr.getValue());
							}
						}
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
					resourcesNode.setAttribute("className", "org.apache.naming.resources.VirtualDirContext");
					contextNode.appendChild(resourcesNode);

					Element loaderNode = contextXmlDocument.createElement("Loader");
					loaderNode.setAttribute("className", "org.apache.catalina.loader.VirtualWebappLoader");
					contextNode.appendChild(loaderNode);
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

	@Override
	public void setClasspathEntries(List<String> newClasspathEntries) {
		isModified = isModified || !classpathEntries.equals(newClasspathEntries);
		if (isModified) {
			classpathEntries.clear();
			if (newClasspathEntries != null) {
				classpathEntries.addAll(newClasspathEntries);
			}
		}
	}

	@Override
	public void setResourceEntries(List<String[]> newResourceEntries) {
		isModified = isModified || !TomcatContextUtil.areEntriesSame(resourceEntries, newResourceEntries);
		if (isModified) {
			resourceEntries.clear();
			if (newResourceEntries != null) {
				resourceEntries.addAll(newResourceEntries);
			}
		}
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
	public void store() {
		if (isModified && workspace != null && contextXmlDocument != null) {
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
					isModified = true;
					((Element) parameterNode).setAttribute("value", currentParamValue);
					parametersMap.remove(paramName);
				}

			} else {
				isModified = true;
				parameterNode.getParentNode().removeChild(parameterNode);
			}
		}

		if (!parametersMap.isEmpty()) {
			isModified = true;
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
		}
		return contextNode;
	}

	private void processLoaderNode() {
		Node loaderNode = DOMUtil.findFirstNode(contextXmlDocument, "Loader");
		if (loaderNode != null) {
			Attr virtualClasspathAttr = (Attr) loaderNode.getAttributes().getNamedItem("virtualClasspath");
			if (virtualClasspathAttr == null) {
				virtualClasspathAttr = contextXmlDocument.createAttribute("virtualClasspath");
				loaderNode.getAttributes().setNamedItem(virtualClasspathAttr);
			}

			StringBuilder classpathEntriesBuilder = new StringBuilder();
			for (String classpathEntry : classpathEntries) {
				classpathEntriesBuilder.append(classpathEntry);
				classpathEntriesBuilder.append(";");
			}
			virtualClasspathAttr.setNodeValue(classpathEntriesBuilder.toString());
		}
	}

	private void processResourcesNode() {
		Node resourcesNode = DOMUtil.findFirstNode(contextXmlDocument, "Resources");
		if (resourcesNode != null) {
			Attr extraResourcePathsAttr = (Attr) resourcesNode.getAttributes().getNamedItem("extraResourcePaths");
			if (extraResourcePathsAttr == null) {
				extraResourcePathsAttr = contextXmlDocument.createAttribute("extraResourcePaths");
				resourcesNode.getAttributes().setNamedItem(extraResourcePathsAttr);
			}

			StringBuilder resourceEntriesBuilder = new StringBuilder();
			int resourceEntriesCount = resourceEntries.size();
			for (int i = 0; i < resourceEntriesCount; i++) {
				String[] resourceEntry = resourceEntries.get(i);
				resourceEntriesBuilder.append(resourceEntry[0] + "=" + resourceEntry[1]);
				if (i < resourceEntriesCount - 1) {
					resourceEntriesBuilder.append(",");
				}
			}
			extraResourcePathsAttr.setNodeValue(resourceEntriesBuilder.toString());
		}
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
	}

	@Override
	public void setDocBase(String docBase) {
		isModified = isModified || (docBase != null && !docBase.equals(this.docBase));
		this.docBase = docBase;
	}

}
