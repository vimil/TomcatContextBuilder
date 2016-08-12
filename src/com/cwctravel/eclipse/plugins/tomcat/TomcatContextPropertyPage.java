package com.cwctravel.eclipse.plugins.tomcat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;

public class TomcatContextPropertyPage extends PropertyPage {

	private static final String CONTEXT_CONFIG_TITLE = "&Tomcat Context:";
	private static final String CONTEXT_CONFIG_LOCATION_TITLE = "&Tomcat Context Location:";
	private static final String CONTEXT_RELOADABLE_FLAG_TITLE = "Reloadable";
	private static final String CONTEXT_USEHTTPONLY_FLAG_TITLE = "Use Http Only";
	private static final String CONTEXT_SCAN_ALL_DIRECTORIES_FOR_JARS_FLAG_TITLE = "Scan all directories for JARs";

	private static final Pattern CONTEXT_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

	private IProject project;

	private Text contextConfigText;

	private Text parameterNameText;

	private Text parameterValueText;

	private Text resourcePathText;

	private Text resourceLocationText;

	private Label contextSeparator;

	private Label contextConfigSeparator;

	private Label resourceSelectionSeparator;

	private Label resourceTableSeparator;

	private Label parameterSelectionSeparator;

	private Button addContextButton;

	private Button removeContextButton;

	private Button contextConfigSelectorButton;

	private Button addParameterButton;

	private Button resourceSelectLocationButton;

	private Button addResourceButton;

	private Button reloadableFlagButton;

	private Button useHttpOnlyFlagButton;

	private Button scanAllDirectoriesForJarsButton;

	private Combo contextsCombo;

	private TableViewer resourcesViewer;

	private TableViewer parametersViewer;

	private Group contextConfigComposite;

	private IEclipsePreferences preferences;

	private List<ContextConfigInfo> contextConfigs;

	private Map<String, ContextConfigInfo> contextConfigsMap;

	private ContextConfigInfo selectedContextConfig;

	private TomcatContextPreferenceManager tomcatPreferencesManager;

	private void addMainSection(final Composite parent, final IProject project) {
		addContextSelectorSection(parent);
		addContextConfigSection(parent);
		updateView();
	}

	private void addContextConfigSection(final Composite parent) {
		contextConfigComposite = new Group(parent, SWT.LEFT);
		FormData fdContextConfigComposite = new FormData();
		fdContextConfigComposite.top = new FormAttachment(contextSeparator, 10, SWT.BOTTOM);
		contextConfigComposite.setLayoutData(fdContextConfigComposite);
		contextConfigComposite.setLayout(new FormLayout());

		addContextLocationSection(contextConfigComposite);

		addResourcesSection(contextConfigComposite);
		createResourcesViewer(contextConfigComposite);
		createResourcesTable(contextConfigComposite, project);

		addParametersSection(contextConfigComposite);
		createParametersViewer(contextConfigComposite);
		createParametersTable(contextConfigComposite, project);
	}

