package com.scipath.scipathj.ui.controllers;

import com.scipath.scipathj.core.analysis.AnalysisPipeline;
import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.core.config.SegmentationConstants;
import com.scipath.scipathj.core.config.VesselSegmentationSettings;
import com.scipath.scipathj.core.engine.SciPathJEngine;
import com.scipath.scipathj.ui.components.StatusPanel;
import com.scipath.scipathj.ui.dialogs.FeatureDisplayDialog;
import com.scipath.scipathj.ui.model.PipelineInfo;
import java.awt.*;
import java.io.File;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for managing analysis operations.
 *
 * <p>This class handles the start/stop analysis functionality, progress tracking,
 * and coordination between the UI and the analysis engine.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnalysisController {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisController.class);
  private final SciPathJEngine engine;
  private final ConfigurationManager configurationManager;
  private final StatusPanel statusPanel;
  private final Component parentComponent;

  private JButton startButton;
  private JButton stopButton;
  private SwingWorker<Void, String> currentAnalysisWorker;

  // Analysis parameters
  private PipelineInfo currentPipeline;
  private File currentFolder;
  private int currentImageCount;
  private java.util.Map<String, java.util.Map<String, Object>> currentFeatures;

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
    this.engine = engine;
    this.configurationManager = configurationManager;
    this.statusPanel = statusPanel;
    this.parentComponent = parentComponent;

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
    stopButton.addActionListener(e -> stopAnalysis());

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

    // Show progress bar
    statusPanel.showProgress(0, "Starting analysis...");

    // Start combined vessel and nucleus segmentation analysis
    performCombinedSegmentationAnalysis();
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
    LOGGER.info("Analysis stop requested");

    if (currentAnalysisWorker != null && !currentAnalysisWorker.isDone()) {
      currentAnalysisWorker.cancel(true);
    }

    if (startButton != null) {
      startButton.setEnabled(true);
    }
    if (stopButton != null) {
      stopButton.setEnabled(false);
    }

    statusPanel.setProgress(0);
    statusPanel.setProgressMessage("Stopped");
    statusPanel.hideProgress();
    statusPanel.setStatus("Analysis stopped");
  }

  /**
   * Performs analysis using the new AnalysisPipeline.
   * Currently implements steps 1-2 (vessel and nuclear segmentation).
   */
  private void performCombinedSegmentationAnalysis() {
    if (startButton != null) {
      startButton.setEnabled(false);
    }
    if (stopButton != null) {
      stopButton.setEnabled(true);
    }

    currentAnalysisWorker =
        new SwingWorker<Void, String>() {
          @Override
          protected Void doInBackground() throws Exception {
            try {
              // Get all image files from the folder
              File[] imageFiles = getImageFiles(currentFolder);

              if (imageFiles.length == 0) {
                publish("No supported image files found in the selected folder.");
                return null;
              }

              LOGGER.info("Starting analysis pipeline for {} images", imageFiles.length);

              // Create analysis pipeline with current settings
              VesselSegmentationSettings vesselSettings =
                  configurationManager.loadVesselSegmentationSettings();
              NuclearSegmentationSettings nuclearSettings =
                  configurationManager.loadNuclearSegmentationSettings();

              // Create required dependencies for AnalysisPipeline
              com.scipath.scipathj.core.config.MainSettings mainSettings =
                  configurationManager.loadMainSettings();
              com.scipath.scipathj.ui.components.ROIManager roiManager =
                  com.scipath.scipathj.ui.components.ROIManager.getInstance();

              AnalysisPipeline pipeline =
                  new AnalysisPipeline(configurationManager, mainSettings, roiManager);

              // Set up progress callbacks
              pipeline.setProgressMessageCallback(message -> publish(message));
              pipeline.setProgressPercentCallback(percent -> statusPanel.setProgress(percent));

              // Execute the pipeline
              AnalysisPipeline.AnalysisResults results = pipeline.processBatch(imageFiles);

              // Store extracted features for later display
              if (results.allExtractedFeatures() != null && !results.allExtractedFeatures().isEmpty()) {
                storeFeatures(results.allExtractedFeatures());
              }

              // Final update
              statusPanel.setProgress(100);
              publish(
                  String.format(
                      "Analysis completed! Found %d vessels, %d nuclei, and %d cells across %d images.",
                      results.totalVessels(), results.totalNuclei(), results.totalCells(), results.processedImages()));

            } catch (Exception e) {
              LOGGER.error("Error during analysis pipeline execution", e);
              publish("Analysis failed: " + e.getMessage());
            }

            return null;
          }

          @Override
          protected void process(java.util.List<String> chunks) {
            String latestMessage = chunks.get(chunks.size() - 1);
            statusPanel.setProgressMessage(latestMessage);
            statusPanel.setStatus("Combined Segmentation: " + latestMessage);
          }

          @Override
          protected void done() {
            if (startButton != null) {
              startButton.setEnabled(true);
            }
            if (stopButton != null) {
              stopButton.setEnabled(false);
            }

            try {
              get(); // Check for exceptions
              statusPanel.setProgress(100);
              statusPanel.setProgressMessage("Analysis Complete");
              statusPanel.setStatus("Combined segmentation analysis completed successfully");

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

            } catch (Exception e) {
              LOGGER.error("Analysis failed", e);
              statusPanel.setProgressMessage("Analysis Failed");
              statusPanel.setStatus("Analysis failed: " + e.getMessage());

              JOptionPane.showMessageDialog(
                  parentComponent,
                  "Analysis failed:\n" + e.getMessage(),
                  "Analysis Error",
                  JOptionPane.ERROR_MESSAGE);
            }

            // Hide progress after a delay
            Timer timer = new Timer(3000, e -> statusPanel.hideProgress());
            timer.setRepeats(false);
            timer.start();

            LOGGER.info("Analysis pipeline execution completed");
          }
        };

    currentAnalysisWorker.execute();
  }

  /**
   * Gets all supported image files from the specified folder.
   */
  private File[] getImageFiles(File folder) {
    return folder.listFiles(
        (dir, name) -> {
          String lowerName = name.toLowerCase();
          for (String extension : SegmentationConstants.SUPPORTED_IMAGE_EXTENSIONS) {
            if (lowerName.endsWith(extension)) {
              return true;
            }
          }
          return false;
        });
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
    return currentAnalysisWorker != null && !currentAnalysisWorker.isDone();
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
      FeatureDisplayDialog dialog = new FeatureDisplayDialog(
          (java.awt.Frame) SwingUtilities.getWindowAncestor(parentComponent),
          currentFeatures);
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
