package com.cwctravel.eclipse.plugins.tomcat;

public enum TomcatVersion {
	VERSION_7("7.0"), VERSION_8("8.0");

	private String versionStr;

	private TomcatVersion(String versionStr) {
		this.versionStr = versionStr;
	}

	public String toString() {
		return versionStr;
	}

	public String getVersionStr() {
		return versionStr;
	}

	public static TomcatVersion fromVersionStr(String versionStr) {
		if (versionStr != null) {
			for (TomcatVersion b : TomcatVersion.values()) {
				if (versionStr.equalsIgnoreCase(b.versionStr)) {
					return b;
				}
			}
		}
		return null;
	}
}
