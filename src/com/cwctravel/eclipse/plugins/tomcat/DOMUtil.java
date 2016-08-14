package com.cwctravel.eclipse.plugins.tomcat;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DOMUtil {

	public static Node findFirstNode(Node node, String nodeName) {
		Node result = null;
		if (node != null && nodeName != null) {
			if (nodeName.equals(node.getNodeName())) {
				result = node;
			} else {
				NodeList childNodes = node.getChildNodes();
				for (int i = 0; result == null && i < childNodes.getLength(); i++) {
					result = findFirstNode(childNodes.item(i), nodeName);
				}
			}
		}

		return result;
	}

	public static List<Node> findAllNodes(Node node, String nodeName) {
		List<Node> result = new ArrayList<Node>();
		DOMUtil.findAllNodes(node, nodeName, result);
		return result;
	}

	public static void findAllNodes(Node node, String nodeName, List<Node> result) {
		if (node != null && nodeName != null) {
			if (nodeName.equals(node.getNodeName())) {
				result.add(node);
			} else {
				NodeList childNodes = node.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					findAllNodes(childNodes.item(i), nodeName, result);
				}
			}
		}
	}

	public static void removeAllChildren(Node node) {
		while (node.hasChildNodes()) {
			node.removeChild(node.getFirstChild());
		}
	}
}
