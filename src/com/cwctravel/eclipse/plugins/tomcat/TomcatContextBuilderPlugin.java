package com.cwctravel.eclipse.plugins.tomcat;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class TomcatContextBuilderPlugin extends AbstractUIPlugin {
	private static BundleContext context;
	private static TomcatContextBuilderPlugin instance;

	public static final String TOMCAT_CONTEXT_ICON_ID = "com.cwctravel.eclipse.plugins.tomcat.icons.tomcatContext";
	public static final String TOMCAT_ENABLE_CONTEXT_ICON_ID = "com.cwctravel.eclipse.plugins.tomcat.icons.enableContext";
	public static final String TOMCAT_DISABLE_CONTEXT_ICON_ID = "com.cwctravel.eclipse.plugins.tomcat.icons.disableContext";
	public static final String TOMCAT_REFRESH_CONTEXTS_ICON_ID = "com.cwctravel.eclipse.plugins.tomcat.icons.refreshContexts";

	public static TomcatContextBuilderPlugin getInstance() {
		return instance;
	}

	public TomcatContextBuilderPlugin() {
		instance = this;
	}

	static BundleContext getContext() {
		return context;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		Bundle bundle = Platform.getBundle(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);

		ImageDescriptor tomcatContextImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/tomcat-context.png"), null));
		registry.put(TOMCAT_CONTEXT_ICON_ID, tomcatContextImage);

		ImageDescriptor enableTomcatContextImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/enable-context.png"), null));
		registry.put(TOMCAT_ENABLE_CONTEXT_ICON_ID, enableTomcatContextImage);

		ImageDescriptor disableTomcatContextImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/disable-context.png"), null));
		registry.put(TOMCAT_DISABLE_CONTEXT_ICON_ID, disableTomcatContextImage);

		ImageDescriptor refreshTomcatContextImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/refresh-contexts.png"), null));
		registry.put(TOMCAT_REFRESH_CONTEXTS_ICON_ID, refreshTomcatContextImage);
	}

	public void start(BundleContext bundleContext) throws Exception {
		TomcatContextBuilderPlugin.context = bundleContext;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(ProjectEventListener.getInstance(), IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		TomcatContextBuilderPlugin.context = null;
	}

	public static void log(int severity, String message, Throwable t) {
		getInstance().getLog().log(new Status(severity, TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID, message, t));
	}

}
