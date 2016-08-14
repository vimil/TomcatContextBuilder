package com.cwctravel.eclipse.plugins.tomcat;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.prefs.BackingStoreException;

import com.cwctravel.eclipse.plugins.tomcat.contextprocessors.TomcatContextProcessor;

public class TomcatContextsView extends ViewPart {
	public static final String ID = "com.cwctravel.eclipse.plugins.dependencies.tomcat.TomcatContextsView";

	private static abstract class ContextConfigColumnLabelProvider extends ColumnLabelProvider implements DisposeListener {
		private final Table table;
		private final Font boldFont;

		private ContextConfigColumnLabelProvider(Table table) {
			this.table = table;
			FontDescriptor boldDescriptor = FontDescriptor.createFrom(table.getFont()).setStyle(SWT.BOLD);
			boldFont = boldDescriptor.createFont(table.getDisplay());
			table.addDisposeListener(this);
		}

		@Override
		public Font getFont(Object element) {
			Font result = null;
			if (element != null) {
				if (!isContextDisabled(element)) {
					result = boldFont;
				}
			}

			return result;
		}

		@Override
		public Color getForeground(Object element) {
			Color result = table.getDisplay().getSystemColor(SWT.COLOR_RED);
			if (element != null) {
				if (isContextDisabled(element)) {
					result = table.getDisplay().getSystemColor(SWT.COLOR_RED);
				} else {
					result = table.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
				}
			}
			return result;
		}

		@Override
		public void widgetDisposed(DisposeEvent e) {
			boldFont.dispose();

		}

	}

	private TableViewer viewer;

