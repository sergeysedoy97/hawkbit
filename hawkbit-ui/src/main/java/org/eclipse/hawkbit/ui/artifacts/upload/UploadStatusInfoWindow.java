/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.ProgressBarRenderer;
import com.vaadin.ui.themes.ValoTheme;

import elemental.json.JsonValue;

/**
 * Shows upload status during upload.
 *
 *
 *
 */

@ViewScope
@SpringComponent
public class UploadStatusInfoWindow extends Window {

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    private static final String PROGRESS = "Progress";

    private static final String FILE_NAME = "File name";

    private static final String STATUS = "Status";

    private static final String REASON = "Reason";

    private static final long serialVersionUID = 1L;

    private Grid grid;

    private IndexedContainer uploads;

    private volatile boolean errorOccured = false;

    private Button minimizeButton;

    private VerticalLayout mainLayout;

    private Label windowCaption;

    private Button closeButton;

    private Button resizeButton;
    
    private UI ui;


    /**
     * Default Constructor.
     */
    @PostConstruct
    void init() {

        setPopupProperties();
        createStatusPopupHeaderComponents();

        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.setSizeUndefined();
        setPopupSizeInMinMode();

        uploads = getGridContainer();
        grid = createGrid();
        setGridColumnProperties();

        mainLayout.addComponents(getCaptionLayout(), grid);
        mainLayout.setExpandRatio(grid, 1.0F);
        setContent(mainLayout);
        eventBus.subscribe(this);
        ui = UI.getCurrent();

    }

    
    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadStatusEvent event) {
        if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_IN_PROGRESS) {
            UI.getCurrent().access(
                    () -> updateProgress(event.getUploadStatus().getFileName(), event.getUploadStatus().getBytesRead(),
                            event.getUploadStatus().getContentLength()));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STARTED) {
            UI.getCurrent().access(() -> onStartOfUpload(event));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STREAMING_FAILED) {
            ui.access(() -> uploadFailed(event.getUploadStatus().getFileName(), event.getUploadStatus()
                    .getFailureReason()));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_SUCCESSFUL) {
            UI.getCurrent().access(() -> uploadSucceeded(event.getUploadStatus().getFileName()));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STREAMING_FINISHED) {
            ui.access(() -> uploadSucceeded(event.getUploadStatus().getFileName()));
        }
    }
    
    private void onStartOfUpload(UploadStatusEvent event) {
        uploadSessionStarted();
        uploadStarted(event.getUploadStatus().getFileName());
    }


    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventBus.unsubscribe(this);
    }
    
    private void restoreState() {
        Indexed container = grid.getContainerDataSource();
        if (container.getItemIds().isEmpty()) {
            container.removeAllItems();
            for (UploadStatusObject statusObject : artifactUploadState.getUploadedFileStatusList()) {
                Item item = container.addItem(statusObject.getFilename());
                item.getItemProperty(REASON).setValue(statusObject.getReason() != null ? statusObject.getReason() : "");
                item.getItemProperty(STATUS).setValue(statusObject.getStatus());
                item.getItemProperty(PROGRESS).setValue(statusObject.getProgress());
                item.getItemProperty(FILE_NAME).setValue(statusObject.getFilename());
            }
            if (artifactUploadState.isUploadCompleted()) {
                minimizeButton.setEnabled(false);
            }
        }
    }

    private void setPopupProperties() {
        setId(SPUIComponetIdProvider.UPLOAD_STATUS_POPUP_ID);
        addStyleName(SPUIStyleDefinitions.UPLOAD_INFO);
        setImmediate(true);
        setResizable(false);
        setDraggable(true);
        setClosable(false);
        setModal(true);
    }

    private void setGridColumnProperties() {
        grid.getColumn(STATUS).setRenderer(new StatusRenderer());
        grid.getColumn(PROGRESS).setRenderer(new ProgressBarRenderer());
        grid.setColumnOrder(STATUS, PROGRESS, FILE_NAME, REASON);
        setColumnWidth();
        grid.setFrozenColumnCount(4);
    }

    private Grid createGrid() {
        Grid statusGrid = new Grid(uploads);
        statusGrid.addStyleName(SPUIStyleDefinitions.UPLOAD_STATUS_GRID);
        statusGrid.setSelectionMode(SelectionMode.NONE);
        statusGrid.setHeaderVisible(true);
        statusGrid.setImmediate(true);
        statusGrid.setSizeFull();
        return statusGrid;
    }

    private IndexedContainer getGridContainer() {
        IndexedContainer uploadContainer = new IndexedContainer();
        uploadContainer.addContainerProperty(STATUS, String.class, "Active");
        uploadContainer.addContainerProperty(FILE_NAME, String.class, null);
        uploadContainer.addContainerProperty(PROGRESS, Double.class, 0D);
        uploadContainer.addContainerProperty(REASON, String.class, "");
        return uploadContainer;
    }

    private HorizontalLayout getCaptionLayout() {
        final HorizontalLayout captionLayout = new HorizontalLayout();
        captionLayout.setSizeFull();
        captionLayout.setHeight("36px");
        captionLayout.addComponents(windowCaption, minimizeButton, resizeButton, closeButton);
        captionLayout.setExpandRatio(windowCaption, 1.0F);
        captionLayout.addStyleName("v-window-header");
        return captionLayout;
    }

    private void createStatusPopupHeaderComponents() {
        minimizeButton = getMinimizeButton();
        windowCaption = new Label("Upload status");
        closeButton = getCloseButton();
        resizeButton = getResizeButton();
    }

    private void setColumnWidth() {
        grid.getColumn(STATUS).setWidth(70);
        grid.getColumn(PROGRESS).setWidth(150);
        grid.getColumn(FILE_NAME).setWidth(280);
        grid.getColumn(REASON).setWidth(300);
    }

    private void resetColumnWidth() {
        grid.getColumn(STATUS).setWidthUndefined();
        grid.getColumn(PROGRESS).setWidthUndefined();
        grid.getColumn(FILE_NAME).setWidthUndefined();
        grid.getColumn(REASON).setWidthUndefined();
    }

    private static class StatusRenderer extends HtmlRenderer {

        private static final long serialVersionUID = -5365795450234970943L;

        @Override
        public JsonValue encode(final String value) {
            String result ;
            switch (value) {
            case "Finished":
                result = "<div class=\"statusIconGreen\">" + FontAwesome.CHECK_CIRCLE.getHtml() + "</div>";
                break;
            case "Failed":
                result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml() + "</div>";
                break;
            default:
                result = "<div class=\"statusIconActive\"></div>";
            }

            return super.encode(result);
        }
    }

    /**
     * Automatically close if not error has occured.
     */
    void uploadSessionFinished() {
        if (!errorOccured) {
            close();
        }
        artifactUploadState.setUploadCompleted(true);
        minimizeButton.setEnabled(false);
    }

    void uploadSessionStarted() {
        if (!artifactUploadState.isStatusPopupMinimized()) {
            close();
            openWindow();
        }
        minimizeButton.setEnabled(true);
        artifactUploadState.setUploadCompleted(false);
    }

    void openWindow() {
        UI.getCurrent().addWindow(this);
        center();
    }

    void maximizeStatusPopup() {
        openWindow();
        restoreState();
    }

    void uploadStarted(final String filename) {
        final Item item = uploads.addItem(filename);
        if (item != null) {
            item.getItemProperty(FILE_NAME).setValue(filename);
        }
        grid.scrollToEnd();
        UploadStatusObject uploadStatus = new UploadStatusObject(filename);
        uploadStatus.setStatus("Active");
        artifactUploadState.getUploadedFileStatusList().add(uploadStatus);
    }

    void updateProgress(final String filename, final long readBytes, final long contentLength) {
         final Item item = uploads.getItem(filename);
        if (item != null) {
            double progress = (double) readBytes / (double) contentLength;
            item.getItemProperty(PROGRESS).setValue(progress);
            List<UploadStatusObject> uploadStatusObjectList = (List<UploadStatusObject>) artifactUploadState 
                    .getUploadedFileStatusList().stream().filter(e -> e.getFilename().equals(filename))
                    .collect(Collectors.toList());
            if (!uploadStatusObjectList.isEmpty()) {
                UploadStatusObject uploadStatusObject = uploadStatusObjectList.get(0);
                uploadStatusObject.setProgress(progress);
            }
        }
    }

    /**
     * Called when each file upload is success.
     * 
     * @param filename
     *            of the uploaded file.
     */
    public void uploadSucceeded(final String filename) {
        final Item item = uploads.getItem(filename);
        if (item != null) {
            String status = "Finished";
            item.getItemProperty(STATUS).setValue(status);
            List<UploadStatusObject> uploadStatusObjectList = (List<UploadStatusObject>) artifactUploadState
                    .getUploadedFileStatusList().stream().filter(e -> e.getFilename().equals(filename))
                    .collect(Collectors.toList());
            if (!uploadStatusObjectList.isEmpty()) {
                UploadStatusObject uploadStatusObject = uploadStatusObjectList.get(0);
                uploadStatusObject.setStatus(status);
            }
        }
    }

    void uploadFailed(final String filename, final String errorReason) {
        if (!errorOccured) {
            errorOccured = true;
        }
        String status = "Failed";
        final Item item = uploads.getItem(filename);
        if (item != null) {
            item.getItemProperty(REASON).setValue(errorReason);
            item.getItemProperty(STATUS).setValue(status);
        }
        List<UploadStatusObject> uploadStatusObjectList = (List<UploadStatusObject>) artifactUploadState
                .getUploadedFileStatusList().stream().filter(e -> e.getFilename().equals(filename))
                .collect(Collectors.toList());
        if (!uploadStatusObjectList.isEmpty()) {
            UploadStatusObject uploadStatusObject = uploadStatusObjectList.get(0);
            uploadStatusObject.setStatus(status);
            uploadStatusObject.setReason(errorReason);
        }
    }

    protected void clearWindow() {
        errorOccured = false;
        uploads.removeAllItems();
        setWindowMode(WindowMode.NORMAL);
        this.close();
        artifactUploadState.getUploadedFileStatusList().clear();
        artifactUploadState.getNumberOfFileUploadsFailed().set(0);
    }

    private void setPopupSizeInMinMode() {
        mainLayout.setWidth(800, Unit.PIXELS);
        mainLayout.setHeight(510, Unit.PIXELS);
    }

    private Button getMinimizeButton() {
        final Button minimizeBtn = SPUIComponentProvider.getButton(
                SPUIComponetIdProvider.UPLOAD_STATUS_POPUP_MINIMIZE_BUTTON_ID, "", "", "", true, FontAwesome.MINUS,
                SPUIButtonStyleSmallNoBorder.class);
        minimizeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        minimizeBtn.addClickListener(event -> minimizeWindow());
        minimizeBtn.setEnabled(true);
        return minimizeBtn;
    }

    private Button getResizeButton() {
        final Button resizeBtn = SPUIComponentProvider.getButton(
                SPUIComponetIdProvider.UPLOAD_STATUS_POPUP_RESIZE_BUTTON_ID, "", "", "", true, FontAwesome.EXPAND,
                SPUIButtonStyleSmallNoBorder.class);
        resizeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        resizeBtn.addClickListener(event -> resizeWindow(event));
        return resizeBtn;
    }

    private void resizeWindow(ClickEvent event) {
        if (event.getButton().getIcon() == FontAwesome.EXPAND) {
            event.getButton().setIcon(FontAwesome.COMPRESS);
            setWindowMode(WindowMode.MAXIMIZED);
            resetColumnWidth();
            grid.getColumn(STATUS).setExpandRatio(0);
            grid.getColumn(PROGRESS).setExpandRatio(1);
            grid.getColumn(FILE_NAME).setExpandRatio(2);
            grid.getColumn(REASON).setExpandRatio(3);
            mainLayout.setSizeFull();
        } else {
            event.getButton().setIcon(FontAwesome.EXPAND);
            setWindowMode(WindowMode.NORMAL);
            setColumnWidth();
            setPopupSizeInMinMode();
        }
    }

    private void minimizeWindow() {
        this.close();
        artifactUploadState.setStatusPopupMinimized(true);
        eventBus.publish(this, UploadArtifactUIEvent.MINIMIZED_STATUS_POPUP);
    }

    private Button getCloseButton() {
        final Button closeBtn = SPUIComponentProvider.getButton(
                SPUIComponetIdProvider.UPLOAD_STATUS_POPUP_CLOSE_BUTTON_ID, "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleSmallNoBorder.class);
        closeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        closeBtn.addClickListener(event -> clearWindow());
        return closeBtn;
    }
}
