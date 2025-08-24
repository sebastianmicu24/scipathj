package com.scipath.scipathj.ui.controllers;

import com.scipath.scipathj.ui.analysis.components.FolderSelectionPanel;
import com.scipath.scipathj.ui.common.MainImageViewer;
import com.scipath.scipathj.ui.analysis.components.PipelineRecapPanel;
import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.common.StatusPanel;
import com.scipath.scipathj.ui.model.PipelineInfo;
import java.awt.*;
import java.io.File;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for managing navigation between different UI states.
 *
 * <p>This class handles the navigation logic between pipeline selection,
 * folder selection, and image gallery views, managing the card layout
 * and updating UI components accordingly.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class NavigationController {

  private static final Logger LOGGER = LoggerFactory.getLogger(NavigationController.class);

  /**
   * UI State enumeration.
   */
  public enum UIState {
    MAIN_MENU,
    FOLDER_SELECTION,
    IMAGE_GALLERY,
    DATASET_CREATION,
    RESULTS_VISUALIZATION
  }

  private final CardLayout cardLayout;
  private final JPanel mainContentPanel;
  private final StatusPanel statusPanel;
  private final PipelineRecapPanel pipelineRecapPanel;
  private final FolderSelectionPanel folderSelectionPanel;
  private final SimpleImageGallery imageGallery;
  private final MainImageViewer mainImageViewer;

  private UIState currentState = UIState.MAIN_MENU;
  private PipelineInfo selectedPipeline;
  private File selectedFolder;
  private File selectedFile;

  // Callback for updating start button state
  private Runnable startButtonStateUpdater;

  /**
   * Creates a new NavigationController instance.
   *
   * @param cardLayout the card layout for switching panels
   * @param mainContentPanel the main content panel
   * @param statusPanel the status panel
   * @param pipelineRecapPanel the pipeline recap panel
   * @param folderSelectionPanel the folder selection panel
   * @param imageGallery the image gallery
   * @param mainImageViewer the main image viewer
   */
  public NavigationController(
      CardLayout cardLayout,
      JPanel mainContentPanel,
      StatusPanel statusPanel,
      PipelineRecapPanel pipelineRecapPanel,
      FolderSelectionPanel folderSelectionPanel,
      SimpleImageGallery imageGallery,
      MainImageViewer mainImageViewer) {
    this.cardLayout = cardLayout;
    this.mainContentPanel = mainContentPanel;
    this.statusPanel = statusPanel;
    this.pipelineRecapPanel = pipelineRecapPanel;
    this.folderSelectionPanel = folderSelectionPanel;
    this.imageGallery = imageGallery;
    this.mainImageViewer = mainImageViewer;

    LOGGER.debug("Navigation controller created");
  }

  /**
   * Sets the callback for updating start button state.
   *
   * @param startButtonStateUpdater the callback to update start button state
   */
  public void setStartButtonStateUpdater(Runnable startButtonStateUpdater) {
    this.startButtonStateUpdater = startButtonStateUpdater;
  }

  /**
   * Switches to the analysis setup screen.
   *
   * @param pipeline the selected pipeline
   */
  public void switchToAnalysisSetup(PipelineInfo pipeline) {
    this.selectedPipeline = pipeline;
    currentState = UIState.FOLDER_SELECTION;
    cardLayout.show(mainContentPanel, UIState.FOLDER_SELECTION.name());

    // Update pipeline recap
    pipelineRecapPanel.setPipeline(selectedPipeline);

    // Show back button
    statusPanel.showBackButton();

    // Hide analysis buttons (not available in folder selection)
    statusPanel.hideAnalysisButtons();

    // Update status
    statusPanel.setStatus("Select a folder containing images");

    // Update start button state
    updateStartButtonState();

    LOGGER.info("Switched to analysis setup for pipeline: {}", pipeline.getDisplayName());
  }

  /**
   * Switches to the image gallery view.
   *
   * @param folder the selected folder (or parent folder if a single file was selected)
   * @param selectedFile optional specific file to highlight (if single file was selected)
   */
  public void switchToImageGallery(File folder, File selectedFile) {
    this.selectedFolder = folder;
    this.selectedFile = selectedFile;
    currentState = UIState.IMAGE_GALLERY;
    cardLayout.show(mainContentPanel, UIState.IMAGE_GALLERY.name());

    // Load images into gallery - if single file selected, show only that file
    if (selectedFile != null) {
      // For single file selection, create an array with just that file
      imageGallery.loadImagesFromFolder(selectedFolder, selectedFile);
    } else {
      // For folder selection, show all images in folder
      imageGallery.loadImagesFromFolder(selectedFolder);
    }

    // Show analysis buttons in status panel
    statusPanel.showAnalysisButtons();

    // Update status
    statusPanel.setStatus("Select images for analysis");

    // Update start button state
    updateStartButtonState();

    LOGGER.info("Switched to image gallery view with folder: {}", selectedFolder.getAbsolutePath());
  }

  /**
   * Switches to the image gallery view with a folder.
   *
   * @param folder the selected folder
   */
  public void switchToImageGallery(File folder) {
    switchToImageGallery(folder, null);
  }

  /**
   * Switches back to folder selection from image gallery.
   */
  public void switchToFolderSelection() {
    currentState = UIState.FOLDER_SELECTION;
    cardLayout.show(mainContentPanel, UIState.FOLDER_SELECTION.name());

    // Clear image viewer
    mainImageViewer.clearImage();

    // Hide analysis buttons (not available in folder selection)
    statusPanel.hideAnalysisButtons();

    // Update status
    statusPanel.setStatus("Select a folder containing images");

    // Update start button state
    updateStartButtonState();

    LOGGER.info("Switched back to folder selection");
  }

  /**
   * Switches to the main menu from folder selection.
   */
  public void switchToMainMenuFromFolder() {
    currentState = UIState.MAIN_MENU;
    cardLayout.show(mainContentPanel, UIState.MAIN_MENU.name());

    // Hide back button
    statusPanel.hideBackButton();

    // Hide analysis buttons
    statusPanel.hideAnalysisButtons();

    // Update status
    statusPanel.setStatus("Select an option to begin");

    // Clear selections
    selectedPipeline = null;
    selectedFolder = null;
    folderSelectionPanel.clearSelection();

    // Update start button state
    updateStartButtonState();

    LOGGER.info("Switched to main menu from folder selection");
  }

  /**
   * Switches to the dataset creation panel.
   */
  public void switchToDatasetCreation() {
    currentState = UIState.DATASET_CREATION;
    cardLayout.show(mainContentPanel, UIState.DATASET_CREATION.name());

    // Show back button
    statusPanel.showBackButton();

    // Hide analysis buttons (not available in dataset creation)
    statusPanel.hideAnalysisButtons();

    // Update status
    statusPanel.setStatus("Dataset creation tools");

    // Update start button state
    updateStartButtonState();

    LOGGER.info("Switched to dataset creation panel");
  }

  /**
   * Switches to the results visualization panel.
   */
  public void switchToResultsVisualization() {
    currentState = UIState.RESULTS_VISUALIZATION;
    cardLayout.show(mainContentPanel, UIState.RESULTS_VISUALIZATION.name());

    // Show back button
    statusPanel.showBackButton();

    // Hide analysis buttons (not available in results visualization)
    statusPanel.hideAnalysisButtons();

    // Update status
    statusPanel.setStatus("Results visualization tools");

    // Update start button state
    updateStartButtonState();

    LOGGER.info("Switched to results visualization panel");
  }

  /**
   * Switches back to the main menu screen.
   */
  public void switchToMainMenu() {
    currentState = UIState.MAIN_MENU;
    cardLayout.show(mainContentPanel, UIState.MAIN_MENU.name());

    // Hide back button
    statusPanel.hideBackButton();

    // Hide analysis buttons
    statusPanel.hideAnalysisButtons();

    // Update status
    statusPanel.setStatus("Select an option to begin");

    // Clear selections
    selectedPipeline = null;
    selectedFolder = null;
    folderSelectionPanel.clearSelection();
    mainImageViewer.clearImage();

    // Update start button state
    updateStartButtonState();

    LOGGER.info("Switched back to main menu");
  }

  /**
   * Updates the start button state based on current selections.
   */
  private void updateStartButtonState() {
    if (startButtonStateUpdater != null) {
      startButtonStateUpdater.run();
    }
  }

  /**
   * Checks if analysis can be started based on current state and selections.
   *
   * @return true if analysis can be started, false otherwise
   */
  public boolean canStartAnalysis() {
    return (currentState == UIState.FOLDER_SELECTION || currentState == UIState.IMAGE_GALLERY)
        && selectedPipeline != null
        && selectedFolder != null
        && folderSelectionPanel.hasSelection();
  }

  /**
   * Gets the current UI state.
   *
   * @return the current UI state
   */
  public UIState getCurrentState() {
    return currentState;
  }

  /**
   * Gets the selected pipeline.
   *
   * @return the selected pipeline
   */
  public PipelineInfo getSelectedPipeline() {
    return selectedPipeline;
  }

  /**
   * Gets the selected folder.
   *
   * @return the selected folder
   */
  public File getSelectedFolder() {
    return selectedFolder;
  }

  /**
   * Sets the selected folder.
   *
   * @param folder the selected folder
   */
  public void setSelectedFolder(File folder) {
    this.selectedFolder = folder;
    updateStartButtonState();
  }

  /**
   * Gets the image count for analysis.
   *
   * @return the number of images available for analysis
   */
  public int getImageCount() {
    return currentState == UIState.IMAGE_GALLERY ? imageGallery.getImageCount() : 0;
  }
}