	private void addContextSelectorSection(final Composite parent) {
		Label contextLabel = new Label(parent, SWT.NONE);
		contextLabel.setText(CONTEXT_CONFIG_TITLE);
		FormData fd1 = new FormData(convertWidthInCharsToPixels(18), convertHeightInCharsToPixels(1));
		fd1.left = new FormAttachment(0, 5);
		fd1.top = new FormAttachment(0, 10);
		contextLabel.setLayoutData(fd1);

		contextsCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		FormData fdContextsCombo = new FormData();
		fdContextsCombo.width = 150;
		fdContextsCombo.left = new FormAttachment(contextLabel, 5);
		fdContextsCombo.top = new FormAttachment(contextLabel, 0, SWT.CENTER);

		contextsCombo.setLayoutData(fdContextsCombo);

		List<String> contextNames = new ArrayList<String>();
		for(ContextConfigInfo contextConfig: contextConfigs) {
			String contextName = contextConfig.getContextName();
			contextNames.add(contextName);
			contextsCombo.setData(contextName, contextConfig);
		}
		contextsCombo.setItems(contextNames.toArray(new String[0]));
		if(!contextNames.isEmpty()) {
			contextsCombo.select(0);
		}

		addContextButton = new Button(parent, SWT.PUSH);
		addContextButton.setText("Add Context...");

		addContextButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				InputDialog inputDialog = new InputDialog(getShell(), "Context name", "Enter a context name", null, new IInputValidator() {

					@Override
					public String isValid(String newText) {
						if(newText != null) {
							Matcher matcher = CONTEXT_NAME_PATTERN.matcher(newText);
							if(!matcher.matches()) {
								return "Context name is not valid";
							}
						}
						return null;
					}
				});

				if(inputDialog.open() == InputDialog.OK) {
					String contextName = inputDialog.getValue();
					if(contextName != null && !contextName.isEmpty() && !contextConfigsMap.containsKey(contextName) && indexOf(contextsCombo.getItems(), contextName) == -1) {
						addContextConfig(contextName);
					}
					else {
						MessageDialog.openError(getShell(), "Invalid Context Name", "Tomcat Context Name is not valid");
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		removeContextButton = new Button(parent, SWT.PUSH);
		removeContextButton.setText("Remove Context");

		FormData fdRemoveContextButton = new FormData(convertWidthInCharsToPixels(18), 20);
		fdRemoveContextButton.right = new FormAttachment(100, -5);
		removeContextButton.setLayoutData(fdRemoveContextButton);
		removeContextButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectionIndex = contextsCombo.getSelectionIndex();
				if(selectionIndex >= 0) {
					String selectedContextConfigName = contextsCombo.getItem(selectionIndex);
					ContextConfigInfo contextConfigInfo = (ContextConfigInfo)contextsCombo.getData(selectedContextConfigName);
					contextConfigs.remove(contextConfigInfo);
					contextConfigsMap.remove(selectedContextConfigName);
					contextsCombo.remove(selectionIndex);
					contextsCombo.deselectAll();
					selectedContextConfig = null;
					updateView();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}

		});

		FormData fdAddContextButton = new FormData(convertWidthInCharsToPixels(18), 20);
		fdAddContextButton.right = new FormAttachment(removeContextButton, -5);
		addContextButton.setLayoutData(fdAddContextButton);

		contextsCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectionIndex = contextsCombo.getSelectionIndex();
				if(selectionIndex >= 0) {
					String selectedContextConfigName = contextsCombo.getItem(selectionIndex);
					selectedContextConfig = (ContextConfigInfo)contextsCombo.getData(selectedContextConfigName);
					removeContextButton.setEnabled(true);
					updateView();
				}
				else {
					removeContextButton.setEnabled(false);
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		contextSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdContextConfigSeparatorSeperator = new FormData(10, 10);
		fdContextConfigSeparatorSeperator.left = new FormAttachment(0, 5);
		fdContextConfigSeparatorSeperator.right = new FormAttachment(100, -5);
		fdContextConfigSeparatorSeperator.top = new FormAttachment(contextLabel, 10, SWT.BOTTOM);
		contextSeparator.setLayoutData(fdContextConfigSeparatorSeperator);
	}

	private void updateView() {
		if(selectedContextConfig != null) {
			String contextConfigLocation = selectedContextConfig.getContextConfigLocation();
			if(contextConfigLocation != null) {
				contextConfigText.setText(contextConfigLocation);
			}
			else {
				contextConfigText.setText("");
			}

			reloadableFlagButton.setSelection(selectedContextConfig.isReloadableFlag());
			useHttpOnlyFlagButton.setSelection(selectedContextConfig.isReloadableFlag());
			scanAllDirectoriesForJarsButton.setSelection(selectedContextConfig.isScanAllDirectoriesForJarsFlag());

			List<ResourceInfo> resources = new ArrayList<ResourceInfo>(selectedContextConfig.getResources());
			ResourcesContentProvider resourcesContentProvider = new ResourcesContentProvider(resources);
			resourcesViewer.setContentProvider(resourcesContentProvider);
			resourcesViewer.setInput(resourcesContentProvider.getResources());

			List<ParameterInfo> parameters = new ArrayList<ParameterInfo>(selectedContextConfig.getParameters());
			ParametersContentProvider parametersContentProvider = new ParametersContentProvider(parameters);
			parametersViewer.setContentProvider(parametersContentProvider);
			parametersViewer.setInput(parametersContentProvider.getParameters());

		}
		else {
			contextConfigText.setText("");
			reloadableFlagButton.setSelection(false);
			useHttpOnlyFlagButton.setSelection(false);
			scanAllDirectoriesForJarsButton.setSelection(false);

			List<ResourceInfo> resources = new ArrayList<ResourceInfo>();
			ResourcesContentProvider resourcesContentProvider = new ResourcesContentProvider(resources);
			resourcesViewer.setContentProvider(resourcesContentProvider);
			resourcesViewer.setInput(resourcesContentProvider.getResources());

			List<ParameterInfo> parameters = new ArrayList<ParameterInfo>();
			ParametersContentProvider parametersContentProvider = new ParametersContentProvider(parameters);
			parametersViewer.setContentProvider(parametersContentProvider);
			parametersViewer.setInput(parametersContentProvider.getParameters());
		}

		boolean contextSelected = contextsCombo.getSelectionIndex() >= 0;
		removeContextButton.setEnabled(contextSelected);
		addResourceButton.setEnabled(contextSelected);
		addParameterButton.setEnabled(contextSelected);
		resourceSelectLocationButton.setEnabled(contextSelected);
		contextConfigSelectorButton.setEnabled(contextSelected);
	}

	private void addContextConfig(String contextName) {
		if(contextName != null && !contextName.isEmpty() && !contextConfigsMap.containsKey(contextName) && indexOf(contextsCombo.getItems(), contextName) == -1) {
			ContextConfigInfo contextConfigInfo = new ContextConfigInfo();
			contextConfigInfo.setContextName(contextName);

			String[] contextNames = new String[contextsCombo.getItemCount() + 1];
			System.arraycopy(contextsCombo.getItems(), 0, contextNames, 0, contextsCombo.getItemCount());
			contextNames[contextNames.length - 1] = contextName;
			selectedContextConfig = contextConfigInfo;
			contextsCombo.setItems(contextNames);
			contextsCombo.setData(contextName, contextConfigInfo);
			contextsCombo.select(contextNames.length - 1);
			updateView();
		}
		else {
			MessageDialog.openError(getShell(), "Invalid Context Name", "Tomcat Context Name is not valid");
		}

	}

	private void addContextLocationSection(final Composite parent) {

		Label contextXmlLabel = new Label(parent, SWT.NONE);
		contextXmlLabel.setText(CONTEXT_CONFIG_LOCATION_TITLE);
		FormData fd1 = new FormData(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(1));
		fd1.left = new FormAttachment(0, 5);
		contextXmlLabel.setLayoutData(fd1);

		contextConfigText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		FormData fd2 = new FormData(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(1));
		fd2.top = new FormAttachment(contextXmlLabel, 0, SWT.CENTER);
		fd2.left = new FormAttachment(contextXmlLabel, 3);
		fd2.right = new FormAttachment(100, -5);
		contextConfigText.setLayoutData(fd2);

		contextConfigSelectorButton = new Button(parent, SWT.PUSH);
		contextConfigSelectorButton.setText("Select...");
		FormData fd3 = new FormData(80, 20);
		fd3.top = new FormAttachment(contextConfigText, 3, SWT.BOTTOM);
		fd3.right = new FormAttachment(100, -5);
		contextConfigSelectorButton.setLayoutData(fd3);

		Listener contextConfigSelectorButtonListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if(event.widget == contextConfigSelectorButton) {
					DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
					String contextConfigLocation = dialog.open();
					if(contextConfigLocation != null) {
						contextConfigText.setText(contextConfigLocation);
					}
				}
			}
		};

