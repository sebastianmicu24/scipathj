package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.ui.dataset.DatasetROIManager;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.dataset.DatasetImageViewer;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced dataset classification panel with Dataset Creator 2.0 integration.
 * Features interactive ROI overlay, class management, and visual controls.
 */
public class DatasetClassificationPanel extends JPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetClassificationPanel.class);
    
    // Core components
    private SimpleImageGallery imageGallery;
    private DatasetImageViewer datasetImageViewer;
    private DatasetControlPanel controlPanel;
    private DatasetROIManager datasetROIManager;
    private File selectedRoiZip;
    
    // High-performance threading
    private final Executor fastExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "FastROI-" + System.currentTimeMillis());
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });

    public DatasetClassificationPanel() {
        this.datasetROIManager = new DatasetROIManager();
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

        // Title
        add(UIUtils.createTitleLabel("Dataset Creation - Classification"), BorderLayout.NORTH);

        // Enhanced control panel with Dataset Creator 2.0 features
        controlPanel = new DatasetControlPanel(datasetROIManager);
        
        // Main content with controls on the right
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createMainContent(), BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);

        // Footer
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createMainContent() {
        JPanel mainContent = new JPanel(new BorderLayout());

        // Image gallery
        imageGallery = new SimpleImageGallery();
        mainContent.add(imageGallery, BorderLayout.WEST);

        // Dataset-specific image viewer with native ROI integration
        datasetImageViewer = new DatasetImageViewer(datasetROIManager, MainSettings.createDefault());
        mainContent.add(datasetImageViewer, BorderLayout.CENTER);

        return mainContent;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING, 0, 0, 0));
        footer.setOpaque(false);

        String footerText = "Click on ROIs to assign them to the selected class • Use E key to toggle outlines • Use controls to adjust appearance";
        JLabel footerLabel = new JLabel(footerText);
        footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE));
        footerLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        footerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        footer.add(footerLabel, BorderLayout.CENTER);
        return footer;
    }


    /**
     * ZERO-DELAY: Loads images and sets up instant ROI loading.
     */
    public void loadImagesFromFolder(File imageFolder) {
        if (imageGallery != null) {
            imageGallery.loadImagesFromFolder(imageFolder);

            // Setup lightning-fast image selection
            imageGallery.setSelectionChangeListener(e -> {
                File selectedImage = imageGallery.getSelectedImageFile();
                if (selectedImage != null && datasetImageViewer != null) {
                    instantLoadImageAndROIs(selectedImage);
                }
            });
        }
    }
    /**
     * INSTANT: Simultaneous image and ROI loading with enhanced overlay.
     */
    private void instantLoadImageAndROIs(File imageFile) {
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                SwingUtilities.invokeLater(() -> {
                    if (datasetImageViewer != null) {
                        try {
                            // Load image and ROIs together using DatasetImageViewer
                            LOGGER.debug("Image and ROI loading started");
                            datasetImageViewer.loadImageWithROIs(imageFile, selectedRoiZip);
                            
                            LOGGER.debug("Loading completed in {}ms", System.currentTimeMillis() - startTime);
                            
                        } catch (Exception e) {
                            LOGGER.error("High-speed loading failed for {}: {}", imageFile.getName(), e.getMessage(), e);
                        }
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Background loading failed for {}: {}", imageFile.getName(), e.getMessage(), e);
            }
        }, fastExecutor);
    }

    /**
     * Sets the ROI ZIP file for the dataset.
     */
    public void setRoiZipFile(File roiZipFile) {
        this.selectedRoiZip = roiZipFile;
        LOGGER.info("ROI ZIP file set: {}", roiZipFile.getAbsolutePath());
        
        // Clear any existing cache
        if (datasetROIManager != null) {
            datasetROIManager.clearAllROIs();
        }
    }

    // ===== Getters =====

    public SimpleImageGallery getImageGallery() {
        return imageGallery;
    }

    public DatasetImageViewer getDatasetImageViewer() {
        return datasetImageViewer;
    }

    public DatasetControlPanel getControlPanel() {
        return controlPanel;
    }

    public File getSelectedImageFile() {
        return imageGallery != null ? imageGallery.getSelectedImageFile() : null;
    }

    public String getSelectedClassName() {
        return datasetROIManager != null ? datasetROIManager.getSelectedClass() : "Unclassified";
    }

    public int getROICountForCurrentImage() {
        File currentImage = getSelectedImageFile();
        if (currentImage != null && datasetROIManager != null) {
            return datasetROIManager.getROICount(currentImage.getName());
        }
        return 0;
    }

    // ===== Class Management (delegated to DatasetROIManager) =====

    public void addClass(String className) {
        if (datasetROIManager != null) {
            datasetROIManager.createClass(className, java.awt.Color.RED);
        }
    }

    public void removeClass(String className) {
        if (datasetROIManager != null) {
            datasetROIManager.removeClass(className);
        }
    }

    public java.util.List<String> getClassNames() {
        return datasetROIManager != null ? 
            java.util.List.copyOf(datasetROIManager.getAllAvailableClasses()) : 
            java.util.Collections.emptyList();
    }

    public void clearROIsForCurrentImage() {
        File currentImage = getSelectedImageFile();
        if (currentImage != null && datasetROIManager != null) {
            datasetROIManager.clearROIsForImage(currentImage.getName());
            if (datasetImageViewer != null && datasetImageViewer.getDatasetROIOverlay() != null) {
                // Update overlay to show no ROIs - use new overlay system
                datasetImageViewer.getDatasetROIOverlay().setRois(null);
                datasetImageViewer.getDatasetROIOverlay().repaint();
            }
            LOGGER.info("Cleared ROIs for image '{}'", currentImage.getName());
        }
    }
    
    /**
     * Cleanup resources when panel is disposed.
     */
    public void dispose() {
        if (controlPanel != null) {
            controlPanel.dispose();
        }
        LOGGER.debug("Disposed DatasetClassificationPanel");
    }
}