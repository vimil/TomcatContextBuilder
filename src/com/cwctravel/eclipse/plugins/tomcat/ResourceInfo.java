package com.cwctravel.eclipse.plugins.tomcat;

public class ResourceInfo {
	private String path;
	private String location;
	private String resolvedLocation;

	public ResourceInfo(String path, String location, String resolvedLocation) {
		this.path = path;
		this.location = location;
		this.resolvedLocation = resolvedLocation;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getResolvedLocation() {
		return resolvedLocation;
	}

	public void setResolvedLocation(String resolvedLocation) {
		this.resolvedLocation = resolvedLocation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		ResourceInfo other = (ResourceInfo)obj;
		if(path == null) {
			if(other.path != null)
				return false;
		}
		else if(!path.equals(other.path))
			return false;
		return true;
	}

}
