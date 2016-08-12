package com.cwctravel.eclipse.plugins.tomcat;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class ParameterEditingSupport extends EditingSupport {
	private boolean isName;
	private CellEditor cellEditor;

	ParameterEditingSupport(ColumnViewer viewer, boolean isName) {
		super(viewer);
		this.isName = isName;

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
		ParameterInfo parameterInfo = (ParameterInfo)element;
		if(parameterInfo != null) {
			return isName ? parameterInfo.getParamName() : parameterInfo.getParamValue();
		}
		return "";
	}

	@Override
	protected void setValue(Object element, Object value) {
		ParameterInfo parameterInfo = (ParameterInfo)element;
		if(parameterInfo != null) {
			String strValue = (String)value;
			if(isName) {
				parameterInfo.setParamName(strValue);
			}
			else {
				parameterInfo.setParamValue(strValue);
			}
			getViewer().refresh(element);
		}
	}
}