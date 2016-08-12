package com.cwctravel.eclipse.plugins.tomcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ParametersContentProvider implements IStructuredContentProvider {
	private List<ParameterInfo> parameters;
	private final Map<String, ParameterInfo> parametersMap;

	public ParametersContentProvider(List<ParameterInfo> parameters) {
		this.parameters = parameters;
		this.parametersMap = new HashMap<String, ParameterInfo>();

		if(parameters != null) {
			for(ParameterInfo parameterInfo: parameters) {
				parametersMap.put(parameterInfo.getParamName(), parameterInfo);
			}
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
		if(parameters != null) {
			return parameters.toArray();
		}
		return new Object[] {};
	}

	public List<ParameterInfo> getParameters() {
		if(parameters == null) {
			parameters = new ArrayList<ParameterInfo>();
		}
		return parameters;
	}

	public void addParameter(ParameterInfo parameterInfo) {
		if(parameterInfo != null) {
			if(!parametersMap.containsKey(parameterInfo.getParamName())) {
				getParameters().add(parameterInfo);
				parametersMap.put(parameterInfo.getParamName(), parameterInfo);
			}
		}
	}

	public void removeParameters(List<Integer> parameterIndices) {
		if(parameterIndices != null) {
			List<ParameterInfo> parameters = getParameters();
			Collections.sort(parameterIndices);
			int parameterIndicesCount = parameterIndices.size();
			for(int i = parameterIndicesCount - 1; i >= 0; i--) {
				ParameterInfo removedParameterInfo = parameters.remove((int)parameterIndices.get(i));
				parametersMap.remove(removedParameterInfo.getParamName());
			}
		}
	}
}
