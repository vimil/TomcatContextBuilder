package com.cwctravel.eclipse.plugins.tomcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

class ResourcesContentProvider implements IStructuredContentProvider {
	private List<ResourceInfo> resources;
	private final Set<ResourceInfo> resourcesSet;

	public ResourcesContentProvider(List<ResourceInfo> resources) {
		this.resources = resources;
		this.resourcesSet = new HashSet<ResourceInfo>();

		if(resources != null) {
			resourcesSet.addAll(resources);
		}
	}

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(resources != null) {
			return resources.toArray();
		}
		return new Object[] {};
	}

	public List<ResourceInfo> getResources() {
		if(resources == null) {
			resources = new ArrayList<ResourceInfo>();
		}
		return resources;
	}

	public void addResource(ResourceInfo resourceInfo) {
		if(resourceInfo != null) {
			if(!resourcesSet.contains(resourceInfo)) {
				getResources().add(resourceInfo);
				resourcesSet.add(resourceInfo);
			}
		}
	}

	public void removeResources(List<Integer> resourceIndices) {
		if(resourceIndices != null) {
			List<ResourceInfo> resources = getResources();
			Collections.sort(resourceIndices);
			int resourceIndicesCount = resourceIndices.size();
			for(int i = resourceIndicesCount - 1; i >= 0; i--) {
				ResourceInfo removedResourceInfo = resources.remove((int)resourceIndices.get(i));
				resourcesSet.remove(removedResourceInfo);
			}
		}
	}

}