	private Action enableContextAction;
	private Action disableContextAction;
	private Action refreshContextsAction;
	private Action enableAllContextsAction;
	private Action disableAllContextsAction;
	private Action enableSelectedContextsAction;
	private Action disableSelectedContextsAction;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new TomcatContextsViewContentProvider());

		final Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		final TableViewerColumn viewerColumnContextName = new TableViewerColumn(viewer, SWT.NONE);
		viewerColumnContextName.setLabelProvider(new ContextConfigColumnLabelProvider(table) {
			@Override
			public String getText(Object element) {
				if (element != null) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) element;
					return contextConfigInfo.getContextName();
				}
				return null;
			}
		});

		final TableColumn tableColumnContextName = viewerColumnContextName.getColumn();
		tableColumnContextName.setText("Context");
		tableColumnContextName.setWidth(150);

		final TableViewerColumn viewerColumnContextLocation = new TableViewerColumn(viewer, SWT.NONE);
		viewerColumnContextLocation.setLabelProvider(new ContextConfigColumnLabelProvider(table) {
			@Override
			public String getText(Object element) {
				if (element != null) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) element;
					return contextConfigInfo.getResolvedContextConfigFile();
				}
				return null;
			}

		});

		final TableColumn tableColumnContextLocation = viewerColumnContextLocation.getColumn();
		tableColumnContextLocation.setText("Context Location");
		tableColumnContextLocation.setWidth(400);

		final TableViewerColumn viewerColumnContextStatus = new TableViewerColumn(viewer, SWT.NONE);
		viewerColumnContextStatus.setLabelProvider(new ContextConfigColumnLabelProvider(table) {
			@Override
			public String getText(Object element) {
				if (element != null) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) element;
					IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
					String contextConfigDisabledKey = TomcatContextUtil.getContextConfigDisabledKey(contextConfigInfo);
					return preferences.getBoolean(contextConfigDisabledKey, false) ? "Disabled" : "Enabled";
				}
				return "Disabled";
			}
		});

		final TableColumn tableColumnContextStatus = viewerColumnContextStatus.getColumn();
		tableColumnContextStatus.setText("Context Status");
		tableColumnContextStatus.setWidth(100);

		viewer.setInput(getViewSite());
		makeActions();
		hookContextMenu();
		contributeToActionBars();
	}

	@Override
	public void setFocus() {
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				TomcatContextsView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		Table contextTable = viewer.getTable();
		int selectionCount = contextTable.getSelectionCount();
		if (selectionCount == 1) {
			int selectionIndex = contextTable.getSelectionIndex();
			if (selectionIndex >= 0) {
				ContextConfigInfo contextConfigInfo = (ContextConfigInfo) contextTable.getItem(selectionIndex).getData();
				if (contextConfigInfo != null) {
					IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
					String contextConfigDisabledKey = TomcatContextUtil.getContextConfigDisabledKey(contextConfigInfo);
					boolean contextDisabled = preferences.getBoolean(contextConfigDisabledKey, false);
					if (contextDisabled) {
						manager.add(enableContextAction);
					} else {
						manager.add(disableContextAction);
					}

					manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				}
			}
		} else if (selectionCount > 1) {
			manager.add(enableSelectedContextsAction);
			manager.add(disableSelectedContextsAction);
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(enableAllContextsAction);
		manager.add(disableAllContextsAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(enableAllContextsAction);
		manager.add(disableAllContextsAction);
		manager.add(refreshContextsAction);
	}

	private void makeActions() {
		enableContextAction = new Action() {
			@Override
			public void run() {
				Table contextTable = viewer.getTable();
				int selectionIndex = contextTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) contextTable.getItem(selectionIndex).getData();
					enableContext(contextConfigInfo, true);
				}
			}
		};
		enableContextAction.setText("Enable");
		enableContextAction.setToolTipText("Enable");
		enableContextAction.setImageDescriptor(
				TomcatContextBuilderPlugin.getInstance().getImageRegistry().getDescriptor(TomcatContextBuilderPlugin.TOMCAT_ENABLE_CONTEXT_ICON_ID));

		disableContextAction = new Action() {
			@Override
			public void run() {
				Table contextTable = viewer.getTable();
				int selectionIndex = contextTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) contextTable.getItem(selectionIndex).getData();
					disableContext(contextConfigInfo, true);
				}
			}
		};
		disableContextAction.setText("Disable");
		disableContextAction.setToolTipText("Disable");
		disableContextAction.setImageDescriptor(
				TomcatContextBuilderPlugin.getInstance().getImageRegistry().getDescriptor(TomcatContextBuilderPlugin.TOMCAT_DISABLE_CONTEXT_ICON_ID));

		refreshContextsAction = new Action() {
			@Override
			public void run() {
				viewer.refresh();
			}
		};

		refreshContextsAction.setText("Refresh");
		refreshContextsAction.setToolTipText("Refresh Contexts");
		refreshContextsAction.setImageDescriptor(TomcatContextBuilderPlugin.getInstance().getImageRegistry()
				.getDescriptor(TomcatContextBuilderPlugin.TOMCAT_REFRESH_CONTEXTS_ICON_ID));

		enableSelectedContextsAction = new Action() {
			@Override
			public void run() {
				Table contextTable = viewer.getTable();
				int[] selectedIndices = contextTable.getSelectionIndices();
				for (int selectedIndex : selectedIndices) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) contextTable.getItem(selectedIndex).getData();
					enableContext(contextConfigInfo, false);
				}
				viewer.refresh();
			}
		};

		enableSelectedContextsAction.setText("Enable Selected");
		enableSelectedContextsAction.setToolTipText("Enable Selected Contexts");
		enableSelectedContextsAction.setImageDescriptor(
				TomcatContextBuilderPlugin.getInstance().getImageRegistry().getDescriptor(TomcatContextBuilderPlugin.TOMCAT_ENABLE_CONTEXT_ICON_ID));

		disableSelectedContextsAction = new Action() {
			@Override
			public void run() {
				Table contextTable = viewer.getTable();
				int[] selectedIndices = contextTable.getSelectionIndices();
				for (int selectedIndex : selectedIndices) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) contextTable.getItem(selectedIndex).getData();
					disableContext(contextConfigInfo, false);
				}
				viewer.refresh();
			}
		};

		disableSelectedContextsAction.setText("Disable Selected");
		disableSelectedContextsAction.setToolTipText("Disable Selected Contexts");
		disableSelectedContextsAction.setImageDescriptor(
				TomcatContextBuilderPlugin.getInstance().getImageRegistry().getDescriptor(TomcatContextBuilderPlugin.TOMCAT_DISABLE_CONTEXT_ICON_ID));

		enableAllContextsAction = new Action() {
			@Override
			public void run() {
				Table contextTable = viewer.getTable();
				TableItem[] items = contextTable.getItems();
				for (TableItem item : items) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) item.getData();
					enableContext(contextConfigInfo, false);
				}
				viewer.refresh();
			}
		};

		enableAllContextsAction.setText("Enable All");
		enableAllContextsAction.setToolTipText("Enable All Contexts");
		enableAllContextsAction.setImageDescriptor(
				TomcatContextBuilderPlugin.getInstance().getImageRegistry().getDescriptor(TomcatContextBuilderPlugin.TOMCAT_ENABLE_CONTEXT_ICON_ID));

		disableAllContextsAction = new Action() {
			@Override
			public void run() {
				Table contextTable = viewer.getTable();
				TableItem[] items = contextTable.getItems();
				for (TableItem item : items) {
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo) item.getData();
					disableContext(contextConfigInfo, false);
				}
				viewer.refresh();
			}
		};

		disableAllContextsAction.setText("Disable All");
		disableAllContextsAction.setToolTipText("Disable All Contexts");
		disableAllContextsAction.setImageDescriptor(
				TomcatContextBuilderPlugin.getInstance().getImageRegistry().getDescriptor(TomcatContextBuilderPlugin.TOMCAT_DISABLE_CONTEXT_ICON_ID));
	}

	private void enableContext(ContextConfigInfo contextConfigInfo, boolean refreshView) {
		try {
			if (contextConfigInfo != null) {
				IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
				String contextConfigDisabledKey = TomcatContextUtil.getContextConfigDisabledKey(contextConfigInfo);
				preferences.putBoolean(contextConfigDisabledKey, false);
				preferences.flush();
				preferences.sync();

				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(contextConfigInfo.getProjectName());
				if (project != null) {

					List<String> classpathEntries = TomcatContextUtil.getClasspathEntries(project);

					String docBase = TomcatContextUtil.getDocBase(project);

					TomcatContextProcessor tomcatContextProcessor = TomcatContextUtil.newTomcatContextProcessor(project.getWorkspace(),
							contextConfigInfo.getTomcatVersion());
					tomcatContextProcessor.load(contextConfigInfo.getResolvedContextConfigFile());
					tomcatContextProcessor.setDocBase(docBase);
					tomcatContextProcessor.setClasspathEntries(classpathEntries);
					tomcatContextProcessor.setResourceEntries(TomcatContextUtil.getResourceEntries(contextConfigInfo.getResources()));
					tomcatContextProcessor.setParameterEntries(TomcatContextUtil.getParameterEntries(contextConfigInfo.getParameters()));
					tomcatContextProcessor.setReloadableFlag(contextConfigInfo.isReloadableFlag());
					tomcatContextProcessor.setUseHttpOnlyFlag(contextConfigInfo.isUseHttpOnlyFlag());
					tomcatContextProcessor.setScanAllDirectoriesForJarsFlag(contextConfigInfo.isScanAllDirectoriesForJarsFlag());
					tomcatContextProcessor.setContainerSciFilter(contextConfigInfo.getContainerSciFilter());
					tomcatContextProcessor.store();

					if (refreshView) {
						viewer.refresh();
					}

				}
			}
		} catch (BackingStoreException e) {
			TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
	}

	private void disableContext(ContextConfigInfo contextConfigInfo, boolean refreshView) {
		if (contextConfigInfo != null) {
			try {
				IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
				String contextConfigDisabledKey = TomcatContextUtil.getContextConfigDisabledKey(contextConfigInfo);
				preferences.putBoolean(contextConfigDisabledKey, true);
				preferences.flush();
				preferences.sync();

				if (refreshView) {
					viewer.refresh();
				}

				new File(contextConfigInfo.getResolvedContextConfigFile()).delete();
			} catch (BackingStoreException e) {
				TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
	}

	private static boolean isContextDisabled(Object element) {
		ContextConfigInfo contextConfigInfo = (ContextConfigInfo) element;
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
		String contextConfigDisabledKey = TomcatContextUtil.getContextConfigDisabledKey(contextConfigInfo);
		boolean isContextDisabled = preferences.getBoolean(contextConfigDisabledKey, false);
		return isContextDisabled;
	}
}
