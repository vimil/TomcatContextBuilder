package com.cwctravel.eclipse.plugins.tomcat.contextprocessors.v8;

public class ResourceEntryInfo {
	public static final int TYPE_PRE = 0;
	public static final int TYPE_JAR = 1;
	public static final int TYPE_POST = 2;

	private int type;
	private String base;
	private String className;
	private String webAppMount;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getWebAppMount() {
		return webAppMount;
	}

	public void setWebAppMount(String webAppMount) {
		this.webAppMount = webAppMount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + type;
		result = prime * result + ((webAppMount == null) ? 0 : webAppMount.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ResourceEntryInfo other = (ResourceEntryInfo) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (className == null) {
			if (other.className != null) {
				return false;
			}
		} else if (!className.equals(other.className)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		if (webAppMount == null) {
			if (other.webAppMount != null) {
				return false;
			}
		} else if (!webAppMount.equals(other.webAppMount)) {
			return false;
		}
		return true;
	}

}