		contextConfigSelectorButton.addListener(SWT.Selection, contextConfigSelectorButtonListener);

		reloadableFlagButton = new Button(parent, SWT.CHECK);
		reloadableFlagButton.setText(CONTEXT_RELOADABLE_FLAG_TITLE);
		FormData fdReloadableFlagButton = new FormData(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(1));
		fdReloadableFlagButton.left = new FormAttachment(0, 5);
		fdReloadableFlagButton.top = new FormAttachment(contextConfigSelectorButton, 3, SWT.BOTTOM);
		reloadableFlagButton.setLayoutData(fdReloadableFlagButton);

		useHttpOnlyFlagButton = new Button(parent, SWT.CHECK);
		useHttpOnlyFlagButton.setText(CONTEXT_USEHTTPONLY_FLAG_TITLE);
		FormData fdUseHttpOnlyFlagButton = new FormData(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(1));
		fdUseHttpOnlyFlagButton.left = new FormAttachment(0, 5);
		fdUseHttpOnlyFlagButton.top = new FormAttachment(reloadableFlagButton, 3, SWT.BOTTOM);
		useHttpOnlyFlagButton.setLayoutData(fdUseHttpOnlyFlagButton);

		scanAllDirectoriesForJarsButton = new Button(parent, SWT.CHECK);
		scanAllDirectoriesForJarsButton.setText(CONTEXT_SCAN_ALL_DIRECTORIES_FOR_JARS_FLAG_TITLE);
		FormData fdScanAllDirectoriesForJarsButton = new FormData(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(1));
		fdScanAllDirectoriesForJarsButton.left = new FormAttachment(0, 5);
		fdScanAllDirectoriesForJarsButton.top = new FormAttachment(useHttpOnlyFlagButton, 3, SWT.BOTTOM);
		scanAllDirectoriesForJarsButton.setLayoutData(fdScanAllDirectoriesForJarsButton);

		contextConfigSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdContextConfigSeparatorSeperator = new FormData(10, 10);
		fdContextConfigSeparatorSeperator.left = new FormAttachment(0, 5);
		fdContextConfigSeparatorSeperator.right = new FormAttachment(100, -5);
		fdContextConfigSeparatorSeperator.top = new FormAttachment(scanAllDirectoriesForJarsButton, 10, SWT.BOTTOM);
		contextConfigSeparator.setLayoutData(fdContextConfigSeparatorSeperator);

	}

	private void addParametersSection(final Composite parent) {
		Label parameterNameLabel = new Label(parent, SWT.NONE);
		parameterNameLabel.setText("Parameter Name: ");
		FormData fdParameterNameLabel = new FormData(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(1));
		fdParameterNameLabel.top = new FormAttachment(resourceTableSeparator, 3, SWT.BOTTOM);
		fdParameterNameLabel.left = new FormAttachment(0, 10);
		parameterNameLabel.setLayoutData(fdParameterNameLabel);

		parameterNameText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		FormData fdParameterNameText = new FormData(150, convertHeightInCharsToPixels(1));
		fdParameterNameText.top = new FormAttachment(parameterNameLabel, 0, SWT.CENTER);
		fdParameterNameText.left = new FormAttachment(parameterNameLabel, 3);
		fdParameterNameText.right = new FormAttachment(100, -5);
		parameterNameText.setLayoutData(fdParameterNameText);

		Label parameterValueLabel = new Label(parent, SWT.NONE);
		parameterValueLabel.setText("Parameter Value: ");
		FormData fdParameterValueLabel = new FormData(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(1));
		fdParameterValueLabel.top = new FormAttachment(parameterNameText, 3, SWT.BOTTOM);
		fdParameterValueLabel.left = new FormAttachment(0, 10);
		parameterValueLabel.setLayoutData(fdParameterValueLabel);

		parameterValueText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		FormData fdParameterValueText = new FormData(150, convertHeightInCharsToPixels(1));
		fdParameterValueText.top = new FormAttachment(parameterValueLabel, 0, SWT.CENTER);
		fdParameterValueText.left = new FormAttachment(parameterValueLabel, 3);
		fdParameterValueText.right = new FormAttachment(100, -5);
		parameterValueText.setLayoutData(fdParameterValueText);

		addParameterButton = new Button(parent, SWT.PUSH);
		addParameterButton.setText("Add");
		FormData fdParameterAddButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdParameterAddButton.right = new FormAttachment(100, -5);
		fdParameterAddButton.top = new FormAttachment(parameterValueText, 3, SWT.BOTTOM);
		addParameterButton.setLayoutData(fdParameterAddButton);
		addParameterButton.setEnabled(false);

		Listener addParameterButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				ParametersContentProvider parametersContentProvider = (ParametersContentProvider)parametersViewer.getContentProvider();
				ParameterInfo parameterInfo = new ParameterInfo(parameterNameText.getText(), parameterValueText.getText());
				parametersContentProvider.addParameter(parameterInfo);
				parametersViewer.refresh();

			}
		};

		addParameterButton.addListener(SWT.Selection, addParameterButtonOnClickEventListener);

		parameterSelectionSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdParameterSelectionSeparator = new FormData(10, 10);
		fdParameterSelectionSeparator.left = new FormAttachment(0, 5);
		fdParameterSelectionSeparator.right = new FormAttachment(100, -5);
		fdParameterSelectionSeparator.top = new FormAttachment(addParameterButton, 10, SWT.BOTTOM);
		parameterSelectionSeparator.setLayoutData(fdParameterSelectionSeparator);
	}

	private void createParametersTable(final Composite parent, final IProject project) {
		final Table parametersTable = parametersViewer.getTable();
		FormData fdParametersTable = new FormData(750, 100);
		fdParametersTable.top = new FormAttachment(parameterSelectionSeparator, 3, SWT.BOTTOM);
		fdParametersTable.left = new FormAttachment(0, 5);
		fdParametersTable.right = new FormAttachment(100, -5);
		fdParametersTable.bottom = new FormAttachment(100, -35);
		parametersTable.setLayoutData(fdParametersTable);

		final Button removeParametersButton = new Button(parent, SWT.PUSH);
		removeParametersButton.setText("Remove");
		FormData fdRemoveParametersButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdRemoveParametersButton.left = new FormAttachment(0, 5);
		fdRemoveParametersButton.top = new FormAttachment(parametersTable, 10, SWT.BOTTOM);
		removeParametersButton.setLayoutData(fdRemoveParametersButton);
		removeParametersButton.setEnabled(false);

		Listener removeParametersButtonButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				ParametersContentProvider parametersContentProvider = (ParametersContentProvider)parametersViewer.getContentProvider();
				List<Integer> checkedItemIndices = new ArrayList<Integer>();

				TableItem[] items = parametersTable.getItems();
				if(items != null) {
					for(int i = 0; i < items.length; i++) {
						if(items[i].getChecked()) {
							checkedItemIndices.add(i);
						}
					}
				}

				if(!checkedItemIndices.isEmpty()) {
					parametersContentProvider.removeParameters(checkedItemIndices);
					parametersViewer.refresh();

					removeParametersButton.setEnabled(false);
				}
			}
		};

		removeParametersButton.addListener(SWT.Selection, removeParametersButtonButtonOnClickEventListener);

		Listener parametersTableOnSelectEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				removeParametersButton.setEnabled(isAnyItemChecked(parametersTable));
			}

		};

		parametersTable.addListener(SWT.Selection, parametersTableOnSelectEventListener);
	}

	private void createResourcesTable(final Composite parent, final IProject project) {
		final Table resourcesTable = resourcesViewer.getTable();
		FormData fdResourcesTable = new FormData(750, 100);
		fdResourcesTable.top = new FormAttachment(resourceSelectionSeparator, 3, SWT.BOTTOM);
		fdResourcesTable.left = new FormAttachment(0, 5);
		fdResourcesTable.right = new FormAttachment(100, -5);
		resourcesTable.setLayoutData(fdResourcesTable);

		final Button moveResourcesTopButton = new Button(parent, SWT.PUSH);
		moveResourcesTopButton.setText("Move Top");
		moveResourcesTopButton.setEnabled(false);
		FormData fdMoveResourcesTopButtonButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveResourcesTopButtonButton.left = new FormAttachment(0, 5);
		fdMoveResourcesTopButtonButton.top = new FormAttachment(resourcesTable, 10, SWT.BOTTOM);
		moveResourcesTopButton.setLayoutData(fdMoveResourcesTopButtonButton);

		final Button moveResourcesUpButton = new Button(parent, SWT.PUSH);
		moveResourcesUpButton.setText("Move Up");
		moveResourcesUpButton.setEnabled(false);
		FormData fdMoveResourcesUpButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveResourcesUpButton.left = new FormAttachment(moveResourcesTopButton, 5);
		fdMoveResourcesUpButton.top = new FormAttachment(resourcesTable, 10, SWT.BOTTOM);
		moveResourcesUpButton.setLayoutData(fdMoveResourcesUpButton);

		final Button moveResourcesDownButton = new Button(parent, SWT.PUSH);
		moveResourcesDownButton.setText("Move Down");
		moveResourcesDownButton.setEnabled(false);
		FormData fdMoveResourcesDownButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveResourcesDownButton.left = new FormAttachment(moveResourcesUpButton, 5);
		fdMoveResourcesDownButton.top = new FormAttachment(resourcesTable, 10, SWT.BOTTOM);
		moveResourcesDownButton.setLayoutData(fdMoveResourcesDownButton);

		final Button moveResourcesBottomButton = new Button(parent, SWT.PUSH);
		moveResourcesBottomButton.setText("Move Bottom");
		moveResourcesBottomButton.setEnabled(false);
		FormData fdMoveResourcesBottomButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveResourcesBottomButton.left = new FormAttachment(moveResourcesDownButton, 5);
		fdMoveResourcesBottomButton.top = new FormAttachment(resourcesTable, 10, SWT.BOTTOM);
		moveResourcesBottomButton.setLayoutData(fdMoveResourcesBottomButton);

		final Button removeResourcesButton = new Button(parent, SWT.PUSH);
		removeResourcesButton.setText("Remove");
		FormData fdRemoveResourcesButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdRemoveResourcesButton.left = new FormAttachment(moveResourcesBottomButton, 5);
		fdRemoveResourcesButton.top = new FormAttachment(resourcesTable, 10, SWT.BOTTOM);
		removeResourcesButton.setLayoutData(fdRemoveResourcesButton);
		removeResourcesButton.setEnabled(false);

		resourceTableSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdResourceTableSeparator = new FormData(10, 10);
		fdResourceTableSeparator.left = new FormAttachment(0, 5);
		fdResourceTableSeparator.right = new FormAttachment(100, -5);
		fdResourceTableSeparator.top = new FormAttachment(removeResourcesButton, 10, SWT.BOTTOM);
		resourceTableSeparator.setLayoutData(fdResourceTableSeparator);

		final Button[] moveResourcesButtons = new Button[] {moveResourcesTopButton, moveResourcesUpButton, moveResourcesDownButton, moveResourcesBottomButton};
		Listener moveResourcesTopButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = resourcesTable.getSelectionIndices();
				if(selectedIndices != null) {
					ResourcesContentProvider resourcesContentProvider = (ResourcesContentProvider)resourcesViewer.getContentProvider();
					List<ResourceInfo> resources = resourcesContentProvider.getResources();

					Set<ResourceInfo> checkedResources = getCheckedResources(resourcesTable, resources);

					Arrays.sort(selectedIndices);

					List<ResourceInfo> removedResources = new ArrayList<ResourceInfo>();
					for(int i = selectedIndices.length - 1; i >= 0; i--) {
						removedResources.add(0, resources.remove(selectedIndices[i]));
					}
					resources.addAll(0, removedResources);

					resourcesViewer.refresh();

					checkItems(resourcesTable, resources, checkedResources);

					setMoveResourcesButtonsEnablement(resourcesTable.getSelectionIndices(), resourcesTable.getItemCount(), moveResourcesButtons);
				}
			}

		};

		moveResourcesTopButton.addListener(SWT.Selection, moveResourcesTopButtonOnClickEventListener);

		Listener moveResourcesUpButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = resourcesTable.getSelectionIndices();
				if(selectedIndices != null) {
					ResourcesContentProvider resourcesContentProvider = (ResourcesContentProvider)resourcesViewer.getContentProvider();
					List<ResourceInfo> resources = resourcesContentProvider.getResources();

					Set<ResourceInfo> checkedResources = getCheckedResources(resourcesTable, resources);

					Arrays.sort(selectedIndices);

					for(int i = 0; i < selectedIndices.length; i++) {
						int currentIndex = selectedIndices[i];
						int previousIndex = (currentIndex - 1);
						if(previousIndex >= 0) {
							ResourceInfo previousResource = resources.get(previousIndex);
							ResourceInfo currentResource = resources.get(currentIndex);
							resources.set(previousIndex, currentResource);
							resources.set(currentIndex, previousResource);
						}
					}

					resourcesViewer.refresh();

					checkItems(resourcesTable, resources, checkedResources);

					setMoveResourcesButtonsEnablement(resourcesTable.getSelectionIndices(), resourcesTable.getItemCount(), moveResourcesButtons);
				}
			}

		};

		moveResourcesUpButton.addListener(SWT.Selection, moveResourcesUpButtonOnClickEventListener);

		Listener moveResourcesDownButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = resourcesTable.getSelectionIndices();
				if(selectedIndices != null) {
					ResourcesContentProvider resourcesContentProvider = (ResourcesContentProvider)resourcesViewer.getContentProvider();
					List<ResourceInfo> resources = resourcesContentProvider.getResources();

					Set<ResourceInfo> checkedResources = getCheckedResources(resourcesTable, resources);

					Arrays.sort(selectedIndices);

					int itemCount = resourcesTable.getItemCount();

					for(int i = selectedIndices.length - 1; i >= 0; i--) {
						int currentIndex = selectedIndices[i];
						int nextIndex = (currentIndex + 1);
						if(nextIndex < itemCount) {
							ResourceInfo previousResource = resources.get(nextIndex);
							ResourceInfo currentResource = resources.get(currentIndex);
							resources.set(nextIndex, currentResource);
							resources.set(currentIndex, previousResource);
						}
					}

					resourcesViewer.refresh();

					checkItems(resourcesTable, resources, checkedResources);

					setMoveResourcesButtonsEnablement(resourcesTable.getSelectionIndices(), itemCount, moveResourcesButtons);
				}
			}

		};

		moveResourcesDownButton.addListener(SWT.Selection, moveResourcesDownButtonOnClickEventListener);

		Listener moveResourcesBottomButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = resourcesTable.getSelectionIndices();
				if(selectedIndices != null) {

					ResourcesContentProvider resourcesContentProvider = (ResourcesContentProvider)resourcesViewer.getContentProvider();
					List<ResourceInfo> resources = resourcesContentProvider.getResources();

					Set<ResourceInfo> checkedResources = getCheckedResources(resourcesTable, resources);

					Arrays.sort(selectedIndices);

					List<ResourceInfo> removedResources = new ArrayList<ResourceInfo>();
					for(int i = selectedIndices.length - 1; i >= 0; i--) {
						removedResources.add(0, resources.remove(selectedIndices[i]));
					}
					resources.addAll(removedResources);

					resourcesViewer.refresh();

					checkItems(resourcesTable, resources, checkedResources);

					setMoveResourcesButtonsEnablement(resourcesTable.getSelectionIndices(), resourcesTable.getItemCount(), moveResourcesButtons);
				}
			}

		};

		moveResourcesBottomButton.addListener(SWT.Selection, moveResourcesBottomButtonOnClickEventListener);

		resourceLocationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				addResourceButton.setEnabled(false);

				String resourceLocationStr = resourceLocationText.getText();
				String resolvedLocation = TomcatContextUtil.resolvePath(project, resourceLocationStr);
				if(resolvedLocation != null) {
					resourceLocationText.setData(resolvedLocation);
					addResourceButton.setEnabled(true);
				}
			}
		});

		Listener removeResourcesButtonButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				ResourcesContentProvider resourcesContentProvider = (ResourcesContentProvider)resourcesViewer.getContentProvider();
				List<Integer> checkedItemIndices = new ArrayList<Integer>();

				TableItem[] items = resourcesTable.getItems();
				if(items != null) {
					for(int i = 0; i < items.length; i++) {
						if(items[i].getChecked()) {
							checkedItemIndices.add(i);
						}
					}
				}

				if(!checkedItemIndices.isEmpty()) {
					resourcesContentProvider.removeResources(checkedItemIndices);
					resourcesViewer.refresh();

					removeResourcesButton.setEnabled(false);
				}
			}
		};

		removeResourcesButton.addListener(SWT.Selection, removeResourcesButtonButtonOnClickEventListener);

		Listener resourcesTableOnSelectEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				setMoveResourcesButtonsEnablement(resourcesTable.getSelectionIndices(), resourcesTable.getItemCount(), moveResourcesButtons);

				removeResourcesButton.setEnabled(isAnyItemChecked(resourcesTable));
			}

		};

		resourcesTable.addListener(SWT.Selection, resourcesTableOnSelectEventListener);
	}

	private void addResourcesSection(final Composite parent) {
		Label resourcePathLabel = new Label(parent, SWT.NONE);
		resourcePathLabel.setText("Resource Path: ");
		FormData fdResourcePathLabel = new FormData(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(1));
		fdResourcePathLabel.top = new FormAttachment(contextConfigSeparator, 3, SWT.BOTTOM);
		fdResourcePathLabel.left = new FormAttachment(0, 10);
		resourcePathLabel.setLayoutData(fdResourcePathLabel);

		resourcePathText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		FormData fdResourcePathText = new FormData(150, convertHeightInCharsToPixels(1));
		fdResourcePathText.top = new FormAttachment(resourcePathLabel, 0, SWT.CENTER);
		fdResourcePathText.left = new FormAttachment(resourcePathLabel, 3);
		fdResourcePathText.right = new FormAttachment(100, -5);
		resourcePathText.setLayoutData(fdResourcePathText);

		Label resourceLocationLabel = new Label(parent, SWT.NONE);
		resourceLocationLabel.setText("Resource Location: ");
		FormData fdResourceLocationLabel = new FormData(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(1));
		fdResourceLocationLabel.top = new FormAttachment(resourcePathText, 3, SWT.BOTTOM);
		fdResourceLocationLabel.left = new FormAttachment(0, 10);
		resourceLocationLabel.setLayoutData(fdResourceLocationLabel);

		resourceLocationText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		FormData fdResourceLocationText = new FormData(150, convertHeightInCharsToPixels(1));
		fdResourceLocationText.top = new FormAttachment(resourceLocationLabel, 0, SWT.CENTER);
		fdResourceLocationText.left = new FormAttachment(resourceLocationLabel, 3);
		fdResourceLocationText.right = new FormAttachment(100, -5);
		resourceLocationText.setLayoutData(fdResourceLocationText);

		resourceSelectLocationButton = new Button(parent, SWT.PUSH);
		resourceSelectLocationButton.setText("Select Location...");
		FormData fdResourceSelectLocationButton = new FormData(convertWidthInCharsToPixels(25), 20);
		fdResourceSelectLocationButton.top = new FormAttachment(resourceLocationText, 3, SWT.BOTTOM);
		resourceSelectLocationButton.setLayoutData(fdResourceSelectLocationButton);

		Listener resourceSelectlocationButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				String result = dialog.open();
				if(result != null) {
					resourceLocationText.setText(result);
				}
			}

		};

		resourceSelectLocationButton.addListener(SWT.Selection, resourceSelectlocationButtonOnClickEventListener);

		addResourceButton = new Button(parent, SWT.PUSH);
		addResourceButton.setText("Add");
		FormData fdResourceAddButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdResourceAddButton.right = new FormAttachment(100, -5);
		fdResourceAddButton.top = new FormAttachment(resourceLocationText, 3, SWT.BOTTOM);
		addResourceButton.setLayoutData(fdResourceAddButton);
		addResourceButton.setEnabled(false);

		Listener addResourceButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				ResourcesContentProvider resourcesContentProvider = (ResourcesContentProvider)resourcesViewer.getContentProvider();
				ResourceInfo resourceInfo = new ResourceInfo(resourcePathText.getText(), resourceLocationText.getText(), (String)resourceLocationText.getData());
				resourcesContentProvider.addResource(resourceInfo);
				resourcesViewer.refresh();

			}
		};

		addResourceButton.addListener(SWT.Selection, addResourceButtonOnClickEventListener);

		fdResourceSelectLocationButton.right = new FormAttachment(addResourceButton, -3);

		resourceSelectionSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdResourceSelectionSeperator = new FormData(10, 10);
		fdResourceSelectionSeperator.left = new FormAttachment(0, 5);
		fdResourceSelectionSeperator.right = new FormAttachment(100, -5);
		fdResourceSelectionSeperator.top = new FormAttachment(resourceSelectLocationButton, 10, SWT.BOTTOM);
		resourceSelectionSeparator.setLayoutData(fdResourceSelectionSeperator);
	}

	private void loadPreferences(final IProject project) {
		contextConfigs = new ArrayList<ContextConfigInfo>();
		contextConfigsMap = new HashMap<String, ContextConfigInfo>();

		preferences = new ProjectScope(project).getNode(TomcatConstants.TOMCAT_CONTEXT_BUILDER_PLUGIN_ID);
		tomcatPreferencesManager = new TomcatContextPreferenceManager(project, preferences);
		List<ContextConfigInfo> contextConfigList = tomcatPreferencesManager.read();
		contextConfigs.addAll(contextConfigList);

		for(ContextConfigInfo contextConfigInfo: contextConfigs) {
			contextConfigsMap.put(contextConfigInfo.getContextName(), contextConfigInfo);
		}

		if(!contextConfigs.isEmpty()) {
			selectedContextConfig = contextConfigs.get(0);
		}
	}

	private void setMoveResourcesButtonsEnablement(int[] selectedIndices, int itemCount, final Button[] moveResourcesButtons) {
		if(allowSelectionToMoveUp(selectedIndices)) {
			moveResourcesButtons[0].setEnabled(true);
			moveResourcesButtons[1].setEnabled(true);
		}
		else {
			moveResourcesButtons[0].setEnabled(false);
			moveResourcesButtons[1].setEnabled(false);
		}

		if(allowSelectionToMoveDown(selectedIndices, itemCount)) {
			moveResourcesButtons[2].setEnabled(true);
			moveResourcesButtons[3].setEnabled(true);
		}
		else {
			moveResourcesButtons[2].setEnabled(false);
			moveResourcesButtons[3].setEnabled(false);
		}
	}

	private boolean allowSelectionToMoveUp(int[] selectedIndices) {
		boolean result = false;
		Arrays.sort(selectedIndices);
		for(int i = 0; i < selectedIndices.length; i++) {
			if(selectedIndices[i] != i) {
				result = true;
			}
		}
		return result;
	}

	private boolean allowSelectionToMoveDown(int[] selectedIndices, int itemCount) {
		boolean result = false;
		Arrays.sort(selectedIndices);
		for(int i = selectedIndices.length - 1, j = 1; i >= 0; i--, j++) {
			if(selectedIndices[i] != itemCount - j) {
				result = true;
			}
		}
		return result;
	}

	private boolean isAnyItemChecked(final Table table) {
		boolean isAnyItemChecked = false;
		TableItem[] items = table.getItems();
		if(items != null) {
			for(int i = 0; i < items.length; i++) {
				if(items[i].getChecked()) {
					isAnyItemChecked = true;
					break;
				}
			}
		}
		return isAnyItemChecked;
	}

	private List<Integer> getCheckedItemIndices(final Table resourcesTable) {
		List<Integer> result = new ArrayList<Integer>();
		TableItem[] items = resourcesTable.getItems();
		if(items != null) {
			for(int i = 0; i < items.length; i++) {
				if(items[i].getChecked()) {
					result.add(i);
				}
			}
		}
		return result;
	}

	private Set<ResourceInfo> getCheckedResources(final Table resourcesTable, List<ResourceInfo> resources) {
		List<Integer> checkedItemIndices = getCheckedItemIndices(resourcesTable);
		Set<ResourceInfo> result = new HashSet<ResourceInfo>();
		for(int checkedItemIndex: checkedItemIndices) {
			result.add(resources.get(checkedItemIndex));
		}

		return result;
	}

	private void checkItems(final Table resourcesTable, List<ResourceInfo> resources, Set<ResourceInfo> checkedResources) {
		TableItem[] items = resourcesTable.getItems();
		if(items != null) {
			for(int i = 0; i < items.length; i++) {
				if(checkedResources.contains(resources.get(i))) {
					items[i].setChecked(true);
				}
				else {
					items[i].setChecked(false);
				}
			}
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		IAdaptable adaptable = getElement();
		IProject project = (IProject)adaptable.getAdapter(IProject.class);

		loadPreferences(project);
		initializeDialogUnits(parent);
		Composite composite = createDefaultComposite(parent);
		addMainSection(composite, project);

		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		FormLayout layout = new FormLayout();
		composite.setLayout(layout);

		return composite;
	}

	private TableViewer createParametersViewer(Composite parent) {
		parametersViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		ParametersContentProvider parametersContentProvider = new ParametersContentProvider(new ArrayList<ParameterInfo>());
		parametersViewer.setContentProvider(parametersContentProvider);

		final Table parameterTable = parametersViewer.getTable();
		parameterTable.setHeaderVisible(true);
		parameterTable.setLinesVisible(true);

		TableViewerColumn parameterViewerColumn1 = new TableViewerColumn(parametersViewer, SWT.NONE);
		parameterViewerColumn1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element != null) {
					ParameterInfo parameterInfo = (ParameterInfo)element;
					return parameterInfo.getParamName();
				}
				return null;
			}
		});

		parameterViewerColumn1.setEditingSupport(new ParameterEditingSupport(parametersViewer, true));

		TableColumn parameterTableColumn1 = parameterViewerColumn1.getColumn();
		parameterTableColumn1.setText("Parameter Name");
		parameterTableColumn1.setWidth(150);
		parameterTableColumn1.setResizable(true);

		TableViewerColumn parameterViewerColumn2 = new TableViewerColumn(parametersViewer, SWT.NONE);
		parameterViewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element != null) {
					ParameterInfo parameterInfo = (ParameterInfo)element;
					return parameterInfo.getParamValue();
				}
				return null;
			}
		});

		parameterViewerColumn2.setEditingSupport(new ParameterEditingSupport(parametersViewer, false));

		TableColumn parameterTableColumn2 = parameterViewerColumn2.getColumn();
		parameterTableColumn2.setText("Parameter Value");
		parameterTableColumn2.setWidth(490);
		parameterTableColumn2.setResizable(true);

		parametersViewer.setInput(parametersContentProvider.getParameters());

		return parametersViewer;
	}

	private TableViewer createResourcesViewer(Composite parent) {
		resourcesViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		ResourcesContentProvider resourcesContentProvider = new ResourcesContentProvider(new ArrayList<ResourceInfo>());
		resourcesViewer.setContentProvider(resourcesContentProvider);

		final Table resourceTable = resourcesViewer.getTable();
		resourceTable.setHeaderVisible(true);
		resourceTable.setLinesVisible(true);

		TableViewerColumn resourceViewerColumn1 = new TableViewerColumn(resourcesViewer, SWT.NONE);
		resourceViewerColumn1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element != null) {
					ResourceInfo resourceInfo = (ResourceInfo)element;
					return resourceInfo.getPath();
				}
				return null;
			}
		});

		resourceViewerColumn1.setEditingSupport(new ResourceEditingSupport(resourcesViewer, project, true));

		TableColumn resourceTableColumn1 = resourceViewerColumn1.getColumn();
		resourceTableColumn1.setText("Resource Path");
		resourceTableColumn1.setWidth(150);
		resourceTableColumn1.setResizable(true);

		TableViewerColumn resourceViewerColumn2 = new TableViewerColumn(resourcesViewer, SWT.NONE);
		resourceViewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element != null) {
					ResourceInfo resourceInfo = (ResourceInfo)element;
					return resourceInfo.getLocation();
				}
				return null;
			}
		});

		resourceViewerColumn2.setEditingSupport(new ResourceEditingSupport(resourcesViewer, project, false));

		TableColumn resourceTableColumn2 = resourceViewerColumn2.getColumn();
		resourceTableColumn2.setText("Resource Location");
		resourceTableColumn2.setWidth(490);
		resourceTableColumn2.setResizable(true);

		resourcesViewer.setInput(resourcesContentProvider.getResources());

		return resourcesViewer;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
	}

	@Override
	protected void performApply() {
		if(selectedContextConfig != null) {
			String contextConfigLocationStr = contextConfigText.getText();
			if(!contextConfigLocationStr.isEmpty()) {
				selectedContextConfig.setContextConfigLocation(contextConfigLocationStr);
			}
			else {
				MessageDialog.openError(getShell(), "Blank Tomcat Context Location", "Tomcat Context Location cannot be blank");
			}

			boolean reloadableFlag = reloadableFlagButton.getSelection();
			selectedContextConfig.setReloadableFlag(reloadableFlag);

			boolean useHttpOnlyFlag = useHttpOnlyFlagButton.getSelection();
			selectedContextConfig.setUseHttpOnlyFlag(useHttpOnlyFlag);

			boolean scanAllDirectoriesForJarsFlag = scanAllDirectoriesForJarsButton.getSelection();
			selectedContextConfig.setScanAllDirectoriesForJarsFlag(scanAllDirectoriesForJarsFlag);

			ResourcesContentProvider resourcesContentProvider = (ResourcesContentProvider)resourcesViewer.getContentProvider();
			selectedContextConfig.setResources(resourcesContentProvider.getResources());

			ParametersContentProvider parametersContentProvider = (ParametersContentProvider)parametersViewer.getContentProvider();
			selectedContextConfig.setParameters(parametersContentProvider.getParameters());

			if(!contextConfigsMap.containsKey(selectedContextConfig.getContextName())) {
				contextConfigsMap.put(selectedContextConfig.getContextName(), selectedContextConfig);
				contextConfigs.add(selectedContextConfig);
			}
		}
	}

	@Override
	public boolean performOk() {
		try {
			tomcatPreferencesManager.store(contextConfigs);
		}
		catch(BackingStoreException e) {
			TomcatContextBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			return false;
		}
		return true;
	}

	private int indexOf(String[] strArray, String str) {
		if(strArray != null) {
			for(int i = 0; i < strArray.length; i++) {
				if(strArray[i] == str || (strArray[i] != null && strArray[i].equals(str))) {
					return i;
				}
			}
		}
		return -1;
	}
}
