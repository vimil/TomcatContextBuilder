package com.cwctravel.eclipse.plugins.tomcat;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

class ResourceEditingSupport extends EditingSupport {
	private boolean isPath;
	private CellEditor cellEditor;
	private IProject currentProject;

	ResourceEditingSupport(ColumnViewer viewer, final IProject currentProject, boolean isPath) {
		super(viewer);
		this.currentProject = currentProject;
		this.isPath = isPath;

		cellEditor = new CellEditor((Composite)viewer.getControl()) {
			private Text editor;

			@Override
			protected Control createControl(Composite parent) {
				editor = new Text(parent, SWT.SINGLE);
				return editor;
			}

			@Override
			protected Object doGetValue() {
				return editor.getText();
			}

			@Override
			protected void doSetFocus() {
				editor.setFocus();
			}

			@Override
			protected void doSetValue(Object value) {
				editor.setText((String)value);
			}

			@Override
			public LayoutData getLayoutData() {
				LayoutData data = new LayoutData();
				data.minimumWidth = 0;
				return data;
			}
		};
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return cellEditor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		ResourceInfo resourceInfo = (ResourceInfo)element;
		if(resourceInfo != null) {
			return isPath ? resourceInfo.getPath() : resourceInfo.getLocation();
		}
		return "";
	}

	@Override
	protected void setValue(Object element, Object value) {
		ResourceInfo resourceInfo = (ResourceInfo)element;
		if(resourceInfo != null) {
			String path = (String)value;
			if(isPath) {
				resourceInfo.setPath(path);
			}
			else {
				String resolvedPath = TomcatContextUtil.resolvePath(currentProject, path);
				if(resolvedPath != null) {
					resourceInfo.setLocation(resolvedPath);
				}
			}
			getViewer().refresh(element);
		}
	}
}