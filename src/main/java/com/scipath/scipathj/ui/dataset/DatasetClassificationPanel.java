package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.dataset.DatasetROILoader;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.common.MainImageViewer;
import com.scipath.scipathj.ui.common.ROIOverlay;
import com.scipath.scipathj.ui.common.ROIManager;
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
 * Zero-delay dataset classification panel with lightning-fast ROI loading.
 * Completely rewritten for maximum performance and responsiveness.
 */
public class DatasetClassificationPanel extends JPanel implements ROIOverlay.ROIOverlayListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetClassificationPanel.class);
    
    // Core components
    private SimpleImageGallery imageGallery;
    private MainImageViewer mainImageViewer;
    private DatasetClassManager classManager;
    private DatasetROILoader roiLoader;
    private ROIManager roiManager;
    private File selectedRoiZip;
    
    // High-performance threading
    private final Executor fastExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "FastROI-" + System.currentTimeMillis());
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });

    public DatasetClassificationPanel() {
        this.roiLoader = new DatasetROILoader();
        this.roiManager = ROIManager.getInstance();
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

        // Title
        add(UIUtils.createTitleLabel("Dataset Creation - Classification"), BorderLayout.NORTH);

        // Class management
        classManager = new DatasetClassManager();
        add(classManager, BorderLayout.NORTH);

        // Main content
        add(createMainContent(), BorderLayout.CENTER);

        // Footer
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createMainContent() {
        JPanel mainContent = new JPanel(new BorderLayout());

        // Image gallery
        imageGallery = new SimpleImageGallery();
        mainContent.add(imageGallery, BorderLayout.WEST);

        // Main image viewer
        mainImageViewer = new MainImageViewer();
        mainContent.add(mainImageViewer, BorderLayout.CENTER);

        // Setup ROI handling
        setupROIHandling();

        return mainContent;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING, 0, 0, 0));
        footer.setOpaque(false);

        String footerText = "Click on ROIs to assign them to the selected class • Use class manager to add/remove classes";
        JLabel footerLabel = new JLabel(footerText);
        footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE));
        footerLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        footerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        footer.add(footerLabel, BorderLayout.CENTER);
        return footer;
    }

    private void setupROIHandling() {
        if (mainImageViewer != null) {
            mainImageViewer.addROIOverlayListener(this);
        }
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
                if (selectedImage != null && mainImageViewer != null) {
                    instantLoadImageAndROIs(selectedImage);
                }
            });
        }
    }

    /**
     * INSTANT: Simultaneous image and ROI loading with zero coordination delay.
     */
    private void instantLoadImageAndROIs(File imageFile) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("INSTANT LOAD START: {}", imageFile.getName());

        // Start image loading immediately
        mainImageViewer.displayImage(imageFile);

        // Start ROI loading in parallel with zero delay
        if (selectedRoiZip != null) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    LOGGER.debug("ROI loading started after {}ms", System.currentTimeMillis() - startTime);
                    return roiLoader.loadROIsForImage(selectedRoiZip, imageFile.getName());
                } catch (Exception e) {
                    LOGGER.error("ROI loading failed: {}", e.getMessage());
                    return List.<UserROI>of();
                }
            }, fastExecutor).thenAccept(rois -> {
                long loadTime = System.currentTimeMillis() - startTime;
                LOGGER.debug("ROI loading completed in {}ms, applying {} ROIs", loadTime, rois.size());
                
                // Apply ROIs immediately on EDT
                SwingUtilities.invokeLater(() -> {
                    // Clear existing ROIs
                    roiLoader.clearROIsForImage(imageFile.getName());
                    
                    // Add new ROIs instantly
                    for (UserROI roi : rois) {
                        roiManager.addROI(roi);
                    }
                    
                    long totalTime = System.currentTimeMillis() - startTime;
                    LOGGER.info("INSTANT LOAD COMPLETE: {} ROIs in {}ms for '{}'", 
                              rois.size(), totalTime, imageFile.getName());
                });
            });
        }
    }

    /**
     * Sets the ROI ZIP file for the dataset.
     */
    public void setRoiZipFile(File roiZipFile) {
        this.selectedRoiZip = roiZipFile;
        LOGGER.info("ROI ZIP file set: {}", roiZipFile.getAbsolutePath());
        
        // Clear any existing cache
        if (roiLoader != null) {
            roiLoader.clearCache();
        }
    }

    // ===== Getters =====

    public SimpleImageGallery getImageGallery() {
        return imageGallery;
    }

    public MainImageViewer getMainImageViewer() {
        return mainImageViewer;
    }

    public DatasetClassManager getClassManager() {
        return classManager;
    }

    public File getSelectedImageFile() {
        return imageGallery != null ? imageGallery.getSelectedImageFile() : null;
    }

    public String getSelectedClassName() {
        return classManager != null ? classManager.getSelectedClassName() : null;
    }

    public int getROICountForCurrentImage() {
        File currentImage = getSelectedImageFile();
        if (currentImage != null && roiLoader != null) {
            return roiLoader.getROICount(currentImage.getName());
        }
        return 0;
    }

    // ===== Class Management =====

    public void addClass(String className) {
        if (classManager != null) {
            classManager.addClass(className);
        }
    }

    public void removeClass(String className) {
        if (classManager != null) {
            classManager.removeClass(className);
        }
    }

    public java.util.List<String> getClassNames() {
        return classManager != null ? classManager.getClassNames() : java.util.Collections.emptyList();
    }

    public void clearROIsForCurrentImage() {
        File currentImage = getSelectedImageFile();
        if (currentImage != null && roiLoader != null) {
            roiLoader.clearROIsForImage(currentImage.getName());
            LOGGER.info("Cleared ROIs for image '{}'", currentImage.getName());
        }
    }

    // ===== ROI Overlay Listener Implementation =====

    @Override
    public void onROICreated(UserROI roi) {
        // Not used in dataset classification mode
    }

    @Override
    public void onROISelected(UserROI roi) {
        handleROISelection(roi);
    }

    @Override
    public void onROIDeselected() {
        // Not used in dataset classification mode
    }

    /**
     * Handles ROI selection for class assignment.
     */
    private void handleROISelection(UserROI selectedROI) {
        if (selectedROI == null || classManager == null) {
            return;
        }

        String selectedClass = classManager.getSelectedClassName();
        if (selectedClass == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a class first before clicking on ROIs.",
                "No Class Selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show confirmation dialog
        int result = JOptionPane.showConfirmDialog(this,
            String.format("Assign ROI '%s' to class '%s'?", selectedROI.getName(), selectedClass),
            "Assign ROI to Class",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            assignROIToClass(selectedROI, selectedClass);
            
            JOptionPane.showMessageDialog(this,
                String.format("ROI '%s' assigned to class '%s'", selectedROI.getName(), selectedClass),
                "ROI Assigned",
                JOptionPane.INFORMATION_MESSAGE);
            
            LOGGER.info("Assigned ROI '{}' to class '{}'", selectedROI.getName(), selectedClass);
        }
    }

    /**
     * Assigns a ROI to a specific class.
     */
    private void assignROIToClass(UserROI roi, String className) {
        // For now, just log the assignment
        // TODO: Implement proper data structure for export
        LOGGER.info("ROI '{}' (ID: {}) assigned to class '{}'", roi.getName(), roi.getId(), className);
    }
}