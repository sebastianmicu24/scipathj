package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.analysis.algorithms.classification.FeatureExtraction;
import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.training.TrainingController;
import com.scipath.scipathj.ui.main.MainWindow;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.infrastructure.roi.UserROI.ROIType;
import com.scipath.scipathj.ui.dataset.NewDatasetROIOverlay;
import com.scipath.scipathj.ui.dataset.DatasetImageViewer;
import com.scipath.scipathj.ui.dataset.model.ProgressState;
import com.scipath.scipathj.infrastructure.roi.UserROI.ROIType;
import ij.ImagePlus;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all file operations for the dataset controls panel.
 * Manages ROI loading, training data download, and model training operations.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class FileOperationsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileOperationsHandler.class);

    // Dependencies
    private NewDatasetROIOverlay overlay;
    private DatasetImageViewer datasetImageViewer;
    private MainWindow mainWindow;

    // State
    private final ProgressState progressState = new ProgressState();
    private boolean trainingDataDownloaded = false;

    // Callbacks for progress reporting
    private Consumer<String> statusUpdater;
    private Consumer<ProgressState> progressUpdater;
    private Runnable clearROIsCallback;

    /**
     * Interface for different file operations.
     */
    public interface FileOperation {
        boolean execute(Container parent);
    }

    /**
     * Creates a new FileOperationsHandler.
     *
     * @param statusUpdater Callback for status updates
     * @param progressUpdater Callback for progress updates
     * @param clearROIsCallback Callback for clearing ROIs
     */
    public FileOperationsHandler(
            Consumer<String> statusUpdater,
            Consumer<ProgressState> progressUpdater,
            Runnable clearROIsCallback) {
        this.statusUpdater = statusUpdater;
        this.progressUpdater = progressUpdater;
        this.clearROIsCallback = clearROIsCallback;
    }

    /**
     * Set the overlay for ROI operations.
     */
    public void setOverlay(NewDatasetROIOverlay overlay) {
        this.overlay = overlay;
    }

    /**
     * Set the dataset image viewer.
     */
    public void setDatasetImageViewer(DatasetImageViewer datasetImageViewer) {
        this.datasetImageViewer = datasetImageViewer;
    }

    /**
     * Set the main window for Modal dialogs.
     */
    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    /**
     * Execute the Load ROIs from ZIP operation.
     */
    public boolean loadROIsFromZip(Container parent) {
        if (overlay == null) {
            JOptionPane.showMessageDialog(parent, "No ROI overlay available.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP files", "zip"));

        if (fileChooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return false; // User cancelled
        }

        File selectedFile = fileChooser.getSelectedFile();

        // For simplicity, assume image name for now
        String imageName = "P1 - 9 - 03.tif"; // This would come from image selection

        progressState.update(0, 1);
        progressState.setOperation("Loading ROIs");
        progressState.setDetails("Loading from ZIP file: " + selectedFile.getName());
        progressUpdater.accept(progressState);

        try {
            overlay.loadROIsFromZip(selectedFile, imageName);
            updateStatus("Loading ROIs from: " + selectedFile.getName());
            progressState.update(1, 1);
            progressUpdater.accept(progressState);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error loading ROIs from ZIP", e);
            JOptionPane.showMessageDialog(parent,
                "Error loading ROIs from ZIP file: " + e.getMessage(),
                "Load Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Execute the Clear ROIs operation.
     */
    public boolean clearROIs(Container parent) {
        if (overlay != null) {
            overlay.clear();
            clearROIsCallback.run();
        }

        updateStatus("ROIs cleared for current image (persistent classifications remain)");
        return true;
    }

    /**
     * Execute the Download Training Data operation.
     */
    public boolean downloadTrainingData(Container parent) {
        if (overlay == null) {
            JOptionPane.showMessageDialog(parent, "No ROI overlay available. Please load ROIs first.",
                                         "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!hasClassifications()) {
            JOptionPane.showMessageDialog(parent, getClassificationErrorMessage(),
                                         "Warning", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("training_data.json"));
        if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        File outputFile = fileChooser.getSelectedFile();

        progressState.update(0, 1);
        progressState.setOperation("Extracting Features");
        progressState.setDetails("Processing ROIs and extracting features...");
        progressUpdater.accept(progressState);

        try {
            extractAndSaveTrainingData(outputFile);
            trainingDataDownloaded = true;

            updateStatus("Training data saved to: " + outputFile.getName());
            progressState.update(1, 1);
            progressUpdater.accept(progressState);

            showTrainingDataSuccessMessage(parent);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error downloading training data", e);
            JOptionPane.showMessageDialog(parent,
                "Error extracting training data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Execute the Train XGBoost Model operation.
     */
    public boolean trainXGBoostModel(Container parent) {
        if (!trainingDataDownloaded) {
            LOGGER.warn("No training data downloaded yet");
            return false;
        }

        LOGGER.info("Launching XGBoost training interface...");

        try {
            if (mainWindow instanceof MainWindow) {
                MainWindow parentWindow = (MainWindow) mainWindow;
                parentWindow.switchToXGBoostTraining();
            } else {
                // Fallback to modal dialog
                if (parent instanceof JFrame) {
                    TrainingController controller = new TrainingController((JFrame) parent);
                    controller.showTrainingDialog(null, null);
                } else {
                    JOptionPane.showMessageDialog(parent,
                        "Unable to open training dialog: parent window is not a JFrame",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            LOGGER.info("XGBoost training workflow completed");
            return true;
        } catch (Exception e) {
            LOGGER.error("Error launching XGBoost training interface", e);
            JOptionPane.showMessageDialog(parent,
                "Error opening training interface: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Check if there are any manual classifications.
     */
    private boolean hasClassifications() {
        return true; // Simplified for now - would check overlay for classifications
    }

    /**
     * Get classification error message.
     */
    private String getClassificationErrorMessage() {
        return "No manually classified ROIs found. Please classify some cells first.";
    }

    /**
     * Extract and save training data using the analysis pipeline.
     */
    private void extractAndSaveTrainingData(File outputFile) throws Exception {
        // This would be a large method in the original - simplified for now
        LOGGER.info("Extracting features and saving training data to: {}", outputFile.getName());

        // Placeholder implementation
        Map<String, Object> jsonData = new LinkedHashMap<>();
        jsonData.put("timestamp", new java.util.Date().toString());
        jsonData.put("totalClassifiedCells", 0);
        jsonData.put("message", "Training data extraction from analysis pipeline");

        // Would normally use Jackson ObjectMapper to write the actual data
        throw new UnsupportedOperationException("Training data extraction not yet implemented in refactored version");

        // TODO: Implement full feature extraction logic from original DatasetControlsPanel
    }

    /**
     * Update status through callback.
     */
    private void updateStatus(String message) {
        if (statusUpdater != null) {
            statusUpdater.accept(message);
        }
    }

    /**
     * Show success message after training data download.
     */
    private void showTrainingDataSuccessMessage(Container parent) {
        JOptionPane.showMessageDialog(parent,
            "Training data successfully saved! You can now train your XGBoost classifier.",
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Check if training data has been downloaded.
     */
    public boolean isTrainingDataDownloaded() {
        return trainingDataDownloaded;
    }

    /**
     * Reset the training data downloaded flag.
     */
    public void resetTrainingDataFlag() {
        trainingDataDownloaded = false;
    }

    /**
     * Get current progress state.
     */
    public ProgressState getProgressState() {
        return progressState.copy();
    }
}