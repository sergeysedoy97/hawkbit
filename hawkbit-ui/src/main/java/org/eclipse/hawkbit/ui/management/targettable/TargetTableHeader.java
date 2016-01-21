/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUITargetDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Target table header layout.
 *
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetTableHeader extends AbstractTableHeader {

    private static final long serialVersionUID = -8647521126666320022L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private UINotification notification;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementViewAcceptCriteria managementViewAcceptCriteria;

    @Autowired
    private TargetAddUpdateWindowLayout targetAddUpdateWindow;

    @Autowired
    private TargetBulkUpdateWindowLayout targetBulkUpdateWindow;

    private Boolean isComplexFilterViewDisplayed = Boolean.FALSE;

    /**
     * Initialization of Target Header Component.
     */
    @PostConstruct
    protected void init() {
        super.init();
        // creating add window for adding new target
        targetAddUpdateWindow.init();
        targetBulkUpdateWindow.init();
        eventBus.subscribe(this);
        onLoadRestoreState();
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_TARGET_TAG_LAYOUT) {
            setFilterButtonsIconVisible(true);
        } else if (event == ManagementUIEvent.SHOW_TARGET_TAG_LAYOUT) {
            setFilterButtonsIconVisible(false);
        } else if (event == ManagementUIEvent.RESET_SIMPLE_FILTERS) {
            UI.getCurrent().access(() -> onSimpleFilterReset());
        } else if (event == ManagementUIEvent.RESET_TARGET_FILTER_QUERY) {
            UI.getCurrent().access(() -> onCustomFilterReset());
        }
    }

    private void onCustomFilterReset() {
        isComplexFilterViewDisplayed = Boolean.FALSE;
        reEnableSearch();
    }

    private void onLoadRestoreState() {
        if (managementUIState.isCustomFilterSelected()) {
            onSimpleFilterReset();
        }
    }

    private void onSimpleFilterReset() {
        isComplexFilterViewDisplayed = Boolean.TRUE;
        disableSearch();
        if (isSearchFieldOpen()) {
            resetSearch();
        }
        if (managementUIState.getTargetTableFilters().getDistributionSet().isPresent()) {
            closeFilterByDistribution();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent dragEvent) {
        if (dragEvent == DragEvent.DISTRIBUTION_DRAG) {
            if (!isComplexFilterViewDisplayed) {
                UI.getCurrent().access(() -> addStyleName("show-table-header-drop-hint"));
            }
        } else {
            UI.getCurrent().access(() -> removeStyleName("show-table-header-drop-hint"));
        }
    }

    @Override
    protected String getHeaderCaption() {
        return i18n.get("header.target.table");
    }

    @Override
    protected String getSearchBoxId() {
        return SPUIComponetIdProvider.TARGET_TEXT_FIELD;
    }

    @Override
    protected String getSearchRestIconId() {
        return SPUIComponetIdProvider.TARGET_TBL_SEARCH_RESET_ID;
    }

    @Override
    protected String getAddIconId() {
        return SPUIComponetIdProvider.TARGET_TBL_ADD_ICON_ID;
    }

    @Override
    protected String getBulkUploadIconId() {
        return SPUIComponetIdProvider.TARGET_TBL_BULK_UPLOAD_ICON_ID;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        return getSearchText();
    }

    @Override
    protected String getDropFilterId() {
        return SPUIComponetIdProvider.TARGET_DROP_FILTER_ICON;
    }

    @Override
    protected String getDropFilterWrapperId() {
        return SPUIComponetIdProvider.TARGET_FILTER_WRAPPER_ID;
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateTargetPermission();
    }

    @Override
    protected boolean isDropHintRequired() {
        return Boolean.TRUE;
    }

    @Override
    protected boolean isDropFilterRequired() {
        return Boolean.TRUE;
    }

    @Override
    protected String getShowFilterButtonLayoutId() {
        return SPUIComponetIdProvider.SHOW_TARGET_TAGS;
    }

    @Override
    protected void showFilterButtonsLayout() {
        managementUIState.setTargetTagFilterClosed(false);
        eventBus.publish(this, ManagementUIEvent.SHOW_TARGET_TAG_LAYOUT);
    }

    @Override
    protected void resetSearchText() {
        managementUIState.getTargetTableFilters().setSearchText(null);
        eventBus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_TEXT);
    }

    private String getSearchText() {
        return managementUIState.getTargetTableFilters().getSearchText().isPresent() ? managementUIState
                .getTargetTableFilters().getSearchText().get() : null;
    }

    @Override
    protected String getMaxMinIconId() {
        return SPUIComponetIdProvider.TARGET_MAX_MIN_TABLE_ICON;
    }

    @Override
    public void maximizeTable() {
        managementUIState.setTargetTableMaximized(Boolean.TRUE);
        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.MAXIMIZED));
    }

    @Override
    public void minimizeTable() {
        managementUIState.setTargetTableMaximized(Boolean.FALSE);
        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.MINIMIZED));
    }

    @Override
    public Boolean onLoadIsTableMaximized() {
        return managementUIState.isTargetTableMaximized();
    }

    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return managementUIState.isTargetTagFilterClosed();
    }

    @Override
    protected void searchBy(final String newSearchText) {
        managementUIState.getTargetTableFilters().setSearchText(newSearchText);
        eventBus.publish(this, TargetFilterEvent.FILTER_BY_TEXT);
    }

    @Override
    protected void addNewItem(final ClickEvent event) {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        targetAddUpdateWindow.resetComponents();
        final Window addTargetWindow = targetAddUpdateWindow.getWindow();
        addTargetWindow.setCaption(i18n.get("caption.add.new.target"));
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected void bulkUpload(final ClickEvent event) {
        targetBulkUpdateWindow.resetComponents();
        final Window bulkUploadTargetWindow = targetBulkUpdateWindow.getWindow();
        UI.getCurrent().addWindow(bulkUploadTargetWindow);
        bulkUploadTargetWindow.setVisible(true);
    }

    @Override
    protected Boolean isAddNewItemAllowed() {
        return Boolean.TRUE;
    }

    @Override
    protected Boolean isBulkUploadAllowed() {
        return Boolean.TRUE;
    }

    @Override
    protected DropHandler getDropFilterHandler() {
        return new DropHandler() {
            @Override
            public void drop(final DragAndDropEvent event) {
                filterByDroppedDist(event);
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return managementViewAcceptCriteria;
            }

        };
    }

    private void filterByDroppedDist(final DragAndDropEvent event) {
        if (doValidations(event)) {
            final TableTransferable tableTransferable = (TableTransferable) event.getTransferable();
            final Table source = tableTransferable.getSourceComponent();
            if (source.getId().equals(SPUIComponetIdProvider.DIST_TABLE_ID)) {
                final Set<DistributionSetIdName> distributionIdSet = getDropppedDistributionDetails(tableTransferable);
                if (distributionIdSet != null && !distributionIdSet.isEmpty()) {
                    final DistributionSetIdName distributionSetIdName = distributionIdSet.iterator().next();
                    managementUIState.getTargetTableFilters().setDistributionSet(distributionSetIdName);
                    addFilterTextField(distributionSetIdName);
                }
            }
        }
    }

    /**
     * Validation for drag event.
     *
     * @param dragEvent
     * @return
     */
    private Boolean doValidations(final DragAndDropEvent dragEvent) {
        final Component compsource = dragEvent.getTransferable().getSourceComponent();
        Boolean isValid = Boolean.TRUE;
        if (compsource instanceof Table && !isComplexFilterViewDisplayed) {
            final TableTransferable transferable = (TableTransferable) dragEvent.getTransferable();
            final Table source = transferable.getSourceComponent();

            if (!source.getId().equals(SPUIComponetIdProvider.DIST_TABLE_ID)) {
                notification.displayValidationError(i18n.get("message.action.not.allowed"));
                isValid = Boolean.FALSE;
            } else {
                if (getDropppedDistributionDetails(transferable).size() > 1) {
                    notification.displayValidationError(i18n.get("message.onlyone.distribution.dropallowed"));
                    isValid = Boolean.FALSE;
                }
            }
        } else {
            notification.displayValidationError(i18n.get("message.action.not.allowed"));
            isValid = Boolean.FALSE;
        }
        return isValid;
    }

    private Set<DistributionSetIdName> getDropppedDistributionDetails(final TableTransferable transferable) {
        @SuppressWarnings("unchecked")
        final Set<DistributionSetIdName> distSelected = (Set<DistributionSetIdName>) transferable.getSourceComponent()
                .getValue();
        final Set<DistributionSetIdName> distributionIdSet = new HashSet<DistributionSetIdName>();
        if (!distSelected.contains(transferable.getData("itemId"))) {
            distributionIdSet.add((DistributionSetIdName) transferable.getData("itemId"));
        } else {
            distributionIdSet.addAll(distSelected);
        }

        return distributionIdSet;
    }

    private void addFilterTextField(final DistributionSetIdName distributionSetIdName) {
        final Button filterLabelClose = SPUIComponentProvider.getButton("drop.filter.close", "", "", "", true,
                FontAwesome.TIMES_CIRCLE, SPUIButtonStyleSmallNoBorder.class);
        filterLabelClose.addClickListener(clickEvent -> closeFilterByDistribution());
        final Label filteredDistLabel = new Label();
        filteredDistLabel.setStyleName(ValoTheme.LABEL_COLORED + " " + ValoTheme.LABEL_SMALL);
        String name = HawkbitCommonUtil.getDistributionNameAndVersion(distributionSetIdName.getName(),
                distributionSetIdName.getVersion());
        if (name.length() > SPUITargetDefinitions.DISTRIBUTION_NAME_MAX_LENGTH_ALLOWED) {
            name = new StringBuilder(name.substring(0, SPUITargetDefinitions.DISTRIBUTION_NAME_LENGTH_ON_FILTER))
                    .append("...").toString();
        }
        filteredDistLabel.setValue(name);
        filteredDistLabel.setSizeUndefined();
        getFilterDroppedInfo().removeAllComponents();
        getFilterDroppedInfo().setSizeFull();
        getFilterDroppedInfo().addComponent(filteredDistLabel);
        getFilterDroppedInfo().addComponent(filterLabelClose);
        getFilterDroppedInfo().setExpandRatio(filteredDistLabel, 1.0f);
        eventBus.publish(this, TargetFilterEvent.FILTER_BY_DISTRIBUTION);
    }

    private void closeFilterByDistribution() {

        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        /* Remove filter by distribution information. */
        getFilterDroppedInfo().removeAllComponents();
        getFilterDroppedInfo().setSizeUndefined();
        /* Remove distribution Id from target filter parameters */
        managementUIState.getTargetTableFilters().setDistributionSet(null);

        /* Reload the table */
        eventBus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_DISTRIBUTION);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * displayFilterDropedInfoOnLoad()
     */
    @Override
    protected void displayFilterDropedInfoOnLoad() {
        if (managementUIState.getTargetTableFilters().getDistributionSet().isPresent()) {
            addFilterTextField(managementUIState.getTargetTableFilters().getDistributionSet().get());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * getFilterIconStyle()
     */
    @Override
    protected String getFilterIconStyle() {
        return null;
    }
}
