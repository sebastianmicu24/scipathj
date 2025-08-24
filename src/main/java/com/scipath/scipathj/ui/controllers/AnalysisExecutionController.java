package com.scipath.scipathj.ui.controllers;

import com.scipath.scipathj.analysis.pipeline.AnalysisPipeline;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.analysis.config.NuclearSegmentationSettings;
import com.scipath.scipathj.analysis.config.SegmentationConstants;
import com.scipath.scipathj.analysis.config.VesselSegmentationSettings;
import com.scipath.scipathj.infrastructure.engine.SciPathJEngine;
import com.scipath.scipathj.ui.common.StatusPanel;
import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for managing analysis execution operations.
 *
 * <p>This controller handles the start/stop of analysis operations, progress tracking,
 * and result processing. It focuses specifically on the execution aspects of analysis
 * while delegating UI coordination to other controllers.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnalysisExecutionController {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisExecutionController.class);

  private final SciPathJEngine engine;
  private final ConfigurationManager configurationManager;
  private final StatusPanel statusPanel;
  private final Component parentComponent;

  private SwingWorker<Void, String> currentAnalysisWorker;
  private BiConsumer<String, Map<String, Map<String, Object>>> onAnalysisComplete;

  // Analysis state
  private boolean isAnalysisRunning = false;

  /**
   * Creates a new AnalysisExecutionController instance.
   *
   * @param engine the SciPathJ engine
   * @param configurationManager the configuration manager
   * @param statusPanel the status panel for progress updates
   * @param parentComponent the parent component for dialogs
   */
  public AnalysisExecutionController(
      SciPathJEngine engine,
      ConfigurationManager configurationManager,
      StatusPanel statusPanel,
      Component parentComponent) {
    this.engine = engine;
    this.configurationManager = configurationManager;
    this.statusPanel = statusPanel;
    this.parentComponent = parentComponent;

    LOGGER.debug("Analysis execution controller created");
  }

  /**
   * Sets the callback for when analysis completes.
   *
   * @param callback the callback function receiving (imageName, features) parameters
   */
  public void setAnalysisCompleteCallback(BiConsumer<String, Map<String, Map<String, Object>>> callback) {
    this.onAnalysisComplete = callback;
  }

  /**
   * Starts the analysis process for the given parameters.
   *
   * @param selectedFolder the selected folder containing images
   * @param imageCount the number of images to process
   * @param onComplete callback when analysis completes
   */
  public void startAnalysis(File selectedFolder, int imageCount, Runnable onComplete) {
    LOGGER.info("Analysis start requested for folder: {} ({} images)", selectedFolder.getAbsolutePath(), imageCount);

    if (isAnalysisRunning) {
      LOGGER.warn("Analysis already running, ignoring start request");
      return;
    }

    // Show progress bar
    statusPanel.showProgress(0, "Starting analysis...");

    // Start analysis
    performCombinedSegmentationAnalysis(selectedFolder, imageCount, onComplete);
  }

  /**
   * Stops the current analysis process.
   */
  public void stopAnalysis() {
    LOGGER.info("Analysis stop requested");

    if (currentAnalysisWorker != null && !currentAnalysisWorker.isDone()) {
      currentAnalysisWorker.cancel(true);
      isAnalysisRunning = false;
    }

    statusPanel.setProgress(0);
    statusPanel.setProgressMessage("Stopped");
    statusPanel.hideProgress();
    statusPanel.setStatus("Analysis stopped");
  }

  /**
   * Checks if analysis is currently running.
   *
   * @return true if analysis is running, false otherwise
   */
  public boolean isAnalysisRunning() {
    return isAnalysisRunning;
  }

  /**
   * Performs the combined segmentation analysis.
   */
  private void performCombinedSegmentationAnalysis(File selectedFolder, int imageCount, Runnable onComplete) {
    isAnalysisRunning = true;

    currentAnalysisWorker = new SwingWorker<Void, String>() {
      @Override
      protected Void doInBackground() throws Exception {
        try {
          // Get all image files from the folder
          File[] imageFiles = getImageFiles(selectedFolder);

          if (imageFiles.length == 0) {
            publish("No supported image files found in the selected folder.");
            return null;
          }

          LOGGER.info("Starting analysis pipeline for {} images", imageFiles.length);

          // Create analysis pipeline with current settings
          VesselSegmentationSettings vesselSettings = configurationManager.loadVesselSegmentationSettings();
          NuclearSegmentationSettings nuclearSettings = configurationManager.loadNuclearSegmentationSettings();

          var mainSettings = configurationManager.loadMainSettings();
          var roiManager = com.scipath.scipathj.ui.common.ROIManager.getInstance();

          AnalysisPipeline pipeline = new AnalysisPipeline(configurationManager, mainSettings, roiManager);

          // Set up progress callbacks
          pipeline.setProgressMessageCallback(this::publish);
          pipeline.setProgressPercentCallback(percent -> statusPanel.setProgress(percent));

          // Execute the pipeline
          AnalysisPipeline.AnalysisResults results = pipeline.processBatch(imageFiles);

          // Store extracted features for later display
          if (results.allExtractedFeatures() != null && !results.allExtractedFeatures().isEmpty()) {
            // Notify callback about extracted features
            if (onAnalysisComplete != null) {
              onAnalysisComplete.accept(null, results.allExtractedFeatures());
            }
          }

          // Final update
          statusPanel.setProgress(100);
          publish(String.format("Analysis completed! Found %d vessels, %d nuclei, and %d cells across %d images.",
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
        isAnalysisRunning = false;

        try {
          get(); // Check for exceptions
          statusPanel.setProgress(100);
          statusPanel.setProgressMessage("Analysis Complete");
          statusPanel.setStatus("Combined segmentation analysis completed successfully");

          // Call completion callback
          if (onComplete != null) {
            onComplete.run();
          }

        } catch (Exception e) {
          LOGGER.error("Analysis failed", e);
          statusPanel.setProgressMessage("Analysis Failed");
          statusPanel.setStatus("Analysis failed: " + e.getMessage());

          JOptionPane.showMessageDialog(parentComponent,
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
    return folder.listFiles((dir, name) -> {
      String lowerName = name.toLowerCase();
      for (String extension : SegmentationConstants.SUPPORTED_IMAGE_EXTENSIONS) {
        if (lowerName.endsWith(extension)) {
          return true;
        }
      }
      return false;
    });
  }
}