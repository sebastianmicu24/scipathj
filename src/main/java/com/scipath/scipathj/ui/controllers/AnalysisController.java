package com.scipath.scipathj.ui.controllers;

import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.engine.SciPathJEngine;
import com.scipath.scipathj.ui.common.StatusPanel;
import com.scipath.scipathj.ui.analysis.dialogs.FeatureDisplayDialog;
import com.scipath.scipathj.ui.model.PipelineInfo;
import java.awt.*;
import java.io.File;
import java.util.Map;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for coordinating analysis operations.
 *
 * <p>This controller coordinates between the UI components and the analysis execution
 * controller. It handles UI updates, feature management, and user interactions while
 * delegating the actual analysis execution to the AnalysisExecutionController.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnalysisController {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisController.class);

  private final AnalysisExecutionController executionController;
  private final ConfigurationManager configurationManager;
  private final StatusPanel statusPanel;
  private final Component parentComponent;

  private JButton startButton;
  private JButton stopButton;

  // Analysis state
  private PipelineInfo currentPipeline;
  private File currentFolder;
  private int currentImageCount;
  private Map<String, Map<String, Object>> currentFeatures;

  /**
   * Creates a new AnalysisController instance.
   *
   * @param engine the SciPathJ engine
   * @param configurationManager the configuration manager
   * @param statusPanel the status panel for progress updates
   * @param parentComponent the parent component for dialogs
   */
  public AnalysisController(
       SciPathJEngine engine,
       ConfigurationManager configurationManager,
       StatusPanel statusPanel,
       Component parentComponent) {
    this.configurationManager = configurationManager;
    this.statusPanel = statusPanel;
    this.parentComponent = parentComponent;

    // Create the execution controller
    this.executionController = new AnalysisExecutionController(engine, configurationManager, statusPanel, parentComponent);
    this.executionController.setAnalysisCompleteCallback(this::handleAnalysisComplete);

    LOGGER.debug("Analysis controller created");
  }

  /**
   * Sets the start and stop buttons.
   *
   * @param startButton the start analysis button
   * @param stopButton the stop analysis button
   */
  public void setControlButtons(JButton startButton, JButton stopButton) {
    this.startButton = startButton;
    this.stopButton = stopButton;

    // Set up event handlers
    startButton.addActionListener(e -> startAnalysis());
    stopButton.addActionListener(e -> executionController.stopAnalysis());

    LOGGER.debug("Control buttons configured");
  }

  /**
   * Starts the analysis process.
   *
   * @param selectedPipeline the selected pipeline
   * @param selectedFolder the selected folder
   * @param imageCount the number of images to process
   */
  public void startAnalysis(PipelineInfo selectedPipeline, File selectedFolder, int imageCount) {
    LOGGER.info(
        "Analysis start requested for pipeline: {} with folder: {} ({} images)",
        selectedPipeline.getDisplayName(),
        selectedFolder.getAbsolutePath(),
        imageCount);

    // Store analysis parameters
    this.currentPipeline = selectedPipeline;
    this.currentFolder = selectedFolder;
    this.currentImageCount = imageCount;

    // Update button states
    if (startButton != null) {
      startButton.setEnabled(false);
    }
    if (stopButton != null) {
      stopButton.setEnabled(true);
    }

    // Start analysis via execution controller
    executionController.startAnalysis(selectedFolder, imageCount, this::handleAnalysisCompletion);
  }

  /**
   * Starts the analysis process (called by button).
   */
  private void startAnalysis() {
    // This method will be called by the button, but the actual logic
    // should be handled by the main window which has access to the selections
    LOGGER.debug("Start analysis button clicked");
  }

  /**
   * Stops the analysis process.
   */
  public void stopAnalysis() {
    executionController.stopAnalysis();

    if (startButton != null) {
      startButton.setEnabled(true);
    }
    if (stopButton != null) {
      stopButton.setEnabled(false);
    }
  }

  /**
   * Handles analysis completion from the execution controller.
   */
  private void handleAnalysisCompletion() {
    if (startButton != null) {
      startButton.setEnabled(true);
    }
    if (stopButton != null) {
      stopButton.setEnabled(false);
    }

    // Show completion dialog
    JOptionPane.showMessageDialog(
        parentComponent,
        "Analysis pipeline completed!\n\n"
            + "Pipeline: "
            + currentPipeline.getDisplayName()
            + "\n"
            + "Images processed: "
            + currentImageCount
            + "\n"
            + "Steps completed: Vessel Segmentation, Nuclear Segmentation\n"
            + "Remaining steps: Cytoplasm, Features, Classification, Statistics"
            + " (TODO)\n\n"
            + "Check the ROI toolbar to see detected vessels and nuclei.",
        "Analysis Complete",
        JOptionPane.INFORMATION_MESSAGE);

    LOGGER.info("Analysis pipeline execution completed");
  }

  /**
   * Handles extracted features from analysis completion.
   *
   * @param imageName the image name (may be null)
   * @param features the extracted features
   */
  private void handleAnalysisComplete(String imageName, Map<String, Map<String, Object>> features) {
    storeFeatures(features);
    LOGGER.debug("Received features from analysis completion");
  }

  /**
   * Updates the start button state based on current conditions.
   *
   * @param canStart whether analysis can be started
   */
  public void updateStartButtonState(boolean canStart) {
    if (startButton != null) {
      startButton.setEnabled(canStart);
    }
  }

  /**
   * Checks if analysis is currently running.
   *
   * @return true if analysis is running, false otherwise
   */
  public boolean isAnalysisRunning() {
    return executionController.isAnalysisRunning();
  }

  /**
   * Gets the start button.
   *
   * @return the start button
   */
  public JButton getStartButton() {
    return startButton;
  }

  /**
   * Gets the stop button.
   *
   * @return the stop button
   */
  public JButton getStopButton() {
    return stopButton;
  }

  /**
   * Shows the extracted features in a table dialog.
   */
  public void showFeaturesDialog() {
    if (currentFeatures == null || currentFeatures.isEmpty()) {
      JOptionPane.showMessageDialog(parentComponent,
          "No features available. Please run analysis first.",
          "No Features", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    try {
      // Load current settings for CSV format
      com.scipath.scipathj.infrastructure.config.MainSettings mainSettings = configurationManager.loadMainSettings();

      FeatureDisplayDialog dialog = new FeatureDisplayDialog(
          (java.awt.Frame) SwingUtilities.getWindowAncestor(parentComponent),
          currentFeatures,
          null, // imageName
          mainSettings);
      dialog.setVisible(true);
    } catch (Exception e) {
      LOGGER.error("Error showing features dialog", e);
      JOptionPane.showMessageDialog(parentComponent,
          "Error displaying features: " + e.getMessage(),
          "Display Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Stores the extracted features from analysis results.
   */
  public void storeFeatures(java.util.Map<String, java.util.Map<String, Object>> features) {
    this.currentFeatures = features;
    LOGGER.debug("Stored {} ROI features from analysis", features.size());
  }

  /**
   * Gets the current extracted features.
   */
  @SuppressWarnings("unchecked")
  public java.util.Map<String, java.util.Map<String, Object>> getCurrentFeatures() {
    return currentFeatures;
  }

  /**
   * Checks if features are available.
   */
  public boolean hasFeatures() {
    return currentFeatures != null && !currentFeatures.isEmpty();
  }
}
