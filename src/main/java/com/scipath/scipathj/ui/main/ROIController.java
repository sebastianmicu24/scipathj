package com.scipath.scipathj.ui.main;

import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.common.ROIManager;
import com.scipath.scipathj.ui.analysis.components.ROIToolbar;
import com.scipath.scipathj.ui.common.MainImageViewer;
import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.analysis.dialogs.ROIStatisticsDialog;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for ROI-related operations and business logic.
 *
 * <p>This class handles ROI management operations including saving/loading ROIs,
 * statistics calculation, filtering, and coordination between UI components
 * and the ROI manager.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ROIController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIController.class);

  private final ROIManager roiManager;
  private final java.awt.Window parentWindow;

  private ROIToolbar roiToolbar;
  private MainImageViewer mainImageViewer;
  private SimpleImageGallery imageGallery;

  /**
   * Creates a new ROIController instance.
   *
   * @param roiManager the ROI manager instance
   * @param parentWindow the parent window for dialogs
   */
  public ROIController(ROIManager roiManager, Window parentWindow) {
    this.roiManager = roiManager;
    this.parentWindow = parentWindow;
  }

  /**
   * Sets the UI components for ROI operations.
   *
   * @param roiToolbar the ROI toolbar
   * @param mainImageViewer the main image viewer
   * @param imageGallery the image gallery
   */
  public void setUIComponents(ROIToolbar roiToolbar, MainImageViewer mainImageViewer, SimpleImageGallery imageGallery) {
    this.roiToolbar = roiToolbar;
    this.mainImageViewer = mainImageViewer;
    this.imageGallery = imageGallery;

    setupEventHandlers();
  }

  /**
   * Sets up event handlers for ROI operations.
   */
  private void setupEventHandlers() {
    if (roiToolbar != null) {
      roiToolbar.addROIToolbarListener(new ROIToolbar.ROIToolbarListener() {
        @Override
        public void onROICreationModeChanged(UserROI.ROIType type, boolean enabled) {
          if (mainImageViewer != null) {
            mainImageViewer.setROICreationMode(type);
          }
          LOGGER.debug("ROI creation mode changed: {} (enabled: {})", type, enabled);
        }

        @Override
        public void onSaveROIs(String imageFileName, File outputFile) {
          saveROIs(imageFileName, outputFile);
        }

        @Override
        public void onSaveAllROIs(File outputFile) {
          saveAllROIs(outputFile);
        }

        @Override
        public void onClearAllROIs() {
          String currentImageName = getCurrentImageName();
          if (currentImageName != null) {
            roiManager.clearROIsForImage(currentImageName);
            updateROIToolbarState();
            LOGGER.info("Cleared all ROIs for image: {}", currentImageName);
          }
        }

        @Override
        public void onROIFilterChanged(MainSettings.ROICategory category, boolean enabled) {
          LOGGER.debug("ROI filter changed: {} -> {}", category, enabled);
          if (mainImageViewer != null && mainImageViewer.getROIOverlay() != null) {
            mainImageViewer.getROIOverlay().setFilterState(category, enabled);
            mainImageViewer.getROIOverlay().repaint();
          }
        }

        @Override
        public void onShowROIStatistics() {
          showROIStatistics();
        }

        @Override
        public void onShowFeatures() {
          // This will be handled by the AnalysisController
          LOGGER.debug("Features dialog requested from ROI toolbar");
        }

        @Override
        public void onChangeROIType(String imageFileName, UserROI.ROIType newType) {
          LOGGER.debug("ROI type change requested for image {} to type {}", imageFileName, newType);
        }
      });
    }

    // Set up ROI manager listeners
    roiManager.addROIChangeListener(new ROIManager.ROIChangeListener() {
      @Override
      public void onROIAdded(UserROI roi) {
        updateROIToolbarState();
      }

      @Override
      public void onROIRemoved(UserROI roi) {
        updateROIToolbarState();
      }

      @Override
      public void onROIUpdated(UserROI roi) {
        updateROIToolbarState();
      }

      @Override
      public void onROIsCleared(String imageFileName) {
        updateROIToolbarState();
      }
    });

    // Update ROI toolbar when image selection changes
    if (imageGallery != null) {
      imageGallery.setSelectionChangeListener(e -> {
        File selectedImageFile = imageGallery.getSelectedImageFile();
        if (selectedImageFile != null) {
          if (mainImageViewer != null) {
            mainImageViewer.displayImage(selectedImageFile);
          }
          if (roiToolbar != null) {
            roiToolbar.setCurrentImage(selectedImageFile.getName());
          }
          updateROIToolbarState();
          LOGGER.debug("Selected image: {}", selectedImageFile.getName());
        }
      });
    }
  }

  /**
   * Saves ROIs for a specific image.
   */
  private void saveROIs(String imageFileName, File outputFile) {
    try {
      roiManager.saveROIsToFile(imageFileName, outputFile);
      JOptionPane.showMessageDialog(
          parentWindow,
          "ROIs saved successfully to:\n" + outputFile.getAbsolutePath(),
          "Save ROIs",
          JOptionPane.INFORMATION_MESSAGE);
      LOGGER.info("Successfully saved ROIs to file: {}", outputFile.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.error("Error saving ROIs to file: {}", outputFile.getAbsolutePath(), e);
      JOptionPane.showMessageDialog(
          parentWindow,
          "Error saving ROIs:\n" + e.getMessage(),
          "Save ROIs Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Saves all ROIs to a master ZIP file.
   */
  private void saveAllROIs(File outputFile) {
    try {
      roiManager.saveAllROIsToMasterZip(outputFile);
      JOptionPane.showMessageDialog(
          parentWindow,
          "All ROIs saved successfully to master ZIP:\n" + outputFile.getAbsolutePath(),
          "Save All ROIs",
          JOptionPane.INFORMATION_MESSAGE);
      LOGGER.info("Successfully saved all ROIs to master ZIP file: {}", outputFile.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.error("Error saving all ROIs to master ZIP file: {}", outputFile.getAbsolutePath(), e);
      JOptionPane.showMessageDialog(
          parentWindow,
          "Error saving all ROIs:\n" + e.getMessage(),
          "Save All ROIs Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Shows ROI statistics dialog.
   */
  private void showROIStatistics() {
    Map<String, List<UserROI>> allROIs = roiManager.getAllROIsByImage();
    // Note: We'll need MainSettings passed in - this is a simplified version
    MainSettings currentSettings = null; // TODO: Get from configuration manager

    ROIStatisticsDialog dialog = new ROIStatisticsDialog((java.awt.Frame) parentWindow, allROIs, currentSettings);
    dialog.setVisible(true);

    LOGGER.info("Showing ROI statistics dialog with {} images", allROIs.size());
  }

  /**
   * Updates the ROI toolbar state based on current image and ROI count.
   */
  private void updateROIToolbarState() {
    if (roiToolbar != null) {
      String currentImageName = getCurrentImageName();
      if (currentImageName != null) {
        int totalCount = roiManager.getROICount(currentImageName);
        roiToolbar.updateROICount(totalCount);

        // Update type counts
        Map<MainSettings.ROICategory, Integer> typeCounts = getROICountsByType();
        roiToolbar.updateROITypeCounts(typeCounts);
      } else {
        roiToolbar.updateROICount(0);
      }
    }
  }

  /**
   * Gets the name of the currently displayed image.
   */
  private String getCurrentImageName() {
    if (mainImageViewer != null && mainImageViewer.getCurrentImageFile() != null) {
      return mainImageViewer.getCurrentImageFile().getName();
    }
    return null;
  }

  /**
   * Gets ROI counts by type for all images.
   */
  private Map<MainSettings.ROICategory, Integer> getROICountsByType() {
    Map<MainSettings.ROICategory, Integer> counts = new java.util.HashMap<>();

    // Initialize counts
    for (MainSettings.ROICategory category : MainSettings.ROICategory.values()) {
      counts.put(category, 0);
    }

    // Count ROIs by type across all images
    Map<String, List<UserROI>> allROIs = roiManager.getAllROIsByImage();
    int totalROIs = 0;
    int ignoredROIs = 0;

    for (List<UserROI> roiList : allROIs.values()) {
      for (UserROI roi : roiList) {
        if (roi.isIgnored()) {
          ignoredROIs++;
        } else {
          MainSettings.ROICategory category = determineROICategory(roi);
          counts.put(category, counts.get(category) + 1);
        }
        totalROIs++;
      }
    }

    // Store ignored ROI count
    counts.put(null, ignoredROIs); // Use null key for ignored ROIs

    return counts;
  }

  /**
   * Maps ROI to category based on its type and class.
   */
  private MainSettings.ROICategory determineROICategory(UserROI roi) {
    // First check the class type - this is more reliable than ROI type
    if (roi instanceof com.scipath.scipathj.infrastructure.roi.NucleusROI) {
      return MainSettings.ROICategory.NUCLEUS;
    }

    // Then check the ROI type
    UserROI.ROIType roiType = roi.getType();
    switch (roiType) {
      case VESSEL:
      case COMPLEX_SHAPE:
        return MainSettings.ROICategory.VESSEL;
      case NUCLEUS:
        return MainSettings.ROICategory.NUCLEUS;
      case CYTOPLASM:
        return MainSettings.ROICategory.CYTOPLASM;
      case CELL:
        return MainSettings.ROICategory.CELL;
      default:
        // Check name-based heuristics as fallback
        String name = roi.getName().toLowerCase();
        if (name.contains("vessel")) {
          return MainSettings.ROICategory.VESSEL;
        } else if (name.contains("nucleus") || name.contains("nuclei")) {
          return MainSettings.ROICategory.NUCLEUS;
        } else if (name.contains("cytoplasm") || name.contains("cyto")) {
          return MainSettings.ROICategory.CYTOPLASM;
        } else if (name.contains("cell")) {
          return MainSettings.ROICategory.CELL;
        }
        return MainSettings.ROICategory.VESSEL; // Default fallback
    }
  }

  /**
   * Initializes the ROI toolbar with current settings.
   */
  public void initializeROIToolbar(MainSettings currentSettings) {
    if (roiToolbar != null && currentSettings != null) {
      roiToolbar.setMainSettings(currentSettings);
      updateROIToolbarState();
    }
  }
}