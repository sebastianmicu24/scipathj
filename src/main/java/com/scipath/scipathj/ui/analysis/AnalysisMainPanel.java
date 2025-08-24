package com.scipath.scipathj.ui.analysis;

import com.scipath.scipathj.ui.common.MainImageViewer;
import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.common.StatusPanel;
import com.scipath.scipathj.ui.analysis.components.PipelineRecapPanel;
import com.scipath.scipathj.ui.analysis.components.ROIToolbar;
import com.scipath.scipathj.ui.controllers.AnalysisController;
import com.scipath.scipathj.ui.model.PipelineInfo;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.io.File;
import javax.swing.*;

/**
 * Main panel for analysis functionality.
 *
 * <p>This panel provides the primary interface for performing image analysis operations,
 * including pipeline selection, image gallery navigation, and ROI management during analysis.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnalysisMainPanel extends JPanel {

  private final PipelineRecapPanel pipelineRecapPanel;
  private final SimpleImageGallery imageGallery;
  private final MainImageViewer mainImageViewer;
  private final ROIToolbar roiToolbar;
  private final AnalysisController analysisController;

  private JButton changeFolderButton;
  private JButton mainSettingsButton;
  private JButton displaySettingsButton;

  /**
   * Creates a new AnalysisMainPanel instance.
   *
   * @param analysisController the analysis controller for handling analysis operations
   * @param pipelineRecapPanel the pipeline recap panel
   * @param imageGallery the image gallery component
   * @param mainImageViewer the main image viewer component
   * @param roiToolbar the ROI toolbar component
   */
  public AnalysisMainPanel(
      AnalysisController analysisController,
      PipelineRecapPanel pipelineRecapPanel,
      SimpleImageGallery imageGallery,
      MainImageViewer mainImageViewer,
      ROIToolbar roiToolbar) {
    this.analysisController = analysisController;
    this.pipelineRecapPanel = pipelineRecapPanel;
    this.imageGallery = imageGallery;
    this.mainImageViewer = mainImageViewer;
    this.roiToolbar = roiToolbar;

    initializeComponents();
    setupLayout();
    setupEventHandlers();
  }

  /**
   * Initializes the panel components.
   */
  private void initializeComponents() {
    // Create control buttons
    changeFolderButton = UIUtils.createSmallButton("Change Folder", null);
    mainSettingsButton = UIUtils.createSmallButton("Main Settings", null);
    displaySettingsButton = UIUtils.createSmallButton("Display Settings", null);

    // Set preferred sizes for buttons
    Dimension buttonSize = new Dimension(180, 32);
    changeFolderButton.setPreferredSize(buttonSize);
    mainSettingsButton.setPreferredSize(buttonSize);
    displaySettingsButton.setPreferredSize(buttonSize);
  }

  /**
   * Sets up the panel layout.
   */
  private void setupLayout() {
    setLayout(new BorderLayout());

    // Top panel with pipeline recap and buttons
    add(createTopPanel(), BorderLayout.NORTH);

    // Main content area with gallery and image viewer
    add(createMainContent(), BorderLayout.CENTER);

    // ROI toolbar at the bottom
    add(roiToolbar, BorderLayout.SOUTH);
  }

  /**
   * Creates the top panel with pipeline recap and control buttons.
   */
  private JPanel createTopPanel() {
    JPanel topPanel = new JPanel(new BorderLayout());

    // Button panel on the right
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.MEDIUM_SPACING, UIConstants.SMALL_SPACING));
    buttonPanel.setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING, UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING));
    buttonPanel.add(displaySettingsButton);
    buttonPanel.add(mainSettingsButton);
    buttonPanel.add(changeFolderButton);
    topPanel.add(buttonPanel, BorderLayout.EAST);

    // Pipeline recap panel in the center
    JPanel recapWrapper = new JPanel(new BorderLayout());
    recapWrapper.setBorder(UIUtils.createPadding(0, 0, UIConstants.SMALL_SPACING, 0));
    recapWrapper.add(pipelineRecapPanel, BorderLayout.CENTER);
    topPanel.add(recapWrapper, BorderLayout.CENTER);

    return topPanel;
  }

  /**
   * Creates the main content area with image gallery and viewer.
   */
  private JPanel createMainContent() {
    JPanel mainContent = new JPanel(new BorderLayout());

    // Image gallery on the left
    imageGallery.setBorder(BorderFactory.createEtchedBorder());
    mainContent.add(imageGallery, BorderLayout.WEST);

    // Main image viewer on the right
    mainImageViewer.setBorder(BorderFactory.createEtchedBorder());
    mainContent.add(mainImageViewer, BorderLayout.CENTER);

    return mainContent;
  }

  /**
   * Sets up event handlers for the panel components.
   */
  private void setupEventHandlers() {
    // Image gallery selection handler
    imageGallery.setSelectionChangeListener(e -> {
      File selectedImageFile = imageGallery.getSelectedImageFile();
      if (selectedImageFile != null) {
        mainImageViewer.displayImage(selectedImageFile);
        roiToolbar.setCurrentImage(selectedImageFile.getName());
      }
    });
  }

  /**
   * Sets the selected pipeline for this analysis session.
   *
   * @param pipeline the selected pipeline
   */
  public void setPipeline(PipelineInfo pipeline) {
    pipelineRecapPanel.setPipeline(pipeline);
  }

  /**
   * Loads images from the specified folder into the gallery.
   *
   * @param folder the folder containing images
   */
  public void loadImagesFromFolder(File folder) {
    imageGallery.loadImagesFromFolder(folder);
  }

  /**
   * Loads images from the specified folder and highlights a specific file.
   *
   * @param folder the folder containing images
   * @param selectedFile the file to highlight
   */
  public void loadImagesFromFolder(File folder, File selectedFile) {
    imageGallery.loadImagesFromFolder(folder, selectedFile);
  }

  /**
   * Gets the number of images currently loaded in the gallery.
   *
   * @return the image count
   */
  public int getImageCount() {
    return imageGallery.getImageCount();
  }

  /**
   * Gets the currently selected image file.
   *
   * @return the selected image file, or null if none selected
   */
  public File getSelectedImageFile() {
    return imageGallery.getSelectedImageFile();
  }

  /**
   * Clears the current image from the viewer.
   */
  public void clearImage() {
    mainImageViewer.clearImage();
  }

  /**
   * Sets the change folder button listener.
   *
   * @param listener the action listener
   */
  public void setChangeFolderListener(java.awt.event.ActionListener listener) {
    changeFolderButton.addActionListener(listener);
  }

  /**
   * Sets the main settings button listener.
   *
   * @param listener the action listener
   */
  public void setMainSettingsListener(java.awt.event.ActionListener listener) {
    mainSettingsButton.addActionListener(listener);
  }

  /**
   * Sets the display settings button listener.
   *
   * @param listener the action listener
   */
  public void setDisplaySettingsListener(java.awt.event.ActionListener listener) {
    displaySettingsButton.addActionListener(listener);
  }

  /**
   * Updates the ROI toolbar state.
   */
  public void updateROIToolbarState() {
    String currentImageName = getCurrentImageName();
    if (currentImageName != null) {
      roiToolbar.setCurrentImage(currentImageName);
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
}