package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streamlined dataset classification panel using the new high-performance architecture.
 * Integrates DatasetImageViewer, NewDatasetROIOverlay, and DatasetControlsPanel.
 * 
 * @author Sebastian Micu
 * @version 5.0.0
 */
public class DatasetClassificationPanel extends JPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetClassificationPanel.class);
    
    // Main components - using new streamlined architecture
    private SimpleImageGallery imageGallery;
    private DatasetImageViewer datasetImageViewer;
    private DatasetControlsPanel controlsPanel;
    
    // Data
    private File selectedImageFolder;
    private File selectedRoiZip;
    private final List<String> classNames = new ArrayList<>();
    
    // Settings
    private final MainSettings settings;

    public DatasetClassificationPanel(MainSettings settings) {
        this.settings = settings;
        initializeClassNames();
        initializeComponents();
        setupEventHandlers();
        
        LOGGER.info("Initialized streamlined DatasetClassificationPanel v5.0.0");
    }
    
    private void initializeClassNames() {
        classNames.add("Unclassified");
        classNames.add("Normal");
        classNames.add("Tumor");
    }

    /**
     * Initialize components using new streamlined architecture.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
        
        // Create image gallery
        imageGallery = new SimpleImageGallery();
        imageGallery.setPreferredSize(new Dimension(200, 0));
        
        // Create streamlined dataset image viewer with zoom/pan
        datasetImageViewer = new DatasetImageViewer();
        
        // Create integrated controls panel
        controlsPanel = new DatasetControlsPanel();
        
        // Connect controls to overlay
        controlsPanel.setOverlay(datasetImageViewer.getROIOverlay());
        
        // Layout using split panes
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftSplit.setLeftComponent(imageGallery);
        leftSplit.setRightComponent(datasetImageViewer);
        leftSplit.setDividerLocation(200);
        leftSplit.setResizeWeight(0.0);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(leftSplit);
        mainSplit.setRightComponent(controlsPanel);
        mainSplit.setDividerLocation(800);
        mainSplit.setResizeWeight(1.0);
        
        add(mainSplit, BorderLayout.CENTER);
        
        LOGGER.debug("Components initialized with new streamlined architecture");
    }
    
    private void setupEventHandlers() {
        // Image gallery selection handler - use correct method name
        imageGallery.setSelectionChangeListener(e -> {
            File selectedImage = imageGallery.getSelectedImageFile();
            if (selectedImage != null) {
                loadImageWithROIs(selectedImage);
            }
        });
        
        // ROI overlay interaction handler
        datasetImageViewer.getROIOverlay().addInteractionListener(new NewDatasetROIOverlay.InteractionListener() {
            @Override
            public void onROIClicked(UserROI roi, String assignedClass) {
                LOGGER.info("ROI '{}' assigned to class '{}'", roi.getName(), assignedClass);
                controlsPanel.updateStatus("Assigned '" + assignedClass + "' to " + roi.getName());
            }
            
            @Override
            public void onROIHovered(UserROI roi) {
                controlsPanel.updateStatus("Hovering: " + roi.getName());
            }
            
            @Override
            public void onClassAssigned(UserROI roi, String className) {
                LOGGER.debug("Class '{}' assigned to ROI '{}'", className, roi.getName());
            }
            
            @Override
            public void onProgressUpdate(int loaded, int total) {
                controlsPanel.updateProgress(loaded, total);
            }
        });
        
        // Controls panel listeners
        // Controls panel listeners
        controlsPanel.addControlListener(new DatasetControlsPanel.ControlListener() {
            @Override
            public void onLoadROIsRequested() {
                LOGGER.debug("Load ROIs requested from controls panel");
            }
            
            @Override
            public void onClearROIsRequested() {
                datasetImageViewer.getROIOverlay().clear();
                LOGGER.debug("ROIs cleared from controls panel");
            }
            
            @Override
            public void onVisualControlsChanged(float borderWidth, float fillOpacity, boolean showNuclei, boolean showCells) {
                LOGGER.debug("Visual controls changed: border={}, opacity={}, nuclei={}, cells={}",
                           borderWidth, fillOpacity, showNuclei, showCells);
                // Update overlay visual settings
                if (datasetImageViewer != null && datasetImageViewer.getROIOverlay() != null) {
                    // Use the new 4-parameter method with separate nuclei/cells visibility
                    datasetImageViewer.getROIOverlay().setVisualControls(borderWidth, fillOpacity, showNuclei, showCells);
                }
            }
            
            @Override
            public void onSelectedClassChanged(String className) {
                datasetImageViewer.getROIOverlay().setSelectedClass(className);
                LOGGER.debug("Selected class changed to: {}", className);
            }
            
            @Override
            public void onClassAdded(String className, Color color) {
                // Add the new class to our local list
                if (!classNames.contains(className)) {
                    classNames.add(className);
                    LOGGER.info("Added new class '{}' with color {}", className, color);
                }
            }
        });
        LOGGER.debug("Event handlers setup complete");
    }

    /**
     * Load images from folder.
     */
    public void loadImagesFromFolder(File imageFolder) {
        this.selectedImageFolder = imageFolder;
        if (imageGallery != null) {
            imageGallery.loadImagesFromFolder(imageFolder);
            controlsPanel.updateStatus("Loaded images from: " + imageFolder.getName());
            LOGGER.info("Loaded images from folder: {}", imageFolder.getAbsolutePath());
        }
    }
    
    /**
     * Load image and ROIs using the new DatasetImageViewer.
     */
    private void loadImageWithROIs(File imageFile) {
        try {
            LOGGER.info("Loading image with streamlined system: {}", imageFile.getName());
            
            // Load image and ROIs using the new integrated viewer
            datasetImageViewer.loadImageWithROIs(imageFile, selectedRoiZip);
            
            controlsPanel.updateStatus("Loaded: " + imageFile.getName());
            LOGGER.info("Successfully loaded image: {}", imageFile.getName());
            
        } catch (Exception e) {
            String errorMsg = "Failed to load image: " + e.getMessage();
            controlsPanel.updateStatus(errorMsg);
            LOGGER.error("Failed to load image with ROIs: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sets the ROI ZIP file for the dataset.
     */
    public void setRoiZipFile(File roiZipFile) {
        this.selectedRoiZip = roiZipFile;
        controlsPanel.updateStatus("ROI ZIP file set: " + roiZipFile.getName());
        LOGGER.info("ROI ZIP file set: {}", roiZipFile.getAbsolutePath());
    }

    // ===== Getters =====

    public SimpleImageGallery getImageGallery() {
        return imageGallery;
    }

    public DatasetImageViewer getDatasetImageViewer() {
        return datasetImageViewer;
    }

    public DatasetControlsPanel getControlsPanel() {
        return controlsPanel;
    }

    public File getSelectedImageFile() {
        return imageGallery != null ? imageGallery.getSelectedImageFile() : null;
    }

    public String getSelectedClassName() {
        // Default to first class
        return classNames.isEmpty() ? "Unclassified" : classNames.get(0);
    }

    public int getROICountForCurrentImage() {
        return datasetImageViewer != null ? datasetImageViewer.getROIOverlay().getROICount() : 0;
    }

    // ===== Class Management =====

    public void addClass(String className) {
        if (className != null && !classNames.contains(className)) {
            classNames.add(className);
            LOGGER.info("Added class: {}", className);
        }
    }

    public void removeClass(String className) {
        if (className != null && !"Unclassified".equals(className)) {
            classNames.remove(className);
            LOGGER.info("Removed class: {}", className);
        }
    }

    public List<String> getClassNames() {
        return new ArrayList<>(classNames);
    }

    public void clearROIsForCurrentImage() {
        if (datasetImageViewer != null) {
            datasetImageViewer.getROIOverlay().clear();
            controlsPanel.updateStatus("Cleared ROIs for current image");
            LOGGER.info("Cleared ROIs for current image");
        }
    }
    
    /**
     * Cleanup resources when panel is disposed.
     */
    public void dispose() {
        if (datasetImageViewer != null) {
            datasetImageViewer.getROIOverlay().dispose();
        }
        LOGGER.debug("Disposed DatasetClassificationPanel");
    }
}